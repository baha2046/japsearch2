package org.nagoya.view;

import com.jfoenix.controls.*;
import io.vavr.control.Try;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;
import org.nagoya.controls.FXImageViewerWindow;
import org.nagoya.controls.FXListView;
import org.nagoya.model.MovieV2;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.model.dataitem.Genre;
import org.nagoya.system.FXMLController;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.view.customcell.ActorListCell;
import org.nagoya.view.customcell.GenreListRectCell;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class FXMovieDetailCompareView extends FXMLController {
    @FXML
    protected JFXTextField txtNewTitle, txtNewId, txtNewStudio, txtNewSet, txtNewDate;
    @FXML
    protected JFXTextField txtTitle, txtId, txtStudio, txtSet, txtDate;
    @FXML
    protected JFXTextArea txtNewPlot, txtPlot;
    @FXML
    protected JFXTextField txtNewDirector, txtNewMaker;
    @FXML
    protected JFXTextField txtDirector, txtMaker;
    @FXML
    protected Pane paneActors0;
    protected FXListView<ActorV2> lvActors0;
    @FXML
    protected Pane paneActors1;
    protected FXListView<ActorV2> lvActors1;
    @FXML
    JFXListView<Genre> lvGenres0;
    @FXML
    JFXListView<Genre> lvGenres1;
    @FXML
    JFXToggleButton useTitle, useDate, useId, useStudio, useSet, usePlot;
    @FXML
    JFXToggleButton useActors, useGenres, useDirector, useMaker, btnUseCover, btnUseExtra;
    @FXML
    JFXToggleButton btnAll;
    @FXML
    Label txtExtraImage;
    @FXML
    JFXButton btnExtraFanArt, btnCover;

    public FXMovieDetailCompareView() {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.lvGenres0.setCellFactory((ListView<Genre> l) -> new GenreListRectCell());

        this.lvActors0 = new FXListView<>();
        this.lvActors0.setCellFactory((ListView<ActorV2> l) -> new ActorListCell(false));
        this.lvActors0.fitToPane(this.paneActors0);

        this.lvGenres1.setCellFactory((ListView<Genre> l) -> new GenreListRectCell());

        this.lvActors1 = new FXListView<>();
        this.lvActors1.setCellFactory((ListView<ActorV2> l) -> new ActorListCell(false));
        this.lvActors1.fitToPane(this.paneActors1);

        this.txtTitle.disableProperty().bind(this.useTitle.selectedProperty());
        this.txtNewTitle.disableProperty().bind(this.useTitle.selectedProperty().not());
        this.txtDate.disableProperty().bind(this.useDate.selectedProperty());
        this.txtNewDate.disableProperty().bind(this.useDate.selectedProperty().not());
        this.txtId.disableProperty().bind(this.useId.selectedProperty());
        this.txtNewId.disableProperty().bind(this.useId.selectedProperty().not());
        this.txtStudio.disableProperty().bind(this.useStudio.selectedProperty());
        this.txtNewStudio.disableProperty().bind(this.useStudio.selectedProperty().not());
        this.txtSet.disableProperty().bind(this.useSet.selectedProperty());
        this.txtNewSet.disableProperty().bind(this.useSet.selectedProperty().not());
        this.txtPlot.disableProperty().bind(this.usePlot.selectedProperty());
        this.txtNewPlot.disableProperty().bind(this.usePlot.selectedProperty().not());
        this.txtDirector.disableProperty().bind(this.useDirector.selectedProperty());
        this.txtNewDirector.disableProperty().bind(this.useDirector.selectedProperty().not());
        this.txtMaker.disableProperty().bind(this.useMaker.selectedProperty());
        this.txtNewMaker.disableProperty().bind(this.useMaker.selectedProperty().not());
        this.lvGenres0.disableProperty().bind(this.useGenres.selectedProperty());
        this.lvGenres1.disableProperty().bind(this.useGenres.selectedProperty().not());
        this.lvActors0.disableProperty().bind(this.useActors.selectedProperty());
        this.lvActors1.disableProperty().bind(this.useActors.selectedProperty().not());
        this.useTitle.setSelected(true);
        this.useDate.setSelected(true);
        this.useId.setSelected(true);
        this.useStudio.setSelected(true);
        this.useSet.setSelected(true);
        this.usePlot.setSelected(true);
        this.useActors.setSelected(true);
        this.useGenres.setSelected(true);
        this.useDirector.setSelected(true);
        this.useMaker.setSelected(true);
        this.btnAll.setSelected(true);
        this.btnAll.setOnAction((e) -> {
            this.useTitle.setSelected(this.btnAll.isSelected());
            this.useDate.setSelected(this.btnAll.isSelected());
            this.useId.setSelected(this.btnAll.isSelected());
            this.useStudio.setSelected(this.btnAll.isSelected());
            this.useSet.setSelected(this.btnAll.isSelected());
            this.usePlot.setSelected(this.btnAll.isSelected());
            this.useActors.setSelected(this.btnAll.isSelected());
            this.useGenres.setSelected(this.btnAll.isSelected());
            this.useDirector.setSelected(this.btnAll.isSelected());
            this.useMaker.setSelected(this.btnAll.isSelected());
        });

        this.useTitle.visibleProperty().bind(this.txtTitle.textProperty().isEqualTo(this.txtNewTitle.textProperty()).not());
        this.useDate.visibleProperty().bind(this.txtDate.textProperty().isEqualTo(this.txtNewDate.textProperty()).not());
    }

    public boolean isUseTitle() {
        return this.useTitle.isSelected();
    }

    public boolean isUseDate() {
        return this.useDate.isSelected();
    }

    public boolean isUseID() {
        return this.useId.isSelected();
    }

    public boolean isUseStudio() {
        return this.useStudio.isSelected();
    }

    public boolean isUseMaker() {
        return this.useMaker.isSelected();
    }

    public boolean isUseSet() {
        return this.useSet.isSelected();
    }

    public boolean isUsePlot() {
        return this.usePlot.isSelected();
    }

    public boolean isUseActors() {
        return this.useActors.isSelected();
    }

    public boolean isUseGenres() {
        return this.useGenres.isSelected();
    }

    public boolean isUseDirector() {
        return this.useDirector.isSelected();
    }

    public boolean isUseCover() {
        return this.btnUseCover.isSelected();
    }

    public boolean isUseExtraArt() {
        return this.btnUseExtra.isSelected();
    }

    public void setEditable(boolean canEdit) {
        this.txtTitle.setEditable(canEdit);
        this.txtDate.setEditable(canEdit);
        this.txtId.setEditable(canEdit);
        this.txtStudio.setEditable(canEdit);
        this.txtSet.setEditable(canEdit);
        this.txtPlot.setEditable(canEdit);

        this.txtNewTitle.setEditable(canEdit);
        this.txtNewDate.setEditable(canEdit);
        this.txtNewId.setEditable(canEdit);
        this.txtNewStudio.setEditable(canEdit);
        this.txtNewSet.setEditable(canEdit);
        this.txtNewPlot.setEditable(canEdit);

        this.txtDirector.setEditable(canEdit);
        this.txtNewDirector.setEditable(canEdit);
        this.txtMaker.setEditable(canEdit);
        this.txtNewMaker.setEditable(canEdit);
    }

    public void setMovieOld(MovieV2 movieV2) {
        this.txtDate.setText(movieV2.getReleaseDate());
        this.txtTitle.setText(movieV2.getMovieTitle());
        this.txtId.setText(movieV2.getMovieID());
        this.txtStudio.setText(movieV2.getStudio());
        this.txtSet.setText(movieV2.getSeries());
        this.txtPlot.setText(movieV2.getPlot());
        this.txtDirector.setText(movieV2.getDirectorString());
        this.txtMaker.setText(movieV2.getMovieMaker());

        this.lvGenres0.setItems(movieV2.getGenreList());
        this.lvActors0.setItems(movieV2.getActorList());
    }

    public void setMovieNew(MovieV2 movieV2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.txtNewDate.setText(Try.of(() -> LocalDate.parse(movieV2.getReleaseDate(), formatter).toString()).getOrElse(""));

        this.txtNewTitle.setText(movieV2.getMovieTitle());
        this.txtNewId.setText(movieV2.getMovieID());
        this.txtNewStudio.setText(movieV2.getStudio());
        this.txtNewSet.setText(movieV2.getSeries());
        this.txtNewPlot.setText(movieV2.getPlot());
        this.txtNewDirector.setText(movieV2.getDirectorString());
        this.txtNewMaker.setText(movieV2.getMovieMaker());

        this.txtExtraImage.setText("Extra Image: " + movieV2.getImgExtras().length());
        this.btnExtraFanArt.setDisable(movieV2.getImgExtras().length() == 0);
        this.btnExtraFanArt.setOnAction((e) -> FXImageViewerWindow.show(movieV2.getImgExtras().toVector()));

        this.btnCover.setDisable(movieV2.getImgBackCover().isEmpty());
        this.btnCover.setOnAction((e) -> {
            DialogBuilder.create()
                    .heading(movieV2.getImgBackCover().map(t -> t.getThumbURL().toString()).getOrElse(""))
                    .body(new ImageView(movieV2.getImgBackCover().map(FxThumb::getImage).getOrElse((Image) null)))
                    .build()
                    .show();
        });

        this.btnUseCover.setDisable(this.btnCover.isDisable());
        this.btnUseExtra.setDisable(this.btnExtraFanArt.isDisable());
        this.btnUseCover.setSelected(!this.btnCover.isDisable());
        this.btnUseExtra.setSelected(!this.btnExtraFanArt.isDisable());

        this.autoSelection(this.txtId, this.txtNewId, this.useId);
        this.autoSelection(this.txtStudio, this.txtNewStudio, this.useStudio);
        this.autoSelection(this.txtSet, this.txtNewSet, this.useSet);
        this.autoSelection(this.txtDirector, this.txtNewDirector, this.useDirector);
        this.autoSelection(this.txtMaker, this.txtNewMaker, this.useMaker);

        if (Objects.equals(this.txtNewPlot.getText(), this.txtPlot.getText())) {
            this.usePlot.setVisible(false);
        } else if (this.txtPlot.getText().length() > 0 && this.txtNewPlot.getText().length() == 0) {
            this.usePlot.setSelected(false);
        }

        this.lvGenres1.setItems(movieV2.getGenreList());
        this.lvActors1.setItems(movieV2.getActorList());
    }

    private void autoSelection(@NotNull JFXTextField t1, @NotNull JFXTextField t2, JFXToggleButton b) {
        if (Objects.equals(t2.getText(), t1.getText())) {
            b.setVisible(false);
        } else if (t1.getText().length() > 0 && t2.getText().length() == 0) {
            b.setSelected(false);
        }
    }
}
