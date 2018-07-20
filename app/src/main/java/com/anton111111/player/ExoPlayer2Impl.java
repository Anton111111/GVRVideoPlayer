package com.anton111111.player;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;
import android.view.View;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 14.12.16.
 */

public class ExoPlayer2Impl extends VideoPlayer implements Player.EventListener {


    private final String userAgent;
    private final Context context;
    private SimpleExoPlayer exoPlayer;
    private boolean isNewSource = false;
    private DefaultDataSourceFactory dataSourceFactory;
    private long playerPositionAfterPause;
    private boolean shouldRestoreAfterPause = false;
    private ExtractorMediaSource videoSource;


    public ExoPlayer2Impl(Context context, String userAgent) {
        super(context);
        this.userAgent = userAgent;
        this.context = context;

    }

    public ExoPlayer2Impl(Context context, String userAgent, Surface surface) {
        super(context, surface);
        this.userAgent = userAgent;
        this.context = context;
    }

    @Override
    protected MediaPlayerControl createPlayer(Context context) {
        DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory adaptiveTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(defaultBandwidthMeter);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);

        exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory,
                trackSelector);
        if (view != null) {
            ((PlayerView) view).setPlayer(exoPlayer);
        } else if (surface != null) {
            exoPlayer.setVideoSurface(surface);
        }
        if (playerPositionAfterPause != C.TIME_UNSET) {
            exoPlayer.seekTo(playerPositionAfterPause);
        }

        exoPlayer.addListener(this);

        return new MediaPlayerControlImpl(exoPlayer);
    }

    @Override
    protected View createView(Context context) {
        PlayerView playerView = new PlayerView(context);
        playerView.setUseController(false);
        return playerView;
    }

    @Override
    public void setUri(String uri) {
        dataSourceFactory = new DefaultDataSourceFactory(context,
                userAgent, null);

        videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(uri));

        // Prepare the player with the source.
        if (exoPlayer != null) {
            exoPlayer.prepare(videoSource);
        }
        isNewSource = true;
    }

    @Override
    public void changeQualityUri(String uri) {
        videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(uri));

        // Prepare the player with the source.
        if (exoPlayer != null) {
            exoPlayer.prepare(videoSource, false, true);
        }
        isNewSource = true;
    }

    @Override
    public int getBufferPercentage() {
        return getPlayer().getBufferPercentage();
    }

    @Override
    public void onResume() {
        if ((Util.SDK_INT <= 23 && shouldRestoreAfterPause)) {
            restorePlayerAfterPause();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23 && shouldRestoreAfterPause) {
            restorePlayerAfterPause();
        }
    }

    @Override
    public void onPause() {
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    public void restorePlayerAfterPause() {
        player = createPlayer(context);
        if (videoSource != null) {
            exoPlayer.prepare(videoSource, false, true);
        }
    }

    public void releasePlayer() {
        shouldRestoreAfterPause = true;
        playerPositionAfterPause = C.TIME_UNSET;
        if (exoPlayer == null) {
            return;
        }
        Timeline timeline = exoPlayer.getCurrentTimeline();
        if (timeline != null) {
            playerPositionAfterPause = exoPlayer.getCurrentPosition();
        }
        exoPlayer.removeListener(this);
        exoPlayer.release();
        exoPlayer = null;
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_BUFFERING) {
            if (eventListener != null) {
                this.isLoading = true;
                eventListener.onVideoStartBuffering();
            }
        }
        if (playbackState == Player.STATE_READY) {
            this.isLoading = false;
            if (isNewSource) {
                isNewSource = false;
                if (eventListener != null) {
                    eventListener.onVideoPrepared();
                }
            }
            if (eventListener != null) {
                eventListener.onVideoEndBuffering();
            }
        }
        if (playbackState == Player.STATE_ENDED) {
            this.isLoading = false;
            if (isNewSource) {
                isNewSource = false;
                if (eventListener != null) {
                    eventListener.onVideoPrepared();
                }
            }
            if (eventListener != null &&
                    exoPlayer.getCurrentPosition() > 0 &&
                    exoPlayer.getDuration() > 0 &&
                    exoPlayer.getCurrentPosition() >= exoPlayer.getDuration()) {
                eventListener.onVideoEnded();
            }
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }


    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (eventListener != null) {
            eventListener.onVideoLoadingError(new VideoPlayerException("Can't play video (" + error.getMessage() + ")"));
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }


    public class MediaPlayerControlImpl implements MediaPlayerControl {

        private final ExoPlayer exoPlayer;

        public MediaPlayerControlImpl(ExoPlayer exoPlayer) {
            this.exoPlayer = exoPlayer;
        }

        @Override
        public void start() {
            if (exoPlayer == null) {
                return;
            }
            exoPlayer.setPlayWhenReady(true);
        }

        @Override
        public void pause() {
            if (exoPlayer == null) {
                return;
            }
            exoPlayer.setPlayWhenReady(false);
        }

        @Override
        public long getDuration() {
            if (exoPlayer == null) {
                return 0;
            }
            return exoPlayer.getDuration();
        }

        @Override
        public long getCurrentPosition() {
            if (exoPlayer == null) {
                return 0;
            }
            return exoPlayer.getCurrentPosition();
        }

        @Override
        public void seekTo(long pos) {
            if (exoPlayer == null) {
                return;
            }
            exoPlayer.seekTo(Math.min(Math.max(0, pos), getDuration()));
        }

        @Override
        public boolean isPlaying() {
            if (exoPlayer == null) {
                return shouldRestoreAfterPause;
            }
            return exoPlayer.getPlayWhenReady();
        }

        @Override
        public boolean isEnded() {
            if (exoPlayer.getCurrentPosition() > 0 &&
                    exoPlayer.getDuration() > 0 &&
                    exoPlayer.getCurrentPosition() >= exoPlayer.getDuration()) {
                return true;
            }
            return false;
        }

        @Override
        public int getBufferPercentage() {
            if (exoPlayer == null) {
                return 0;
            }
            return exoPlayer.getBufferedPercentage();
        }

        @Override
        public boolean canPause() {
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            return true;
        }

        @Override
        public boolean canSeekForward() {
            return true;
        }

        @Override
        public int getAudioSessionId() {
            throw new UnsupportedOperationException();
        }
    }

}
