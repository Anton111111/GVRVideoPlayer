package com.anton111111.vr.renderer;

import android.content.Context;
import android.opengl.Matrix;
import android.os.Handler;


import com.anton111111.gvrvideoplayer.R;
import com.anton111111.util.StringUtil;
import com.google.vr.sdk.base.Eye;
import com.anton111111.player.VideoPlayer;
import com.anton111111.vr.Quaternion;
import com.anton111111.vr.raypicking.RayPicking;
import com.anton111111.vr.widgets.Cursor;
import com.anton111111.vr.widgets.Icon;
import com.anton111111.vr.widgets.IconsList;
import com.anton111111.vr.widgets.Panel;
import com.anton111111.vr.widgets.SeekBar;

import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 04.12.17.
 */
public class VideoControllerRenderer {

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float DETECTION_AREA_PADDING = 0.05f;
    private static final float PANEL_PADDING = 0.01f;
    private static final float PANEL_X = 0.0f; // X for center of panel
    private static final float PANEL_Y = 0.0f; // Y for center of panel
    private static final float PANEL_Z = -0.8f; // Z for center of panel
    private static final float DEGREE_TO_SHOW_PANEL = 70f; // degree to calculate default offset
    private static final float LAYERS_MARGIN_Z = 0.0001f;
    private static final float SEEK_BAR_MARGIN = 0.01f; //top and bottom margin
    private static final float SEEK_BAR_WIDTH = 0.4f;
    private static final float SEEK_BAR_PROGRESS_HEIGHT = 0.005f;
    private static final float SEEK_BAR_CLICK_AREA_HEIGHT = 0.06f;
    private static final int SHOW_DEFAULT_TIMEOUT = 3000;

    private static final short[] DETECTION_AREA_VERTEX_INDEXES = new short[]{
            0, 1, 2, 0, 2, 3
    };


    public enum Status {
        SHOW,
        HIDE,
        RECENTER_VIEW,
        NEED_RECALCULATE_ROTATE;

        public static EnumSet<Status> none() {
            return EnumSet.noneOf(Status.class);
        }

        public static EnumSet<Status> hide() {
            EnumSet<Status> statuses = EnumSet.noneOf(Status.class);
            statuses.add(HIDE);
            return statuses;
        }

        public static boolean isShow(EnumSet<Status> s) {
            return s.contains(Status.SHOW);
        }
    }

    private final Context context;
    private float[] detectionAreaCoords;
    private boolean isForceShow = false;
    private VideoPlayer videoPlayer;
    private VideoControllerListener videoControllerListener;
    private SeekBar seekBar;
    private Icon playPauseIcon;
    private float[] panelCorrectionQuaternion = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    private float[] panelDefaultQuaternion = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    private EnumSet<Status> status = Status.hide();
    private IconsList iconsList;
    private Panel panel;
    private Cursor cursor;
    private int viewWidth;
    private int viewHeight;
    private Handler handler;
    private float[] viewMatrix;
    private float[] headTransformQuaternion = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    public EnumSet<Status> getStatus() {
        return status;
    }

    public boolean isShow() {
        return Status.isShow(status);
    }

    public void setVideoControllerListener(VideoControllerListener videoControllerListener) {
        this.videoControllerListener = videoControllerListener;
    }

    public void setMediaPlayer(VideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
    }


    public void setViewMatrix(float[] viewMatrix) {
        this.viewMatrix = viewMatrix;
    }

    public void setHeadTransformQuaternion(float[] headTransformQuaternion) {
        this.headTransformQuaternion = headTransformQuaternion;
    }

    public VideoControllerRenderer(Context context, Handler handler) {
        this.handler = handler;
        this.context = context;
        handler.postDelayed(mFadeOut, SHOW_DEFAULT_TIMEOUT);
        calculateDefaultCorrection();
        init(context);
    }


    public void onSurfaceChanged(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;
        if (cursor != null) {
            cursor.setViewSize(width, height);
        }
    }

    private void init(Context context) {
        initCursor(context);
        initPanel(context);
        initDetectionArea();
        initIcons(context);
        initSeekBar(context);
    }

