package io.github.patpatchpatrick.nflseasonsim.mvp_utils;

import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Standings;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Week;

public interface SimulatorMvpContract {

    //Contract for interaction between MainActivity view, Presenter and Model

    interface SimulatorView extends BaseView{
        void onDisplayStandings(int standingsType, Cursor cursor);
        void onDisplayScores(int weekNumber, Cursor cursor, String scoresWeekNumberHeader, boolean matchesPlayed);
        void onPriorSimulatedDataLoaded();
        void onDataDeleted();
        void setCurrentWeekPreference(int currentWeek);
    }

    interface SimulatorPresenter {
        void simulateWeek();
        void simulatePlayoffWeek();
        void simulateSeason();
        void initializeSeason();
        void initiatePlayoffs();
        boolean getPlayoffsStarted();
        void setPlayoffsStarted(boolean playoffsStarted);
        void loadSeasonFromDatabase(int requestType);
        void loadAlreadySimulatedData();
        void loadAlreadySimulatedPlayoffData();
        void teamsInserted();
        void addBaseView(BaseView baseView);
        void matchesInserted(int insertType);
        void matchesQueried(int queryType, Cursor matchesCursor,  boolean matchesPlayed);
        void teamsOrStandingsQueried(int queryType, Cursor standingsCursor);
        void resetSeason();
        void resetTeamElos();
        void resetTeamFutureElos();
        void resetTeamUserElos();
        void setTeamUserElos();
        void dataDeleted();
        void destroyPresenter();
    }

    interface SimulatorModel {
        void setSchedule(Schedule schedule);
        void setTeamList(HashMap<String, Team> teamList);
        void createTeamLogoMap();
        int getLogo(String teamName);
        void setTeamEloMap(HashMap<String, Double> teamEloMap);
        Team getTeam(String teamName);
        HashMap<String, Team> getTeamList();
        ArrayList<Team> getTeamArrayList();
        ArrayList<String> getTeamNameArrayList();
        HashMap<String, Double> getTeamEloMap();
        Schedule getSchedule();
        void insertMatch(Match match);
        void insertMatches(int insertType);
        void insertMatches(int insertType, Week week);
        void insertTeam(Team team);
        void insertTeams();
        void updateMatch(Match match, Uri uri);
        void updateTeam(Team team, Uri uri);
        void queryStandings(int queryType);
        void queryMatches(int weekNumber, boolean singleMatch, boolean matchesPlayed);
        void deleteAllData();
        void destroyModel();
    }




}
