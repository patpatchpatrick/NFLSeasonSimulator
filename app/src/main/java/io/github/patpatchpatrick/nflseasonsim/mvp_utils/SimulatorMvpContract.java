package io.github.patpatchpatrick.nflseasonsim.mvp_utils;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
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
        void destroyPresenter();
    }

    interface SimulatorModel {
        void insertMatch(Match match);
        void insertTeam(Team team);
        void updateMatch(Match match);
        void updateTeam(Team team);
        void queryStandings();
        void destroyModel();

    }



}
