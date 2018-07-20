package com.anton111111.vr.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;

import com.anton111111.gvrvideoplayer.R;
import com.anton111111.vr.GLHelper;
import com.anton111111.vr.program.Program;
import com.anton111111.vr.program.ProgramHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 12.12.17.
 */
public class Text {

    public static final int START_COORDS_TYPE_CENTER = 1;
    public static final int START_COORDS_TYPE_LEFT_BOTTOM_CORNER = 2;
    public static final int START_COORDS_TYPE_LEFT_TOP_CORNER = 3;

    private static final float TEXTURE_PADDING = 10.0f;

    private static final float TEXTURE_COORDS[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f};

    private static final short[] VERTEX_INDEXES = new short[]{
            0, 1, 2, 0, 2, 3
    };

    private float fontPixelSize;
    private String text = "";
    private float width = -1.0f;
    private float height = -1.0f;
    private float texturePadding = TEXTURE_PADDING;
    private boolean isPrepared = false;
    private boolean isNeedGlow = true;
    private int textColor = Color.WHITE;
    private int glowColor = Color.BLACK;
    private int texture;
    private int startCoordsType = START_COORDS_TYPE_CENTER;
    private float[] coords;
    private FloatBuffer verticesBuffer;
    private ShortBuffer verticesIndexesBuffer;
    private FloatBuffer textureBuffer;
    private float[] startCoords;

    public boolean isPrepared() {
        return isPrepared;
    }

    public Text setStartCoordsType(int startCoordsType) {
        if (this.startCoordsType == startCoordsType) {
            return this;
        }
        this.startCoordsType = startCoordsType;
        if (isPrepared) {
            prepareBuffers();
        }
        return this;
    }

    public Text setHeight(float height) {
        if (this.height == height) {
            return this;
        }
        this.height = height;
        if (isPrepared) {
            prepareTexture();
            prepareBuffers();
        }
        return this;
    }

    public Text setTextColor(int textColor) {
        if (this.textColor == textColor) {
            return this;
        }
        this.textColor = textColor;
        if (isPrepared) {
            prepareTexture();
            prepareBuffers();
        }
        return this;
    }

    public Text setGlowColor(int glowColor) {
        if (this.glowColor == glowColor) {
            return this;
        }
        this.glowColor = glowColor;
        if (isPrepared) {
            prepareTexture();
            prepareBuffers();
        }
        return this;
    }

    /**
     * Set font size in pixels
     *
     * @param fontPixelSize
     * @return
     */
    public Text setFontPixelSize(float fontPixelSize) {
        if (this.fontPixelSize == fontPixelSize) {
            return this;
        }
        this.fontPixelSize = fontPixelSize;
        if (isPrepared) {
            prepareTexture();
            prepareBuffers();
        }
        return this;
    }

    /**
     * Set texture padding in pixels
     *
     * @param padding
     * @return
     */
    public Text setTexturePadding(float padding) {
        if (this.texturePadding == texturePadding) {
            return this;
        }
        this.texturePadding = padding;
        if (isPrepared) {
            prepareTexture();
            prepareBuffers();
        }
        return this;
    }


    public Text setText(String text) {
        if (this.text.equals(text)) {
            return this;
        }
        if (text == null) {
            text = "";
        }
        this.text = text;
        if (isPrepared) {
            prepareTexture();
            prepareBuffers();
        }
        return this;
    }

    public String getText() {
        return text;
    }

    /**
     * @param text
     */
    public Text(String text) {
        this.text = text;

    }

    /**
     * Prepare texture and buffers
     *
     * @param context
     * @param startCoords coords of center
     */
    public Text prepare(Context context, float[] startCoords) {
        fontPixelSize = context.getResources().getDimensionPixelSize(R.dimen.vr_player_activity_text_font_size);
        this.startCoords = startCoords;
        prepareTexture();
        prepareBuffers();
        ProgramHelper.initShapeTexturedProgram(context);
        isPrepared = true;
        return this;
    }

