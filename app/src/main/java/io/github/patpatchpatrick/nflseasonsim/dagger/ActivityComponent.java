package io.github.patpatchpatrick.nflseasonsim.dagger;

import javax.inject.Singleton;

import dagger.Component;
import io.github.patpatchpatrick.nflseasonsim.EloRecyclerViewAdapter;
import io.github.patpatchpatrick.nflseasonsim.EloValuesActivity;
import io.github.patpatchpatrick.nflseasonsim.HomeScreen;
import io.github.patpatchpatrick.nflseasonsim.MainActivity;
import io.github.patpatchpatrick.nflseasonsim.MatchPredictorActivity;
import io.github.patpatchpatrick.nflseasonsim.NextWeekMatchesActivity;
import io.github.patpatchpatrick.nflseasonsim.ScoresRecyclerViewAdapter;
import io.github.patpatchpatrick.nflseasonsim.SeasonStandingsRecyclerViewAdapter;
import io.github.patpatchpatrick.nflseasonsim.SettingsFragment;
import io.github.patpatchpatrick.nflseasonsim.StandingsActivity;
import io.github.patpatchpatrick.nflseasonsim.StandingsRecyclerViewAdapter;
import io.github.patpatchpatrick.nflseasonsim.WeeklyMatchesRecyclerViewAdapter;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimDbHelper;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;

@Singleton @Component(modules = { ActivityModule.class})
public interface ActivityComponent {

    //Dagger ActivityComponent (used for MainActivity objects)
    //The inject methods below indicate which objects we will inject the module data into

    void inject(HomeScreen homeScreen);
    void inject(EloValuesActivity eloValuesActivity);
    void inject(MainActivity mainActivity);
    void inject(MatchPredictorActivity matchPredictorActivity);
    void inject(NextWeekMatchesActivity nextWeekMatchesActivity);
    void inject(StandingsActivity standingsActivity);
    void inject(SimulatorPresenter simulatorPresenter);
    void inject(SimulatorModel simulatorModel);
    void inject(Match match);
    void inject(EloRecyclerViewAdapter eloRecyclerViewAdapter);
    void inject(ScoresRecyclerViewAdapter scoresRecyclerViewAdapter);
    void inject(StandingsRecyclerViewAdapter standingsRecyclerViewAdapter);
    void inject(SeasonStandingsRecyclerViewAdapter seasonStandingsRecyclerViewAdapter);
    void inject(WeeklyMatchesRecyclerViewAdapter weeklyMatchesRecyclerViewAdapter);
    void inject(SettingsFragment settingsFragment);
    void inject(SeasonSimDbHelper seasonSimDbHelper);

}
