package com.anton111111.util;

import java.util.Locale;


public class StringUtil {

    /**
     * Convert milliseconds to string
     *
     * @param timeMs
     * @return
     */
    public static String millisecondsToString(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Convert seconds to string
     *
     * @param seconds
     * @return
     */
    public static String secondsToString(int seconds) {

        int _seconds = seconds % 60;
        int minutes = (seconds / 60) % 60;
        int hours = seconds / 3600;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, _seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, _seconds);
        }
    }

    /**
     * Check is string null or empty
     *
     * @param string
     * @return
     */
    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

}
