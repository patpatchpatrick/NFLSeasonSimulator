package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.database.Cursor;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;

public class Standings {

    public static void generateStandings() {
    }

    public static void generateTestPlayoffTeams(ArrayList<Team> divisionWinnersAFC, ArrayList<Team> divisionWinnersNFC, ArrayList<Team> potentialAFCWildCardTeams, ArrayList<Team> potentialNFCWildCardTeams){

        generateDivisionWinnerSeeds(divisionWinnersAFC);
        generateDivisionWinnerSeeds(divisionWinnersNFC);

        //Generate two wildcard teams for each conference
        generateWildCardTeams(potentialAFCWildCardTeams);
        generateWildCardTeams(potentialNFCWildCardTeams);

    }

    public static void generatePlayoffTeams(Cursor standingsCursor, HashMap<String, Team> teams) {

        ArrayList<Team> potentialAFCWildCardTeams = new ArrayList<>();
        ArrayList<Team> potentialNFCWildCardTeams = new ArrayList<>();
        ArrayList<Team> divisionWinnersAFC = new ArrayList<>();
        ArrayList<Team> divisionWinnersNFC = new ArrayList<>();

        standingsCursor.moveToPosition(-1);
        int i = 4;
        while (standingsCursor.moveToNext()) {

            String teamName = standingsCursor.getString(standingsCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_NAME));
            if (teamName == null){
                Log.d("nullError",  "1" + teamName);
            }
            if (teams == null){
                Log.d("nullErrr",  "null hashmap");
            }
            Team team = teams.get(teamName);


            //For every 4 teams, the first team is the division winner and is playoff eligible
            //Mark the team as playoff eligible division winner
            int teamConference = team.getConference();

            if (i % 4 == 0) {

                if (teamConference == TeamEntry.CONFERENCE_AFC){
                    divisionWinnersAFC.add(team);
                } else {
                    divisionWinnersNFC.add(team);
                }

                //If team is not the division winner, update them as not playoff eligible and
                // add them to the correct conference ArrayList of potential wildcard teams
            } else if (teamConference == TeamEntry.CONFERENCE_AFC) {
                team.setPlayoffEligible(TeamEntry.PLAYOFF_NOT_ELIGIBLE);
                potentialAFCWildCardTeams.add(team);
            } else {
                team.setPlayoffEligible(TeamEntry.PLAYOFF_NOT_ELIGIBLE);
                potentialNFCWildCardTeams.add(team);
            }
            i++;
        }

        generateDivisionWinnerSeeds(divisionWinnersAFC);
        generateDivisionWinnerSeeds(divisionWinnersNFC);

        //Generate two wildcard teams for each conference
        generateWildCardTeams(potentialAFCWildCardTeams);
        generateWildCardTeams(potentialNFCWildCardTeams);

        standingsCursor.close();

    }

    private static void generateWildCardTeams(ArrayList<Team> potentialWildCardTeams) {

        //Sort the wildCardTeams by win loss percentage in descending order
        Collections.sort(potentialWildCardTeams, new Comparator<Team>() {
            @Override
            public int compare(Team teamOne, Team teamTwo) {
                return teamOne.getWinLossPct() > teamTwo.getWinLossPct() ? -1 : (teamOne.getWinLossPct() < teamTwo.getWinLossPct()) ? 1 : 0;
            }
        });

        //If the top team has a better win loss percentage than the second team, set it as a wildcard team
        //If the second team has a better win loss percentage than the third team, set it as a wildcard team as well
        //Otherwise, run a random draw to randomly select teams that have the same win loss percentage to determine the wildcards

        if (potentialWildCardTeams.get(0).getWinLossPct() > potentialWildCardTeams.get(1).getWinLossPct()) {
            potentialWildCardTeams.get(0).setPlayoffEligible(5);
            if (potentialWildCardTeams.get(1).getWinLossPct() > potentialWildCardTeams.get(2).getWinLossPct()) {
                potentialWildCardTeams.get(1).setPlayoffEligible(6);
            } else {
                randomWildCardDraw(potentialWildCardTeams,  1);
            }
        } else {
           randomWildCardDraw(potentialWildCardTeams, 2);
        }
        
    }
    
    private static void randomWildCardDraw(ArrayList<Team> drawTeams, int numberOfTeamsToDraw) {

        //Run a random wildcard draw.  Select the "numberOfTeamsToDraw" from the drawTeams.

        int teamNumber;
        if (numberOfTeamsToDraw <= 1){
            teamNumber = 1;
        } else {
            teamNumber = 0;
        }

        //Add all of teams that have the same win loss percentage to the wildCardDrawTeams ArrayList
        //Then, randomly select 1 or 2 teams from this ArrayList as wildcard teams (depending on whether
        //the numberOfTeamsToDraw variable is set to 1 or 2.

        ArrayList<Team> wildCardDrawTeams = new ArrayList<>();
        wildCardDrawTeams.add(drawTeams.get(teamNumber));
        while (true) {
            if (teamNumber + 1 >= drawTeams.size()){
                Log.d("Max ", "Size reached");
                break;}
            if (drawTeams.get(teamNumber + 1).getWinLossPct() == drawTeams.get(teamNumber).getWinLossPct()) {
                wildCardDrawTeams.add(drawTeams.get(teamNumber + 1));
            } else {
                break;
            }
            teamNumber++;
        }
        int wildCardDrawSize = wildCardDrawTeams.size();
        int randomWildCardTeam = (int) (Math.random() * wildCardDrawSize);
        wildCardDrawTeams.get(randomWildCardTeam).setPlayoffEligible(6);

        if (numberOfTeamsToDraw > 1) {
            wildCardDrawTeams.remove(randomWildCardTeam);
            wildCardDrawSize = wildCardDrawTeams.size();
            randomWildCardTeam = (int) (Math.random() * wildCardDrawSize);
            wildCardDrawTeams.get(randomWildCardTeam).setPlayoffEligible(5);
        }
        
    }

    private static void generateDivisionWinnerSeeds(ArrayList<Team> divisionWinners){

        //Shuffle the arraylist to randomize it, then sort by division win percentage to determine playoff order

        Collections.shuffle(divisionWinners);

        //Sort the division winners by win loss percentage in descending order
        Collections.sort(divisionWinners, new Comparator<Team>() {
            @Override
            public int compare(Team teamOne, Team teamTwo) {
                return teamOne.getWinLossPct() > teamTwo.getWinLossPct() ? -1 : (teamOne.getWinLossPct() < teamTwo.getWinLossPct()) ? 1 : 0;
            }
        });


        //Set playoff order on all 4 division winners
        for (int i = 0; i <= 3; i++){
            divisionWinners.get(i).setPlayoffEligible(i + 1);
        }



    }

}
