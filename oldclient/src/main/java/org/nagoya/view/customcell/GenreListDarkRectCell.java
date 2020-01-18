package org.nagoya.view.customcell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;

public class GenreListDarkRectCell extends GenreListRectCell {
    public GenreListDarkRectCell() {
        this.setTextFill(Color.web("#4059A9"));
        this.getStyleClass().add("text_jp_14");
        this.setAlignment(Pos.TOP_LEFT);
        this.setPadding(new Insets(5, 5, 5, 5));
    }
}
