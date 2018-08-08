package io.github.patpatchpatrick.nflseasonsim.dagger;

import javax.inject.Singleton;

import dagger.Component;
import io.github.patpatchpatrick.nflseasonsim.EloRecyclerViewAdapter;
import io.github.patpatchpatrick.nflseasonsim.EloValuesActivity;
import io.github.patpatchpatrick.nflseasonsim.MainActivity;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;

@Singleton @Component(modules = { ActivityModule.class})
public interface ActivityComponent {

    //Dagger ActivityComponent (used for MainActivity objects)
    //The inject methods below indicate which objects we will inject the module data into

    void inject(EloValuesActivity eloValuesActivity);
    void inject(MainActivity mainActivity);
    void inject(SimulatorPresenter simulatorPresenter);
    void inject(SimulatorModel simulatorModel);
    void inject(Match match);
    void inject(EloRecyclerViewAdapter eloRecyclerViewAdapter);

}
