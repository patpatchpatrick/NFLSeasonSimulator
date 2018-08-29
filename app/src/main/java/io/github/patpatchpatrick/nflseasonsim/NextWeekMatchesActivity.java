package io.github.patpatchpatrick.nflseasonsim;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.mvp_utils.BaseView;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class NextWeekMatchesActivity extends AppCompatActivity implements BaseView{

    @Inject
    SimulatorPresenter mPresenter;
    
    RecyclerView mNextWeekMatchesRecyclerView;
    WeeklyMatchesRecyclerViewAdapter mNextWeekMatchesRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_week_matches);

        //Inject with dagger
        HomeScreen.getActivityComponent().inject(this);

        //Add this activity as a baseview for the presenter to notify
        mPresenter.addBaseView(this);

        // Set up the weekly matches recyclerview
        mNextWeekMatchesRecyclerView = (RecyclerView) findViewById(R.id.next_week_matches_recyclerview);
        mNextWeekMatchesRecyclerView.setHasFixedSize(true);
        mNextWeekMatchesRecyclerView.setLayoutManager(new LinearLayoutManager(NextWeekMatchesActivity.this));
        mNextWeekMatchesRecyclerViewAdapter = new WeeklyMatchesRecyclerViewAdapter();
        mNextWeekMatchesRecyclerView.setAdapter(mNextWeekMatchesRecyclerViewAdapter);


        
    }

    @Override
    public void onSeasonInitialized(int initializedFrom) {

    }

    @Override
    public void onSeasonLoadedFromDb(int requestType) {

    }
}
