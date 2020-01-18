package org.nagoya.controller;

import com.jfoenix.controls.JFXDialog;
import io.vavr.collection.Vector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controller.siteparsingprofile.SiteParsingProfile;
import org.nagoya.controls.FXListView;
import org.nagoya.model.MovieV2;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.system.dialog.DialogBuilder;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class ScrapeMovieAction {

    //private static ScrapeMovieAction instance = new ScrapeMovieAction();

    @Contract(pure = true)
    private ScrapeMovieAction() {
    }

    /**
     * Run in Background
     * Search for results
     */
    @NotNull
    public static Mono<Vector<SimpleMovieData>> scrapeMovie(SiteParsingProfile siteScraper, String searchStr) {
        return Mono.fromCallable(() -> MovieV2.scrapeMovie(siteScraper, searchStr));
    }

    /**
     * Run in FX Thread
     * Ask user for result selection
     *
     * @param results SearchResult Array
     */
    public static void scrapeMovieDetail(@NotNull Vector<SimpleMovieData> results, SiteParsingProfile siteScraper, Consumer<MovieV2> movieConsumer) {

        if (results.length() == 1) {
            scrapeMovieDetail(results.get(0), siteScraper)
                    .subscribe(movieConsumer);
            return;
        }

        FXListView<SimpleMovieData> listView = new FXListView<>();
        ObservableList<SimpleMovieData> data = FXCollections.observableArrayList(results.toJavaList());

        DialogBuilder dialogBuilder = DialogBuilder.create();
        JFXDialog dialog = dialogBuilder.getDialog();

        listView.setMinWidth(510);
        listView.setPrefHeight(400);
        listView.setMaxSize(510, 400);
        listView.setCellFactory((ListView<SimpleMovieData> l) -> new ScrapeMovieSelectCell());
        listView.setItems(data);
        listView.setOnMouseClicked((event) -> {
            if (event.getClickCount() == 2) {
                dialog.close();
                scrapeMovieDetail(results.get(listView.getSelectionModel().getSelectedIndex()), siteScraper)
                        .subscribe(movieConsumer);
            }
        });

        dialogBuilder
                .heading("Select : (Double Click on the List)")
                .body(listView)
                .button("Cancel", null, null)
                .build().show();
    }

    /**
     * Run in Background
     * Get the result data
     */
    @NotNull
    public static Mono<MovieV2> scrapeMovieDetail(SimpleMovieData searchResult, SiteParsingProfile siteScraper) {
        return UtilCommon.tryToMono(() -> MovieV2.fromSearchResult(searchResult, siteScraper))
                .doFirst(() -> GUICommon.setLoading(true))
                .doFinally((signalType) -> GUICommon.setLoading(false))
                .subscribeOn(ExecuteSystem.get().getNormalScheduler());
    }


  /*  private void scrapeMovieResultCompare(Movie currentMovie, Movie movieToWrite) {
        FXMovieDetailCompareView unit = new FXMovieDetailCompareView();
        unit.setEditable(false);
        unit.setMovieOld(movieDetailDataMap, this.actorObservableList, this.genreObservableList);
        unit.setMovieNew(movieToWrite);

        GUICommon.showDialog("Select the information you want to overwrite :", unit.getPane(), "Cancel", "Confirm", () -> {
            if (!unit.isUseTitle()) {
                movieToWrite.setTitle(currentMovie.getTitle());
            }
            if (!unit.isUseDate()) {
                movieToWrite.setYear(currentMovie.getYear());
                movieToWrite.setReleaseDate(currentMovie.getReleaseDate());
            }
            if (!unit.isUseID()) {
                movieToWrite.setId(currentMovie.getId());
            }
            if (!unit.isUseStudio()) {
                movieToWrite.setStudio(currentMovie.getStudio());
            }
            if (!unit.isUseMaker()) {
                movieToWrite.setMaker(currentMovie.getMaker());
            }
            if (!unit.isUseSet()) {
                movieToWrite.setSet(currentMovie.getSet());
            }
            if (!unit.isUsePlot()) {
                movieToWrite.setPlot(currentMovie.getPlot());
            }
            if (!unit.isUseGenres()) {
                movieToWrite.setGenres(currentMovie.getGenres());
            }
            if (!unit.isUseActors()) {
                movieToWrite.setActors(currentMovie.getActors());
            }
            if (!unit.isUseDirector()) {
                movieToWrite.setDirectors(currentMovie.getDirectors());
            }
            if (!unit.isUseCover()) {
                movieToWrite.setPoster(currentMovie.getPoster());
                movieToWrite.setCover(currentMovie.getCover());
            }
            if (!unit.isUseExtraArt()) {
                movieToWrite.setExtraFanart(currentMovie.getExtraFanart());
            }
        });
    }
*/
}

class ScrapeMovieSelectCell extends ListCell<SimpleMovieData> {

    private final FXScrapeMovieSelectCell cell;

    ScrapeMovieSelectCell() {
        this.cell = new FXScrapeMovieSelectCell();
        this.setPrefWidth(500);
    }

    @Override
    public void updateItem(SimpleMovieData item, boolean empty) {
        super.updateItem(item, empty);

        this.setText("");

        if (!empty) {
            this.cell.setResult(item);
            this.setGraphic(this.cell);
        } else {
            this.setGraphic(null);
        }
    }

}

class FXScrapeMovieSelectCell extends AnchorPane {

    @FXML
    ImageView imgCover;

    @FXML
    Label txtTitle, txtTitle2;

    FXScrapeMovieSelectCell() {
        GUICommon.loadFXMLRoot(this);
    }

    void setResult(@NotNull SimpleMovieData item) {
        this.txtTitle.setText(item.getStrTitle());
        this.txtTitle2.setText(item.getStrUrl());
        this.imgCover.setImage(item.getImgCover().map(FxThumb::getImage).getOrNull());
    }
}