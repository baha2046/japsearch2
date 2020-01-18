package org.nagoya.view.dialog;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.system.Systems;
import org.nagoya.video.player.VLCPlayerNano;

import java.util.ArrayList;
import java.util.List;

public class FXVideoViewer extends BorderPane {
    private final MediaPlayer mp;
    private boolean stopRequested = false;
    private boolean atEndOfMedia = false;
    private Duration duration;
    private Slider timeSlider;
    private Label playTime;
    private Slider volumeSlider;

    public static void show(@NotNull String uri) {
        VLCPlayerNano.getInstance().load(uri);
    }

    public static void showWithoutVlc(@NotNull String uri) {
        Option<FXVideoViewer> player = Option.of(uri)
                .map(Media::new)
                .map(MediaPlayer::new)
                .map(p -> Tuple.of(p, new MediaView(p)))
                .map(t -> t.map2(FXVideoViewer::setView))
                .map(FXVideoViewer::new);

        player.peek(pane -> {
            JFXDialog dialog = Systems.getDialogPool().takeDialog();
            JFXButton closeBtn = FXFactory.button(" Close ", (e) -> {
                dialog.close();
                pane.shutdown();
            });
            List<Node> buttonList = new ArrayList<>();
            buttonList.add(closeBtn);

            Systems.getDialogPool().showDialog(dialog, null,
                    pane, buttonList, false);
        });
    }

    @NotNull
    @Contract("_ -> param1")
    private static MediaView setView(@NotNull MediaView view) {
        //change width and height to fit video
       /* DoubleProperty width = viewer.fitWidthProperty();
        DoubleProperty height = viewer.fitHeightProperty();
        width.bind(Bindings.selectDouble(viewer.sceneProperty(), "width"));
        height.bind(Bindings.selectDouble(viewer.sceneProperty(), "height"));
        */
        view.setFitHeight(540);
        view.setPreserveRatio(true);
        return view;
    }

    private FXVideoViewer(@NotNull Tuple2<MediaPlayer, MediaView> media) {
        this(media._1, media._2);
    }

    private FXVideoViewer(final MediaPlayer mp, MediaView view) {
        this.mp = mp;
        this.setStyle("-fx-background-color: #bfc2c7;");
        GridPane mvPane = new GridPane();
        mvPane.setAlignment(Pos.CENTER);
        mvPane.getChildren().add(view);
        mvPane.setMinWidth(1200);
        mvPane.setStyle("-fx-background-color: black;");
        this.setCenter(mvPane);
        this.init();
    }

    private void init() {
        HBox mediaBar = new HBox();
        mediaBar.setAlignment(Pos.CENTER);
        mediaBar.setPadding(new Insets(5, 10, 5, 10));
        BorderPane.setAlignment(mediaBar, Pos.CENTER);

        final Button playButton = new Button(">");
        mediaBar.getChildren().add(playButton);
        this.setBottom(mediaBar);

        // Add spacer
        Label spacer = new Label("   ");
        mediaBar.getChildren().add(spacer);

        // Add Time label
        Label timeLabel = new Label("Time: ");
        mediaBar.getChildren().add(timeLabel);

        // Add time slider
        this.timeSlider = new Slider();
        HBox.setHgrow(this.timeSlider, Priority.ALWAYS);
        this.timeSlider.setMinWidth(50);
        this.timeSlider.setMaxWidth(Double.MAX_VALUE);
        mediaBar.getChildren().add(this.timeSlider);

        // Add Play label
        this.playTime = new Label();
        this.playTime.setPrefWidth(130);
        this.playTime.setMinWidth(50);
        mediaBar.getChildren().add(this.playTime);

        // Add the volume label
        Label volumeLabel = new Label("Vol: ");
        mediaBar.getChildren().add(volumeLabel);

        // Add Volume slider
        this.volumeSlider = new Slider();
        this.volumeSlider.setPrefWidth(70);
        this.volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        this.volumeSlider.setMinWidth(30);

        mediaBar.getChildren().add(this.volumeSlider);

        playButton.setOnAction(e -> {
            MediaPlayer.Status status = this.mp.getStatus();

            if (status == MediaPlayer.Status.UNKNOWN || status == MediaPlayer.Status.HALTED) {
                // don't do anything in these states
                return;
            }

            if (status == MediaPlayer.Status.PAUSED
                    || status == MediaPlayer.Status.READY
                    || status == MediaPlayer.Status.STOPPED) {
                // rewind the movie if we're sitting at the end
                if (this.atEndOfMedia) {
                    this.mp.seek(this.mp.getStartTime());
                    this.atEndOfMedia = false;
                }
                this.mp.play();
            } else {
                this.mp.pause();
            }
        });

        this.mp.currentTimeProperty().addListener(ov -> this.updateValues());

        this.mp.setOnPlaying(() -> {
            if (this.stopRequested) {
                this.mp.pause();
                this.stopRequested = false;
            } else {
                playButton.setText("||");
            }
        });

        this.mp.setOnPaused(() -> {
            playButton.setText(">");
        });

        this.mp.setOnReady(() -> {
            this.duration = this.mp.getMedia().getDuration();
            this.updateValues();
        });

        this.mp.setCycleCount(1);
        this.mp.setOnEndOfMedia(() -> {
            playButton.setText(">");
            this.stopRequested = true;
            this.atEndOfMedia = true;
        });

        this.timeSlider.valueProperty().addListener(ov -> {
            if (this.timeSlider.isValueChanging()) {
                // multiply duration by percentage calculated by slider position
                this.mp.seek(this.duration.multiply(this.timeSlider.getValue() / 100.0));
            }
        });

        this.volumeSlider.valueProperty().addListener(ov -> {
            if (this.volumeSlider.isValueChanging()) {
                this.mp.setVolume(this.volumeSlider.getValue() / 100.0);
            }
        });
    }

    public void shutdown() {
        this.mp.stop();
        this.mp.dispose();
    }

    private void updateValues() {
        if (this.playTime != null && this.timeSlider != null && this.volumeSlider != null) {
            Platform.runLater(() -> {
                Duration currentTime = this.mp.getCurrentTime();
                this.playTime.setText(formatTime(currentTime, this.duration));
                this.timeSlider.setDisable(this.duration.isUnknown());
                if (!this.timeSlider.isDisabled()
                        && this.duration.greaterThan(Duration.ZERO)
                        && !this.timeSlider.isValueChanging()) {
                    this.timeSlider.setValue(currentTime.divide(this.duration.toMillis()).toMillis()
                            * 100.0);
                }
                if (!this.volumeSlider.isValueChanging()) {
                    this.volumeSlider.setValue((int) Math.round(this.mp.getVolume()
                            * 100));
                }
            });
        }
    }

    private static String formatTime(@NotNull Duration elapsed, Duration duration) {
        int intElapsed = (int) Math.floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed /*- elapsedHours * 60 * 60*/ - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) Math.floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration /*- durationHours * 60 * 60*/ - durationMinutes * 60;
            if (durationHours > 0) {
                return String.format("%d:%02d:%02d/%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return String.format("%02d:%02d/%02d:%02d",
                        elapsedMinutes, elapsedSeconds, durationMinutes,
                        durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds);
            } else {
                return String.format("%02d:%02d", elapsedMinutes,
                        elapsedSeconds);
            }
        }
    }
}
