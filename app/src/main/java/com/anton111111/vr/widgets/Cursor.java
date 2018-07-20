package com.anton111111.vr.widgets;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.Matrix;

import com.google.vr.sdk.base.Eye;
import com.anton111111.vr.GLHelper;
import com.anton111111.vr.program.Program;
import com.anton111111.vr.program.ProgramHelper;
import com.anton111111.vr.raypicking.RayPicking;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;


public class Cursor {

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CURSOR_TEXT_HEIGHT = 0.15f;
    private static final float CURSOR_TEXT_MARGIN = 0.1f;
    private static final int CURSOR_TEXT_FONT_COLOR = Color.WHITE;
    private static final int CURSOR_TEXT_GLOW_COLOR = Color.BLACK;

    /**
     * Delay before start timer for click timeout
     */
    public static final long CLICK_TIMER_DELAY = 600;
    public static final long CLICK_TIMEOUT = 2000;

    private static final float CURSOR_SIZE = 0.06f;
    private static final float CURSOR_EXPANDED_SIZE = 0.12f;
    private static final float CURSOR_THICKNESS = 0.015f;
    private static final float CURSOR_ANIMATION_STEP = 0.01f;
    private static final int CURSOR_ANIMATION_SPEED = 20;

    private static final float PROGRESS_CIRCLE_SIZE = 0.4f;
    private static final float PROGRESS_CIRCLE_THICKNESS = 0.03f;

    private static final float CURSOR_Z = -3.0f;

    private static final short[] CURSOR_VERTICES_INDEXES = new short[]{
            0, 1, 2, 0, 2, 3
    };


    private static final float CURSOR_COLOR[] = {
            1.0f, 1.0f, 1.0f, 1.0f

    };

    public enum Status {
        ANIMATION_EXPAND,
        ANIMATION_COLLAPSE;

        public static EnumSet<Status> none() {
            return EnumSet.noneOf(Status.class);
        }

        public static void stopAnimation(EnumSet<Status> status) {
            status.remove(ANIMATION_EXPAND);
            status.remove(ANIMATION_COLLAPSE);
        }

        public static void startCollapseAnimation(EnumSet<Status> status) {
            Status.stopAnimation(status);
            status.add(ANIMATION_COLLAPSE);
        }

        public static void startExpandAnimation(EnumSet<Status> status) {
            Status.stopAnimation(status);
            status.add(ANIMATION_EXPAND);
        }
    }

    private EnumSet<Status> status = Status.none();
    private final FloatBuffer cursorVerticesBuffer;
    private final ShortBuffer verticesIndexesBuffer;
    private final float[] coords;
    private final float[] cursorCenterCoords;
    private final float[] progressCoords;
    private final FloatBuffer progressVerticesBuffer;
    private float cursorX = -1;
    private float cursorY = -1;
    private int viewWidth = -1;
    private int viewHeight = -1;
    private float cursorSize = CURSOR_SIZE;
    private List<SelectableObject> selectableObjects = new ArrayList<>();
    private SelectableObjectWrapper selectableObjectWrapper;
    private float progressPercents = 0;
    private Text cursorText;
    private long animationTimer = System.currentTimeMillis();


    public void cancelClickTimer(long clickTimeout, long startTimerDelay) {
        if (selectableObjectWrapper != null) {
            selectableObjectWrapper.cancelTimer(clickTimeout, startTimerDelay);
        }
    }

    public void setActiveClickTimer(boolean b) {
        if (selectableObjectWrapper != null) {
            selectableObjectWrapper.setActive(b);
        }
    }

    public void setSelectableObjectWrapper(SelectableObjectWrapper selectableObjectWrapper) {
        if (this.selectableObjectWrapper != null) {
            this.selectableObjectWrapper.getObject().onCursorOut(this);
        }
        this.selectableObjectWrapper = selectableObjectWrapper;
    }

    public void addSelectableObject(SelectableObject object) {
        if (!selectableObjects.contains(object)) {
            selectableObjects.add(object);
        }
    }

    public void removeSelectableObject(SelectableObject object) {
        selectableObjects.remove(object);
    }

    public float getCursorX() {
        return cursorX;
    }

    public float getCursorY() {
        return cursorY;
    }

    public void setViewSize(int viewWidth, int viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        cursorX = -1;
        cursorY = -1;
    }

