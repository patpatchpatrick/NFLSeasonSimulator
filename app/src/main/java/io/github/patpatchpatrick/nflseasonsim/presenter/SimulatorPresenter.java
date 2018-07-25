package io.github.patpatchpatrick.nflseasonsim.presenter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    public SimulatorPresenter(SimulatorMvpContract.SimulatorView view) {
        super(view);
    }

    @Override
    public void simulateWeek() {

    }

    @Override
    public void simulateSeason() {

    }

    @Override
    public void initializeSeason() {
        createTeams();
        createSchedule();

    }

    private void createTeams() {
        mTeamList = new HashMap<String, Team>();
        mTeamList.put(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING,
                new Team(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING, NFLConstants.TEAM_ARIZONA_CARDINALS_ELO, 
                        NFLConstants.TEAM_ARIZONA_CARDINALS_OFFRAT, NFLConstants.TEAM_ARIZONA_CARDINALS_DEFRAT, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.put(NFLConstants.TEAM_ATLANTA_FALCONS_STRING,
                new Team("Atlanta Falcons", NFLConstants.TEAM_ATLANTA_FALCONS_ELO,
                        NFLConstants.TEAM_ATLANTA_FALCONS_OFFRAT, NFLConstants.TEAM_ATLANTA_FALCONS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING,
                new Team("Baltimore Ravens", NFLConstants.TEAM_BALTIMORE_RAVENS_ELO,
                        NFLConstants.TEAM_BALTIMORE_RAVENS_OFFRAT, NFLConstants.TEAM_BALTIMORE_RAVENS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_BUFFALO_BILLS_STRING,
                new Team("Buffalo Bills", NFLConstants.TEAM_BUFFALO_BILLS_ELO,
                        NFLConstants.TEAM_BUFFALO_BILLS_OFFRAT, NFLConstants.TEAM_BUFFALO_BILLS_DEFRAT, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.put(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING,
                new Team("Carolina Panthers", NFLConstants.TEAM_CAROLINA_PANTHERS_ELO,
                        NFLConstants.TEAM_CAROLINA_PANTHERS_OFFRAT, NFLConstants.TEAM_CAROLINA_PANTHERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_CHICAGO_BEARS_STRING,
                new Team("Chicago Bears", NFLConstants.TEAM_CHICAGO_BEARS_ELO,
                        NFLConstants.TEAM_CHICAGO_BEARS_OFFRAT, NFLConstants.TEAM_CHICAGO_BEARS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING,
                new Team("Cincinnati Bengals", NFLConstants.TEAM_CINCINNATI_BENGALS_ELO,
                        NFLConstants.TEAM_CINCINNATI_BENGALS_OFFRAT, NFLConstants.TEAM_CINCINNATI_BENGALS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING,
                new Team("Cleveland Browns", NFLConstants.TEAM_CLEVELAND_BROWNS_ELO,
                        NFLConstants.TEAM_CLEVELAND_BROWNS_OFFRAT, NFLConstants.TEAM_CLEVELAND_BROWNS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_DALLAS_COWBOYS_STRING,
                new Team("Dallas Cowboys", NFLConstants.TEAM_DALLAS_COWBOYS_ELO,
                        NFLConstants.TEAM_DALLAS_COWBOYS_OFFRAT, NFLConstants.TEAM_DALLAS_COWBOYS_DEFRAT, TeamEntry.DIVISION_NFC_EAST));
        mTeamList.put(NFLConstants.TEAM_DENVER_BRONCOS_STRING,
                new Team("Denver Broncos", NFLConstants.TEAM_DENVER_BRONCOS_ELO,
                        NFLConstants.TEAM_DENVER_BRONCOS_OFFRAT, NFLConstants.TEAM_DENVER_BRONCOS_DEFRAT, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.put(NFLConstants.TEAM_DETROIT_LIONS_STRING,
                new Team("Detroit Lions", NFLConstants.TEAM_DETROIT_LIONS_ELO,
                        NFLConstants.TEAM_DETROIT_LIONS_OFFRAT, NFLConstants.TEAM_DETROIT_LIONS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_GREENBAY_PACKERS_STRING,
                new Team("Green Bay Packers", NFLConstants.TEAM_GREENBAY_PACKERS_ELO,
                        NFLConstants.TEAM_GREENBAY_PACKERS_OFFRAT, NFLConstants.TEAM_GREENBAY_PACKERS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_HOUSTON_TEXANS_STRING,
                new Team("Houston Texans", NFLConstants.TEAM_HOUSTON_TEXANS_ELO,
                        NFLConstants.TEAM_HOUSTON_TEXANS_OFFRAT, NFLConstants.TEAM_HOUSTON_TEXANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING,
                new Team("Indianapolis Colts", NFLConstants.TEAM_INDIANAPOLIS_COLTS_ELO,
                        NFLConstants.TEAM_INDIANAPOLIS_COLTS_OFFRAT, NFLConstants.TEAM_INDIANAPOLIS_COLTS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING,
                new Team("Jacksonville Jaguars", NFLConstants.TEAM_JACKSONVILLE_JAGUARS_ELO,
                        NFLConstants.TEAM_JACKSONVILLE_JAGUARS_OFFRAT, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING,
                new Team("Kansas City Chiefs", NFLConstants.TEAM_KANSASCITY_CHIEFS_ELO,
                        NFLConstants.TEAM_KANSASCITY_CHIEFS_OFFRAT, NFLConstants.TEAM_KANSASCITY_CHIEFS_DEFRAT, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.put(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING,
                new Team("Los Angeles Chargers", NFLConstants.TEAM_LOSANGELES_CHARGERS_ELO,
                        NFLConstants.TEAM_LOSANGELES_CHARGERS_OFFRAT, NFLConstants.TEAM_LOSANGELES_CHARGERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.put(NFLConstants.TEAM_LOSANGELES_RAMS_STRING,
                new Team("Los Angeles Rams", NFLConstants.TEAM_LOSANGELES_RAMS_ELO,
                        NFLConstants.TEAM_LOSANGELES_RAMS_OFFRAT, NFLConstants.TEAM_LOSANGELES_RAMS_DEFRAT, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.put(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING,
                new Team("Miami Dolphins", NFLConstants.TEAM_MIAMI_DOLPHINS_ELO,
                        NFLConstants.TEAM_MIAMI_DOLPHINS_OFFRAT, NFLConstants.TEAM_MIAMI_DOLPHINS_DEFRAT, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.put(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING,
                new Team("Minnesota Vikings", NFLConstants.TEAM_MINNESOTA_VIKINGS_ELO,
                        NFLConstants.TEAM_MINNESOTA_VIKINGS_OFFRAT, NFLConstants.TEAM_MINNESOTA_VIKINGS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING,
                new Team("New England Patriots", NFLConstants.TEAM_NEWENGLAND_PATRIOTS_ELO,
                        NFLConstants.TEAM_NEWENGLAND_PATRIOTS_OFFRAT, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_DEFRAT, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.put(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING,
                new Team("New Orleans Saints", NFLConstants.TEAM_NEWORLEANS_SAINTS_ELO,
                        NFLConstants.TEAM_NEWORLEANS_SAINTS_OFFRAT, NFLConstants.TEAM_NEWORLEANS_SAINTS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_NEWYORK_GIANTS_STRING,
                new Team("New York Giants", NFLConstants.TEAM_NEWYORK_GIANTS_ELO,
                        NFLConstants.TEAM_NEWYORK_GIANTS_OFFRAT, NFLConstants.TEAM_NEWYORK_GIANTS_DEFRAT, TeamEntry.DIVISION_NFC_EAST));
        mTeamList.put(NFLConstants.TEAM_NEWYORK_JETS_STRING,
                new Team("New York Jets", NFLConstants.TEAM_NEWYORK_JETS_ELO,
                        NFLConstants.TEAM_NEWYORK_JETS_OFFRAT, NFLConstants.TEAM_NEWYORK_JETS_DEFRAT, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.put(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING,
                new Team("Oakland Raiders", NFLConstants.TEAM_OAKLAND_RAIDERS_ELO,
                        NFLConstants.TEAM_OAKLAND_RAIDERS_OFFRAT, NFLConstants.TEAM_OAKLAND_RAIDERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.put(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING,
                new Team("Philadelphia Eagles", NFLConstants.TEAM_PHILADELPHIA_EAGLES_ELO,
                        NFLConstants.TEAM_PHILADELPHIA_EAGLES_OFFRAT, NFLConstants.TEAM_PHILADELPHIA_EAGLES_DEFRAT, TeamEntry.DIVISION_NFC_EAST));
        mTeamList.put(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING,
                new Team("Pittsburgh Steelers", NFLConstants.TEAM_PITTSBURGH_STEELERS_ELO,
                        NFLConstants.TEAM_PITTSBURGH_STEELERS_OFFRAT, NFLConstants.TEAM_PITTSBURGH_STEELERS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.put(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING,
                new Team("San Francisco 49ers", NFLConstants.TEAM_SANFRANCISCO_49ERS_ELO,
                        NFLConstants.TEAM_SANFRANCISCO_49ERS_OFFRAT, NFLConstants.TEAM_SANFRANCISCO_49ERS_DEFRAT, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.put(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING,
                new Team("Seattle Seahawks", NFLConstants.TEAM_SEATTLE_SEAHAWKS_ELO,
                        NFLConstants.TEAM_SEATTLE_SEAHAWKS_OFFRAT, NFLConstants.TEAM_SEATTLE_SEAHAWKS_DEFRAT, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.put(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING,
                new Team("Tampa Bay Buccaneers", NFLConstants.TEAM_TAMPABAY_BUCCANEERS_ELO,
                        NFLConstants.TEAM_TAMPABAY_BUCCANEERS_OFFRAT, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_TENNESSEE_TITANS_STRING,
                new Team("Tennessee Titans", NFLConstants.TEAM_TENNESSEE_TITANS_ELO,
                        NFLConstants.TEAM_TENNESSEE_TITANS_OFFRAT, NFLConstants.TEAM_TENNESSEE_TITANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.put(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING,
                new Team("Washington Redskins", NFLConstants.TEAM_WASHINGTON_REDSKINS_ELO,
                        NFLConstants.TEAM_WASHINGTON_REDSKINS_OFFRAT, NFLConstants.TEAM_WASHINGTON_REDSKINS_DEFRAT, TeamEntry.DIVISION_NFC_EAST));
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
