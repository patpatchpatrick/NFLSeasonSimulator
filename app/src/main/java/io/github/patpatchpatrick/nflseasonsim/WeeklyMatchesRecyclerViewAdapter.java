package io.github.patpatchpatrick.nflseasonsim;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.season_resources.ELORatingSystem;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;

public class WeeklyMatchesRecyclerViewAdapter extends RecyclerView.Adapter<WeeklyMatchesRecyclerViewAdapter.ViewHolder> {

    @Inject
    SimulatorModel mModel;

    @Inject
    Context mContext;

    @Inject
    SharedPreferences mSharedPrefs;

    Cursor dataCursor;
    Integer eloType;

    public WeeklyMatchesRecyclerViewAdapter() {

        //Inject with Dagger Activity Component to get access to model data
        HomeScreen.getActivityComponent().inject(this);

        eloType = mSharedPrefs.getInt(mContext.getString(R.string.settings_elo_type_key), mContext.getResources().getInteger(R.integer.settings_elo_type_current_season));

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View weeklyMatchesView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_weekly_matches_recyclerview_item, parent, false);
        return new WeeklyMatchesRecyclerViewAdapter.ViewHolder(weeklyMatchesView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String teamOneString;
        String teamTwoString;

        dataCursor.moveToPosition(position);

        //Set team  one and two name and logo
        Typeface tf = ResourcesCompat.getFont(mContext, R.font.montserrat);
        teamOneString = dataCursor.getString(dataCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_ONE));
        teamTwoString = dataCursor.getString(dataCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_TWO));
        holder.teamOneName.setText(teamOneString);
        holder.teamTwoName.setText(teamTwoString);
        holder.teamOneName.setTypeface(tf);
        holder.teamTwoName.setTypeface(tf);
        holder.teamOneLogo.setImageResource(mModel.getLogo(teamOneString));
        holder.teamTwoLogo.setImageResource(mModel.getLogo(teamTwoString));

        //Set string for ELO odds to win. This string is the short name of the home team (team two)
        //combined with the ELO odds of team two winning
        Team teamOne = mModel.getSimulatorTeam(teamOneString);
        Team teamTwo = mModel.getSimulatorTeam(teamTwoString);
        Double teamOneElo;
        Double teamTwoElo;
        //Set elo value based on user selected elo preference
        if (eloType == mContext.getResources().getInteger(R.integer.settings_elo_type_last_season)) {
            teamOneElo = teamOne.getDefaultElo();
            teamTwoElo = teamTwo.getDefaultElo();
        } else if (eloType == mContext.getResources().getInteger(R.integer.settings_elo_type_current_season)) {
            teamOneElo = teamOne.getFutureElo();
            teamTwoElo = teamTwo.getFutureElo();
        } else if (eloType == mContext.getResources().getInteger(R.integer.settings_elo_type_user)) {
            teamOneElo = teamOne.getUserElo();
            teamTwoElo = teamTwo.getUserElo();
        } else {
            teamOneElo = teamOne.getDefaultElo();
            teamTwoElo = teamTwo.getDefaultElo();
        }
        Double teamOneOddsToWin = ELORatingSystem.probabilityOfTeamOneWinning(teamOneElo, teamTwoElo, true);
        Double teamTwoOddsToWin = 1 - teamOneOddsToWin;
        Double teamTwoOddsToWinPercent = teamTwoOddsToWin * 100;
        String teamTwoShortName = teamTwo.getShortName();
        DecimalFormat df = new DecimalFormat("#.##");
        holder.teamTwoEloOdds.setText(teamTwoShortName + " " + df.format(teamTwoOddsToWinPercent) + "%");
        holder.teamTwoEloOdds.setTypeface(tf);

        //Set the vegas line for the match
        //If team two is favored, the odds will be greater than 0 so show the absolute value of the odds with a - sign
        //If team one is favored, the odds will be less than  0 so show the absolute value of the odds with a + sign
        //If there are no odds, clear the textview
        Double teamTwoVegasOdds = dataCursor.getDouble(dataCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_TWO_ODDS));
        if (teamTwoVegasOdds != MatchEntry.MATCH_NO_ODDS_SET) {
            if (teamTwoVegasOdds >= 0) {
                holder.teamTwoVegasOdds.setText(teamTwoShortName + " -" + Math.abs(teamTwoVegasOdds));
            } else {
                holder.teamTwoVegasOdds.setText(teamTwoShortName + " +" + Math.abs(teamTwoVegasOdds));
            }
        } else {
            holder.teamTwoVegasOdds.setText("");
        }


    }

    @Override
    public int getItemCount() {
        if (dataCursor == null) {
            return 0;
        } else {
            return dataCursor.getCount();
        }
    }

    public void swapCursor(Cursor cursor) {

        if (dataCursor == cursor) {
            return;
        }

        Cursor oldCursor = this.dataCursor;
        this.dataCursor = cursor;
        if (oldCursor != null) {
            oldCursor.close();
        }

        //When new data is swapped in, reload elo type preference
        eloType = mSharedPrefs.getInt(mContext.getString(R.string.settings_elo_type_key), mContext.getResources().getInteger(R.integer.settings_elo_type_current_season));
        this.notifyDataSetChanged();


    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public TextView teamOneName;
        public TextView teamTwoName;
        public TextView teamTwoEloOdds;
        public TextView teamTwoVegasOdds;
        public ImageView teamOneLogo;
        public ImageView teamTwoLogo;


        public ViewHolder(View view) {
            super(view);

            teamOneName = (TextView) view.findViewById(R.id.weekly_matches_recycler_team_one_name);
            teamOneLogo = (ImageView) view.findViewById(R.id.weekly_matches_recycler_team_one_logo);
            teamTwoName = (TextView) view.findViewById(R.id.weekly_matches_recycler_team_two_name);
            teamTwoLogo = (ImageView) view.findViewById(R.id.weekly_matches_recycler_team_two_logo);
            teamTwoEloOdds = (TextView) view.findViewById(R.id.weekly_matches_elo_odds_value);
            teamTwoVegasOdds = (TextView) view.findViewById(R.id.weekly_matches_vegas_line_value);


        }
    }

}
