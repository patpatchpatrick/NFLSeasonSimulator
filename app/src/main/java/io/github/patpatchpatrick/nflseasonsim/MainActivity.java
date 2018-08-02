package io.github.patpatchpatrick.nflseasonsim;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    Boolean mPlayoffsStarted = false;
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
                setViewsNotReadyToSimulate();
                mPresenter.simulateSeason();

            }
        });

        mSimulateWeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Disable the buttons until week is finished simulating
                setViewsNotReadyToSimulate();

                //Simulate either a regular week or playoff week depending on whether or not playoffs have started
                if (!mPlayoffsStarted) {
                    mPresenter.simulateWeek();
                }
                if (mPlayoffsStarted) {
                    mPresenter.simulatePlayoffWeek();
                }
            }
        });

        mStartPlayoffs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayoffsStarted = true;
                mPresenter.initiatePlayoffs();
            }
        });

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSeasonInitializedPreference(false);
                mPlayoffsStarted = false;
                SimulatorPresenter.setCurrentWeek(0);
                mScoresTextView.setText("");
                mPresenter.resetSeason();
            }
        });


    }

    private void setSeasonInitializedPreference(boolean seasonInitialized) {
        //Set season initialized boolean preference to true
        SimulatorPresenter.setSeasonInitialized(seasonInitialized);
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putBoolean(getString(R.string.settings_season_initialized_key), seasonInitialized);
        prefs.commit();
    }

    private void setUpSharedPreferences() {

        //Get default shared pref values and set other variables accordingly

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean seasonInitialized = false;
        seasonInitialized = mSharedPreferences.getBoolean(getString(R.string.settings_season_initialized_key), getResources().getBoolean(R.bool.pref_season_initialized_default));
        SimulatorPresenter.setSeasonInitialized(seasonInitialized);
        int currentWeek = 1;
        currentWeek = mSharedPreferences.getInt(getString(R.string.settings_week_num_key), 1);
        SimulatorPresenter.setCurrentWeek(currentWeek);
    }

    @Override
    public void onSeasonInitialized() {

        //After season is initialized, enable simulate buttons and let user know they can now simulate
        //Set season initialized boolean preference to true
        //Run this code on the UI thread, since original call to update the button/textview is made on the
        //schedulers.IO thread

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
        if (SimulatorPresenter.getCurrentWeek() > 1) {
            mPresenter.loadAlreadySimulatedData();
        } else {
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
        setViewsNotReadyToSimulate();
        mPresenter.initializeSeason();
    }

    @Override
    public void onDisplayStandings(String standings) {
        //Callback received from presenter to display standings after they are loaded
        mSimulateSeason.setEnabled(true);
        mSimulateWeek.setEnabled(true);
        mStandingsTextView.setText(standings);
        setWeekNumberPreference(SimulatorPresenter.getCurrentWeek());
        if (regularSeasonIsComplete()) {
            if (!mPlayoffsStarted) {
                setViewsNotReadyToSimulate();
            }
            if (mPlayoffsStarted) {
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
        mSimulateSeason.setEnabled(true);
        mSimulateWeek.setEnabled(true);
        if (weekNumber == MatchEntry.MATCH_WEEK_WILDCARD) {
            mScoresTextView.setText(scores);
            mScoresTextView.setTag(scores);
            setViewsPlayoffs();
        } else if (weekNumber > MatchEntry.MATCH_WEEK_WILDCARD) {
            if (matchesPlayed){
                mScoresTextView.setTag(scores + mScoresTextView.getTag());
            }
            mScoresTextView.setText(scores + mScoresTextView.getTag());
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

        mSimulateSeason.setEnabled(false);
        mSimulateWeek.setEnabled(false);
        mResetButton.setVisibility(View.GONE);
        if (!regularSeasonIsComplete()) {
            mStandingsTextView.setText(getString(R.string.loading));
        }
        if (regularSeasonIsComplete() && !mPlayoffsStarted) {
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

        //Playoffs are complete
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
