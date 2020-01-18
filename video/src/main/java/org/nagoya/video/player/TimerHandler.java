package org.nagoya.video.player;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

/**
 * A media player event listener dedicated to managing the repaint timer.
 * <p>
 * No need to consume CPU if paused/stopped.
 */
final class TimerHandler extends MediaPlayerEventAdapter {

    private final FXVideoPlayer player;

    TimerHandler(FXVideoPlayer player) {
        this.player = player;
    }

    private void startTimer() {
        this.player.startTimer();
    }

    private void pauseTimer() {
        this.player.pauseTimer();
    }

    private void stopTimer() {
        this.player.stopTimer();
    }

    @Override
    public void playing(MediaPlayer mediaPlayer) {
        this.startTimer();
    }

    @Override
    public void paused(MediaPlayer mediaPlayer) {
        this.pauseTimer();
    }

    @Override
    public void stopped(MediaPlayer mediaPlayer) {
        this.stopTimer();
    }

    @Override
    public void finished(MediaPlayer mediaPlayer) {
        this.stopTimer();
    }

    @Override
    public void error(MediaPlayer mediaPlayer) {
        this.stopTimer();
    }
}