package org.nagoya.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.vfs2.FileObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controller.siteparsingprofile.SiteParsingProfile;
import org.nagoya.controller.siteparsingprofile.specific.ArzonParsingProfile;
import org.nagoya.controller.siteparsingprofile.specific.DmmParsingProfile;
import org.nagoya.controller.siteparsingprofile.specific.DugaParsingProfile;
import org.nagoya.controller.siteparsingprofile.specific.JavBusParsingProfile;
import org.nagoya.controls.FXImageViewerWindow;
import org.nagoya.controls.FXListView;
import org.nagoya.io.LocalVFS;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.model.MovieFolder;
import org.nagoya.model.MovieV2;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.system.event.CustomEventType;
import org.nagoya.system.event.FXContextImp;
import org.nagoya.view.FXMovieDetailCompareView;
import org.nagoya.view.FXMoviePanelView;
import org.nagoya.view.dialog.FXVideoViewer;
import org.nagoya.view.editor.FXRenameFormatEditor;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.function.Consumer;

public class FXMoviePanelControl extends FXContextImp {

    public static final CustomEventType<Runnable> EVENT_MOVIE_CHANGE = new CustomEventType<>("EVENT_MOVIE_CHANGE");
    public static final CustomEventType<Runnable> EVENT_MOVIE_EMPTY = new CustomEventType<>("EVENT_MOVIE_EMPTY");
    public static final CustomEventType<Boolean> EVENT_ACTION_SCRAPE_MOVIE = new CustomEventType<>("EVENT_ACTION_SCRAPE_MOVIE");
    public static final CustomEventType<String> EVENT_ACTION_SCRAPE_DETAIL = new CustomEventType<>("EVENT_ACTION_SCRAPE_DETAIL");
    public static final CustomEventType<MovieV2> EVENT_ACTION_SAVE_MOVIE = new CustomEventType<>("EVENT_ACTION_SAVE_MOVIE");
    public static final CustomEventType<Void> EVENT_SHOW_EXTRA_ART = new CustomEventType<>("EVENT_SHOW_EXTRA_ART");

    public static final CustomEventType<Void> EVENT_PLAY_VIDEO = new CustomEventType<>("EVENT_PLAY_VIDEO");


    private static FXMoviePanelControl instance = null;

    private final FXMoviePanelView moviePanel;
    //private DirectoryEntry selectedEntry;
    private MovieV2 currentMovie;
    private Option<Runnable> playVideoTask;

    private final BooleanProperty incomeMovieNotReady;
    private final DirectoryEntryProperty selectedEntry;

    private Disposable scapeProcess;
    private final Map<String, SearchResultHandler> searchResultMap;

    private final ObjectProperty<Option<MovieV2>> displayMovie;

    private FXMoviePanelControl() {
        this.moviePanel = GUICommon.loadFXMLController(FXMoviePanelView.class);

        if (this.moviePanel == null) {
            throw new RuntimeException();
        }

        this.currentMovie = null;
        this.displayMovie = new SimpleObjectProperty<>(Option.none());
        this.incomeMovieNotReady = new SimpleBooleanProperty();
        this.selectedEntry = new DirectoryEntryProperty();

        this.playVideoTask = Option.none();

        this.moviePanel.setup(this, this.displayMovie);

        this.searchResultMap = HashMap.ofEntries(
                Map.entry(DmmParsingProfile.parserName(), new SearchResultHandler(new DmmParsingProfile(), this.moviePanel.btnSDmm)),
                Map.entry(ArzonParsingProfile.parserName(), new SearchResultHandler(new ArzonParsingProfile(), this.moviePanel.btnSArzon)),
                Map.entry(DugaParsingProfile.parserName(), new SearchResultHandler(new DugaParsingProfile(), this.moviePanel.btnSDuga)),
                Map.entry(JavBusParsingProfile.parserName(), new SearchResultHandler(new JavBusParsingProfile(), this.moviePanel.btnSJavBus))
        );
        this.searchResultMap.forEach((k, b) -> b.disableButton());
        this.scapeProcess = null;

        this.incomeMovieNotReady.addListener((ov, o, n) -> {
            if (!n) {
                this.doMovieSelect(Option.of(this.selectedEntry.get()));
            }
        });

        this.selectedEntry.addListener((ov, o, n) -> {
            if (o != null) {
                this.incomeMovieNotReady.unbind();
            }
            if (n == null) {
                this.doMovieSelect(Option.none());
            } else {
                if (!n.getNeedCheck()) {
                    this.doMovieSelect(Option.of(n));
                } else {
                    this.incomeMovieNotReady.bind(n.getNeedCheckProperty());
                }
            }
        });

        this.moviePanel.btnExtraFanArt.disableProperty().bind(
                Bindings.createBooleanBinding(() -> this.displayMovie.get().map(MovieV2::getImgExtras).map(Stream::length).getOrElse(0) == 0, this.displayMovie));
        this.moviePanel.btnSaveMovie.disableProperty().bind(
                Bindings.createBooleanBinding(() -> this.displayMovie.get().isEmpty(), this.displayMovie));
    }

