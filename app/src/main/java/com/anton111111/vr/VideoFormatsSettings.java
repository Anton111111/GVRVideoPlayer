package com.anton111111.vr;

import com.anton111111.gvrvideoplayer.R;

import java.util.HashMap;

/**
 * Created by Anton Potekhin on 30.11.2017.
 */

public class VideoFormatsSettings {

    public static final String VIDEO_FORMAT_3D_180_SIDE_BY_SIDE = "3D180SBS";
    public static final String VIDEO_FORMAT_3D_270_SIDE_BY_SIDE = "3D270SBS";
    public static final String VIDEO_FORMAT_3D_180_OVER_BY_UNDER = "3D180OU";
    public static final String VIDEO_FORMAT_3D_360_SIDE_BY_SIDE = "3D360SBS";
    public static final String VIDEO_FORMAT_3D_360_OVER_UNDER = "3D360OU";
    public static final String VIDEO_FORMAT_2D_360 = "2D360";
    public static final String VIDEO_FORMAT_2D_180 = "2D180";

    public static final int STEREO_TYPE_MONO = 0;
    public static final int STEREO_TYPE_SIDE_BY_SIDE = 1;
    public static final int STEREO_TYPE_OVER_UNDER = 2;

    private static HashMap<String, Integer> fragments = new HashMap<String, Integer>() {{
        put(VIDEO_FORMAT_3D_180_SIDE_BY_SIDE, R.raw.vr_video_fragment);
        put(VIDEO_FORMAT_3D_270_SIDE_BY_SIDE, R.raw.vr_video_fragment);
        put(VIDEO_FORMAT_3D_180_OVER_BY_UNDER, R.raw.vr_video_fragment);
        put(VIDEO_FORMAT_3D_360_SIDE_BY_SIDE, R.raw.vr_video_fragment);
        put(VIDEO_FORMAT_3D_360_OVER_UNDER, R.raw.vr_video_fragment);
        put(VIDEO_FORMAT_2D_360, R.raw.vr_video_fragment);
        put(VIDEO_FORMAT_2D_180, R.raw.vr_video_fragment);
    }};

    private static HashMap<String, Float> fovs = new HashMap<String, Float>() {{
        put(VIDEO_FORMAT_3D_180_SIDE_BY_SIDE, (float) Math.toRadians(180));
        put(VIDEO_FORMAT_3D_270_SIDE_BY_SIDE, (float) Math.toRadians(270));
        put(VIDEO_FORMAT_3D_180_OVER_BY_UNDER, (float) Math.toRadians(180));
        put(VIDEO_FORMAT_3D_360_SIDE_BY_SIDE, (float) Math.toRadians(360));
        put(VIDEO_FORMAT_3D_360_OVER_UNDER, (float) Math.toRadians(360));
        put(VIDEO_FORMAT_2D_360, (float) Math.toRadians(360));
        put(VIDEO_FORMAT_2D_180, (float) Math.toRadians(180));
    }};


    private static HashMap<String, Integer> stereoType = new HashMap<String, Integer>() {{
        put(VIDEO_FORMAT_3D_180_SIDE_BY_SIDE, STEREO_TYPE_SIDE_BY_SIDE);
        put(VIDEO_FORMAT_3D_270_SIDE_BY_SIDE, STEREO_TYPE_SIDE_BY_SIDE);
        put(VIDEO_FORMAT_3D_180_OVER_BY_UNDER, STEREO_TYPE_OVER_UNDER);
        put(VIDEO_FORMAT_3D_360_SIDE_BY_SIDE, STEREO_TYPE_SIDE_BY_SIDE);
        put(VIDEO_FORMAT_3D_360_OVER_UNDER, STEREO_TYPE_OVER_UNDER);
        put(VIDEO_FORMAT_2D_360, STEREO_TYPE_MONO);
        put(VIDEO_FORMAT_2D_180, STEREO_TYPE_MONO);
    }};

    public static int getFragmentId(String videoFormat) {
        if (!fragments.containsKey(videoFormat))
            throw new IllegalArgumentException("Wrong video format in getFragmentId: " + videoFormat);
        return fragments.get(videoFormat);
    }

    public static float getFov(String videoFormat) {
        if (!fovs.containsKey(videoFormat))
            throw new IllegalArgumentException("Wrong video format in getFov: " + videoFormat);
        return fovs.get(videoFormat);
    }

    public static int getStereoType(String videoFormat) {
        if (!stereoType.containsKey(videoFormat))
            throw new IllegalArgumentException("Wrong video format in getStereoType: " + videoFormat);
        return stereoType.get(videoFormat);
    }

}
