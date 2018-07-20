package com.anton111111.vr.program;

import android.content.Context;
import android.opengl.GLES20;

import com.anton111111.gvrvideoplayer.R;
import com.anton111111.vr.GLHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 08.12.17.
 */
public class ProgramHelper {

    public static final String PROGRAM_SHAPE_TEXTURED = "PROGRAM_SHAPE_TEXTURED";
    public static final String PROGRAM_SHAPE_COLORED = "PROGRAM_SHAPE_COLORED";
    public static final String PROGRAM_CIRCLE_COLORED = "PROGRAM_CIRCLE_COLORED";
    public static final String PROGRAM_PROGRESS_CIRCLE = "PROGRAM_PROGRESS_CIRCLE";

    public static final String SHAPE_COLORED_ATTR_POSITION = "a_Position";
    public static final String SHAPE_COLORED_UNIFORM_MVP = "u_MVP";
    public static final String SHAPE_COLORED_UNIFORM_COLOR = "u_Color";

    public static final String CIRCLE_COLORED_ATTR_POSITION = "a_Position";
    public static final String CIRCLE_COLORED_UNIFORM_MVP = "u_MVP";
    public static final String CIRCLE_COLORED_UNIFORM_COLOR = "u_Color";
    public static final String CIRCLE_COLORED_UNIFORM_RADIUS = "u_Radius";
    public static final String CIRCLE_COLORED_UNIFORM_THICKNESS = "u_Thickness";
    public static final String CIRCLE_COLORED_UNIFORM_CENTER = "u_Center";


    public static final String SHAPE_TEXTURED_ATTR_POSITION = "a_Position";
    public static final String SHAPE_TEXTURED_ATTR_TEXTURE_COORDS = "a_TexCoordinate";
    public static final String SHAPE_TEXTURED_UNIFORM_MVP = "u_MVP";
    public static final String SHAPE_TEXTURED_UNIFORM_TEXTURE = "u_Texture";

    public static final String PROGRESS_CIRCLE_ATTR_POSITION = "a_Position";
    public static final String PROGRESS_CIRCLE_UNIFORM_MVP = "u_MVP";
    public static final String PROGRESS_CIRCLE_UNIFORM_COLOR = "u_Color";
    public static final String PROGRESS_CIRCLE_UNIFORM_RADIUS = "u_Radius";
    public static final String PROGRESS_CIRCLE_UNIFORM_THICKNESS = "u_Thickness";
    public static final String PROGRESS_CIRCLE_UNIFORM_PERCENT = "u_Percent";
    public static final String PROGRESS_CIRCLE_UNIFORM_CENTER = "u_Center";


    private static ProgramHelper instance;
    private HashMap<String, Program> programs = new HashMap<>();
    private HashMap<Integer, String> programsMap = new HashMap<>();

    private volatile ReentrantLock programLock = new ReentrantLock();


    public static ProgramHelper getInstance() {
        if (instance == null) {
            instance = new ProgramHelper();
        }
        return instance;
    }


    public void clean() {
        try {
            programLock.lock();
            programs = new HashMap<>();
            programsMap = new HashMap<>();
        } finally {
            programLock.unlock();
        }
    }

    /**
     * Create program is need
     *
     * @param context
     * @param key
     * @param vertexShaderId
     * @param fragmentShaderId
     * @return
     */
    public Program createProgram(Context context, String key, int vertexShaderId, int fragmentShaderId,
                                 List<String> _attrs, List<String> _uniforms) {
        try {
            programLock.lock();

            if (!programs.containsKey(key)) {
                Program program = GLHelper.createProgram(context, vertexShaderId,
                        fragmentShaderId,
                        _attrs, _uniforms);
                programs.put(key, program);
                programsMap.put(program.getProgramId(), key);
                return program;
            } else {
                return programs.get(key);
            }
        } finally {
            programLock.unlock();
        }
    }


