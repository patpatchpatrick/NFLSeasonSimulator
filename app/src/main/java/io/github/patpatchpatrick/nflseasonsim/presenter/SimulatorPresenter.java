package io.github.patpatchpatrick.nflseasonsim.presenter;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.SimulatorActivity;
import io.github.patpatchpatrick.nflseasonsim.R;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.BaseView;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.ScoreView;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Data;
import io.github.patpatchpatrick.nflseasonsim.season_resources.ELORatingSystem;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.NFLConstants;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Standings;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Week;

public class SimulatorPresenter extends BasePresenter<SimulatorMvpContract.SimulatorView>
        implements SimulatorMvpContract.SimulatorPresenter, Data {

    //Simulator presenter class is used to communicate between the SimulatorActivity (view) and the Model (MVP Architecture)

    @Inject
    SimulatorModel mModel;

    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    Context mContext;

    @Inject
    BaseView mHomeScreenBaseView;

    @Inject
    ArrayList<BaseView> mBaseViews;

    @Inject
    ArrayList<ScoreView> mScoreViews;

    private static int mCurrentSimulatorWeek;
    private static int mCurrentSeasonWeek;
    private static Boolean mSeasonInitialized = false;
    private static Boolean mSimulatorPlayoffsStarted = false;
    private static Boolean mCurrentSeasonPlayoffsStarted = false;
    public static Boolean mCurrentSeasonMatchesLoaded = false;
    public static Boolean mTestSimulation = false;
    public static int mTotalTestSimulations;
    public static int mCurrentTestSimulations;
    private static final int SEASON_TYPE_CURRENT = 1;
    private static final int SEASON_TYPE_SIMULATOR = 2;

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
        Week currentWeek = mModel.getSimulatorSchedule().getWeek(mCurrentSimulatorWeek);
        currentWeek.simulate(true);

        ArrayList<Match> currentWeekMatches = currentWeek.getMatches();

        Log.d("PresenterCurrentWeek", "" + mCurrentSimulatorWeek);
        Log.d("Current Week Matches", "" + currentWeekMatches.size());
        Log.d("Num Matches Updated", "" + currentWeek.getNumberMatchesUpdated());

        //After the week is complete, generate the playoff teams and query the standings (and display them)
        generateAndSetPlayoffSeeds(SimulatorPresenter.SEASON_TYPE_SIMULATOR);
        mModel.querySimulatorStandings(SimulatorModel.QUERY_STANDINGS_PLAYOFF);

        Log.d("Post Stand Query Num", "" + currentWeek.getNumberMatchesUpdated());
        //Query the week scores and display them
        mModel.querySimulatorMatches(mCurrentSimulatorWeek, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        //Week is complete so increment the current week value
        mCurrentSimulatorWeek++;
        this.view.setCurrentWeekPreference(mCurrentSimulatorWeek);
    }

    @Override
    public void simulatePlayoffWeek() {
        //Simulate a single playoff week
        //If you are simulating the superbowl, don't use home field advantage in the simulation
        //Otherwise, include home field advantage in the simulation
        if (mCurrentSimulatorWeek == MatchEntry.MATCH_WEEK_SUPERBOWL) {
            mModel.getSimulatorSchedule().getWeek(mCurrentSimulatorWeek).simulate(false);
        } else {
            mModel.getSimulatorSchedule().getWeek(mCurrentSimulatorWeek).simulate(true);
        }
        //After the week is complete, query the standings (and display them)
        mModel.querySimulatorStandings(SimulatorModel.QUERY_STANDINGS_POSTSEASON);
        //Week is complete so increment the current week value
        mCurrentSimulatorWeek++;
        this.view.setCurrentWeekPreference(mCurrentSimulatorWeek);
    }

    private void generateAndSetPlayoffSeeds(int seasonType) {

        //Generate the playoff teams and set the playoff seeds

        ArrayList<ArrayList<Team>> allPlayoffTeams;
        if (seasonType == SimulatorPresenter.SEASON_TYPE_CURRENT) {
            allPlayoffTeams = generateSimulatorPlayoffTeams(SimulatorPresenter.SEASON_TYPE_CURRENT);
        } else {
            allPlayoffTeams = generateSimulatorPlayoffTeams(SimulatorPresenter.SEASON_TYPE_SIMULATOR);
        }
        ArrayList<Team> afcPlayoffTeams = allPlayoffTeams.get(0);
        ArrayList<Team> nfcPlayoffTeams = allPlayoffTeams.get(1);
        ArrayList<Team> seasonTeams;

        if (seasonType == SimulatorPresenter.SEASON_TYPE_CURRENT) {
            seasonTeams = mModel.getSeasonTeamArrayList();
        } else {
            seasonTeams = mModel.getSimulatorTeamArrayList();
        }

        for (Team team : seasonTeams) {
            team.setPlayoffEligible(0);
        }
        int i = 0;
        while (i < 6) {
            afcPlayoffTeams.get(i).setPlayoffEligible(i + 1);
            i++;
        }
        i = 0;
        while (i < 6) {
            nfcPlayoffTeams.get(i).setPlayoffEligible(i + 1);
            i++;
        }

    }

    @Override
    public void queryCurrentSeasonStandings() {

        //Generate the playoff teams and set the playoff seeds
        generateAndSetPlayoffSeeds(SimulatorPresenter.SEASON_TYPE_CURRENT);

        //Query playoff teams
        mModel.queryCurrentSeasonStandings(SimulatorModel.QUERY_STANDINGS_PLAYOFF);

    }

    @Override
    public void initializeSeason() {
        //Set current week to 1 for both the simulator and current season and create teams for both
        mCurrentSimulatorWeek = 1;
        setCurrentSimulatorWeekPreference(mCurrentSimulatorWeek);
        createSimulatorTeams();

        mCurrentSeasonWeek = 1;
        setCurrentSeasonWeekPreference(mCurrentSeasonWeek);
        createSeasonTeams();

        //Insert teams into database.  After teams are inserted, the simulatorTeamsInserted() callback is
        //received from the model
        mModel.insertSimulatorTeams();
    }

    @Override
    public void initiatePlayoffs() {
        mCurrentSimulatorWeek = 18;
        this.view.setCurrentWeekPreference(mCurrentSimulatorWeek);
        //Initiate the playoffs
        //Query the standings from the playoffs standings and the rest of the playoffs is initiated via the
        // standingsQueried method
        mModel.querySimulatorStandings(SimulatorModel.QUERY_STANDINGS_POSTSEASON);

    }

    @Override
    public void loadSeasonFromDatabase() {
        //Load season from database for both simulator activity and current season (create teams and schedule)
        mModel.querySimulatorStandings(SimulatorModel.QUERY_STANDINGS_LOAD_SEASON);
        mModel.queryCurrentSeasonStandings(SimulatorModel.QUERY_STANDINGS_LOAD_SEASON);

        //Season has been loaded, so set preference to true
        setSeasonLoadedPreference(true);

        mHomeScreenBaseView.onSeasonLoadedFromDb();

        //Notify all baseViews that the season was loaded
        for (BaseView baseView : mBaseViews) {
            baseView.onSeasonLoadedFromDb();
        }


    }

    @Override
    public void loadAlreadySimulatedData() {
        //Generate the playoff seeds and load the standings, as well as the matches that have already been simulated (last week's matches)
        generateAndSetPlayoffSeeds(SimulatorPresenter.SEASON_TYPE_SIMULATOR);
        mModel.querySimulatorStandings(SimulatorModel.QUERY_STANDINGS_PLAYOFF);
        //Query all weeks that have already occurred;
        mModel.querySimulatorMatches(mCurrentSimulatorWeek - 1, false, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
    }

    @Override
    public void loadAlreadySimulatedPlayoffData() {
        //Query playoff data
        mModel.querySimulatorStandings(SimulatorModel.QUERY_STANDINGS_LOAD_POSTSEASON);
    }

    @Override
    public void simulatorTeamsInserted() {
        //After the teams are inserted into the DB, create the season schedule and insert the
        //schedule matches into the DB
        //After the matches are inserted into the DB, the simulatorMatchesInserted() callback is received
        //from the model
        createSimulatorSchedule();

        //Set the teams elo types based on user selected preference
        setSimulatorTeamEloType();

        mModel.insertSimulatorMatches(SimulatorModel.INSERT_MATCHES_SCHEDULE);

    }

    @Override
    public void seasonTeamsInserted() {

        //Set current season teams to use current season elos
        resetCurrentSeasonTeamCurrentSeasonElos();
        createSeasonSchedule();
        mModel.insertSeasonMatches(SimulatorModel.INSERT_MATCHES_SCHEDULE);

    }

    @Override
    public void addBaseView(BaseView baseView) {
        //Add a new baseView to the list of baseViews to notify when items are changed
        mBaseViews.add(baseView);
    }

    @Override
    public void loadCurrentSeasonMatches() {
        //Load all the current  season matches
        //If they are complete, they have already been completed/loaded so no need to complete them again
        Week weekOne = mModel.getSeasonSchedule().getWeek(1);
        ArrayList<Match> weekOneMatches = weekOne.getMatches();
        Week weekTwo = mModel.getSeasonSchedule().getWeek(2);
        ArrayList<Match> weekTwoMatches = weekTwo.getMatches();
        Week weekThree = mModel.getSeasonSchedule().getWeek(3);
        ArrayList<Match> weekThreeMatches = weekThree.getMatches();
        Week weekFour = mModel.getSeasonSchedule().getWeek(4);
        ArrayList<Match> weekFourMatches = weekFour.getMatches();
        Week weekFive = mModel.getSeasonSchedule().getWeek(5);
        ArrayList<Match> weekFiveMatches = weekFive.getMatches();
        Week weekSix = mModel.getSeasonSchedule().getWeek(6);
        ArrayList<Match> weekSixMatches = weekSix.getMatches();
        Week weekSeven = mModel.getSeasonSchedule().getWeek(7);
        ArrayList<Match> weekSevenMatches = weekSeven.getMatches();
        Week weekEight = mModel.getSeasonSchedule().getWeek(8);
        ArrayList<Match> weekEightMatches = weekEight.getMatches();
        Week weekNine = mModel.getSeasonSchedule().getWeek(9);
        ArrayList<Match> weekNineMatches = weekNine.getMatches();
        Week weekTen = mModel.getSeasonSchedule().getWeek(10);
        ArrayList<Match> weekTenMatches = weekTen.getMatches();
        if (!weekOneMatches.get(0).getComplete()) {
            weekOneMatches.get(0).complete(12, 18);
        }
        if (!weekOneMatches.get(1).getComplete()) {
            weekOneMatches.get(1).complete(34, 23);
        }
        if (!weekOneMatches.get(2).getComplete()) {
            weekOneMatches.get(2).complete(3, 47);
        }
        if (!weekOneMatches.get(3).getComplete()) {
            weekOneMatches.get(3).complete(48, 40);
        }
        if (!weekOneMatches.get(4).getComplete()) {
            weekOneMatches.get(4).complete(20, 27);
        }
        if (!weekOneMatches.get(5).getComplete()) {
            weekOneMatches.get(5).complete(16, 24);
        }
        if (!weekOneMatches.get(6).getComplete()) {
            weekOneMatches.get(6).complete(20, 27);
        }
        if (!weekOneMatches.get(7).getComplete()) {
            weekOneMatches.get(7).complete(20, 15);
        }
        if (!weekOneMatches.get(8).getComplete()) {
            weekOneMatches.get(8).complete(21, 21);
        }
        if (!weekOneMatches.get(9).getComplete()) {
            weekOneMatches.get(9).complete(38, 28);
        }
        if (!weekOneMatches.get(10).getComplete()) {
            weekOneMatches.get(10).complete(8, 16);
        }
        if (!weekOneMatches.get(11).getComplete()) {
            weekOneMatches.get(11).complete(24, 6);
        }
        if (!weekOneMatches.get(12).getComplete()) {
            weekOneMatches.get(12).complete(24, 27);
        }
        if (!weekOneMatches.get(13).getComplete()) {
            weekOneMatches.get(13).complete(23, 24);
        }
        if (!weekOneMatches.get(14).getComplete()) {
            weekOneMatches.get(14).complete(48, 17);
        }
        if (!weekOneMatches.get(15).getComplete()) {
            weekOneMatches.get(15).complete(33, 13);
        }
        if (!weekTwoMatches.get(0).getComplete()) {
            weekTwoMatches.get(0).complete(23, 34);
        }
        if (!weekTwoMatches.get(1).getComplete()) {
            weekTwoMatches.get(1).complete(42, 37);

        }
        if (!weekTwoMatches.get(2).getComplete()) {
            weekTwoMatches.get(2).complete(20, 12);
        }
        if (!weekTwoMatches.get(3).getComplete()) {
            weekTwoMatches.get(3).complete(21, 27);
        }
        if (!weekTwoMatches.get(4).getComplete()) {
            weekTwoMatches.get(4).complete(18, 21);
        }
        if (!weekTwoMatches.get(5).getComplete()) {
            weekTwoMatches.get(5).complete(21, 9);
        }
        if (!weekTwoMatches.get(6).getComplete()) {
            weekTwoMatches.get(6).complete(31, 20);
        }
        if (!weekTwoMatches.get(7).getComplete()) {
            weekTwoMatches.get(7).complete(29, 29);
        }
        if (!weekTwoMatches.get(8).getComplete()) {
            weekTwoMatches.get(8).complete(24, 31);
        }
        if (!weekTwoMatches.get(9).getComplete()) {
            weekTwoMatches.get(9).complete(17, 20);
        }
        if (!weekTwoMatches.get(10).getComplete()) {
            weekTwoMatches.get(10).complete(0, 34);
        }
        if (!weekTwoMatches.get(11).getComplete()) {
            weekTwoMatches.get(11).complete(27, 30);
        }
        if (!weekTwoMatches.get(12).getComplete()) {
            weekTwoMatches.get(12).complete(19, 20);
        }
        if (!weekTwoMatches.get(13).getComplete()) {
            weekTwoMatches.get(13).complete(20, 31);
        }
        if (!weekTwoMatches.get(14).getComplete()) {
            weekTwoMatches.get(14).complete(13, 20);
        }
        if (!weekTwoMatches.get(15).getComplete()) {
            weekTwoMatches.get(15).complete(17, 24);
        }
        if (!weekThreeMatches.get(0).getComplete()) {
            weekThreeMatches.get(0).complete(17, 21);
        }
        if (!weekThreeMatches.get(1).getComplete()) {
            weekThreeMatches.get(1).complete(27, 6);
        }
        if (!weekThreeMatches.get(2).getComplete()) {
            weekThreeMatches.get(2).complete(27, 22);
        }
        if (!weekThreeMatches.get(3).getComplete()) {
            weekThreeMatches.get(3).complete(17, 31);
        }
        if (!weekThreeMatches.get(4).getComplete()) {
            weekThreeMatches.get(4).complete(27, 38);
        }
        if (!weekThreeMatches.get(5).getComplete()) {
            weekThreeMatches.get(5).complete(20, 28);
        }
        if (!weekThreeMatches.get(6).getComplete()) {
            weekThreeMatches.get(6).complete(16, 20);
        }
        if (!weekThreeMatches.get(7).getComplete()) {
            weekThreeMatches.get(7).complete(9, 6);
        }
        if (!weekThreeMatches.get(8).getComplete()) {
            weekThreeMatches.get(8).complete(21, 31);
        }
        if (!weekThreeMatches.get(9).getComplete()) {
            weekThreeMatches.get(9).complete(14, 27);
        }
        if (!weekThreeMatches.get(10).getComplete()) {
            weekThreeMatches.get(10).complete(43, 37);
        }
        if (!weekThreeMatches.get(11).getComplete()) {
            weekThreeMatches.get(11).complete(23, 35);
        }
        if (!weekThreeMatches.get(12).getComplete()) {
            weekThreeMatches.get(12).complete(16, 14);
        }
        if (!weekThreeMatches.get(13).getComplete()) {
            weekThreeMatches.get(13).complete(13, 24);
        }
        if (!weekThreeMatches.get(14).getComplete()) {
            weekThreeMatches.get(14).complete(10, 26);
        }
        if (!weekThreeMatches.get(15).getComplete()) {
            weekThreeMatches.get(15).complete(30, 27);
        }
        if (!weekFourMatches.get(0).getComplete()) {
            weekFourMatches.get(0).complete(31, 38);
        }
        if (!weekFourMatches.get(1).getComplete()) {
            weekFourMatches.get(1).complete(37, 34);
        }
        if (!weekFourMatches.get(2).getComplete()) {
            weekFourMatches.get(2).complete(0, 22);
        }
        if (!weekFourMatches.get(3).getComplete()) {
            weekFourMatches.get(3).complete(10, 48);
        }
        if (!weekFourMatches.get(4).getComplete()) {
            weekFourMatches.get(4).complete(7, 38);
        }
        if (!weekFourMatches.get(5).getComplete()) {
            weekFourMatches.get(5).complete(24, 26);
        }
        if (!weekFourMatches.get(6).getComplete()) {
            weekFourMatches.get(6).complete(37, 36);
        }
        if (!weekFourMatches.get(7).getComplete()) {
            weekFourMatches.get(7).complete(12, 31);
        }
        if (!weekFourMatches.get(8).getComplete()) {
            weekFourMatches.get(8).complete(23, 26);
        }
        if (!weekFourMatches.get(9).getComplete()) {
            weekFourMatches.get(9).complete(42, 45);
        }
        if (!weekFourMatches.get(10).getComplete()) {
            weekFourMatches.get(10).complete(20, 17);
        }
        if (!weekFourMatches.get(11).getComplete()) {
            weekFourMatches.get(11).complete(33, 18);
        }
        if (!weekFourMatches.get(12).getComplete()) {
            weekFourMatches.get(12).complete(27, 29);
        }
        if (!weekFourMatches.get(13).getComplete()) {
            weekFourMatches.get(13).complete(26, 14);
        }
        if (!weekFourMatches.get(14).getComplete()) {
            weekFourMatches.get(14).complete(27, 23);
        }
        if (!weekFiveMatches.get(0).getComplete()) {
            weekFiveMatches.get(0).complete(24, 38);
        }
        if (!weekFiveMatches.get(1).getComplete()) {
            weekFiveMatches.get(1).complete(16, 34);
        }
        if (!weekFiveMatches.get(2).getComplete()) {
            weekFiveMatches.get(2).complete(23, 31);
        }
        if (!weekFiveMatches.get(3).getComplete()) {
            weekFiveMatches.get(3).complete(31, 33);
        }
        if (!weekFiveMatches.get(4).getComplete()) {
            weekFiveMatches.get(4).complete(12, 13);
        }
        if (!weekFiveMatches.get(5).getComplete()) {
            weekFiveMatches.get(5).complete(17, 41);
        }
        if (!weekFiveMatches.get(6).getComplete()) {
            weekFiveMatches.get(6).complete(9, 12);
        }
        if (!weekFiveMatches.get(7).getComplete()) {
            weekFiveMatches.get(7).complete(17, 27);
        }
        if (!weekFiveMatches.get(8).getComplete()) {
            weekFiveMatches.get(8).complete(14, 30);
        }
        if (!weekFiveMatches.get(9).getComplete()) {
            weekFiveMatches.get(9).complete(10, 26);
        }
        if (!weekFiveMatches.get(10).getComplete()) {
            weekFiveMatches.get(10).complete(23, 21);
        }
        if (!weekFiveMatches.get(11).getComplete()) {
            weekFiveMatches.get(11).complete(33, 31);
        }
        if (!weekFiveMatches.get(12).getComplete()) {
            weekFiveMatches.get(12).complete(28, 18);
        }
        if (!weekFiveMatches.get(13).getComplete()) {
            weekFiveMatches.get(13).complete(16, 19);
        }
        if (!weekFiveMatches.get(14).getComplete()) {
            weekFiveMatches.get(14).complete(19, 43);
        }
        if (!weekSixMatches.get(0).getComplete()) {
            weekSixMatches.get(0).complete(34, 13);
        }
        if (!weekSixMatches.get(1).getComplete()) {
            weekSixMatches.get(1).complete(29, 34);
        }
        if (!weekSixMatches.get(2).getComplete()) {
            weekSixMatches.get(2).complete(38, 14);
        }
        if (!weekSixMatches.get(3).getComplete()) {
            weekSixMatches.get(3).complete(34, 42);
        }
        if (!weekSixMatches.get(4).getComplete()) {
            weekSixMatches.get(4).complete(27, 3);
        }
        if (!weekSixMatches.get(5).getComplete()) {
            weekSixMatches.get(5).complete(17, 23);
        }
        if (!weekSixMatches.get(6).getComplete()) {
            weekSixMatches.get(6).complete(13, 20);
        }
        if (!weekSixMatches.get(7).getComplete()) {
            weekSixMatches.get(7).complete(17, 27);
        }
        if (!weekSixMatches.get(8).getComplete()) {
            weekSixMatches.get(8).complete(28, 21);
        }
        if (!weekSixMatches.get(9).getComplete()) {
            weekSixMatches.get(9).complete(28, 31);
        }
        if (!weekSixMatches.get(10).getComplete()) {
            weekSixMatches.get(10).complete(23, 20);
        }
        if (!weekSixMatches.get(11).getComplete()) {
            weekSixMatches.get(11).complete(21, 0);
        }
        if (!weekSixMatches.get(12).getComplete()) {
            weekSixMatches.get(12).complete(7, 40);
        }
        if (!weekSixMatches.get(13).getComplete()) {
            weekSixMatches.get(13).complete(40, 43);
        }
        if (!weekSixMatches.get(14).getComplete()) {
            weekSixMatches.get(14).complete(30, 33);
        }
        if (!weekSevenMatches.get(0).getComplete()) {
            weekSevenMatches.get(0).complete(45, 10);
        }
        if (!weekSevenMatches.get(1).getComplete()) {
            weekSevenMatches.get(1).complete(19, 20);
        }
        if (!weekSevenMatches.get(2).getComplete()) {
            weekSevenMatches.get(2).complete(21, 17);
        }
        if (!weekSevenMatches.get(3).getComplete()) {
            weekSevenMatches.get(3).complete(37, 17);
        }
        if (!weekSevenMatches.get(4).getComplete()) {
            weekSevenMatches.get(4).complete(5, 37);
        }
        if (!weekSevenMatches.get(5).getComplete()) {
            weekSevenMatches.get(5).complete(38, 31);
        }
        if (!weekSevenMatches.get(6).getComplete()) {
            weekSevenMatches.get(6).complete(23, 26);
        }
        if (!weekSevenMatches.get(7).getComplete()) {
            weekSevenMatches.get(7).complete(20, 7);
        }
        if (!weekSevenMatches.get(8).getComplete()) {
            weekSevenMatches.get(8).complete(10, 45);
        }
        if (!weekSevenMatches.get(9).getComplete()) {
            weekSevenMatches.get(9).complete(32, 21);
        }
        if (!weekSevenMatches.get(10).getComplete()) {
            weekSevenMatches.get(10).complete(24, 23);
        }
        if (!weekSevenMatches.get(11).getComplete()) {
            weekSevenMatches.get(11).complete(17, 20);
        }
        if (!weekSevenMatches.get(12).getComplete()) {
            weekSevenMatches.get(12).complete(39, 10);
        }
        if (!weekSevenMatches.get(13).getComplete()) {
            weekSevenMatches.get(13).complete(20, 23);
        }
        if (!weekEightMatches.get(0).getComplete()) {
            weekEightMatches.get(0).complete(23, 42);
        }
        if (!weekEightMatches.get(1).getComplete()) {
            weekEightMatches.get(1).complete(24, 18);
        }
        if (!weekEightMatches.get(2).getComplete()) {
            weekEightMatches.get(2).complete(21, 36);
        }
        if (!weekEightMatches.get(3).getComplete()) {
            weekEightMatches.get(3).complete(23, 30);
        }
        if (!weekEightMatches.get(4).getComplete()) {
            weekEightMatches.get(4).complete(18, 33);
        }
        if (!weekEightMatches.get(5).getComplete()) {
            weekEightMatches.get(5).complete(28, 14);
        }
        if (!weekEightMatches.get(6).getComplete()) {
            weekEightMatches.get(6).complete(34, 37);
        }
        if (!weekEightMatches.get(7).getComplete()) {
            weekEightMatches.get(7).complete(10, 24);
        }
        if (!weekEightMatches.get(8).getComplete()) {
            weekEightMatches.get(8).complete(20, 13);
        }
        if (!weekEightMatches.get(9).getComplete()) {
            weekEightMatches.get(9).complete(42, 28);
        }
        if (!weekEightMatches.get(10).getComplete()) {
            weekEightMatches.get(10).complete(15, 18);
        }
        if (!weekEightMatches.get(11).getComplete()) {
            weekEightMatches.get(11).complete(27, 29);
        }
        if (!weekEightMatches.get(12).getComplete()) {
            weekEightMatches.get(12).complete(30, 20);
        }
        if (!weekEightMatches.get(13).getComplete()) {
            weekEightMatches.get(13).complete(25, 6);
        }
        if (!weekNineMatches.get(0).getComplete()) {
            weekNineMatches.get(0).complete(3, 34);
        }
        if (!weekNineMatches.get(1).getComplete()) {
            weekNineMatches.get(1).complete(38, 14);
        }
        if (!weekNineMatches.get(2).getComplete()) {
            weekNineMatches.get(2).complete(9, 24);
        }
        if (!weekNineMatches.get(3).getComplete()) {
            weekNineMatches.get(3).complete(23, 16);
        }
        if (!weekNineMatches.get(4).getComplete()) {
            weekNineMatches.get(4).complete(37, 21);
        }
        if (!weekNineMatches.get(5).getComplete()) {
            weekNineMatches.get(5).complete(6, 13);
        }
        if (!weekNineMatches.get(6).getComplete()) {
            weekNineMatches.get(6).complete(41, 9);
        }
        if (!weekNineMatches.get(7).getComplete()) {
            weekNineMatches.get(7).complete(28, 42);
        }
        if (!weekNineMatches.get(8).getComplete()) {
            weekNineMatches.get(8).complete(25, 17);
        }
        if (!weekNineMatches.get(9).getComplete()) {
            weekNineMatches.get(9).complete(19, 17);
        }
        if (!weekNineMatches.get(10).getComplete()) {
            weekNineMatches.get(10).complete(35, 45);
        }
        if (!weekNineMatches.get(11).getComplete()) {
            weekNineMatches.get(11).complete(17, 31);
        }
        if (!weekNineMatches.get(12).getComplete()) {
            weekNineMatches.get(12).complete(28, 14);
        }
        if (!weekTenMatches.get(0).getComplete()) {
            weekTenMatches.get(0).complete(21, 52);
        }
        if (!weekTenMatches.get(1).getComplete()) {
            weekTenMatches.get(1).complete(16, 3);
        }
        if (!weekTenMatches.get(2).getComplete()) {
            weekTenMatches.get(2).complete(14, 26);
        }
        if (!weekTenMatches.get(3).getComplete()) {
            weekTenMatches.get(3).complete(41, 10);
        }
        if (!weekTenMatches.get(4).getComplete()) {
            weekTenMatches.get(4).complete(26, 29);
        }
        if (!weekTenMatches.get(5).getComplete()) {
            weekTenMatches.get(5).complete(22, 34);
        }
        if (!weekTenMatches.get(6).getComplete()) {
            weekTenMatches.get(6).complete(12, 31);
        }
        if (!weekTenMatches.get(7).getComplete()) {
            weekTenMatches.get(7).complete(51, 14);
        }
        if (!weekTenMatches.get(8).getComplete()) {
            weekTenMatches.get(8).complete(10, 34);
        }
        if (!weekTenMatches.get(9).getComplete()) {
            weekTenMatches.get(9).complete(16, 28);
        }
        if (!weekTenMatches.get(10).getComplete()) {
            weekTenMatches.get(10).complete(20, 6);
        }
        if (!weekTenMatches.get(11).getComplete()) {
            weekTenMatches.get(11).complete(31, 36);
        }
        if (!weekTenMatches.get(12).getComplete()) {
            weekTenMatches.get(12).complete(27, 20);
        }
        if (!weekTenMatches.get(13).getComplete()) {
            weekTenMatches.get(13).complete(27, 23);
        }


    }

    @Override
    public void loadCurrentSeasonPlayoffOdds() {
        HashMap<String, Team> currentSeasonTeams = mModel.getSeasonTeamList();
        currentSeasonTeams.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING).setPlayoffOddsString("0-0-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING).setPlayoffOddsString("86.3-78.68-4.02-1.32");
        currentSeasonTeams.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING).setPlayoffOddsString("85.74-56.6-12.08-6.26");
        currentSeasonTeams.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING).setPlayoffOddsString("0-0-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING).setPlayoffOddsString("100-100-47.22-26.92");
        currentSeasonTeams.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING).setPlayoffOddsString("40.9-35.04-3.56-1.86");
        currentSeasonTeams.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING).setPlayoffOddsString("0-0-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING).setPlayoffOddsString("0.02-0.02-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING).setPlayoffOddsString("69.02-62.2-2.96-1.04");
        currentSeasonTeams.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING).setPlayoffOddsString("13.88-1.32-0.46-0.22");
        currentSeasonTeams.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING).setPlayoffOddsString("0.08-0-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING).setPlayoffOddsString("100-91.94-43.3-22.72");
        currentSeasonTeams.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING).setPlayoffOddsString("97.62-74.58-20.64-9.9");
        currentSeasonTeams.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING).setPlayoffOddsString("15.96-5.9-0.88-0.32");
        currentSeasonTeams.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING).setPlayoffOddsString("21.34-2.16-0.7-0.18");
        currentSeasonTeams.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING).setPlayoffOddsString("3.44-2.74-0.04-0");
        currentSeasonTeams.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING).setPlayoffOddsString("45.26-15.24-1.12-0.22");
        currentSeasonTeams.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING).setPlayoffOddsString("59.72-25.8-3.94-1.78");
        currentSeasonTeams.get(NFLConstants.TEAM_NEWYORK_JETS_STRING).setPlayoffOddsString("0.08-0-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING).setPlayoffOddsString("1.18-0-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING).setPlayoffOddsString("92.06-82.54-13.74-6.44");
        currentSeasonTeams.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING).setPlayoffOddsString("2.84-0-0.04-0");
        currentSeasonTeams.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING).setPlayoffOddsString("95.32-8.06-6.14-2.38");
        currentSeasonTeams.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING).setPlayoffOddsString("81.12-24.02-8.34-3.54");
        currentSeasonTeams.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING).setPlayoffOddsString("99.56-98.98-29.78-14.72");
        currentSeasonTeams.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING).setPlayoffOddsString("4.78-2.02-0.02-0");
        currentSeasonTeams.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING).setPlayoffOddsString("8.54-1.02-0.06-0");
        currentSeasonTeams.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING).setPlayoffOddsString("0.18-0.06-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING).setPlayoffOddsString("46.24-17.6-0.76-0.12");
        currentSeasonTeams.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING).setPlayoffOddsString("27.7-13.4-0.2-0.06");
        currentSeasonTeams.get(NFLConstants.TEAM_DETROIT_LIONS_STRING).setPlayoffOddsString("0.02-0-0-0");
        currentSeasonTeams.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING).setPlayoffOddsString("1.1-0.08-0-0");
    }

    public void addScoreView(ScoreView scoreView) {
        mScoreViews.add(scoreView);
    }

    @Override
    public void simulatorMatchesInserted(int insertType) {

        //Callback received after matches are inserted into the database in the model
        //An action is performed below depending on the insertType

        if (insertType == SimulatorModel.INSERT_MATCHES_SCHEDULE) {
            //After simulator season is initialized, teams are inserted, and matches inserted, 
            //insert season teams to finish initializing the current season

            updateSimulatorCompletedGameScores();

            mModel.insertSeasonTeams();

        }

        if (insertType == SimulatorModel.INSERT_MATCHES_PLAYOFFS_WILDCARD) {
            mModel.querySimulatorMatches(MatchEntry.MATCH_WEEK_WILDCARD, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }

        if (insertType == SimulatorModel.INSERT_MATCHES_PLAYOFFS_DIVISIONAL) {
            mModel.querySimulatorMatches(MatchEntry.MATCH_WEEK_DIVISIONAL, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }
        if (insertType == SimulatorModel.INSERT_MATCHES_PLAYOFFS_CHAMPIONSHIP) {
            mModel.querySimulatorMatches(MatchEntry.MATCH_WEEK_CHAMPIONSHIP, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }
        if (insertType == SimulatorModel.INSERT_MATCHES_PLAYOFFS_SUPERBOWL) {
            mModel.querySimulatorMatches(MatchEntry.MATCH_WEEK_SUPERBOWL, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }
    }

    private void updateSimulatorCompletedGameScores() {
        //Update the scores for games that have already occurred
        Week weekOne = mModel.getSimulatorSchedule().getWeek(1);
        ArrayList<Match> weekOneMatches = weekOne.getMatches();
        Week weekTwo = mModel.getSimulatorSchedule().getWeek(2);
        ArrayList<Match> weekTwoMatches = weekTwo.getMatches();
        Week weekThree = mModel.getSimulatorSchedule().getWeek(3);
        ArrayList<Match> weekThreeMatches = weekThree.getMatches();
        Week weekFour = mModel.getSimulatorSchedule().getWeek(4);
        ArrayList<Match> weekFourMatches = weekFour.getMatches();
        Week weekFive = mModel.getSimulatorSchedule().getWeek(5);
        ArrayList<Match> weekFiveMatches = weekFive.getMatches();
        Week weekSix = mModel.getSimulatorSchedule().getWeek(6);
        ArrayList<Match> weekSixMatches = weekSix.getMatches();
        Week weekSeven = mModel.getSimulatorSchedule().getWeek(7);
        ArrayList<Match> weekSevenMatches = weekSeven.getMatches();
        Week weekEight = mModel.getSimulatorSchedule().getWeek(8);
        ArrayList<Match> weekEightMatches = weekEight.getMatches();
        Week weekNine = mModel.getSimulatorSchedule().getWeek(9);
        ArrayList<Match> weekNineMatches = weekNine.getMatches();
        Week weekTen = mModel.getSimulatorSchedule().getWeek(10);
        ArrayList<Match> weekTenMatches = weekTen.getMatches();
        weekOneMatches.get(0).complete(12, 18);
        weekOneMatches.get(1).complete(34, 23);
        weekOneMatches.get(2).complete(3, 47);
        weekOneMatches.get(3).complete(48, 40);
        weekOneMatches.get(4).complete(20, 27);
        weekOneMatches.get(5).complete(16, 24);
        weekOneMatches.get(6).complete(20, 27);
        weekOneMatches.get(7).complete(20, 15);
        weekOneMatches.get(8).complete(21, 21);
        weekOneMatches.get(9).complete(38, 28);
        weekOneMatches.get(10).complete(8, 16);
        weekOneMatches.get(11).complete(24, 6);
        weekOneMatches.get(12).complete(24, 27);
        weekOneMatches.get(13).complete(23, 24);
        weekOneMatches.get(14).complete(48, 17);
        weekOneMatches.get(15).complete(33, 13);
        weekTwoMatches.get(0).complete(23, 34);
        weekTwoMatches.get(1).complete(42, 37);
        weekTwoMatches.get(2).complete(20, 12);
        weekTwoMatches.get(3).complete(21, 27);
        weekTwoMatches.get(4).complete(18, 21);
        weekTwoMatches.get(5).complete(21, 9);
        weekTwoMatches.get(6).complete(31, 20);
        weekTwoMatches.get(7).complete(29, 29);
        weekTwoMatches.get(8).complete(24, 31);
        weekTwoMatches.get(9).complete(17, 20);
        weekTwoMatches.get(10).complete(0, 34);
        weekTwoMatches.get(11).complete(27, 30);
        weekTwoMatches.get(12).complete(19, 20);
        weekTwoMatches.get(13).complete(20, 31);
        weekTwoMatches.get(14).complete(13, 20);
        weekTwoMatches.get(15).complete(17, 24);
        weekThreeMatches.get(0).complete(17, 21);
        weekThreeMatches.get(1).complete(27, 6);
        weekThreeMatches.get(2).complete(27, 22);
        weekThreeMatches.get(3).complete(17, 31);
        weekThreeMatches.get(4).complete(27, 38);
        weekThreeMatches.get(5).complete(20, 28);
        weekThreeMatches.get(6).complete(16, 20);
        weekThreeMatches.get(7).complete(9, 6);
        weekThreeMatches.get(8).complete(21, 31);
        weekThreeMatches.get(9).complete(14, 27);
        weekThreeMatches.get(10).complete(43, 37);
        weekThreeMatches.get(11).complete(23, 35);
        weekThreeMatches.get(12).complete(16, 14);
        weekThreeMatches.get(13).complete(13, 24);
        weekThreeMatches.get(14).complete(10, 26);
        weekThreeMatches.get(15).complete(30, 27);
        weekFourMatches.get(0).complete(31, 38);
        weekFourMatches.get(1).complete(37, 34);
        weekFourMatches.get(2).complete(0, 22);
        weekFourMatches.get(3).complete(10, 48);
        weekFourMatches.get(4).complete(7, 38);
        weekFourMatches.get(5).complete(24, 26);
        weekFourMatches.get(6).complete(37, 36);
        weekFourMatches.get(7).complete(12, 31);
        weekFourMatches.get(8).complete(23, 26);
        weekFourMatches.get(9).complete(42, 45);
        weekFourMatches.get(10).complete(20, 17);
        weekFourMatches.get(11).complete(33, 18);
        weekFourMatches.get(12).complete(27, 29);
        weekFourMatches.get(13).complete(26, 14);
        weekFourMatches.get(14).complete(27, 23);
        weekFiveMatches.get(0).complete(24, 38);
        weekFiveMatches.get(1).complete(16, 34);
        weekFiveMatches.get(2).complete(23, 31);
        weekFiveMatches.get(3).complete(31, 33);
        weekFiveMatches.get(4).complete(12, 13);
        weekFiveMatches.get(5).complete(17, 41);
        weekFiveMatches.get(6).complete(9, 12);
        weekFiveMatches.get(7).complete(17, 27);
        weekFiveMatches.get(8).complete(14, 30);
        weekFiveMatches.get(9).complete(10, 26);
        weekFiveMatches.get(10).complete(23, 21);
        weekFiveMatches.get(11).complete(33, 31);
        weekFiveMatches.get(12).complete(28, 18);
        weekFiveMatches.get(13).complete(16, 19);
        weekFiveMatches.get(14).complete(19, 43);
        weekSixMatches.get(0).complete(34, 13);
        weekSixMatches.get(1).complete(29, 34);
        weekSixMatches.get(2).complete(38, 14);
        weekSixMatches.get(3).complete(34, 42);
        weekSixMatches.get(4).complete(27, 3);
        weekSixMatches.get(5).complete(17, 23);
        weekSixMatches.get(6).complete(13, 20);
        weekSixMatches.get(7).complete(17, 27);
        weekSixMatches.get(8).complete(28, 21);
        weekSixMatches.get(9).complete(28, 31);
        weekSixMatches.get(10).complete(23, 20);
        weekSixMatches.get(11).complete(21, 0);
        weekSixMatches.get(12).complete(7, 40);
        weekSixMatches.get(13).complete(40, 43);
        weekSixMatches.get(14).complete(30, 33);
        weekSevenMatches.get(0).complete(45, 10);
        weekSevenMatches.get(1).complete(19, 20);
        weekSevenMatches.get(2).complete(21, 17);
        weekSevenMatches.get(3).complete(37, 17);
        weekSevenMatches.get(4).complete(5, 37);
        weekSevenMatches.get(5).complete(38, 31);
        weekSevenMatches.get(6).complete(23, 26);
        weekSevenMatches.get(7).complete(20, 7);
        weekSevenMatches.get(8).complete(10, 45);
        weekSevenMatches.get(9).complete(32, 21);
        weekSevenMatches.get(10).complete(24, 23);
        weekSevenMatches.get(11).complete(17, 20);
        weekSevenMatches.get(12).complete(39, 10);
        weekSevenMatches.get(13).complete(20, 23);
        weekEightMatches.get(0).complete(23, 42);
        weekEightMatches.get(1).complete(24, 18);
        weekEightMatches.get(2).complete(21, 36);
        weekEightMatches.get(3).complete(23, 30);
        weekEightMatches.get(4).complete(18, 33);
        weekEightMatches.get(5).complete(28, 14);
        weekEightMatches.get(6).complete(34, 37);
        weekEightMatches.get(7).complete(10, 24);
        weekEightMatches.get(8).complete(20, 13);
        weekEightMatches.get(9).complete(42, 28);
        weekEightMatches.get(10).complete(15, 18);
        weekEightMatches.get(11).complete(27, 29);
        weekEightMatches.get(12).complete(30, 20);
        weekEightMatches.get(13).complete(25, 6);
        weekNineMatches.get(0).complete(3, 34);
        weekNineMatches.get(1).complete(38, 14);
        weekNineMatches.get(2).complete(9, 24);
        weekNineMatches.get(3).complete(23, 16);
        weekNineMatches.get(4).complete(37, 21);
        weekNineMatches.get(5).complete(6, 13);
        weekNineMatches.get(6).complete(41, 9);
        weekNineMatches.get(7).complete(28, 42);
        weekNineMatches.get(8).complete(25, 17);
        weekNineMatches.get(9).complete(19, 17);
        weekNineMatches.get(10).complete(35, 45);
        weekNineMatches.get(11).complete(17, 31);
        weekNineMatches.get(12).complete(28, 14);
        weekTenMatches.get(0).complete(21, 52);
        weekTenMatches.get(1).complete(16, 3);
        weekTenMatches.get(2).complete(14, 26);
        weekTenMatches.get(3).complete(41, 10);
        weekTenMatches.get(4).complete(26, 29);
        weekTenMatches.get(5).complete(22, 34);
        weekTenMatches.get(6).complete(12, 31);
        weekTenMatches.get(7).complete(51, 14);
        weekTenMatches.get(8).complete(10, 34);
        weekTenMatches.get(9).complete(16, 28);
        weekTenMatches.get(10).complete(20, 6);
        weekTenMatches.get(11).complete(31, 36);
        weekTenMatches.get(12).complete(27, 20);
        weekTenMatches.get(13).complete(27, 23);


        mCurrentSimulatorWeek = 10;
        mCurrentSimulatorWeek++;
        //Set current week preference when week is updated
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(mContext.getString(R.string.settings_simulator_week_num_key), mCurrentSimulatorWeek).apply();
        prefs.commit();


    }

    @Override
    public void seasonMatchesInserted(int insertType) {

        //Callback received after current season matches are inserted into the database in the model
        //An action is performed below depending on the insertType

        if (insertType == SimulatorModel.INSERT_MATCHES_SCHEDULE) {

            updateCurrentSeasonTeamOdds();

            //After current season is initialized, set season initialized pref to true
            //Set season loading preference to true as well, since initializing a season also loads it
            //Notify all base views that the season was initialized
            setSeasonInitializedPreference(true);
            setSeasonLoadedPreference(true);
            for (BaseView baseView : mBaseViews) {
                baseView.onSeasonInitialized();
            }

        }

    }

    @Override
    public void simulatorMatchesQueried(int queryType, Cursor matchesCursor, int queriedFrom) {

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

            for (ScoreView scoreView : mScoreViews) {
                scoreView.onDisplayScores(queryType, matchesCursor, scoreWeekNumberHeader, queriedFrom);
            }


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
                int matchComplete = matchesCursor.getInt(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_COMPLETE));
                double teamTwoOdds = matchesCursor.getDouble(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_TWO_ODDS));
                int ID = matchesCursor.getInt(matchesCursor.getColumnIndexOrThrow(MatchEntry._ID));
                Uri matchUri = ContentUris.withAppendedId(MatchEntry.CONTENT_URI, ID);

                HashMap<String, Team> teamList = mModel.getSimulatorTeamList();

                if (teamList != null) {

                    switch (matchWeek) {

                        case 1:
                            weekOne.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 1, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 2:
                            weekTwo.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 2, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 3:
                            weekThree.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 3, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 4:
                            weekFour.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 4, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 5:
                            weekFive.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 5, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 6:
                            weekSix.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 6, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 7:
                            weekSeven.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 7, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 8:
                            weekEight.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 8, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 9:
                            weekNine.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 9, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 10:
                            weekTen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 10, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 11:
                            weekEleven.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 11, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 12:
                            weekTwelve.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 12, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 13:
                            weekThirteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 13, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 14:
                            weekFourteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 14, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 15:
                            weekFifteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 15, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 16:
                            weekSixteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 16, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 17:
                            weekSeventeen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 17, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 18:
                            wildCard.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 18, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 19:
                            divisional.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 19, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 20:
                            championship.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 20, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
                            break;
                        case 21:
                            superbowl.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 21, this, matchUri, teamTwoOdds, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO, matchComplete));
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

            if (mCurrentSimulatorWeek >= 18 && mSimulatorPlayoffsStarted) {
                seasonSchedule.addWeek(wildCard);
            }
            if (mCurrentSimulatorWeek >= 19 && mSimulatorPlayoffsStarted) {
                seasonSchedule.addWeek(divisional);
            }
            if (mCurrentSimulatorWeek >= 20 && mSimulatorPlayoffsStarted) {
                seasonSchedule.addWeek(championship);
            }
            if (mCurrentSimulatorWeek >= 21 && mSimulatorPlayoffsStarted) {
                seasonSchedule.addWeek(superbowl);
            }


            mModel.setSimulatorSchedule(seasonSchedule);

            if (mSimulatorPlayoffsStarted) {
                //If the playoffs have already started, re-query the playoff standings  after all matches are created
                mModel.querySimulatorStandings(SimulatorModel.QUERY_STANDINGS_LOAD_POSTSEASON);
            }
        }

    }

    @Override
    public void currentSeasonMatchesQueried(int queryType, Cursor matchesCursor, int queriedFrom) {

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

            for (ScoreView scoreView : mScoreViews) {
                scoreView.onDisplayScores(queryType, matchesCursor, scoreWeekNumberHeader, queriedFrom);
            }


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
                int matchComplete = matchesCursor.getInt(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_COMPLETE));
                double teamTwoOdds = matchesCursor.getDouble(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_TWO_ODDS));
                int currentSeason = matchesCursor.getInt(matchesCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_CURRENT_SEASON));
                int ID = matchesCursor.getInt(matchesCursor.getColumnIndexOrThrow(MatchEntry._ID));
                Uri matchUri = ContentUris.withAppendedId(MatchEntry.CONTENT_URI, ID);

                HashMap<String, Team> teamList = mModel.getSeasonTeamList();

                if (teamList != null) {

                    switch (matchWeek) {

                        case 1:
                            weekOne.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 1, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 2:
                            weekTwo.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 2, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 3:
                            weekThree.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 3, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 4:
                            weekFour.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 4, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 5:
                            weekFive.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 5, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 6:
                            weekSix.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 6, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 7:
                            weekSeven.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 7, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 8:
                            weekEight.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 8, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 9:
                            weekNine.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 9, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 10:
                            weekTen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 10, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 11:
                            weekEleven.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 11, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 12:
                            weekTwelve.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 12, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 13:
                            weekThirteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 13, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 14:
                            weekFourteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 14, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 15:
                            weekFifteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 15, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 16:
                            weekSixteen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 16, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 17:
                            weekSeventeen.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 17, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 18:
                            wildCard.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 18, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 19:
                            divisional.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 19, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 20:
                            championship.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 20, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
                            break;
                        case 21:
                            superbowl.addMatch(new Match(teamList.get(teamOne), teamList.get(teamTwo), teamOneWon, 21, this, matchUri, teamTwoOdds, currentSeason, matchComplete));
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

            if (mCurrentSimulatorWeek >= 18 && mCurrentSeasonPlayoffsStarted) {
                seasonSchedule.addWeek(wildCard);
            }
            if (mCurrentSimulatorWeek >= 19 && mCurrentSeasonPlayoffsStarted) {
                seasonSchedule.addWeek(divisional);
            }
            if (mCurrentSimulatorWeek >= 20 && mCurrentSeasonPlayoffsStarted) {
                seasonSchedule.addWeek(championship);
            }
            if (mCurrentSimulatorWeek >= 21 && mCurrentSeasonPlayoffsStarted) {
                seasonSchedule.addWeek(superbowl);
            }


            mModel.setSeasonSchedule(seasonSchedule);
            updateCurrentSeasonTeamOdds();

            if (mCurrentSeasonPlayoffsStarted) {
                //If the playoffs have already started, re-query the playoff standings  after all matches are created
                mModel.queryCurrentSeasonStandings(SimulatorModel.QUERY_STANDINGS_LOAD_POSTSEASON);
            }
        }


    }

    @Override
    public void simulatorStandingsQueried(int queryType, Cursor standingsCursor) {

        //This callback will be received from the model whenever teams/standings are queried for the simulated season
        //Depending on the queryType, a specific action is performed

        if (queryType == SimulatorModel.QUERY_STANDINGS_PLAYOFF) {
            //A playoff standings was queried
            //Display playoff standings in UI
            Log.d("Standings", "displayStandingsCalled");
            displaySimulatorStandings(standingsCursor);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_LOAD_SEASON) {
            //The entire season was loaded from the db
            //Create teams from the db data and then query all matches from the db
            createSimulatorTeamsFromDb(standingsCursor);
            mModel.querySimulatorMatches(SimulatorModel.QUERY_MATCHES_ALL, false, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_POSTSEASON) {
            //The postseason standings were queried
            //Create playoff matchups based on the playoff standings/round and then display the
            //standings in the Main Activity UI
            createPlayoffMatchups(standingsCursor);
            displaySimulatorPlayoffStandings(standingsCursor);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_LOAD_POSTSEASON) {
            //The app was restarted and the postseason standings need to be re-loaded from the database
            //Display the standings in the SimulatorActivity UI and query the playoff matches
            displaySimulatorPlayoffStandings(standingsCursor);
            mModel.querySimulatorMatches(mCurrentSimulatorWeek, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }

    }

    @Override
    public void currentSeasonStandingsQueried(int queryType, Cursor standingsCursor) {
        //This callback will be received from the model whenever teams/standings are queried for the current season
        //Depending on the queryType, a specific action is performed

        if (queryType == SimulatorModel.QUERY_STANDINGS_PLAYOFF) {
            //A playoff standings was queried
            //Display playoff standings in UI
            displayCurrentSeasonStandings(standingsCursor);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_LOAD_SEASON) {
            //The entire season was loaded from the db
            //Create teams from the db data and then query all matches from the db
            createCurrentSeasonTeamsFromDb(standingsCursor);
            mModel.queryCurrentSeasonMatches(SimulatorModel.QUERY_MATCHES_ALL, false, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_POSTSEASON) {
            //TODO Fix this so that it works for loading postseason if necessary for current season instead of simulator
            //The postseason standings were queried
            //Create playoff matchups based on the playoff standings/round and then display the
            //standings in the Main Activity UI
            createPlayoffMatchups(standingsCursor);
            displaySimulatorPlayoffStandings(standingsCursor);
        }
        if (queryType == SimulatorModel.QUERY_STANDINGS_LOAD_POSTSEASON) {
            //TODO Fix this so that it works for loading postseason if necessary for current season instead of simulator
            //The app was restarted and the postseason standings need to be re-loaded from the database
            //Display the standings in the SimulatorActivity UI and query the playoff matches
            displaySimulatorPlayoffStandings(standingsCursor);
            mModel.querySimulatorMatches(mCurrentSimulatorWeek, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }

    }

    private void updateCurrentSeasonTeamOdds() {
        Schedule currentSchedule = mModel.getSeasonSchedule();
        Week weekOne = currentSchedule.getWeek(1);
        ArrayList<Match> weekOneMatches = weekOne.getMatches();
        weekOneMatches.get(0).setOdds(1.0);
        weekOneMatches.get(1).setOdds(3.0);
        weekOneMatches.get(2).setOdds(7.5);
        weekOneMatches.get(3).setOdds(9.5);
        weekOneMatches.get(4).setOdds(6.5);
        weekOneMatches.get(5).setOdds(6.5);
        weekOneMatches.get(6).setOdds(-1.5);
        weekOneMatches.get(7).setOdds(-3.0);
        weekOneMatches.get(8).setOdds(-3.5);
        weekOneMatches.get(9).setOdds(3.5);
        weekOneMatches.get(10).setOdds(3.0);
        weekOneMatches.get(11).setOdds(1.0);
        weekOneMatches.get(12).setOdds(3.0);
        weekOneMatches.get(13).setOdds(7.5);
        weekOneMatches.get(14).setOdds(6.5);
        weekOneMatches.get(15).setOdds(-4.0);
        Week weekTwo = currentSchedule.getWeek(2);
        ArrayList<Match> weekTwoMatches = weekTwo.getMatches();
        weekTwoMatches.get(0).setOdds(-1.0);
        weekTwoMatches.get(1).setOdds(5.5);
        weekTwoMatches.get(2).setOdds(2.5);
        weekTwoMatches.get(3).setOdds(-3.5);
        weekTwoMatches.get(4).setOdds(9.5);
        weekTwoMatches.get(5).setOdds(6.0);
        weekTwoMatches.get(6).setOdds(-7.5);
        weekTwoMatches.get(7).setOdds(1.0);
        weekTwoMatches.get(8).setOdds(6.0);
        weekTwoMatches.get(9).setOdds(-3.0);
        weekTwoMatches.get(10).setOdds(13.0);
        weekTwoMatches.get(11).setOdds(6.0);
        weekTwoMatches.get(12).setOdds(6.5);
        weekTwoMatches.get(13).setOdds(-1.0);
        weekTwoMatches.get(14).setOdds(3.0);
        weekTwoMatches.get(15).setOdds(3.5);
        Week weekThree = currentSchedule.getWeek(3);
        ArrayList<Match> weekThreeMatches = weekThree.getMatches();
        weekThreeMatches.get(0).setOdds(3.0);
        weekThreeMatches.get(1).setOdds(16.5);
        weekThreeMatches.get(2).setOdds(6.0);
        weekThreeMatches.get(3).setOdds(-3.0);
        weekThreeMatches.get(4).setOdds(6.0);
        weekThreeMatches.get(5).setOdds(3.0);
        weekThreeMatches.get(6).setOdds(6.5);
        weekThreeMatches.get(7).setOdds(0.0);
        weekThreeMatches.get(8).setOdds(3.0);
        weekThreeMatches.get(9).setOdds(5.0);
        weekThreeMatches.get(10).setOdds(3.0);
        weekThreeMatches.get(11).setOdds(7.0);
        weekThreeMatches.get(12).setOdds(1.0);
        weekThreeMatches.get(13).setOdds(1.0);
        weekThreeMatches.get(14).setOdds(-6.5);
        weekThreeMatches.get(15).setOdds(-2.0);
        Week weekFour = currentSchedule.getWeek(4);
        ArrayList<Match> weekFourMatches = weekFour.getMatches();
        weekFourMatches.get(0).setOdds(6.5);
        weekFourMatches.get(1).setOdds(2.0);
        weekFourMatches.get(2).setOdds(10.5);
        weekFourMatches.get(3).setOdds(1.0);
        weekFourMatches.get(4).setOdds(1.0);
        weekFourMatches.get(5).setOdds(1.0);
        weekFourMatches.get(6).setOdds(6.0);
        weekFourMatches.get(7).setOdds(9.0);
        weekFourMatches.get(8).setOdds(-3.0);
        weekFourMatches.get(9).setOdds(2.5);
        weekFourMatches.get(10).setOdds(-3.0);
        weekFourMatches.get(11).setOdds(-3.0);
        weekFourMatches.get(12).setOdds(9.5);
        weekFourMatches.get(13).setOdds(1.0);
        weekFourMatches.get(14).setOdds(-4.0);
        Week weekFive = currentSchedule.getWeek(5);
        ArrayList<Match> weekFiveMatches = weekFive.getMatches();
        weekFiveMatches.get(0).setOdds(10.5);
        weekFiveMatches.get(1).setOdds(1.0);
        weekFiveMatches.get(2).setOdds(-1.5);
        weekFiveMatches.get(3).setOdds(7.0);
        weekFiveMatches.get(4).setOdds(-3.0);
        weekFiveMatches.get(5).setOdds(3.0);
        weekFiveMatches.get(6).setOdds(-3.0);
        weekFiveMatches.get(7).setOdds(6.0);
        weekFiveMatches.get(8).setOdds(1.0);
        weekFiveMatches.get(9).setOdds(5.5);
        weekFiveMatches.get(10).setOdds(3.0);
        weekFiveMatches.get(11).setOdds(-7.0);
        weekFiveMatches.get(12).setOdds(4.0);
        weekFiveMatches.get(13).setOdds(3.5);
        weekFiveMatches.get(14).setOdds(6.5);
        Week weekSix = currentSchedule.getWeek(6);
        ArrayList<Match> weekSixMatches = weekSix.getMatches();
        weekSixMatches.get(0).setOdds(-3.0);
        weekSixMatches.get(1).setOdds(3.5);
        weekSixMatches.get(2).setOdds(-1.0);
        weekSixMatches.get(3).setOdds(2.5);
        weekSixMatches.get(4).setOdds(-3.0);
        weekSixMatches.get(5).setOdds(1.0);
        weekSixMatches.get(6).setOdds(1.0);
        weekSixMatches.get(7).setOdds(10.5);
        weekSixMatches.get(8).setOdds(2.5);
        weekSixMatches.get(9).setOdds(-3.0);
        weekSixMatches.get(10).setOdds(-7.0);
        weekSixMatches.get(11).setOdds(-3.0);
        weekSixMatches.get(12).setOdds(-3.0);
        weekSixMatches.get(13).setOdds(3.5);
        weekSixMatches.get(14).setOdds(9.5);
        Week weekSeven = currentSchedule.getWeek(7);
        ArrayList<Match> weekSevenMatches = weekSeven.getMatches();
        weekSevenMatches.get(0).setOdds(-2.5);
        weekSevenMatches.get(1).setOdds(6.5);
        weekSevenMatches.get(2).setOdds(4.5);
        weekSevenMatches.get(3).setOdds(-3.0);
        weekSevenMatches.get(4).setOdds(6.5);
        weekSevenMatches.get(5).setOdds(-3.5);
        weekSevenMatches.get(6).setOdds(3.0);
        weekSevenMatches.get(7).setOdds(4.5);
        weekSevenMatches.get(8).setOdds(6.0);
        weekSevenMatches.get(9).setOdds(0.0);
        weekSevenMatches.get(10).setOdds(2.5);
        weekSevenMatches.get(11).setOdds(1.5);
        weekSevenMatches.get(12).setOdds(1.0);
        weekSevenMatches.get(13).setOdds(6.5);
        Week weekEight = currentSchedule.getWeek(8);
        ArrayList<Match> weekEightMatches = weekEight.getMatches();
        weekEightMatches.get(0).setOdds(0.0);
        weekEightMatches.get(1).setOdds(-3.0);
        weekEightMatches.get(2).setOdds(-1.5);
        weekEightMatches.get(3).setOdds(10.0);
        weekEightMatches.get(4).setOdds(7.5);
        weekEightMatches.get(5).setOdds(2.5);
        weekEightMatches.get(6).setOdds(6.0);
        weekEightMatches.get(7).setOdds(6.5);
        weekEightMatches.get(8).setOdds(0.0);
        weekEightMatches.get(9).setOdds(-1.5);
        weekEightMatches.get(10).setOdds(1.0);
        weekEightMatches.get(11).setOdds(8.5);
        weekEightMatches.get(12).setOdds(1.0);
        weekEightMatches.get(13).setOdds(-13.5);
        Week weekNine = currentSchedule.getWeek(9);
        ArrayList<Match> weekNineMatches = weekNine.getMatches();
        weekNineMatches.get(0).setOdds(3.0);
        weekNineMatches.get(1).setOdds(1.5);
        weekNineMatches.get(2).setOdds(5.5);
        weekNineMatches.get(3).setOdds(3.0);
        weekNineMatches.get(4).setOdds(-8.0);
        weekNineMatches.get(5).setOdds(3.0);
        weekNineMatches.get(6).setOdds(0.0);
        weekNineMatches.get(7).setOdds(6.5);
        weekNineMatches.get(8).setOdds(1.5);
        weekNineMatches.get(9).setOdds(2.5);
        weekNineMatches.get(10).setOdds(1.5);
        weekNineMatches.get(11).setOdds(1.0);
        weekNineMatches.get(12).setOdds(6.5);
        Week weekTen = currentSchedule.getWeek(10);
        ArrayList<Match> weekTenMatches = weekTen.getMatches();
        weekTenMatches.get(0).setOdds(5.5);
        weekTenMatches.get(1).setOdds(1.5);
        weekTenMatches.get(2).setOdds(17.0);
        weekTenMatches.get(3).setOdds(8.0);
        weekTenMatches.get(4).setOdds(3.5);
        weekTenMatches.get(5).setOdds(6.5);
        weekTenMatches.get(6).setOdds(9.0);
        weekTenMatches.get(7).setOdds(-4.5);
        weekTenMatches.get(8).setOdds(-5.0);
        weekTenMatches.get(9).setOdds(-4.0);
        weekTenMatches.get(10).setOdds(-9.5);
        weekTenMatches.get(11).setOdds(10.0);
        weekTenMatches.get(12).setOdds(6.0);
        weekTenMatches.get(13).setOdds(2.5);
        Week weekEleven = currentSchedule.getWeek(11);
        ArrayList<Match> weekElevenMatches = weekEleven.getMatches();
        weekElevenMatches.get(0).setOdds(2.5);
        weekElevenMatches.get(1).setOdds(7.0);
        weekElevenMatches.get(2).setOdds(4.5);
        weekElevenMatches.get(3).setOdds(-3.5);
        weekElevenMatches.get(4).setOdds(3.0);
        weekElevenMatches.get(5).setOdds(0.0);
        weekElevenMatches.get(6).setOdds(-3.0);
        weekElevenMatches.get(7).setOdds(-4.5);
        weekElevenMatches.get(8).setOdds(0.0);
        weekElevenMatches.get(9).setOdds(3.5);
        weekElevenMatches.get(10).setOdds(7.0);
        weekElevenMatches.get(11).setOdds(3.0);
        weekElevenMatches.get(12).setOdds(1.5);



    }

    private void createCurrentSeasonTeamsFromDb(Cursor standingsCursor) {

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
            int teamDraws = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_CURRENT_DRAWS));
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
                            offRating, defRating, division, this, teamWins, teamLosses, teamDraws, divisionWins, divisionLosses, winLossPct, divWinLossPct, playoffEligible, teamUri, TeamEntry.CURRENT_SEASON_YES));

            teamUserElos.put(teamName, teamUserElo);

        }

        mModel.setSeasonTeamList(teamList);

        standingsCursor.close();
    }

    @Override
    public void queryCurrentSeasonMatches(int week, boolean singleMatch, int queryFrom) {
        mModel.queryCurrentSeasonMatches(week, singleMatch, queryFrom);
    }

    @Override
    public void resetSeason() {
        //When season is reset, delete all data in the database
        mModel.deleteAllData();
    }

    @Override
    public void resetSimulatorTeamLastSeasonElos() {
        //Reset teams Elos to last seasons Elo Values
        ArrayList<Team> teamList = mModel.getSimulatorTeamArrayList();
        for (Team team : teamList) {
            team.resetElo();
        }
    }

    @Override
    public void resetSimulatorTeamCurrentSeasonElos() {
        //Reset teams Elos to current season Elo Values
        ArrayList<Team> teamList = mModel.getSimulatorTeamArrayList();
        for (Team team : teamList) {
            team.setCurrentSeasonElos();
        }

    }

    @Override
    public void resetCurrentSeasonTeamCurrentSeasonElos() {
        //Reset teams Elos to current season Elo Values
        ArrayList<Team> teamList = mModel.getSeasonTeamArrayList();
        for (Team team : teamList) {
            team.setCurrentSeasonElos();
        }

    }

    @Override
    public void resetSimulatorTeamUserElos() {
        //Reset team Elo values to be user defined values
        HashMap<String, Team> teamMap = mModel.getSimulatorTeamList();
        HashMap<String, Double> teamElos = mModel.getTeamEloMap();

        for (String teamName : teamMap.keySet()) {
            teamMap.get(teamName).setElo(teamElos.get(teamName));
            teamMap.get(teamName).setUserElo();
        }


    }

    @Override
    public void resetSimulatorTeamWinsLosses() {

        for (Team team : mModel.getSimulatorTeamArrayList()) {
            team.resetWinsLosses();
        }

    }

    @Override
    public void setTeamUserElos() {
        //Set team User Elos to be whatever elo values were manually set by the user
        //Provide the data to the model in the form of a hashmap (this will be used to reset the values
        // back to default when the season is reset)

        HashMap<String, Double> teamUserElos = new HashMap<>();
        ArrayList<Team> teamList = mModel.getSimulatorTeamArrayList();

        for (Team team : teamList) {
            team.setUserElo();
            teamUserElos.put(team.getName(), team.getUserElo());
        }

        mModel.setTeamEloMap(teamUserElos);

    }

    @Override
    public void dataDeleted() {
        //Callback after data has been deleted
        //If this.view is not null, main activity is loaded so we will call on data deleted method of main activity
        if (this.view != null) {
            this.view.onDataDeleted();
        }
    }

    private void createSimulatorTeamsFromDb(Cursor standingsCursor) {

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
            int teamDraws = standingsCursor.getInt(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_CURRENT_DRAWS));
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
                            offRating, defRating, division, this, teamWins, teamLosses, teamDraws, divisionWins, divisionLosses, winLossPct, divWinLossPct, playoffEligible, teamUri, TeamEntry.CURRENT_SEASON_NO));

            teamUserElos.put(teamName, teamUserElo);

        }

        mModel.setSimulatorTeamList(teamList);
        mModel.setTeamEloMap(teamUserElos);
        mModel.createTeamLogoMap();


        standingsCursor.close();

    }

    @Override
    public void destroyPresenter() {
        mModel.destroyModel();
    }


    private void createSimulatorTeams() {

        //Create teams for the first time

        HashMap<String, Team> simulatorTeamList = new HashMap();
        simulatorTeamList.put(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING,
                new Team(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING, NFLConstants.TEAM_ARIZONA_CARDINALS_SHORT_STRING, NFLConstants.TEAM_ARIZONA_CARDINALS_ELO, NFLConstants.TEAM_ARIZONA_CARDINALS_FUTURE_RANKING,
                        NFLConstants.TEAM_ARIZONA_CARDINALS_OFFRAT, NFLConstants.TEAM_ARIZONA_CARDINALS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        simulatorTeamList.put(NFLConstants.TEAM_ATLANTA_FALCONS_STRING,
                new Team(NFLConstants.TEAM_ATLANTA_FALCONS_STRING, NFLConstants.TEAM_ATLANTA_FALCONS_SHORT_STRING, NFLConstants.TEAM_ATLANTA_FALCONS_ELO, NFLConstants.TEAM_ATLANTA_FALCONS_FUTURE_RANKING,
                        NFLConstants.TEAM_ATLANTA_FALCONS_OFFRAT, NFLConstants.TEAM_ATLANTA_FALCONS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING,
                new Team(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING, NFLConstants.TEAM_BALTIMORE_RAVENS_SHORT_STRING, NFLConstants.TEAM_BALTIMORE_RAVENS_ELO, NFLConstants.TEAM_BALTIMORE_RAVENS_FUTURE_RANKING,
                        NFLConstants.TEAM_BALTIMORE_RAVENS_OFFRAT, NFLConstants.TEAM_BALTIMORE_RAVENS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_BUFFALO_BILLS_STRING,
                new Team(NFLConstants.TEAM_BUFFALO_BILLS_STRING, NFLConstants.TEAM_BUFFALO_BILLS_SHORT_STRING, NFLConstants.TEAM_BUFFALO_BILLS_ELO, NFLConstants.TEAM_BUFFALO_BILLS_FUTURE_RANKING,
                        NFLConstants.TEAM_BUFFALO_BILLS_OFFRAT, NFLConstants.TEAM_BUFFALO_BILLS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        simulatorTeamList.put(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING,
                new Team(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING, NFLConstants.TEAM_CAROLINA_PANTHERS_SHORT_STRING, NFLConstants.TEAM_CAROLINA_PANTHERS_ELO, NFLConstants.TEAM_CAROLINA_PANTHERS_FUTURE_RANKING,
                        NFLConstants.TEAM_CAROLINA_PANTHERS_OFFRAT, NFLConstants.TEAM_CAROLINA_PANTHERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_CHICAGO_BEARS_STRING,
                new Team(NFLConstants.TEAM_CHICAGO_BEARS_STRING, NFLConstants.TEAM_CHICAGO_BEARS_SHORT_STRING, NFLConstants.TEAM_CHICAGO_BEARS_ELO, NFLConstants.TEAM_CHICAGO_BEARS_FUTURE_RANKING,
                        NFLConstants.TEAM_CHICAGO_BEARS_OFFRAT, NFLConstants.TEAM_CHICAGO_BEARS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING,
                new Team(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING, NFLConstants.TEAM_CINCINNATI_BENGALS_SHORT_STRING, NFLConstants.TEAM_CINCINNATI_BENGALS_ELO, NFLConstants.TEAM_CINCINNATI_BENGALS_FUTURE_RANKING,
                        NFLConstants.TEAM_CINCINNATI_BENGALS_OFFRAT, NFLConstants.TEAM_CINCINNATI_BENGALS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING,
                new Team(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING, NFLConstants.TEAM_CLEVELAND_BROWNS_SHORT_STRING, NFLConstants.TEAM_CLEVELAND_BROWNS_ELO, NFLConstants.TEAM_CLEVELAND_BROWNS_FUTURE_RANKING,
                        NFLConstants.TEAM_CLEVELAND_BROWNS_OFFRAT, NFLConstants.TEAM_CLEVELAND_BROWNS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_DALLAS_COWBOYS_STRING,
                new Team(NFLConstants.TEAM_DALLAS_COWBOYS_STRING, NFLConstants.TEAM_DALLAS_COWBOYS_SHORT_STRING, NFLConstants.TEAM_DALLAS_COWBOYS_ELO, NFLConstants.TEAM_DALLAS_COWBOYS_FUTURE_RANKING,
                        NFLConstants.TEAM_DALLAS_COWBOYS_OFFRAT, NFLConstants.TEAM_DALLAS_COWBOYS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        simulatorTeamList.put(NFLConstants.TEAM_DENVER_BRONCOS_STRING,
                new Team(NFLConstants.TEAM_DENVER_BRONCOS_STRING, NFLConstants.TEAM_DENVER_BRONCOS_SHORT_STRING, NFLConstants.TEAM_DENVER_BRONCOS_ELO, NFLConstants.TEAM_DENVER_BRONCOS_FUTURE_RANKING,
                        NFLConstants.TEAM_DENVER_BRONCOS_OFFRAT, NFLConstants.TEAM_DENVER_BRONCOS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        simulatorTeamList.put(NFLConstants.TEAM_DETROIT_LIONS_STRING,
                new Team(NFLConstants.TEAM_DETROIT_LIONS_STRING, NFLConstants.TEAM_DETROIT_LIONS_SHORT_STRING, NFLConstants.TEAM_DETROIT_LIONS_ELO, NFLConstants.TEAM_DETROIT_LIONS_FUTURE_RANKING,
                        NFLConstants.TEAM_DETROIT_LIONS_OFFRAT, NFLConstants.TEAM_DETROIT_LIONS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_GREENBAY_PACKERS_STRING,
                new Team(NFLConstants.TEAM_GREENBAY_PACKERS_STRING, NFLConstants.TEAM_GREENBAY_PACKERS_SHORT_STRING, NFLConstants.TEAM_GREENBAY_PACKERS_ELO, NFLConstants.TEAM_GREENBAY_PACKERS_FUTURE_RANKING,
                        NFLConstants.TEAM_GREENBAY_PACKERS_OFFRAT, NFLConstants.TEAM_GREENBAY_PACKERS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_HOUSTON_TEXANS_STRING,
                new Team(NFLConstants.TEAM_HOUSTON_TEXANS_STRING, NFLConstants.TEAM_HOUSTON_TEXANS_SHORT_STRING, NFLConstants.TEAM_HOUSTON_TEXANS_ELO, NFLConstants.TEAM_HOUSTON_TEXANS_FUTURE_RANKING,
                        NFLConstants.TEAM_HOUSTON_TEXANS_OFFRAT, NFLConstants.TEAM_HOUSTON_TEXANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING,
                new Team(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING, NFLConstants.TEAM_INDIANAPOLIS_COLTS_SHORT_STRING, NFLConstants.TEAM_INDIANAPOLIS_COLTS_ELO, NFLConstants.TEAM_INDIANAPOLIS_COLTS_FUTURE_RANKING,
                        NFLConstants.TEAM_INDIANAPOLIS_COLTS_OFFRAT, NFLConstants.TEAM_INDIANAPOLIS_COLTS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING,
                new Team(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_SHORT_STRING, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_ELO, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_FUTURE_RANKING,
                        NFLConstants.TEAM_JACKSONVILLE_JAGUARS_OFFRAT, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING,
                new Team(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING, NFLConstants.TEAM_KANSASCITY_CHIEFS_SHORT_STRING, NFLConstants.TEAM_KANSASCITY_CHIEFS_ELO, NFLConstants.TEAM_KANSASCITY_CHIEFS_FUTURE_RANKING,
                        NFLConstants.TEAM_KANSASCITY_CHIEFS_OFFRAT, NFLConstants.TEAM_KANSASCITY_CHIEFS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        simulatorTeamList.put(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING,
                new Team(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING, NFLConstants.TEAM_LOSANGELES_CHARGERS_SHORT_STRING, NFLConstants.TEAM_LOSANGELES_CHARGERS_ELO, NFLConstants.TEAM_LOSANGELES_CHARGERS_FUTURE_RANKING,
                        NFLConstants.TEAM_LOSANGELES_CHARGERS_OFFRAT, NFLConstants.TEAM_LOSANGELES_CHARGERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        simulatorTeamList.put(NFLConstants.TEAM_LOSANGELES_RAMS_STRING,
                new Team(NFLConstants.TEAM_LOSANGELES_RAMS_STRING, NFLConstants.TEAM_LOSANGELES_RAMS_SHORT_STRING, NFLConstants.TEAM_LOSANGELES_RAMS_ELO, NFLConstants.TEAM_LOSANGELES_RAMS_FUTURE_RANKING,
                        NFLConstants.TEAM_LOSANGELES_RAMS_OFFRAT, NFLConstants.TEAM_LOSANGELES_RAMS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        simulatorTeamList.put(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING,
                new Team(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING, NFLConstants.TEAM_MIAMI_DOLPHINS_SHORT_STRING, NFLConstants.TEAM_MIAMI_DOLPHINS_ELO, NFLConstants.TEAM_MIAMI_DOLPHINS_FUTURE_RANKING,
                        NFLConstants.TEAM_MIAMI_DOLPHINS_OFFRAT, NFLConstants.TEAM_MIAMI_DOLPHINS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        simulatorTeamList.put(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING,
                new Team(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING, NFLConstants.TEAM_MINNESOTA_VIKINGS_SHORT_STRING, NFLConstants.TEAM_MINNESOTA_VIKINGS_ELO, NFLConstants.TEAM_MINNESOTA_VIKINGS_FUTURE_RANKING,
                        NFLConstants.TEAM_MINNESOTA_VIKINGS_OFFRAT, NFLConstants.TEAM_MINNESOTA_VIKINGS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING,
                new Team(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_SHORT_STRING, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_ELO, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWENGLAND_PATRIOTS_OFFRAT, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        simulatorTeamList.put(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING,
                new Team(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING, NFLConstants.TEAM_NEWORLEANS_SAINTS_SHORT_STRING, NFLConstants.TEAM_NEWORLEANS_SAINTS_ELO, NFLConstants.TEAM_NEWORLEANS_SAINTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWORLEANS_SAINTS_OFFRAT, NFLConstants.TEAM_NEWORLEANS_SAINTS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_NEWYORK_GIANTS_STRING,
                new Team(NFLConstants.TEAM_NEWYORK_GIANTS_STRING, NFLConstants.TEAM_NEWYORK_GIANTS_SHORT_STRING, NFLConstants.TEAM_NEWYORK_GIANTS_ELO, NFLConstants.TEAM_NEWYORK_GIANTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWYORK_GIANTS_OFFRAT, NFLConstants.TEAM_NEWYORK_GIANTS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        simulatorTeamList.put(NFLConstants.TEAM_NEWYORK_JETS_STRING,
                new Team(NFLConstants.TEAM_NEWYORK_JETS_STRING, NFLConstants.TEAM_NEWYORK_JETS_SHORT_STRING, NFLConstants.TEAM_NEWYORK_JETS_ELO, NFLConstants.TEAM_NEWYORK_JETS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWYORK_JETS_OFFRAT, NFLConstants.TEAM_NEWYORK_JETS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        simulatorTeamList.put(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING,
                new Team(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING, NFLConstants.TEAM_OAKLAND_RAIDERS_SHORT_STRING, NFLConstants.TEAM_OAKLAND_RAIDERS_ELO, NFLConstants.TEAM_OAKLAND_RAIDERS_FUTURE_RANKING,
                        NFLConstants.TEAM_OAKLAND_RAIDERS_OFFRAT, NFLConstants.TEAM_OAKLAND_RAIDERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        simulatorTeamList.put(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING,
                new Team(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING, NFLConstants.TEAM_PHILADELPHIA_EAGLES_SHORT_STRING, NFLConstants.TEAM_PHILADELPHIA_EAGLES_ELO, NFLConstants.TEAM_PHILADELPHIA_EAGLES_FUTURE_RANKING,
                        NFLConstants.TEAM_PHILADELPHIA_EAGLES_OFFRAT, NFLConstants.TEAM_PHILADELPHIA_EAGLES_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        simulatorTeamList.put(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING,
                new Team(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING, NFLConstants.TEAM_PITTSBURGH_STEELERS_SHORT_STRING, NFLConstants.TEAM_PITTSBURGH_STEELERS_ELO, NFLConstants.TEAM_PITTSBURGH_STEELERS_FUTURE_RANKING,
                        NFLConstants.TEAM_PITTSBURGH_STEELERS_OFFRAT, NFLConstants.TEAM_PITTSBURGH_STEELERS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING,
                new Team(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING, NFLConstants.TEAM_SANFRANCISCO_49ERS_SHORT_STRING, NFLConstants.TEAM_SANFRANCISCO_49ERS_ELO, NFLConstants.TEAM_SANFRANCISCO_49ERS_FUTURE_RANKING,
                        NFLConstants.TEAM_SANFRANCISCO_49ERS_OFFRAT, NFLConstants.TEAM_SANFRANCISCO_49ERS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        simulatorTeamList.put(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING,
                new Team(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING, NFLConstants.TEAM_SEATTLE_SEAHAWKS_SHORT_STRING, NFLConstants.TEAM_SEATTLE_SEAHAWKS_ELO, NFLConstants.TEAM_SEATTLE_SEAHAWKS_FUTURE_RANKING,
                        NFLConstants.TEAM_SEATTLE_SEAHAWKS_OFFRAT, NFLConstants.TEAM_SEATTLE_SEAHAWKS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        simulatorTeamList.put(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING,
                new Team(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_SHORT_STRING, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_ELO, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_FUTURE_RANKING,
                        NFLConstants.TEAM_TAMPABAY_BUCCANEERS_OFFRAT, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_TENNESSEE_TITANS_STRING,
                new Team(NFLConstants.TEAM_TENNESSEE_TITANS_STRING, NFLConstants.TEAM_TENNESSEE_TITANS_SHORT_STRING, NFLConstants.TEAM_TENNESSEE_TITANS_ELO, NFLConstants.TEAM_TENNESSEE_TITANS_FUTURE_RANKING,
                        NFLConstants.TEAM_TENNESSEE_TITANS_OFFRAT, NFLConstants.TEAM_TENNESSEE_TITANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        simulatorTeamList.put(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING,
                new Team(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING, NFLConstants.TEAM_WASHINGTON_REDSKINS_SHORT_STRING, NFLConstants.TEAM_WASHINGTON_REDSKINS_ELO, NFLConstants.TEAM_WASHINGTON_REDSKINS_FUTURE_RANKING,
                        NFLConstants.TEAM_WASHINGTON_REDSKINS_OFFRAT, NFLConstants.TEAM_WASHINGTON_REDSKINS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        mModel.setSimulatorTeamList(simulatorTeamList);

        mModel.createTeamLogoMap();
    }

    private void createSeasonTeams() {

        //Create teams for the first time

        HashMap<String, Team> seasonTeamList = new HashMap();
        seasonTeamList.put(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING,
                new Team(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING, NFLConstants.TEAM_ARIZONA_CARDINALS_SHORT_STRING, NFLConstants.TEAM_ARIZONA_CARDINALS_ELO, NFLConstants.TEAM_ARIZONA_CARDINALS_FUTURE_RANKING,
                        NFLConstants.TEAM_ARIZONA_CARDINALS_OFFRAT, NFLConstants.TEAM_ARIZONA_CARDINALS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_ATLANTA_FALCONS_STRING,
                new Team(NFLConstants.TEAM_ATLANTA_FALCONS_STRING, NFLConstants.TEAM_ATLANTA_FALCONS_SHORT_STRING, NFLConstants.TEAM_ATLANTA_FALCONS_ELO, NFLConstants.TEAM_ATLANTA_FALCONS_FUTURE_RANKING,
                        NFLConstants.TEAM_ATLANTA_FALCONS_OFFRAT, NFLConstants.TEAM_ATLANTA_FALCONS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING,
                new Team(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING, NFLConstants.TEAM_BALTIMORE_RAVENS_SHORT_STRING, NFLConstants.TEAM_BALTIMORE_RAVENS_ELO, NFLConstants.TEAM_BALTIMORE_RAVENS_FUTURE_RANKING,
                        NFLConstants.TEAM_BALTIMORE_RAVENS_OFFRAT, NFLConstants.TEAM_BALTIMORE_RAVENS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_BUFFALO_BILLS_STRING,
                new Team(NFLConstants.TEAM_BUFFALO_BILLS_STRING, NFLConstants.TEAM_BUFFALO_BILLS_SHORT_STRING, NFLConstants.TEAM_BUFFALO_BILLS_ELO, NFLConstants.TEAM_BUFFALO_BILLS_FUTURE_RANKING,
                        NFLConstants.TEAM_BUFFALO_BILLS_OFFRAT, NFLConstants.TEAM_BUFFALO_BILLS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING,
                new Team(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING, NFLConstants.TEAM_CAROLINA_PANTHERS_SHORT_STRING, NFLConstants.TEAM_CAROLINA_PANTHERS_ELO, NFLConstants.TEAM_CAROLINA_PANTHERS_FUTURE_RANKING,
                        NFLConstants.TEAM_CAROLINA_PANTHERS_OFFRAT, NFLConstants.TEAM_CAROLINA_PANTHERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_CHICAGO_BEARS_STRING,
                new Team(NFLConstants.TEAM_CHICAGO_BEARS_STRING, NFLConstants.TEAM_CHICAGO_BEARS_SHORT_STRING, NFLConstants.TEAM_CHICAGO_BEARS_ELO, NFLConstants.TEAM_CHICAGO_BEARS_FUTURE_RANKING,
                        NFLConstants.TEAM_CHICAGO_BEARS_OFFRAT, NFLConstants.TEAM_CHICAGO_BEARS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING,
                new Team(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING, NFLConstants.TEAM_CINCINNATI_BENGALS_SHORT_STRING, NFLConstants.TEAM_CINCINNATI_BENGALS_ELO, NFLConstants.TEAM_CINCINNATI_BENGALS_FUTURE_RANKING,
                        NFLConstants.TEAM_CINCINNATI_BENGALS_OFFRAT, NFLConstants.TEAM_CINCINNATI_BENGALS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING,
                new Team(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING, NFLConstants.TEAM_CLEVELAND_BROWNS_SHORT_STRING, NFLConstants.TEAM_CLEVELAND_BROWNS_ELO, NFLConstants.TEAM_CLEVELAND_BROWNS_FUTURE_RANKING,
                        NFLConstants.TEAM_CLEVELAND_BROWNS_OFFRAT, NFLConstants.TEAM_CLEVELAND_BROWNS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_DALLAS_COWBOYS_STRING,
                new Team(NFLConstants.TEAM_DALLAS_COWBOYS_STRING, NFLConstants.TEAM_DALLAS_COWBOYS_SHORT_STRING, NFLConstants.TEAM_DALLAS_COWBOYS_ELO, NFLConstants.TEAM_DALLAS_COWBOYS_FUTURE_RANKING,
                        NFLConstants.TEAM_DALLAS_COWBOYS_OFFRAT, NFLConstants.TEAM_DALLAS_COWBOYS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_DENVER_BRONCOS_STRING,
                new Team(NFLConstants.TEAM_DENVER_BRONCOS_STRING, NFLConstants.TEAM_DENVER_BRONCOS_SHORT_STRING, NFLConstants.TEAM_DENVER_BRONCOS_ELO, NFLConstants.TEAM_DENVER_BRONCOS_FUTURE_RANKING,
                        NFLConstants.TEAM_DENVER_BRONCOS_OFFRAT, NFLConstants.TEAM_DENVER_BRONCOS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_DETROIT_LIONS_STRING,
                new Team(NFLConstants.TEAM_DETROIT_LIONS_STRING, NFLConstants.TEAM_DETROIT_LIONS_SHORT_STRING, NFLConstants.TEAM_DETROIT_LIONS_ELO, NFLConstants.TEAM_DETROIT_LIONS_FUTURE_RANKING,
                        NFLConstants.TEAM_DETROIT_LIONS_OFFRAT, NFLConstants.TEAM_DETROIT_LIONS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_GREENBAY_PACKERS_STRING,
                new Team(NFLConstants.TEAM_GREENBAY_PACKERS_STRING, NFLConstants.TEAM_GREENBAY_PACKERS_SHORT_STRING, NFLConstants.TEAM_GREENBAY_PACKERS_ELO, NFLConstants.TEAM_GREENBAY_PACKERS_FUTURE_RANKING,
                        NFLConstants.TEAM_GREENBAY_PACKERS_OFFRAT, NFLConstants.TEAM_GREENBAY_PACKERS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_HOUSTON_TEXANS_STRING,
                new Team(NFLConstants.TEAM_HOUSTON_TEXANS_STRING, NFLConstants.TEAM_HOUSTON_TEXANS_SHORT_STRING, NFLConstants.TEAM_HOUSTON_TEXANS_ELO, NFLConstants.TEAM_HOUSTON_TEXANS_FUTURE_RANKING,
                        NFLConstants.TEAM_HOUSTON_TEXANS_OFFRAT, NFLConstants.TEAM_HOUSTON_TEXANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING,
                new Team(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING, NFLConstants.TEAM_INDIANAPOLIS_COLTS_SHORT_STRING, NFLConstants.TEAM_INDIANAPOLIS_COLTS_ELO, NFLConstants.TEAM_INDIANAPOLIS_COLTS_FUTURE_RANKING,
                        NFLConstants.TEAM_INDIANAPOLIS_COLTS_OFFRAT, NFLConstants.TEAM_INDIANAPOLIS_COLTS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING,
                new Team(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_SHORT_STRING, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_ELO, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_FUTURE_RANKING,
                        NFLConstants.TEAM_JACKSONVILLE_JAGUARS_OFFRAT, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING,
                new Team(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING, NFLConstants.TEAM_KANSASCITY_CHIEFS_SHORT_STRING, NFLConstants.TEAM_KANSASCITY_CHIEFS_ELO, NFLConstants.TEAM_KANSASCITY_CHIEFS_FUTURE_RANKING,
                        NFLConstants.TEAM_KANSASCITY_CHIEFS_OFFRAT, NFLConstants.TEAM_KANSASCITY_CHIEFS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING,
                new Team(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING, NFLConstants.TEAM_LOSANGELES_CHARGERS_SHORT_STRING, NFLConstants.TEAM_LOSANGELES_CHARGERS_ELO, NFLConstants.TEAM_LOSANGELES_CHARGERS_FUTURE_RANKING,
                        NFLConstants.TEAM_LOSANGELES_CHARGERS_OFFRAT, NFLConstants.TEAM_LOSANGELES_CHARGERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_LOSANGELES_RAMS_STRING,
                new Team(NFLConstants.TEAM_LOSANGELES_RAMS_STRING, NFLConstants.TEAM_LOSANGELES_RAMS_SHORT_STRING, NFLConstants.TEAM_LOSANGELES_RAMS_ELO, NFLConstants.TEAM_LOSANGELES_RAMS_FUTURE_RANKING,
                        NFLConstants.TEAM_LOSANGELES_RAMS_OFFRAT, NFLConstants.TEAM_LOSANGELES_RAMS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING,
                new Team(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING, NFLConstants.TEAM_MIAMI_DOLPHINS_SHORT_STRING, NFLConstants.TEAM_MIAMI_DOLPHINS_ELO, NFLConstants.TEAM_MIAMI_DOLPHINS_FUTURE_RANKING,
                        NFLConstants.TEAM_MIAMI_DOLPHINS_OFFRAT, NFLConstants.TEAM_MIAMI_DOLPHINS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING,
                new Team(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING, NFLConstants.TEAM_MINNESOTA_VIKINGS_SHORT_STRING, NFLConstants.TEAM_MINNESOTA_VIKINGS_ELO, NFLConstants.TEAM_MINNESOTA_VIKINGS_FUTURE_RANKING,
                        NFLConstants.TEAM_MINNESOTA_VIKINGS_OFFRAT, NFLConstants.TEAM_MINNESOTA_VIKINGS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING,
                new Team(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_SHORT_STRING, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_ELO, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWENGLAND_PATRIOTS_OFFRAT, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING,
                new Team(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING, NFLConstants.TEAM_NEWORLEANS_SAINTS_SHORT_STRING, NFLConstants.TEAM_NEWORLEANS_SAINTS_ELO, NFLConstants.TEAM_NEWORLEANS_SAINTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWORLEANS_SAINTS_OFFRAT, NFLConstants.TEAM_NEWORLEANS_SAINTS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_NEWYORK_GIANTS_STRING,
                new Team(NFLConstants.TEAM_NEWYORK_GIANTS_STRING, NFLConstants.TEAM_NEWYORK_GIANTS_SHORT_STRING, NFLConstants.TEAM_NEWYORK_GIANTS_ELO, NFLConstants.TEAM_NEWYORK_GIANTS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWYORK_GIANTS_OFFRAT, NFLConstants.TEAM_NEWYORK_GIANTS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_NEWYORK_JETS_STRING,
                new Team(NFLConstants.TEAM_NEWYORK_JETS_STRING, NFLConstants.TEAM_NEWYORK_JETS_SHORT_STRING, NFLConstants.TEAM_NEWYORK_JETS_ELO, NFLConstants.TEAM_NEWYORK_JETS_FUTURE_RANKING,
                        NFLConstants.TEAM_NEWYORK_JETS_OFFRAT, NFLConstants.TEAM_NEWYORK_JETS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING,
                new Team(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING, NFLConstants.TEAM_OAKLAND_RAIDERS_SHORT_STRING, NFLConstants.TEAM_OAKLAND_RAIDERS_ELO, NFLConstants.TEAM_OAKLAND_RAIDERS_FUTURE_RANKING,
                        NFLConstants.TEAM_OAKLAND_RAIDERS_OFFRAT, NFLConstants.TEAM_OAKLAND_RAIDERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING,
                new Team(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING, NFLConstants.TEAM_PHILADELPHIA_EAGLES_SHORT_STRING, NFLConstants.TEAM_PHILADELPHIA_EAGLES_ELO, NFLConstants.TEAM_PHILADELPHIA_EAGLES_FUTURE_RANKING,
                        NFLConstants.TEAM_PHILADELPHIA_EAGLES_OFFRAT, NFLConstants.TEAM_PHILADELPHIA_EAGLES_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING,
                new Team(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING, NFLConstants.TEAM_PITTSBURGH_STEELERS_SHORT_STRING, NFLConstants.TEAM_PITTSBURGH_STEELERS_ELO, NFLConstants.TEAM_PITTSBURGH_STEELERS_FUTURE_RANKING,
                        NFLConstants.TEAM_PITTSBURGH_STEELERS_OFFRAT, NFLConstants.TEAM_PITTSBURGH_STEELERS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING,
                new Team(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING, NFLConstants.TEAM_SANFRANCISCO_49ERS_SHORT_STRING, NFLConstants.TEAM_SANFRANCISCO_49ERS_ELO, NFLConstants.TEAM_SANFRANCISCO_49ERS_FUTURE_RANKING,
                        NFLConstants.TEAM_SANFRANCISCO_49ERS_OFFRAT, NFLConstants.TEAM_SANFRANCISCO_49ERS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING,
                new Team(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING, NFLConstants.TEAM_SEATTLE_SEAHAWKS_SHORT_STRING, NFLConstants.TEAM_SEATTLE_SEAHAWKS_ELO, NFLConstants.TEAM_SEATTLE_SEAHAWKS_FUTURE_RANKING,
                        NFLConstants.TEAM_SEATTLE_SEAHAWKS_OFFRAT, NFLConstants.TEAM_SEATTLE_SEAHAWKS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING,
                new Team(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_SHORT_STRING, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_ELO, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_FUTURE_RANKING,
                        NFLConstants.TEAM_TAMPABAY_BUCCANEERS_OFFRAT, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_TENNESSEE_TITANS_STRING,
                new Team(NFLConstants.TEAM_TENNESSEE_TITANS_STRING, NFLConstants.TEAM_TENNESSEE_TITANS_SHORT_STRING, NFLConstants.TEAM_TENNESSEE_TITANS_ELO, NFLConstants.TEAM_TENNESSEE_TITANS_FUTURE_RANKING,
                        NFLConstants.TEAM_TENNESSEE_TITANS_OFFRAT, NFLConstants.TEAM_TENNESSEE_TITANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this, TeamEntry.CURRENT_SEASON_YES));
        seasonTeamList.put(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING,
                new Team(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING, NFLConstants.TEAM_WASHINGTON_REDSKINS_SHORT_STRING, NFLConstants.TEAM_WASHINGTON_REDSKINS_ELO, NFLConstants.TEAM_WASHINGTON_REDSKINS_FUTURE_RANKING,
                        NFLConstants.TEAM_WASHINGTON_REDSKINS_OFFRAT, NFLConstants.TEAM_WASHINGTON_REDSKINS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this, TeamEntry.CURRENT_SEASON_YES));
        mModel.setSeasonTeamList(seasonTeamList);

    }

    private void createSimulatorSchedule() {

        //Initialize all schedule, weeks, and matches.  Add weeks to schedule.
        Schedule simulatorSeasonSchedule = new Schedule();
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
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekOne.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 1, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwo.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThree.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFour.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFive.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSix.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEight.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekNine.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekEleven.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekTwelve.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekThirteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFourteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekFifteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSixteen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        weekSeventeen.addMatch(new Match(mModel.getSimulatorTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSimulatorTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
        simulatorSeasonSchedule.addWeek(weekOne);
        simulatorSeasonSchedule.addWeek(weekTwo);
        simulatorSeasonSchedule.addWeek(weekThree);
        simulatorSeasonSchedule.addWeek(weekFour);
        simulatorSeasonSchedule.addWeek(weekFive);
        simulatorSeasonSchedule.addWeek(weekSix);
        simulatorSeasonSchedule.addWeek(weekSeven);
        simulatorSeasonSchedule.addWeek(weekEight);
        simulatorSeasonSchedule.addWeek(weekNine);
        simulatorSeasonSchedule.addWeek(weekTen);
        simulatorSeasonSchedule.addWeek(weekEleven);
        simulatorSeasonSchedule.addWeek(weekTwelve);
        simulatorSeasonSchedule.addWeek(weekThirteen);
        simulatorSeasonSchedule.addWeek(weekFourteen);
        simulatorSeasonSchedule.addWeek(weekFifteen);
        simulatorSeasonSchedule.addWeek(weekSixteen);
        simulatorSeasonSchedule.addWeek(weekSeventeen);

        mModel.setSimulatorSchedule(simulatorSeasonSchedule);


    }

    private void createSeasonSchedule() {
        //Initialize all schedule, weeks, and matches.  Add weeks to schedule.
        Schedule currentSeasonSchedule = new Schedule();
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
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 1, this, 1.0, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 1, this, 3.0, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 1, this, 7.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 1, this, 9.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 1, this, 6.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 1, this, 6.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 1, this, -1.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 1, this, -3.0, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 1, this, -3.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 1, this, 3.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 1, this, 3.0, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 1, this, 1.0, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 1, this, 3.0, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 1, this, 7.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 1, this, 6.5, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekOne.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 1, this, -4.0, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwo.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 2, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThree.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 3, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFour.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 4, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFive.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 5, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSix.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 6, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 7, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEight.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 8, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekNine.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 9, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 10, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekEleven.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 11, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekTwelve.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 12, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekThirteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 13, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFourteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 14, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekFifteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 15, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSixteen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 16, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        weekSeventeen.addMatch(new Match(mModel.getSeasonTeamList().get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mModel.getSeasonTeamList().get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 17, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_YES));
        currentSeasonSchedule.addWeek(weekOne);
        currentSeasonSchedule.addWeek(weekTwo);
        currentSeasonSchedule.addWeek(weekThree);
        currentSeasonSchedule.addWeek(weekFour);
        currentSeasonSchedule.addWeek(weekFive);
        currentSeasonSchedule.addWeek(weekSix);
        currentSeasonSchedule.addWeek(weekSeven);
        currentSeasonSchedule.addWeek(weekEight);
        currentSeasonSchedule.addWeek(weekNine);
        currentSeasonSchedule.addWeek(weekTen);
        currentSeasonSchedule.addWeek(weekEleven);
        currentSeasonSchedule.addWeek(weekTwelve);
        currentSeasonSchedule.addWeek(weekThirteen);
        currentSeasonSchedule.addWeek(weekFourteen);
        currentSeasonSchedule.addWeek(weekFifteen);
        currentSeasonSchedule.addWeek(weekSixteen);
        currentSeasonSchedule.addWeek(weekSeventeen);

        mModel.setSeasonSchedule(currentSeasonSchedule);

    }

    @Override
    public void simulateSeason() {
        //From week 1 to week 17 (full season), simulate the season
        while (mCurrentSimulatorWeek <= 17) {
            mModel.getSimulatorSchedule().getWeek(mCurrentSimulatorWeek).simulate(true);
            mCurrentSimulatorWeek++;
        }

        //After the season  is complete, query the standings (and display them)
        generateAndSetPlayoffSeeds(SimulatorPresenter.SEASON_TYPE_SIMULATOR);
        mModel.querySimulatorStandings(SimulatorModel.QUERY_STANDINGS_PLAYOFF);
        //Query all weeks that have already occurred;
        mModel.querySimulatorMatches(mCurrentSimulatorWeek - 1, false, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
    }

    @Override
    public void simulateTestSeason() {

        if (mCurrentTestSimulations > 0) {
            //If this is not the 1st simulation, update the completed game scores before completing the rest of the simulation
            updateSimulatorCompletedGameScores();
        }

        //From week 1 to week 17 (full season), simulate the season
        while (mCurrentSimulatorWeek <= 17) {
            mModel.getSimulatorSchedule().getWeek(mCurrentSimulatorWeek).simulateTestMatches(true);
            mCurrentSimulatorWeek++;
        }

        mCurrentSimulatorWeek = 18;
        this.view.setCurrentWeekPreference(mCurrentSimulatorWeek);

        ArrayList<ArrayList<Team>> allPlayoffTeams = generateSimulatorPlayoffTeams(SimulatorPresenter.SEASON_TYPE_SIMULATOR);
        simulateTestPlayoffs(allPlayoffTeams);


    }

    private ArrayList<ArrayList<Team>> generateSimulatorPlayoffTeams(int seasonType) {

        //Create two ArrayList<Team>'s... one with afcPlayoffTeams and one with nfcPlayoffTeams
        //These ArrayLists will be created using current list of simulator teams
        //Both of these arraylists will contain the playoff teams sorted by seed

        Team afcNorthDivLeader = null;
        Team afcSouthDivLeader = null;
        Team afcWestDivLeader = null;
        Team afcEastDivLeader = null;
        Team nfcNorthDivLeader = null;
        Team nfcSouthDivLeader = null;
        Team nfcWestDivLeader = null;
        Team nfcEastDivLeader = null;
        ArrayList<Team> afcPotentialWildCardTeams = new ArrayList<>();
        ArrayList<Team> nfcPotentialWildCardTeams = new ArrayList<>();
        ArrayList<Team> afcDivisonWinners = new ArrayList<>();
        ArrayList<Team> nfcDivisionWinners = new ArrayList<>();

        ArrayList<Team> allTeams;
        if (seasonType == SimulatorPresenter.SEASON_TYPE_SIMULATOR) {
            allTeams = mModel.getSimulatorTeamArrayList();
        } else {
            allTeams = mModel.getSeasonTeamArrayList();
        }

        for (Team team : allTeams) {
            if (team.getDivision() == TeamEntry.DIVISION_AFC_NORTH) {
                if (afcNorthDivLeader == null) {
                    afcNorthDivLeader = team;
                } else {
                    if (team.getWinLossPct() > afcNorthDivLeader.getWinLossPct()) {
                        afcPotentialWildCardTeams.add(afcNorthDivLeader);
                        afcNorthDivLeader = team;
                    } else if (team.getWinLossPct() == afcNorthDivLeader.getWinLossPct()) {
                        if (team.getDivisionWinLossPct() > afcNorthDivLeader.getDivisionWinLossPct()) {
                            afcPotentialWildCardTeams.add(afcNorthDivLeader);
                            afcNorthDivLeader = team;
                        } else if (team.getDivisionWinLossPct() == afcNorthDivLeader.getDivisionWinLossPct()) {
                            if (Math.random() > 0.5) {
                                afcPotentialWildCardTeams.add(afcNorthDivLeader);
                                afcNorthDivLeader = team;
                            } else {
                                afcPotentialWildCardTeams.add(team);
                            }
                        } else {
                            afcPotentialWildCardTeams.add(team);
                        }
                    } else {
                        afcPotentialWildCardTeams.add(team);
                    }
                }
            } else if (team.getDivision() == TeamEntry.DIVISION_AFC_SOUTH) {
                if (afcSouthDivLeader == null) {
                    afcSouthDivLeader = team;
                } else {
                    if (team.getWinLossPct() > afcSouthDivLeader.getWinLossPct()) {
                        afcPotentialWildCardTeams.add(afcSouthDivLeader);
                        afcSouthDivLeader = team;
                    } else if (team.getWinLossPct() == afcSouthDivLeader.getWinLossPct()) {
                        if (team.getDivisionWinLossPct() > afcSouthDivLeader.getDivisionWinLossPct()) {
                            afcPotentialWildCardTeams.add(afcSouthDivLeader);
                            afcSouthDivLeader = team;
                        } else if (team.getDivisionWinLossPct() == afcSouthDivLeader.getDivisionWinLossPct()) {
                            if (Math.random() > 0.5) {
                                afcPotentialWildCardTeams.add(afcSouthDivLeader);
                                afcSouthDivLeader = team;
                            } else {
                                afcPotentialWildCardTeams.add(team);
                            }
                        } else {
                            afcPotentialWildCardTeams.add(team);
                        }
                    } else {
                        afcPotentialWildCardTeams.add(team);
                    }
                }

            } else if (team.getDivision() == TeamEntry.DIVISION_AFC_WEST) {
                if (afcWestDivLeader == null) {
                    afcWestDivLeader = team;
                } else {
                    if (team.getWinLossPct() > afcWestDivLeader.getWinLossPct()) {
                        afcPotentialWildCardTeams.add(afcWestDivLeader);
                        afcWestDivLeader = team;
                    } else if (team.getWinLossPct() == afcWestDivLeader.getWinLossPct()) {
                        if (team.getDivisionWinLossPct() > afcWestDivLeader.getDivisionWinLossPct()) {
                            afcPotentialWildCardTeams.add(afcWestDivLeader);
                            afcWestDivLeader = team;
                        } else if (team.getDivisionWinLossPct() == afcWestDivLeader.getDivisionWinLossPct()) {
                            if (Math.random() > 0.5) {
                                afcPotentialWildCardTeams.add(afcWestDivLeader);
                                afcWestDivLeader = team;
                            } else {
                                afcPotentialWildCardTeams.add(team);
                            }
                        } else {
                            afcPotentialWildCardTeams.add(team);
                        }
                    } else {
                        afcPotentialWildCardTeams.add(team);
                    }
                }

            } else if (team.getDivision() == TeamEntry.DIVISION_AFC_EAST) {
                if (afcEastDivLeader == null) {
                    afcEastDivLeader = team;
                } else {
                    if (team.getWinLossPct() > afcEastDivLeader.getWinLossPct()) {
                        afcPotentialWildCardTeams.add(afcEastDivLeader);
                        afcEastDivLeader = team;
                    } else if (team.getWinLossPct() == afcEastDivLeader.getWinLossPct()) {
                        if (team.getDivisionWinLossPct() > afcEastDivLeader.getDivisionWinLossPct()) {
                            afcPotentialWildCardTeams.add(afcEastDivLeader);
                            afcEastDivLeader = team;
                        } else if (team.getDivisionWinLossPct() == afcEastDivLeader.getDivisionWinLossPct()) {
                            if (Math.random() > 0.5) {
                                afcPotentialWildCardTeams.add(afcEastDivLeader);
                                afcEastDivLeader = team;
                            } else {
                                afcPotentialWildCardTeams.add(team);
                            }
                        } else {
                            afcPotentialWildCardTeams.add(team);
                        }
                    } else {
                        afcPotentialWildCardTeams.add(team);
                    }
                }

            } else if (team.getDivision() == TeamEntry.DIVISION_NFC_NORTH) {
                if (nfcNorthDivLeader == null) {
                    nfcNorthDivLeader = team;
                } else {
                    if (team.getWinLossPct() > nfcNorthDivLeader.getWinLossPct()) {
                        nfcPotentialWildCardTeams.add(nfcNorthDivLeader);
                        nfcNorthDivLeader = team;
                    } else if (team.getWinLossPct() == nfcNorthDivLeader.getWinLossPct()) {
                        if (team.getDivisionWinLossPct() > nfcNorthDivLeader.getDivisionWinLossPct()) {
                            nfcPotentialWildCardTeams.add(nfcNorthDivLeader);
                            nfcNorthDivLeader = team;
                        } else if (team.getDivisionWinLossPct() == nfcNorthDivLeader.getDivisionWinLossPct()) {
                            if (Math.random() > 0.5) {
                                nfcPotentialWildCardTeams.add(nfcNorthDivLeader);
                                nfcNorthDivLeader = team;
                            } else {
                                nfcPotentialWildCardTeams.add(team);
                            }
                        } else {
                            nfcPotentialWildCardTeams.add(team);
                        }
                    } else {
                        nfcPotentialWildCardTeams.add(team);
                    }
                }

            } else if (team.getDivision() == TeamEntry.DIVISION_NFC_SOUTH) {
                if (nfcSouthDivLeader == null) {
                    nfcSouthDivLeader = team;
                } else {
                    if (team.getWinLossPct() > nfcSouthDivLeader.getWinLossPct()) {
                        nfcPotentialWildCardTeams.add(nfcSouthDivLeader);
                        nfcSouthDivLeader = team;
                    } else if (team.getWinLossPct() == nfcSouthDivLeader.getWinLossPct()) {
                        if (team.getDivisionWinLossPct() > nfcSouthDivLeader.getDivisionWinLossPct()) {
                            nfcPotentialWildCardTeams.add(nfcSouthDivLeader);
                            nfcSouthDivLeader = team;
                        } else if (team.getDivisionWinLossPct() == nfcSouthDivLeader.getDivisionWinLossPct()) {
                            if (Math.random() > 0.5) {
                                nfcPotentialWildCardTeams.add(nfcSouthDivLeader);
                                nfcSouthDivLeader = team;
                            } else {
                                nfcPotentialWildCardTeams.add(team);
                            }
                        } else {
                            nfcPotentialWildCardTeams.add(team);
                        }
                    } else {
                        nfcPotentialWildCardTeams.add(team);
                    }
                }

            } else if (team.getDivision() == TeamEntry.DIVISION_NFC_EAST) {
                if (nfcEastDivLeader == null) {
                    nfcEastDivLeader = team;
                } else {
                    if (team.getWinLossPct() > nfcEastDivLeader.getWinLossPct()) {
                        nfcPotentialWildCardTeams.add(nfcEastDivLeader);
                        nfcEastDivLeader = team;
                    } else if (team.getWinLossPct() == nfcEastDivLeader.getWinLossPct()) {
                        if (team.getDivisionWinLossPct() > nfcEastDivLeader.getDivisionWinLossPct()) {
                            nfcPotentialWildCardTeams.add(nfcEastDivLeader);
                            nfcEastDivLeader = team;
                        } else if (team.getDivisionWinLossPct() == nfcEastDivLeader.getDivisionWinLossPct()) {
                            if (Math.random() > 0.5) {
                                nfcPotentialWildCardTeams.add(nfcEastDivLeader);
                                nfcEastDivLeader = team;
                            } else {
                                nfcPotentialWildCardTeams.add(team);
                            }
                        } else {
                            nfcPotentialWildCardTeams.add(team);
                        }
                    } else {
                        nfcPotentialWildCardTeams.add(team);
                    }
                }

            } else if (team.getDivision() == TeamEntry.DIVISION_NFC_WEST) {
                if (nfcWestDivLeader == null) {
                    nfcWestDivLeader = team;
                } else {
                    if (team.getWinLossPct() > nfcWestDivLeader.getWinLossPct()) {
                        nfcPotentialWildCardTeams.add(nfcWestDivLeader);
                        nfcWestDivLeader = team;
                    } else if (team.getWinLossPct() == nfcWestDivLeader.getWinLossPct()) {
                        if (team.getDivisionWinLossPct() > nfcWestDivLeader.getDivisionWinLossPct()) {
                            nfcPotentialWildCardTeams.add(nfcWestDivLeader);
                            nfcWestDivLeader = team;
                        } else if (team.getDivisionWinLossPct() == nfcWestDivLeader.getDivisionWinLossPct()) {
                            if (Math.random() > 0.5) {
                                nfcPotentialWildCardTeams.add(nfcWestDivLeader);
                                nfcWestDivLeader = team;
                            } else {
                                nfcPotentialWildCardTeams.add(team);
                            }
                        } else {
                            nfcPotentialWildCardTeams.add(team);
                        }
                    } else {
                        nfcPotentialWildCardTeams.add(team);
                    }
                }
            }

        }

        afcDivisonWinners.add(afcNorthDivLeader);
        afcNorthDivLeader.wonDivision();
        afcDivisonWinners.add(afcSouthDivLeader);
        afcSouthDivLeader.wonDivision();
        afcDivisonWinners.add(afcWestDivLeader);
        afcWestDivLeader.wonDivision();
        afcDivisonWinners.add(afcEastDivLeader);
        afcEastDivLeader.wonDivision();
        nfcDivisionWinners.add(nfcNorthDivLeader);
        nfcNorthDivLeader.wonDivision();
        nfcDivisionWinners.add(nfcSouthDivLeader);
        nfcSouthDivLeader.wonDivision();
        nfcDivisionWinners.add(nfcWestDivLeader);
        nfcWestDivLeader.wonDivision();
        nfcDivisionWinners.add(nfcEastDivLeader);
        nfcEastDivLeader.wonDivision();

        Log.d("AFCWC", "size" + afcPotentialWildCardTeams.size());
        Log.d("NFCWC", "size" + nfcPotentialWildCardTeams.size());

        ArrayList<ArrayList<Team>> allPlayoffTeams =
                Standings.generateTestPlayoffTeams(afcDivisonWinners, nfcDivisionWinners, afcPotentialWildCardTeams, nfcPotentialWildCardTeams);

        return allPlayoffTeams;
    }

    private void simulateTestPlayoffs(ArrayList<ArrayList<Team>> allPlayoffTeams) {

        ArrayList<Team> afcPlayoffTeams = allPlayoffTeams.get(0);
        ArrayList<Team> nfcPlayoffTeams = allPlayoffTeams.get(1);

        for (Team team : afcPlayoffTeams) {
            team.madePlayoffs();
        }
        for (Team team : nfcPlayoffTeams) {
            team.madePlayoffs();
        }

        Team afcSixSeed = afcPlayoffTeams.get(5);
        Team afcFiveSeed = afcPlayoffTeams.get(4);
        Team afcFourSeed = afcPlayoffTeams.get(3);
        Team afcThreeSeed = afcPlayoffTeams.get(2);
        Team nfcSixSeed = nfcPlayoffTeams.get(5);
        Team nfcFiveSeed = nfcPlayoffTeams.get(4);
        Team nfcFourSeed = nfcPlayoffTeams.get(3);
        Team nfcThreeSeed = nfcPlayoffTeams.get(2);

        Boolean afcWildCardSixSeedWon = ELORatingSystem.simulateTestMatch(afcSixSeed, afcThreeSeed, true);
        Boolean afcWildCardFiveSeedWon = ELORatingSystem.simulateTestMatch(afcFiveSeed, afcFourSeed, true);
        Boolean nfcWildCardSixSeedWon = ELORatingSystem.simulateTestMatch(nfcSixSeed, nfcThreeSeed, true);
        Boolean nfcWildCardFiveSeedWon = ELORatingSystem.simulateTestMatch(nfcFiveSeed, nfcFourSeed, true);

        if (afcWildCardSixSeedWon) {
            afcPlayoffTeams.remove(afcThreeSeed);
        } else {
            afcPlayoffTeams.remove(afcSixSeed);
        }
        if (afcWildCardFiveSeedWon) {
            afcPlayoffTeams.remove(afcFourSeed);
        } else {
            afcPlayoffTeams.remove(afcFiveSeed);
        }
        if (nfcWildCardSixSeedWon) {
            nfcPlayoffTeams.remove(nfcThreeSeed);
        } else {
            nfcPlayoffTeams.remove(nfcSixSeed);
        }
        if (nfcWildCardFiveSeedWon) {
            nfcPlayoffTeams.remove(nfcFourSeed);
        } else {
            nfcPlayoffTeams.remove(nfcFiveSeed);
        }

        Team afcDivFourSeed = afcPlayoffTeams.get(3);
        Team afcDivThreeSeed = afcPlayoffTeams.get(2);
        Team afcDivTwoSeed = afcPlayoffTeams.get(1);
        Team afcDivOneSeed = afcPlayoffTeams.get(0);
        Team nfcDivFourSeed = nfcPlayoffTeams.get(3);
        Team nfcDivThreeSeed = nfcPlayoffTeams.get(2);
        Team nfcDivTwoSeed = nfcPlayoffTeams.get(1);
        Team nfcDivOneSeed = nfcPlayoffTeams.get(0);

        Boolean afcDivisionFourSeedWon = ELORatingSystem.simulateTestMatch(afcDivFourSeed, afcDivOneSeed, true);
        Boolean afcDivisionThreeSeedWon = ELORatingSystem.simulateTestMatch(afcDivThreeSeed, afcDivTwoSeed, true);
        Boolean nfcDivisionFourSeedWon = ELORatingSystem.simulateTestMatch(nfcDivFourSeed, nfcDivOneSeed, true);
        Boolean nfcDivisionThreeSeedWon = ELORatingSystem.simulateTestMatch(nfcDivThreeSeed, nfcDivTwoSeed, true);

        if (afcDivisionFourSeedWon) {
            afcPlayoffTeams.remove(afcDivOneSeed);
        } else {
            afcPlayoffTeams.remove(afcDivFourSeed);
        }
        if (afcDivisionThreeSeedWon) {
            afcPlayoffTeams.remove(afcDivTwoSeed);
        } else {
            afcPlayoffTeams.remove(afcDivThreeSeed);
        }
        if (nfcDivisionFourSeedWon) {
            nfcPlayoffTeams.remove(nfcDivOneSeed);
        } else {
            nfcPlayoffTeams.remove(nfcDivFourSeed);
        }
        if (nfcDivisionThreeSeedWon) {
            nfcPlayoffTeams.remove(nfcDivTwoSeed);
        } else {
            nfcPlayoffTeams.remove(nfcDivThreeSeed);
        }

        Boolean afcConfLowSeedWon = ELORatingSystem.simulateTestMatch(afcPlayoffTeams.get(1), afcPlayoffTeams.get(0), true);
        Boolean nfcConfLowSeedWon = ELORatingSystem.simulateTestMatch(nfcPlayoffTeams.get(1), nfcPlayoffTeams.get(0), true);

        if (afcConfLowSeedWon) {
            afcPlayoffTeams.remove(0);
        } else {
            afcPlayoffTeams.remove(1);
        }
        if (nfcConfLowSeedWon) {
            nfcPlayoffTeams.remove(0);
        } else {
            nfcPlayoffTeams.remove(1);
        }

        afcPlayoffTeams.get(0).wonConference();
        nfcPlayoffTeams.get(0).wonConference();
        Boolean afcWonSuperbowl = ELORatingSystem.simulateTestMatch(afcPlayoffTeams.get(0), nfcPlayoffTeams.get(0), false);
        if (afcWonSuperbowl) {
            afcPlayoffTeams.get(0).wonSuperBowl();
        } else {
            nfcPlayoffTeams.get(0).wonSuperBowl();
        }

        Log.d("AFCSB", "" + afcPlayoffTeams.get(0).getName());
        Log.d("NFCSB", "" + nfcPlayoffTeams.get(0).getName());
        Log.d("AFCWon", "" + afcWonSuperbowl);
        Log.d("Current", "" + mCurrentTestSimulations);

        this.view.simulateAnotherTestWeek();

    }

    private void displaySimulatorStandings(Cursor standingsCursor) {

        //Call display standings call for simulator regular season
        for (ScoreView scoreView : mScoreViews) {
            scoreView.onDisplayStandings(SimulatorActivity.STANDINGS_TYPE_REGULAR_SEASON, standingsCursor, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }
    }

    private void displayCurrentSeasonStandings(Cursor standingsCursor) {

        //Call display standings call for current season regular season
        for (ScoreView scoreView : mScoreViews) {
            scoreView.onDisplayStandings(SimulatorActivity.STANDINGS_TYPE_REGULAR_SEASON, standingsCursor, SimulatorModel.QUERY_FROM_SEASON_STANDINGS_ACTIVITY);
        }

    }

    private void displaySimulatorPlayoffStandings(Cursor standingsCursor) {

        //Call display standings call for simulator playoffs
        for (ScoreView scoreView : mScoreViews) {
            scoreView.onDisplayStandings(SimulatorActivity.STANDINGS_TYPE_PLAYOFFS, standingsCursor, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);
        }

    }


    @Override
    public void updateMatchCallback(Match match, Uri uri) {

        //Callback is received when a match is completed
        //The model is then notified to update the match in the database
        if (!mTestSimulation) {
            mModel.updateMatch(match, uri);
        }
    }

    @Override
    public void updateMatchOddsCallback(Match match, Uri uri) {
        //Callback is received when a match is completed
        //The model is then notified to update the match odds in the database
        mModel.updateMatchOdds(match, uri);
    }

    @Override
    public void updateTeamCallback(Team team, Uri uri) {

        //Callback is received when a match is completed
        //The model is then notified to update the team wins, losses and winLossPct.
        //Don't update the database if it is a test simulation
        if (!mTestSimulation) {
            mModel.updateTeam(team, uri);
        }

    }

    public static boolean seasonIsInitialized() {
        return mSeasonInitialized;
    }

    public static void setSeasonInitialized(Boolean seasonIsInitialized) {
        mSeasonInitialized = seasonIsInitialized;
    }

    public static void setCurrentWeek(int currentWeek) {
        mCurrentSimulatorWeek = currentWeek;
    }

    public static int getCurrentWeek() {
        return mCurrentSimulatorWeek;
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
            Team team = mModel.getSimulatorTeamList().get(teamName);
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
            //The better seed is always the home team, so they are added as team two (home team)
            // Seed 3 plays 6, 5 plays 4 for both conferences
            wildCard.addMatch(new Match(afcTeams.get(5), afcTeams.get(2), MatchEntry.MATCH_WEEK_WILDCARD, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
            wildCard.addMatch(new Match(afcTeams.get(4), afcTeams.get(3), MatchEntry.MATCH_WEEK_WILDCARD, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
            wildCard.addMatch(new Match(nfcTeams.get(5), nfcTeams.get(2), MatchEntry.MATCH_WEEK_WILDCARD, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
            wildCard.addMatch(new Match(nfcTeams.get(4), nfcTeams.get(3), MatchEntry.MATCH_WEEK_WILDCARD, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));

            //Add the week to the schedule and insert the matches in the database
            mModel.getSimulatorSchedule().addWeek(wildCard);
            mModel.insertSimulatorMatches(SimulatorModel.INSERT_MATCHES_PLAYOFFS_WILDCARD, wildCard);
        }

        if (remainingPlayoffTeams == 8) {

            //Query the wildcard match scores
            mModel.querySimulatorMatches(MatchEntry.MATCH_WEEK_WILDCARD, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);

            Week divisional = new Week(MatchEntry.MATCH_WEEK_DIVISIONAL);

            //Initialize divisional matchups from cursor
            //The better seed is always the home team, so they are added as team two (home team)
            // Highest remaining seed plays lowest and the two middle seeds play
            divisional.addMatch(new Match(afcTeams.get(3), afcTeams.get(0), MatchEntry.MATCH_WEEK_DIVISIONAL, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
            divisional.addMatch(new Match(afcTeams.get(2), afcTeams.get(1), MatchEntry.MATCH_WEEK_DIVISIONAL, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
            divisional.addMatch(new Match(nfcTeams.get(3), nfcTeams.get(0), MatchEntry.MATCH_WEEK_DIVISIONAL, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
            divisional.addMatch(new Match(nfcTeams.get(2), nfcTeams.get(1), MatchEntry.MATCH_WEEK_DIVISIONAL, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));

            //Add the week to the schedule and  insert the matches in the database
            mModel.getSimulatorSchedule().addWeek(divisional);
            mModel.insertSimulatorMatches(SimulatorModel.INSERT_MATCHES_PLAYOFFS_DIVISIONAL, divisional);


        }
        if (remainingPlayoffTeams == 4) {

            //Query the divisional match scores
            mModel.querySimulatorMatches(MatchEntry.MATCH_WEEK_DIVISIONAL, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);

            Week championship = new Week(MatchEntry.MATCH_WEEK_CHAMPIONSHIP);

            //Initialize conference championship matchups from cursor
            //The better seed is always the home team, so they are added as team two (home team)
            // Highest remaining seed plays lowest
            championship.addMatch(new Match(afcTeams.get(1), afcTeams.get(0), MatchEntry.MATCH_WEEK_CHAMPIONSHIP, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));
            championship.addMatch(new Match(nfcTeams.get(1), nfcTeams.get(0), MatchEntry.MATCH_WEEK_CHAMPIONSHIP, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));

            //Add the week to the schedule and insert the matches in the database
            mModel.getSimulatorSchedule().addWeek(championship);
            mModel.insertSimulatorMatches(SimulatorModel.INSERT_MATCHES_PLAYOFFS_CHAMPIONSHIP, championship);

        }
        if (remainingPlayoffTeams == 2) {

            //Query the championship match scores
            mModel.querySimulatorMatches(MatchEntry.MATCH_WEEK_CHAMPIONSHIP, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);

            Week superbowl = new Week(MatchEntry.MATCH_WEEK_SUPERBOWL);

            //Initialize superbowl
            // Highest remaining seed plays lowest
            superbowl.addMatch(new Match(afcTeams.get(0), nfcTeams.get(0), MatchEntry.MATCH_WEEK_SUPERBOWL, this, MatchEntry.MATCH_TEAM_CURRENT_SEASON_NO));

            //Add the week to the schedule and insert the matches in the database
            mModel.getSimulatorSchedule().addWeek(superbowl);
            mModel.insertSimulatorMatches(SimulatorModel.INSERT_MATCHES_PLAYOFFS_SUPERBOWL, superbowl);

        }

        if (remainingPlayoffTeams == 1) {

            //Query the superbowl match scores
            mModel.querySimulatorMatches(MatchEntry.MATCH_WEEK_SUPERBOWL, true, SimulatorModel.QUERY_FROM_SIMULATOR_ACTIVITY);

        }


    }

    @Override
    public boolean getPlayoffsStarted() {
        return mSimulatorPlayoffsStarted;
    }

    @Override
    public void setPlayoffsStarted(boolean playoffsStarted) {
        mSimulatorPlayoffsStarted = playoffsStarted;
    }

    public void setCurrentSimulatorWeekPreference(int currentWeek) {
        //Set current week preference when week is updated
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(mContext.getString(R.string.settings_simulator_week_num_key), currentWeek).apply();
        prefs.commit();
    }

    public void setCurrentSeasonWeekPreference(int currentWeek) {
        //Set current week preference when week is updated
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(mContext.getString(R.string.settings_season_week_num_key), currentWeek).apply();
        prefs.commit();
    }

    private void setSeasonLoadedPreference(Boolean seasonLoaded) {
        //Set the season loaded preference boolean
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putBoolean(mContext.getString(R.string.settings_season_loaded_key), seasonLoaded).apply();
        prefs.commit();
    }


    private void setSimulatorTeamEloType() {
        Integer eloType = mSharedPreferences.getInt(mContext.getString(R.string.settings_elo_type_key), mContext.getResources().getInteger(R.integer.settings_elo_type_current_season));
        if (eloType == mContext.getResources().getInteger(R.integer.settings_elo_type_current_season)) {
            resetSimulatorTeamCurrentSeasonElos();
        }
        if (eloType == mContext.getResources().getInteger(R.integer.settings_elo_type_user)) {
            resetSimulatorTeamUserElos();
        }
        if (eloType == mContext.getResources().getInteger(R.integer.settings_elo_type_last_season)) {
            resetSimulatorTeamLastSeasonElos();
        }
    }

    private void setSeasonInitializedPreference(boolean seasonInitialized) {
        //Set season initialized boolean preference
        SimulatorPresenter.setSeasonInitialized(seasonInitialized);
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putBoolean(mContext.getString(R.string.settings_season_initialized_key), seasonInitialized);
        prefs.commit();
    }

    private Boolean getSeasonLoadedPref() {
        //Return the season loaded preference boolean
        return mSharedPreferences.getBoolean(mContext.getString(R.string.settings_season_loaded_key), mContext.getResources().getBoolean(R.bool.settings_season_loaded_default));
    }


}
