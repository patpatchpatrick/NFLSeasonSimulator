package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.net.Uri;

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
    private double mWinLossPct;
    private int mDivisionStanding;

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
        mDivisionStanding = 0;
        mDivision = division;
        mPointsFor = 0;
        mPointsAllowed = 0;

        //Callback to the presenter to insert team into database
        mData.insertTeamCallback(this);

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
        mData.updateTeamCallback(this);
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
        mData.updateTeamCallback(this);
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
}
