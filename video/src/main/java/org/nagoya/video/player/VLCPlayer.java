package org.nagoya.video.player;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

public class VLCPlayer extends FXVideoPlayer {
    private static VLCPlayer INSTANCE = null;

    public static VLCPlayer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VLCPlayer();
        }
        return INSTANCE;
    }

    private static final double FPS = 60.0;
    private final Timeline timeline;

    public VLCPlayer() {
        this.timeline = new Timeline();
        this.timeline.setCycleCount(Timeline.INDEFINITE);
        double duration = 1000.0 / FPS;
        EventHandler<ActionEvent> nextFrameHandler = t -> this.renderFrame();
        this.timeline.getKeyFrames().add(new KeyFrame(Duration.millis(duration), nextFrameHandler));
    }

    @Override
    protected void startTimer() {
        if (this.timeline.getStatus() != Animation.Status.RUNNING) {
            this.timeline.play();
        }
    }

    @Override
    protected void pauseTimer() {
        if (this.timeline.getStatus() != Animation.Status.PAUSED) {
            this.timeline.pause();
        }
    }

    @Override
    protected void stopTimer() {
        if (this.timeline.getStatus() != Animation.Status.STOPPED) {
            this.timeline.stop();
        }
    }

    public void shutdown() {
        this.mediaPlayer.release();
        this.mediaPlayerFactory.release();
    }
}
