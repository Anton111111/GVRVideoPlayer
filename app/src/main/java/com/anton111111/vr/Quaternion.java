package com.anton111111.vr;

import android.opengl.Matrix;

public class Quaternion {

    /**
     * Rotates matrix m to quaternion
     *
     * @param m          source matrix
     * @param mOffset    index into m where the matrix starts
     * @param quaternion Quaternion
     */
    public static void rotateM(float[] m, int mOffset, float[] quaternion) {
        float[] sTemp = new float[32];
        toMatrix(sTemp, quaternion);
        Matrix.multiplyMM(sTemp, 16, m, mOffset, sTemp, 0);
        System.arraycopy(sTemp, 16, m, mOffset, 16);
    }


    /**
     * Converts Quaternion into a matrix, placing the values into the given array.
     *
     * @param matrix     The float array that holds the result {pitch,yaw,roll}.
     * @param quaternion Quaternion
     */
    public static void toMatrix(float[] matrix, float[] quaternion) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }
        if (matrix.length < 16) {
            throw new IllegalArgumentException("Not enough space to write the result");
        }
        matrix[3] = 0.0f;
        matrix[7] = 0.0f;
        matrix[11] = 0.0f;
        matrix[12] = 0.0f;
        matrix[13] = 0.0f;
        matrix[14] = 0.0f;
        matrix[15] = 1.0f;

        matrix[0] = (1.0f - (2.0f * ((quaternion[1] * quaternion[1]) + (quaternion[2] * quaternion[2]))));
        matrix[1] = (2.0f * ((quaternion[0] * quaternion[1]) - (quaternion[2] * quaternion[3])));
        matrix[2] = (2.0f * ((quaternion[0] * quaternion[2]) + (quaternion[1] * quaternion[3])));

        matrix[4] = (2.0f * ((quaternion[0] * quaternion[1]) + (quaternion[2] * quaternion[3])));
        matrix[5] = (1.0f - (2.0f * ((quaternion[0] * quaternion[0]) + (quaternion[2] * quaternion[2]))));
        matrix[6] = (2.0f * ((quaternion[1] * quaternion[2]) - (quaternion[0] * quaternion[3])));

        matrix[8] = (2.0f * ((quaternion[0] * quaternion[2]) - (quaternion[1] * quaternion[3])));
        matrix[9] = (2.0f * ((quaternion[1] * quaternion[2]) + (quaternion[0] * quaternion[3])));
        matrix[10] = (1.0f - (2.0f * ((quaternion[0] * quaternion[0]) + (quaternion[1] * quaternion[1]))));
    }


    /**
     * Multiplies quaternion to another quaternion
     *
     * @param quaternion The float array that holds the result.
     * @param lhs        The float array that holds the left-hand-side quaternion.
     * @param rhs        The float array that holds the right-hand-side quaternion.
     */
    public static void multiplyQQ(float[] quaternion, float[] lhs, float[] rhs) {
        if (lhs.length != 4) {
            throw new IllegalArgumentException("Wrong length of left hand quaternion");
        }
        if (rhs.length != 4) {
            throw new IllegalArgumentException("Wrong length of right hand quaternion");
        }

        if (quaternion.length < 4) {
            throw new IllegalArgumentException("Not enough space to write the result");
        }

        float nw = lhs[0] * rhs[3] - lhs[0] * rhs[0] - lhs[1] * rhs[1] - lhs[2] * rhs[2];
        float nx = lhs[3] * rhs[0] + lhs[0] * rhs[3] + lhs[1] * rhs[2] - lhs[2] * rhs[1];
        float ny = lhs[3] * rhs[1] + lhs[1] * rhs[3] + lhs[2] * rhs[0] - lhs[0] * rhs[2];
        quaternion[2] = lhs[3] * rhs[2] + lhs[0] * rhs[3] + lhs[0] * rhs[1] - lhs[1] * rhs[0];
        quaternion[3] = nw;
        quaternion[0] = nx;
        quaternion[1] = ny;
    }


    /**
     * Get Euler Angle (radians) from quaternion
     *
     * @param quaternion  Quaternion
     * @param eulerAngles The float array that holds the result {pitch,yaw,roll}.
     */
    public static void toEulerAngle(float[] quaternion, float[] eulerAngles) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }

        if (eulerAngles == null) {
            eulerAngles = new float[3];
        } else if (eulerAngles.length != 3) {
            throw new IllegalArgumentException("Angles array must have three elements");
        }

        float sqw = quaternion[3] * quaternion[3];
        float sqx = quaternion[0] * quaternion[0];
        float sqy = quaternion[1] * quaternion[1];
        float sqz = quaternion[2] * quaternion[2];
        float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        float test = quaternion[0] * quaternion[1] + quaternion[2] * quaternion[3];
        if (test > 0.499 * unit) { // singularity at north pole
            eulerAngles[1] = 2.0f * (float) Math.atan2(quaternion[0], quaternion[3]);
            eulerAngles[2] = (float) Math.PI / 2.0f;
            eulerAngles[0] = 0;
        } else if (test < -0.499 * unit) { // singularity at south pole
            eulerAngles[1] = -2.0f * (float) Math.atan2(quaternion[0], quaternion[3]);
            eulerAngles[2] = -(float) Math.PI / 2.0f;
            eulerAngles[0] = 0;
        } else {
            eulerAngles[1] = (float) Math.atan2(2 * quaternion[1] * quaternion[3] - 2 * quaternion[0] * quaternion[2], sqx - sqy - sqz + sqw); // roll or heading
            eulerAngles[2] = (float) Math.asin(2 * test / unit); // pitch or attitude
            eulerAngles[0] = (float) Math.atan2(2 * quaternion[0] * quaternion[3] - 2 * quaternion[1] * quaternion[2], -sqx + sqy - sqz + sqw); // yaw or bank
        }
    }


    /**
     * Sets this quaternion to the values
     * specified by an angle and a normalized axis of rotation.
     *
     * @param quaternion The float array that holds the result.
     * @param angle      the angle to rotate (in radians).
     * @param axis       the axis of rotation (already normalized).
     */
    public static void fromNormalAxisRadianAngle(float[] quaternion, float angle, float[] axis) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }

        if (axis[0] == 0 && axis[1] == 0 && axis[2] == 0) {
            quaternion[0] = 0.0f;
            quaternion[1] = 0.0f;
            quaternion[2] = 0.0f;
            quaternion[3] = 1.0f;
        } else {
            float halfAngle = 0.5f * angle;
            float sin = (float) Math.sin(halfAngle);
            quaternion[3] = (float) Math.cos(halfAngle);
            quaternion[0] = sin * axis[0];
            quaternion[1] = sin * axis[1];
            quaternion[2] = sin * axis[2];
        }
    }

    /**
     * Sets this quaternion to the values
     * specified by an angle and a normalized axis of rotation.
     *
     * @param quaternion The float array that holds the result.
     * @param angle      the angle to rotate (in degree).
     * @param axis       the axis of rotation (already normalized).
     */
    public static void fromNormalAxisDegreeAngle(float[] quaternion, float angle, float[] axis) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }

        fromNormalAxisRadianAngle(quaternion, (float) Math.toRadians(angle), axis);
    }

    /**
     * set quaternion from the Euler angles
     *
     * @param quaternion The float array that holds the result.
     * @param pitch      the Euler pitch of rotation (in radians).
     * @param yaw        the Euler yaw of rotation (in radians).
     * @param roll       the Euler roll of rotation (in radians).
     */
    public static void fromEulerAngles(float[] quaternion, float pitch, float yaw, float roll) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }
        float angle;
        float sinYaw, sinRoll, sinPitch, cosYaw, cosRoll, cosPitch;
        angle = roll * 0.5f;
        sinRoll = (float) Math.sin(angle);
        cosRoll = (float) Math.cos(angle);
        angle = yaw * 0.5f;
        sinYaw = (float) Math.sin(angle);
        cosYaw = (float) Math.cos(angle);
        angle = pitch * 0.5f;
        sinPitch = (float) Math.sin(angle);
        cosPitch = (float) Math.cos(angle);

        // variables used to reduce multiplication calls.
        float cosRollXcosPitch = cosYaw * cosRoll;
        float sinRollXsinPitch = sinYaw * sinRoll;
        float cosRollXsinPitch = cosYaw * sinRoll;
        float sinRollXcosPitch = sinYaw * cosRoll;

        quaternion[3] = (cosRollXcosPitch * cosPitch - sinRollXsinPitch * sinPitch);
        quaternion[0] = (cosRollXcosPitch * sinPitch + sinRollXsinPitch * cosPitch);
        quaternion[1] = (sinRollXcosPitch * cosPitch + cosRollXsinPitch * sinPitch);
        quaternion[2] = (cosRollXsinPitch * cosPitch - sinRollXcosPitch * sinPitch);

        normalizeLocal(quaternion);
    }

    /**
     * returns the inverse of this quaternion If this quaternion does not have an inverse (if its normal is
     * 0 or less), then null is returned.
     *
     * @param invQ       The float array that holds the result.
     * @param quaternion
     */
    public static void inverse(float[] invQ, float[] quaternion) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }
        if (invQ.length != 4) {
            throw new IllegalArgumentException("Wrong length of invQ");
        }
        float norm = norm(quaternion);
        if (norm > 0.0) {
            float invNorm = 1.0f / norm;
            invQ[0] = -quaternion[0] * invNorm;
            invQ[1] = -quaternion[1] * invNorm;
            invQ[2] = -quaternion[2] * invNorm;
            invQ[3] = quaternion[3] * invNorm;

        }
    }


    /**
     * normalizes the quaternion
     *
     * @param quaternion The float array that holds the result.
     */
    public static void normalizeLocal(float[] quaternion) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }
        float n = (float) (1.0f / Math.sqrt(norm(quaternion)));
        quaternion[0] *= n;
        quaternion[1] *= n;
        quaternion[2] *= n;
        quaternion[3] *= n;
    }

    /**
     * returns the norm of quaternion. This is the dot
     * product of this quaternion with itself.
     *
     * @return the norm of the quaternion.
     */
    public static float norm(float[] quaternion) {
        if (quaternion.length != 4) {
            throw new IllegalArgumentException("Wrong length of quaternion");
        }
        return quaternion[3] * quaternion[3] + quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1] + quaternion[2] * quaternion[2];
    }

}