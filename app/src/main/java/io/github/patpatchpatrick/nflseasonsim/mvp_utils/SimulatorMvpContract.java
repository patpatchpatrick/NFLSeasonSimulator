package io.github.patpatchpatrick.nflseasonsim.mvp_utils;

import android.database.Cursor;

import java.util.ArrayList;

import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;

public interface SimulatorMvpContract {

    interface SimulatorView {

        // button events
        void onSimulateWeekButtonClicked();
        void onSimulateSeasonButtonClicked();
        void onDisplayStandings(String standings);
    }

    interface SimulatorPresenter {
        void simulateWeek();
        void simulateSeason();
        void initializeSeason();
    }



}