    public static FXMoviePanelControl getInstance() {
        if (null == instance) {
            instance = new FXMoviePanelControl();
        }
        return instance;
    }

    @Contract("null -> !null")
    private static String normalize(String str) {
        if (str == null) {
            return "";
        }
        return Normalizer.normalize(str, Normalizer.Form.NFKC);
    }

    private void doMovieSelect(@NotNull Option<DirectoryEntry> entryOption) {
        // if (this.currentDirectoryEntry.get().hasNfo() && MovieLock.getInstance().notInList(this.currentDirectoryEntry.get().getFilePath())) {
        this.cancelScrapeMovie();

        this.moviePanel.btnSAll.setDisable(!entryOption.map(DirectoryEntry::isDirectory).getOrElse(false));

        this.playVideoTask = entryOption
                .flatMap(DirectoryEntry::getMovieFolder)
                .map(MovieFolder::getMovieFilesPath)
                .flatMap(mf -> mf.find(m -> true))
                .map(t -> t._1)
                .map(FileObject::getName)
                .map(LocalVFS::fileNameToPath)
                .map(f -> () -> FXVideoViewer.show(f));

        entryOption.flatMap(DirectoryEntry::getMovieDataOption)
                .peek(movie -> this.fireEvent(EVENT_MOVIE_CHANGE, () -> this.setCurrentMovieToMovie(movie, entryOption.flatMap(DirectoryEntry::getMovieFolder).flatMap(MovieFolder::getCustomFlag))))
                .onEmpty(() -> this.fireEvent(EVENT_MOVIE_EMPTY, this::setCurrentMovieToEmpty));
    }

    public void playVideo() {
        this.playVideoTask.peek(Runnable::run);
    }

    private final WritableImage EmptyImage = new WritableImage(10, 270);

    public void setCurrentMovieToEmpty() {
        this.displayMovie.set(Option.none());
        this.currentMovie = null;
    }

    public void setCurrentMovieToMovie(MovieV2 movie, Option<Tuple2<Boolean, Boolean>> customFlag) {
        this.currentMovie = movie;
        this.displayMovie.set(Option.of(MovieV2.clone(movie)));

        this.moviePanel.clearCustomFlag();
        customFlag.peek(t -> this.moviePanel.setCustomFlag(t._1, t._2));
    }

    public void doDisplayExtraImage() {
        if (this.currentMovie.getImgExtras().length() > 0) {
            FXImageViewerWindow.show(this.currentMovie.getImgExtras().toVector());
        }
    }

    private String getButtonStyle() {
        return "-fx-background-color: \n" +
                "        #c3c4c4,\n" +
                "        linear-gradient(#d6d6d6 50%, white 100%),\n" +
                "        radial-gradient(center 50% -40%, radius 200%, #e6e6e6 45%, rgba(230,230,230,0) 50%);\n" +
                "    -fx-background-radius: 30;\n" +
                "    -fx-background-insets: 0,1,1;\n" +
                "    -fx-getLabel-fill: black;\n" +
                "    -fx-font-size: 12;\n" +
                "    -fx-padding: 10 10 10 10;\n" +
                "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 3, 0.0 , 0 , 1 );";
    }

