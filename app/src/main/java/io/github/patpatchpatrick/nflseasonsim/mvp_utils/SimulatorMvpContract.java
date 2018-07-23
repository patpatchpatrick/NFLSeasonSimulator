package io.github.patpatchpatrick.nflseasonsim.mvp_utils;

public interface SimulatorMvpContract {

    interface SimulatorView {

        // button events
        void onSimulateWeekButtonClicked();
        void onSimulateSeasonButtonClicked();
    }

    interface SimulatorPresenter {
        void simulateWeek();
        void simulateSeason();
    }



}
