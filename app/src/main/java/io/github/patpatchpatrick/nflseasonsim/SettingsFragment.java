package io.github.patpatchpatrick.nflseasonsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.BaseView;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, BaseView {

    @Inject
    SharedPreferences mSharedPrefs;

    @Inject
    SimulatorPresenter mPresenter;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_settings);


        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int count = preferenceScreen.getPreferenceCount();

        for (int i = 0; i < count; i++) {
            Preference p = preferenceScreen.getPreference(i);
            // You don't need to set up preference summaries for checkbox preferences because
            // they are already set up in xml using summaryOff and summary On
            if (!(p instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(p.getKey(), "");
                setPreferenceSummary(p, value);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        //If the set elo values preference is clicked, open the elo values activity
        //If the season is not yet loaded, load it

        String key = preference.getKey();
        if (key.equals(getString(R.string.settings_activity_elo_values))) {
            if (!getSeasonLoadedPref()) {
                mPresenter.loadSeasonFromDatabase(SimulatorModel.LOAD_SEASON_FROM_SETTINGS);
            } else {
                Intent startEloValuesActivity = new Intent(getActivity(), EloValuesActivity.class);
                startActivity(startEloValuesActivity);
                return true;
            }
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            // For list preferences, figure out the label of the selected value
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0) {
                // Set the summary to that label
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HomeScreen.getActivityComponent().inject(this);

        //Add this baseview to list of presenter baseviews
        mPresenter.addBaseView(this);

        //Preference change listeners should be registered when the activity is created

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Preference change listeners should be unregistered when the activity is destroyed
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    private Boolean getSeasonLoadedPref() {
        //Return the season loaded preference boolean
        return mSharedPrefs.getBoolean(getString(R.string.settings_season_loaded_key), getResources().getBoolean(R.bool.settings_season_loaded_default));
    }

    @Override
    public void onSeasonInitialized() {

    }

    @Override
    public void onSeasonLoadedFromDb(int requestType) {
        if (requestType == SimulatorModel.LOAD_SEASON_FROM_SETTINGS) {
            //If season is loaded from database from settings activity, start elo values activity if the fragment is added to activity
            if (isAdded()){
            Intent startEloValuesActivity = new Intent(getActivity(), EloValuesActivity.class);
            startActivity(startEloValuesActivity);}
        }
    }
}
