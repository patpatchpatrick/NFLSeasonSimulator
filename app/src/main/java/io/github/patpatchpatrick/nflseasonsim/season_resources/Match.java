package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;

public class Match {

    private static final String LOG_TAG = Match.class.getSimpleName();

    @Inject
    ContentResolver contentResolver;

    private Uri matchUri;
    private Team mTeam1;
    private Team mTeam2;
    private int mWeek;
    private Team winner;
    private Team loser;
    private int mTeam1Score;
    private int mTeam2Score;
    private Boolean matchComplete;


    public Match(Team team1, Team team2, int week) {

        //Inject match with dagger to get contentResolver
        DaggerApplication.getAppComponent().inject(this);

        mTeam1 = team1;
        mTeam2 = team2;
        mWeek = week;
        matchComplete = false;
    }

    protected void simulate() {

        //Simulate match to determine if team one won
        boolean teamOneWon = ELORatingSystem.simulateMatch(this, mTeam1, mTeam2);

        //Update team records based on outcome and mark match as complete
        if (teamOneWon){
            mTeam1.win();
            mTeam2.lose();
        } else {
            mTeam1.lose();
            mTeam2.win();
        }
        matchComplete = true;

    }

    protected void setTeam1Score(int score){
        mTeam1Score = score;
        mTeam1.addPointsFor(score);
        mTeam2.addPointsAllowed(score);
    }

    protected void setmTeam2Score(int score) {
        mTeam2Score = score;
        mTeam2.addPointsFor(score);
        mTeam1.addPointsAllowed(score);
    }

    public void setUri(Uri uri){
        matchUri = uri;
    }

    public Team getTeam1(){
        return mTeam1;
    }

    public Team getTeam2(){
        return mTeam2;
    }

    public int getTeam1Score(){
        return mTeam1Score;
    }

    public int getTeam2Score(){
        return mTeam2Score;
    }

    public Uri getUri(){
        return matchUri;
    }


}
