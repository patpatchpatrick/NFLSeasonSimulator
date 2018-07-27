package io.github.patpatchpatrick.nflseasonsim;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract;
import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;

public class MainActivity extends AppCompatActivity implements SimulatorMvpContract.SimulatorView {

    Button mSimulateSeason;
    TextView mTextView;
    private SimulatorPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.test_dagger_text_view);
        mSimulateSeason =  (Button) findViewById(R.id.simulate_season_button);

        //TODO inject presenter instead of instantiating it
        SimulatorPresenter presenter = new SimulatorPresenter(this);
        mPresenter = presenter;

        mSimulateSeason.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO initialize the season immediately (add teams and matches to db) immediately upon loading and don't do it again.  Store a seasonInitialized boolean in shared prefs
                mPresenter.initializeSeason();
                mTextView.setText(getString(R.string.loading));
            }
        });





    }

    @Override
    public void onSimulateWeekButtonClicked() {

    }

    @Override
    public void onSimulateSeasonButtonClicked() {

    }

    @Override
    public void onDisplayStandings(String standings) {
        mTextView.setText(standings);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.destroyPresenter();
    }
}
