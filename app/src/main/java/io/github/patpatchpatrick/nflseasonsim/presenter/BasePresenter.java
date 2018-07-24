package io.github.patpatchpatrick.nflseasonsim.presenter;

import android.content.ContentResolver;

public abstract class BasePresenter<V> {

    protected final V view;

    protected BasePresenter(V view){
        this.view = view;
    }

}
