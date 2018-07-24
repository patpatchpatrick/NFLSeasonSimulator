package io.github.patpatchpatrick.nflseasonsim.presenter;

import android.content.ContentResolver;

public abstract class BasePresenter<V> {

    public final V view;
    public final ContentResolver contentResolver;

    public BasePresenter(V view, ContentResolver contentResolver){
        this.view = view;
        this.contentResolver = contentResolver;
    }

}
