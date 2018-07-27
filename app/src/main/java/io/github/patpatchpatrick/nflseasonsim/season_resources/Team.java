package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.net.Uri;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;

public class Team {

    //Data Interface
    private Data mData;

    //Uri
    private Uri mUri;

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
    private int mCurrentDivisionWins;
    private int mCurrentDivisionLosses;
    private double mCurrentDivisionWinLossPct;
    private double mWinLossPct;
    private int mDivisionStanding;
    private int mPlayoffEligible;

    //Stats
    private int mPointsFor;
    private int mPointsAllowed;

    public Team(String name, double elo, double offRating, double defRating, int division, Data data) {

        mData = data;
        mName = name;
        mElo = elo;
        mOffRating = offRating;
        mDefRating = defRating;
        mCurrentWins = 0;
        mCurrentLosses = 0;
        mCurrentDraws = 0;
        mWinLossPct = 0;
        mCurrentDivisionWins = 0;
        mCurrentDivisionLosses = 0;
        mCurrentDivisionWinLossPct = 0;
        mDivisionStanding = 0;
        mDivision = division;
        mPointsFor = 0;
        mPointsAllowed = 0;
        mPlayoffEligible = TeamEntry.PLAYOFF_NOT_ELIGIBLE;

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

        mData.updateTeamCallback(this, mUri);
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

    public double getWinLossPct() {
        return mWinLossPct;
    }

    public int getDivision(){
        return mDivision;
    }

    public void lose() {
        mCurrentLosses++;
        mWinLossPct = (double) mCurrentWins / (double) (mCurrentWins + mCurrentLosses);

        mData.updateTeamCallback(this, mUri);
    }

    public void divisionalWin(){
        mCurrentDivisionWins++;
        mCurrentDivisionWinLossPct = (double) mCurrentDivisionWins / (double) (mCurrentDivisionWins + mCurrentDivisionLosses);
    }

    public void divisionalLoss(){
        mCurrentDivisionLosses++;
        mCurrentDivisionWinLossPct = (double) mCurrentDivisionWins / (double) (mCurrentDivisionWins + mCurrentDivisionLosses);
    }

    public int getDivisionWins(){
        return mCurrentDivisionWins;
    }

    public int getDivisionLosses(){
        return mCurrentDivisionLosses;
    }

    public double getDivisionWinLossPct(){
        return mCurrentDivisionWinLossPct;
    }

    public void addPointsFor(int pointsFor) {
        mPointsFor += pointsFor;
    }

    public void addPointsAllowed(int pointsAllowed) {
        mPointsAllowed += pointsAllowed;
    }

    public void setUri(Uri uri){
        mUri = uri;
    }

    public Uri getUri(){
        return mUri;
    }

    public void setPlayoffEligible(int playoffEligible){
        mPlayoffEligible = playoffEligible;
        mData.updateTeamCallback(this, mUri);
    }

    public int getPlayoffEligible(){
        return mPlayoffEligible;
    }
}
