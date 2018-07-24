package io.github.patpatchpatrick.nflseasonsim;

import android.app.Application;

import io.github.patpatchpatrick.nflseasonsim.dagger.AppComponent;
import io.github.patpatchpatrick.nflseasonsim.dagger.AppModule;
import io.github.patpatchpatrick.nflseasonsim.dagger.DaggerAppComponent;

public class DaggerApplication extends Application {

    //DaggerApplication that is used as our main application in order to use Dagger 2

    static AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        // Build our app component and inject it

        appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();

        appComponent.inject(this);
    }

    public static  AppComponent getAppComponent() {

        // Static method to return our app component if other objects/classes need access to it

        return appComponent;
    }


}
