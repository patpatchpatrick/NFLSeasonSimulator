package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;
import io.github.patpatchpatrick.nflseasonsim.MainActivity;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;

public class Team {

    @Inject
    ContentResolver contentResolver;

    private String mName;
    private double mElo;
    private int mOffRating;
    private int mDefRating;
    private int mCurrentWins;
    private int mCurrentLosses;
    private int mCurrentDraws;
    private int mDivision;

    public Team(String name, double elo, int offRating, int defRating, int division){

        //Inject team with dagger to get contentResolver
        DaggerApplication.getAppComponent().inject(this);

        mName = name;
        mElo = elo;
        mOffRating =  offRating;
        mDefRating = defRating;
        mCurrentWins = 0;
        mCurrentLosses = 0;
        mCurrentDraws =  0;
        mDivision = division;

        //Insert team into database
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

        //Insert values into database
        Uri uri = contentResolver.insert(TeamEntry.CONTENT_URI, values);

    }

    public String getName(){
        return mName;
    }

    public double getELO() {
        return (double) mElo;
    }

    public void  setELO( double elo ) {
        mElo = elo;
    }

    public void win() {mCurrentWins ++;}

    public void lose() {mCurrentLosses ++;}
}
