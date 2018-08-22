package io.github.patpatchpatrick.nflseasonsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class EloValuesActivity extends AppCompatActivity {
    //Activity to edit team ELO values

    @Inject
    SharedPreferences mSharedPreferences;

    @Inject
    SimulatorPresenter mSimulatorPresenter;

    private RecyclerView mRecyclerView;
    private Button mLastSeasonEloButton;
    private Button mFutureEloButton;
    private Button mCurrentEloButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Set up theme before creating activity
        initializeTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elo_values);

        //Inject with Dagger Activity Component to get access to presenter
        HomeScreen.getActivityComponent().inject(this);

        // Find recyclerView for list of team names and elos and set linearLayoutManager and recyclerAdapter on recyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_elo);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(EloValuesActivity.this));
        final EloRecyclerViewAdapter eloRecyclerAdapter = new EloRecyclerViewAdapter();
        mRecyclerView.setAdapter(eloRecyclerAdapter);

        mLastSeasonEloButton = (Button) findViewById(R.id.use_default_button);
        mLastSeasonEloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSimulatorPresenter.resetTeamElos();
                eloRecyclerAdapter.notifyDataSetChanged();
                setUseDefaultElosPref();
            }
        });

        mFutureEloButton = (Button) findViewById(R.id.future_elos_button);
        mFutureEloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSimulatorPresenter.resetTeamFutureElos();
                eloRecyclerAdapter.notifyDataSetChanged();
                setUseFutureElosPref();

            }
        });

        mCurrentEloButton = (Button) findViewById(R.id.current_elos_button);
        mCurrentEloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSimulatorPresenter.setTeamUserElos();
                setUseUserElosPref();
            }
        });


    }

    private void initializeTheme() {
        //Set the initial theme of the app based on shared prefs theme
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String appTheme = sharedPreferences.getString(getString(R.string.settings_theme_key), getResources().getString(R.string.settings_theme_value_default));
        setTheme(getTheme(appTheme));
    }

    private void setUseFutureElosPref(){
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_elo_type_key), getResources().getInteger(R.integer.settings_elo_type_future));
        prefs.commit();
    }

    private void setUseDefaultElosPref(){
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_elo_type_key), getResources().getInteger(R.integer.settings_elo_type_default));
        prefs.commit();
    }

    private void setUseUserElosPref(){
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_elo_type_key), getResources().getInteger(R.integer.settings_elo_type_user));
        prefs.commit();
    }

    private int getTheme(String themeValue) {
        //Return the actual theme style that corresponds with the theme sharedPrefs String value
        if (themeValue.equals(getString(R.string.settings_theme_value_default))) {
            return R.style.DarkAppTheme;
        } else if (themeValue.equals(getString(R.string.settings_theme_value_grey))) {
            return R.style.GreyAppTheme;

        } else if (themeValue.equals(getString(R.string.settings_theme_value_purple))) {
            return R.style.PurpleAppTheme;

        } else if (themeValue.equals(getString(R.string.settings_theme_value_blue))) {
            return R.style.AppTheme;
        } else {
            return R.style.DarkAppTheme;
        }
    }



}
