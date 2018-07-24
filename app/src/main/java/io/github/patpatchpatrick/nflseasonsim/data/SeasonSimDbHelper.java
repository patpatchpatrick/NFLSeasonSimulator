package io.github.patpatchpatrick.nflseasonsim.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;


public class SeasonSimDbHelper extends SQLiteOpenHelper{

    public static final String LOG_TAG = SeasonSimDbHelper.class.getSimpleName();

    //DB Name and version
    private static final String DATABASE_NAME = "seasonsim.db";
    private static final int DATABASE_VERSION = 1;


    public SeasonSimDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create a String that contains the SQL statement to create the goals table
        String SQL_CREATE_TEAM_TABLE =  "CREATE TABLE " + TeamEntry.TABLE_NAME + " ("
                + TeamEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TeamEntry.COLUMN_TEAM_NAME + " TEXT NOT NULL, "
                + TeamEntry.COLUMN_TEAM_ELO + " INTEGER NOT NULL, "
                + TeamEntry.COLUMN_TEAM_OFF_RATING + " INTEGER NOT NULL, "
                + TeamEntry.COLUMN_TEAM_DEF_RATING + " INTEGER NOT NULL, "
                + TeamEntry.COLUMN_TEAM_CURRENT_WINS + " INTEGER NOT NULL DEFAULT 0, "
                + TeamEntry.COLUMN_TEAM_CURRENT_LOSSES + " INTEGER NOT NULL DEFAULT 0, "
                + TeamEntry.COLUMN_TEAM_CURRENT_DRAWS  + " INTEGER NOT NULL DEFAULT 0, "
                + TeamEntry.COLUMN_TEAM_DIVISION  + " INTEGER NOT NULL DEFAULT 0);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_TEAM_TABLE);

        // Create a String that contains the SQL statement to create the streaks table
        String SQL_CREATE_MATCHES_TABLE =  "CREATE TABLE " + MatchEntry.TABLE_NAME + " ("
                + MatchEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MatchEntry.COLUMN_MATCH_TEAM_ONE + " TEXT NOT NULL, "
                + MatchEntry.COLUMN_MATCH_TEAM_TWO + " TEXT NOT NULL, "
                + MatchEntry.COLUMN_MATCH_WEEK + " INTEGER NOT NULL, "
                + MatchEntry.COLUMN_MATCH_COMPLETE  + " INTEGER NOT NULL DEFAULT 0);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_MATCHES_TABLE);


    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}