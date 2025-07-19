package com.tungsten.fcllibrary.component.ui;

import android.content.Context;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fcllibrary.component.FCLActivity;

public abstract class FCLBaseUI implements FCLUILifecycleCallbacks {

    private static Runnable defaultBackEvent;

    private final Context context;
    private final FCLActivity activity;

    private View contentView;

    public FCLBaseUI(Context context) {
        this.context = context;
        this.activity = (FCLActivity) context;
    }

    public static void setDefaultBackEvent(Runnable defaultBackEvent) {
        FCLBaseUI.defaultBackEvent = defaultBackEvent;
    }

    public Context getContext() {
        return context;
    }

    public FCLActivity getActivity() {
        return activity;
    }

    public void setContentView(@LayoutRes int id, OnInflateFinishedListener listener) {
        new AsyncLayoutInflater(context).inflate(id, null, (view, resid, parent) -> {
            contentView = view;
            listener.onFinish();
        });
    }

    public View getContentView() {
        return contentView;
    }

    @NonNull
    public final <T extends View> T findViewById(int id) {
        return contentView.findViewById(id);
    }

    public abstract boolean isShowing();

    public abstract Task<?> refresh(Object... param);

    @Override
    public void onCreate() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onBackPressed() {
        if (defaultBackEvent != null && isShowing()) {
            Schedulers.androidUIThread().execute(defaultBackEvent);
        }
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onDestroy() {

    }
}
