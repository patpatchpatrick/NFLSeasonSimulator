package io.github.patpatchpatrick.nflseasonsim;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class MainActivity extends AppCompatActivity implements SimulatorMvpContract.SimulatorView {

    Button mSimulateSeason;
    Button mSimulateWeek;
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
                setViewsNotReadyToSimulate();
                mPresenter.simulateWeek();
            }
        });


    }

    private void setSeasonInitializedPreference() {
        //Set season initialized boolean preference to true
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putBoolean(getString(R.string.settings_season_initialized_key), true);
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
                setSeasonInitializedPreference();
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
    public void onDisplayStandings(String standings) {
        //Callback received from presenter to display standings after they are loaded
        mSimulateSeason.setEnabled(true);
        mSimulateWeek.setEnabled(true);
        mStandingsTextView.setText(standings);
        setWeekNumberPreference(SimulatorPresenter.getCurrentWeek());
        if (regularSeasonIsComplete()) {
            setViewsNotReadyToSimulate();
            mStandingsTextView.setText("***REGULAR SEASON COMPLETE*** \n\n" + mStandingsTextView.getText());
        }

    }

    @Override
    public void onDisplayScores(int weekNumber, String scores) {
        //Callback received from presenter to display scores after they are loaded
        //Also receive the weekNumber that was simulated so we can store it in sharedPrefs
        //When app is reloaded, we can automatically show scores/weeks that have already been simulated
        mSimulateSeason.setEnabled(true);
        mSimulateWeek.setEnabled(true);
        mScoresTextView.setText(scores + mScoresTextView.getText());
        setWeekNumberPreference(SimulatorPresenter.getCurrentWeek());
        if (regularSeasonIsComplete()) {
            setViewsNotReadyToSimulate();
        }
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

        mSimulateSeason.setEnabled(true);
        mSimulateWeek.setEnabled(true);
        mStandingsTextView.setText(getString(R.string.ready_to_simulate));
    }

    private void setViewsNotReadyToSimulate() {

        //App is not ready for simulation, set buttons to disabled and textView to "Loading..."

        mSimulateSeason.setEnabled(false);
        mSimulateWeek.setEnabled(false);
        if (!regularSeasonIsComplete()) {
            mStandingsTextView.setText(getString(R.string.loading));
        }
    }

    private Boolean regularSeasonIsComplete() {
        if (SimulatorPresenter.getCurrentWeek() > 17) {
            return true;
        } else {
            return false;
        }
    }

}