    public void doSaveAction(MovieV2 movieToSave) {
        if (this.currentMovie != null && this.selectedEntry.canScrape() && movieToSave != null) {
            VBox vBox = new VBox();
            vBox.setSpacing(10);

            FXListView<String> text = new FXListView<>();
            text.setMinSize(700, 500);

            JFXButton btnSet = new JFXButton("[ Setting ]");
            btnSet.setButtonType(JFXButton.ButtonType.RAISED);
            btnSet.setOnAction((e) -> FXRenameFormatEditor.show(FXRenameFormatEditor.Type.FILE_NAME));

            JFXButton btnSave = new JFXButton("[ Save ]"/*, new Glyph("FontAwesome", FontAwesome.Glyph.CHECK_CIRCLE)*/);
            btnSave.setButtonType(JFXButton.ButtonType.RAISED);
            btnSave.setOnAction((e) -> {
                this.currentMovie = movieToSave;
                this.saveMovieToFile(this.currentMovie, text.getItems());
            });

            ButtonBar buttonBar = new ButtonBar();
            ButtonBar.setButtonData(btnSet, ButtonBar.ButtonData.HELP);
            ButtonBar.setButtonData(btnSave, ButtonBar.ButtonData.OK_DONE);
            buttonBar.getButtons().setAll(btnSet, btnSave);

            vBox.getChildren().setAll(text, buttonBar);

            DialogBuilder.create().heading("Save :").body(vBox).build().show();
        }
    }

    @Contract("null -> null")
    private MovieV2 applyModify(MovieV2 movie) {
        return movie;
    }

    /**
     * Run in Background
     * Save data to Files
     */
    private void saveMovieToFile(MovieV2 movie, ObservableList<String> outText) {
        //GUICommon.setLoading(true);
        this.selectedEntry.get().writeMovieDataToFile(movie, Option.of(outText), () ->
        {
            this.fireEvent(FXFileListControl.EVENT_FOCUS, null);
        });
    }

    public void doScrapeAll(boolean useCustom) {
        if (this.selectedEntry.canScrape()/*&& this.selectedEntry.hasMovie()*/) {
            Path path = this.selectedEntry.get().getValue();

            if (useCustom) {
                this.doScrapeAllCustom();
            } else {
                this.doScrapeMovie(SiteParsingProfile.findIDTagFromFile(path, Systems.getPreferences().getIsFirstWordOfFileID()));
            }
        }
    }

    private void doScrapeAllCustom() {
        if (this.selectedEntry.canScrape()/*.hasMovie()*/) {

            String fileName = this.selectedEntry.get().getValue().getFileName().toString();

            VBox vBox = new VBox();
            HBox hBox = new HBox();
            JFXButton btnUseName = new JFXButton("Use Movie Name");
            JFXButton btnFrontID = new JFXButton("Front Movie ID");
            JFXButton btnBackID = new JFXButton("Back Movie ID");
            JFXTextField txtSearch = new JFXTextField(SiteParsingProfile.findIDTagFromFile(fileName, Systems.getPreferences().getIsFirstWordOfFileID()));
            txtSearch.setMinWidth(500);
            btnUseName.setOnAction((e) -> txtSearch.setText(fileName));
            btnFrontID.setOnAction((e) -> txtSearch.setText(SiteParsingProfile.findIDTagFromFile(fileName, true)));
            btnBackID.setOnAction((e) -> txtSearch.setText(SiteParsingProfile.findIDTagFromFile(fileName, false)));
            hBox.setAlignment(Pos.CENTER);
            hBox.setSpacing(30);
            vBox.setSpacing(10);
            hBox.getChildren().addAll(btnUseName, btnFrontID, btnBackID);
            vBox.getChildren().addAll(txtSearch, hBox);

            GUICommon.showDialog("Keyword to search :", vBox, "Cancel", "Search", () -> this.doScrapeMovie(txtSearch.getText()));
        }
    }

