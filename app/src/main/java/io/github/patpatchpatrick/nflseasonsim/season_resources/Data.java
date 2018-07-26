package io.github.patpatchpatrick.nflseasonsim.season_resources;

public interface Data {

    //Interface for callbacks necessary to update database
    //This interface is used by the presenter
    //Objects created by the presenter will use this interface to provide callbacks to the presenter
    //when the presenter needs to update the database
    //The presenter will then let the model know to update the database

    void insertTeamCallback(Team team);
    void insertMatchCallback(Match match);
    void updateMatchCallback(Match match);
    void updateTeamCallback(Team team);


}
