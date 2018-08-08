package io.github.patpatchpatrick.nflseasonsim;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;

public class EloRecyclerViewAdapter extends RecyclerView.Adapter<EloRecyclerViewAdapter.ViewHolder> {

    private ArrayList<Team> mTeamArrayList;

    @Inject
    SimulatorModel mModel;


    @NonNull
    @Override
    public EloRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View eloView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_elo_item, parent, false);
        return new EloRecyclerViewAdapter.ViewHolder(eloView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Team currentTeam = mTeamArrayList.get(position);

        String teamName = currentTeam.getName();
        double teamElo = currentTeam.getElo();
        String teamEloString = Double.toString(teamElo);

        holder.teamNameTextView.setText(teamName);
        holder.teamEloEditText.setText(teamEloString);

    }

    public EloRecyclerViewAdapter(){

        //Inject with Dagger Activity Component to get access to model data
        MainActivity.getActivityComponent().inject(this);

        mTeamArrayList = mModel.getTeamArrayList();

    }


    @Override
    public int getItemCount() {
        return (mTeamArrayList == null) ? 0 : mTeamArrayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView teamNameTextView;
        public EditText teamEloEditText;

        public ViewHolder(View view) {
            super(view);

            teamNameTextView = (TextView) view.findViewById(R.id.team_name_textview);
            teamEloEditText = (EditText) view.findViewById(R.id.team_elo_edittext);


        }

        @Override
        public void onClick(View view) {

        }
    }
}
