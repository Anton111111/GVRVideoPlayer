package com.anton111111.player;


/**
 * Created by Anton Potekhin (Anton.Potekhin@gmail.com) on 18.01.17.
 */
public interface MediaPlayerControl {

    void start();

    void pause();

    long getDuration();

    long getCurrentPosition();

    void seekTo(long pos);

    boolean isPlaying();

    boolean isEnded();

    int getBufferPercentage();

    boolean canPause();

    boolean canSeekBackward();

    boolean canSeekForward();

    int getAudioSessionId();
}
