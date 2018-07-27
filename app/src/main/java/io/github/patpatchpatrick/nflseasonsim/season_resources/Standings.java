package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.database.Cursor;

import java.util.HashMap;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;

public class Standings {

    public static void generateStandings(){
    }

    public static void generatePlayoffTeams(Cursor standingsCursor, HashMap<String, Team> teams){

        standingsCursor.moveToPosition(-1);
        int i = 4;
        while (standingsCursor.moveToNext()) {
            //For every 4 teams, the first team is the division winner and is playoff eligible
            //Mark the team is playoff eligible division winner
            if (i % 4 == 0){
                String teamName =  standingsCursor.getString(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_NAME));
                teams.get(teamName).setPlayoffEligible(TeamEntry.PLAYOFF_DIVISION_WINNER);
            }
            i++;
        }

    }

}
