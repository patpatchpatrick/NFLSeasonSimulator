package io.github.patpatchpatrick.nflseasonsim;

import android.content.ContentResolver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;

public class EloValuesActivity extends AppCompatActivity {
    //Activity to edit team ELO values

    private RecyclerView mRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elo_values);

        // Find recyclerView for list of team names and elos and set linearLayoutManager and recyclerAdapter on recyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_elo);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(EloValuesActivity.this));
        EloRecyclerViewAdapter eloRecyclerAdapter = new EloRecyclerViewAdapter();
        mRecyclerView.setAdapter(eloRecyclerAdapter);


    }
}