    private void doScrapeMovie(final String searchStr) {
        this.scapeProcess = Mono.when(this.searchResultMap.mapValues((v) -> v.scrapeStart(searchStr)).valuesIterator())
                .doFirst(() -> this.moviePanel.btnSAll.setDisable(true))
                .doFinally((signalType) -> this.moviePanel.btnSAll.setDisable(false))
                .subscribeOn(ExecuteSystem.get().getNormalScheduler())
                .subscribe();
        /*
        Future<Seq<Vector<SimpleMovieData>>> all = Future.sequence(this.searchResultMap.map(v -> v._2.getSearchResults()));
        all.onComplete((i) -> this.moviePanel.btnSAll.setDisable(false));*/
    }

    private void cancelScrapeMovie() {
        this.searchResultMap.forEach((k, v) -> v.disableButton());
        if (this.scapeProcess != null && !this.scapeProcess.isDisposed()) {
            this.scapeProcess.dispose();
            this.scapeProcess = null;
        }
    }

    public void doScrapeDetail(String parserName) {
        //GUICommon.getPreferences().setScrapeActor(!this.moviePanel.btnSFast.isSelected());
        Consumer<MovieV2> consumer = m -> Platform.runLater(() -> this.processNewDetail(m, this::useScrapeMovieData));
        this.searchResultMap.get(parserName).peek(h -> h.scrapeDetail(consumer));
    }

    /**
     * Run in FX Thread
     * Ask user for overwrite result
     */
    private void processNewDetail(MovieV2 newMovie, Consumer<MovieV2> movieV2Consumer) {
        if (this.currentMovie == null) {
            movieV2Consumer.accept(newMovie);
        } else {
            FXMovieDetailCompareView unit = GUICommon.loadFXMLController(FXMovieDetailCompareView.class);
            assert unit != null;
            unit.setEditable(false);
            unit.setMovieOld(this.currentMovie);
            unit.setMovieNew(newMovie);

            Runnable runnable = () -> {
                if (!unit.isUseTitle()) {
                    newMovie.setMovieTitle(this.currentMovie.getMovieTitle());
                }
                if (!unit.isUseDate()) {
                    newMovie.setYear(this.currentMovie.getYear());
                    newMovie.setReleaseDate(this.currentMovie.getReleaseDate());
                }
                if (!unit.isUseID()) {
                    newMovie.setMovieID(this.currentMovie.getMovieID());
                }
                if (!unit.isUseStudio()) {
                    newMovie.setStudio(this.currentMovie.getStudio());
                }
                if (!unit.isUseMaker()) {
                    newMovie.setMovieMaker(this.currentMovie.getMovieMaker());
                }
                if (!unit.isUseSet()) {
                    newMovie.setSeries(this.currentMovie.getSeries());
                }
                if (!unit.isUsePlot()) {
                    newMovie.setPlot(this.currentMovie.getPlot());
                }
                if (!unit.isUseGenres()) {
                    newMovie.setGenreList(this.currentMovie.getGenreList());
                }
                if (!unit.isUseActors()) {
                    this.currentMovie.setActorList(this.currentMovie.getActorList());
                }
                if (!unit.isUseDirector()) {
                    newMovie.setDirector(this.currentMovie.getDirectorString());
                }
                if (!unit.isUseCover()) {
                    newMovie.setImgFrontCover(this.currentMovie.getImgFrontCover());
                    newMovie.setImgBackCover(this.currentMovie.getImgBackCover());
                }
                if (!unit.isUseExtraArt()) {
                    newMovie.setImgExtras(this.currentMovie.getImgExtras());
                }
                movieV2Consumer.accept(newMovie);
            };

            DialogBuilder.create()
                    .heading("Select the information you want to overwrite :")
                    .body(unit.getPane())
                    .buttonOkCancel(runnable)
                    .build().show();
        }
    }

