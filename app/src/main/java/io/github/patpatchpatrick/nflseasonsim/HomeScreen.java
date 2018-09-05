package io.github.patpatchpatrick.nflseasonsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.kobakei.ratethisapp.RateThisApp;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.dagger.ActivityComponent;
import io.github.patpatchpatrick.nflseasonsim.dagger.ActivityModule;
import io.github.patpatchpatrick.nflseasonsim.dagger.DaggerActivityComponent;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.BaseView;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class HomeScreen extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, BaseView {

    @Inject
    SimulatorPresenter mPresenter;

    @Inject
    SimulatorModel mModel;

    Button mSimulateActivityButton;
    Button mMatchPredictButton;
    Button mSettingsButton;
    Button mNextWeekMatchesButton;
    Button mStandingsButton;
    ImageView mAnimatedFootball;
    Animatable mAnimatedFootballAnimatable;
    static ActivityComponent mActivityComponent;
    SharedPreferences mSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializeTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        getWindow().setBackgroundDrawable(null);
        getSupportActionBar().hide();


        //Initialize Dagger and inject the home activity,  presenter and model
        mActivityComponent = DaggerActivityComponent.builder().activityModule(new ActivityModule(this)).build();
        mActivityComponent.inject(this);
        mActivityComponent.inject(mPresenter);
        mActivityComponent.inject(mModel);

        //Initialize app rater;
        setUpAppRater();

        setSeasonLoadedPreference(false);

        mSimulateActivityButton = (Button) findViewById(R.id.main_menu_sim_season_button);
        mMatchPredictButton = (Button) findViewById(R.id.main_menu_predict_matchup_button);
        mSettingsButton = (Button) findViewById(R.id.main_menu_settings_button);
        mNextWeekMatchesButton = (Button) findViewById(R.id.main_menu_season_schedule_button);
        mStandingsButton = (Button) findViewById(R.id.main_menu_season_standings_button);

        mAnimatedFootball = (ImageView) findViewById(R.id.football_animation);
        mAnimatedFootballAnimatable = (Animatable) mAnimatedFootball.getDrawable();
        if (mAnimatedFootballAnimatable.isRunning()) {
            mAnimatedFootballAnimatable.stop();
        }

        //Initialize season if not initialized
        //Load season, if not loaded already
        //Start loading animation
        if (!getSeasonInitializedPref()) {
            setButtonsActive(false);
            mAnimatedFootballAnimatable.start();
            mPresenter.initializeSeason();
        } else if (!getSeasonLoadedPref()) {
            setButtonsActive(false);
            mAnimatedFootballAnimatable.start();
            mPresenter.loadSeasonFromDatabase();
        }

        mSimulateActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startSimulateActivity = new Intent(HomeScreen.this, MainActivity.class);
                startActivity(startSimulateActivity);

            }
        });

        mMatchPredictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //If season isn't loaded, load it, otherwise start the match predictor activity
                Intent startMatchPredictActivity = new Intent(HomeScreen.this, MatchPredictorActivity.class);
                startActivity(startMatchPredictActivity);

            }
        });

        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startSettingsActivity = new Intent(HomeScreen.this, SettingsActivity.class);
                startActivity(startSettingsActivity);
            }
        });

        mNextWeekMatchesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startNextWeekMatchesActivity = new Intent(HomeScreen.this, NextWeekMatchesActivity.class);
                startActivity(startNextWeekMatchesActivity);
            }
        });

        mStandingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startStandingsActivity = new Intent(HomeScreen.this, StandingsActivity.class);
                startActivity(startStandingsActivity);
            }
        });

    }

    private void setButtonsActive(boolean buttonsActive) {
        mSimulateActivityButton.setEnabled(buttonsActive);
        mMatchPredictButton.setEnabled(buttonsActive);
        mSettingsButton.setEnabled(buttonsActive);
        mNextWeekMatchesButton.setEnabled(buttonsActive);
        mStandingsButton.setEnabled(buttonsActive);
    }


    private void initializeTheme() {
        //Set the initial theme of the app based on shared prefs theme
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String appTheme = mSharedPrefs.getString(getString(R.string.settings_theme_key), getString(R.string.settings_theme_value_default));
        Log.d("theme", appTheme);
        setTheme(getTheme(appTheme));
        mSharedPrefs.registerOnSharedPreferenceChangeListener(this);

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
        //After the season is initialized, stop loading animation and make buttons active again
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setButtonsActive(true);
                if (mAnimatedFootballAnimatable.isRunning()) {
                    mAnimatedFootballAnimatable.stop();
                }
            }

        });
    }

    @Override
    public void onSeasonLoadedFromDb() {
        //Stop loading animation when season is finished loading
        //Set all buttons back to active
        setButtonsActive(true);
        mAnimatedFootballAnimatable.stop();

    }

    private Boolean getSeasonLoadedPref() {
        //Return the season loaded preference boolean
        return mSharedPrefs.getBoolean(getString(R.string.settings_season_loaded_key), getResources().getBoolean(R.bool.settings_season_loaded_default));
    }

    private void setSeasonLoadedPreference(Boolean seasonLoaded) {
        //Set the season loaded preference boolean
        SharedPreferences.Editor prefs = mSharedPrefs.edit();
        prefs.putBoolean(getString(R.string.settings_season_loaded_key), seasonLoaded).apply();
        prefs.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //If animation is still running, stop it
        if (mAnimatedFootballAnimatable.isRunning()) {
            mAnimatedFootballAnimatable.stop();
        }
    }

    private void setUpAppRater(){
        //Launch rate this app activity if conditions are met
        // Monitor launch times and interval from installation
        RateThisApp.onCreate(this);
        // If the condition is satisfied, "Rate this app" dialog will be shown
        RateThisApp.showRateDialogIfNeeded(this);
        RateThisApp.Config config = new RateThisApp.Config();
        config.setUrl("https://play.google.com/store/apps/details?id=io.github.patpatchpatrick.nflseasonsim");
        RateThisApp.init(config);
        RateThisApp.setCallback(new RateThisApp.Callback() {
            @Override
            public void onYesClicked() {
                RateThisApp.stopRateDialog(HomeScreen.this);
            }

            @Override
            public void onNoClicked() {
                RateThisApp.stopRateDialog(HomeScreen.this);
            }

            @Override
            public void onCancelClicked() {

            }
        });
    }


}
