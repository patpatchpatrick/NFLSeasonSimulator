package io.github.patpatchpatrick.nflseasonsim;

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;

public class ScoresRecyclerViewAdapter extends RecyclerView.Adapter<ScoresRecyclerViewAdapter.ViewHolder> {

    //Recycylerview adapter for matches/scores

    @Inject
    SimulatorModel mModel;

    @Inject
    Context mContext;

    Cursor dataCursor;

    public ScoresRecyclerViewAdapter() {

        //Inject with Dagger Activity Component to get access to model data
        MainActivity.getActivityComponent().inject(this);

    }

    @NonNull
    @Override
    public ScoresRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View scoreView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_scores_recyclerview_item, parent, false);
        return new ScoresRecyclerViewAdapter.ViewHolder(scoreView);
    }

    @Override
    public void onBindViewHolder(@NonNull ScoresRecyclerViewAdapter.ViewHolder holder, int position) {

        //Bind the match data to to the view

        String teamOne;
        String teamTwo;
        int scoreOne;
        int scoreTwo;
        int teamOneWonInt;
        boolean teamOneWon;

        dataCursor.moveToPosition(position);

        teamOne = dataCursor.getString(dataCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_ONE));
        teamTwo = dataCursor.getString(dataCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_TWO));
        scoreOne = dataCursor.getInt(dataCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_ONE_SCORE));
        scoreTwo = dataCursor.getInt(dataCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_TWO_SCORE));
        teamOneWonInt = dataCursor.getInt(dataCursor.getColumnIndexOrThrow(MatchEntry.COLUMN_MATCH_TEAM_ONE_WON));

        //Set up boolean for if team one won the match
        if (teamOneWonInt == MatchEntry.MATCH_TEAM_ONE_WON_YES) {
            teamOneWon = true;
        } else {
            teamOneWon = false;
        }

        String scoreOneString = Integer.toString(scoreOne);
        String scoreTwoString = Integer.toString(scoreTwo);

        //Set up textviews and imageviews for score listview
        holder.teamOneName.setText(teamOne);
        holder.teamOneScore.setText(scoreOneString);
        holder.teamTwoName.setText(teamTwo);
        holder.teamTwoScore.setText(scoreTwoString);
        holder.teamOneLogo.setImageResource(mModel.getLogo(teamOne));
        holder.teamTwoLogo.setImageResource(mModel.getLogo(teamTwo));

        //Bold the textviews for the the team that won the match
        if (teamOneWon) {
            holder.teamOneName.setTypeface(Typeface.DEFAULT_BOLD);
            holder.teamOneScore.setTypeface(Typeface.DEFAULT_BOLD);
            holder.teamTwoName.setTypeface(Typeface.DEFAULT);
            holder.teamTwoScore.setTypeface(Typeface.DEFAULT);
        } else {
            holder.teamOneName.setTypeface(Typeface.DEFAULT);
            holder.teamOneScore.setTypeface(Typeface.DEFAULT);
            holder.teamTwoName.setTypeface(Typeface.DEFAULT_BOLD);
            holder.teamTwoScore.setTypeface(Typeface.DEFAULT_BOLD);
            if (scoreOne == 0 && scoreTwo == 0) {
                //If neither game has been played, make all textviews not bold
                holder.teamTwoName.setTypeface(Typeface.DEFAULT);
                holder.teamTwoScore.setTypeface(Typeface.DEFAULT);
            }
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

        this.notifyDataSetChanged();


    }


    class ViewHolder extends RecyclerView.ViewHolder {

        public TextView teamOneName;
        public TextView teamOneScore;
        public ImageView teamOneLogo;
        public TextView teamTwoName;
        public TextView teamTwoScore;
        public ImageView teamTwoLogo;


        public ViewHolder(View view) {
            super(view);

            teamOneName = (TextView) view.findViewById(R.id.score_recycler_team_one_name);
            teamOneScore = (TextView) view.findViewById(R.id.score_recycler_team_one_score);
            teamOneLogo = (ImageView) view.findViewById(R.id.score_recycler_team_one_logo);
            teamTwoName = (TextView) view.findViewById(R.id.score_recycler_team_two_name);
            teamTwoScore = (TextView) view.findViewById(R.id.score_recycler_team_two_score);
            teamTwoLogo = (ImageView) view.findViewById(R.id.score_recycler_team_two_logo);


        }
    }
}