    private void prepareBuffers() {
        float _x;
        float _y;
        switch (startCoordsType) {
            case START_COORDS_TYPE_CENTER:
                _x = Double.valueOf(
                        (double) startCoords[0] - (double) width / 2.0d
                ).floatValue();
                _y = Double.valueOf(
                        (double) startCoords[1] - (double) height / 2.0d
                ).floatValue();
                break;
            case START_COORDS_TYPE_LEFT_BOTTOM_CORNER:
                _x = startCoords[0];
                _y = startCoords[1];
                break;
            case START_COORDS_TYPE_LEFT_TOP_CORNER:
                _x = startCoords[0];
                _y = Double.valueOf((double) startCoords[1] - (double) height).floatValue();
                break;
            default:
                throw new UnsupportedOperationException("Start coords type: " + startCoordsType + " is unsupported.");
        }

        float _z = startCoords[2];
        coords = new float[]{
                _x, _y, _z,
                _x + width, _y, _z,
                _x + width, _y + height, _z,
                _x, _y + height, _z
        };
        ByteBuffer bbcvH = ByteBuffer.allocateDirect(coords.length * 4);
        bbcvH.order(ByteOrder.nativeOrder());
        verticesBuffer = bbcvH.asFloatBuffer();
        verticesBuffer.put(coords);
        verticesBuffer.position(0);

        ByteBuffer bbSVI = ByteBuffer.allocateDirect(VERTEX_INDEXES.length * 2);
        bbSVI.order(ByteOrder.nativeOrder());
        verticesIndexesBuffer = bbSVI.asShortBuffer();
        verticesIndexesBuffer.put(VERTEX_INDEXES);
        verticesIndexesBuffer.position(0);

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuf.asFloatBuffer();
        textureBuffer.put(TEXTURE_COORDS);
        textureBuffer.position(0);
    }

    private void prepareTexture() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(fontPixelSize);
        paint.setTextAlign(Paint.Align.CENTER);

        //Get text size
        Rect rect = new Rect();
        paint.getTextBounds(text, 0, text.length(), rect);

        float textBoundWidth = (float) rect.width() + (texturePadding * 2.0f);
        //Don't use rect.height() because it will has different height
        float textBoundHeight = paint.getFontMetrics().descent - paint.getFontMetrics().ascent;
        //Create bitmap and canvas
        Bitmap bitmap = Bitmap.createBitmap((int) textBoundWidth, (int) textBoundHeight, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        bitmap.eraseColor(0);

        //Glow
        if (isNeedGlow) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(4.0f);
            paint.setColor(glowColor);
            canvas.drawText(text, textBoundWidth / 2.0f, textBoundHeight - texturePadding, paint);
        }
        //Text
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        canvas.drawText(text, textBoundWidth / 2.0f, textBoundHeight - texturePadding, paint);

        //Create texture
        if (texture > 0) {
            GLES20.glDeleteTextures(1, new int[]{texture}, 0);
        }
        texture = GLHelper.loadTexture(bitmap);

        //Clean up memory
        bitmap.recycle();

        //Calculate size in world coordinates
        float aspectRatio = textBoundWidth / textBoundHeight;
        width = height * aspectRatio;
    }


    public void render(float[] modelViewProjection) {
        if (!isPrepared) {
            throw new UnsupportedOperationException("Can't render without prepare.");
        }
        Program program = ProgramHelper.getInstance().useProgram(ProgramHelper.PROGRAM_SHAPE_TEXTURED);

        GLES20.glVertexAttribPointer(program.getAttr(ProgramHelper.SHAPE_TEXTURED_ATTR_TEXTURE_COORDS),
                2, GLES20.GL_FLOAT, false, 0, textureBuffer);


        GLES20.glUniformMatrix4fv(program.getUniform(ProgramHelper.SHAPE_TEXTURED_UNIFORM_MVP),
                1, false, modelViewProjection, 0);
        GLES20.glVertexAttribPointer(program.getAttr(ProgramHelper.SHAPE_TEXTURED_ATTR_POSITION),
                3, GLES20.GL_FLOAT, false, 12, verticesBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(program.getUniform(ProgramHelper.SHAPE_TEXTURED_UNIFORM_TEXTURE), 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                VERTEX_INDEXES.length,
                GLES20.GL_UNSIGNED_SHORT,
                verticesIndexesBuffer);

        GLHelper.checkGLError("Text render");
    }
}
