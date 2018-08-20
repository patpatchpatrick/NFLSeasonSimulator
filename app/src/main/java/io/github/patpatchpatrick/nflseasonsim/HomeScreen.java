package io.github.patpatchpatrick.nflseasonsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomeScreen extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    Button mSimulateActivityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializeTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        mSimulateActivityButton = (Button) findViewById(R.id.main_menu_sim_season_button);

        mSimulateActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startSimulateActivity = new Intent(HomeScreen.this, MainActivity.class);
                startActivity(startSimulateActivity);
            }
        });
    }

    private void initializeTheme() {
        //Set the initial theme of the app based on shared prefs theme
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int appTheme = sharedPreferences.getInt(getString(R.string.settings_theme_key), getResources().getInteger(R.integer.settings_value_theme_default));
        setTheme(getTheme(appTheme));

    }

    private int getTheme(int themeId) {
        //Return the actual theme style that corresponds with the theme sharedPrefs integer
        if (themeId == getResources().getInteger(R.integer.settings_value_theme_blue)) {
            return R.style.AppTheme;
        } else if (themeId == getResources().getInteger(R.integer.settings_value_theme_grey)) {
            return R.style.GreyAppTheme;

        } else if (themeId == getResources().getInteger(R.integer.settings_value_theme_purple)) {
            return R.style.PurpleAppTheme;

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
}
