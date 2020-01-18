package org.nagoya.view.dialog;

import com.jfoenix.controls.JFXButton;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.nagoya.GUICommon;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;
import org.nagoya.controls.FXListView;

public class FXProgressDialog extends VBox {
    private static FXProgressDialog instance;

    public static FXProgressDialog getInstance() {
        if (instance == null) {
            instance = new FXProgressDialog();
        }
        return instance;
    }

    FXListView<String> textArea;
    private Map<String, JFXButton> buttonMap;
    private final ProgressBar progressBar;

    private FXProgressDialog() {
        this.textArea = new FXListView<>();
        this.textArea.setPrefWidth(700);
        this.textArea.setMinWidth(700);
        this.textArea.setPrefHeight(260);
        this.textArea.getItems().addListener((ListChangeListener<String>) c -> this.textArea.scrollTo(c.getList().size() - 1));

        HBox hBox = GUICommon.hbox(10);
        hBox.setAlignment(Pos.CENTER);

        this.buttonMap = HashMap.empty();
        for (int i = 1; i <= Runtime.getRuntime().availableProcessors(); i++) {
            JFXButton button = new JFXButton();
            button.setMinWidth(60);
            this.buttonMap = this.buttonMap.put("parallel-" + i, button);
            hBox.getChildren().add(new Label("T" + i));
            hBox.getChildren().add(button);
        }

        this.progressBar = new ProgressBar();
        this.progressBar.setProgress(1);
        this.progressBar.setPrefWidth(300);

        this.setAlignment(Pos.CENTER);
        this.setPadding(FXWindow.getDefaultInset());
        this.getChildren().addAll(this.textArea, hBox, new Separator(), this.progressBar);
        //this.textArea = GUICommon.getTextArea(700, 260, true);
    }

    public void setProgress(double v) {
        this.progressBar.setProgress(v);
    }

    public void setThreadState(String threadName, String threadState) {
        GUICommon.runOnFx(() -> this.buttonMap.get(threadName).peek(b -> b.setText(threadState)));
    }

    public void write(String text) {
        GUICommon.runOnFx(() -> {
            var list = this.textArea.getItems();
            list.add(text);
            if (list.size() > 500) {
                list.remove(0, 100);
            }
        });
    }

    public ObservableList<String> getList() {
        return this.textArea.getItems();
    }

    public Node getPane() {
        return this;
    }

    public void show() {
        WindowBuilder.create()
                .title("Progress", true)
                .body(this)
                .resizable(false)
                .prefSize(780, 320)
                .buildSingle().show();

        /*if (this.getScene() == null) {
            DialogBuilder.create().heading("[ Progress ]").body(this).build().show();
        }*/
    }

    public Option<ObservableList<String>> startProgressDialog() {
        GUICommon.runOnFx(this::show);
        return Option.of(this.getList());
    }
}