    /* Run in FX Thread
    - Update Result */
    private void useScrapeMovieData(MovieV2 movie) {
        Runnable run = () -> this.setCurrentMovieToMovie(movie, Option.none());
        if (this.currentMovie == null) {
            this.fireEvent(EVENT_MOVIE_CHANGE, run);
        } else {
            run.run();
        }
    }

    @Override
    public Node getPane() {
        return this.moviePanel.getPane();
    }


    @Override
    public void registerListener() {
        this.registerListener(FXFileListControl.EVENT_LIST_SELECTED, e -> this.selectedEntry.setValue(e.getParam()));
        this.registerListener(FXArtPanelControl.EVENT_POSTER_CHANGE, e -> this.updatePoster(e.getParam()));
        this.registerListener(EVENT_ACTION_SCRAPE_MOVIE, e -> this.doScrapeAll(e.getParam()));
        this.registerListener(EVENT_ACTION_SCRAPE_DETAIL, e -> this.doScrapeDetail(e.getParam()));
        this.registerListener(EVENT_ACTION_SAVE_MOVIE, e -> this.doSaveAction(e.getParam()));
        this.registerListener(EVENT_SHOW_EXTRA_ART, e -> this.doDisplayExtraImage());
        this.registerListener(EVENT_PLAY_VIDEO, e -> this.playVideo());
    }

    private void updatePoster(Image image) {
        GUICommon.debugMessage(() -> "EVENT_POSTER_CHANGE updatePoster");
        if (this.currentMovie.getImgFrontCover().isDefined()) {
            this.currentMovie.getImgFrontCover().peek(t -> t.setImage(image));
        } else {
            FxThumb fxThumb = FxThumb.create();
            fxThumb.setImage(image);
            this.currentMovie.setImgFrontCover(Option.of(fxThumb));
        }
    }

    public void editCustomFlagAction() {
        this.fireEvent(FXFileListControl.EVENT_CUSTOM_FLAG_EDIT, null);
    }


    static class SearchResultHandler {
        private final SiteParsingProfile parsingProfile;
        private final JFXButton button;
        private Vector<SimpleMovieData> searchResults;

        @Contract(pure = true)
        @java.beans.ConstructorProperties({"parsingProfile", "button"})
        SearchResultHandler(SiteParsingProfile parsingProfile, JFXButton button) {
            this.parsingProfile = parsingProfile;
            this.button = button;
            this.searchResults = Vector.empty();
        }

        Mono<Vector<SimpleMovieData>> scrapeStart(String searchStr) {
            return ScrapeMovieAction.scrapeMovie(this.getParsingProfile(), searchStr)
                    .doFirst(() -> this.getButton().setDisable(true))
                    .doFirst(() -> this.searchResults = Vector.empty())
                    .doOnSuccess(list -> {
                        if (!list.isEmpty()) {
                            this.getButton().setDisable(false);
                            this.setSearchResults(list);
                        }
                    });
        }

        void scrapeDetail(Consumer<MovieV2> consumer) {
            if (this.getSearchResults().isEmpty()) {
                GUICommon.errorDialog(new Exception("No Search Result"));
                return;
            }
            ScrapeMovieAction.scrapeMovieDetail(this.getSearchResults(), this.getParsingProfile(), consumer);
        }

        void disableButton() {
            this.getButton().setDisable(true);
        }

        SiteParsingProfile getParsingProfile() {
            return this.parsingProfile;
        }

        public JFXButton getButton() {
            return this.button;
        }

        Vector<SimpleMovieData> getSearchResults() {
            return this.searchResults;
        }

        void setSearchResults(Vector<SimpleMovieData> searchResults) {
            this.searchResults = searchResults;
        }
    }

    static class DirectoryEntryProperty extends SimpleObjectProperty<DirectoryEntry> {
        boolean canScrape() {
            return (this.notNull() && this.getValue().isDirectory());
        }

        boolean notNull() {
            return (this.getValue() != null);
        }
    }
}
