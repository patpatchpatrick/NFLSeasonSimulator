package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.content.ContentValues;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;

public class Team {

    private String mName;
    private int mElo;
    private int mOffRating;
    private int mDefRating;
    private int mCurrentWins;
    private int mCurrentLosses;
    private int mCurrentDraws;
    private int mDivision;

    public Team(String name, int elo, int offRating, int defRating, int division){
        mName = name;
        mElo = elo;
        mOffRating =  offRating;
        mDefRating = defRating;
        mCurrentWins = 0;
        mCurrentLosses = 0;
        mCurrentDraws =  0;
        mDivision = division;

        insertTeam();
    }

    private void insertTeam(){
        ContentValues values = new ContentValues();
        values.put(TeamEntry.COLUMN_TEAM_NAME, mName);
        values.put(TeamEntry.COLUMN_TEAM_ELO, mElo);
        values.put(TeamEntry.COLUMN_TEAM_OFF_RATING, mOffRating);
        values.put(TeamEntry.COLUMN_TEAM_DEF_RATING, mDefRating);
        values.put(TeamEntry.COLUMN_TEAM_CURRENT_WINS, mCurrentWins);
        values.put(TeamEntry.COLUMN_TEAM_CURRENT_LOSSES, mCurrentLosses);
        values.put(TeamEntry.COLUMN_TEAM_CURRENT_DRAWS, mCurrentDraws);
        values.put(TeamEntry.COLUMN_TEAM_DIVISION, mDivision);

    }
}
