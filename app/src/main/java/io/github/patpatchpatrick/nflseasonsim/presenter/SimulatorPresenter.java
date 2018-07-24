package io.github.patpatchpatrick.nflseasonsim.presenter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;

import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.NFLConstants;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Week;

public class SimulatorPresenter extends BasePresenter<SimulatorMvpContract.SimulatorView>
        implements SimulatorMvpContract.SimulatorPresenter {

    private HashMap<String, Team> mTeamList;

    public SimulatorPresenter(SimulatorMvpContract.SimulatorView view){
        super(view);
    }

    @Override
    public void simulateWeek() {

    }

    @Override
    public void simulateSeason() {

    }

    @Override
    public void initializeSeason(){
        createTeams();
        createSchedule();

    }

    private void createTeams(){
        mTeamList = new HashMap<String, Team>();
        mTeamList.put(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING, 
                new Team(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING, 1471, 1, 1, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.put(NFLConstants.TEAM_ATLANTA_FALCONS_STRING,
                new Team("Atlanta Falcons", 1648, 1, 1, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING,
                new Team("Baltimore Ravens",  1550, 1, 1, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_BUFFALO_BILLS_STRING,
                new Team("Buffalo Bills", 1501, 1, 1, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.put(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING,
                new Team("Carolina Panthers", 1572, 1, 1,  TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_CHICAGO_BEARS_STRING,
                new Team("Chicago Bears", 1413, 1, 1, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING,
                new Team("Cincinnati Bengals", 1459, 1, 1, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING,
                new Team("Cleveland Browns", 1200, 1, 1, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_DALLAS_COWBOYS_STRING,
                new Team("Dallas Cowboys", 1568, 1, 1,  TeamEntry.DIVISION_NFC_EAST));
        mTeamList.put(NFLConstants.TEAM_DENVER_BRONCOS_STRING,
                new Team("Denver Broncos", 1423, 1, 1,  TeamEntry.DIVISION_AFC_WEST));
        mTeamList.put(NFLConstants.TEAM_DETROIT_LIONS_STRING,
                new Team("Detroit Lions", 1533, 1, 1, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_GREENBAY_PACKERS_STRING,
                new Team("Green Bay Packers", 1455, 1, 1, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_HOUSTON_TEXANS_STRING,
                new Team("Houston Texans", 1344, 1, 1, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING,
                new Team("Indianapolis Colts", 1358, 1, 1, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING,
                new Team("Jacksonville Jaguars", 1550, 1,  1, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING,
                new Team("Kansas City Chiefs", 1604, 1, 1, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.put(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING,
                new Team("Los Angeles Chargers", 1565, 1, 1, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.put(NFLConstants.TEAM_LOSANGELES_RAMS_STRING,
                new Team("Los Angeles Rams", 1542, 1, 1,  TeamEntry.DIVISION_NFC_WEST));
        mTeamList.put(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING,
                new Team("Miami Dolphins", 1422, 1, 1,  TeamEntry.DIVISION_AFC_EAST));
        mTeamList.put(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING,
                new Team("Minnesota Vikings", 1651, 1, 1, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING,
                new Team("New England Patriots", 1724, 1, 1, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.put(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING,
                new Team("New Orleans Saints", 1624, 1, 1, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_NEWYORK_GIANTS_STRING,
                new Team("New York Giants", 1365, 1, 1, TeamEntry.DIVISION_NFC_EAST));
        mTeamList.put(NFLConstants.TEAM_NEWYORK_JETS_STRING,
                new Team("New York Jets", 1396, 1, 1, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.put(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING,
                new Team("Oakland Raiders", 1445, 1, 1, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.put(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING,
                new Team("Philadelphia Eagles", 1718, 1, 1, TeamEntry.DIVISION_NFC_EAST));
        mTeamList.put(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING,
                new Team("Pittsburgh Steelers", 1641, 1, 1, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING,
                new Team("San Francisco 49ers", 1452, 1, 1, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.put(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING,
                new Team("Seattle Seahawks", 1565, 1, 1, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.put(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING,
                new Team("Tampa Bay Buccaneers", 1452, 1, 1, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_TENNESSEE_TITANS_STRING,
                new Team("Tennessee Titans", 1491, 1, 1, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING,
                new Team("Washington Redskins", 1455, 1, 1,  TeamEntry.DIVISION_NFC_EAST));
    }


    private void createSchedule() {
        Schedule schedule = new Schedule();
        Week weekOne = new Week(1);
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 1));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 1));
        weekOne.simulate();

    }


}
