package io.github.patpatchpatrick.nflseasonsim.dagger;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.patpatchpatrick.nflseasonsim.MainActivity;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

@Module
public class ActivityModule {

    //ActivityModule used for Dagger2
    //Provide other classes/objects with access to certain Activity Components listed within the module

    private final SimulatorPresenter presenter;
    private final SimulatorModel model;
    private final ContentResolver contentResolver;
    private final SharedPreferences sharedPreferences;

    public ActivityModule(MainActivity mainActivity) {
        this.presenter = new SimulatorPresenter(mainActivity);
        this.model = new SimulatorModel();
        this.contentResolver = mainActivity.getContentResolver();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
    }

    @Provides
    @Singleton
    SimulatorPresenter providesSimulatorPresenter() {
        return presenter;
    }

    @Provides
    @Singleton
    SimulatorMvpContract.SimulatorPresenter providesSimulatorMvpPresenter() {
        return presenter;
    }

    @Provides
    @Singleton
    SimulatorModel providesSimulatorModel() {
        return model;
    }

    @Provides
    @Singleton
    ContentResolver providesContentResolver() {
        return contentResolver;
    }

    @Provides
    @Singleton
    SharedPreferences providesSharedPreferences() {
        return sharedPreferences;
    }



}