    private void initSeekBar(Context context) {
        seekBar = new SeekBar(context,
                new float[]{
                        panel.getLeftBottomCornerCoords()[0] + PANEL_PADDING,
                        panel.getLeftBottomCornerCoords()[1] + PANEL_PADDING + SEEK_BAR_MARGIN,
                        panel.getLeftBottomCornerCoords()[2] + LAYERS_MARGIN_Z
                },
                SEEK_BAR_WIDTH,
                SEEK_BAR_PROGRESS_HEIGHT,
                SEEK_BAR_CLICK_AREA_HEIGHT);
        cursor.addSelectableObject(seekBar);
        seekBar.setSeekBarListener(new SeekBar.SeekBarListener() {
            @Override
            public void onClick(float progress) {
                if (videoPlayer == null || videoPlayer.getPlayer() == null) {
                    return;
                }
                long newPos = getPosFromPercents(progress);
                if (newPos >= 0) {
                    videoPlayer.getPlayer().seekTo(newPos);
                }
            }
        });
        seekBar.setCursorProgressFormater(new SeekBar.CursorProgressFormater() {
            @Override
            public String format(float progress) {
                long pos = getPosFromPercents(progress);
                if (pos > 0) {
                    return StringUtil.millisecondsToString((int) pos);
                }
                return "";
            }
        });
    }


    private void initCursor(Context context) {
        cursor = new Cursor(context, viewWidth, viewHeight);
    }

    private void initPanel(Context context) {
        float width = Math.max(SEEK_BAR_WIDTH, IconsList.getWidth(2)) + (PANEL_PADDING * 2.0f);
        float height = IconsList.getIconSize() +
                SeekBar.calculateHeight(SEEK_BAR_CLICK_AREA_HEIGHT) + SEEK_BAR_MARGIN * 2.0f +
                PANEL_PADDING * 2.0f;
        panel = new Panel(context,
                new float[]{PANEL_X, PANEL_Y, PANEL_Z},
                width,
                height
        );
    }

    private void initDetectionArea() {
        if (panel != null) {
            float[] pLBCCoords = panel.getLeftBottomCornerCoords();
            float pWidth = panel.getWidth();
            float pHeight = panel.getHeight();

            detectionAreaCoords = new float[]{
                    pLBCCoords[0] - DETECTION_AREA_PADDING, pLBCCoords[1] - DETECTION_AREA_PADDING, pLBCCoords[2],
                    pLBCCoords[0] + pWidth + DETECTION_AREA_PADDING, pLBCCoords[1] - DETECTION_AREA_PADDING, pLBCCoords[2],
                    pLBCCoords[0] + pWidth + DETECTION_AREA_PADDING, pLBCCoords[1] + pHeight + DETECTION_AREA_PADDING, pLBCCoords[2],
                    pLBCCoords[0] - DETECTION_AREA_PADDING, pLBCCoords[1] + pHeight + DETECTION_AREA_PADDING, pLBCCoords[2],
            };
        }
    }


    private void initIcons(Context context) {
        iconsList = new IconsList(new float[]{
                panel.getLeftBottomCornerCoords()[0] + PANEL_PADDING,
                panel.getLeftBottomCornerCoords()[1] + PANEL_PADDING +
                        SeekBar.calculateHeight(SEEK_BAR_CLICK_AREA_HEIGHT) + SEEK_BAR_MARGIN * 2.0f,
                panel.getLeftBottomCornerCoords()[2] + LAYERS_MARGIN_Z
        });
        playPauseIcon = iconsList.add(context,
                new String[]{"Pause", "Play"},
                new int[]{
                        R.drawable.ic_pause_circle_outline_white,
                        R.drawable.ic_play_circle_outline_white
                }, new Runnable() {
                    @Override
                    public void run() {
                        doTogglePauseResume();
                    }
                });

        cursor.addSelectableObject(playPauseIcon);

        Icon recenterIcon = iconsList.add(context,
                "Recenter view",
                R.drawable.ic_my_location_white,
                new Runnable() {
                    @Override
                    public void run() {
                        status.add(Status.RECENTER_VIEW);
                        cursor.setSelectableObjectWrapper(
                                new Cursor.SelectableObjectWrapper(cursor, new Cursor.SelectableObject() {
                                    @Override
                                    public float[] getSelectableAreaCoords() {
                                        return new float[0];
                                    }

                                    @Override
                                    public short[] getSelectableAreaVertexIndexes() {
                                        return new short[0];
                                    }

                                    @Override
                                    public void onCursorOver(Cursor cursor) {
                                    }

                                    @Override
                                    public void onCursorMoveOver(Cursor cursor, float[] coords) {
                                    }

                                    @Override
                                    public void onCursorOut(Cursor cursor) {

                                    }

                                    @Override
                                    public void onClick(Cursor cursor) {
                                        if (videoControllerListener != null) {
                                            videoControllerListener.onRecenterView();
                                        }
                                        status.remove(Status.RECENTER_VIEW);
                                    }
                                }, Cursor.CLICK_TIMEOUT, 0)
                        );
                        hide();
                    }
                });
        cursor.addSelectableObject(recenterIcon);
    }


