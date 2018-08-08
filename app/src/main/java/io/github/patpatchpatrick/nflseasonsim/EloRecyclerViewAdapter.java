package io.github.patpatchpatrick.nflseasonsim;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

public class EloRecyclerViewAdapter extends RecyclerView.Adapter<EloRecyclerViewAdapter.ViewHolder> {


    @NonNull
    @Override
    public EloRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View eloView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_elo_item, parent, false);
        return new EloRecyclerViewAdapter.ViewHolder(eloView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    public EloRecyclerViewAdapter(){

    }


    @Override
    public int getItemCount() {
        return 0;
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