    public int getCurrentProgramId() {
        int[] cp = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, cp, 0);
        return cp[0];
    }

    public Program useProgram(String key) {
        try {
            programLock.lock();

            if (!programs.containsKey(key)) {
                throw new IllegalArgumentException("Program `" + key + "` is not created.");
            }
            Program p = programs.get(key);
            int currentProgramId = getCurrentProgramId();
            if (currentProgramId != p.getProgramId()) {
                Program oldP = getProgram(currentProgramId);
                if (oldP != null) {
                    GLHelper.ShaderAttributesLocations sAL = oldP.getShaderAttributeLocations();
                    Set<String> attrs = sAL.getAttrs();
                    for (String attr : attrs) {
                        GLES20.glDisableVertexAttribArray(sAL.getAttr(attr));
                    }
                    GLHelper.checkGLError("useProgram disable old attrs");
                }

                GLES20.glUseProgram(p.getProgramId());
                GLHelper.checkGLError("useProgram use program");
                GLHelper.ShaderAttributesLocations shaderAttributeLocations = p.getShaderAttributeLocations();
                Set<String> attrs = shaderAttributeLocations.getAttrs();
                for (String attr : attrs) {
                    GLES20.glEnableVertexAttribArray(shaderAttributeLocations.getAttr(attr));
                }
                GLHelper.checkGLError("useProgram enable new attrs");
            }
            return p;
        } finally {
            programLock.unlock();
        }
    }

    public Program getProgram(String key) {
        if (programs.containsKey(key)) {
            return programs.get(key);
        } else {
            return null;
        }
    }

    public Program getProgram(int id) {
        if (programsMap.containsKey(id)) {
            return programs.get(programsMap.get(id));
        } else {
            return null;
        }
    }


    public static void initShapeColoredProgram(Context context) {
        ProgramHelper.getInstance().createProgram(context, ProgramHelper.PROGRAM_SHAPE_COLORED,
                R.raw.shape_colored_vertex,
                R.raw.shape_colored_fragment,
                new ArrayList<String>() {{
                    add(SHAPE_COLORED_ATTR_POSITION);
                }},
                new ArrayList<String>() {{
                    add(SHAPE_COLORED_UNIFORM_MVP);
                    add(SHAPE_COLORED_UNIFORM_COLOR);
                }}
        );
    }

    public static void initShapeTexturedProgram(Context context) {
        ProgramHelper.getInstance().createProgram(context, ProgramHelper.PROGRAM_SHAPE_TEXTURED,
                R.raw.shape_textured_vertex,
                R.raw.shape_textured_fragment,
                new ArrayList<String>() {{
                    add(SHAPE_TEXTURED_ATTR_POSITION);
                    add(SHAPE_TEXTURED_ATTR_TEXTURE_COORDS);
                }},
                new ArrayList<String>() {{
                    add(SHAPE_TEXTURED_UNIFORM_MVP);
                    add(SHAPE_TEXTURED_UNIFORM_TEXTURE);
                }}
        );
    }

    public static void initCircleColoredProgram(Context context) {
        ProgramHelper.getInstance().createProgram(context, ProgramHelper.PROGRAM_CIRCLE_COLORED,
                R.raw.circle_colored_vertex,
                R.raw.circle_colored_fragment,
                new ArrayList<String>() {{
                    add(CIRCLE_COLORED_ATTR_POSITION);
                }},
                new ArrayList<String>() {{
                    add(CIRCLE_COLORED_UNIFORM_MVP);
                    add(CIRCLE_COLORED_UNIFORM_COLOR);
                    add(CIRCLE_COLORED_UNIFORM_RADIUS);
                    add(CIRCLE_COLORED_UNIFORM_THICKNESS);
                    add(CIRCLE_COLORED_UNIFORM_CENTER);
                }}
        );
    }


    public static void initProgressCircleProgram(Context context) {
        ProgramHelper.getInstance().createProgram(context, ProgramHelper.PROGRAM_PROGRESS_CIRCLE,
                R.raw.progress_circle_vertex,
                R.raw.progress_circle_fragment,
                new ArrayList<String>() {{
                    add(PROGRESS_CIRCLE_ATTR_POSITION);
                }},
                new ArrayList<String>() {{
                    add(PROGRESS_CIRCLE_UNIFORM_MVP);
                    add(PROGRESS_CIRCLE_UNIFORM_COLOR);
                    add(PROGRESS_CIRCLE_UNIFORM_RADIUS);
                    add(PROGRESS_CIRCLE_UNIFORM_THICKNESS);
                    add(PROGRESS_CIRCLE_UNIFORM_PERCENT);
                    add(PROGRESS_CIRCLE_UNIFORM_CENTER);
                }}
        );
    }


}
