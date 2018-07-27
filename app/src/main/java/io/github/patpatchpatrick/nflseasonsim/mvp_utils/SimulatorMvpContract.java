package io.github.patpatchpatrick.nflseasonsim.mvp_utils;

import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Standings;
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
        void teamsInserted();
        void matchesInserted(Schedule schedule);
        void standingsUpdated(Cursor standingsCursor);
        void destroyPresenter();
    }

    interface SimulatorModel {
        void insertMatch(Match match);
        void insertMatches(Schedule schedule);
        void insertTeam(Team team);
        void insertTeams(HashMap<String, Team> teamList);
        void updateMatch(Match match, Uri uri);
        void updateTeam(Team team, Uri uri);
        void queryStandings();
        void destroyModel();

    }



}
