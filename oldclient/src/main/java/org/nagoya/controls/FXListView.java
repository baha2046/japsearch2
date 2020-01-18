package org.nagoya.controls;

import io.vavr.collection.Vector;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;
import org.nagoya.NagoyaResource;

public class FXListView<T> extends ListView<T> {

    private static final String STYLE_CLASS = "custom_list_view_";
    private static final String USER_AGENT_STYLESHEET = NagoyaResource.load("css/customListView.css").toExternalForm();

    private boolean bTrans = false;
    private boolean bBorder = true;
    private final Vector<String> oldStyleClass;

    public FXListView() {
        super();

        this.oldStyleClass = Vector.ofAll(this.getStyleClass());

        this.getStylesheets().add(USER_AGENT_STYLESHEET);
        this.applyCustomCss();
        /*this.skinProperty().addListener(it -> {
            // first bind, then add new scrollbars, otherwise the new bars will be found
            this.bindScrollBars();
            this.getChildren().addAll(this.vBar, this.hBar);
        });*/
    }
/*
    @Override
    public String getUserAgentStylesheet() {
        return USER_AGENT_STYLESHEET;
    }*/

    public void disableHorizontalScroll() {

    }

    public void setBorder(boolean border) {
        this.bBorder = border;
        this.applyCustomCss();
    }

    public void setBackgroundTransparent(boolean transparent) {
        this.bTrans = transparent;
        this.applyCustomCss();
    }

    public void applyCustomCss() {
        String style1 = STYLE_CLASS + (this.bBorder ? "border_true" : "border_false");
        String style2 = STYLE_CLASS + (this.bTrans ? "bg_false" : "bg_true");
        this.getStyleClass().setAll(this.oldStyleClass.append(style1).append(style2).toJavaList());

        //this.setId(STYLE_CLASS + (this.bBorder ? "b" : "n") + (this.bTrans ? "n" : "b"));
    }

    public void fitToPane(@NotNull Pane pane) {
        this.setPrefSize(pane.getPrefWidth(), pane.getPrefHeight());
        pane.getChildren().add(this);
    }
}