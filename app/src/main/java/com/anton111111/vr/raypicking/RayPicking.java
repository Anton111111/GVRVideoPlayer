package com.anton111111.vr.raypicking;

import android.opengl.GLU;

import java.util.ArrayList;


public class RayPicking {
    /**
     * Do ray picking and return coords of hit
     *
     * @param viewWidth
     * @param viewHeight
     * @param rx
     * @param ry
     * @param modelViewMatrix
     * @param projMatrix
     * @param objectCoords
     * @param objectIndexes
     * @return return coord of intersection hit area or null if no intersection
     */
    public static float[] rayPicking(int viewWidth, int viewHeight, float rx, float ry,
                                     float[] modelViewMatrix, float[] projMatrix,
                                     final float[] objectCoords, final short[] objectIndexes) {
        int[] viewport = {0, 0, viewWidth, viewHeight};
        float[] temp = new float[4];

        GLU.gluUnProject(rx, ry, 0, modelViewMatrix, 0, projMatrix, 0, viewport, 0, temp, 0);
        float[] near_xyz = new float[]{
                temp[0] / temp[3],
                temp[1] / temp[3],
                temp[2] / temp[3]
        };
        GLU.gluUnProject(rx, ry, 1, modelViewMatrix, 0, projMatrix, 0, viewport, 0, temp, 0);
        float[] far_xyz = new float[]{
                temp[0] / temp[3],
                temp[1] / temp[3],
                temp[2] / temp[3]
        };

        int coordCount = objectCoords.length;
        float[] convertedSquare = new float[coordCount];
        for (int i = 0; i < coordCount; i = i + 3) {
            convertedSquare[i] = objectCoords[i];
            convertedSquare[i + 1] = objectCoords[i + 1];
            convertedSquare[i + 2] = objectCoords[i + 2];
        }


        ArrayList<Triangle> triangles = new ArrayList<>();
        for (int i = 0; i < objectIndexes.length; i = i + 3) {
            int i1 = objectIndexes[i] * 3;
            int i2 = objectIndexes[i + 1] * 3;
            int i3 = objectIndexes[i + 2] * 3;
            triangles.add(
                    new Triangle(
                            new float[]{
                                    convertedSquare[i1],
                                    convertedSquare[i1 + 1],
                                    convertedSquare[i1 + 2]
                            },
                            new float[]{
                                    convertedSquare[i2],
                                    convertedSquare[i2 + 1],
                                    convertedSquare[i2 + 2]
                            },
                            new float[]{
                                    convertedSquare[i3],
                                    convertedSquare[i3 + 1],
                                    convertedSquare[i3 + 2]
                            }
                    )
            );
        }

        for (Triangle t : triangles) {
            float[] point = new float[3];

            int intersects = Triangle.intersectRayAndTriangle(near_xyz, far_xyz, t, point);
            if (intersects == 1 || intersects == 2) {
                return new float[]{
                        point[0], point[1], point[2]
                };
            }
        }
        return null;
    }

}
