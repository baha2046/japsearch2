package org.nagoya.video.player;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import org.jetbrains.annotations.NotNull;
import org.nagoya.commons.GUICommon;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.callback.CallbackMedia;
import uk.co.caprica.vlcj.media.callback.seekable.RandomAccessFileMedia;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.io.File;
import java.nio.ByteBuffer;

public abstract class FXVideoPlayer {
    private static final String BLACK_BACKGROUND_STYLE = "-fx-background-color: rgb(0, 0, 0);";
    //private static final String STATUS_BACKGROUND_STYLE = "-fx-background-color: rgb(232, 232, 232); -fx-label-padding: 8 8 8 8;";

    private static final Color BLACK = new Color(0, 0, 0, 1);
    private static final Color WHITE = new Color(1, 1, 1, 1);
    private static final Font FONT = Font.font("Monospace", 16);

    /**
     * The vlcj media player factory.
     */
    protected final MediaPlayerFactory mediaPlayerFactory;

    /**
     * The vlcj direct rendering media player component.
     */
    protected final EmbeddedMediaPlayer mediaPlayer;

    /**
     * Standard pixel format for the video buffer.
     */
    private final WritablePixelFormat<ByteBuffer> pixelFormat;

    /**
     * Lightweight JavaFX canvas, the video is rendered here.
     */
    private final Canvas canvas;

    /**
     * Wrapper component for the video surface, to manage resizes properly.
     */
    private final Pane canvasPane;
    private final BorderPane borderPane;
    private final FXVideoPlayerControls controlsPane;

    //private final MenuBar menuBar;

    private int bufferWidth;
    private int bufferHeight;

    private PixelBuffer<ByteBuffer> pixelBuffer;
    private WritableImage img;
    private Rectangle2D updatedBuffer;

    private boolean showStats = true;
    private long start;
    private long frames;
    private long maxFrameTime;
    private long totalFrameTime;

    public FXVideoPlayer() {
        this.pixelFormat = PixelFormat.getByteBgraPreInstance();

        this.mediaPlayerFactory = new MediaPlayerFactory();
        this.mediaPlayer = this.mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        this.mediaPlayer.events().addMediaPlayerEventListener(new TimerHandler(this));
        this.mediaPlayer.videoSurface().set(new JavaFxVideoSurface());
        this.mediaPlayer.audio().setVolume(50);

        this.borderPane = new BorderPane();
        this.borderPane.setMinWidth(1200);
        this.borderPane.setMinHeight(630);
        this.borderPane.setStyle(BLACK_BACKGROUND_STYLE);
        this.borderPane.setVisible(false);

        this.canvas = new Canvas();
        this.canvas.setStyle(BLACK_BACKGROUND_STYLE);

        this.canvasPane = new Pane();
        this.canvasPane.setStyle(BLACK_BACKGROUND_STYLE);
        this.canvasPane.getChildren().add(this.canvas);

        this.canvas.widthProperty().bind(this.canvasPane.widthProperty());
        this.canvas.heightProperty().bind(this.canvasPane.heightProperty());

        // Listen to width/height changes to force the video surface to re-render if the media player is not currently
        // playing - this is necessary to repaint damaged regions because the repaint timer is stopped/paused while the
        // media player is not playing
        this.canvas.widthProperty().addListener(event -> {
            if (!this.mediaPlayer.status().isPlaying()) {
                this.renderFrame();
            }
        });
        this.canvas.heightProperty().addListener(event -> {
            if (!this.mediaPlayer.status().isPlaying()) {
                this.renderFrame();
            }
        });

        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(this.canvasPane);
        this.borderPane.setCenter(stackPane);

        JFXButton statusButton = new JFXButton(" Status ");
        statusButton.setOnAction((e) -> this.toggleStatsOverlay(!this.showStats));
        //JFXButton videoButton = GUICommon.getButton(" Video ", (e) -> GUICommon.showDialog(new VideoControlsPane(this.mediaPlayer)));
        JFXButton closeButton = new JFXButton(" Close ");
        closeButton.setOnAction((e) -> this.stop());

        this.controlsPane = new FXVideoPlayerControls(this.mediaPlayer);
        this.controlsPane.setSpacing(15);
        this.controlsPane.getChildren().addAll(statusButton, closeButton);

        this.borderPane.setBottom(this.controlsPane);

        //this.menuBar = createMenu(this);
        //this.borderPane.setTop(this.menuBar);

        this.mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                if (!mediaPlayer.controls().getRepeat()) {
                    FXVideoPlayer.this.showVideo(false);
                }
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                GUICommon.debugMessage("vlc error");
                FXVideoPlayer.this.showVideo(false);
            }

