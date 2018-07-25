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

        insertMatch();
    }

    private void insertMatch() {

        ContentValues values = new ContentValues();
        values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE, mTeam1.getName());
        values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO, mTeam2.getName());
        values.put(MatchEntry.COLUMN_MATCH_WEEK, mWeek);

        //Insert values into database
        Uri uri = contentResolver.insert(MatchEntry.CONTENT_URI, values);
        matchUri = uri;
    }

    protected void simulate() {

        //Simulate match to determine if team one won
        boolean teamOneWon = ELORatingSystem.simulateMatch(mTeam1, mTeam2);

        //Update team records based on outcome and mark match as complete
        if (teamOneWon){
            mTeam1.win();
            mTeam2.lose();
        } else {
            mTeam1.lose();
            mTeam2.win();
        }
        matchComplete = true;

        //Update match database scores and match complete values
        ContentValues values = new ContentValues();
        values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE_SCORE, mTeam1Score);
        values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO_SCORE, mTeam2Score);
        if (matchComplete){
        values.put(MatchEntry.COLUMN_MATCH_COMPLETE, MatchEntry.MATCH_COMPLETE_YES);}

        int rowsUpdated = contentResolver.update(matchUri, values, null, null);

    }

    protected void setTeam1Score(int score){
        mTeam1Score = score;
    }

    protected void setmTeam2Score(int score) {
        mTeam2Score = score;
    }



}
