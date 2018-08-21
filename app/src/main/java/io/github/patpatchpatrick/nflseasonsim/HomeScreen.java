package io.github.patpatchpatrick.nflseasonsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.dagger.ActivityComponent;
import io.github.patpatchpatrick.nflseasonsim.dagger.ActivityModule;
import io.github.patpatchpatrick.nflseasonsim.dagger.DaggerActivityComponent;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.BaseView;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class HomeScreen extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, BaseView {

    @Inject
    SimulatorPresenter mPresenter;

    @Inject
    SimulatorModel mModel;

    Button mSimulateActivityButton;
    Button mSettingsButton;
    static ActivityComponent mActivityComponent;
    SharedPreferences mSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializeTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        //Initialize Dagger and inject the home activity,  presenter and model
        mActivityComponent = DaggerActivityComponent.builder().activityModule(new ActivityModule(this)).build();
        mActivityComponent.inject(this);
        mActivityComponent.inject(mPresenter);
        mActivityComponent.inject(mModel);

        setSeasonLoadedPreference(false);

        mSimulateActivityButton = (Button) findViewById(R.id.main_menu_sim_season_button);
        mSettingsButton = (Button) findViewById(R.id.main_menu_settings_button);

        mSimulateActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("SeasonLoad", "" + getSeasonLoadedPref());
                Log.d("SeasonInit", "" + getSeasonInitializedPref());
                if (!getSeasonInitializedPref()){
                    //If the season is not initialized, initialize it and start simulate activity after
                    //initialization is complete
                    mPresenter.initializeSeason();
                } else if (!getSeasonLoadedPref()){
                    //If the season is not loaded, load the season from the database
                    mPresenter.loadSeasonFromDatabase();
                } else {
                    //If season is loaded/initialized, start simulator activity
                    Intent startSimulateActivity = new Intent(HomeScreen.this, MainActivity.class);
                    startActivity(startSimulateActivity);
                }
            }
        });

        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startSettingsActivity = new Intent(HomeScreen.this, SettingsActivity.class);
                startActivity(startSettingsActivity);
            }
        });
    }

    private void initializeTheme() {
        //Set the initial theme of the app based on shared prefs theme
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String appTheme = mSharedPrefs.getString(getString(R.string.settings_theme_key), getString(R.string.settings_theme_value_default));
        setTheme(getTheme(appTheme));
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this);

    }

    private int getTheme(String themeValue) {
        //Return the actual theme style that corresponds with the theme sharedPrefs String value
        if (themeValue == getString(R.string.settings_theme_value_default)) {
            return R.style.DarkAppTheme;
        } else if (themeValue == getString(R.string.settings_theme_value_grey)) {
            return R.style.GreyAppTheme;

        } else if (themeValue == getString(R.string.settings_theme_value_purple)) {
            return R.style.PurpleAppTheme;

        } else if (themeValue == getString(R.string.settings_theme_value_blue)) {
            return R.style.AppTheme;
        } else {
            return R.style.DarkAppTheme;
        }
    }

    @Override
    public void setTheme(int resid) {
        super.setTheme(resid);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.settings_theme_key))) {
            //If the app theme is changed, the app must be recreated for new theme to be applied
            HomeScreen.this.recreate();
        }
    }

    private Boolean getSeasonInitializedPref() {
        //Return the preference value for if the season has been initialized
        return mSharedPrefs.getBoolean(getString(R.string.settings_season_initialized_key), getResources().getBoolean(R.bool.pref_season_initialized_default));
    }

    @Override
    protected void onDestroy() {
        //Set season loaded to false since it must be reloaded after activity is destroyed
        setSeasonLoadedPreference(false);
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        mPresenter.destroyPresenter();
        super.onDestroy();
    }

    public static ActivityComponent getActivityComponent() {
        return mActivityComponent;
    }

    @Override
    public void onSeasonInitialized() {
        //After the season is initialized, start the simlate activity
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent startSimulateActivity = new Intent(HomeScreen.this, MainActivity.class);
                startActivity(startSimulateActivity);
            }
        });
    }

    @Override
    public void onSeasonLoadedFromDb() {
        Intent startSimulateActivity = new Intent(HomeScreen.this, MainActivity.class);
        startActivity(startSimulateActivity);
    }

    private Boolean getSeasonLoadedPref(){
        //Return the season loaded preference boolean
        return mSharedPrefs.getBoolean(getString(R.string.settings_season_loaded_key), getResources().getBoolean(R.bool.settings_season_loaded_default));
    }

    private void setSeasonLoadedPreference(Boolean seasonLoaded){
        //Set the season loaded preference boolean
        SharedPreferences.Editor prefs = mSharedPrefs.edit();
        prefs.putBoolean(getString(R.string.settings_season_loaded_key), seasonLoaded).apply();
        prefs.commit();
    }



}
