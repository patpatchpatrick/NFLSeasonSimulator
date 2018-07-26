package io.github.patpatchpatrick.nflseasonsim.presenter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.github.patpatchpatrick.nflseasonsim.MainActivity;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Data;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.NFLConstants;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Week;
import io.reactivex.Scheduler;

public class SimulatorPresenter extends BasePresenter<SimulatorMvpContract.SimulatorView>
        implements SimulatorMvpContract.SimulatorPresenter, Data {

    private HashMap<String, Team> mTeamList;
    private SimulatorModel mModel;

    public SimulatorPresenter(SimulatorMvpContract.SimulatorView view) {
        super(view);
        SimulatorModel model = new SimulatorModel(this);
        mModel = model;
    }

    @Override
    public void simulateWeek() {

    }

    @Override
    public void initializeSeason() {
        HashMap<String, Team> teamList = createTeams();
        Schedule seasonSchedule = createSchedule();
        simulateSeasonInternal(seasonSchedule);
        displayStandings();
    }

    @Override
    public void destroyPresenter() {
        mModel.destroyModel();
    }


    private HashMap<String, Team> createTeams() {
        mTeamList = new HashMap<String, Team>();
        mTeamList.put(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING,
                new Team(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING, NFLConstants.TEAM_ARIZONA_CARDINALS_ELO, 
                        NFLConstants.TEAM_ARIZONA_CARDINALS_OFFRAT, NFLConstants.TEAM_ARIZONA_CARDINALS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        mTeamList.put(NFLConstants.TEAM_ATLANTA_FALCONS_STRING,
                new Team("Atlanta Falcons", NFLConstants.TEAM_ATLANTA_FALCONS_ELO,
                        NFLConstants.TEAM_ATLANTA_FALCONS_OFFRAT, NFLConstants.TEAM_ATLANTA_FALCONS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        mTeamList.put(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING,
                new Team("Baltimore Ravens", NFLConstants.TEAM_BALTIMORE_RAVENS_ELO,
                        NFLConstants.TEAM_BALTIMORE_RAVENS_OFFRAT, NFLConstants.TEAM_BALTIMORE_RAVENS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        mTeamList.put(NFLConstants.TEAM_BUFFALO_BILLS_STRING,
                new Team("Buffalo Bills", NFLConstants.TEAM_BUFFALO_BILLS_ELO,
                        NFLConstants.TEAM_BUFFALO_BILLS_OFFRAT, NFLConstants.TEAM_BUFFALO_BILLS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        mTeamList.put(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING,
                new Team("Carolina Panthers", NFLConstants.TEAM_CAROLINA_PANTHERS_ELO,
                        NFLConstants.TEAM_CAROLINA_PANTHERS_OFFRAT, NFLConstants.TEAM_CAROLINA_PANTHERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        mTeamList.put(NFLConstants.TEAM_CHICAGO_BEARS_STRING,
                new Team("Chicago Bears", NFLConstants.TEAM_CHICAGO_BEARS_ELO,
                        NFLConstants.TEAM_CHICAGO_BEARS_OFFRAT, NFLConstants.TEAM_CHICAGO_BEARS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        mTeamList.put(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING,
                new Team("Cincinnati Bengals", NFLConstants.TEAM_CINCINNATI_BENGALS_ELO,
                        NFLConstants.TEAM_CINCINNATI_BENGALS_OFFRAT, NFLConstants.TEAM_CINCINNATI_BENGALS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        mTeamList.put(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING,
                new Team("Cleveland Browns", NFLConstants.TEAM_CLEVELAND_BROWNS_ELO,
                        NFLConstants.TEAM_CLEVELAND_BROWNS_OFFRAT, NFLConstants.TEAM_CLEVELAND_BROWNS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        mTeamList.put(NFLConstants.TEAM_DALLAS_COWBOYS_STRING,
                new Team("Dallas Cowboys", NFLConstants.TEAM_DALLAS_COWBOYS_ELO,
                        NFLConstants.TEAM_DALLAS_COWBOYS_OFFRAT, NFLConstants.TEAM_DALLAS_COWBOYS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        mTeamList.put(NFLConstants.TEAM_DENVER_BRONCOS_STRING,
                new Team("Denver Broncos", NFLConstants.TEAM_DENVER_BRONCOS_ELO,
                        NFLConstants.TEAM_DENVER_BRONCOS_OFFRAT, NFLConstants.TEAM_DENVER_BRONCOS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        mTeamList.put(NFLConstants.TEAM_DETROIT_LIONS_STRING,
                new Team("Detroit Lions", NFLConstants.TEAM_DETROIT_LIONS_ELO,
                        NFLConstants.TEAM_DETROIT_LIONS_OFFRAT, NFLConstants.TEAM_DETROIT_LIONS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        mTeamList.put(NFLConstants.TEAM_GREENBAY_PACKERS_STRING,
                new Team("Green Bay Packers", NFLConstants.TEAM_GREENBAY_PACKERS_ELO,
                        NFLConstants.TEAM_GREENBAY_PACKERS_OFFRAT, NFLConstants.TEAM_GREENBAY_PACKERS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        mTeamList.put(NFLConstants.TEAM_HOUSTON_TEXANS_STRING,
                new Team("Houston Texans", NFLConstants.TEAM_HOUSTON_TEXANS_ELO,
                        NFLConstants.TEAM_HOUSTON_TEXANS_OFFRAT, NFLConstants.TEAM_HOUSTON_TEXANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        mTeamList.put(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING,
                new Team("Indianapolis Colts", NFLConstants.TEAM_INDIANAPOLIS_COLTS_ELO,
                        NFLConstants.TEAM_INDIANAPOLIS_COLTS_OFFRAT, NFLConstants.TEAM_INDIANAPOLIS_COLTS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        mTeamList.put(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING,
                new Team("Jacksonville Jaguars", NFLConstants.TEAM_JACKSONVILLE_JAGUARS_ELO,
                        NFLConstants.TEAM_JACKSONVILLE_JAGUARS_OFFRAT, NFLConstants.TEAM_JACKSONVILLE_JAGUARS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        mTeamList.put(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING,
                new Team("Kansas City Chiefs", NFLConstants.TEAM_KANSASCITY_CHIEFS_ELO,
                        NFLConstants.TEAM_KANSASCITY_CHIEFS_OFFRAT, NFLConstants.TEAM_KANSASCITY_CHIEFS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        mTeamList.put(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING,
                new Team("Los Angeles Chargers", NFLConstants.TEAM_LOSANGELES_CHARGERS_ELO,
                        NFLConstants.TEAM_LOSANGELES_CHARGERS_OFFRAT, NFLConstants.TEAM_LOSANGELES_CHARGERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        mTeamList.put(NFLConstants.TEAM_LOSANGELES_RAMS_STRING,
                new Team("Los Angeles Rams", NFLConstants.TEAM_LOSANGELES_RAMS_ELO,
                        NFLConstants.TEAM_LOSANGELES_RAMS_OFFRAT, NFLConstants.TEAM_LOSANGELES_RAMS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        mTeamList.put(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING,
                new Team("Miami Dolphins", NFLConstants.TEAM_MIAMI_DOLPHINS_ELO,
                        NFLConstants.TEAM_MIAMI_DOLPHINS_OFFRAT, NFLConstants.TEAM_MIAMI_DOLPHINS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        mTeamList.put(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING,
                new Team("Minnesota Vikings", NFLConstants.TEAM_MINNESOTA_VIKINGS_ELO,
                        NFLConstants.TEAM_MINNESOTA_VIKINGS_OFFRAT, NFLConstants.TEAM_MINNESOTA_VIKINGS_DEFRAT, TeamEntry.DIVISION_NFC_NORTH, this));
        mTeamList.put(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING,
                new Team("New England Patriots", NFLConstants.TEAM_NEWENGLAND_PATRIOTS_ELO,
                        NFLConstants.TEAM_NEWENGLAND_PATRIOTS_OFFRAT, NFLConstants.TEAM_NEWENGLAND_PATRIOTS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        mTeamList.put(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING,
                new Team("New Orleans Saints", NFLConstants.TEAM_NEWORLEANS_SAINTS_ELO,
                        NFLConstants.TEAM_NEWORLEANS_SAINTS_OFFRAT, NFLConstants.TEAM_NEWORLEANS_SAINTS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        mTeamList.put(NFLConstants.TEAM_NEWYORK_GIANTS_STRING,
                new Team("New York Giants", NFLConstants.TEAM_NEWYORK_GIANTS_ELO,
                        NFLConstants.TEAM_NEWYORK_GIANTS_OFFRAT, NFLConstants.TEAM_NEWYORK_GIANTS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        mTeamList.put(NFLConstants.TEAM_NEWYORK_JETS_STRING,
                new Team("New York Jets", NFLConstants.TEAM_NEWYORK_JETS_ELO,
                        NFLConstants.TEAM_NEWYORK_JETS_OFFRAT, NFLConstants.TEAM_NEWYORK_JETS_DEFRAT, TeamEntry.DIVISION_AFC_EAST, this));
        mTeamList.put(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING,
                new Team("Oakland Raiders", NFLConstants.TEAM_OAKLAND_RAIDERS_ELO,
                        NFLConstants.TEAM_OAKLAND_RAIDERS_OFFRAT, NFLConstants.TEAM_OAKLAND_RAIDERS_DEFRAT, TeamEntry.DIVISION_AFC_WEST, this));
        mTeamList.put(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING,
                new Team("Philadelphia Eagles", NFLConstants.TEAM_PHILADELPHIA_EAGLES_ELO,
                        NFLConstants.TEAM_PHILADELPHIA_EAGLES_OFFRAT, NFLConstants.TEAM_PHILADELPHIA_EAGLES_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        mTeamList.put(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING,
                new Team("Pittsburgh Steelers", NFLConstants.TEAM_PITTSBURGH_STEELERS_ELO,
                        NFLConstants.TEAM_PITTSBURGH_STEELERS_OFFRAT, NFLConstants.TEAM_PITTSBURGH_STEELERS_DEFRAT, TeamEntry.DIVISION_AFC_NORTH, this));
        mTeamList.put(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING,
                new Team("San Francisco 49ers", NFLConstants.TEAM_SANFRANCISCO_49ERS_ELO,
                        NFLConstants.TEAM_SANFRANCISCO_49ERS_OFFRAT, NFLConstants.TEAM_SANFRANCISCO_49ERS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        mTeamList.put(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING,
                new Team("Seattle Seahawks", NFLConstants.TEAM_SEATTLE_SEAHAWKS_ELO,
                        NFLConstants.TEAM_SEATTLE_SEAHAWKS_OFFRAT, NFLConstants.TEAM_SEATTLE_SEAHAWKS_DEFRAT, TeamEntry.DIVISION_NFC_WEST, this));
        mTeamList.put(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING,
                new Team("Tampa Bay Buccaneers", NFLConstants.TEAM_TAMPABAY_BUCCANEERS_ELO,
                        NFLConstants.TEAM_TAMPABAY_BUCCANEERS_OFFRAT, NFLConstants.TEAM_TAMPABAY_BUCCANEERS_DEFRAT, TeamEntry.DIVISION_NFC_SOUTH, this));
        mTeamList.put(NFLConstants.TEAM_TENNESSEE_TITANS_STRING,
                new Team("Tennessee Titans", NFLConstants.TEAM_TENNESSEE_TITANS_ELO,
                        NFLConstants.TEAM_TENNESSEE_TITANS_OFFRAT, NFLConstants.TEAM_TENNESSEE_TITANS_DEFRAT, TeamEntry.DIVISION_AFC_SOUTH, this));
        mTeamList.put(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING,
                new Team("Washington Redskins", NFLConstants.TEAM_WASHINGTON_REDSKINS_ELO,
                        NFLConstants.TEAM_WASHINGTON_REDSKINS_OFFRAT, NFLConstants.TEAM_WASHINGTON_REDSKINS_DEFRAT, TeamEntry.DIVISION_NFC_EAST, this));
        return mTeamList;
    }


    private Schedule createSchedule() {

        //Initialize all schedule, weeks, and matches.  Add weeks to schedule.
        Schedule seasonSchedule = new Schedule();
        Week weekOne = new Week(1);
        Week weekTwo = new Week(2);
        Week weekThree = new Week(3);
        Week weekFour = new Week(4);
        Week weekFive = new Week(5);
        Week weekSix = new Week(6);
        Week weekSeven = new Week(7);
        Week weekEight = new Week(8);
        Week weekNine = new Week(9);
        Week weekTen = new Week(10);
        Week weekEleven = new Week(11);
        Week weekTwelve = new Week(12);
        Week weekThirteen = new Week(13);
        Week weekFourteen = new Week(14);
        Week weekFifteen = new Week(15);
        Week weekSixteen = new Week(16);
        Week weekSeventeen = new Week(17);
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 1, this));
        weekOne.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 1, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 2, this));
        weekTwo.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 2, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 3, this));
        weekThree.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 3, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 4, this));
        weekFour.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 4, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 5, this));
        weekFive.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 5, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 6, this));
        weekSix.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 6, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 7, this));
        weekSeven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 7, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 8, this));
        weekEight.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 8, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 9, this));
        weekNine.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 9, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 10, this));
        weekTen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 10, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 11, this));
        weekEleven.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 11, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 12, this));
        weekTwelve.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 12, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 13, this));
        weekThirteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 13, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 14, this));
        weekFourteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 14, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 15, this));
        weekFifteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 15, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 16, this));
        weekSixteen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), 16, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_OAKLAND_RAIDERS_STRING), mTeamList.get(NFLConstants.TEAM_KANSASCITY_CHIEFS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_MIAMI_DOLPHINS_STRING), mTeamList.get(NFLConstants.TEAM_BUFFALO_BILLS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_INDIANAPOLIS_COLTS_STRING), mTeamList.get(NFLConstants.TEAM_TENNESSEE_TITANS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CLEVELAND_BROWNS_STRING), mTeamList.get(NFLConstants.TEAM_BALTIMORE_RAVENS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DETROIT_LIONS_STRING), mTeamList.get(NFLConstants.TEAM_GREENBAY_PACKERS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_NEWYORK_JETS_STRING), mTeamList.get(NFLConstants.TEAM_NEWENGLAND_PATRIOTS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CHICAGO_BEARS_STRING), mTeamList.get(NFLConstants.TEAM_MINNESOTA_VIKINGS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CAROLINA_PANTHERS_STRING), mTeamList.get(NFLConstants.TEAM_NEWORLEANS_SAINTS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_JACKSONVILLE_JAGUARS_STRING), mTeamList.get(NFLConstants.TEAM_HOUSTON_TEXANS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_DALLAS_COWBOYS_STRING), mTeamList.get(NFLConstants.TEAM_NEWYORK_GIANTS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_PHILADELPHIA_EAGLES_STRING), mTeamList.get(NFLConstants.TEAM_WASHINGTON_REDSKINS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ATLANTA_FALCONS_STRING), mTeamList.get(NFLConstants.TEAM_TAMPABAY_BUCCANEERS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_CINCINNATI_BENGALS_STRING), mTeamList.get(NFLConstants.TEAM_PITTSBURGH_STEELERS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_ARIZONA_CARDINALS_STRING), mTeamList.get(NFLConstants.TEAM_SEATTLE_SEAHAWKS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_SANFRANCISCO_49ERS_STRING), mTeamList.get(NFLConstants.TEAM_LOSANGELES_RAMS_STRING), 17, this));
        weekSeventeen.addMatch(new Match(mTeamList.get(NFLConstants.TEAM_LOSANGELES_CHARGERS_STRING), mTeamList.get(NFLConstants.TEAM_DENVER_BRONCOS_STRING), 17, this));
        seasonSchedule.addWeek(weekOne);
        seasonSchedule.addWeek(weekTwo);
        seasonSchedule.addWeek(weekThree);
        seasonSchedule.addWeek(weekFour);
        seasonSchedule.addWeek(weekFive);
        seasonSchedule.addWeek(weekSix);
        seasonSchedule.addWeek(weekSeven);
        seasonSchedule.addWeek(weekEight);
        seasonSchedule.addWeek(weekNine);
        seasonSchedule.addWeek(weekTen);
        seasonSchedule.addWeek(weekEleven);
        seasonSchedule.addWeek(weekTwelve);
        seasonSchedule.addWeek(weekThirteen);
        seasonSchedule.addWeek(weekFourteen);
        seasonSchedule.addWeek(weekFifteen);
        seasonSchedule.addWeek(weekSixteen);
        seasonSchedule.addWeek(weekSeventeen);


        return seasonSchedule;


    }

    public void simulateSeasonInternal(Schedule seasonSchedule){
        //TODO internal code to test season simulation... in final app version, use the simulateSeason method callback
        //From week 1 to week 17 (full season), simulate the season
        int i = 1;
        while (i <= 17)  {
            seasonSchedule.getWeek(i).simulate();
            i++;
        }


    }

    @Override
    public void simulateSeason(Schedule seasonSchedule) {


    }

    private void displayStandings(){
        String standings = "";
        for (String team : mTeamList.keySet()) {
            standings += mTeamList.get(team).getName() + " ";
            standings += "Wins: " + mTeamList.get(team).getWins() + " ";
            standings += "Losses: " + mTeamList.get(team).getLosses() + "\n";
        }
        this.view.onDisplayStandings(standings);
    }


    @Override
    public void insertTeamCallback(Team team) {
        //Callback is received when a new team is created
        //The model is then notified to insert the team into the database
        mModel.insertTeam(team);
    }

    @Override
    public void insertMatchCallback(Match match) {
        //Callback is received when a new match is created
        //The model is then notified to insert the match into the database
        mModel.insertMatch(match);
    }

    @Override
    public void updateMatchCallback(Match match) {
        //Callback is received when a match is completed
        //The model is then notified to update the match in the database
        mModel.updateMatch(match);
    }

}
