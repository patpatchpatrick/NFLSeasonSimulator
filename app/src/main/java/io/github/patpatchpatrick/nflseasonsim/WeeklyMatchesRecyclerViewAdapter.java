package io.github.patpatchpatrick.nflseasonsim;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class WeeklyMatchesRecyclerViewAdapter extends RecyclerView.Adapter<WeeklyMatchesRecyclerViewAdapter.ViewHolder> {

    Cursor dataCursor;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View weeklyMatchesView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_weekly_matches_recyclerview_item, parent, false);
        return new WeeklyMatchesRecyclerViewAdapter.ViewHolder(weeklyMatchesView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


    }

    @Override
    public int getItemCount() {
        return 0;
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
        public TextView teamTwoName;
        public ImageView teamOneLogo;
        public ImageView teamTwoLogo;


        public ViewHolder(View view) {
            super(view);

            teamOneName = (TextView) view.findViewById(R.id.weekly_matches_recycler_team_one_name);
            teamOneLogo = (ImageView) view.findViewById(R.id.weekly_matches_recycler_team_one_logo);
            teamTwoName = (TextView) view.findViewById(R.id.weekly_matches_recycler_team_two_name);
            teamTwoLogo = (ImageView) view.findViewById(R.id.weekly_matches_recycler_team_two_logo);


        }
    }

}
