package org.nagoya.view.customcell;

import javafx.scene.control.ListCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.nagoya.model.dataitem.Genre;

public class GenreListRectCell extends ListCell<Genre> {
    protected Rectangle rectangle;

    public GenreListRectCell() {
        super();
        this.rectangle = new Rectangle(10, 10);
        this.rectangle.setArcWidth(8);
        this.rectangle.setArcHeight(8);
        this.rectangle.setFill(Color.rgb(111, 125, 161));
    }

    @Override
    public void updateItem(Genre item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            this.setText(item.getGenre());
            //this.setFont(Font.font(Font.getDefault().getName(), FontWeight.NORMAL, 12));
            this.setGraphic(this.rectangle);
        } else {
            this.setGraphic(null);
            this.setText("");
        }
    }
}
