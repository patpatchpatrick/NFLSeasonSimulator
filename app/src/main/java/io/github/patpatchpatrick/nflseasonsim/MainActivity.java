package io.github.patpatchpatrick.nflseasonsim;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class MainActivity extends AppCompatActivity implements SimulatorMvpContract.SimulatorView {

    Button mSimulateSeason;
    Button mSimulateWeek;
    Button mStartPlayoffs;
    Button mResetButton;
    TextView mStandingsTextView;
    TextView mScoresTextView;
    private SimulatorPresenter mPresenter;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStandingsTextView = (TextView) findViewById(R.id.standings_text_view);
        mScoresTextView = (TextView) findViewById(R.id.scores_text_view);
        mSimulateSeason = (Button) findViewById(R.id.simulate_season_button);
        mSimulateWeek = (Button) findViewById(R.id.simulate_week_button);
        mStartPlayoffs = (Button) findViewById(R.id.start_playoffs_button);
        mResetButton = (Button) findViewById(R.id.reset_button);

        //TODO inject presenter instead of instantiating it
        SimulatorPresenter presenter = new SimulatorPresenter(this);
        mPresenter = presenter;

        setUpSharedPreferences();

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
                //Reset all shared preference values

                setSeasonInitializedPreference(false);
                mPresenter.setPlayoffsStarted(false);
                setPlayoffsStartedPreference(mPresenter.getPlayoffsStarted());
                SimulatorPresenter.setCurrentWeek(0);
                setScoreStringPreference("");
                mScoresTextView.setText("");
                mPresenter.resetSeason();
            }
        });


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

    private String getScoreStringPreference() {
        return mSharedPreferences.getString(getString(R.string.settings_score_string_key), "no data");
    }

    private void setUpSharedPreferences() {

        //Get default shared pref values and set other variables accordingly

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
    public void onDisplayStandings(String standings) {
        //Callback received from presenter to display standings after they are loaded

        //Set simulate buttons to active
        mSimulateSeason.setEnabled(true);
        mSimulateWeek.setEnabled(true);

        //Set standings text
        mStandingsTextView.setText(standings);

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
    public void onDisplayScores(int weekNumber, String scores, boolean matchesPlayed) {
        //Callback received from presenter to display scores after they are loaded
        //Also receive the weekNumber that was simulated so we can store it in sharedPrefs
        //When app is reloaded, we can automatically show scores/weeks that have already been simulated

        //Set simulate buttons to active
        mSimulateSeason.setEnabled(true);
        mSimulateWeek.setEnabled(true);

        //Display scores
        //If displaying playoff scores, the prior round's scores are stored in  a shared preference value,
        //and the next round's scores are prepended to the textview when they are loaded

        //If displaying regular season scores, each week's scores is prepended to the prior week's scores
        if (weekNumber == MatchEntry.MATCH_WEEK_WILDCARD) {
            mScoresTextView.setText(scores);
            setScoreStringPreference(scores);
            setViewsPlayoffs();
        } else if (weekNumber > MatchEntry.MATCH_WEEK_WILDCARD) {
            if (matchesPlayed) {
                setScoreStringPreference(scores + getScoreStringPreference());
            }
            mScoresTextView.setText(scores + getScoreStringPreference());

            setViewsPlayoffs();
        } else {
            mScoresTextView.setText(scores + mScoresTextView.getText());
            if (regularSeasonIsComplete()) {
                setViewsNotReadyToSimulate();
            }
        }
        if (playoffsComplete()) {
            //If playoffs are complete, set playoff complete views (ready views to restart simulation)
            setViewsPlayoffsComplete();
            setScoreStringPreference("");
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
        mStandingsTextView.setText(getString(R.string.ready_to_simulate));
    }

    private void setViewsNotReadyToSimulate() {

        //App is not ready for simulation, set buttons to disabled and textView to "Loading..."

        //If the regular season is complete, enable the "Start Playoffs" button

        mSimulateSeason.setEnabled(false);
        mSimulateWeek.setEnabled(false);
        mResetButton.setVisibility(View.GONE);
        if (!regularSeasonIsComplete()) {
            mStandingsTextView.setText(getString(R.string.loading));
        }
        if (regularSeasonIsComplete() && !mPresenter.getPlayoffsStarted()) {
            mStartPlayoffs.setVisibility(View.VISIBLE);
        }
    }

    private void setViewsPlayoffs() {

        //Playoffs have begun, set up the views for the playoffs
        //Hide the start playoffs button, and enable the simulate week button
        mStartPlayoffs.setVisibility(View.GONE);
        mSimulateSeason.setVisibility(View.INVISIBLE);
        mResetButton.setVisibility(View.GONE);
        mSimulateWeek.setEnabled(true);
    }

    private void setViewsPlayoffsComplete() {

        //Playoffs are complete, hide all buttons except for the reset button
        mSimulateSeason.setVisibility(View.INVISIBLE);
        mSimulateWeek.setVisibility(View.INVISIBLE);
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

}
