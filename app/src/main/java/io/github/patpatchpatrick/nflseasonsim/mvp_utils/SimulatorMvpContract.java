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
        void onPriorSimulatedDataLoaded();
        void onDataDeleted();
        void setCurrentWeekPreference(int currentWeek);
    }

    interface SimulatorPresenter {
        void simulateWeek();
        void simulatePlayoffWeek();
        void queryCurrentSeasonStandings();
        void simulateSeason();
        void initializeSeason();
        void initiatePlayoffs();
        boolean getPlayoffsStarted();
        void setPlayoffsStarted(boolean playoffsStarted);
        void loadSeasonFromDatabase();
        void loadAlreadySimulatedData();
        void loadAlreadySimulatedPlayoffData();
        void simulatorTeamsInserted();
        void seasonTeamsInserted();
        void addBaseView(BaseView baseView);
        void simulatorMatchesInserted(int insertType);
        void seasonMatchesInserted(int insertType);
        void simulatorMatchesQueried(int queryType, Cursor matchesCursor, int queryFrom);
        void currentSeasonMatchesQueried(int queryType, Cursor matchesCursor, int queryFrom);
        void simulatorStandingsQueried(int queryType, Cursor standingsCursor);
        void currentSeasonStandingsQueried(int queryType, Cursor standingsCursor);
        void queryCurrentSeasonMatches(int week, boolean singleMatch, int queryFrom);
        void resetSeason();
        void resetTeamLastSeasonElos();
        void resetTeamCurrentSeasonElos();
        void resetTeamUserElos();
        void setTeamUserElos();
        void dataDeleted();
        void destroyPresenter();
    }

    interface SimulatorModel {
        void setSimulatorSchedule(Schedule schedule);
        void setSeasonSchedule(Schedule schedule);
        void setSimulatorTeamList(HashMap<String, Team> teamList);
        void setSeasonTeamList(HashMap<String, Team> teamList);
        void createTeamLogoMap();
        int getLogo(String teamName);
        void setTeamEloMap(HashMap<String, Double> teamEloMap);
        Team getSimulatorTeam(String teamName);
        Team getCurrentSeasonTeam(String teamName);
        HashMap<String, Team> getSimulatorTeamList();
        HashMap<String, Team> getSeasonTeamList();
        ArrayList<Team> getTeamArrayList();
        ArrayList<String> getTeamNameArrayList();
        HashMap<String, Double> getTeamEloMap();
        Schedule getSchedule();
        void insertMatch(Match match);
        void insertSimulatorMatches(int insertType);
        void insertSimulatorMatches(int insertType, Week week);
        void insertSeasonMatches(int insertType);
        void insertTeam(Team team);
        void insertSimulatorTeams();
        void insertSeasonTeams();
        void updateMatch(Match match, Uri uri);
        void updateTeam(Team team, Uri uri);
        void querySimulatorStandings(int queryType);
        void queryCurrentSeasonStandings(int queryType);
        void querySimulatorMatches(int weekNumber, boolean singleMatch, int queryFrom);
        void queryCurrentSeasonMatches(int weekNumber, boolean singleMatch, int queryFrom);
        void deleteAllData();
        void destroyModel();
    }




}
