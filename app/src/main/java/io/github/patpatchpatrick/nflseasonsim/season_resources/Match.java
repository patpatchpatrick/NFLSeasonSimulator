package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;

public class Match {

    @Inject
    ContentResolver contentResolver;

    private Team mTeam1;
    private Team mTeam2;
    private int mWeek;
    private Team winner;
    private Team loser;
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


}