    private void updatePausePlayBtn() {
        if (playPauseIcon == null) {
            return;
        }

        if (videoPlayer == null || videoPlayer.getPlayer() == null) {
            return;
        }


        if (!videoPlayer.getPlayer().isPlaying() ||
                videoPlayer.getPlayer().isEnded()) {
            playPauseIcon.setHintIndex(1);
            playPauseIcon.setTextureIndex(1);
        } else {
            playPauseIcon.setHintIndex(0);
            playPauseIcon.setTextureIndex(0);
        }

    }


    private void doRayPickingForDetectionArea(float[] modelViewMatrix, float[] projMatrix) {
        if (cursor.getCursorX() < 0 || cursor.getCursorY() < 0) {
            return;
        }
        //Check is cursor over detection area
        isForceShow = RayPicking.rayPicking(viewWidth, viewHeight,
                cursor.getCursorX(), cursor.getCursorY(), modelViewMatrix, projMatrix,
                detectionAreaCoords, DETECTION_AREA_VERTEX_INDEXES) != null;
        if (isForceShow && !Status.isShow(status)) {
            show();
        } else if (!Status.isShow(status)) {
            return;
        }
    }


    public void render(Eye eye) {

        if (videoPlayer == null || viewMatrix == null) {
            return;
        }

        if (cursor != null) {
            cursor.calculateCursorScreenCoords(eye, viewMatrix);
        }

        if (status.contains(Status.RECENTER_VIEW)) {
            cursor.render(eye, viewMatrix);
            return;
        }

        if (eye.getType() == 2 && !Status.isShow(status)) {
            return;
        }

        if (eye.getType() <= 1 && status.contains(Status.NEED_RECALCULATE_ROTATE)) {
            recalculateCorrection();
            status.remove(Status.NEED_RECALCULATE_ROTATE);
        }

        float[] projectionMatrix = eye.getPerspective(Z_NEAR, Z_FAR);
        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Quaternion.rotateM(modelMatrix, 0, headTransformQuaternion);
        Quaternion.rotateM(modelMatrix, 0, panelCorrectionQuaternion);

        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

        if (eye.getType() <= 1) {
            doRayPickingForDetectionArea(modelViewMatrix, projectionMatrix);
            if (!Status.isShow(status)) {
                return;
            }
            cursor.lookUpSelectedObject(modelViewMatrix, projectionMatrix);
            updatePausePlayBtn();
            updateProgress();
        }

        panel.render(modelViewProjectionMatrix);
        iconsList.render(modelViewProjectionMatrix);
        seekBar.render(modelViewProjectionMatrix);
        cursor.render(eye, viewMatrix);
    }


    private void calculateDefaultCorrection() {
        Quaternion.fromEulerAngles(panelDefaultQuaternion, (float) Math.toRadians(DEGREE_TO_SHOW_PANEL), 0.0f, 0.0f);
        panelCorrectionQuaternion = panelDefaultQuaternion.clone();
    }

    private void recalculateCorrection() {
        Quaternion.inverse(panelCorrectionQuaternion, headTransformQuaternion);
    }

