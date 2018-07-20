package com.anton111111.player;

import android.content.Context;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 13.12.16.
 */
public abstract class VideoPlayer {

    protected Surface surface;
    protected View view;
    protected ViewGroup anchorView;
    protected MediaPlayerControl player;
    protected EventListener eventListener;
    protected boolean isLoading = false;

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isLoaded() {
        return !isLoading;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public VideoPlayer(Context context, View view) {
        this.view = view;
        player = createPlayer(context);
    }

    public VideoPlayer(Context context, Surface surface) {
        this.surface = surface;
        player = createPlayer(context);
    }

    public VideoPlayer(Context context) {
        this.view = createView(context);
        player = createPlayer(context);
    }

    public void addToView(ViewGroup viewGroup, int index, ViewGroup.LayoutParams params) {
        anchorView = viewGroup;
        viewGroup.addView(this.view, index, params);
    }

    public void removeFromView() {
        if (anchorView != null) {
            anchorView.removeView(this.view);
        }
    }

    abstract protected MediaPlayerControl createPlayer(Context context);

    abstract protected View createView(Context context);

    public MediaPlayerControl getPlayer() {
        return player;
    }

    public View getView() {
        return view;
    }

    abstract public void setUri(String uri);

    abstract public void changeQualityUri(String uri);

    abstract public int getBufferPercentage();

    public void onPause() {

    }

    public void onStop() {

    }

    public void onResume() {

    }

    public void onStart() {

    }

    public interface EventListener {
        void onVideoPrepared();

        void onVideoStartBuffering();

        void onVideoEndBuffering();

        void onVideoEnded();

        void onVideoLoadingError(VideoPlayerException e);
    }

    public static class VideoPlayerException extends Exception {
        public VideoPlayerException(Throwable cause) {
            super(cause);
        }

        public VideoPlayerException(String message) {
            super(message);
        }
    }
}
