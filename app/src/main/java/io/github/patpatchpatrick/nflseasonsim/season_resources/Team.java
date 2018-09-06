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
    private String mShortName;

    //Division and Conference
    private int mDivision;
    private int mConference;

    //Ratings
    private double mDefaultElo;
    private double mUserElo;
    private double mElo;
    private double mTeamRanking;
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

    //Current Season
    //If team is from current season, value will be 2, otherwise will be 1 if it is a simulator team
    private int mCurrentSeason;

    public Team(String name, String shortName, double elo, double teamRanking, double offRating, double defRating, int division, Data data) {

        //Constructor used when originally creating team


        mData = data;
        mName = name;
        mShortName = shortName;
        mDefaultElo = elo;
        mUserElo = 0;
        mElo = elo;
        mTeamRanking = teamRanking;
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
        mCurrentSeason = TeamEntry.CURRENT_SEASON_NO;

        //Set conference value based on division value (all AFC divisions are ints less than 4)
        if (mDivision <= 4){
            mConference = TeamEntry.CONFERENCE_AFC;
        } else {
            mConference = TeamEntry.CONFERENCE_NFC;
        }

    }

    public Team(String name, String shortName, double elo, double teamRanking, double offRating, double defRating, int division, Data data, int currentSeason) {

        //Constructor used when originally creating team


        mData = data;
        mName = name;
        mShortName = shortName;
        mDefaultElo = elo;
        mUserElo = 0;
        mElo = elo;
        mTeamRanking = teamRanking;
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
        mCurrentSeason = currentSeason;

        //Set conference value based on division value (all AFC divisions are ints less than 4)
        if (mDivision <= 4){
            mConference = TeamEntry.CONFERENCE_AFC;
        } else {
            mConference = TeamEntry.CONFERENCE_NFC;
        }

    }

    public Team(String name, String shortName, double elo, double defaultElo,double userElo, double teamRanking, double offRating, double defRating, int division, Data data, int wins, int losses, int divWins, int divLosses, double winLossPct, double divWinLossPct, int playoffEligible,  Uri uri, int currentSeason) {

        mTeamRanking =  teamRanking;
        mData = data;
        mName = name;
        mShortName = shortName;
        mElo = elo;
        mDefaultElo = defaultElo;
        mUserElo = userElo;
        mOffRating = offRating;
        mDefRating = defRating;
        mCurrentWins = wins;
        mCurrentLosses = losses;
        mCurrentDraws = 0;
        mWinLossPct = winLossPct;
        mCurrentDivisionWins = divWins;
        mCurrentDivisionLosses = divLosses;
        mCurrentDivisionWinLossPct = divWinLossPct;
        mDivisionStanding = 0;
        mDivision = division;
        mPointsFor = 0;
        mPointsAllowed = 0;
        mPlayoffEligible = playoffEligible;
        mUri = uri;
        mCurrentSeason = currentSeason;

        //Set conference value based on division value (all AFC divisions are ints less than 4)
        if (mDivision <= 4){
            mConference = TeamEntry.CONFERENCE_AFC;
        } else {
            mConference = TeamEntry.CONFERENCE_NFC;
        }

    }

    public String getName() {
        return mName;
    }

    public double getElo() {
        return mElo;
    }

    public void setElo(double elo) {
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

    public int getConference(){
        return mConference;
    }

    public double getDefaultElo(){
        return mDefaultElo;
    }

    public void resetElo(){
        //Set team elos based on last seasons elos
        mElo = mDefaultElo;
    }

    public void setFutureElos(){
        //Set team elos based on future ranking
        mElo = 1700 - (mTeamRanking * 12.5);
    }

    public double getFutureElo(){
        double futureElo =  mElo = 1700 - (mTeamRanking * 12.5);
        return futureElo;
    }

    public void setUserElo(){
        //Set the user defined elo value for the team and update the database
        mUserElo = mElo;
        mData.updateTeamCallback(this, mUri);
    }

    public Double getTeamRanking(){
        return mTeamRanking;
    }

    public Double getUserElo() {
        if (mUserElo == 0){
            return mElo;
        } else {
            return mUserElo;
        }
    }

    public String getShortName(){
        return mShortName;
    }

    public int getCurrentSeason() {return mCurrentSeason;}
}