    private void updateProgress() {
        if (videoPlayer == null || videoPlayer.getPlayer() == null || seekBar == null) {
            seekBar.setProgress(0);
            return;
        }
        long position = videoPlayer.getPlayer().getCurrentPosition();
        long duration = videoPlayer.getPlayer().getDuration();
        if (duration <= 1) {
            seekBar.setProgress(0);
            return;
        }
        if (duration < position) {
            position = duration;
        }
        if (duration > 0) {
            // use long to avoid overflow
            seekBar.setProgress(100L * position / duration);
        } else {
            seekBar.setProgress(0);
        }
        seekBar.setSecondaryProgress(videoPlayer.getPlayer().getBufferPercentage());
        seekBar.setTimeStr(StringUtil.millisecondsToString((int) position),
                StringUtil.millisecondsToString((int) duration));
    }

    public void onTouch() {
        if (status.contains(Status.RECENTER_VIEW)) {
            return;
        }
        if (!Status.isShow(status)) {
            if (!status.contains(Status.NEED_RECALCULATE_ROTATE)) {
                status.add(Status.NEED_RECALCULATE_ROTATE);
            }
            show();
            return;
        }

        if (!cursor.click(true)) {
            hide();
        }
    }

    public void toggleViewStatus() {
        if (!Status.isShow(status)) {
            show();
        } else {
            hide();
        }
    }


    private long getPosFromPercents(float p) {
        if (videoPlayer == null || videoPlayer.getPlayer() == null) {
            return -1;
        }
        if (videoPlayer.getPlayer() != null) {
            long duration = videoPlayer.getPlayer().getDuration();
            if (duration <= 1) {
                return -1;
            }
            long pos = (long) (duration * p) / 100L;
            if (pos > 0 && pos < duration) {
                return pos;
            }
        }
        return -1;
    }

    private void doTogglePauseResume() {
        if (videoPlayer == null || videoPlayer.getPlayer() == null) {
            return;
        }
        if (videoPlayer.getPlayer().isPlaying()) {
            videoPlayer.getPlayer().pause();
        } else if (videoPlayer.getPlayer().isEnded()) {
            videoPlayer.getPlayer().seekTo(0);
            videoPlayer.getPlayer().start();
        } else {
            videoPlayer.getPlayer().start();
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after {@link #SHOW_DEFAULT_TIMEOUT} seconds of inactivity.
     */
    public void show() {
        show(SHOW_DEFAULT_TIMEOUT);
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show
     *                the controller until hide() is called.
     */
    public void show(final int timeout) {
        if (!Status.isShow(status)) {
            status.remove(Status.HIDE);
            status.add(Status.SHOW);
        }

        if (timeout != 0) {
            handler.removeCallbacks(mFadeOut);
            handler.postDelayed(mFadeOut.setTimeout(timeout), timeout);
        } else {
            handler.removeCallbacks(mFadeOut);
        }
    }


    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (Status.isShow(status)) {
            panelCorrectionQuaternion = panelDefaultQuaternion.clone();
            status.remove(Status.SHOW);
            status.add(Status.HIDE);
        }
    }

    private class FadeOut implements Runnable {

        int timeout = 0;
        private volatile ReentrantLock timeoutLock = new ReentrantLock();

        public FadeOut setTimeout(int timeout) {
            try {
                timeoutLock.lock();
                this.timeout = timeout;
            } finally {
                timeoutLock.unlock();
            }
            return this;
        }

        public FadeOut(int timeout) {
            this.timeout = timeout;
        }

        @Override
        public void run() {
            try {
                timeoutLock.lock();

                if (Status.isShow(status) &&
                        isForceShow) {
                    handler.removeCallbacks(this);
                    handler.postDelayed(this, timeout);
                    return;
                }
                hide();
            } finally {
                timeoutLock.unlock();
            }
        }
    }

    private FadeOut mFadeOut = new FadeOut(0);


    public void onDestroy() {
        if (handler != null) {
            handler.removeCallbacks(mFadeOut);
        }
    }

    public interface VideoControllerListener {
        void onRecenterView();
    }


}