    public void setCursorText(String text) {
        cursorText.setText(text);
    }

    public Cursor(Context context, int viewWidth, int viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;

        float halfSize = CURSOR_EXPANDED_SIZE / 2.0f;
        coords = new float[]{
                0.0f - halfSize, 0.0f - halfSize, CURSOR_Z,
                0.0f + halfSize, 0.0f - halfSize, CURSOR_Z,
                0.0f + halfSize, 0.0f + halfSize, CURSOR_Z,
                0.0f - halfSize, 0.0f + halfSize, CURSOR_Z
        };

        cursorCenterCoords = new float[]{
                0, 0, CURSOR_Z, 1.0f
        };

        cursorText = new Text("")
                .setTextColor(CURSOR_TEXT_FONT_COLOR)
                .setGlowColor(CURSOR_TEXT_GLOW_COLOR)
                .setHeight(CURSOR_TEXT_HEIGHT)
                .setStartCoordsType(Text.START_COORDS_TYPE_CENTER)
                .prepare(context, new float[]{
                        0.0f, PROGRESS_CIRCLE_SIZE / 2.0f + CURSOR_TEXT_MARGIN, CURSOR_Z
                });

        ByteBuffer bbcv = ByteBuffer.allocateDirect(coords.length * 4);
        bbcv.order(ByteOrder.nativeOrder());
        cursorVerticesBuffer = bbcv.asFloatBuffer();
        cursorVerticesBuffer.put(coords);
        cursorVerticesBuffer.position(0);


        halfSize = PROGRESS_CIRCLE_SIZE / 2.0f;
        progressCoords = new float[]{
                0.0f - halfSize, 0.0f - halfSize, CURSOR_Z,
                0.0f + halfSize, 0.0f - halfSize, CURSOR_Z,
                0.0f + halfSize, 0.0f + halfSize, CURSOR_Z,
                0.0f - halfSize, 0.0f + halfSize, CURSOR_Z
        };

        bbcv = ByteBuffer.allocateDirect(progressCoords.length * 4);
        bbcv.order(ByteOrder.nativeOrder());
        progressVerticesBuffer = bbcv.asFloatBuffer();
        progressVerticesBuffer.put(progressCoords);
        progressVerticesBuffer.position(0);

        ByteBuffer bbSVIB = ByteBuffer.allocateDirect(CURSOR_VERTICES_INDEXES.length * 2);
        bbSVIB.order(ByteOrder.nativeOrder());
        verticesIndexesBuffer = bbSVIB.asShortBuffer();
        verticesIndexesBuffer.put(CURSOR_VERTICES_INDEXES);
        verticesIndexesBuffer.position(0);

        ProgramHelper.initCircleColoredProgram(context);
        ProgramHelper.initProgressCircleProgram(context);
    }

    public void updateProgressPercents() {
        if (selectableObjectWrapper != null) {
            progressPercents = selectableObjectWrapper.getPercents();
            if (progressPercents >= 100) {
                progressPercents = 0;
                selectableObjectWrapper.click();
            }
        } else {
            progressPercents = 0;
        }
    }


    /**
     * Render cursor and progress circle
     *
     * @param eye
     */
    public void render(Eye eye, float[] viewMatrix) {

        //All calculation and operations do for first eye type (left or mono)
        if (eye.getType() <= 1) {
            updateProgressPercents();
        }

        // Change depth func so our HUD is always rendered atop
        GLES20.glDepthFunc(GLES20.GL_ALWAYS);
        // Disable depth writes
        GLES20.glDepthMask(false);

        //Prepare modelViewProjectionMatrix
        float[] projectionMatrix = eye.getPerspective(Z_NEAR, Z_FAR);
        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

        renderCursor(modelViewProjectionMatrix);

        if (cursorText.getText() != null && !cursorText.getText().isEmpty()) {
            cursorText.render(modelViewProjectionMatrix);
        }

        if (progressPercents > 0) {
            renderProgress(modelViewProjectionMatrix, progressPercents);
        }

        // Restore depth func an depth write
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDepthMask(true);

        GLHelper.checkGLError("Cursor renderCursor");
    }

