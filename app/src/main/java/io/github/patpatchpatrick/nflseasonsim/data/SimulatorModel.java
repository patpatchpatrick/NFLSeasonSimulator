package io.github.patpatchpatrick.nflseasonsim.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Week;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SimulatorModel implements SimulatorMvpContract.SimulatorModel {
    //Simulator model class used to manage communication with database

    @Inject
    ContentResolver contentResolver;

    @Inject
    SimulatorMvpContract.SimulatorPresenter mPresenter;

    private CompositeDisposable mCompositeDisposable;

    //FINAL integers used to determine insert types and query types for when data  is inserted or
    //queried from the database.
    //These insertTypes and queryTypes are used to determine what happens to the data

    public static final int QUERY_STANDINGS_PLAYOFF = 1;
    public static final int QUERY_STANDINGS_REGULAR = 2;
    public static final int QUERY_STANDINGS_LOAD_SEASON = 3;
    public static final int QUERY_STANDINGS_POSTSEASON = 4;
    public static final int QUERY_STANDINGS_LOAD_POSTSEASON = 5;
    public static final int QUERY_MATCHES_ALL = 0;

    public static final int INSERT_MATCHES_SCHEDULE = 0;
    public static final int INSERT_MATCHES_PLAYOFFS_WILDCARD = 1;
    public static final int INSERT_MATCHES_PLAYOFFS_DIVISIONAL = 2;
    public static final int INSERT_MATCHES_PLAYOFFS_CHAMPIONSHIP = 3;
    public static final int INSERT_MATCHES_PLAYOFFS_SUPERBOWL = 4;

    //Data for season resources
    public Schedule mSchedule;
    public HashMap<String, Team> mTeamList;

    private Scheduler mScheduler;

    public SimulatorModel() {

        //Create new composite disposable to manage disposables from RxJava subscriptions
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        mCompositeDisposable = compositeDisposable;

        //Scheduler with fixed thread pool to prevent Schedulers.io from creating too many threads/memory leak
        mScheduler = Schedulers.from(Executors.newFixedThreadPool(50));

    }


    @Override
    public void setSchedule(Schedule schedule) {
        mSchedule = schedule;
    }

    @Override
    public void setTeamList(HashMap<String, Team> teamList) {
        mTeamList = teamList;
    }

    @Override
    public HashMap<String, Team> getTeamList() {
        return mTeamList;
    }

    @Override
    public ArrayList<Team> getTeamArrayList() {

        ArrayList<Team> teamArrayList = new ArrayList();

        for (String teamName : mTeamList.keySet()){
            teamArrayList.add(mTeamList.get(teamName));
        }

        return teamArrayList;
    }

    @Override
    public Schedule getSchedule() {
        return mSchedule;
    }

    @Override
    public void insertMatch(final Match match) {

        //Insert a match into the database

        Observable<Uri> insertMatchObservable = Observable.fromCallable(new Callable<Uri>() {
            @Override
            public Uri call() throws Exception {
                ContentValues values = new ContentValues();
                values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE, match.getTeam1().getName());
                values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO, match.getTeam2().getName());
                values.put(MatchEntry.COLUMN_MATCH_WEEK, match.getWeek());
                Uri uri = contentResolver.insert(MatchEntry.CONTENT_URI, values);
                return uri;
            }
        });

        insertMatchObservable.subscribeOn(mScheduler).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Uri>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Uri uri) {
                match.setUri(uri);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {


            }
        });


    }

    @Override
    public void insertMatches(final int insertType) {

        //Insert a regular season schedule's matches into the db
        //First, iterate through the schedule and add all season matches to an ArrayList
        //Then, add an Obervable.fromIterable to iterate through the ArrayList and add each
        //match to the db.
        //After all matches are added to the db, notify the presenter via the matchesInserted callback
        ArrayList<Match> seasonMatches = new ArrayList<>();
        int weekNumber = 1;
        while (weekNumber <= 17) {
            ArrayList<Match> weekMatches = mSchedule.getWeek(weekNumber).getMatches();
            for (Match match : weekMatches) {
                seasonMatches.add(match);
            }
            weekNumber++;
        }


        Observable<Match> insertMatchesObservable = Observable.fromIterable(seasonMatches);
        insertMatchesObservable.subscribeOn(AndroidSchedulers.mainThread()).observeOn(mScheduler).subscribe(new Observer<Match>() {
            @Override
            public void onSubscribe(Disposable d) {

                mCompositeDisposable.add(d);

            }

            @Override
            public void onNext(Match match) {

                ContentValues values = new ContentValues();
                values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE, match.getTeam1().getName());
                values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO, match.getTeam2().getName());
                values.put(MatchEntry.COLUMN_MATCH_WEEK, match.getWeek());
                Uri uri = contentResolver.insert(MatchEntry.CONTENT_URI, values);

                match.setUri(uri);

            }

            @Override
            public void onError(Throwable e) {

                Log.d("InsertMatchesError: ", "" + e);

            }

            @Override
            public void onComplete() {

                mPresenter.matchesInserted(insertType);
            }
        });


    }

    @Override
    public void insertMatches(final int insertType, Week week) {

        //Insert a week's matches into the database

            ArrayList<Match> weekMatches = week.getMatches();


        Observable<Match> insertMatchesObservable = Observable.fromIterable(weekMatches);
        insertMatchesObservable.subscribeOn(AndroidSchedulers.mainThread()).observeOn(mScheduler).subscribe(new Observer<Match>() {
            @Override
            public void onSubscribe(Disposable d) {

                mCompositeDisposable.add(d);

            }

            @Override
            public void onNext(Match match) {

                ContentValues values = new ContentValues();
                values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE, match.getTeam1().getName());
                values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO, match.getTeam2().getName());
                values.put(MatchEntry.COLUMN_MATCH_WEEK, match.getWeek());
                Uri uri = contentResolver.insert(MatchEntry.CONTENT_URI, values);

                match.setUri(uri);

            }

            @Override
            public void onError(Throwable e) {

                Log.d("InsertMatchesError: ", "" + e);

            }

            @Override
            public void onComplete() {

                mPresenter.matchesInserted(insertType);
            }
        });



    }

    @Override
    public void insertTeam(final Team team) {

        //Insert a team into the database

        Observable<Uri> insertTeamObservable = Observable.fromCallable(new Callable<Uri>() {
            @Override
            public Uri call() throws Exception {
                String name = team.getName();
                double elo = team.getElo();
                double offRating = team.getOffRating();
                double defRating = team.getDefRating();
                int currentWins = team.getWins();
                int currentLosses = team.getLosses();
                int currentDraws = team.getDraws();
                int division = team.getDivision();
                int conference = team.getConference();

                ContentValues values = new ContentValues();
                values.put(TeamEntry.COLUMN_TEAM_NAME, name);
                values.put(TeamEntry.COLUMN_TEAM_ELO, elo);
                values.put(TeamEntry.COLUMN_TEAM_OFF_RATING, offRating);
                values.put(TeamEntry.COLUMN_TEAM_DEF_RATING, defRating);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_WINS, currentWins);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_LOSSES, currentLosses);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_DRAWS, currentDraws);
                values.put(TeamEntry.COLUMN_TEAM_DIVISION, division);
                values.put(TeamEntry.COLUMN_TEAM_CONFERENCE,  conference);

                //Insert values into database
                Uri uri = contentResolver.insert(TeamEntry.CONTENT_URI, values);
                return uri;
            }
        });

        insertTeamObservable.subscribeOn(mScheduler).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Uri>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Uri uri) {
                team.setUri(uri);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });


    }

    @Override
    public void insertTeams() {

        //Insert a hashMap's matches into the db
        //First, iterate through the HashMap and add all matches to an ArrayList
        //Then, add an Obervable.fromIterable to iterate through the ArrayList and add each
        //team to the db.
        //After all teams are added to the db, notify the presenter via the teamsInserted callback

        ArrayList<Team> teamArrayList = new ArrayList<>();

        for (String teamName : mTeamList.keySet()) {
            teamArrayList.add(mTeamList.get(teamName));
        }

        Observable<Team> insertTeamsObservable = Observable.fromIterable(teamArrayList);
        insertTeamsObservable.subscribeOn(AndroidSchedulers.mainThread()).observeOn(mScheduler).subscribe(new Observer<Team>() {
            @Override
            public void onSubscribe(Disposable d) {

                mCompositeDisposable.add(d);

            }

            @Override
            public void onNext(Team team) {

                String name = team.getName();
                double elo = team.getElo();
                double offRating = team.getOffRating();
                double defRating = team.getDefRating();
                int currentWins = team.getWins();
                int currentLosses = team.getLosses();
                int currentDraws = team.getDraws();
                int division = team.getDivision();
                int conference = team.getConference();

                ContentValues values = new ContentValues();
                values.put(TeamEntry.COLUMN_TEAM_NAME, name);
                values.put(TeamEntry.COLUMN_TEAM_ELO, elo);
                values.put(TeamEntry.COLUMN_TEAM_OFF_RATING, offRating);
                values.put(TeamEntry.COLUMN_TEAM_DEF_RATING, defRating);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_WINS, currentWins);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_LOSSES, currentLosses);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_DRAWS, currentDraws);
                values.put(TeamEntry.COLUMN_TEAM_DIVISION, division);
                values.put(TeamEntry.COLUMN_TEAM_CONFERENCE, conference);

                //Insert values into database
                Uri uri = contentResolver.insert(TeamEntry.CONTENT_URI, values);

                team.setUri(uri);

            }

            @Override
            public void onError(Throwable e) {

                Log.d("InsertTeamsError: ", "" + e);

            }

            @Override
            public void onComplete() {

                mPresenter.teamsInserted();

            }
        });

    }

    @Override
    public void updateMatch(final Match match, final Uri uri) {


        //Update a match in the database

        Observable<Integer> updateMatchObservable = Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {

                //Update match database scores and match complete values
                ContentValues values = new ContentValues();
                values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE_SCORE, match.getTeam1Score());
                values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO_SCORE, match.getTeam2Score());
                values.put(MatchEntry.COLUMN_MATCH_COMPLETE, MatchEntry.MATCH_COMPLETE_YES);

                int rowsUpdated = contentResolver.update(uri, values, null, null);

                return rowsUpdated;
            }
        });

        updateMatchObservable.subscribeOn(mScheduler).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Integer rowsUpdated) {
            }

            @Override
            public void onError(Throwable e) {
                Log.d("UpdateMatchError ", "" + e);

            }

            @Override
            public void onComplete() {
            }
        });


    }


    @Override
    public void updateTeam(final Team team, final Uri uri) {


        //Update a team in the database

        Observable<Integer> updateTeamObservable = Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                //Update team database wins, losses and win loss pct values
                ContentValues values = new ContentValues();
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_WINS, team.getWins());
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_LOSSES, team.getLosses());
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_DRAWS, team.getDraws());
                values.put(TeamEntry.COLUMN_TEAM_WIN_LOSS_PCT, team.getWinLossPct());
                values.put(TeamEntry.COLUMN_TEAM_DIV_WINS, team.getDivisionWins());
                values.put(TeamEntry.COLUMN_TEAM_DIV_LOSSES, team.getDivisionLosses());
                values.put(TeamEntry.COLUMN_TEAM_DIV_WIN_LOSS_PCT, team.getDivisionWinLossPct());
                values.put(TeamEntry.COLUMN_TEAM_PLAYOFF_ELIGIBILE, team.getPlayoffEligible());

                int rowsUpdated = contentResolver.update(uri, values, null, null);


                return rowsUpdated;
            }
        });

        updateTeamObservable.subscribeOn(mScheduler).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Integer rowsUpdated) {
            }

            @Override
            public void onError(Throwable e) {
                Log.d("UpdateTeamError: ", "" + e);

            }

            @Override
            public void onComplete() {
            }
        });


    }

    @Override
    public void queryStandings(final int queryType) {

        //Query the standings from the database

        Observable<Cursor> queryStandingsObservable = Observable.fromCallable(new Callable<Cursor>() {
            @Override
            public Cursor call() throws Exception {
                //Query standings
                String[] standingsProjection = {
                        TeamEntry._ID,
                        TeamEntry.COLUMN_TEAM_NAME,
                        TeamEntry.COLUMN_TEAM_DIVISION,
                        TeamEntry.COLUMN_TEAM_CONFERENCE,
                        TeamEntry.COLUMN_TEAM_CURRENT_WINS,
                        TeamEntry.COLUMN_TEAM_CURRENT_LOSSES,
                        TeamEntry.COLUMN_TEAM_CURRENT_DRAWS,
                        TeamEntry.COLUMN_TEAM_WIN_LOSS_PCT,
                        TeamEntry.COLUMN_TEAM_DIV_WINS,
                        TeamEntry.COLUMN_TEAM_DIV_LOSSES,
                        TeamEntry.COLUMN_TEAM_DIV_WIN_LOSS_PCT,
                        TeamEntry.COLUMN_TEAM_PLAYOFF_ELIGIBILE,
                        TeamEntry.COLUMN_TEAM_ELO,
                        TeamEntry.COLUMN_TEAM_OFF_RATING,
                        TeamEntry.COLUMN_TEAM_DEF_RATING,
                };

                Cursor standingsCursor;

                //Query the team data depending on queryType requested

                if (queryType == QUERY_STANDINGS_POSTSEASON || queryType == QUERY_STANDINGS_LOAD_POSTSEASON) {

                    //For postseason query,  don't query teams that aren't playoff eligible
                    //Sort by playoff seed and conference

                    String selection = TeamEntry.COLUMN_TEAM_PLAYOFF_ELIGIBILE + "!=?";
                    String[] selectionArgs = new String[]{String.valueOf(TeamEntry.PLAYOFF_NOT_ELIGIBLE)};

                    standingsCursor = contentResolver.query(TeamEntry.CONTENT_URI, standingsProjection,
                            selection, selectionArgs,
                            TeamEntry.COLUMN_TEAM_CONFERENCE + ", " + TeamEntry.COLUMN_TEAM_PLAYOFF_ELIGIBILE);

                } else {

                    standingsCursor = contentResolver.query(TeamEntry.CONTENT_URI, standingsProjection,
                            null, null,
                            TeamEntry.COLUMN_TEAM_DIVISION + ", " + TeamEntry.COLUMN_TEAM_WIN_LOSS_PCT + " DESC, " + TeamEntry.COLUMN_TEAM_DIV_WIN_LOSS_PCT + " DESC");
                }

                return standingsCursor;
            }
        });

        queryStandingsObservable.subscribeOn(mScheduler).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Cursor>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Cursor standingsCursor) {
                mPresenter.teamsOrStandingsQueried(queryType, standingsCursor);
            }

            @Override
            public void onError(Throwable e) {
                Log.d("Query Error: ", "" + e);

            }

            @Override
            public void onComplete() {
            }
        });


    }

    @Override
    public void queryMatches(final int weekNumber, final boolean singleMatch, final boolean matchesPlayed) {

        //Query the matches/schedule from the database

        Observable<Cursor> queryMatchesObservable = Observable.fromCallable(new Callable<Cursor>() {
            @Override
            public Cursor call() throws Exception {
                //Query standings
                String[] matchesProjection = {
                        MatchEntry._ID,
                        MatchEntry.COLUMN_MATCH_TEAM_ONE,
                        MatchEntry.COLUMN_MATCH_TEAM_TWO,
                        MatchEntry.COLUMN_MATCH_TEAM_ONE_SCORE,
                        MatchEntry.COLUMN_MATCH_TEAM_TWO_SCORE,
                        MatchEntry.COLUMN_MATCH_WEEK,
                        MatchEntry.COLUMN_MATCH_COMPLETE,
                };

                Cursor matchesCursor;

                //Either query all matches or query matches for a specific week, depending on the
                //input weekNumber integer that was given

                if (weekNumber == QUERY_MATCHES_ALL) {

                    //Query all matches

                    matchesCursor = contentResolver.query(MatchEntry.CONTENT_URI, matchesProjection,
                            null, null,
                            null);
                } else if (singleMatch == true) {

                    //Query a single week's matches

                    String selection = MatchEntry.COLUMN_MATCH_WEEK + "=?";
                    String[] selectionArgs = new String[]{String.valueOf(weekNumber)};

                    matchesCursor = contentResolver.query(MatchEntry.CONTENT_URI, matchesProjection,
                            selection, selectionArgs,
                            null);

                } else {

                    //Query all matches up to and including a single week

                    String selection = MatchEntry.COLUMN_MATCH_WEEK + "<=?";
                    String[] selectionArgs = new String[]{String.valueOf(weekNumber)};

                    matchesCursor = contentResolver.query(MatchEntry.CONTENT_URI, matchesProjection,
                            selection, selectionArgs,
                            MatchEntry.COLUMN_MATCH_WEEK + " DESC");

                }


                return matchesCursor;
            }
        });

        queryMatchesObservable.subscribeOn(mScheduler).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Cursor>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Cursor matchesCursor) {
                mPresenter.matchesQueried(weekNumber, matchesCursor, matchesPlayed);
            }

            @Override
            public void onError(Throwable e) {
                Log.d("Query Error: ", "" + e);

            }

            @Override
            public void onComplete() {
            }
        });


    }

    @Override
    public void deleteAllData() {

        //Delete all data from the database

        Observable<Integer> deleteDataObservable = Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int teamsDeleted = contentResolver.delete(TeamEntry.CONTENT_URI, null, null);
                int matchesDeleted = contentResolver.delete(MatchEntry.CONTENT_URI, null, null);
                return teamsDeleted + matchesDeleted;
            }
        });

        deleteDataObservable.subscribeOn(mScheduler).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Integer rowsDeleted) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {
                mPresenter.dataDeleted();


            }
        });



    }

    @Override
    public void destroyModel() {

        //This method is called when the main activity is destroyed.  It will dispose of all disposables
        //within the composite disposable.

        mCompositeDisposable.dispose();
    }


}
