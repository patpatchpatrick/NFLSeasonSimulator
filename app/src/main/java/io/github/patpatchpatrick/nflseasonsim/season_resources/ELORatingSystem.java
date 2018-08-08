package io.github.patpatchpatrick.nflseasonsim.season_resources;

public class ELORatingSystem {

    // ELO constant used to determine magnitude that ratings change when a team wins
    private static double constantK = 32;

    private static final String LOG_TAG = ELORatingSystem.class.getSimpleName();

    public static boolean simulateMatch(Match match, Team teamOne, Team teamTwo){

        double eloTeamOne = teamOne.getElo();
        double eloTeamTwo = teamTwo.getElo();

        //Simulate a match between two teams with two elo ratings
        //If the first team wins, return true, otherwise return false

        //Calculate probability of the first team winning (double between 0-1)
        double probTeamOneWin = probabilityOfTeamOneWinning(eloTeamOne, eloTeamTwo);

        //Calculate a random double value between 0-1
        double matchOutcome = (double) Math.random();

        //If the random double is less than the probability of the first team winning, they won and set boolean to true
        //If not, they lost and set boolean to false
        boolean teamOneWon;
        if (matchOutcome <= probTeamOneWin) {
            teamOneWon = true;
        } else {
            teamOneWon = false;
        }

        //Get the losers score
        int losingScore = ScoringSystem.getLosingScore();

        //Get the winners score
        int winningScore = ScoringSystem.getWinningScore(losingScore, matchOutcome, probTeamOneWin);

        //Set the scores in the match for the winners and losers
        if (teamOneWon) {
            match.setTeam1Score(winningScore);
            match.setmTeam2Score(losingScore);
        } else {
            match.setTeam1Score(losingScore);
            match.setmTeam2Score(winningScore);
        }

        //Update the teams ratings based on the outcome, and return the outcome boolean
        updateRatings(teamOne, teamTwo, probTeamOneWin, teamOneWon);
        return teamOneWon;
    }

    private static double probabilityOfTeamOneWinning(double eloTeamOne, double eloTeamTwo) {
        //Returns probably of team one winning based on their ELO
        return 1.0 /(1 + Math.pow(10,
                (eloTeamTwo - eloTeamOne) / 400.0));
    }

    private static void updateRatings(Team teamOne, Team teamTwo, double probTeamOneWin, boolean teamOneWon){
        //Updates the ratings of teamOne and teamTwo, based on the outcome of match

        //Get the elos of the teams
        double eloTeamOne = teamOne.getElo();
        double eloTeamTwo = teamTwo.getElo();

        //Get the probability of team two winning from the probability of team one winning
        double probTeamTwoWin = (1.0 - probTeamOneWin);

        //Calculate the new elos for both teams depending on outcome of match
        if (teamOneWon) {
            eloTeamOne = eloTeamOne + constantK * (1.0 - probTeamOneWin);
            eloTeamTwo = eloTeamTwo + constantK * (0.0 - probTeamTwoWin);
        } else {
            eloTeamOne = eloTeamOne + constantK * (0.0 - probTeamOneWin);
            eloTeamTwo = eloTeamTwo + constantK * (1.0 - probTeamTwoWin);
        }

        //Set the new ELO ratings for the teams
        teamOne.setElo(eloTeamOne);
        teamTwo.setElo(eloTeamTwo);

    }

}
