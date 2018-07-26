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

    //Name
    private String mName;

    //Division
    private int mDivision;

    //Ratings
    private double mElo;
    private double mOffRating;
    private double mDefRating;

    //Standings
    private int mCurrentWins;
    private int mCurrentLosses;
    private int mCurrentDraws;
    private double mWinLossPct;
    private int mDivisionStanding;

    //Stats
    private int mPointsFor;
    private int mPointsAllowed;

    public Team(String name, double elo, double offRating, double defRating, int division) {

        mName = name;
        mElo = elo;
        mOffRating = offRating;
        mDefRating = defRating;
        mCurrentWins = 0;
        mCurrentLosses = 0;
        mCurrentDraws = 0;
        mWinLossPct = 0;
        mDivisionStanding = 0;
        mDivision = division;
        mPointsFor = 0;
        mPointsAllowed = 0;

    }

    public String getName() {
        return mName;
    }

    public double getELO() {
        return mElo;
    }

    public void setELO(double elo) {
        mElo = elo;
    }

    public void win() {
        mCurrentWins++;
        mWinLossPct = (double) mCurrentWins / (double) (mCurrentWins + mCurrentLosses);

    }

    public int getWins() {
        return mCurrentWins;
    }

    public int getLosses() {
        return mCurrentLosses;
    }

    public int getDraws(){
        return mCurrentDraws;
    }

    public double getOffRating(){
        return mOffRating;
    }

    public double getDefRating(){
        return mDefRating;
    }

    public int getDivision(){
        return mDivision;
    }

    public void lose() {
        mCurrentLosses++;
        mWinLossPct = (double) mCurrentWins / (double) (mCurrentWins + mCurrentLosses);
    }

    public void addPointsFor(int pointsFor) {
        mPointsFor += pointsFor;
    }

    public void addPointsAllowed(int pointsAllowed) {
        mPointsAllowed += pointsAllowed;
    }
}
