package io.github.patpatchpatrick.nflseasonsim.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;

public class SimulatorModel implements SimulatorMvpContract.SimulatorModel {

    private final SimulatorMvpContract.SimulatorPresenter mPresenter;

    @Inject
    ContentResolver contentResolver;

    public SimulatorModel(SimulatorMvpContract.SimulatorPresenter presenter){
        mPresenter = presenter;

        //Inject team with dagger to get contentResolver
        DaggerApplication.getAppComponent().inject(this);
    }


    @Override
    public void onUpdateDatabase() {

    }

    @Override
    public void insertTeams(HashMap<String, Team> teamList) {

        for (String teamString : teamList.keySet()){
            Team team = teamList.get(teamString);
            String name = team.getName();
            double elo = team.getELO();
            double offRating = team.getOffRating();
            double defRating = team.getDefRating();
            int currentWins = team.getWins();
            int currentLosses = team.getLosses();
            int currentDraws = team.getDraws();
            int division = team.getDivision();

            ContentValues values = new ContentValues();
            values.put(TeamEntry.COLUMN_TEAM_NAME, name);
            values.put(TeamEntry.COLUMN_TEAM_ELO, elo);
            values.put(TeamEntry.COLUMN_TEAM_OFF_RATING, offRating);
            values.put(TeamEntry.COLUMN_TEAM_DEF_RATING, defRating);
            values.put(TeamEntry.COLUMN_TEAM_CURRENT_WINS, currentWins);
            values.put(TeamEntry.COLUMN_TEAM_CURRENT_LOSSES, currentLosses);
            values.put(TeamEntry.COLUMN_TEAM_CURRENT_DRAWS, currentDraws);
            values.put(TeamEntry.COLUMN_TEAM_DIVISION, division);

            //Insert values into database
            Uri uri = contentResolver.insert(TeamEntry.CONTENT_URI, values);

        }

    }


}
