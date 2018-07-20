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

/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 08.12.17.
 */
public class Icon implements Cursor.SelectableObject {

    private static final float HINT_HEIGHT = 0.04f;
    private static final float HINT_MARGIN = 0.02f;
    private static final int HINT_FONT_COLOR = Color.WHITE;
    private static final int HINT_GLOW_COLOR = Color.BLACK;

    public static final short[] VERTEX_INDEXES = new short[]{
            0, 1, 2, 0, 2, 3
    };

    private static final float TEXTURE_COORDS[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f};

    private final float[] iconCoords;
    private final Runnable callback;
    private Text[] hints;
    private int hintIndex = 0;
    private int[] texture;
    private int textureIndex = 0;
    private FloatBuffer iconVerticesBuffer;
    private ShortBuffer iconsVerticesIndexesBuffer;
    private FloatBuffer iconsTextureBuffer;
    private boolean isSelected;

    public float[] getIconCoords() {
        return iconCoords;
    }

    public boolean isSelected() {
        return isSelected;
    }


    public void setHintIndex(int hintIndex) {
        if (hintIndex > hints.length - 1) {
            throw new ArrayIndexOutOfBoundsException("Wrong hint index offset: " + hintIndex + " size: " + hints.length);
        }
        this.hintIndex = hintIndex;
    }

    public void setTextureIndex(int textureIndex) {
        if (textureIndex > texture.length - 1) {
            throw new ArrayIndexOutOfBoundsException("Wrong texture index offset: " + textureIndex + " size: " + texture.length);
        }
        this.textureIndex = textureIndex;
    }

    public Icon(Context context, String hint, int texture, float[] iconCoords, Runnable callback) {
        this(context, new String[]{hint}, new int[]{texture}, iconCoords, callback);
    }

    public Icon(Context context, String[] hints, int[] textures, float[] iconCoords, Runnable callback) {
        this.texture = textures;
        this.iconCoords = iconCoords;
        this.callback = callback;

        this.hints = new Text[hints.length];
        for (int i = 0; i < hints.length; i++) {
            if (hints == null || hints.length <= 0) {
                this.hints[i] = null;
            }
            float _x = Double.valueOf(
                    (double) iconCoords[0] +
                            ((double) iconCoords[3] - (double) iconCoords[0]) / 2.0d
            ).floatValue();

            float _y = Double.valueOf(
                    (double) iconCoords[10] + (double) HINT_MARGIN + (double) HINT_HEIGHT / 2.0d
            ).floatValue();

            this.hints[i] = new Text(hints[i])
                    .setTextColor(HINT_FONT_COLOR)
                    .setGlowColor(HINT_GLOW_COLOR)
                    .setHeight(HINT_HEIGHT)
                    .prepare(context, new float[]{
                            _x, _y, iconCoords[11]
                    });
        }
        prepareBuffers();
        ProgramHelper.initShapeTexturedProgram(context);
    }

    private void prepareBuffers() {
        ByteBuffer bbcv = ByteBuffer.allocateDirect(iconCoords.length * 4);
        bbcv.order(ByteOrder.nativeOrder());
        iconVerticesBuffer = bbcv.asFloatBuffer();
        iconVerticesBuffer.put(iconCoords);
        iconVerticesBuffer.position(0);

        ByteBuffer bbSVI = ByteBuffer.allocateDirect(VERTEX_INDEXES.length * 2);
        bbSVI.order(ByteOrder.nativeOrder());
        iconsVerticesIndexesBuffer = bbSVI.asShortBuffer();
        iconsVerticesIndexesBuffer.put(VERTEX_INDEXES);
        iconsVerticesIndexesBuffer.position(0);

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        iconsTextureBuffer = byteBuf.asFloatBuffer();
        iconsTextureBuffer.put(TEXTURE_COORDS);
        iconsTextureBuffer.position(0);
    }


    public void render(float[] modelViewProjection) {
        Program program = ProgramHelper.getInstance().useProgram(ProgramHelper.PROGRAM_SHAPE_TEXTURED);

        GLES20.glVertexAttribPointer(program.getAttr(ProgramHelper.SHAPE_TEXTURED_ATTR_TEXTURE_COORDS),
                2, GLES20.GL_FLOAT, false, 0, iconsTextureBuffer);


        GLES20.glUniformMatrix4fv(program.getUniform(ProgramHelper.SHAPE_TEXTURED_UNIFORM_MVP),
                1, false, modelViewProjection, 0);

        GLES20.glVertexAttribPointer(program.getAttr(ProgramHelper.SHAPE_TEXTURED_ATTR_POSITION),
                3, GLES20.GL_FLOAT, false, 12, iconVerticesBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[textureIndex]);
        GLES20.glUniform1i(program.getUniform(ProgramHelper.SHAPE_TEXTURED_UNIFORM_TEXTURE), 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                VERTEX_INDEXES.length,
                GLES20.GL_UNSIGNED_SHORT,
                iconsVerticesIndexesBuffer);


        if (hints[hintIndex] != null && isSelected) {
            hints[hintIndex].render(modelViewProjection);
        }

        GLHelper.checkGLError("Icon render");
    }

    @Override
    public float[] getSelectableAreaCoords() {
        return iconCoords;
    }

    @Override
    public short[] getSelectableAreaVertexIndexes() {
        return VERTEX_INDEXES;
    }

    @Override
    public void onCursorOver(Cursor cursor) {
        isSelected = true;
    }

    @Override
    public void onCursorMoveOver(Cursor cursor, float[] coords) {
    }

    @Override
    public void onCursorOut(Cursor cursor) {
        isSelected = false;
    }

    @Override
    public void onClick(Cursor cursor) {
        callback.run();
    }
}
