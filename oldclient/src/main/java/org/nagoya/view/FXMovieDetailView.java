package org.nagoya.view;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.nagoya.App;
import org.nagoya.GUICommon;
import org.nagoya.controller.FXFileListControl;
import org.nagoya.controller.FXMoviePanelControl;
import org.nagoya.controls.FXListView;
import org.nagoya.controls.FXScrollableLabel;
import org.nagoya.controls.FXWebViewControl;
import org.nagoya.controls.FXWebViewWindow;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.fx.scene.FXUtil;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.model.MovieV2;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.model.dataitem.Genre;
import org.nagoya.model.dataitem.MakerData;
import org.nagoya.system.FXMLController;
import org.nagoya.system.Systems;
import org.nagoya.system.database.MovieScanner;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.system.event.CustomEventSourceImp;
import org.nagoya.view.customcell.ActorListCell;
import org.nagoya.view.customcell.GenreListDarkRectCell;
import org.nagoya.view.dialog.FXMovieJavBusGridWindow;
import org.nagoya.view.dialog.FXSeedListDialog;
import org.nagoya.view.editor.FXGenresEditor;

import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class FXMovieDetailView extends FXMLController {

    @FXML
    JFXTextField txtTitle, txtYear, txtId, txtStudio, txtSet, txtDate;
    @FXML
    Pane paneActors;
    FXListView<ActorV2> lvActors;
    @FXML
    Pane paneGenres;
    private FXListView<Genre> lvGenres;
    @FXML
    JFXTextField txtDirector, txtMaker;
    @FXML
    ImageView viewFrontCover, viewBackCover;

    @FXML
    JFXButton btnGetSeed, btnCustom;

    @FXML
    Hyperlink btnGetDirector, btnGetMaker, btnGetLabel, btnGetSet;

    @FXML
    private AnchorPane basePane;

    private boolean editable;

    @FXML
    private Label labHd, labWater;

    @FXML
    private Pane paneDesc;
    private final FXScrollableLabel txtDesc;

    private Option<MovieV2> currentMovie = Option.none();

    public FXMovieDetailView() {
        this.txtDesc = new FXScrollableLabel();
    }

    static String normalize(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFKC);
    }


    public void setup(FXMoviePanelControl control, ObjectProperty<Option<MovieV2>> property) {

        property.addListener((ov, o, n) -> {
            this.currentMovie.peek(this::unbindData);
            this.currentMovie = n;
            this.currentMovie.peek(this::bindData);
        });
    }

    private void bindData(@NotNull MovieV2 movieV2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.txtDate.setText(Try.of(() -> LocalDate.parse(movieV2.getReleaseDate(), formatter).toString()).getOrElse(""));
        this.txtDate.textProperty().bindBidirectional(movieV2.dateProperty);
        this.txtTitle.textProperty().bindBidirectional(movieV2.titleProperty);
        this.txtYear.textProperty().bindBidirectional(movieV2.yearProperty);
        this.txtId.textProperty().bindBidirectional(movieV2.movieIdProperty);
        this.txtStudio.textProperty().bindBidirectional(movieV2.studioProperty);
        this.txtSet.textProperty().bindBidirectional(movieV2.seriesProperty);
        this.txtDirector.textProperty().bindBidirectional(movieV2.directorProperty);
        this.txtMaker.textProperty().bindBidirectional(movieV2.makerProperty);
        this.txtDesc.textProperty().bindBidirectional(movieV2.plotProperty);
        this.lvGenres.itemsProperty().bindBidirectional(movieV2.genresProperty);
        this.lvActors.itemsProperty().bindBidirectional(movieV2.actorProperty);

        if (movieV2.imgFrontProperty.get().isDefined()) {
            this.viewFrontCover.setViewport(null);
            movieV2.imgFrontProperty.get().peek(m -> m.getImage(this.viewFrontCover::setImage));
            movieV2.imgBackProperty.get().peek(m -> m.getImage(this.viewBackCover::setImage));
        } else {
            movieV2.imgBackProperty.get().peek(m -> m.getImage(i -> {
                FXUtil.runOnFx(()->{
                    this.viewBackCover.setImage(i);
                    this.viewFrontCover.setImage(i);
                    this.viewFrontCover.setViewport(FxThumb.getCoverCrop(i.getWidth(), i.getHeight(), movieV2.getMovieID()));

                    Image c = this.viewFrontCover.snapshot(new SnapshotParameters(), null);

                    if (movieV2.getImgFrontCover().isDefined()) {
                        movieV2.getImgFrontCover().peek(t -> t.setImage(c));
                    } else {
                        FxThumb fxThumb = FxThumb.create();
                        fxThumb.setImage(c);
                        movieV2.setImgFrontCover(Option.of(fxThumb));
                    }
                });
            }));
        }
    }

    private void unbindData(@NotNull MovieV2 movieV2) {
        this.txtDate.textProperty().unbindBidirectional(movieV2.dateProperty);
        this.txtTitle.textProperty().unbindBidirectional(movieV2.titleProperty);
        this.txtYear.textProperty().unbindBidirectional(movieV2.yearProperty);
        this.txtId.textProperty().unbindBidirectional(movieV2.movieIdProperty);
        this.txtStudio.textProperty().unbindBidirectional(movieV2.studioProperty);
        this.txtSet.textProperty().unbindBidirectional(movieV2.seriesProperty);
        this.txtDirector.textProperty().unbindBidirectional(movieV2.directorProperty);
        this.txtMaker.textProperty().unbindBidirectional(movieV2.makerProperty);
        this.txtDesc.textProperty().unbindBidirectional(movieV2.plotProperty);
        this.lvGenres.itemsProperty().unbindBidirectional(movieV2.genresProperty);
        this.lvActors.itemsProperty().unbindBidirectional(movieV2.actorProperty);

        this.viewFrontCover.setImage(null);
        this.viewBackCover.setImage(null);
        this.viewFrontCover.setViewport(null);
    }

    public Option<LocalDate> getMovieReleaseDate() {
        return Try.of(() -> LocalDate.parse(this.txtDate.getText())).toOption();
    }

    void clearCustomFlag() {
        this.labHd.setVisible(false);
        this.labWater.setVisible(false);
    }

    void setCustomFlag(boolean hd, boolean water) {
        this.labHd.setOpacity(hd ? 1.0 : 0.2);
        this.labWater.setOpacity(water ? 1.0 : 0.2);
        this.labHd.setVisible(true);
        this.labWater.setVisible(true);
    }

    public Option<MovieV2> getCurrentMovie() {
        return this.currentMovie;
    }

    @NotNull
    private List<JFXButton> getCustomButton(WebView webView) {
        List<JFXButton> buttonList = new ArrayList<>();

        JFXButton btnSearchDB = FXFactory.buttonWithBorder("  Search DB ", e -> {
            String strId = webView.getEngine().getLocation();
            GUICommon.debugMessage(strId);
            strId = strId.substring(strId.lastIndexOf('/') + 1);
            MovieScanner.searchIDDialog(strId);
        });

        buttonList.add(btnSearchDB);
        return buttonList;
    }

    public void showMakerAction(String maker) {
        String strSearch = Systems.getDirectorySystem().getDirectoryEntryFromPath(Systems.getDirectorySystem().getCurrentPath())
                .peek(d -> GUICommon.debugMessage(d.getValue().toString()))
                .flatMap(DirectoryEntry::getMakerData)
                .map(MakerData::getMakerJavUrl)
                .filter(s -> !s.equals(""))
                .getOrElse(FXMovieJavBusGridWindow.getInstance().getParsingProfile().createSearchString(maker + "&type=3"));

        if (!strSearch.equals("")) {
            FXMovieJavBusGridWindow.urlAndShow(strSearch);
        }
    }

    public void showLabelAction(String label) {
        if (!label.equals("")) {
            FXMovieJavBusGridWindow.searchAndShow(label + "&type=4");
        }
    }

    public void showSetAction(String set) {
        if (!set.equals("")) {
            FXMovieJavBusGridWindow.searchAndShow(set + "&type=&parent=ce");
        }
    }

    public void showDirectorAction(String director) {
        if (!director.equals("")) {
            FXMovieJavBusGridWindow.searchAndShow(director + "&type=2");
        }
    }

    @NotNull
    private String getJavBusLink() {
        return FXMovieJavBusGridWindow.getInstance().getParsingProfile().getUrlFromID(this.txtId.getText());
    }

    @FXML
    private void addActorAction() {
        this.lvActors.getItems().add(ActorV2.of("Actor", ActorV2.Source.NONE, "", "", ""));
    }

    @FXML
    private void showJavBusAction() {
        FXWebViewWindow.show(this.getJavBusLink(), Option.of(this::getCustomButton), Option.of(FXWebViewControl::modifyJavBus));
    }

    @FXML
    private void customAction() {
        CustomEventSourceImp.fire(FXFileListControl.EVENT_CUSTOM_FLAG_EDIT, null);
    }

    @FXML
    private void showSeedAction() {
        FXSeedListDialog.show(this.getJavBusLink(), Option.none());
    }

    @FXML
    private void showMakerAction() {
        this.showMakerAction(this.txtMaker.getText());
    }

    @FXML
    private void showLabelAction() {
        this.showLabelAction(this.txtStudio.getText());
    }

    @FXML
    private void showSetAction() {
        this.showSetAction(this.txtSet.getText());
    }

    @FXML
    private void showDirectorAction() {
        this.showDirectorAction(this.txtDirector.getText());
    }

    @FXML
    private void playVideoAction() {
        CustomEventSourceImp.fire(FXMoviePanelControl.EVENT_PLAY_VIDEO, null);
        //this.panelControl.playVideo();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.lvActors = new FXListView<>();
        this.lvActors.setCellFactory((ListView<ActorV2> l) -> new ActorListCell(this));
        this.lvActors.setBorder(false);
        this.lvActors.setBackgroundTransparent(true);
        this.lvActors.fitToPane(this.paneActors);

        this.lvGenres = new FXListView<>();
        this.lvGenres.setBackgroundTransparent(true);
        this.lvGenres.setBorder(false);
        this.lvGenres.setCellFactory((ListView<Genre> l) -> new GenreListDarkRectCell());
        this.lvGenres.fitToPane(this.paneGenres);
        //this.lvGenres.setExpanded(true);
        this.lvGenres.setOnMouseClicked((event) -> {
            if (event.getClickCount() == 2) {
                FXGenresEditor.show(this.lvGenres.getItems());
            }
        });

        this.txtDesc.setEditable(true);
        this.txtDesc.getLabel().getStyleClass().add("text_jp_14");
        this.txtDesc.fitToPane(this.paneDesc);

        this.viewFrontCover.setFitHeight(430);
        this.viewFrontCover.setPreserveRatio(true);
        this.viewFrontCover.setOnMouseClicked((e) -> {
            ImageView view = new ImageView();
            Double maxWidth = App.getCurrentStage().map(Window::getWidth).getOrElse((double) 400) - 70;
            Double maxHeight = App.getCurrentStage().map(Window::getHeight).getOrElse((double) 400) - 170;
            view.imageProperty().bind(this.viewFrontCover.imageProperty());
            //FxThumb.fitImageView(new ImageView(), () -> this.viewFrontCover.getImage(), Option.of(maxWidth), Option.of(maxHeight));
            DialogBuilder.create().body(view).build().show();
        });

        this.viewBackCover.setFitHeight(270);
        this.viewBackCover.setPreserveRatio(true);
        this.viewBackCover.setOnMouseClicked((e) -> {
            ImageView view = new ImageView();
            Double maxWidth = App.getCurrentStage().map(Window::getWidth).getOrElse((double) 400) - 70;
            Double maxHeight = App.getCurrentStage().map(Window::getHeight).getOrElse((double) 400) - 170;
            //FxThumb.fitImageView(new ImageView(), () -> this.viewBackCover.getImage(), Option.of(maxWidth), Option.of(maxHeight));
            view.imageProperty().bind(this.viewBackCover.imageProperty());
            DialogBuilder.create().body(view).build().show();
        });

        this.btnGetSet.disableProperty().bind(Systems.getWebService().isWorkingProperty());
        this.btnGetDirector.disableProperty().bind(Systems.getWebService().isWorkingProperty());
        this.btnGetLabel.disableProperty().bind(Systems.getWebService().isWorkingProperty());
        this.btnGetSeed.disableProperty().bind(Systems.getWebService().isWorkingProperty());
        this.btnGetMaker.disableProperty().bind(Systems.getWebService().isWorkingProperty());

        this.btnGetSet.visibleProperty().bind(this.txtSet.textProperty().isNotEmpty());

        this.labHd.setVisible(false);
        this.labWater.setVisible(false);

    }

}




