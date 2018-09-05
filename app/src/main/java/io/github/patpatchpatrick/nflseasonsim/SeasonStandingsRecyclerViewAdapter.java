package io.github.patpatchpatrick.nflseasonsim;

import android.content.Context;
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

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;

public class SeasonStandingsRecyclerViewAdapter extends RecyclerView.Adapter<SeasonStandingsRecyclerViewAdapter.ViewHolder> {

    @Inject
    SimulatorModel mModel;

    @Inject
    Context mContext;


    Cursor dataCursor;
    int mStandingsType;

    public SeasonStandingsRecyclerViewAdapter() {

        //Inject with Dagger Activity Component to get access to model data
        HomeScreen.getActivityComponent().inject(this);

    }

    @NonNull
    @Override
    public SeasonStandingsRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View standingsView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_season_standings_recyclerview_item, parent, false);
        return new SeasonStandingsRecyclerViewAdapter.ViewHolder(standingsView);
    }

    @Override
    public void onBindViewHolder(@NonNull SeasonStandingsRecyclerViewAdapter.ViewHolder holder, int position) {

        dataCursor.moveToPosition(position);

        if (mStandingsType == MainActivity.STANDINGS_TYPE_REGULAR_SEASON) {
            //Display regular season standings

            //For every 4th team, the header should show the division
            if (position % 4 == 0) {
                int teamDivision = dataCursor.getInt(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_DIVISION));
                holder.standingsHeader.setVisibility(View.VISIBLE);
                if (position == 0) {
                    //For the first division, don't include a line break in header
                    holder.standingsHeader.setText("" + TeamEntry.getDivisionString(teamDivision));
                } else {
                    holder.standingsHeader.setText("\n" + TeamEntry.getDivisionString(teamDivision));
                }
            } else {
                holder.standingsHeader.setVisibility(View.GONE);
            }

            //Show the team details (record and playoff seed)
            String standingsDetails = "";
            String teamName = dataCursor.getString(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_NAME));
            String teamShortName = dataCursor.getString(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_SHORT_NAME));
            Integer teamWinsInt = dataCursor.getInt(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_CURRENT_WINS));
            Integer teamLossesInt = dataCursor.getInt(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_CURRENT_LOSSES));
            String teamWins = Integer.toString(teamWinsInt);
            String teamLosses = Integer.toString(teamLossesInt);
            int playoffSeed = dataCursor.getInt(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_PLAYOFF_ELIGIBILE));
            String playoffSeedString = Integer.toString(playoffSeed);

            standingsDetails += teamShortName + "  " + teamWins + " - " + teamLosses;

            //Check if it is the first week of the season by determining if a team has losses or wins
            Boolean firstWeekOfSeason = true;
            if (teamWinsInt != 0 || teamLossesInt != 0) {
                firstWeekOfSeason = false;
            }

            if (!firstWeekOfSeason) {
                //Bold the strings of playoff teams and add their playoff seed to standings
                if (playoffSeed > 0) {
                    standingsDetails += " (" + playoffSeedString + ")";
                    holder.standingsDetails.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    holder.standingsDetails.setTypeface(Typeface.DEFAULT);
                }
            }

            holder.standingsDetails.setText(standingsDetails);
            //Set font by default (currently font won't auto-set in layout so need to programmatically set it)
            holder.standingsDetails.setTypeface(ResourcesCompat.getFont(mContext, R.font.montserrat));

            holder.standingsTeamLogo.setImageResource(mModel.getLogo(teamName));

        }

        if (mStandingsType == MainActivity.STANDINGS_TYPE_PLAYOFFS) {

            //Display playoff standings

            int remainingPlayoffTeams = dataCursor.getCount();

            //Display the correct standings headers depending on remaining playoff teams
            if (remainingPlayoffTeams == 12) {
                if (position == 0) {
                    holder.standingsHeader.setVisibility(View.VISIBLE);
                    holder.standingsHeader.setText("AFC Playoff Standings");
                } else if (position == 6) {
                    holder.standingsHeader.setVisibility(View.VISIBLE);
                    holder.standingsHeader.setText("NFC Playoff Standings");
                } else {
                    holder.standingsHeader.setVisibility(View.GONE);
                }
            } else if (remainingPlayoffTeams == 8) {
                if (position == 0) {
                    holder.standingsHeader.setVisibility(View.VISIBLE);
                    holder.standingsHeader.setText("AFC Playoff Standings");
                } else if (position == 4) {
                    holder.standingsHeader.setVisibility(View.VISIBLE);
                    holder.standingsHeader.setText("NFC Playoff Standings");
                } else {
                    holder.standingsHeader.setVisibility(View.GONE);
                }
            } else if (remainingPlayoffTeams == 4) {
                if (position == 0) {
                    holder.standingsHeader.setVisibility(View.VISIBLE);
                    holder.standingsHeader.setText("AFC Playoff Standings");
                } else if (position == 2) {
                    holder.standingsHeader.setVisibility(View.VISIBLE);
                    holder.standingsHeader.setText("NFC Playoff Standingsr");
                } else {
                    holder.standingsHeader.setVisibility(View.GONE);
                }
            } else {
                holder.standingsHeader.setVisibility(View.GONE);
            }

            //Show the team details (name and playoff seed)
            String standingsDetails = "";
            String teamName = dataCursor.getString(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_NAME));
            String teamShortName = dataCursor.getString(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_SHORT_NAME));
            int playoffSeed = dataCursor.getInt(dataCursor.getColumnIndexOrThrow(TeamEntry.COLUMN_TEAM_PLAYOFF_ELIGIBILE));
            String playoffSeedString = Integer.toString(playoffSeed);
            standingsDetails += teamShortName;
            standingsDetails += " (" + playoffSeedString + ")";

            holder.standingsDetails.setTypeface(Typeface.DEFAULT);
            holder.standingsDetails.setText(standingsDetails);

            holder.standingsTeamLogo.setImageResource(mModel.getLogo(teamName));


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

    public void swapCursor(int standingsType, Cursor cursor) {

        mStandingsType = standingsType;

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

        public TextView standingsHeader;
        public TextView standingsDetails;
        public ImageView standingsTeamLogo;


        public ViewHolder(View view) {
            super(view);

            standingsHeader = view.findViewById(R.id.season_standings_recycler_header);
            standingsDetails = view.findViewById(R.id.season_standings_details_text_view);
            standingsTeamLogo = view.findViewById(R.id.season_standings_recycler_team_logo);

        }
    }

}
