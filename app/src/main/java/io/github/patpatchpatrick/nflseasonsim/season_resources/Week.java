package io.github.patpatchpatrick.nflseasonsim.season_resources;

import android.util.Log;

import java.util.ArrayList;

public class Week {

    private ArrayList<Match> mMatches;
    private int mWeekNumber;
    private int mNumberMatchesUpdated = 0;
    private boolean mComplete;

    public Week(int weekNumber) {
        mMatches = new ArrayList<Match>();
        mWeekNumber = weekNumber;
    }

    public void addMatch(Match match) {
        mMatches.add(match);
    }

    public ArrayList<Match> getMatches() {
        return mMatches;
    }

    public void simulate(boolean useHomeFieldAdvantage) {
        for (Match match : mMatches) {
            if (!match.getComplete()) {
                match.simulate(useHomeFieldAdvantage);
            }
        }
    }

    public void simulateTestMatches(boolean useHomeFieldAdvantage) {
        for (Match match : mMatches) {
            match.simulateTestMatch(useHomeFieldAdvantage);
        }
    }

    public void matchUpdated() {
        mNumberMatchesUpdated++;
    }

    public int getNumberMatchesUpdated() {
        return mNumberMatchesUpdated;
    }
}
