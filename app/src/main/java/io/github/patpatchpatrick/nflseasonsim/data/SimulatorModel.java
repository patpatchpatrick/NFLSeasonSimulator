package io.github.patpatchpatrick.nflseasonsim.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import io.github.patpatchpatrick.nflseasonsim.DaggerApplication;
import io.github.patpatchpatrick.nflseasonsim.mvp_utils.SimulatorMvpContract;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Match;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Schedule;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Team;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.TeamEntry;
import io.github.patpatchpatrick.nflseasonsim.data.SeasonSimContract.MatchEntry;
import io.github.patpatchpatrick.nflseasonsim.season_resources.Week;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class SimulatorModel implements SimulatorMvpContract.SimulatorModel {
    //Simulator model class used to manage communication with database

    private final SimulatorMvpContract.SimulatorPresenter mPresenter;

    @Inject
    ContentResolver contentResolver;

    private CompositeDisposable mCompositeDisposable;

    public SimulatorModel(SimulatorMvpContract.SimulatorPresenter presenter) {
        mPresenter = presenter;

        //Inject team with dagger to get contentResolver
        DaggerApplication.getAppComponent().inject(this);

        //Create new composite disposable to manage disposables from RxJava subscriptions
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        mCompositeDisposable = compositeDisposable;
    }


    @Override
    public void insertMatch(final Match match) {

        //Insert a match into the database

        Observable<Uri> insertMatchObservable = Observable.fromCallable(new Callable<Uri>() {
            @Override
            public Uri call() throws Exception {
                ContentValues values = new ContentValues();
                values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE, match.getTeam1().getName());
                values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO, match.getTeam2().getName());
                values.put(MatchEntry.COLUMN_MATCH_WEEK, match.getWeek());
                Uri uri = contentResolver.insert(MatchEntry.CONTENT_URI, values);
                return uri;
            }
        });

        insertMatchObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Uri>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Uri uri) {
                match.setUri(uri);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });



    }

    @Override
    public void insertTeam(final Team team) {

        //Insert a team into the database

        Observable<Uri> insertMatchObservable = Observable.fromCallable(new Callable<Uri>() {
            @Override
            public Uri call() throws Exception {
                String name = team.getName();
                double elo = team.getELO();
                double offRating = team.getOffRating();
                double defRating = team.getDefRating();
                int currentWins = team.getWins();
                int currentLosses = team.getLosses();
                int currentDraws = team.getDraws();
                int division = team.getDivision();

                ContentValues values = new ContentValues();
                values.put(TeamEntry.COLUMN_TEAM_NAME, name);
                values.put(TeamEntry.COLUMN_TEAM_ELO, elo);
                values.put(TeamEntry.COLUMN_TEAM_OFF_RATING, offRating);
                values.put(TeamEntry.COLUMN_TEAM_DEF_RATING, defRating);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_WINS, currentWins);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_LOSSES, currentLosses);
                values.put(TeamEntry.COLUMN_TEAM_CURRENT_DRAWS, currentDraws);
                values.put(TeamEntry.COLUMN_TEAM_DIVISION, division);

                //Insert values into database
                Uri uri = contentResolver.insert(TeamEntry.CONTENT_URI, values);
                return uri;
            }
        });

        insertMatchObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Uri>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Uri uri) {
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });


    }

    @Override
    public void updateMatch(final Match match) {

        //Update a match in the database

        Observable<Integer> updateMatchObservable = Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                //Update match database scores and match complete values
                ContentValues values = new ContentValues();
                values.put(MatchEntry.COLUMN_MATCH_TEAM_ONE_SCORE, match.getTeam1Score());
                values.put(MatchEntry.COLUMN_MATCH_TEAM_TWO_SCORE, match.getTeam2Score());
                values.put(MatchEntry.COLUMN_MATCH_COMPLETE, MatchEntry.MATCH_COMPLETE_YES);

                int rowsUpdated = contentResolver.update(match.getUri(), values, null, null);
                return rowsUpdated;
            }
        });

        updateMatchObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                mCompositeDisposable.add(d);
            }

            @Override
            public void onNext(Integer rowsUpdated) {
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });



    }

    @Override
    public void destroyModel() {

        //This method is called when the main activity is destroyed.  It will dispose of all disposables
        //within the composite disposable.

        mCompositeDisposable.dispose();
    }


}
