package io.github.patpatchpatrick.nflseasonsim.dagger;

import android.content.ContentResolver;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

@Module
public class AppModule {

    //AppModule used for Dagger2
    //Provide other classes/objects with access to certain Application Components listed within the module

    private final DaggerApplication application;
    private final ContentResolver contentResolver;

    public AppModule(DaggerApplication app) {
        this.application = app;
        this.contentResolver = app.getContentResolver();
    }

    @Provides
    @Singleton
    Context providesApplicationContext() {
        return application;
    }

    @Provides
    @Singleton
    ContentResolver providesContentResolver() {
        return contentResolver;
    }

    @Provides
    SimulatorMvpContract.SimulatorPresenter providesSimulatorPresenter(SimulatorMvpContract.SimulatorView simulatorView) {
        return new SimulatorPresenter(simulatorView);
    }




}
