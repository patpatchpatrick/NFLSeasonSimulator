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
        void onDisplayStandings(String standings);
        void onDisplayScores(String scores);
        void onSeasonInitialized();
        void onSeasonLoadedFromDb();
    }

    interface SimulatorPresenter {
        void simulateWeek();
        void simulateSeason();
        void initializeSeason();
        void loadSeasonFromDatabase();
        void teamsInserted();
        void matchesInserted(Schedule schedule);
        void matchesQueried(int queryType, Cursor matchesCursor);
        void standingsUpdated(int queryType, Cursor standingsCursor);
        void destroyPresenter();
    }

    interface SimulatorModel {
        void insertMatch(Match match);
        void insertMatches(Schedule schedule);
        void insertTeam(Team team);
        void insertTeams(HashMap<String, Team> teamList);
        void updateMatch(Match match, Uri uri);
        void updateTeam(Team team, Uri uri);
        void queryStandings(int queryType);
        void queryMatches(int weekNumber);
        void destroyModel();

    }



}