            @Override
            public void playing(MediaPlayer mediaPlayer) {
                FXVideoPlayer.this.showVideo(true);
                // Reset the frame stats each time the media is started (otherwise e.g. a pause would mess with the
                // stats (like FPS)
                FXVideoPlayer.this.resetStats();
            }
        });
    }

    public void load(String path) {
        GUICommon.debugMessage("MediaPlay : " + path);
        this.mediaPlayer.media().play(path);
        this.showVideo(true);
    }

    public void load(File file) {
        CallbackMedia media = new RandomAccessFileMedia(file);
        this.mediaPlayer.media().play(media);
        this.showVideo(true);
    }

    public BorderPane getPane() {
        return this.borderPane;
    }

    public void start() {
        this.mediaPlayer.controls().setRepeat(true);
        this.startTimer();
    }

    public void stop() {
        this.stopTimer();
        this.mediaPlayer.controls().stop();
        this.showVideo(false);
    }

    private class JavaFxVideoSurface extends CallbackVideoSurface {
        JavaFxVideoSurface() {
            super(new JavaFxBufferFormatCallback(), new JavaFxRenderCallback(), true, VideoSurfaceAdapters.getVideoSurfaceAdapter());
        }
    }

    private class JavaFxBufferFormatCallback implements BufferFormatCallback {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            FXVideoPlayer.this.bufferWidth = sourceWidth;
            FXVideoPlayer.this.bufferHeight = sourceHeight;

            // This does not need to be done here, but you could set the video surface size to match the native video
            // size

            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

        @Override
        public void allocatedBuffers(@NotNull ByteBuffer[] buffers) {
            // This is the new magic sauce, the native video buffer is used directly for the image buffer - there is no
            // full-frame buffer copy here
            FXVideoPlayer.this.pixelBuffer = new PixelBuffer<>(FXVideoPlayer.this.bufferWidth, FXVideoPlayer.this.bufferHeight, buffers[0], FXVideoPlayer.this.pixelFormat);
            FXVideoPlayer.this.img = new WritableImage(FXVideoPlayer.this.pixelBuffer);
            // Since for every frame the entire buffer will be updated, we can optimise by caching the result here
            FXVideoPlayer.this.updatedBuffer = new Rectangle2D(0, 0, FXVideoPlayer.this.bufferWidth, FXVideoPlayer.this.bufferHeight);
        }
    }

    // This is correct as far as it goes, but we need to use one of the timers to get smooth rendering (the timer is
    // handled by the demo sub-classes)
    private class JavaFxRenderCallback implements RenderCallback {
        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            // We only need to tell the pixel buffer which pixels were updated (in this case all of them) - the
            // pre-cached value is used
            Platform.runLater(() -> FXVideoPlayer.this.pixelBuffer.updateBuffer(pixBuf -> FXVideoPlayer.this.updatedBuffer));
        }
    }

    /**
     * This method is called for each tick of whatever timer implementation has been chosen..
     * <p>
     * Needless to say, this method should run as quickly as possible.
     */
    protected final void renderFrame() {
        this.frames++;

        long renderStart = System.currentTimeMillis();

        GraphicsContext g = this.canvas.getGraphicsContext2D();

        double width = this.canvas.getWidth();
        double height = this.canvas.getHeight();

        // The canvas must always be filled with background colour first since the rendered image may actually be
        // smaller than the full canvas - otherwise we will end up with garbage in the borders on resize
        g.setFill(new Color(0, 0, 0, 1));
        g.fillRect(0, 0, width, height);

        if (this.img != null) {
            double imageWidth = this.img.getWidth();
            double imageHeight = this.img.getHeight();

            double sx = width / imageWidth;
            double sy = height / imageHeight;

            double sf = Math.min(sx, sy);

            double scaledW = imageWidth * sf;
            double scaledH = imageHeight * sf;

            Affine ax = g.getTransform();

            g.translate(
                    (width - scaledW) / 2,
                    (height - scaledH) / 2
            );

            if (sf != 1.0) {
                g.scale(sf, sf);
            }

            // You can do this here if you want, instead of the display() callback, doesn't seem to make much difference
//            pixelBuffer.updateBuffer(pixBuf -> updatedBuffer);

            g.drawImage(this.img, 0, 0);

            double fps = (double) 1000 * this.frames / (renderStart - this.start);
            double meanFrameTime = this.totalFrameTime / (double) this.frames;

            if (this.showStats) {
                String val = String.format(
                        " Frames: %d\n" +
                                "Seconds: %d\n" +
                                "    FPS: %01.1f\n" +
                                "Maximum: %d ms\n" +
                                "   Mean: %01.3f ms",
                        this.frames, (renderStart - this.start) / 1000, fps, this.maxFrameTime, meanFrameTime
                );

                this.renderText(g, val, 10, 20);
            }

            g.setTransform(ax);
        }

        if (renderStart - this.start > 1000) {
            long renderTime = System.currentTimeMillis() - renderStart;
            this.maxFrameTime = Math.max(this.maxFrameTime, renderTime);
            this.totalFrameTime += renderTime;
        }
    }

    /**
     * A crude, but fast, renderer to draw outlined text.
     * <p>
     * Generally the approach here is faster than getting the text outline and stroking it.
     *
     * @param g    GraphicsContext
     * @param text Text
     * @param x    X
     * @param y    Y
     */
    private void renderText(@NotNull GraphicsContext g, String text, double x, double y) {
        g.setFont(FONT);
        g.setFill(BLACK);
        g.fillText(text, x - 1, y - 1);
        g.fillText(text, x + 1, y - 1);
        g.fillText(text, x - 1, y + 1);
        g.fillText(text, x + 1, y + 1);
        g.setFill(WHITE);
        g.fillText(text, x, y);
    }

    private void resetStats() {
        this.start = System.currentTimeMillis();
        this.frames = 0;
        this.maxFrameTime = 0;
        this.totalFrameTime = 0;
    }

    public void toggleStatsOverlay(boolean show) {
        this.showStats = show;
    }

    public MediaPlayer mediaPlayer() {
        return this.mediaPlayer;
    }

    private void showVideo(boolean show) {
        Platform.runLater(() -> this.borderPane.setVisible(show));
    }

    protected abstract void startTimer();

    protected abstract void pauseTimer();

    protected abstract void stopTimer();
}