package com.anton111111.vr.widgets;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;

import com.anton111111.vr.GLHelper;
import com.anton111111.vr.program.Program;
import com.anton111111.vr.program.ProgramHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class SeekBar implements Cursor.SelectableObject {

    private static final float TIMER_HEIGHT = 0.04f;
    private static final float TIMER_MARGIN = 0.0f;
    private static final int TIMER_FONT_COLOR = Color.WHITE;
    private static final int TIMER_GLOW_COLOR = Color.BLACK;

    private static final float MIN_CURSOR_STEP = 2.0f;


    private static final short[] VERTEX_INDEXES = new short[]{
            0, 1, 2, 0, 2, 3
    };

    private static final float SEEKBAR_COLORS[] = {
            0.412f, 0.412f, 0.412f, 1.0f, //Background
            0.412f, 1.0f, 1.0f, 1.0f,  //Progress color
            0.412f, 0.412f, 1.0f, 1.0f  //Secondary Progress color
    };

    private final float[] progressBarCoords;
    private final float[] clickAreaCoords;
    private final float progressWidth;
    private final float progressHeight;
    private final float clickAreaHeight;
    private final float height;
    private final float width;
    private float progress = 0.0f;
    private float secondaryProgress = 0.0f;
    private float cursorProgress = -1.0f;
    private final FloatBuffer verticesBuffer;
    private final ShortBuffer verticesIndexesBuffer;
    private FloatBuffer verticesProgressBuffer;
    private FloatBuffer verticesSecondaryProgressBuffer;
    private final Text timer;
    private String timeStr = "00:00/00:00";
    private SeekBarListener seekBarListener;
    private CursorProgressFormater cursorProgressFormater;


    public void setCursorProgressFormater(CursorProgressFormater cursorProgressFormater) {
        this.cursorProgressFormater = cursorProgressFormater;
    }

    public static float calculateHeight(float clickAreaHeight) {
        return clickAreaHeight + TIMER_HEIGHT + TIMER_MARGIN;
    }

    public void setSeekBarListener(SeekBarListener seekBarListener) {
        this.seekBarListener = seekBarListener;
    }

    public void setTimeStr(String curTimeStr, String fullTimeStr) {
        this.timeStr = curTimeStr + "/" + fullTimeStr;
    }

    public void setSecondaryProgress(float secondaryProgress) {
        this.secondaryProgress = secondaryProgress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public float[] getProgressBarCoords() {
        return progressBarCoords;
    }

    public static short[] getVertexIndexes() {
        return VERTEX_INDEXES;
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public SeekBar(Context context, float[] startCoords, float progressWidth, float progressHeight, float clickAreaHeight) {
        this.progressWidth = progressWidth;
        this.progressHeight = progressHeight;
        this.clickAreaHeight = clickAreaHeight;

        float _y = startCoords[1];
        timer = new Text(timeStr)
                .setTextColor(TIMER_FONT_COLOR)
                .setGlowColor(TIMER_GLOW_COLOR)
                .setHeight(TIMER_HEIGHT)
                .setStartCoordsType(Text.START_COORDS_TYPE_LEFT_BOTTOM_CORNER)
                .prepare(context, new float[]{
                        startCoords[0], _y, startCoords[2]
                });

        _y += TIMER_MARGIN + TIMER_HEIGHT;

        this.clickAreaCoords = new float[]{
                startCoords[0], _y, startCoords[2],
                startCoords[0] + progressWidth, _y, startCoords[2],
                startCoords[0] + progressWidth, _y + clickAreaHeight, startCoords[2],
                startCoords[0], _y + clickAreaHeight, startCoords[2],
        };

        _y += (clickAreaHeight - progressHeight) / 2.0f;

        this.progressBarCoords = new float[]{
                startCoords[0], _y, startCoords[2],
                startCoords[0] + progressWidth, _y, startCoords[2],
                startCoords[0] + progressWidth, _y + progressHeight, startCoords[2],
                startCoords[0], _y + progressHeight, startCoords[2],
        };

        ByteBuffer bbcv = ByteBuffer.allocateDirect(progressBarCoords.length * 4);
        bbcv.order(ByteOrder.nativeOrder());
        verticesBuffer = bbcv.asFloatBuffer();
        verticesBuffer.put(progressBarCoords);
        verticesBuffer.position(0);

        ByteBuffer bbSVIB = ByteBuffer.allocateDirect(VERTEX_INDEXES.length * 2);
        bbSVIB.order(ByteOrder.nativeOrder());
        verticesIndexesBuffer = bbSVIB.asShortBuffer();
        verticesIndexesBuffer.put(VERTEX_INDEXES);
        verticesIndexesBuffer.position(0);

        ProgramHelper.initShapeColoredProgram(context);

        this.width = progressWidth;
        this.height = clickAreaHeight + TIMER_HEIGHT + TIMER_MARGIN;
    }

    private void prepareProgressBuffer() {
        float[] coords = Arrays.copyOf(this.progressBarCoords, this.progressBarCoords.length);
        float _x = Double.valueOf((double) coords[0] + (double) progressWidth * (double) progress / 100.0d).floatValue();
        coords[3] = _x;
        coords[6] = _x;
        ByteBuffer bbcv = ByteBuffer.allocateDirect(coords.length * 4);
        bbcv.order(ByteOrder.nativeOrder());
        verticesProgressBuffer = bbcv.asFloatBuffer();
        verticesProgressBuffer.put(coords);
        verticesProgressBuffer.position(0);
    }

    private void prepareSecondaryProgressBuffer() {
        float[] coords = Arrays.copyOf(this.progressBarCoords, this.progressBarCoords.length);
        float _x = Double.valueOf((double) coords[0] + (double) progressWidth * (double) secondaryProgress / 100.0d).floatValue();
        coords[3] = _x;
        coords[6] = _x;
        ByteBuffer bbcv = ByteBuffer.allocateDirect(coords.length * 4);
        bbcv.order(ByteOrder.nativeOrder());
        verticesSecondaryProgressBuffer = bbcv.asFloatBuffer();
        verticesSecondaryProgressBuffer.put(coords);
        verticesSecondaryProgressBuffer.position(0);
    }

    public void render(float[] modelViewProjection) {
        renderBackground(modelViewProjection);
        if (secondaryProgress > 0) {
            prepareSecondaryProgressBuffer();
            renderSecondaryProgress(modelViewProjection);
        }
        if (progress > 0) {
            prepareProgressBuffer();
            renderProgress(modelViewProjection);
        }

        if (timer != null) {
            timer.setText(timeStr);
            timer.render(modelViewProjection);
        }

    }

    private void renderProgress(float[] modelViewProjection) {
        Program program = ProgramHelper.getInstance().useProgram(ProgramHelper.PROGRAM_SHAPE_COLORED);
        GLES20.glVertexAttribPointer(
                program.getAttr(ProgramHelper.SHAPE_COLORED_ATTR_POSITION), 3,
                GLES20.GL_FLOAT, false,
                12, verticesProgressBuffer);


        GLES20.glUniform4fv(program.getUniform(ProgramHelper.SHAPE_COLORED_UNIFORM_COLOR),
                1, SEEKBAR_COLORS, 4);

        GLES20.glUniformMatrix4fv(program.getUniform(ProgramHelper.SHAPE_COLORED_UNIFORM_MVP),
                1, false, modelViewProjection, 0);

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, 6,
                GLES20.GL_UNSIGNED_SHORT, verticesIndexesBuffer);
        // Disable vertex array
        GLHelper.checkGLError("renderProgress renderPanel");
    }

    private void renderSecondaryProgress(float[] modelViewProjection) {
        Program program = ProgramHelper.getInstance().useProgram(ProgramHelper.PROGRAM_SHAPE_COLORED);
        GLES20.glVertexAttribPointer(
                program.getAttr(ProgramHelper.SHAPE_COLORED_ATTR_POSITION), 3,
                GLES20.GL_FLOAT, false,
                12, verticesSecondaryProgressBuffer);


        GLES20.glUniform4fv(program.getUniform(ProgramHelper.SHAPE_COLORED_UNIFORM_COLOR),
                1, SEEKBAR_COLORS, 8);

        GLES20.glUniformMatrix4fv(program.getUniform(ProgramHelper.SHAPE_COLORED_UNIFORM_MVP),
                1, false, modelViewProjection, 0);

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, 6,
                GLES20.GL_UNSIGNED_SHORT, verticesIndexesBuffer);
        // Disable vertex array
        GLHelper.checkGLError("renderProgress renderPanel");
    }

    private void renderBackground(float[] modelViewProjection) {
        Program program = ProgramHelper.getInstance().useProgram(ProgramHelper.PROGRAM_SHAPE_COLORED);
        GLES20.glVertexAttribPointer(
                program.getAttr(ProgramHelper.SHAPE_COLORED_ATTR_POSITION), 3,
                GLES20.GL_FLOAT, false,
                12, verticesBuffer);


        GLES20.glUniform4fv(program.getUniform(ProgramHelper.SHAPE_COLORED_UNIFORM_COLOR),
                1, SEEKBAR_COLORS, 0);

        GLES20.glUniformMatrix4fv(program.getUniform(ProgramHelper.SHAPE_COLORED_UNIFORM_MVP),
                1, false, modelViewProjection, 0);

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, 6,
                GLES20.GL_UNSIGNED_SHORT, verticesIndexesBuffer);
        // Disable vertex array
        GLHelper.checkGLError("renderBackground renderPanel");
    }

    /**
     * Get percents by x coord
     *
     * @param x world space X
     * @return
     */
    private float getPercent(float x) {
        if (progressBarCoords[0] >= x) {
            return 0.0f;
        }
        if (progressBarCoords[3] <= x) {
            return 100.0f;
        }
        float l = x - progressBarCoords[0];
        return l / progressWidth * 100.0f;
    }

    @Override
    public float[] getSelectableAreaCoords() {
        return clickAreaCoords;
    }

    @Override
    public short[] getSelectableAreaVertexIndexes() {
        return VERTEX_INDEXES;
    }

    @Override
    public void onCursorOver(Cursor cursor) {

    }

    @Override
    public void onCursorMoveOver(Cursor cursor, float[] coords) {
        float p = getPercent(coords[0]);
        if (cursorProgress < 0) {
            cursorProgress = p;
            if (cursorProgressFormater != null) {
                cursor.setCursorText(cursorProgressFormater.format(p));

            }
            return;
        }
        if (Math.abs(cursorProgress - p) < MIN_CURSOR_STEP) {
            return;
        }
        cursorProgress = p;
        if (cursorProgressFormater != null) {
            cursor.setCursorText(cursorProgressFormater.format(p));

        }
        cursor.cancelClickTimer(Cursor.CLICK_TIMEOUT, Cursor.CLICK_TIMER_DELAY);
    }

    @Override
    public void onCursorOut(Cursor cursor) {
        cursorProgress = -1.0f;
        cursor.setCursorText("");
    }

    @Override
    public void onClick(Cursor cursor) {
        if (seekBarListener != null) {
            seekBarListener.onClick(cursorProgress);
        }
        cursor.setActiveClickTimer(true);
    }

    public interface SeekBarListener {
        void onClick(float progress);
    }

    public interface CursorProgressFormater {
        String format(float progress);
    }


}
