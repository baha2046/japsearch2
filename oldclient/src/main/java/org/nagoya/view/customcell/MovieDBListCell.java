package org.nagoya.view.customcell;

import com.jfoenix.controls.JFXListCell;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.view.FXImageViewWrapper;

public class MovieDBListCell extends JFXListCell<DirectoryEntry> {

    private final FXMovieDBListCell cell;

    public MovieDBListCell() {
        this.cell = new FXMovieDBListCell();
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(Insets.EMPTY);
        this.setPrefWidth(500);
        this.setPrefHeight(100);
    }

    @Override
    public void updateItem(DirectoryEntry item, boolean empty) {
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

class FXMovieDBListCell extends AnchorPane {

    @FXML
    Label txtId, txtTitle, txtMaker;

    @FXML
    ImageView imgCover;

    private final FXImageViewWrapper imageViewWrapper;

    FXMovieDBListCell() {
        GUICommon.loadFXMLRoot(this);
        this.imageViewWrapper = new FXImageViewWrapper();
    }

    void show(@NotNull DirectoryEntry directoryEntry) {
        if (directoryEntry.getMovieData() != null) {
            this.imageViewWrapper.setKey(directoryEntry.getValue().toString());

            this.txtId.setText(directoryEntry.getMovieData().getMovieID());
            this.txtTitle.setText(directoryEntry.getMovieData().getMovieTitle());
            this.txtMaker.setText(directoryEntry.getMovieData().getMovieMaker());

            directoryEntry.getMovieData().getImgFrontCover().peek(i ->
                    i.getImage(this.imageViewWrapper::setImage, directoryEntry.getValue().toString(), this.imgCover));
        } else {
            this.txtId.setText("");
            this.txtTitle.setText(directoryEntry.getValue().toString());
            this.txtMaker.setText("");
            this.imgCover.setImage(null);
            GUICommon.debugMessage(directoryEntry.getNeedCheck().toString());
            GUICommon.debugMessage("ERROR >> null movie " + directoryEntry.getValue().toString());
        }
    }
}
