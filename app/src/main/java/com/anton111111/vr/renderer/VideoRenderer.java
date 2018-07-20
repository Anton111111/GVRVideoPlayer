package com.anton111111.vr.renderer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.anton111111.gvrvideoplayer.R;
import com.google.vr.sdk.base.Eye;
import com.anton111111.vr.Quaternion;
import com.anton111111.vr.VideoFormatsSettings;
import com.anton111111.vr.GLHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 04.12.17.
 */
public class VideoRenderer implements SurfaceTexture.OnFrameAvailableListener {

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;


    private static final float[] VERTEX_COORDS = new float[]{
            //front
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,

            //right
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,

            //back
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,


            //left
            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, -1.0f,

            //upper
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,

            //bottom
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,

    };

    private static final short[] VERTEX_INDEXES = new short[]{
            0, 1, 2, 0, 2, 3, //front
            4, 5, 6, 4, 6, 7, //right
            8, 9, 10, 8, 10, 11, //back
            12, 13, 14, 12, 14, 15, //left
            16, 17, 18, 16, 18, 19, //upper
            20, 21, 22, 20, 22, 23 //bottom
    };
    private final Context context;

    private FloatBuffer vertexBuffer;
    private ShortBuffer vertexIndexesBuffer;
    private int[] textures;
    private SurfaceTexture videoTextureSurface;
    private int program;
    private GLHelper.ShaderAttributesLocations programmAttrLoc;
    private String videoFormat = VideoFormatsSettings.VIDEO_FORMAT_3D_180_SIDE_BY_SIDE;
    private boolean isVideoFrameAvailable = true;

    private boolean isVideoFormatChanged = false;
    private float[] textureTransformMatrix = new float[16];
    private float[] headTransformQuaternion = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    private float[] viewMatrix;

    public void setViewMatrix(float[] viewMatrix) {
        this.viewMatrix = viewMatrix;
    }

    public SurfaceTexture getVideoTextureSurface() {
        return videoTextureSurface;
    }


    public VideoRenderer(Context context, String videoFormat) {
        this.videoFormat = videoFormat;
        this.context = context;
        init(context);
    }

    private void init(Context context) {
        ByteBuffer bbcv = ByteBuffer.allocateDirect(VERTEX_COORDS.length * 4);
        ByteBuffer order = bbcv.order(ByteOrder.nativeOrder());
        vertexBuffer = bbcv.asFloatBuffer();
        vertexBuffer.put(VERTEX_COORDS);
        vertexBuffer.position(0);

        ByteBuffer bbSVIB = ByteBuffer.allocateDirect(VERTEX_INDEXES.length * 2);
        bbSVIB.order(ByteOrder.nativeOrder());
        vertexIndexesBuffer = bbSVIB.asShortBuffer();
        vertexIndexesBuffer.put(VERTEX_INDEXES);
        vertexIndexesBuffer.position(0);

        createProgram(context);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        videoTextureSurface = new SurfaceTexture(textures[0]);
        GLHelper.checkGLError("init VideoRenderer");

        videoTextureSurface.setOnFrameAvailableListener(this);
    }

    private void createProgram(Context context) {

        program = GLHelper.createProgram(context, R.raw.vr_video_vertex,
                VideoFormatsSettings.getFragmentId(videoFormat));

        programmAttrLoc = GLHelper.getShaderAttributeLocations(program,
                new ArrayList<String>() {{
                    add("a_Position"); //vertexBuffer coords
                }},
                new ArrayList<String>() {{
                    add("u_MVP");
                    add("u_TTM");
                    add("u_Texture");
                    add("u_TextureCordOffset");
                    add("u_Fov");
                    add("u_StereoType");
                }}
        );

        GLHelper.checkGLError("VideoRenderer create program");
    }


    public void onNewFrame(float[] headTransformQuaternion) {
        this.headTransformQuaternion = headTransformQuaternion;

        if (isVideoFormatChanged) {
            createProgram(context);
            isVideoFormatChanged = false;
        }

        synchronized (this) {
            if (isVideoFrameAvailable) {
                videoTextureSurface.updateTexImage();
                videoTextureSurface.getTransformMatrix(textureTransformMatrix);
                isVideoFrameAvailable = false;
            }
        }
    }

    public void render(Eye eye) {

        float[] projectionMatrix = eye.getPerspective(Z_NEAR, Z_FAR);
        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjection = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Quaternion.rotateM(modelMatrix, 0, headTransformQuaternion);
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

        GLES20.glUseProgram(program);
        GLES20.glEnableVertexAttribArray(programmAttrLoc.getAttr("a_Position"));
        GLES20.glVertexAttribPointer(programmAttrLoc.getAttr("a_Position"),
                3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glUniformMatrix4fv(programmAttrLoc.getUniform("u_MVP"),
                1, false, modelViewProjection, 0);

        GLES20.glUniformMatrix4fv(programmAttrLoc.getUniform("u_TTM"),
                1, false, textureTransformMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);

        GLES20.glUniform1i(programmAttrLoc.getUniform("u_Texture"), 0);

        if (programmAttrLoc.getUniform("u_TextureCordOffset") >= 0) {
            GLES20.glUniform1f(programmAttrLoc.getUniform("u_TextureCordOffset"),
                    (eye.getType() <= 1) ? 0.0f : 0.5f);
        }

        if (programmAttrLoc.getUniform("u_Fov") >= 0) {
            GLES20.glUniform1f(programmAttrLoc.getUniform("u_Fov"),
                    VideoFormatsSettings.getFov(videoFormat));
        }

        if (programmAttrLoc.getUniform("u_StereoType") >= 0) {
            GLES20.glUniform1i(programmAttrLoc.getUniform("u_StereoType"),
                    VideoFormatsSettings.getStereoType(videoFormat));
        }

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                VERTEX_INDEXES.length,
                GLES20.GL_UNSIGNED_SHORT,
                vertexIndexesBuffer);

        GLES20.glDisableVertexAttribArray(programmAttrLoc.getAttr("a_Position"));

        GLHelper.checkGLError("VideoRenderer render");

    }

    public void changeVideoFormat(String videoFormat) {
        this.videoFormat = videoFormat;
        isVideoFormatChanged = true;
    }

    public void onRendererShutdown() {
        if (textures != null) {
            GLES20.glDeleteTextures(1, textures, 0);
            textures = null;
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            isVideoFrameAvailable = true;
        }
    }

    public void onDestroy() {

    }
}
