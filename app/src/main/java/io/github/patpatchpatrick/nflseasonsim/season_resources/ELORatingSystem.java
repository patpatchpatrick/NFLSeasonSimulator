package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.nfc.Tag;
import android.util.Log;

public class ELORatingSystem {

    private static final String LOG_TAG = ELORatingSystem.class.getSimpleName();

    public static void simulateMatch(double eloTeamOne, double eloTeamTwo){

        double probTeamOneWin = probabilityOfTeamOneWinning(eloTeamOne, eloTeamTwo);
        Log.d(LOG_TAG, "" + probTeamOneWin);

    }

    private static double probabilityOfTeamOneWinning(double eloTeamOne, double eloTeamTwo) {
        return 1.0 /(1 + Math.pow(10,
                (eloTeamTwo - eloTeamOne) / 400.0));
    }

}
