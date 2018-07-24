package io.github.patpatchpatrick.nflseasonsim.season_resources;

import java.util.ArrayList;

public class Week {

    private ArrayList<Match> mMatches;
    private int mWeekNumber;
    private boolean mComplete;

    public Week(int weekNumber){
        mMatches = new ArrayList<Match>();
        mWeekNumber = weekNumber;
    }

    public void addMatch(Match match){
        mMatches.add(match);
    }

    public void simulate(){
        for (Match match: mMatches) {
            match.simulate();
        }
    }
}