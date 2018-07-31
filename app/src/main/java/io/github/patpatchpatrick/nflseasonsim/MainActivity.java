package io.github.patpatchpatrick.nflseasonsim;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;

public class MainActivity extends AppCompatActivity implements SimulatorMvpContract.SimulatorView {

    Button mSimulateSeason;
    TextView mTextView;
    private SimulatorPresenter mPresenter;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setUpSharedPreferences();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.test_dagger_text_view);
        mSimulateSeason = (Button) findViewById(R.id.simulate_season_button);

        //TODO inject presenter instead of instantiating it
        SimulatorPresenter presenter = new SimulatorPresenter(this);
        mPresenter = presenter;

        //Initialize the season if not yet initialized
        //If already initialized, load season from the database
        if (!SimulatorPresenter.seasonIsInitialized()) {
            mSimulateSeason.setEnabled(false);
            mTextView.setText(getString(R.string.loading));
            mPresenter.initializeSeason();
        } else {
            mSimulateSeason.setEnabled(false);
            mTextView.setText(getString(R.string.loading));
            mPresenter.loadSeasonFromDatabase();
        }

        mSimulateSeason.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTextView.setText(getString(R.string.loading));
                mSimulateSeason.setEnabled(false);
                mPresenter.simulateSeason();
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

        //Get default shared pref values

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean seasonInitialized = false;
        seasonInitialized = mSharedPreferences.getBoolean(getString(R.string.settings_season_initialized_key), getResources().getBoolean(R.bool.pref_season_initialized_default));
        SimulatorPresenter.setSeasonInitialized(seasonInitialized);
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

                mSimulateSeason.setEnabled(true);
                mTextView.setText(getString(R.string.ready_to_simulate));
                setSeasonInitializedPreference();
            }
        });
    }

    @Override
    public void onSeasonLoadedFromDb() {
        mSimulateSeason.setEnabled(true);
        mTextView.setText(getString(R.string.ready_to_simulate));
    }

    @Override
    public void onDisplayStandings(String standings) {
        mSimulateSeason.setEnabled(true);
        mTextView.setText(standings);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.destroyPresenter();
    }
}
