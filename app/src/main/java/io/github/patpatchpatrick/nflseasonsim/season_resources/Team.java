package io.github.patpatchpatrick.nflseasonsim.season_resources;

public class Team {

    private String mName;
    private int mElo;
    private int mOffRating;
    private int mDefRating;
    private int mCurrentWins;
    private int mCurrentLosses;
    private int mCurrentDraws;

    public Team(String name, int elo, int offRating, int defRating){
        mName = name;
        mElo = elo;
        mOffRating =  offRating;
        mDefRating = defRating;
        mCurrentWins = 0;
        mCurrentLosses = 0;
        mCurrentDraws =  0;
    }
}
