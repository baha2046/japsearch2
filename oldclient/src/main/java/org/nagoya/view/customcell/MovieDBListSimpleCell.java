package org.nagoya.view.customcell;

import com.jfoenix.controls.JFXListCell;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.view.FXImageViewWrapper;

public class MovieDBListSimpleCell extends JFXListCell<SimpleMovieData> {

    private final FXMovieDBListSimpleCell cell;

    public MovieDBListSimpleCell() {
        this.cell = new FXMovieDBListSimpleCell();
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(Insets.EMPTY);
        this.setPrefWidth(500);
        this.setPrefHeight(200);
    }

    @Override
    public void updateItem(SimpleMovieData item, boolean empty) {
        super.updateItem(item, empty);

        this.setText("");

        if (!empty) {
            this.cell.show(item);
            this.setGraphic(this.cell);
        } else {
            this.setGraphic(null);
        }
    }
}

class FXMovieDBListSimpleCell extends AnchorPane {

    @FXML
    Label txtId, txtTitle;

    @FXML
    ImageView imgCover;

    private final PseudoClass isVirtual;
    private final FXImageViewWrapper imageViewWrapper;

    FXMovieDBListSimpleCell() {
        this.setId("movie_list_cell");
        this.isVirtual = PseudoClass.getPseudoClass("virtual");

        GUICommon.loadFXMLRoot(this);
        this.imageViewWrapper = new FXImageViewWrapper();
    }

    void show(@NotNull SimpleMovieData movieData) {
        this.imageViewWrapper.setKey(movieData.getStrId());

        this.pseudoClassStateChanged(this.isVirtual, !movieData.isExist());

        this.txtId.setText(movieData.getStrId());
        this.txtTitle.setText(movieData.getStrTitle());
        movieData.getImgCover().peek(i ->
                i.getImage(this.imageViewWrapper::setImage, movieData.getStrId(), this.imgCover));
    }
}

