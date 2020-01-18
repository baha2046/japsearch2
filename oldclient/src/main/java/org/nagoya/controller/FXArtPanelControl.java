package org.nagoya.controller;

import io.vavr.control.Option;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.Window;
import org.jetbrains.annotations.Nullable;
import org.nagoya.App;
import org.nagoya.model.MovieV2;
import org.nagoya.system.event.FXContextImp;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.system.event.CustomEventType;
import org.nagoya.view.FXArtPanelView;

public class FXArtPanelControl extends FXContextImp {

    public static final CustomEventType<Image> EVENT_POSTER_CHANGE = new CustomEventType<>("EVENT_POSTER_CHANGE");

    //public static final EventSource<Image> EVENT_POSTER_CHANGE = new EventSource<>();

    private static FXArtPanelControl instance = null;

    private final FXArtPanelView view;

    private FXArtPanelControl() {
        this.view = new FXArtPanelView();
    }

    public static FXArtPanelControl getInstance() {
        if (instance == null) {
            instance = new FXArtPanelControl();
        }
        return instance;
    }

    @Override
    public FXArtPanelView getPane() {
        return this.view;
    }

    private WritableImage getPosterImage() {
        return this.view.getPosterImage();
    }

    private void updateView(@Nullable MovieV2 inMovie) {
        if (inMovie == null) {
            this.view.clear();
            return;
        }

        // Movie inMovie = directoryEntry.getMovieData();

        //system.out.println("updateView  -- hasCover " + inMovie.hasCover());
        //system.out.println("updateView  -- hasPoster " + inMovie.hasPoster());

        if (inMovie.hasBackCover()) {
            this.view.setMovieID(inMovie.getMovieID().toString());
            this.view.setOnCoverClick((e) -> {
                ImageView view = new ImageView();

                Double maxWidth = App.getCurrentStage().map(Window::getWidth).getOrElse((double) 400) - 70;
                Double maxHeight = App.getCurrentStage().map(Window::getHeight).getOrElse((double) 400) - 170;
                inMovie.getImgBackCover().get().fitInImageView(view, Option.of(maxWidth), Option.of(maxHeight));

                DialogBuilder.create().body(view).build().show();
            });

            if (inMovie.hasFrontCover()) {
                inMovie.getImgFrontCover().peek(t -> t.getImage(this.view::showPoster));
                inMovie.getImgBackCover().peek(t -> t.getImage(this.view::showCover));
            } else {
                inMovie.getImgBackCover().peek(t -> t.getImage((image) -> {
                    this.view.showImageWithoutPoster(image, e -> this.fireEvent(EVENT_POSTER_CHANGE, this.getPosterImage()));
                    //Platform.runLater(() -> this.fireEvent(EVENT_POSTER_CHANGE, this.getPosterImage()));
                }));
            }
        } else {
            this.view.clear();
            this.view.setOnCoverClick(null);
        }
    }

    @Override
    public void registerListener() {
        //this.registerListener(FXMoviePanelControl.EVENT_MOVIE_CHANGE, e -> this.updateView((MovieV2) e.getObject()));
    }

    // @Override
    //public void executeEvent(CustomEvent e) {
    //     this.updateView((MovieV2) e.getObject());
    // }
}
