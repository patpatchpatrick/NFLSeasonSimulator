package io.github.patpatchpatrick.nflseasonsim.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class SeasonSimContract {

    //Contract for DB used to store data for this app

    //URI Information
    public static final String CONTENT_AUTHORITY = "io.github.patpatchpatrick.nflseasonsim";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_TEAM = "team";

    private SeasonSimContract() {
    }

    public static final class TeamEntry implements BaseColumns {

        //The MIME type of the {@link #CONTENT_URI} for a list of teams.
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TEAM;


        //The MIME type of the {@link #CONTENT_URI} for a single team.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TEAM;

        //URI for Team Ratings table
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_TEAM);


        //Define table and columns for team data
        public static final String TABLE_NAME = "team";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_TEAM_NAME = "teamName";
        public static final String COLUMN_TEAM_ELO = "teamELO";
        public static final String COLUMN_TEAM_OFF_RATING = "teamOffensiveRating";
        public static final String COLUMN_TEAM_DEF_RATING = "teamDefensiveRating";
        public static final String COLUMN_TEAM_CURRENT_WINS = "teamWins";
        public static final String COLUMN_TEAM_CURRENT_LOSSES = "teamLosses";
        public static final String COLUMN_TEAM_CURRENT_DRAWS = "teamDraws";


    }
}
