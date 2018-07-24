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
    public static final String PATH_MATCH = "match";

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
        public static final String COLUMN_TEAM_DIVISION = "teamDivision";

        //Define input variables for team divisions
        public static final int DIVISION_AFC_NORTH = 1;
        public static final int DIVISION_AFC_EAST = 2;
        public static final int DIVISION_AFC_SOUTH = 3;
        public static final int DIVISION_AFC_WEST = 4;
        public static final int DIVISION_NFC_NORTH = 5;
        public static final int DIVISION_NFC_EAST = 6;
        public static final int DIVISION_NFC_SOUTH = 7;
        public static final int DIVISION_NFC_WEST = 8;
    }

    public static final class MatchEntry implements BaseColumns{

        //The MIME type of the {@link #CONTENT_URI} for a list of matches.
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MATCH;


        //The MIME type of the {@link #CONTENT_URI} for a single match.
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MATCH;

        //URI for Matches table
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI,  PATH_MATCH);


        //Define table and columns for streaks data
        public static final String TABLE_NAME = "Matches";

        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_MATCH_TEAM_ONE = "matchTeamOne";
        public static final String COLUMN_MATCH_TEAM_TWO = "matchTeamTwo";
        public static final String COLUMN_MATCH_WEEK = "matchWeek";
        public static final String COLUMN_MATCH_COMPLETE = "matchComplete";

        //Define input variables for match table
        public static final int MATCH_COMPLETE_NO = 0;
        public static final int MATCH_COMPLETE_YES = 1;




    }
}