    private void renderProgress(float[] modelViewProjectionMatrix, float progress) {
        if (progress > 100) {
            progress = 100;
        }

        Program program = ProgramHelper.getInstance().useProgram(ProgramHelper.PROGRAM_PROGRESS_CIRCLE);

        GLES20.glVertexAttribPointer(program.getAttr(ProgramHelper.PROGRESS_CIRCLE_ATTR_POSITION),
                3, GLES20.GL_FLOAT, false, 12, progressVerticesBuffer);

        GLES20.glUniform3fv(program.getUniform(ProgramHelper.PROGRESS_CIRCLE_UNIFORM_CENTER),
                1, cursorCenterCoords, 0);

        GLES20.glUniform4fv(program.getUniform(ProgramHelper.PROGRESS_CIRCLE_UNIFORM_COLOR),
                1, CURSOR_COLOR, 0);

        GLES20.glUniform1f(program.getUniform(ProgramHelper.PROGRESS_CIRCLE_UNIFORM_RADIUS),
                PROGRESS_CIRCLE_SIZE / 2.0f);
        GLES20.glUniform1f(program.getUniform(ProgramHelper.PROGRESS_CIRCLE_UNIFORM_THICKNESS),
                PROGRESS_CIRCLE_THICKNESS);
        GLES20.glUniformMatrix4fv(program.getUniform(ProgramHelper.PROGRESS_CIRCLE_UNIFORM_MVP),
                1, false, modelViewProjectionMatrix, 0);

        GLES20.glUniform1f(program.getUniform(ProgramHelper.PROGRESS_CIRCLE_UNIFORM_PERCENT),
                progress);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                CURSOR_VERTICES_INDEXES.length,
                GLES20.GL_UNSIGNED_SHORT,
                verticesIndexesBuffer);
    }

    private void renderCursor(float[] modelViewProjectionMatrix) {

        if ((status.contains(Status.ANIMATION_COLLAPSE) || status.contains(Status.ANIMATION_EXPAND)) &&
                (System.currentTimeMillis() - animationTimer) > CURSOR_ANIMATION_SPEED) {

            animationTimer = System.currentTimeMillis();

            if (status.contains(Status.ANIMATION_COLLAPSE)) {
                cursorSize -= CURSOR_ANIMATION_STEP;
                if (cursorSize <= CURSOR_SIZE) {
                    Status.stopAnimation(status);
                    cursorSize = CURSOR_SIZE;
                }
            }

            if (status.contains(Status.ANIMATION_EXPAND)) {
                cursorSize += CURSOR_ANIMATION_STEP;
                if (cursorSize >= CURSOR_EXPANDED_SIZE) {
                    Status.stopAnimation(status);
                    cursorSize = CURSOR_EXPANDED_SIZE;
                }
            }
        }

        Program program = ProgramHelper.getInstance().useProgram(ProgramHelper.PROGRAM_CIRCLE_COLORED);

        GLES20.glVertexAttribPointer(program.getAttr(ProgramHelper.CIRCLE_COLORED_ATTR_POSITION),
                3, GLES20.GL_FLOAT, false, 12, cursorVerticesBuffer);

        GLES20.glUniform3fv(program.getUniform(ProgramHelper.CIRCLE_COLORED_UNIFORM_CENTER),
                1, cursorCenterCoords, 0);

        GLES20.glUniform4fv(program.getUniform(ProgramHelper.CIRCLE_COLORED_UNIFORM_COLOR),
                1, CURSOR_COLOR, 0);

        GLES20.glUniform1f(program.getUniform(ProgramHelper.CIRCLE_COLORED_UNIFORM_RADIUS),
                cursorSize / 2.0f);
        GLES20.glUniform1f(program.getUniform(ProgramHelper.CIRCLE_COLORED_UNIFORM_THICKNESS),
                CURSOR_THICKNESS);
        GLES20.glUniformMatrix4fv(program.getUniform(ProgramHelper.CIRCLE_COLORED_UNIFORM_MVP),
                1, false, modelViewProjectionMatrix, 0);


        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                CURSOR_VERTICES_INDEXES.length,
                GLES20.GL_UNSIGNED_SHORT,
                verticesIndexesBuffer);
    }

    /**
     * Calculate cursor screen coords is need
     *
     * @param eye
     * @param viewMatrix
     */
    public void calculateCursorScreenCoords(Eye eye, float[] viewMatrix) {
        if (viewWidth < 0 || viewHeight < 0 ||
                (cursorX > 0 && cursorY > 0)) {
            return;
        }

        float[] projectionMatrix = eye.getPerspective(Z_NEAR, Z_FAR);
        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        int[] viewport = {0, 0, viewWidth, viewHeight};

        float[] screenCoords = new float[3];
        GLU.gluProject(cursorCenterCoords[0], cursorCenterCoords[1], cursorCenterCoords[2],
                modelViewMatrix, 0,
                projectionMatrix, 0,
                viewport, 0,
                screenCoords, 0
        );
        cursorX = screenCoords[0];
        cursorY = screenCoords[1];
    }

    /**
     * Do click
     *
     * @param force ignore isActive status
     * @return return true is click is made
     */
    public boolean click(boolean force) {
        if (selectableObjectWrapper != null) {
            selectableObjectWrapper.click(force);
            return true;
        }
        return false;
    }

    /**
     * Do ray picking to lookup object selected with cursor
     *
     * @return number os found objects
     */
    public boolean lookUpSelectedObject(float[] modelViewMatrix, float[] projMatrix) {
        if (cursorX < 0 || cursorY < 0) {
            return false;
        }

        for (SelectableObject sObj : selectableObjects) {
            float[] intersectPoint = RayPicking.rayPicking(viewWidth, viewHeight, cursorX, cursorY,
                    modelViewMatrix, projMatrix,
                    sObj.getSelectableAreaCoords(), sObj.getSelectableAreaVertexIndexes());

            if (intersectPoint == null) {
                continue;
            }

            if (selectableObjectWrapper == null ||
                    !selectableObjectWrapper.isContainObject(sObj)) {
                if (selectableObjectWrapper != null) {
                    selectableObjectWrapper.getObject().onCursorOut(this);
                }
                selectableObjectWrapper = new SelectableObjectWrapper(this, sObj, CLICK_TIMEOUT, CLICK_TIMER_DELAY);
                sObj.onCursorOver(this);
                Status.startExpandAnimation(status);
            }
            sObj.onCursorMoveOver(this, intersectPoint);
            return true;
        }

        if (selectableObjectWrapper != null) {
            selectableObjectWrapper.getObject().onCursorOut(this);
            selectableObjectWrapper = null;
            Status.startCollapseAnimation(status);
        }

        return false;
    }


    public static class SelectableObjectWrapper {
        private final Cursor cursor;
        private long timeout;
        private long startTime = -1;
        private SelectableObject object;
        private boolean active = true;

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void cancelTimer(long clickTimeout, long startTimerDelay) {
            this.startTime = new Date().getTime() + startTimerDelay;
            this.timeout = clickTimeout;
        }

        public SelectableObjectWrapper(Cursor cursor, SelectableObject object, long clickTimeout, long startTimerDelay) {
            this.cursor = cursor;
            cancelTimer(clickTimeout, startTimerDelay);
            this.object = object;
            this.active = true;
        }

        /**
         * Do click is wrapper is active
         *
         * @return
         */
        public boolean click() {
            return click(true);
        }

        /**
         * Do click
         *
         * @param force ignore isActive status
         * @return
         */
        public boolean click(boolean force) {
            if (!isActive() && !force) {
                return false;
            }
            if (object != null) {
                setActive(false);
                object.onClick(cursor);
                return true;
            }
            return false;
        }

        public boolean isContainObject(SelectableObject o) {
            return this.object == o;
        }

        public SelectableObject getObject() {
            return object;
        }

        public float getPercents() {
            if (!active) {
                return 0.0f;
            }
            long time = new Date().getTime() - this.startTime;
            if (time <= 0) {
                return 0.0f;
            }
            float p = Double.valueOf((double) time / (double) timeout * 100.0d).floatValue();
            return (p > 100) ? 100.0f : p;
        }
    }

    public interface SelectableObject {

        float[] getSelectableAreaCoords();

        short[] getSelectableAreaVertexIndexes();

        /**
         * On cursor over object
         *
         * @param cursor
         */
        void onCursorOver(Cursor cursor);

        /**
         * On cursor move over object
         *
         * @param cursor
         * @param coords {x,y,z} in world space
         * @return
         */
        void onCursorMoveOver(Cursor cursor, float[] coords);

        /**
         * on cursor out of object
         *
         * @param cursor
         */
        void onCursorOut(Cursor cursor);

        /**
         * On click object
         *
         * @param cursor
         */
        void onClick(Cursor cursor);
    }

}
