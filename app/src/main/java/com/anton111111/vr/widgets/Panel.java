package com.anton111111.vr.widgets;


import android.content.Context;
import android.opengl.GLES20;

import com.anton111111.vr.GLHelper;
import com.anton111111.vr.program.Program;
import com.anton111111.vr.program.ProgramHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Panel {

    private static final short[] VERTEX_INDEXES = new short[]{
            0, 1, 2, 0, 2, 3
    };

    private static final float PANEL_COLOR[] = {
            0.412f, 0.412f, 0.412f, 0.5f

    };
    private final float[] coords;
    private final FloatBuffer verticesBuffer;
    private final ShortBuffer verticesIndexesBuffer;
    private final float width;
    private final float height;
    private final float[] centerCoords;

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float[] getCenterCoords() {
        return centerCoords;
    }

    public float[] getCoords() {
        return coords;
    }

    public static short[] getVertexIndexes() {
        return VERTEX_INDEXES;
    }

    public float[] getLeftBottomCornerCoords() {
        return new float[]{
                coords[0], coords[1], coords[2]
        };
    }

    /**
     * @param context
     * @param centerCoords panel center coords
     * @param width
     * @param height
     */
    public Panel(Context context, float[] centerCoords, float width, float height) {
        this.width = width;
        this.height = height;
        this.centerCoords = centerCoords;
        float _x = centerCoords[0] - width / 2.0f;
        float _y = centerCoords[1] - height / 2.0f;
        this.coords = new float[]{
                _x, _y, centerCoords[2],
                _x + width, _y, centerCoords[2],
                _x + width, _y + height, centerCoords[2],
                _x, _y + height, centerCoords[2],
        };

        ByteBuffer bbcv = ByteBuffer.allocateDirect(coords.length * 4);
        bbcv.order(ByteOrder.nativeOrder());
        verticesBuffer = bbcv.asFloatBuffer();
        verticesBuffer.put(coords);
        verticesBuffer.position(0);

        ByteBuffer bbSVIB = ByteBuffer.allocateDirect(VERTEX_INDEXES.length * 2);
        bbSVIB.order(ByteOrder.nativeOrder());
        verticesIndexesBuffer = bbSVIB.asShortBuffer();
        verticesIndexesBuffer.put(VERTEX_INDEXES);
        verticesIndexesBuffer.position(0);

        ProgramHelper.initShapeColoredProgram(context);
    }


    public void render(float[] modelViewProjection) {
        Program program = ProgramHelper.getInstance().useProgram(ProgramHelper.PROGRAM_SHAPE_COLORED);
        GLES20.glVertexAttribPointer(
                program.getAttr(ProgramHelper.SHAPE_COLORED_ATTR_POSITION), 3,
                GLES20.GL_FLOAT, false,
                12, verticesBuffer);


        GLES20.glUniform4fv(program.getUniform(ProgramHelper.SHAPE_COLORED_UNIFORM_COLOR),
                1, PANEL_COLOR, 0);

        GLES20.glUniformMatrix4fv(program.getUniform(ProgramHelper.SHAPE_COLORED_UNIFORM_MVP),
                1, false, modelViewProjection, 0);

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, 6,
                GLES20.GL_UNSIGNED_SHORT, verticesIndexesBuffer);
        // Disable vertex array
        GLHelper.checkGLError("Panel renderPanel");
    }
}
