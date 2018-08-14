package io.github.patpatchpatrick.nflseasonsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.dagger.ActivityComponent;
import io.github.patpatchpatrick.nflseasonsim.dagger.ActivityModule;
import io.github.patpatchpatrick.nflseasonsim.dagger.DaggerActivityComponent;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class MainActivity extends AppCompatActivity implements SimulatorMvpContract.SimulatorView, SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    SimulatorPresenter mPresenter;

    @Inject
    SimulatorModel mModel;

    @Inject
    SharedPreferences mSharedPreferences;

    Button mSimulateSeason;
    Button mSimulateWeek;
    Button mStartPlayoffs;
    Button mResetButton;
    TextView mWeekNumberHeader;
    TextView mSimulationStatus;
    RecyclerView mScoresRecyclerView;
    ScoresRecyclerViewAdapter mScoresRecyclerAdapter;
    RecyclerView mStandingsRecyclerView;
    StandingsRecyclerViewAdapter mStandingsRecyclerAdapter;
    private static ActivityComponent mActivityComponent;

    public final static int STANDINGS_TYPE_REGULAR_SEASON = 1;
    public final static int STANDINGS_TYPE_PLAYOFFS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializeTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Build out dagger activity component
        // Inject the mainActivity, presenter and model
        mActivityComponent = DaggerActivityComponent.builder().activityModule(new ActivityModule(this)).build();
        mActivityComponent.inject(this);
        mActivityComponent.inject(mPresenter);
        mActivityComponent.inject(mModel);

        mWeekNumberHeader = (TextView) findViewById(R.id.week_number_header);
        mSimulationStatus = (TextView) findViewById(R.id.simulation_status_textview);
        mSimulateSeason = (Button) findViewById(R.id.simulate_season_button);
        mSimulateWeek = (Button) findViewById(R.id.simulate_week_button);
        mStartPlayoffs = (Button) findViewById(R.id.start_playoffs_button);
        mResetButton = (Button) findViewById(R.id.reset_button);

        setUpSharedPreferences();

        // Set up the scores/matches recyclerview
        mScoresRecyclerView = (RecyclerView) findViewById(R.id.scores_recycler_view);
        mScoresRecyclerView.setHasFixedSize(true);
        mScoresRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        mScoresRecyclerAdapter = new ScoresRecyclerViewAdapter();
        mScoresRecyclerView.setAdapter(mScoresRecyclerAdapter);

        // Set up the standings recyclerview
        mStandingsRecyclerView = (RecyclerView) findViewById(R.id.standings_recycler_view);
        mStandingsRecyclerView.setHasFixedSize(true);
        mStandingsRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        mStandingsRecyclerAdapter = new StandingsRecyclerViewAdapter();
        mStandingsRecyclerView.setAdapter(mStandingsRecyclerAdapter);


        //Initialize the season if not yet initialized
        //If already initialized, load season from the database
        if (!SimulatorPresenter.seasonIsInitialized()) {
            setViewsNotReadyToSimulate();
            mPresenter.initializeSeason();
        } else {
            setViewsNotReadyToSimulate();
            mPresenter.loadSeasonFromDatabase();
        }

        mSimulateSeason.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Simulate season
                //Set views to not ready to simulate while the season simulates
                setViewsNotReadyToSimulate();
                mPresenter.simulateSeason();

            }
        });

        mSimulateWeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Simulate the week
                //Disable the buttons until week is finished simulating
                setViewsNotReadyToSimulate();

                //Simulate either a regular week or playoff week depending on whether or not playoffs have started
                if (!mPresenter.getPlayoffsStarted()) {
                    mPresenter.simulateWeek();
                }
                if (mPresenter.getPlayoffsStarted()) {
                    mPresenter.simulatePlayoffWeek();
                }
            }
        });

        mStartPlayoffs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Start the playoffs
                mPresenter.setPlayoffsStarted(true);
                setPlayoffsStartedPreference(true);
                mPresenter.initiatePlayoffs();
            }
        });

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Reset the season
                resetSeason();
            }
        });


    }

    private void initializeTheme() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int appTheme = sharedPreferences.getInt(getString(R.string.settings_theme_key), getResources().getInteger(R.integer.settings_value_theme_default));
        setTheme(getTheme(appTheme));

    }

    private void resetSeason() {

        //Reset the season
        //Reset all shared preference values

        setSeasonInitializedPreference(false);
        mPresenter.setPlayoffsStarted(false);
        setPlayoffsStartedPreference(mPresenter.getPlayoffsStarted());
        SimulatorPresenter.setCurrentWeek(0);
        setCurrentWeekPreference(0);
        setScoreStringPreference("");
        mWeekNumberHeader.setText("");
        mScoresRecyclerAdapter.swapCursor(null);
        mStandingsRecyclerAdapter.swapCursor(MainActivity.STANDINGS_TYPE_REGULAR_SEASON, null);
        mPresenter.resetSeason();


    }

    private void setSeasonInitializedPreference(boolean seasonInitialized) {
        //Set season initialized boolean preference
        SimulatorPresenter.setSeasonInitialized(seasonInitialized);
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putBoolean(getString(R.string.settings_season_initialized_key), seasonInitialized);
        prefs.commit();
    }

    private void setPlayoffsStartedPreference(boolean playoffsStarted) {
        //Set playoffs started boolean preference
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putBoolean(getString(R.string.settings_playoffs_started_key), playoffsStarted);
        prefs.commit();

    }

    private void setScoreStringPreference(String scoreString) {
        //Set score string preference that holds playoff scores
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putString(getString(R.string.settings_score_string_key), scoreString).apply();
        prefs.commit();
    }

    public void setCurrentWeekPreference(int currentWeek) {
        //Set current week preference when week is updated
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_week_num_key), currentWeek).apply();
        prefs.commit();
    }

    private String getScoreStringPreference() {
        return mSharedPreferences.getString(getString(R.string.settings_score_string_key), "no data");
    }

    private void setUpSharedPreferences() {

        //Get default shared pref values and set other variables accordingly

        Boolean seasonInitialized = false;
        seasonInitialized = mSharedPreferences.getBoolean(getString(R.string.settings_season_initialized_key), getResources().getBoolean(R.bool.pref_season_initialized_default));
        mPresenter.setPlayoffsStarted(mSharedPreferences.getBoolean(getString(R.string.settings_playoffs_started_key), getResources().getBoolean(R.bool.pref_playoffs_started_default)));
        SimulatorPresenter.setSeasonInitialized(seasonInitialized);
        int currentWeek = 1;
        currentWeek = mSharedPreferences.getInt(getString(R.string.settings_week_num_key), 1);
        SimulatorPresenter.setCurrentWeek(currentWeek);
    }

    @Override
    public void onSeasonInitialized() {

        //After season is initialized, enable simulate buttons and let user know they can now simulate
        //Set season initialized boolean preference to true
        //Run this code on the UI thread, since original call to update the button/textview is made on a separate thread

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                setViewsReadyToSimulate();
                setSeasonInitializedPreference(true);

                //Check if user is using future elo values instead of default, if so, set teams to have future elo values
                Integer eloType = mSharedPreferences.getInt(getString(R.string.settings_elo_type_key), getResources().getInteger(R.integer.settings_elo_type_default));
                if (eloType == getResources().getInteger(R.integer.settings_elo_type_future)) {
                    mPresenter.resetTeamFutureElos();
                }
                if (eloType == getResources().getInteger(R.integer.settings_elo_type_user)) {
                    mPresenter.resetTeamUserElos();
                }
            }
        });
    }

    @Override
    public void onSeasonLoadedFromDb() {

        //After the season is loaded from the database
        //If the current week is greater than one and playoffs haven't started, the season has been partially
        //simulated, so load the already simulated data

        //Otherwise, if the playoffs hasn't started, set the views as ready to simulate

        if (SimulatorPresenter.getCurrentWeek() >= 1 && !mPresenter.getPlayoffsStarted()) {
            mPresenter.loadAlreadySimulatedData();
        } else if (!mPresenter.getPlayoffsStarted()) {
            setViewsReadyToSimulate();
        }
    }

    @Override
    public void onPriorSimulatedDataLoaded() {
        if (regularSeasonIsComplete()) {
            setViewsNotReadyToSimulate();
        } else {
            setViewsReadyToSimulate();
        }
    }

    @Override
    public void onDataDeleted() {
        //After data is deleted from database when season is reset, re-initialize the next season

        setViewsNotReadyToSimulate();
        mPresenter.initializeSeason();
    }

    @Override
    public void onDisplayStandings(int standingsType, Cursor cursor) {
        //Callback received from presenter to display standings after they are loaded

        //Set simulate buttons to active
        setViewsReadyToSimulate();

        mStandingsRecyclerAdapter.swapCursor(standingsType, cursor);

        //Set current week preference value since the week is now complete.  This display standings call
        //is received after the week  is simulated and the week number was incremented during simulation.
        setWeekNumberPreference(SimulatorPresenter.getCurrentWeek());


        if (regularSeasonIsComplete()) {
            //If the regular season is complete, set up the views for playoffs depending on if playoffs
            //have started or playoffs have completed
            if (!mPresenter.getPlayoffsStarted()) {
                setViewsNotReadyToSimulate();
            }
            if (mPresenter.getPlayoffsStarted()) {
                if (playoffsComplete()) {
                    setViewsPlayoffsComplete();
                } else {
                    setViewsPlayoffs();
                }
            }
        }

    }

    @Override
    public void onDisplayScores(int weekNumber, Cursor cursor, String scoresWeekNumberHeader, boolean matchesPlayed) {
        //Callback received from presenter to display scores after they are loaded
        //Also receive the weekNumber that was simulated so we can store it in sharedPrefs
        //When app is reloaded, we can automatically show scores/weeks that have already been simulated

        //Set simulate buttons to active
        setViewsReadyToSimulate();

        //Swap matches/scores cursor into the recyclerview adapter
        //Set the week number header text
        mScoresRecyclerAdapter.swapCursor(cursor);
        mWeekNumberHeader.setText(scoresWeekNumberHeader);


        //Set views depending on which week it is and playoff status
        if (weekNumber >= MatchEntry.MATCH_WEEK_WILDCARD) {
            setViewsPlayoffs();
        } else {
            if (regularSeasonIsComplete()) {
                setViewsNotReadyToSimulate();
            }
        }
        if (playoffsComplete()) {
            //If playoffs are complete, set playoff complete views (ready views to restart simulation)
            setViewsPlayoffsComplete();
        }

        setWeekNumberPreference(SimulatorPresenter.getCurrentWeek());

    }

    private void setWeekNumberPreference(int weekNumber) {
        //Set season weekNumber preference to the current week
        //The current week is one more than the week that was simulated
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_week_num_key), weekNumber);
        prefs.commit();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.destroyPresenter();
    }

    private void setViewsReadyToSimulate() {

        //App is ready for simulation, set buttons to enabled and textview to "ready to simulate"

        mSimulateWeek.setVisibility(View.VISIBLE);
        mSimulateSeason.setVisibility(View.VISIBLE);
        mSimulateSeason.setEnabled(true);
        mSimulateWeek.setEnabled(true);
        mResetButton.setVisibility(View.GONE);
        mSimulationStatus.setVisibility(View.VISIBLE);
        mSimulationStatus.setText(getString(R.string.ready_to_simulate));
    }

    private void setViewsNotReadyToSimulate() {

        //App is not ready for simulation, set buttons to disabled and textView to "Loading..."

        //If the regular season is complete, enable the "Start Playoffs" button

        mSimulateSeason.setEnabled(false);
        mSimulateWeek.setEnabled(false);
        mSimulateSeason.setVisibility(View.INVISIBLE);
        mSimulateWeek.setVisibility(View.INVISIBLE);
        mResetButton.setVisibility(View.GONE);
        if (!regularSeasonIsComplete()) {
            mSimulationStatus.setVisibility(View.VISIBLE);
            mSimulationStatus.setText(getString(R.string.loading));
        }
        if (regularSeasonIsComplete() && !mPresenter.getPlayoffsStarted()) {
            mSimulateSeason.setVisibility(View.GONE);
            mStartPlayoffs.setVisibility(View.VISIBLE);
            mSimulationStatus.setVisibility(View.GONE);
        }
    }

    private void setViewsPlayoffs() {

        //Playoffs have begun, set up the views for the playoffs
        //Hide the start playoffs button, and enable the simulate week button
        mStartPlayoffs.setVisibility(View.GONE);
        mSimulateSeason.setVisibility(View.INVISIBLE);
        mResetButton.setVisibility(View.GONE);
        mSimulateWeek.setEnabled(true);
        mSimulateWeek.setVisibility(View.VISIBLE);
    }

    private void setViewsPlayoffsComplete() {

        //Playoffs are complete, hide all buttons except for the reset button
        mSimulateSeason.setVisibility(View.INVISIBLE);
        mSimulateWeek.setVisibility(View.GONE);
        mStartPlayoffs.setVisibility(View.GONE);
        mResetButton.setVisibility(View.VISIBLE);


    }

    private Boolean regularSeasonIsComplete() {
        if (SimulatorPresenter.getCurrentWeek() > 17) {
            return true;
        } else {
            return false;
        }
    }

    private Boolean playoffsComplete() {
        if (SimulatorPresenter.getCurrentWeek() > MatchEntry.MATCH_WEEK_SUPERBOWL) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.primary_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.primary_settings:
                Intent startEloValuesActivity = new Intent(MainActivity.this, EloValuesActivity.class);
                startActivity(startEloValuesActivity);
                return true;
            case R.id.reset_menu_button:
                resetSeason();
                return true;
            case R.id.theme_picker_default:
                setAppTheme(getResources().getInteger(R.integer.settings_value_theme_default));
                return true;
            case R.id.theme_picker_blue:
                setAppTheme(getResources().getInteger(R.integer.settings_value_theme_blue));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setAppTheme(int themeResId) {
        setTheme(getTheme(themeResId));
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_theme_key), getTheme(themeResId));
        prefs.commit();
    }

    private int getTheme(int themeId) {
        if (themeId == getResources().getInteger(R.integer.settings_value_theme_blue)) {
            return R.style.AppTheme;
        } else {
            return R.style.DarkAppTheme;
        }
    }

    @Override
    public void setTheme(int resid) {
        super.setTheme(resid);
    }

    public static ActivityComponent getActivityComponent() {
        return mActivityComponent;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.settings_theme_key))) {
            //If the app theme is changed, the app must be recreated for new theme to be applied
            MainActivity.this.recreate();
        }
    }
}
