package io.github.patpatchpatrick.nflseasonsim.dagger;

import android.content.ContentResolver;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;

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




}
