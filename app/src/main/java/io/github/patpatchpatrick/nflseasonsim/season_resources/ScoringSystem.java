package io.github.patpatchpatrick.nflseasonsim.season_resources;

public class ScoringSystem {

    private static int LOSING_SCORE_ZERO = 1400;
    private static int LOSING_SCORE_TWO = 1430;
    private static int LOSING_SCORE_THREE = 2205;
    private static int LOSING_SCORE_FIVE = 2215;
    private static int LOSING_SCORE_SIX = 2885;
    private static int LOSING_SCORE_SEVEN = 4435;
    private static int LOSING_SCORE_EIGHT = 4470;
    private static int LOSING_SCORE_NINE = 4770;
    private static int LOSING_SCORE_TEN = 6170;
    private static int LOSING_SCORE_ELEVEN = 6200;
    private static int LOSING_SCORE_TWELVE = 6375;
    private static int LOSING_SCORE_THIRTEEN = 7375;
    private static int LOSING_SCORE_FOURTEEN = 8775;
    private static int LOSING_SCORE_FIFTEEN = 8875;
    private static int LOSING_SCORE_SIXTEEN = 9325;
    private static int LOSING_SCORE_SEVENTEEN = 10625;
    private static int LOSING_SCORE_EIGHTEEN = 10675;
    private static int LOSING_SCORE_NINETEEN = 10875;
    private static int LOSING_SCORE_TWENTY = 11675;
    private static int LOSING_SCORE_TWENTYONE = 12475;
    private static int LOSING_SCORE_TWENTYTWO = 12575;
    private static int LOSING_SCORE_TWENTYTHREE = 12875;
    private static int LOSING_SCORE_TWENTYFOUR = 13525;
    private static int LOSING_SCORE_TWENTYFIVE = 13575;
    private static int LOSING_SCORE_TWENTYSIX = 13675;
    private static int LOSING_SCORE_TWENTYSEVEN = 13975;
    private static int LOSING_SCORE_TWENTYEIGHT = 14275;
    private static int LOSING_SCORE_TWENTYNINE = 14300;
    private static int LOSING_SCORE_THIRTY = 14375;
    private static int LOSING_SCORE_THIRTYONE =  14575;
    private static int LOSING_SCORE_THIRTYTWO = 14595;
    private static int LOSING_SCORE_THIRTYTHREE = 14630;
    private static int LOSING_SCORE_THIRTYFOUR = 14700;
    private static int LOSING_SCORE_THIRTYFIVE = 14760;
    private static int LOSING_SCORE_THIRTYEIGHT = 14790;
    private static int LOSING_SCORE_FOURTYONE = 14810;

    protected static int getLosingScore(double matchOutcome, double probTeamOneWin ){

        if (matchOutcome <= probTeamOneWin) {
            double marginOfVictory = matchOutcome / probTeamOneWin ;
        } else {
            double marginOfVictory = (matchOutcome - probTeamOneWin) / (1.0 - probTeamOneWin);
        }

        double loserScorePercentage = (double) Math.random();
        double losingScoreValue = loserScorePercentage * (double) LOSING_SCORE_FOURTYONE;

        if (losingScoreValue > LOSING_SCORE_THIRTYEIGHT) {return 41;}
        if (losingScoreValue > LOSING_SCORE_THIRTYFIVE) {return 38;}
        if (losingScoreValue > LOSING_SCORE_THIRTYFOUR) {return 35;}
        if (losingScoreValue > LOSING_SCORE_THIRTYTHREE) {return 34;}
        if (losingScoreValue > LOSING_SCORE_THIRTYTWO) {return 33;}
        if (losingScoreValue > LOSING_SCORE_THIRTYONE) {return 32;}
        if (losingScoreValue > LOSING_SCORE_THIRTY) {return 31;}
        if (losingScoreValue > LOSING_SCORE_TWENTYNINE) {return 30;}
        if (losingScoreValue > LOSING_SCORE_TWENTYEIGHT) {return 29;}
        if (losingScoreValue > LOSING_SCORE_TWENTYSEVEN) {return 28;}
        if (losingScoreValue > LOSING_SCORE_TWENTYSIX) {return 27;}
        if (losingScoreValue > LOSING_SCORE_TWENTYFIVE) {return 26;}
        if (losingScoreValue > LOSING_SCORE_TWENTYFOUR) {return 25;}
        if (losingScoreValue > LOSING_SCORE_TWENTYTHREE) {return 24;}
        if (losingScoreValue > LOSING_SCORE_TWENTYTWO) {return 23;}
        if (losingScoreValue > LOSING_SCORE_TWENTYONE) {return 22;}
        if (losingScoreValue > LOSING_SCORE_TWENTY) {return 21;}
        if (losingScoreValue > LOSING_SCORE_NINETEEN) {return 20;}
        if (losingScoreValue > LOSING_SCORE_EIGHTEEN) {return 19;}
        if (losingScoreValue > LOSING_SCORE_SEVENTEEN) {return 18;}
        if (losingScoreValue > LOSING_SCORE_SIXTEEN) {return 17;}
        if (losingScoreValue > LOSING_SCORE_FIFTEEN) {return 16;}
        if (losingScoreValue > LOSING_SCORE_FOURTEEN) {return 15;}
        if (losingScoreValue > LOSING_SCORE_THIRTEEN) {return 14;}
        if (losingScoreValue > LOSING_SCORE_TWELVE) {return 13;}
        if (losingScoreValue > LOSING_SCORE_ELEVEN) {return 12;}
        if (losingScoreValue > LOSING_SCORE_TEN) {return 11;}
        if (losingScoreValue > LOSING_SCORE_NINE) {return 10;}
        if (losingScoreValue > LOSING_SCORE_EIGHT) {return 9;}
        if (losingScoreValue > LOSING_SCORE_SEVEN) {return 8;}
        if (losingScoreValue > LOSING_SCORE_SIX) {return 7;}
        if (losingScoreValue > LOSING_SCORE_FIVE) {return 6;}
        if (losingScoreValue > LOSING_SCORE_THREE) {return 5;}
        if (losingScoreValue > LOSING_SCORE_TWO) {return 3;}
        if (losingScoreValue > LOSING_SCORE_ZERO) {return 2;}
        return 0;

    }


}
