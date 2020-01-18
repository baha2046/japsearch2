package org.nagoya.controller;

import com.jfoenix.controls.JFXButton;
import io.vavr.collection.Vector;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import org.nagoya.App;
import org.nagoya.GUICommon;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.event.CustomEventType;
import org.nagoya.system.event.FXContextImp;

public class FXTaskBarControl extends FXContextImp {

    public static final CustomEventType<String> EVENT_ADD_TASK = new CustomEventType<>("EVENT_ADD_TASK");
    public static final CustomEventType<String> EVENT_REMOVE_TASK = new CustomEventType<>("EVENT_REMOVE_TASK");

    public static final CustomEventType<FXWindow> EVENT_ADD_WINDOW = new CustomEventType<>("EVENT_ADD_WINDOW");
    public static final CustomEventType<FXWindow> EVENT_REMOVE_WINDOW = new CustomEventType<>("EVENT_REMOVE_WINDOW");

    private static FXTaskBarControl instance = null;

    public static FXTaskBarControl getInstance() {
        if (instance == null) {
            instance = GUICommon.loadFXMLController(FXTaskBarControl.class);//new FXTaskBarControl();
        }
        return instance;
    }

    @FXML
    private HBox taskBar;

    @FXML
    private HBox windowBar;

    private Vector<JFXButton> taskList;
    private Vector<JFXButton> windowList;

    public FXTaskBarControl() {
        this.taskList = Vector.empty();
        this.windowList = Vector.empty();
    }

    @FXML
    public void doAvAction() {
        App.getMainScreen().setCenter(FXMoviePanelControl.getInstance().getPane());
    }

    @FXML
    public void doMangaAction() {
        App.getMainScreen().setCenter(FXMangaPanelControl.getInstance().getPane());
    }

    @Override
    public void registerListener() {
        this.registerListener(EVENT_ADD_TASK, e -> this.addTask(e.getParam()));
        this.registerListener(EVENT_REMOVE_TASK, e -> this.removeTask(e.getParam()));
        this.registerListener(EVENT_ADD_WINDOW, e -> this.addWindow(e.getParam()));
        this.registerListener(EVENT_REMOVE_WINDOW, e -> this.removeWindow(e.getParam()));
    }

    private void addTask(String taskString) {
        JFXButton button = FXFactory.buttonWithBorder(taskString, (e) -> {
        });
        //button.setTextFill(Paint.valueOf("WHITE"));
        this.taskList = this.taskList.append(button);
        this.taskBar.getChildren().setAll(this.taskList.toJavaList());
    }

    private void removeTask(String taskString) {
        this.taskList = this.taskList.removeFirst(b -> b.getText().equals(taskString));
        this.taskBar.getChildren().setAll(this.taskList.toJavaList());
    }

    private void addWindow(FXWindow win) {
        String title = win.getTitle();
        int num = 0;
        String displayTitle = title;
        while (this.checkTitleExist(displayTitle)) {
            num++;
            displayTitle = title + " " + num;
        }
        win.setTitle(displayTitle);
        JFXButton button = FXFactory.buttonWithBorder(displayTitle, null);
        button.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                win.show();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                win.hide();
                win.release();
            }
        });
        //button.setTextFill(Paint.valueOf("WHITE"));
        this.windowList = this.windowList.append(button);
        this.windowBar.getChildren().setAll(this.windowList.toJavaList());
    }

    private boolean checkTitleExist(String title) {
        return this.windowList.exists(w -> w.getText().equals(title));
    }

    private void removeWindow(FXWindow win) {
        this.windowList = this.windowList.removeFirst(b -> b.getText().equals(win.getTitle()));
        this.windowBar.getChildren().setAll(this.windowList.toJavaList());
    }
}
