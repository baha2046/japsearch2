package org.nagoya.view.customcell;

import io.vavr.control.Option;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.controls.FXGridCell;
import org.nagoya.controls.FXGridView;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.view.FXImageViewWrapper;

public class MovieDBListSimpleGridCell extends FXGridCell<SimpleMovieData> {
    public static int CELL_WIDTH = 160;
    public static int CELL_HEIGHT = 360;

    private final FXMovieDBListSimpleGridCell cell;

    public MovieDBListSimpleGridCell(FXGridView<SimpleMovieData> gridView) {
        super(gridView);

        this.cell = new FXMovieDBListSimpleGridCell();
        this.setAlignment(Pos.CENTER);
        this.setPadding(Insets.EMPTY);
        this.setPrefWidth(CELL_WIDTH);
        this.setPrefHeight(CELL_HEIGHT);

        this.cell.selectedProperty().bind(this.cellSelectedProperty);
        //this.cell.selectedProperty().bind(gridView.selectionProperty().isEqualTo(this.itemProperty()));
        //this.cell.setOnMouseClicked((e) -> gridView.setSelection(this.getItem()));//movieDataConsumer.accept(this.cell.getMovieData()));
    }

    @Override
    public void updateItem(SimpleMovieData item, boolean empty) {
        super.updateItem(item, empty);

        this.setText("");
        this.cell.setMovieData(item);

        if (!empty) {
            this.cell.show(item);
            this.setGraphic(this.cell);
        } else {
            this.setGraphic(null);
        }
    }
}

class FXMovieDBListSimpleGridCell extends AnchorPane {

    @FXML
    Label txtId, txtTitle;

    @FXML
    Rectangle rect;

    @FXML
    ImageView imgCover;

    private SimpleMovieData movieData;
    private final FXImageViewWrapper imageViewWrapper;

    private static final Color RECT_GRAY = Color.valueOf("#cccccc");
    private static final Color RECT_BLUE = Color.valueOf("#dad4f5");
    private static final Color RECT_RED = Color.valueOf("#f5d4da");
    private static final Color RECT_NORMAL = Color.valueOf("#f8feff");

    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    FXMovieDBListSimpleGridCell() {
        GUICommon.loadFXMLRoot(this);
        this.imageViewWrapper = new FXImageViewWrapper();

        /*this.selected.addListener((ov, o, n) -> {
            if (n) {
                this.rect.setVisible(false);
                this.rect.setVisible(true);
            }
        });*/
    }

    String getCss(Color color) {
        /*if (this.isSelected()) {
            color = color.darker();
        }*/
        return "-fx-fill: " + this.toHexString(color) + ";";
    }

    @NotNull
    private String format(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    public String toHexString(@NotNull Color value) {
        return "#" + (this.format(value.getRed()) + this.format(value.getGreen()) + this.format(value.getBlue()) + this.format(value.getOpacity()))
                .toUpperCase();
    }

    public boolean isSelected() {
        return this.selected.get();
    }

    public BooleanProperty selectedProperty() {
        return this.selected;
    }

    void show(@NotNull SimpleMovieData movieData) {
        this.imageViewWrapper.setKey(movieData.getStrId());

        /*if (this.isSelected()) {
            GUICommon.debugMessage("isSelect : " + movieData.getStrTitle());
        }*/

        if (movieData.isTemp()) {
            if (movieData.isBlackList()) {
                this.rect.setStyle(this.getCss(RECT_GRAY));//rgba(255, 255, 35, 0.2);");
            } else {
                this.rect.setStyle(this.getCss(RECT_BLUE));
            }
        } else if (movieData.isExist()) {
            this.rect.setStyle(this.getCss(RECT_NORMAL));//rgba(255, 255, 35, 0.2);");
        } else {
            this.rect.setStyle(this.getCss(RECT_RED));
        }

        this.txtId.setText(movieData.getStrId());
        this.txtTitle.setText(movieData.getStrTitle());
        movieData.getImgCover().peek(i ->
                i.getImage(this.imageViewWrapper::setImage, movieData.getStrId(), this.imgCover));
    }

    public Option<SimpleMovieData> getMovieData() {
        return Option.of(this.movieData);
    }

    public void setMovieData(SimpleMovieData movieData) {
        this.movieData = movieData;
    }
}
