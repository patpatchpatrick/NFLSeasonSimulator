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

    private Team mTeam1;
    private Team mTeam2;
    private int mWeek;
    private Team winner;
    private Team loser;
    private int winnerScore;
    private int loserScore;
    private Boolean mComplete;


    public Match(Team team1, Team team2, int week) {

        //Inject match with dagger to get contentResolver
        DaggerApplication.getAppComponent().inject(this);

        mTeam1 = team1;
        mTeam2 = team2;
        mWeek = week;

        insertMatch();
    }

    private void insertMatch() {

        ContentValues values = new ContentValues();
        values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE, mTeam1.getName());
        values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO, mTeam2.getName());
        values.put(MatchEntry.COLUMN_MATCH_WEEK, mWeek);

        //Insert values into database
        Uri uri = contentResolver.insert(MatchEntry.CONTENT_URI, values);
    }

    protected void simulate() {
        Log.d(LOG_TAG, "Team 1 " + mTeam1.getName());
        Log.d(LOG_TAG, "Team 2 " + mTeam2.getName());
        ELORatingSystem.simulateMatch(mTeam1.getELO(), mTeam2.getELO());
    }


}
