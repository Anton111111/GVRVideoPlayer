package com.anton111111.gvrvideoplayer;


import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.anton111111.player.ExoPlayer2Impl;
import com.anton111111.player.VideoPlayer;
import com.anton111111.vr.Quaternion;
import com.anton111111.vr.VideoFormatsSettings;
import com.anton111111.vr.program.ProgramHelper;
import com.anton111111.vr.renderer.VideoControllerRenderer;
import com.anton111111.vr.renderer.VideoRenderer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 27.02.18.
 */
public class VRPlayerActivity extends GvrActivity
        implements GvrView.StereoRenderer,
        VideoPlayer.EventListener,
        SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "GVRVIDEOPLAYER";

    public static final String INTENT_EXTRA_URL_KEY = "INTENT_EXTRA_URL_KEY";
    public static final String INTENT_EXTRA_VIDEO_FORMAT_KEY = "INTENT_EXTRA_VIDEO_FORMAT_KEY";
    public static final String INTENT_EXTRA_STEREO_MODE_ENABLED = "INTENT_EXTRA_STEREO_MODE_ENABLED";


    private static final long VIBRATE_MILLISECONDS = 100;
    public static final float Z_NEAR = 0.1f;
    public static final float Z_FAR = 100.0f;

    private float[] viewMatrix = new float[16];
    private float eyeZ = 0.01f; //default value 0.01f
    private VideoRenderer videoRenderer;
    private VideoControllerRenderer videoControllerRenderer;
    private String videoFormat = VideoFormatsSettings.VIDEO_FORMAT_3D_180_SIDE_BY_SIDE;
    private String url;
    private ExoPlayer2Impl exoPlayerImpl;
    Handler handler = new Handler();
    private boolean isStereoModeEnabled = true;
    private float[] headTransformQuaternion = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    private float eulerPitchCorrection = 0.0f;
    private boolean isNeedPitchCorrection = false;
    private Vibrator vibrator;
    private AudioManager audioManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vr_player_activity);

        url = getIntent().getStringExtra(INTENT_EXTRA_URL_KEY);
        videoFormat = getIntent().getStringExtra(INTENT_EXTRA_VIDEO_FORMAT_KEY);
        boolean isStereoModeEnabled = getIntent().getBooleanExtra(INTENT_EXTRA_STEREO_MODE_ENABLED, true);

        if (url == null && url.length() <= 0) {
            Log.e(TAG, "Video is empty", new VRPlayerActivityException("Video is null"));
            finish();
            return;
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        GvrView gvrView = findViewById(R.id.vr_player_activity_gvr_view);
        gvrView.setStereoModeEnabled(isStereoModeEnabled);
        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);

        // Enable Cardboard-trigger feedback with Daydream headsets. This is a simple way of supporting
        // Daydream controller input for basic interactions using the existing Cardboard trigger API.
        gvrView.enableCardboardTriggerEmulation();

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }
        setGvrView(gvrView);
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        headTransform.getQuaternion(headTransformQuaternion, 0);

        if (isNeedPitchCorrection) {
            float[] eulerAngles = new float[3];
            Quaternion.toEulerAngle(headTransformQuaternion, eulerAngles);
            eulerPitchCorrection = eulerAngles[0];
            isNeedPitchCorrection = false;
        }

        if (eulerPitchCorrection != 0) {
            float[] eulerAngles = new float[3];
            Quaternion.toEulerAngle(headTransformQuaternion, eulerAngles);
            eulerAngles[0] = eulerAngles[0] - eulerPitchCorrection;
            Quaternion.fromEulerAngles(headTransformQuaternion, eulerAngles[0], eulerAngles[1], eulerAngles[2]);
        }

        if (videoRenderer != null) {
            videoRenderer.onNewFrame(headTransformQuaternion);
        }
        if (videoControllerRenderer != null) {
            videoControllerRenderer.setHeadTransformQuaternion(headTransformQuaternion);
        }
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        videoRenderer.render(eye);
        videoControllerRenderer.render(eye);
    }

    @Override
    public void onCardboardTrigger() {
        if (videoControllerRenderer != null) {
            videoControllerRenderer.onTouch();
            vibrator.vibrate(VIBRATE_MILLISECONDS);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        //fix volume button policies
        if (audioManager != null &&
                (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ||
                        event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)) {

            if (event.getAction() == KeyEvent.ACTION_UP) {
                return true;
            }

            int streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            streamVolume = (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) ? ++streamVolume : --streamVolume;
            if (streamVolume >= 0 &&
                    streamVolume <= audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolume, 0);
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (exoPlayerImpl != null && exoPlayerImpl.getPlayer() != null) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    if (exoPlayerImpl.getPlayer().isPlaying()) {
                        exoPlayerImpl.getPlayer().pause();
                    } else {
                        exoPlayerImpl.getPlayer().start();
                    }
                    vibrator.vibrate(VIBRATE_MILLISECONDS);
                    return true;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY && !exoPlayerImpl.getPlayer().isPlaying()) {
                    exoPlayerImpl.getPlayer().start();
                    vibrator.vibrate(VIBRATE_MILLISECONDS);
                    return true;
                }

                if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE && exoPlayerImpl.getPlayer().isPlaying()) {
                    exoPlayerImpl.getPlayer().pause();
                    vibrator.vibrate(VIBRATE_MILLISECONDS);
                    return true;
                }
            }


            if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A ||
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                onCardboardTrigger();
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B ||
                    event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (videoControllerRenderer != null &&
                        videoControllerRenderer.isShow()) {
                    videoControllerRenderer.hide();
                    return true;
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        videoControllerRenderer.onSurfaceChanged(width, height);
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);
        Matrix.setLookAtM(viewMatrix, 0,
                0.0f, 0.0f, eyeZ,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f);

        videoRenderer = new VideoRenderer(this, videoFormat);

        exoPlayerImpl = new ExoPlayer2Impl(this, TAG, new Surface(videoRenderer.getVideoTextureSurface()));
        exoPlayerImpl.setEventListener(this);
        if (url != null && !url.isEmpty()) {
            Log.e(TAG, "Start play: " + url);
            exoPlayerImpl.setUri(url);
        } else {
            Log.e(TAG, "Video url is not inited!",
                    new VRPlayerActivityException("Video url is not inited!"));
            finish();
            return;
        }

        videoControllerRenderer = new VideoControllerRenderer(this, handler);
        videoControllerRenderer.setVideoControllerListener(new VideoControllerRenderer.VideoControllerListener() {
            @Override
            public void onRecenterView() {
                getGvrView().recenterHeadTracker();
                isNeedPitchCorrection = true;
            }
        });
        videoControllerRenderer.setViewMatrix(viewMatrix);
        videoRenderer.setViewMatrix(viewMatrix);
    }

    @Override
    public void onRendererShutdown() {
        if (videoRenderer != null) {
            videoRenderer.onRendererShutdown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayerImpl != null) {
            exoPlayerImpl.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayerImpl != null) {
            exoPlayerImpl.onResume();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (exoPlayerImpl != null) {
            exoPlayerImpl.onStart();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (exoPlayerImpl != null) {
            exoPlayerImpl.onStop();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (videoRenderer != null) {
            videoRenderer.onDestroy();
        }

        if (videoControllerRenderer != null) {
            videoControllerRenderer.onDestroy();
        }

        ProgramHelper.getInstance().clean();
    }

    @Override
    public void onVideoPrepared() {
        exoPlayerImpl.getPlayer().start();
        videoControllerRenderer.setMediaPlayer(exoPlayerImpl);
    }

    @Override
    public void onVideoStartBuffering() {

    }

    @Override
    public void onVideoEndBuffering() {

    }

    @Override
    public void onVideoEnded() {

    }

    @Override
    public void onVideoLoadingError(VideoPlayer.VideoPlayerException e) {
        Log.e(TAG, e.getMessage(),
                new VRPlayerActivityException(e));
        finish();
        return;
    }


    public class VRPlayerActivityException extends Exception {
        public VRPlayerActivityException(String message) {
            super(message);
        }

        public VRPlayerActivityException(Throwable cause) {
            super(cause);
        }
    }
}
