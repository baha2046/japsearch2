package org.nagoya.controls;

import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;

public class FXTest extends VBox {

    FXPagination pagination;

    FXTest() {
        TextField textField = new TextField();
        this.pagination = new FXPagination(10);
        this.pagination.setPageCount(6);
        this.pagination.setPageFactory((i) -> textField.setText("" + i));
        this.setPrefSize(400, 200);
        this.getChildren().setAll(textField, this.pagination);
        this.setPadding(FXWindow.getDefaultInset());
    }

    public void show() {
        WindowBuilder.create()
                .title("TEST", true)
                .body(this)
                .buildSingle()
                .show();
    }
}
