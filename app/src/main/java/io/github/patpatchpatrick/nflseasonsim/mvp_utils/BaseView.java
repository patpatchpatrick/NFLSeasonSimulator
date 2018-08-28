package io.github.patpatchpatrick.nflseasonsim.mvp_utils;

public interface BaseView {

    //Base view interface that is implemented by all views

    void onSeasonInitialized(int initializedFrom);
    void onSeasonLoadedFromDb(int requestType);
}
