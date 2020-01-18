package org.nagoya.video.player;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import org.jetbrains.annotations.NotNull;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A component containing simple media player controls.
 */
public class FXVideoPlayerControls extends HBox {
    private static final String COMPONENT_STYLE = "-fx-padding: 8; -fx-background-color: rgb(232, 232, 232);";
    private static final String BUTTON_STYLE = "-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: black;";

    private final MediaPlayer mediaPlayer;

    private final Label currentTimeLabel;
    private final JFXSlider timelineSlider;
    private final Label durationLabel;

    private final AtomicBoolean tracking = new AtomicBoolean();

    private Timer clockTimer;

    public FXVideoPlayerControls(@NotNull MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;

        this.currentTimeLabel = new Label(Time.formatTime(0L));

        this.timelineSlider = new JFXSlider(0, 100, 0);
        this.timelineSlider.setPadding(new Insets(8));

        this.durationLabel = new Label(Time.formatTime(0L));

        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(700);
        HBox.setHgrow(this.currentTimeLabel, Priority.NEVER);
        HBox.setHgrow(this.timelineSlider, Priority.ALWAYS);
        HBox.setHgrow(this.durationLabel, Priority.NEVER);

        box.getChildren().addAll(this.currentTimeLabel, this.timelineSlider, this.durationLabel);

        TilePane buttonsPane = new TilePane();
        buttonsPane.setPadding(new Insets(8));

        JFXButton playButton = this.createButton("Play", "play");
        JFXButton pauseButton = this.createButton("Pause", "pause");
        JFXButton stopButton = this.createButton("Stop", "stop");

        JFXSlider volSlider = new JFXSlider(0, 100, mediaPlayer.audio().volume());
        volSlider.setMinWidth(100);
        volSlider.valueProperty().addListener((obs, oV, nV) ->
                mediaPlayer.audio().setVolume(nV.intValue()));

        this.setStyle(COMPONENT_STYLE);
        this.setAlignment(Pos.CENTER);

        this.getChildren().addAll(box, buttonsPane, volSlider);
        this.setPrefHeight(50);

        buttonsPane.getChildren().addAll(playButton, pauseButton, stopButton);

        playButton.setOnAction(actionEvent -> mediaPlayer.controls().play());
        pauseButton.setOnAction(actionEvent -> mediaPlayer.controls().pause());
        stopButton.setOnAction(actionEvent -> mediaPlayer.controls().stop());

        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                FXVideoPlayerControls.this.startTimer();
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                FXVideoPlayerControls.this.stopTimer();
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                FXVideoPlayerControls.this.stopTimer();
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                FXVideoPlayerControls.this.stopTimer();
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                FXVideoPlayerControls.this.stopTimer();
            }

            @Override
            public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                Platform.runLater(() -> FXVideoPlayerControls.this.updateDuration(newLength));
            }

            @Override
            public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
                Platform.runLater(() -> FXVideoPlayerControls.this.updateSliderPosition(newPosition));
            }
        });

        this.timelineSlider.setOnMousePressed(mouseEvent -> this.beginTracking());
        this.timelineSlider.setOnMouseReleased(mouseEvent -> this.endTracking());

        this.timelineSlider.valueProperty().addListener((obs, oldValue, newValue) -> this.updateMediaPlayerPosition(newValue.floatValue() / 100));
    }

    @NotNull
    private JFXButton createButton(String name, String icon) {
        JFXButton button = new JFXButton();
        String url = String.format("/buttons/%s.png", icon);
        Image image = new Image(this.getClass().getResourceAsStream(url));
        button.setGraphic(new ImageView(image));
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        button.setStyle(BUTTON_STYLE);
        return button;
    }

    private void startTimer() {
        this.clockTimer = new Timer();
        this.clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> FXVideoPlayerControls.this.currentTimeLabel.setText(Time.formatTime(FXVideoPlayerControls.this.mediaPlayer.status().time())));
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        this.clockTimer.cancel();
    }

    private void updateDuration(long newValue) {
        this.durationLabel.setText(Time.formatTime(newValue));
    }

    private synchronized void updateMediaPlayerPosition(float newValue) {
        if (this.tracking.get()) {
            this.mediaPlayer.controls().setPosition(newValue);
        }
    }

    private synchronized void beginTracking() {
        this.tracking.set(true);
    }

    private synchronized void endTracking() {
        this.tracking.set(false);
        // This deals with the case where there was an absolute click in the timeline rather than a drag
        this.mediaPlayer.controls().setPosition((float) this.timelineSlider.getValue() / 100);
    }

    private synchronized void updateSliderPosition(float newValue) {
        if (!this.tracking.get()) {
            this.timelineSlider.setValue(newValue * 100);
        }
    }

}