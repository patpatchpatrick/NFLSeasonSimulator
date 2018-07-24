package io.github.patpatchpatrick.nflseasonsim.season_resources;

import java.util.ArrayList;

public class Schedule {

    private ArrayList<Week> mWeeks;

    public Schedule(){
        mWeeks = new ArrayList<Week>();
    }

    public void addWeek(Week week){
        mWeeks.add(week);
    }
}
