package io.github.patpatchpatrick.nflseasonsim.presenter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.view.View;

import java.util.ArrayList;

import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;

public class SimulatorPresenter extends BasePresenter<SimulatorMvpContract.SimulatorView>
        implements SimulatorMvpContract.SimulatorPresenter {

    private ArrayList<Team> mTeamList;

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
        mTeamList = new ArrayList<Team>();
        mTeamList.add(new Team("Arizona Cardinals", 1471, 1, 1, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.add(new Team("Atlanta Falcons", 1648, 1, 1, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.add(new Team("Baltimore Ravens",  1550, 1, 1, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.add(new Team("Buffalo Bills", 1501, 1, 1, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.add(new Team("Carolina Panthers", 1572, 1, 1,  TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.add(new Team("Chicago Bears", 1413, 1, 1, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.add(new Team("Cincinnati Bengals", 1459, 1, 1, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.add(new Team("Cleveland Browns", 1200, 1, 1, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.add(new Team("Dallas Cowboys", 1568, 1, 1,  TeamEntry.DIVISION_NFC_EAST));
        mTeamList.add(new Team("Denver Broncos", 1423, 1, 1,  TeamEntry.DIVISION_AFC_WEST));
        mTeamList.add(new Team("Detroit Lions", 1533, 1, 1, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.add(new Team("Green Bay Packers", 1455, 1, 1, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.add(new Team("Houston Texans", 1344, 1, 1, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.add(new Team("Indianapolis Colts", 1358, 1, 1, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.add(new Team("Jacksonville Jaguars", 1550, 1,  1, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.add(new Team("Kansas City Chiefs", 1604, 1, 1, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.add(new Team("Los Angeles Chargers", 1565, 1, 1, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.add(new Team("Los Angeles Rams", 1542, 1, 1,  TeamEntry.DIVISION_NFC_WEST));
        mTeamList.add(new Team("Miami Dolphins", 1422, 1, 1,  TeamEntry.DIVISION_AFC_EAST));
        mTeamList.add(new Team("Minnesota Vikings", 1651, 1, 1, TeamEntry.DIVISION_NFC_NORTH));
        mTeamList.add(new Team("New England Patriots", 1724, 1, 1, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.add(new Team("New Orleans Saints", 1624, 1, 1, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.add(new Team("New York Giants", 1365, 1, 1, TeamEntry.DIVISION_NFC_EAST));
        mTeamList.add(new Team("New York Jets", 1396, 1, 1, TeamEntry.DIVISION_AFC_EAST));
        mTeamList.add(new Team("Oakland Raiders", 1445, 1, 1, TeamEntry.DIVISION_AFC_WEST));
        mTeamList.add(new Team("Philadelphia Eagles", 1718, 1, 1, TeamEntry.DIVISION_NFC_EAST));
        mTeamList.add(new Team("Pittsburgh Steelers", 1641, 1, 1, TeamEntry.DIVISION_AFC_NORTH));
        mTeamList.add(new Team("San Francisco 49ers", 1452, 1, 1, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.add(new Team("Seattle Seahawks", 1565, 1, 1, TeamEntry.DIVISION_NFC_WEST));
        mTeamList.add(new Team("Tampa Bay Buccaneers", 1452, 1, 1, TeamEntry.DIVISION_NFC_SOUTH));
        mTeamList.add(new Team("Tennessee Titans", 1491, 1, 1, TeamEntry.DIVISION_AFC_SOUTH));
        mTeamList.add(new Team("Washington Redskins", 1455, 1, 1,  TeamEntry.DIVISION_NFC_EAST));
    }


}
