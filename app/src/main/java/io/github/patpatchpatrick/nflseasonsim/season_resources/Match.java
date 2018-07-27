package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class Match {

    private static final String LOG_TAG = Match.class.getSimpleName();

    @Inject
    ContentResolver contentResolver;

    private Data mData;
    private Uri matchUri;
    private Team mTeam1;
    private Team mTeam2;
    private int mWeek;
    private Team winner;
    private Team loser;
    private int mTeam1Score;
    private int mTeam2Score;
    private Boolean matchComplete;
    private Boolean divisionalMatchup;


    public Match(Team team1, Team team2, int week, Data data) {

        //Inject match with dagger to get contentResolver
        DaggerApplication.getAppComponent().inject(this);

        mData = data;
        mTeam1 = team1;
        mTeam2 = team2;
        mWeek = week;
        matchComplete = false;

        //Determine if match is a divisional matchup
        if (mTeam1.getDivision() == mTeam2.getDivision()){
            divisionalMatchup = true;
        } else {
            divisionalMatchup = false;
        }

    }

    protected void simulate() {

        //Simulate match to determine if team one won
        boolean teamOneWon = ELORatingSystem.simulateMatch(this, mTeam1, mTeam2);

        //Update team records based on outcome and mark match as complete
        if (teamOneWon){
            if (divisionalMatchup){
                mTeam1.divisionalWin();
                mTeam2.divisionalLoss();
            }
            mTeam1.win();
            mTeam2.lose();
        } else {
            if (divisionalMatchup){
                mTeam1.divisionalLoss();
                mTeam2.divisionalWin();
            }
            mTeam1.lose();
            mTeam2.win();
        }
        matchComplete = true;

        //Callback to presenter to update match in database with match result
        mData.updateMatchCallback(this, matchUri);

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

    public int getWeek(){
        return mWeek;
    }


}
