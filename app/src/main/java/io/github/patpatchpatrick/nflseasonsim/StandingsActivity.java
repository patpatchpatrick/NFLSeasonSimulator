package io.github.patpatchpatrick.nflseasonsim;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.data.SimulatorModel;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.ScoreView;
import io.github.patpatchpatrick.nflseasonsim.presenter.SimulatorPresenter;

public class StandingsActivity extends AppCompatActivity implements ScoreView {

    @Inject
    SimulatorPresenter mPresenter;

    private RecyclerView mStandingsRecyclerView;
    private SeasonStandingsRecyclerViewAdapter mSeasonStandingsRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializeTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_standings);
        getWindow().setBackgroundDrawable(null);
        getSupportActionBar().hide();

        //Inject with dagger
        HomeScreen.getActivityComponent().inject(this);

        //Add as standings activity as presenter scoreview to receive display standings callbacks
        mPresenter.addScoreView(this);

        // Set up the standings recyclerview
        mStandingsRecyclerView = (RecyclerView) findViewById(R.id.season_standings_recyclerview);
        mStandingsRecyclerView.setHasFixedSize(true);
        mStandingsRecyclerView.setLayoutManager(new LinearLayoutManager(StandingsActivity.this));
        mSeasonStandingsRecyclerViewAdapter = new SeasonStandingsRecyclerViewAdapter();
        mStandingsRecyclerView.setAdapter(mSeasonStandingsRecyclerViewAdapter);

        //Load and query the current season data
        mPresenter.loadCurrentSeasonMatches();
        mPresenter.queryCurrentSeasonStandings();


    }

    private void initializeTheme() {
        //Set the initial theme of the app based on shared prefs theme
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String appTheme = sharedPrefs.getString(getString(R.string.settings_theme_key), getString(R.string.settings_theme_value_default));
        setTheme(getTheme(appTheme));
    }

    private int getTheme(String themeValue) {
        //Return the actual theme style that corresponds with the theme sharedPrefs String value
        if (themeValue.equals(getString(R.string.settings_theme_value_default))) {
            return R.style.DarkAppTheme;
        } else if (themeValue.equals(getString(R.string.settings_theme_value_grey))) {
            return R.style.GreyAppTheme;

        } else if (themeValue.equals(getString(R.string.settings_theme_value_purple))) {
            return R.style.PurpleAppTheme;

        } else if (themeValue.equals(getString(R.string.settings_theme_value_blue))) {
            return R.style.AppTheme;
        } else {
            return R.style.DarkAppTheme;
        }
    }

    @Override
    public void onDisplayScores(int weekNumber, Cursor cursor, String scoresWeekNumberHeader, int queriedFrom) {

    }

    @Override
    public void onDisplayStandings(int standingsType, Cursor cursor, int queriedFrom) {

        //Swap cursor into the standings recyclerview adapter  to display standings
        if (queriedFrom == SimulatorModel.QUERY_FROM_SEASON_STANDINGS_ACTIVITY) {
            mSeasonStandingsRecyclerViewAdapter.swapCursor(standingsType, cursor);
        }
    }
}
