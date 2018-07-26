package io.github.patpatchpatrick.nflseasonsim.mvp_utils;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
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
        void simulateSeason(Schedule seasonSchedule);
        void initializeSeason();
    }

    interface SimulatorModel {
        void onUpdateDatabase();
        void insertTeams(HashMap<String, Team> teamList);
        void insertMatches(Schedule schedule);
        void updateSimulatedScheduleMatches(Schedule schedule);

    }



}
