package io.github.patpatchpatrick.nflseasonsim;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class EloValuesActivity extends AppCompatActivity {
    //Activity to edit team ELO values

    @Inject
    SimulatorPresenter mSimulatorPresenter;

    @Inject
    SharedPreferences mSharedPreferences;

    private RecyclerView mRecyclerView;
    private Button mLastSeasonEloButton;
    private Button mFutureEloButton;
    private Button mCurrentEloButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.DarkAppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elo_values);

        //Inject with Dagger Activity Component to get access to presenter
        MainActivity.getActivityComponent().inject(this);

        // Find recyclerView for list of team names and elos and set linearLayoutManager and recyclerAdapter on recyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_elo);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(EloValuesActivity.this));
        final EloRecyclerViewAdapter eloRecyclerAdapter = new EloRecyclerViewAdapter();
        mRecyclerView.setAdapter(eloRecyclerAdapter);

        mLastSeasonEloButton = (Button) findViewById(R.id.use_default_button);
        mLastSeasonEloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSimulatorPresenter.resetTeamElos();
                eloRecyclerAdapter.notifyDataSetChanged();
                setUseDefaultElosPref();
            }
        });

        mFutureEloButton = (Button) findViewById(R.id.future_elos_button);
        mFutureEloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSimulatorPresenter.resetTeamFutureElos();
                eloRecyclerAdapter.notifyDataSetChanged();
                setUseFutureElosPref();

            }
        });

        mCurrentEloButton = (Button) findViewById(R.id.current_elos_button);
        mCurrentEloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSimulatorPresenter.setTeamUserElos();
                setUseUserElosPref();
            }
        });


    }

    private void setUseFutureElosPref(){
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_elo_type_key), getResources().getInteger(R.integer.settings_elo_type_future));
        prefs.commit();
    }

    private void setUseDefaultElosPref(){
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_elo_type_key), getResources().getInteger(R.integer.settings_elo_type_default));
        prefs.commit();
    }

    private void setUseUserElosPref(){
        SharedPreferences.Editor prefs = mSharedPreferences.edit();
        prefs.putInt(getString(R.string.settings_elo_type_key), getResources().getInteger(R.integer.settings_elo_type_user));
        prefs.commit();
    }



}
