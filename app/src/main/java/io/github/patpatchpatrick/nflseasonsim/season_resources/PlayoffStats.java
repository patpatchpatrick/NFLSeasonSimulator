package io.github.patpatchpatrick.nflseasonsim.season_resources;

public class PlayoffStats {

    //Playoff stats object used for playoff predictions

    private int mSuperBowls;
    private int mConferenceChampionships;
    private int mDivisionTitles;
    private int mPlayoffAppearances;

    public PlayoffStats(){
    }

    public void winSuperBowl(){
        mSuperBowls++;
    }

    public void winConference(){
        mConferenceChampionships++;
    }

    public void winDivision(){
        mDivisionTitles++;
    }

    public void makePlayoffs(){
        mPlayoffAppearances++;
    }

}
