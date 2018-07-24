package io.github.patpatchpatrick.nflseasonsim.dagger;

import javax.inject.Singleton;

import dagger.Component;
import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;
import io.github.patpatchpatrick.nflseasonsim.MainActivity;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;

@Singleton @Component(modules = { AppModule.class})
public interface AppComponent {

    //Dagger AppComponent
    //The inject methods below indicate which objects we will inject the module data into

    void inject(DaggerApplication application);

    void inject(Team team);

    void inject(Match match);

    void inject(MainActivity mainActivity);

}
