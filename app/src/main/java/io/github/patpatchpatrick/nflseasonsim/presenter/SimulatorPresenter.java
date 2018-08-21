package io.github.patpatchpatrick.nflseasonsim.presenter;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.MainActivity;
import io.github.patpatchpatrick.nflseasonsim.R;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.BaseView;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Data;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.NFLConstants;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Standings;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Week;

public class SimulatorPresenter extends BasePresenter<SimulatorMvpContract.SimulatorView>
        implements SimulatorMvpContract.SimulatorPresenter, Data {

    //Simulator presenter class is used to communicate between the MainActivity (view) and the Model (MVP Architecture)

    @Inject
    SimulatorModel mModel;

    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    Context mContext;

    @Inject
    BaseView mBaseView;

    private static int mCurrentWeek;
    private static Boolean mSeasonInitialized = false;
    private static Boolean mPlayoffsStarted = false;

    public SimulatorPresenter(SimulatorMvpContract.SimulatorView view) {
        super(view);
    }

    public SimulatorPresenter() {
        super();
    }

    public void setView(SimulatorMvpContract.SimulatorView view) {
        super.setView(view);
    }

    @Override
    public void simulateWeek() {
        //Simulate a single week
        mModel.getSchedule().getWeek(mCurrentWeek).simulate();
        //After the week is complete, query the standings (and display them)
        mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_REGULAR);
        //Query the week scores and display them
        mModel.queryMatches(mCurrentWeek, true, true);
        //Week is complete so increment the current week value
        mCurrentWeek++;
        this.view.setCurrentWeekPreference(mCurrentWeek);
    }

    @Override
    public void simulatePlayoffWeek() {
        //Simulate a single week
        mModel.getSchedule().getWeek(mCurrentWeek).simulate();
        //After the week is complete, query the standings (and display them)
        mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_POSTSEASON);
        //Week is complete so increment the current week value
        mCurrentWeek++;
        this.view.setCurrentWeekPreference(mCurrentWeek);
    }

    @Override
    public void initializeSeason() {
        mCurrentWeek = 1;
        setCurrentWeekPreference(mCurrentWeek);
        createTeams();
        //Insert teams into database.  After teams are inserted, the teamsInserted() callback is
        //received from the model
        mModel.insertTeams();
    }

    @Override
    public void initiatePlayoffs() {
        mCurrentWeek = 18;
        this.view.setCurrentWeekPreference(mCurrentWeek);
        //Initiate the playoffs
        //Query the standings from the playoffs standings and the rest of the playoffs is initiated via the
        // standingsQueried method
        mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_POSTSEASON);

    }

    @Override
    public void loadSeasonFromDatabase() {
        //Load season from database (create teams and schedule)
        mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_LOAD_SEASON);
        //Season has been loaded, so set preference to true
        setSeasonLoadedPreference(true);
        //Notify baseview that season has been loaded
        mBaseView.onSeasonLoadedFromDb();

    }

    @Override
    public void loadAlreadySimulatedData() {
        //Load the standings, as well as the matches that have already been simulated (last week's matches)
        mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_REGULAR);
        //Query all weeks that have already occurred;
        mModel.queryMatches(mCurrentWeek - 1, false, true);
    }

    @Override
    public void loadAlreadySimulatedPlayoffData() {
        //Query playoff data
        mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_LOAD_POSTSEASON);
    }

    @Override
    public void teamsInserted() {
        //After the teams are inserted into the DB, create the season schedule and insert the
        //schedule matches into the DB
        //After the matches are inserted into the DB, the matchesInserted() callback is received
        //from the model
        createSchedule();
        mModel.insertMatches(SimulatorModel.INSERT_MATCHES_SCHEDULE);
        //Set the teams elo types based on user selected preference
        setEloType();

    }

    @Override
    public void matchesInserted(int insertType) {

        //Callback received after matches are inserted into the database in the model
        //An action is performed below depending on the insertType

        if (insertType == SimulatorModel.INSERT_MATCHES_SCHEDULE) {
            //Notify main activity view that season is initialized
            setSeasonInitializedPreference(true);
            setSeasonLoadedPreference(true);
            mBaseView.onSeasonInitialized();
        }

        if (insertType == SimulatorModel.INSERT_MATCHES_PLAYOFFS_WILDCARD) {
            mModel.queryMatches(MatchEntry.MATCH_WEEK_WILDCARD, true, false);
        }

        if (insertType == SimulatorModel.INSERT_MATCHES_PLAYOFFS_DIVISIONAL) {
            mModel.queryMatches(MatchEntry.MATCH_WEEK_DIVISIONAL, true, false);
        }
        if (insertType == SimulatorModel.INSERT_MATCHES_PLAYOFFS_CHAMPIONSHIP) {
            mModel.queryMatches(MatchEntry.MATCH_WEEK_CHAMPIONSHIP, true, false);
        }
        if (insertType == SimulatorModel.INSERT_MATCHES_PLAYOFFS_SUPERBOWL) {
            mModel.queryMatches(MatchEntry.MATCH_WEEK_SUPERBOWL, true, false);
        }
    }

    @Override
    public void matchesQueried(int queryType, Cursor matchesCursor, boolean matchesPlayed) {

        Log.d("Matches", "QUERIED");

        //If you are not querying all matches, put the match score data in a string and display it in the main activity
        //If all matches are being queried, the ELSE statement below will be hit and the entire schedule will loaded and created
        //from the data queried in the database

        if (queryType != SimulatorModel.QUERY_MATCHES_ALL) {

            //Load match data into a string to be displayed in the main activity
            //Set the string header depending on the week number that was queried

            int weekNumber = queryType;

            String scoreWeekNumberHeader;

            scoreWeekNumberHeader = "Week " + weekNumber;
            if (queryType == MatchEntry.MATCH_WEEK_WILDCARD) {
                scoreWeekNumberHeader = "Wildcard Playoffs";
            }
            if (queryType == MatchEntry.MATCH_WEEK_DIVISIONAL) {
                scoreWeekNumberHeader = "Divisional Playoffs";
            }
            if (queryType == MatchEntry.MATCH_WEEK_CHAMPIONSHIP) {
                scoreWeekNumberHeader = "Conference Championships";
            }
            if (queryType == MatchEntry.MATCH_WEEK_SUPERBOWL) {
                scoreWeekNumberHeader = "Superbowl";
            }

            matchesCursor.moveToPosition(0);

            Log.d("Scores", "displayScoresCalled");
            this.view.onDisplayScores(queryType, matchesCursor, scoreWeekNumberHeader, matchesPlayed);


        } else {

            //This else statement is hit if ALL matches are queried
            //Initialize all schedule, weeks, and matches.  Add weeks to schedule.

            Schedule seasonSchedule = new Schedule();
            Week weekOne = new Week(1);
            Week weekTwo = new Week(2);
            Week weekThree = new Week(3);
            Week weekFour = new Week(4);
            Week weekFive = new Week(5);
            Week weekSix = new Week(6);
            Week weekSeven = new Week(7);
            Week weekEight = new Week(8);
            Week weekNine = new Week(9);
            Week weekTen = new Week(10);
            Week weekEleven = new Week(11);
            Week weekTwelve = new Week(12);
            Week weekThirteen = new Week(13);
            Week weekFourteen = new Week(14);
            Week weekFifteen = new Week(15);
            Week weekSixteen = new Week(16);
            Week weekSeventeen = new Week(17);
            Week wildCard = new Week(MatchEntry.MATCH_WEEK_WILDCARD);
            Week divisional = new Week(MatchEntry.MATCH_WEEK_DIVISIONAL);
            Week championship = new Week(MatchEntry.MATCH_WEEK_CHAMPIONSHIP);
            Week superbowl = new Week(MatchEntry.MATCH_WEEK_SUPERBOWL);


            //Get matches from database cursor and add them to the schedule
            matchesCursor.moveToPosition(-1);
            while (matchesCursor.moveToNext()) {

                String teamOne = matchesCursor.getString(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_ONE));
                String teamTwo = matchesCursor.getString(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_TWO));
                int teamOneWon = matchesCursor.getInt(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_ONE_WON));
                int matchWeek = matchesCursor.getInt(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_WEEK));
                int ID = matchesCursor.getInt(matchesCursor.getColumnIndexOrThrow(MatchEntry._ID));
                Uri matchUri = ContentUris.withAppendedId(MatchEntry.CONTENT_URI, ID);

                HashMap<String, Team> teamList = mModel.getTeamList();

                if (teamList != null) {

                    switch (matchWeek) {

                        case 1:
                            weekOne.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 1, this, matchUri));
                            break;
                        case 2:
                            weekTwo.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 2, this, matchUri));
                            break;
                        case 3:
                            weekThree.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 3, this, matchUri));
                            break;
                        case 4:
                            weekFour.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 4, this, matchUri));
                            break;
                        case 5:
                            weekFive.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 5, this, matchUri));
                            break;
                        case 6:
                            weekSix.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 6, this, matchUri));
                            break;
                        case 7:
                            weekSeven.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 7, this, matchUri));
                            break;
                        case 8:
                            weekEight.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 8, this, matchUri));
                            break;
                        case 9:
                            weekNine.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 9, this, matchUri));
                            break;
                        case 10:
                            weekTen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 10, this, matchUri));
                            break;
                        case 11:
                            weekEleven.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 11, this, matchUri));
                            break;
                        case 12:
                            weekTwelve.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 12, this, matchUri));
                            break;
                        case 13:
                            weekThirteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 13, this, matchUri));
                            break;
                        case 14:
                            weekFourteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 14, this, matchUri));
                            break;
                        case 15:
                            weekFifteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 15, this, matchUri));
                            break;
                        case 16:
                            weekSixteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 16, this, matchUri));
                            break;
                        case 17:
                            weekSeventeen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 17, this, matchUri));
                            break;
                        case 18:
                            wildCard.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 18, this, matchUri));
                            break;
                        case 19:
                            divisional.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 19, this, matchUri));
                            break;
                        case 20:
                            championship.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 20, this, matchUri));
                            break;
                        case 21:
                            superbowl.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 21, this, matchUri));
                            break;

                    }
                }

            }

            matchesCursor.close();

            seasonSchedule.addWeek(weekOne);
            seasonSchedule.addWeek(weekTwo);
            seasonSchedule.addWeek(weekThree);
            seasonSchedule.addWeek(weekFour);
            seasonSchedule.addWeek(weekFive);
            seasonSchedule.addWeek(weekSix);
            seasonSchedule.addWeek(weekSeven);
            seasonSchedule.addWeek(weekEight);
            seasonSchedule.addWeek(weekNine);
            seasonSchedule.addWeek(weekTen);
            seasonSchedule.addWeek(weekEleven);
            seasonSchedule.addWeek(weekTwelve);
            seasonSchedule.addWeek(weekThirteen);
            seasonSchedule.addWeek(weekFourteen);
            seasonSchedule.addWeek(weekFifteen);
            seasonSchedule.addWeek(weekSixteen);
            seasonSchedule.addWeek(weekSeventeen);

            if (mCurrentWeek >= 18 && mPlayoffsStarted) {
                seasonSchedule.addWeek(wildCard);
            }
            if (mCurrentWeek >= 19 && mPlayoffsStarted) {
                seasonSchedule.addWeek(divisional);
            }
            if (mCurrentWeek >= 20 && mPlayoffsStarted) {
                seasonSchedule.addWeek(championship);
            }
            if (mCurrentWeek >= 21 && mPlayoffsStarted) {
                seasonSchedule.addWeek(superbowl);
            }


            mModel.setSchedule(seasonSchedule);

            if (mPlayoffsStarted) {
                //If the playoffs have already started, re-query the playoff standings  after all matches are created
                mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_LOAD_POSTSEASON);
            }
        }

    }

    @Override
    public void teamsOrStandingsQueried(int queryType, Cursor standingsCursor) {

        Log.d("Standings", "QUERIED");
        //This callback will be received from the model whenever teams/standings are queried
        //Depending on the queryType, a specific action is performed

        if (queryType == SimulatorModel.QUERY_STANDINGS_REGULAR) {
            //A regular standings was queried
            //This regular standings will be evaluated to determine team playoff eligibility
            if (mModel.getTeamList() != null) {
                Standings.generatePlayoffTeams(standingsCursor, mModel.getTeamList());
            }
            //Teams playoff eligibility has been updated so re-query the standings
            mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_PLAYOFF);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_PLAYOFF) {
            //A playoff standings was queried
            //Display playoff standings in UI
            Log.d("Standings", "displayStandingsCalled");
            displayStandings(standingsCursor);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_LOAD_SEASON) {
            //The entire season was loaded from the db
            //Create teams from the db data and then query all matches from the db
            createTeamsFromDb(standingsCursor);
            mModel.queryMatches(SimulatorModel.QUERY_MATCHES_ALL, false, true);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_POSTSEASON) {
            //The postseason standings were queried
            //Create playoff matchups based on the playoff standings/round and then display the
            //standings in the Main Activity UI
            createPlayoffMatchups(standingsCursor);
            displayPlayoffStandings(standingsCursor);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_LOAD_POSTSEASON) {
            //The app was restarted and the postseason standings need to be re-loaded from the database
            //Display the standings in the MainActivity UI and query the playoff matches
            displayPlayoffStandings(standingsCursor);
            mModel.queryMatches(mCurrentWeek, true, false);
        }

    }

    @Override
    public void resetSeason() {
        //When season is reset, delete all data in the database
        mModel.deleteAllData();
    }

    @Override
    public void resetTeamElos() {
        //Reset teams Elos to last seasons Elo Values
        ArrayList<Team> teamList = mModel.getTeamArrayList();
        for (Team team : teamList) {
            team.resetElo();
        }
    }

    @Override
    public void resetTeamFutureElos() {
        //Reset teams Elos to future Elo Values
        ArrayList<Team> teamList = mModel.getTeamArrayList();
        for (Team team : teamList) {
            team.setFutureElos();
        }

    }

    @Override
    public void resetTeamUserElos() {
        //Reset team Elo values to be user defined values
        HashMap<String, Team> teamMap = mModel.getTeamList();
        HashMap<String, Double> teamElos = mModel.getTeamEloMap();

        for (String teamName : teamMap.keySet()) {
            teamMap.get(teamName).setElo(teamElos.get(teamName));
            teamMap.get(teamName).setUserElo();
        }


    }

    @Override
    public void setTeamUserElos() {
        //Set team User Elos to be whatever elo values were manually set by the user
        //Provide the data to the model in the form of a hashmap (this will be used to reset the values
        // back to default when the season is reset)

        HashMap<String, Double> teamUserElos = new HashMap<>();
        ArrayList<Team> teamList = mModel.getTeamArrayList();

        for (Team team : teamList) {
            team.setUserElo();
            teamUserElos.put(team.getName(), team.getUserElo());
        }

        mModel.setTeamEloMap(teamUserElos);

    }

    @Override
    public void dataDeleted() {
        //Callback after data has been deleted
        this.view.onDataDeleted();
    }

    private void createTeamsFromDb(Cursor standingsCursor) {

        //Create team objects from team database data

        HashMap<String, Team> teamList = new HashMap();
        HashMap<String, Double> teamUserElos = new HashMap();

        standingsCursor.moveToPosition(-1);


        while (standingsCursor.moveToNext()) {

            int ID = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry._ID));
            String teamName = standingsCursor.getString(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_NAME));
            String teamShortName = standingsCursor.getString(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_SHORT_NAME));
            int teamWins = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_CURRENT_WINS));
            int teamLosses = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_CURRENT_LOSSES));
            double winLossPct = standingsCursor.getDouble(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_WIN_LOSS_PCT));
            int divisionWins = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_DIV_WINS));
            int divisionLosses = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_DIV_LOSSES));
            double divWinLossPct = standingsCursor.getDouble(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_DIV_WIN_LOSS_PCT));
            Double teamElo = standingsCursor.getDouble(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_ELO));
            Double teamDefaultElo = standingsCursor.getDouble(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_DEFAULT_ELO));
            Double teamUserElo = standingsCursor.getDouble(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_USER_ELO));
            Double teamRanking = standingsCursor.getDouble(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_RANKING));
            Double offRating = standingsCursor.getDouble(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_OFF_RATING));
            Double defRating = standingsCursor.getDouble(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_DEF_RATING));
            int division = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_DIVISION));
            int playoffEligible = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_PLAYOFF_ELIGIBILE));
            Uri teamUri = ContentUris.withAppendedId(TeamEntry.CONTENT_URI, ID);


            teamList.put(teamName,
                    new Team(teamName, teamShortName, teamElo, teamDefaultElo, teamUserElo, teamRanking,
                            offRating, defRating, division, this, teamWins, teamLosses, divisionWins, divisionLosses, winLossPct, divWinLossPct, playoffEligible, teamUri));

            teamUserElos.put(teamName, teamUserElo);

        }

        mModel.setTeamList(teamList);
        mModel.setTeamEloMap(teamUserElos);
        mModel.createTeamLogoMap();


        standingsCursor.close();

    }

    @Override
    public void destroyPresenter() {
        mModel.destroyModel();
    }


    private void createTeams() {

        //Create teams for the first time

        HashMap<String, Team> teamList = new HashMap();
        teamList.put(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING,
                new Team(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING, NFLConstants.TEAM_ARIZONA_CARDINALS_SHORT_STRING, NFLConstants.TEAM_ARIZONA_CARDINALS_ELO, NFLConstants.TEAM_ARIZONA_CARDINALS_FUTURE_RANKING,
                        NFLConstants.TEAM_ARIZONA_CARDINALS_OFFRAT, NFLConstants.TEAM_ARIZONA_CARDINALS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        teamList.put(NFLConstants.TEAM_ATLANTA_FALCONS_STRING,
                new Team(NFLConstants.TEAM_ATLANTA_FALCONS_STRING, NFLConstants.TEAM_ATLANTA_FALCONS_SHORT_STRING, NFLConstants.TEAM_ATLANTA_FALCONS_ELO, NFLConstants.TEAM_ATLANTA_FALCONS_FUTURE_RANKING,
                        NFLConstants.TEAM_ATLANTA_FALCONS_OFFRAT, NFLConstants.TEAM_ATLANTA_FALCONS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        teamList.put(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING,
                new Team(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING, NFLConstants.TEAM_BALTIMORE_RAVENS_SHORT_STRING, NFLConstants.TEAM_BALTIMORE_RAVENS_ELO, NFLConstants.TEAM_BALTIMORE_RAVENS_FUTURE_RANKING,
                        NFLConstants.TEAM_BALTIMORE_RAVENS_OFFRAT, NFLConstants.TEAM_BALTIMORE_RAVENS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        teamList.put(NFLConstants.TEAM_BUFFALO_BILLS_STRING,
                new Team(NFLConstants.TEAM_BUFFALO_BILLS_STRING, NFLConstants.TEAM_BUFFALO_BILLS_SHORT_STRING, NFLConstants.TEAM_BUFFALO_BILLS_ELO, NFLConstants.TEAM_BUFFALO_BILLS_FUTURE_RANKING,
                        NFLConstants.TEAM_BUFFALO_BILLS_OFFRAT, NFLConstants.TEAM_BUFFALO_BILLS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        teamList.put(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING,
                new Team(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING, NFLConstants.TEAM_CAROLINA_PANTHERS_SHORT_STRING, NFLConstants.TEAM_CAROLINA_PANTHERS_ELO, NFLConstants.TEAM_CAROLINA_PANTHERS_FUTURE_RANKING,
                        NFLConstants.TEAM_CAROLINA_PANTHERS_OFFRAT, NFLConstants.TEAM_CAROLINA_PANTHERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        teamList.put(NFLConstants.TEAM_CHICAGO_BEARS_STRING,
                new Team(NFLConstants.TEAM_CHICAGO_BEARS_STRING, NFLConstants.TEAM_CHICAGO_BEARS_SHORT_STRING, NFLConstants.TEAM_CHICAGO_BEARS_ELO, NFLConstants.TEAM_CHICAGO_BEARS_FUTURE_RANKING,
                        NFLConstants.TEAM_CHICAGO_BEARS_OFFRAT, NFLConstants.TEAM_CHICAGO_BEARS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        teamList.put(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING,
                new Team(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING, NFLConstants.TEAM_CINCINNATI_BENGALS_SHORT_STRING, NFLConstants.TEAM_CINCINNATI_BENGALS_ELO, NFLConstants.TEAM_CINCINNATI_BENGALS_FUTURE_RANKING,
                        NFLConstants.TEAM_CINCINNATI_BENGALS_OFFRAT, NFLConstants.TEAM_CINCINNATI_BENGALS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        teamList.put(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING,
                new Team(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING, NFLConstants.TEAM_CLEVELAND_BROWNS_SHORT_STRING, NFLConstants.TEAM_CLEVELAND_BROWNS_ELO, NFLConstants.TEAM_CLEVELAND_BROWNS_FUTURE_RANKING,
                        NFLConstants.TEAM_CLEVELAND_BROWNS_OFFRAT, NFLConstants.TEAM_CLEVELAND_BROWNS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        teamList.put(NFLConstants.TEAM_DALLAS_COWBOYS_STRING,
                new Team(NFLConstants.TEAM_DALLAS_COWBOYS_STRING, NFLConstants.TEAM_DALLAS_COWBOYS_SHORT_STRING, NFLConstants.TEAM_DALLAS_COWBOYS_ELO, NFLConstants.TEAM_DALLAS_COWBOYS_FUTURE_RANKING,
                        NFLConstants.TEAM_DALLAS_COWBOYS_OFFRAT, NFLConstants.TEAM_DALLAS_COWBOYS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        teamList.put(NFLConstants.TEAM_DENVER_BRONCOS_STRING,
                new Team(NFLConstants.TEAM_DENVER_BRONCOS_STRING, NFLConstants.TEAM_DENVER_BRONCOS_SHORT_STRING, NFLConstants.TEAM_DENVER_BRONCOS_ELO, NFLConstants.TEAM_DENVER_BRONCOS_FUTURE_RANKING,
                        NFLConstants.TEAM_DENVER_BRONCOS_OFFRAT, NFLConstants.TEAM_DENVER_BRONCOS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        teamList.put(NFLConstants.TEAM_DETROIT_LIONS_STRING,
                new Team(NFLConstants.TEAM_DETROIT_LIONS_STRING, NFLConstants.TEAM_DETROIT_LIONS_SHORT_STRING, NFLConstants.TEAM_DETROIT_LIONS_ELO, NFLConstants.TEAM_DETROIT_LIONS_FUTURE_RANKING,
                        NFLConstants.TEAM_DETROIT_LIONS_OFFRAT, NFLConstants.TEAM_DETROIT_LIONS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        teamList.put(NFLConstants.TEAM_GREENBAY_PACKERS_STRING,
                new Team(NFLConstants.TEAM_GREENBAY_PACKERS_STRING, NFLConstants.TEAM_GREENBAY_PACKERS_SHORT_STRING, NFLConstants.TEAM_GREENBAY_PACKERS_ELO, NFLConstants.TEAM_GREENBAY_PACKERS_FUTURE_RANKING,
                        NFLConstants.TEAM_GREENBAY_PACKERS_OFFRAT, NFLConstants.TEAM_GREENBAY_PACKERS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        teamList.put(NFLConstants.TEAM_HOUSTON_TEXANS_STRING,
                new Team(NFLConstants.TEAM_HOUSTON_TEXANS_STRING, NFLConstants.TEAM_HOUSTON_TEXANS_SHORT_STRING, NFLConstants.TEAM_HOUSTON_TEXANS_ELO, NFLConstants.TEAM_HOUSTON_TEXANS_FUTURE_RANKING,
                        NFLConstants.TEAM_HOUSTON_TEXANS_OFFRAT, NFLConstants.TEAM_HOUSTON_TEXANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        teamList.put(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING,
                new Team(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING, NFLConstants.TEAM_INDIANAPOLIS_COLTS_SHORT_STRING, NFLConstants.TEAM_INDIANAPOLIS_COLTS_ELO, NFLConstants.TEAM_INDIANAPOLIS_COLTS_FUTURE_RANKING,
                        NFLConstants.TEAM_INDIANAPOLIS_COLTS_OFFRAT, NFLConstants.TEAM_INDIANAPOLIS_COLTS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        teamList.put(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING,
                new Team(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_SHORT_STRING, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_ELO, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_FUTURE_RANKING,
                        NFLConstants.TEAM_JACKSONVILLE_JAGUARS_OFFRAT, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        teamList.put(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING,
                new Team(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING, NFLConstants.TEAM_KANSASCITY_CHIEFS_SHORT_STRING, NFLConstants.TEAM_KANSASCITY_CHIEFS_ELO, NFLConstants.TEAM_KANSASCITY_CHIEFS_FUTURE_RANKING,
                        NFLConstants.TEAM_KANSASCITY_CHIEFS_OFFRAT, NFLConstants.TEAM_KANSASCITY_CHIEFS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        teamList.put(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING,
                new Team(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING, NFLConstants.TEAM_LOSANGELES_CHARGERS_SHORT_STRING, NFLConstants.TEAM_LOSANGELES_CHARGERS_ELO, NFLConstants.TEAM_LOSANGELES_CHARGERS_FUTURE_RANKING,
                        NFLConstants.TEAM_LOSANGELES_CHARGERS_OFFRAT, NFLConstants.TEAM_LOSANGELES_CHARGERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        teamList.put(NFLConstants.TEAM_LOSANGELES_RAMS_STRING,
                new Team(NFLConstants.TEAM_LOSANGELES_RAMS_STRING, NFLConstants.TEAM_LOSANGELES_RAMS_SHORT_STRING, NFLConstants.TEAM_LOSANGELES_RAMS_ELO, NFLConstants.TEAM_LOSANGELES_RAMS_FUTURE_RANKING,
                        NFLConstants.TEAM_LOSANGELES_RAMS_OFFRAT, NFLConstants.TEAM_LOSANGELES_RAMS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        teamList.put(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING,
                new Team(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING, NFLConstants.TEAM_MIAMI_DOLPHINS_SHORT_STRING, NFLConstants.TEAM_MIAMI_DOLPHINS_ELO, NFLConstants.TEAM_MIAMI_DOLPHINS_FUTURE_RANKING,
                        NFLConstants.TEAM_MIAMI_DOLPHINS_OFFRAT, NFLConstants.TEAM_MIAMI_DOLPHINS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        teamList.put(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING,
                new Team(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING, NFLConstants.TEAM_MINNESOTA_VIKINGS_SHORT_STRING, NFLConstants.TEAM_MINNESOTA_VIKINGS_ELO, NFLConstants.TEAM_MINNESOTA_VIKINGS_FUTURE_RANKING,
                        NFLConstants.TEAM_MINNESOTA_VIKINGS_OFFRAT, NFLConstants.TEAM_MINNESOTA_VIKINGS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        teamList.put(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING,
                new Team(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_SHORT_STRING, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_ELO, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWENGLAND_PATRIOTS_OFFRAT, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        teamList.put(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING,
                new Team(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING, NFLConstants.TEAM_NEWORLEANS_SAINTS_SHORT_STRING, NFLConstants.TEAM_NEWORLEANS_SAINTS_ELO, NFLConstants.TEAM_NEWORLEANS_SAINTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWORLEANS_SAINTS_OFFRAT, NFLConstants.TEAM_NEWORLEANS_SAINTS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        teamList.put(NFLConstants.TEAM_NEWYORK_GIANTS_STRING,
                new Team(NFLConstants.TEAM_NEWYORK_GIANTS_STRING, NFLConstants.TEAM_NEWYORK_GIANTS_SHORT_STRING, NFLConstants.TEAM_NEWYORK_GIANTS_ELO, NFLConstants.TEAM_NEWYORK_GIANTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWYORK_GIANTS_OFFRAT, NFLConstants.TEAM_NEWYORK_GIANTS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        teamList.put(NFLConstants.TEAM_NEWYORK_JETS_STRING,
                new Team(NFLConstants.TEAM_NEWYORK_JETS_STRING, NFLConstants.TEAM_NEWYORK_JETS_SHORT_STRING, NFLConstants.TEAM_NEWYORK_JETS_ELO, NFLConstants.TEAM_NEWYORK_JETS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWYORK_JETS_OFFRAT, NFLConstants.TEAM_NEWYORK_JETS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        teamList.put(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING,
                new Team(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING, NFLConstants.TEAM_OAKLAND_RAIDERS_SHORT_STRING, NFLConstants.TEAM_OAKLAND_RAIDERS_ELO, NFLConstants.TEAM_OAKLAND_RAIDERS_FUTURE_RANKING,
                        NFLConstants.TEAM_OAKLAND_RAIDERS_OFFRAT, NFLConstants.TEAM_OAKLAND_RAIDERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        teamList.put(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING,
                new Team(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING, NFLConstants.TEAM_PHILADELPHIA_EAGLES_SHORT_STRING, NFLConstants.TEAM_PHILADELPHIA_EAGLES_ELO, NFLConstants.TEAM_PHILADELPHIA_EAGLES_FUTURE_RANKING,
                        NFLConstants.TEAM_PHILADELPHIA_EAGLES_OFFRAT, NFLConstants.TEAM_PHILADELPHIA_EAGLES_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        teamList.put(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING,
                new Team(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING, NFLConstants.TEAM_PITTSBURGH_STEELERS_SHORT_STRING, NFLConstants.TEAM_PITTSBURGH_STEELERS_ELO, NFLConstants.TEAM_PITTSBURGH_STEELERS_FUTURE_RANKING,
                        NFLConstants.TEAM_PITTSBURGH_STEELERS_OFFRAT, NFLConstants.TEAM_PITTSBURGH_STEELERS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        teamList.put(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING,
                new Team(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING, NFLConstants.TEAM_SANFRANCISCO_49ERS_SHORT_STRING, NFLConstants.TEAM_SANFRANCISCO_49ERS_ELO, NFLConstants.TEAM_SANFRANCISCO_49ERS_FUTURE_RANKING,
                        NFLConstants.TEAM_SANFRANCISCO_49ERS_OFFRAT, NFLConstants.TEAM_SANFRANCISCO_49ERS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        teamList.put(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING,
                new Team(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING, NFLConstants.TEAM_SEATTLE_SEAHAWKS_SHORT_STRING, NFLConstants.TEAM_SEATTLE_SEAHAWKS_ELO, NFLConstants.TEAM_SEATTLE_SEAHAWKS_FUTURE_RANKING,
                        NFLConstants.TEAM_SEATTLE_SEAHAWKS_OFFRAT, NFLConstants.TEAM_SEATTLE_SEAHAWKS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        teamList.put(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING,
                new Team(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_SHORT_STRING, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_ELO, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_FUTURE_RANKING,
                        NFLConstants.TEAM_TAMPABAY_BUCCANEERS_OFFRAT, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        teamList.put(NFLConstants.TEAM_TENNESSEE_TITANS_STRING,
                new Team(NFLConstants.TEAM_TENNESSEE_TITANS_STRING, NFLConstants.TEAM_TENNESSEE_TITANS_SHORT_STRING, NFLConstants.TEAM_TENNESSEE_TITANS_ELO, NFLConstants.TEAM_TENNESSEE_TITANS_FUTURE_RANKING,
                        NFLConstants.TEAM_TENNESSEE_TITANS_OFFRAT, NFLConstants.TEAM_TENNESSEE_TITANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        teamList.put(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING,
                new Team(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING, NFLConstants.TEAM_WASHINGTON_REDSKINS_SHORT_STRING, NFLConstants.TEAM_WASHINGTON_REDSKINS_ELO, NFLConstants.TEAM_WASHINGTON_REDSKINS_FUTURE_RANKING,
                        NFLConstants.TEAM_WASHINGTON_REDSKINS_OFFRAT, NFLConstants.TEAM_WASHINGTON_REDSKINS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        mModel.setTeamList(teamList);

        mModel.createTeamLogoMap();
    }


    private void createSchedule() {

        //Initialize all schedule, weeks, and matches.  Add weeks to schedule.
        Schedule seasonSchedule = new Schedule();
        Week weekOne = new Week(1);
        Week weekTwo = new Week(2);
        Week weekThree = new Week(3);
        Week weekFour = new Week(4);
        Week weekFive = new Week(5);
        Week weekSix = new Week(6);
        Week weekSeven = new Week(7);
        Week weekEight = new Week(8);
        Week weekNine = new Week(9);
        Week weekTen = new Week(10);
        Week weekEleven = new Week(11);
        Week weekTwelve = new Week(12);
        Week weekThirteen = new Week(13);
        Week weekFourteen = new Week(14);
        Week weekFifteen = new Week(15);
        Week weekSixteen = new Week(16);
        Week weekSeventeen = new Week(17);
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 1, this));
        weekOne.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 1, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 2, this));
        weekTwo.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 2, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 3, this));
        weekThree.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 3, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 4, this));
        weekFour.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 4, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 5, this));
        weekFive.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 5, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 6, this));
        weekSix.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 6, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 7, this));
        weekSeven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 7, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 8, this));
        weekEight.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 8, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 9, this));
        weekNine.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 9, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 10, this));
        weekTen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 10, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 11, this));
        weekEleven.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 11, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 12, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 13, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 14, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 15, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 16, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mModel.getTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 17, this));
        seasonSchedule.addWeek(weekOne);
        seasonSchedule.addWeek(weekTwo);
        seasonSchedule.addWeek(weekThree);
        seasonSchedule.addWeek(weekFour);
        seasonSchedule.addWeek(weekFive);
        seasonSchedule.addWeek(weekSix);
        seasonSchedule.addWeek(weekSeven);
        seasonSchedule.addWeek(weekEight);
        seasonSchedule.addWeek(weekNine);
        seasonSchedule.addWeek(weekTen);
        seasonSchedule.addWeek(weekEleven);
        seasonSchedule.addWeek(weekTwelve);
        seasonSchedule.addWeek(weekThirteen);
        seasonSchedule.addWeek(weekFourteen);
        seasonSchedule.addWeek(weekFifteen);
        seasonSchedule.addWeek(weekSixteen);
        seasonSchedule.addWeek(weekSeventeen);

        mModel.setSchedule(seasonSchedule);


    }

    @Override
    public void simulateSeason() {
        //From week 1 to week 17 (full season), simulate the season
        while (mCurrentWeek <= 17) {
            mModel.getSchedule().getWeek(mCurrentWeek).simulate();
            mCurrentWeek++;
        }

        //After the season  is complete, query the standings (and display them)
        mModel.queryStandings(SimulatorModel.QUERY_STANDINGS_REGULAR);
        //Query all weeks that have already occurred;
        mModel.queryMatches(mCurrentWeek - 1, false, true);
    }

    private void displayStandings(Cursor standingsCursor) {
        Log.d("DISPLAY", "STANDINGS");
        this.view.onDisplayStandings(MainActivity.STANDINGS_TYPE_REGULAR_SEASON, standingsCursor);
    }

    private void displayPlayoffStandings(Cursor standingsCursor) {

        //Display the AFC and NFC playoff teams with their playoff seeds
        this.view.onDisplayStandings(MainActivity.STANDINGS_TYPE_PLAYOFFS, standingsCursor);

    }


    @Override
    public void updateMatchCallback(Match match, Uri uri) {

        //Callback is received when a match is completed
        //The model is then notified to update the match in the database
        mModel.updateMatch(match, uri);
    }

    @Override
    public void updateTeamCallback(Team team, Uri uri) {

        //Callback is received when a match is completed
        //The model is then notified to update the team wins, losses and winLossPct.
        mModel.updateTeam(team, uri);
    }

    public static boolean seasonIsInitialized() {
        return mSeasonInitialized;
    }

    public static void setSeasonInitialized(Boolean seasonIsInitialized) {
        mSeasonInitialized = seasonIsInitialized;
    }

    public static void setCurrentWeek(int currentWeek) {
        mCurrentWeek = currentWeek;
    }

    public static int getCurrentWeek() {
        return mCurrentWeek;
    }

    public void createPlayoffMatchups(Cursor standingsCursor) {

        //If standings cursor count is 12, the playoffs are just starting because there are still 12 teams
        //Therefore, initialize the playoffs schedule and set the wildcard matchups

        //If the standings cursor count is 8, initialize divisional playoffs.
        //If the standings cursor count is 4, initialize conference playoffs.
        //If the standings cursor count is 2, initialize superbowl.

        int remainingPlayoffTeams = standingsCursor.getCount();

        ArrayList<Team> afcTeams = new ArrayList<>();
        ArrayList<Team> nfcTeams = new ArrayList<>();

        //Go through the cursor and add the teams to their respective conference in order of their seed (cursor is sorted by seed)
        standingsCursor.moveToPosition(-1);
        while (standingsCursor.moveToNext()) {
            int teamConference = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_CONFERENCE));
            int teamPlayoffSeed = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_PLAYOFF_ELIGIBILE));
            String teamName = standingsCursor.getString(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_NAME));
            Team team = mModel.getTeamList().get(teamName);
            if (team.getConference() == TeamEntry.CONFERENCE_AFC) {
                afcTeams.add(team);
            } else {
                nfcTeams.add(team);
            }
        }

        if (remainingPlayoffTeams == 12) {

            //Initialize all playoffs schedule
            Week wildCard = new Week(MatchEntry.MATCH_WEEK_WILDCARD);

            //Initialize wildcard matchups from cursor
            // Seed 3 plays 6, 5 plays 4 for both conferences
            wildCard.addMatch(new Match(afcTeams.get(2), afcTeams.get(5), MatchEntry.MATCH_WEEK_WILDCARD, this));
            wildCard.addMatch(new Match(afcTeams.get(3), afcTeams.get(4), MatchEntry.MATCH_WEEK_WILDCARD, this));
            wildCard.addMatch(new Match(nfcTeams.get(2), nfcTeams.get(5), MatchEntry.MATCH_WEEK_WILDCARD, this));
            wildCard.addMatch(new Match(nfcTeams.get(3), nfcTeams.get(4), MatchEntry.MATCH_WEEK_WILDCARD, this));

            //Add the week to the schedule and insert the matches in the database
            mModel.getSchedule().addWeek(wildCard);
            mModel.insertMatches(SimulatorModel.INSERT_MATCHES_PLAYOFFS_WILDCARD, wildCard);
        }

        if (remainingPlayoffTeams == 8) {

            //Query the wildcard match scores
            mModel.queryMatches(MatchEntry.MATCH_WEEK_WILDCARD, true, true);

            Week divisional = new Week(MatchEntry.MATCH_WEEK_DIVISIONAL);

            //Initialize divisional matchups from cursor
            // Highest remaining seed plays lowest and the two middle seeds play
            divisional.addMatch(new Match(afcTeams.get(0), afcTeams.get(3), MatchEntry.MATCH_WEEK_DIVISIONAL, this));
            divisional.addMatch(new Match(afcTeams.get(1), afcTeams.get(2), MatchEntry.MATCH_WEEK_DIVISIONAL, this));
            divisional.addMatch(new Match(nfcTeams.get(0), nfcTeams.get(3), MatchEntry.MATCH_WEEK_DIVISIONAL, this));
            divisional.addMatch(new Match(nfcTeams.get(1), nfcTeams.get(2), MatchEntry.MATCH_WEEK_DIVISIONAL, this));

            //Add the week to the schedule and  insert the matches in the database
            mModel.getSchedule().addWeek(divisional);
            mModel.insertMatches(SimulatorModel.INSERT_MATCHES_PLAYOFFS_DIVISIONAL, divisional);


        }
        if (remainingPlayoffTeams == 4) {

            //Query the divisional match scores
            mModel.queryMatches(MatchEntry.MATCH_WEEK_DIVISIONAL, true, true);

            Week championship = new Week(MatchEntry.MATCH_WEEK_CHAMPIONSHIP);

            //Initialize conference championship matchups from cursor
            // Highest remaining seed plays lowest
            championship.addMatch(new Match(afcTeams.get(0), afcTeams.get(1), MatchEntry.MATCH_WEEK_CHAMPIONSHIP, this));
            championship.addMatch(new Match(nfcTeams.get(0), nfcTeams.get(1), MatchEntry.MATCH_WEEK_CHAMPIONSHIP, this));

            //Add the week to the schedule and insert the matches in the database
            mModel.getSchedule().addWeek(championship);
            mModel.insertMatches(SimulatorModel.INSERT_MATCHES_PLAYOFFS_CHAMPIONSHIP, championship);

        }
        if (remainingPlayoffTeams == 2) {

            //Query the championship match scores
            mModel.queryMatches(MatchEntry.MATCH_WEEK_CHAMPIONSHIP, true, true);

            Week superbowl = new Week(MatchEntry.MATCH_WEEK_SUPERBOWL);

            //Initialize superbowl
            // Highest remaining seed plays lowest
            superbowl.addMatch(new Match(afcTeams.get(0), nfcTeams.get(0), MatchEntry.MATCH_WEEK_SUPERBOWL, this));

            //Add the week to the schedule and insert the matches in the database
            mModel.getSchedule().addWeek(superbowl);
            mModel.insertMatches(SimulatorModel.INSERT_MATCHES_PLAYOFFS_SUPERBOWL, superbowl);

        }

        if (remainingPlayoffTeams == 1) {

            //Query the superbowl match scores
            mModel.queryMatches(MatchEntry.MATCH_WEEK_SUPERBOWL, true, true);

        }


    }

    @Override
    public boolean getPlayoffsStarted() {
        return mPlayoffsStarted;
    }

    @Override
    public void setPlayoffsStarted(boolean playoffsStarted) {
        mPlayoffsStarted = playoffsStarted;
    }

    public void setCurrentWeekPreference(int currentWeek) {
        //Set current week preference when week is updated
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(mContext.getString(R.string.settings_week_num_key), currentWeek).apply();
        prefs.commit();
    }

    private void setSeasonInitializedPreference(boolean seasonInitialized) {
        //Set season initialized boolean preference
        SimulatorPresenter.setSeasonInitialized(seasonInitialized);
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putBoolean(mContext.getString(R.string.settings_season_initialized_key), seasonInitialized);
        prefs.commit();
    }

    private void setSeasonLoadedPreference(Boolean seasonLoaded){
        //Set the season loaded preference boolean
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putBoolean(mContext.getString(R.string.settings_season_loaded_key), seasonLoaded).apply();
        prefs.commit();
    }

    private void setEloType() {
        Integer eloType = mSharedPreferences.getInt(mContext.getString(R.string.settings_elo_type_key), mContext.getResources().getInteger(R.integer.settings_elo_type_default));
        if (eloType == mContext.getResources().getInteger(R.integer.settings_elo_type_future)) {
            resetTeamFutureElos();
        }
        if (eloType == mContext.getResources().getInteger(R.integer.settings_elo_type_user)) {
            resetTeamUserElos();
        }
    }


}
