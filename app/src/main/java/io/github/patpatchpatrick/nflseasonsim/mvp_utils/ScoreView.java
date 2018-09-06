package io.github.patpatchpatrick.nflseasonsim.mvp_utils;

import android.database.Cursor;

public interface ScoreView {

    //Score view interface that is implemented by all views that display scores

    void onDisplayScores(int weekNumber, Cursor cursor, String scoresWeekNumberHeader, int queriedFrom);
    void onDisplayStandings(int standingsType, Cursor cursor, int queriedFrom);
}
