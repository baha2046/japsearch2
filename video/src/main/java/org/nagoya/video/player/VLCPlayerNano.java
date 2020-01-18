package org.nagoya.video.player;

import javafx.application.Platform;

public class VLCPlayerNano extends FXVideoPlayer {
    private static VLCPlayerNano INSTANCE = null;

    public static VLCPlayerNano getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VLCPlayerNano();
        }
        return INSTANCE;
    }

    private static final double FPS = 60.0;
    private final NanoTimer nanoTimer = new NanoTimer(1000.0 / FPS) {
        @Override
        protected void onSucceeded() {
            VLCPlayerNano.this.renderFrame();
        }
    };

    public VLCPlayerNano() {
    }

    @Override
    protected void startTimer() {
        Platform.runLater(() -> {
            if (!this.nanoTimer.isRunning()) {
                this.nanoTimer.reset();
                this.nanoTimer.start();
            }
        });
    }

    @Override
    protected void pauseTimer() {
        Platform.runLater(() -> {
            if (this.nanoTimer.isRunning()) {
                this.nanoTimer.cancel();
            }
        });
    }

    @Override
    protected void stopTimer() {
        Platform.runLater(() -> {
            if (this.nanoTimer.isRunning()) {
                this.nanoTimer.cancel();
            }
        });
    }

    public void shutdown() {
        this.mediaPlayer.release();
        this.mediaPlayerFactory.release();
    }

}
