package com.anton111111.vr.widgets;


import android.content.Context;

import com.anton111111.vr.GLHelper;

import java.util.ArrayList;

public class IconsList extends ArrayList<Icon> {

    private static final float ICON_SIZE = 0.07f;
    private static final float ICON_MARGIN = 0.01f;

    private float[] startCoords;


    public static float getIconSize() {
        return ICON_SIZE;
    }

    public float getIconMargin() {
        return ICON_MARGIN;
    }

    /**
     * Get current width of icons list
     *
     * @return
     */
    public float getWidth() {
        return ((float) size() * ICON_SIZE) + ((float) (size() - 1) * ICON_MARGIN);
    }

    /**
     * Calculate width for a given number of icons
     *
     * @param number
     * @return
     */
    public static float getWidth(int number) {
        return ((float) number * ICON_SIZE) + ((float) (number - 1) * ICON_MARGIN);
    }

    /**
     * Get current height of icons list
     *
     * @return
     */
    public float getHeight() {
        return getIconSize();
    }


    /**
     * @param startCoords left bottom corner of icons list
     */
    public IconsList(float[] startCoords) {
        this.startCoords = startCoords;
    }

    public Icon add(Context context, String hint, int drawableResource, Runnable callback) {
        return add(context, new String[]{hint}, new int[]{drawableResource}, callback);
    }

    public Icon add(Context context, String[] hints, int[] drawableResources, Runnable callback) {
        int[] textures = new int[drawableResources.length];
        for (int i = 0; i < drawableResources.length; i++) {
            textures[i] = GLHelper.loadTexture(context, drawableResources[i]);
        }
        Icon icon = new Icon(context, hints,
                textures,
                getNextIconCoords(),
                callback);
        if (super.add(icon)) {
            return icon;
        } else {
            return null;
        }
    }

    private float[] getNextIconCoords() {
        float _x = startCoords[0] + ((float) this.size() * getIconSize()) + ((float) this.size() * getIconMargin());
        float[] coords = new float[]{
                _x, startCoords[1], startCoords[2],
                _x + getIconSize(), startCoords[1], startCoords[2],
                _x + getIconSize(), startCoords[1] + getIconSize(), startCoords[2],
                _x, startCoords[1] + getIconSize(), startCoords[2]
        };
        return coords;
    }


    public void render(float[] modelViewProjection) {
        for (Icon icon : this) {
            icon.render(modelViewProjection);
        }

        GLHelper.checkGLError("IconsList render");
    }

}
