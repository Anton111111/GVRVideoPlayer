package com.anton111111.vr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;


import com.anton111111.vr.program.Program;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by Anton Potekhin on 23.11.2017.
 */
public class GLHelper {

    private static final String TAG = "GLHelper";

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type  The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    public static int loadGLShader(Context context, int type, int resId) {
        String code = readRawTextFile(context, resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {

            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader. (" + resId + ":+" + type + ")");
        }

        return shader;
    }

    /**
     * Load texture from Resource
     *
     * @param context
     * @param resourceId
     * @return
     */
    public static int loadTexture(final Context context, final int resourceId) {
        int texture;

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   // No pre-scaling

        // Read in the resource
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        texture = loadTexture(bitmap);

        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle();

        if (texture == 0) {
            throw new RuntimeException("Error loading texture from resource.");
        }

        return texture;
    }

    /**
     * Load texture from bitmap
     *
     * @param bitmap
     * @return
     */
    public static int loadTexture(Bitmap bitmap) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture from bitmap.");
        }
        checkGLError("GLHelper loadTexture from bitmap");
        return textureHandle[0];
    }

    /**
     * Create program from vr_video_vertex and vr_video_fragment shader resource id
     *
     * @param context
     * @param vertexShaderResourceId   The vr_video_vertex resource ID of the raw text file about to be turned into a shader.
     * @param fragmentShaderResourceId The vr_video_fragment resource ID of the raw text file about to be turned into a shader.
     * @return
     */
    public static int createProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        int program = createProgram(loadGLShader(context, GLES20.GL_VERTEX_SHADER, vertexShaderResourceId),
                loadGLShader(context, GLES20.GL_FRAGMENT_SHADER, fragmentShaderResourceId));
        checkGLError("GLHelper createProgram");
        return program;
    }


    /**
     * Create Program from vr_video_vertex and vr_video_fragment shader resource id and attrs lists
     *
     * @param context
     * @param vertexShaderResourceId   The vr_video_vertex resource ID of the raw text file about to be turned into a shader.
     * @param fragmentShaderResourceId The vr_video_fragment resource ID of the raw text file about to be turned into a shader.
     * @param _attrs                   Attributes
     * @param _uniforms                Uniforms
     * @return
     */
    public static Program createProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId,
                                        List<String> _attrs, List<String> _uniforms) {
        int program = createProgram(loadGLShader(context, GLES20.GL_VERTEX_SHADER, vertexShaderResourceId),
                loadGLShader(context, GLES20.GL_FRAGMENT_SHADER, fragmentShaderResourceId));
        ShaderAttributesLocations shaderAttributeLocations = getShaderAttributeLocations(program, _attrs, _uniforms);
        checkGLError("GLHelper createProgram");
        return new Program(program, shaderAttributeLocations);
    }

    /**
     * Create program from vr_video_vertex and vr_video_fragment shader ids
     *
     * @param vertexShaderId
     * @param fragmentShaderId
     * @return
     */
    public static int createProgram(int vertexShaderId, int fragmentShaderId) {
        final int programId = GLES20.glCreateProgram();
        if (programId == 0) {
            return 0;
        }
        GLES20.glAttachShader(programId, vertexShaderId);
        GLES20.glAttachShader(programId, fragmentShaderId);
        GLES20.glLinkProgram(programId);
        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(programId);
            return 0;
        }
        return programId;
    }


    public static ShaderAttributesLocations getShaderAttributeLocations(int programm, List<String> _attrs, List<String> _uniforms) {
        return new ShaderAttributesLocations(programm, _attrs, _uniforms);
    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     */
    public static void checkGLError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }


    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    public static String readRawTextFile(Context context, int resId) {
        InputStream inputStream = context.getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static int getMaxTextureSize() {

        int[] maxSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        if (maxSize[0] > 0) {
            return maxSize[0];
        }

        EGLDisplay dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] vers = new int[2];
        EGL14.eglInitialize(dpy, vers, 0, vers, 1);
        //Next, we need to find a config. Since we won't use this context for rendering, the exact attributes aren't very critical:
        int[] configAttr = {
                EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
                EGL14.EGL_LEVEL, 0,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        EGL14.eglChooseConfig(dpy, configAttr, 0,
                configs, 0, 1, numConfig, 0);
        if (numConfig[0] == 0) {
            // TROUBLE! No config found.
        }
        EGLConfig config = configs[0];
        //To make a context current, which we will need later, you need a rendering surface, even if you don't actually plan to render. To satisfy this requirement, create a small offscreen (Pbuffer) surface:

        int[] surfAttr = {
                EGL14.EGL_WIDTH, 64,
                EGL14.EGL_HEIGHT, 64,
                EGL14.EGL_NONE
        };
        EGLSurface surf = EGL14.eglCreatePbufferSurface(dpy, config, surfAttr, 0);
        //Next, create the context:

        int[] ctxAttrib = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        EGLContext ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0);
        //Ready to make the context current now:

        EGL14.eglMakeCurrent(dpy, surf, surf, ctx);
        //If all of the above succeeded (error checking was omitted), you can make your OpenGL calls now:

        maxSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        //Once you're all done, you can tear down everything:

        EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroySurface(dpy, surf);
        EGL14.eglDestroyContext(dpy, ctx);
        EGL14.eglTerminate(dpy);
        return maxSize[0];
    }


    public static class ShaderAttributesLocations {
        private HashMap<String, Integer> attrs = new HashMap<>();
        private HashMap<String, Integer> uniforms = new HashMap<>();

        public Set<String> getAttrs() {
            return attrs.keySet();
        }

        public ShaderAttributesLocations(int programm, List<String> _attrs, List<String> _uniforms) {
            for (String attr : _attrs) {
                attrs.put(attr, GLES20.glGetAttribLocation(programm, attr));
            }
            for (String uniform : _uniforms) {
                uniforms.put(uniform, GLES20.glGetUniformLocation(programm, uniform));
            }
        }

        public int getAttr(String name) {
            if (attrs.containsKey(name)) {
                return attrs.get(name);
            }
            throw new IllegalArgumentException("Location for attribute `" + name + "` is not found!");
        }

        public int getUniform(String name) {
            if (uniforms.containsKey(name)) {
                return uniforms.get(name);
            }
            throw new IllegalArgumentException("Location for uniform `" + name + "` is not found!");
        }
    }


}
