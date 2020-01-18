/*
 * Copyright 2014-2016 Arnaud Nouard. All rights reserved.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.nagoya.fx.scene;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class, with the UndecoratorController, is the central class for the decoration of Transparent Stages. The Stage
 * Undecorator TODO: Themes, manage Quit (main stage)
 * <p>
 * Bugs (Mac only?): Accelerators + Fullscreen crashes JVM KeyCombination does not respect keyboard's locale. Multi
 * screen: On second screen JFX returns wrong value for MinY (300)
 */
public class Undecorator extends StackPane {

    public int SHADOW_WIDTH = 15;
    public int SAVED_SHADOW_WIDTH = 15;
    static public int RESIZE_PADDING = 7;
    static public int FEEDBACK_STROKE = 4;
    static public double ROUNDED_DELTA = 5;
    public static final Logger LOGGER = Logger.getLogger("Undecorator");
    public static ResourceBundle LOC;
    StageStyle stageStyle;
    @FXML
    private Button menu;
    @FXML
    private Button close;
    @FXML
    private Button maximize;
    @FXML
    private Button minimize;
    @FXML
    private Button resize;
    @FXML
    private Button fullscreen;
    @FXML
    private Label title;
    @FXML
    private Pane decorationRoot;
    @FXML
    private ContextMenu contextMenu;

    MenuItem maximizeMenuItem;
    CheckMenuItem fullScreenMenuItem;
    Region clientArea;
    Pane stageDecoration = null;
    Rectangle shadowRectangle;
    Pane glassPane;
    Rectangle dockFeedback;
    FadeTransition dockFadeTransition;
    Stage dockFeedbackPopup;
    ParallelTransition parallelTransition;
    Effect dsFocused;
    Effect dsNotFocused;
    UndecoratorController undecoratorController;
    Stage stage;
    Rectangle backgroundRect;
    SimpleBooleanProperty maximizeProperty;
    SimpleBooleanProperty minimizeProperty;
    SimpleBooleanProperty closeProperty;
    SimpleBooleanProperty fullscreenProperty;
    String shadowBackgroundStyleClass = "decoration-shadow";
    String decorationBackgroundStyle = "decoration-background";
    TranslateTransition fullscreenButtonTransition;
    final Rectangle internal = new Rectangle();
    final Rectangle external = new Rectangle();

    public SimpleBooleanProperty maximizeProperty() {
        return this.maximizeProperty;
    }

    public SimpleBooleanProperty minimizeProperty() {
        return this.minimizeProperty;
    }

    public SimpleBooleanProperty closeProperty() {
        return this.closeProperty;
    }

    public SimpleBooleanProperty fullscreenProperty() {
        return this.fullscreenProperty;
    }

    public Undecorator(Stage stage, Region root) {
        this(stage, root, "stagedecoration.fxml", StageStyle.UNDECORATED);
    }

    public Undecorator(Stage stag, Region clientArea, String stageDecorationFxml, StageStyle st) {
        this.create(stag, clientArea, this.getClass().getResource(stageDecorationFxml), st);
    }

    public Undecorator(Stage stag, Region clientArea, URL stageDecorationFxmlAsURL, StageStyle st) {
        this.create(stag, clientArea, stageDecorationFxmlAsURL, st);
    }

    public void create(Stage stag, Region clientArea, URL stageDecorationFxmlAsURL, StageStyle st) {
        this.stage = stag;
        this.clientArea = clientArea;

        this.setStageStyle(st);
        this.loadConfig();

        // Properties 
        this.maximizeProperty = new SimpleBooleanProperty(false);
        this.maximizeProperty.addListener((ov, t, t1) -> this.getController().maximizeOrRestore());
        this.minimizeProperty = new SimpleBooleanProperty(false);
        this.minimizeProperty.addListener((ov, t, t1) -> this.getController().minimize());

        this.closeProperty = new SimpleBooleanProperty(false);
        this.closeProperty.addListener((ov, t, t1) -> this.getController().close());
        this.fullscreenProperty = new SimpleBooleanProperty(false);
        this.fullscreenProperty.addListener((ov, t, t1) -> this.getController().setFullScreen(!this.stage.isFullScreen()));

        // The controller
        this.undecoratorController = new UndecoratorController(this);

        this.undecoratorController.setAsStageDraggable(this.stage, clientArea);

        // Focus drop shadows: radius, spread, offsets
        this.dsFocused = new DropShadow(BlurType.THREE_PASS_BOX, Color.BLACK, this.SHADOW_WIDTH, 0.1, 0, 0);
        this.dsNotFocused = new DropShadow(BlurType.THREE_PASS_BOX, Color.DARKGREY, this.SHADOW_WIDTH, 0, 0, 0);

        this.shadowRectangle = new Rectangle();
        this.shadowRectangle.layoutBoundsProperty().addListener((observable, oldBounds, newBounds) -> {
            if (Undecorator.this.SHADOW_WIDTH != 0) {
                Undecorator.this.shadowRectangle.setVisible(true);
                Undecorator.this.setShadowClip(newBounds);
            } else {
                Undecorator.this.shadowRectangle.setVisible(false);
            }
        });

        // UI part of the decoration
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(stageDecorationFxmlAsURL);
            fxmlLoader.setController(this);
            this.stageDecoration = fxmlLoader.load();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Decorations not found", ex);
        }

        this.initDecoration();

        /*
         * Resize rectangle
         */
        this.undecoratorController.setStageResizableWith(this.stage, this.decorationRoot, RESIZE_PADDING, this.SHADOW_WIDTH);

        // If not resizable (quick fix)
        if (this.fullscreen
                != null) {
            this.fullscreen.setVisible(this.stage.isResizable());
        }
        if (this.resize != null) {
            this.resize.setVisible(this.stage.isResizable());
        }
        if (this.maximize
                != null) {
            this.maximize.setVisible(this.stage.isResizable());
        }
        if (this.minimize
                != null && !this.stage.isResizable()) {
            AnchorPane.setRightAnchor(this.minimize, 34d);
        }

        // Glass Pane
        this.glassPane = new Pane();

        this.glassPane.setMouseTransparent(true);
        this.buildDockFeedbackStage();

        this.title.getStyleClass().add("undecorator-label-titlebar");
        this.shadowRectangle.getStyleClass().add(this.shadowBackgroundStyleClass);
//        resizeRect.getStyleClass().add(resizeStyleClass);
        // Do not intercept mouse events on stage's shadow
        this.shadowRectangle.setMouseTransparent(true);

        // Is it possible to apply an effect without affecting decendent?
        super.setStyle("-fx-background-color:transparent;");
        // Or this:
//        super.setStyle("-fx-background-color:transparent;-fx-border-color:white;-fx-border-radius:30;-fx-border-width:1;-fx-border-insets:"+SHADOW_WIDTH+";");
//        super.setEffect(dsFocused);
//          super.getChildren().addAll(clientArea,stageDecoration, glassPane);

        this.backgroundRect = new Rectangle();
        this.backgroundRect.getStyleClass().add(this.decorationBackgroundStyle);
        this.backgroundRect.setMouseTransparent(true);

        // Add all layers
        super.getChildren().addAll(this.shadowRectangle, this.backgroundRect, clientArea, this.stageDecoration, this.glassPane);
//        super.getChildren().addAll(shadowRectangle, backgroundRect);

        /*
         * Focused stage
         */
        this.stage.focusedProperty()
                .addListener((ov, t, t1) -> Undecorator.this.setShadowFocused(t1)
                );
        /*
         * Fullscreen
         */
        if (this.fullscreen
                != null) {
//            fullscreen.setOnMouseEntered(new EventHandler<MouseEvent>() {
//                @Override
//                public void handle(MouseEvent t) {
//                    if (stage.isFullScreen()) {
//                        fullscreen.setOpacity(1);
//                    }
//                }
//            });
//
//            fullscreen.setOnMouseExited(new EventHandler<MouseEvent>() {
//                @Override
//                public void handle(MouseEvent t) {
//                    if (stage.isFullScreen()) {
//                        fullscreen.setOpacity(0.4);
//                    }
//                }
//            });

            this.stage.fullScreenProperty().addListener((ov, t, fullscreenState) -> {
                Undecorator.this.setShadow(!fullscreenState);
                Undecorator.this.fullScreenMenuItem.setSelected(fullscreenState);
                Undecorator.this.maximize.setVisible(!fullscreenState);
                Undecorator.this.minimize.setVisible(!fullscreenState);
                if (Undecorator.this.resize != null) {
                    Undecorator.this.resize.setVisible(!fullscreenState);
                }
                if (fullscreenState) {
                    // String and icon
                    Undecorator.this.fullscreen.getStyleClass().add("decoration-button-unfullscreen");
                    Undecorator.this.fullscreen.setTooltip(new Tooltip(LOC.getString("Restore")));

                    Undecorator.this.undecoratorController.saveFullScreenBounds();
                    if (Undecorator.this.fullscreenButtonTransition != null) {
                        Undecorator.this.fullscreenButtonTransition.stop();
                    }
                    // Animate the fullscreen button
                    Undecorator.this.fullscreenButtonTransition = new TranslateTransition();
                    Undecorator.this.fullscreenButtonTransition.setDuration(Duration.millis(2000));
                    Undecorator.this.fullscreenButtonTransition.setToX(80);
                    //  fullscreen.setOpacity(0.2);
                } else {
                    // String and icon
                    Undecorator.this.fullscreen.getStyleClass().remove("decoration-button-unfullscreen");
                    Undecorator.this.fullscreen.setTooltip(new Tooltip(LOC.getString("FullScreen")));

                    Undecorator.this.undecoratorController.restoreFullScreenSavedBounds(Undecorator.this.stage);
                    //  fullscreen.setOpacity(1);
                    if (Undecorator.this.fullscreenButtonTransition != null) {
                        Undecorator.this.fullscreenButtonTransition.stop();
                    }
                    // Animate the change
                    Undecorator.this.fullscreenButtonTransition = new TranslateTransition();
                    Undecorator.this.fullscreenButtonTransition.setDuration(Duration.millis(1000));
                    Undecorator.this.fullscreenButtonTransition.setToX(0);

                }
                Undecorator.this.fullscreenButtonTransition.setNode(Undecorator.this.fullscreen);
                Undecorator.this.fullscreenButtonTransition.setOnFinished(t12 -> Undecorator.this.fullscreenButtonTransition = null);
                Undecorator.this.fullscreenButtonTransition.play();

            });
        }

        this.computeAllSizes();
    }

    /**
     * Compute the needed clip for stage's shadow border
     */
    void setShadowClip(Bounds newBounds) {
        this.external.relocate(
                newBounds.getMinX() - this.SHADOW_WIDTH,
                newBounds.getMinY() - this.SHADOW_WIDTH
        );
        this.internal.setX(this.SHADOW_WIDTH);
        this.internal.setY(this.SHADOW_WIDTH);
        this.internal.setWidth(newBounds.getWidth());
        this.internal.setHeight(newBounds.getHeight());
        this.internal.setArcWidth(this.shadowRectangle.getArcWidth());    // shadowRectangle CSS cannot be applied on this
        this.internal.setArcHeight(this.shadowRectangle.getArcHeight());

        this.external.setWidth(newBounds.getWidth() + this.SHADOW_WIDTH * 2);
        this.external.setHeight(newBounds.getHeight() + this.SHADOW_WIDTH * 2);
        Shape clip = Shape.subtract(this.external, this.internal);
        this.shadowRectangle.setClip(clip);

    }

    /**
     * Install default accelerators
     *
     * @param scene
     */
    public void installAccelerators(Scene scene) {
        // Accelerators
        if (this.stage.isResizable()) {
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN), Undecorator.this::switchFullscreen);
        }
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN), Undecorator.this::switchMinimize);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN), Undecorator.this::switchClose);
    }

    /**
     * Init the minimum/pref/max sizes in order to be reflected in the primary stage
     */
    private void computeAllSizes() {
        double minWidth = this.minWidth(this.getWidth());
        this.setMinWidth(minWidth);
        double minHeight = this.minHeight(this.getHeight());
        this.setMinHeight(minHeight);

        double prefWidth = this.prefWidth(this.getWidth());
        this.setPrefWidth(prefWidth);
        this.setWidth(prefWidth);

        double prefHeight = this.prefHeight(this.getHeight());
        this.setPrefHeight(prefHeight);
        this.setHeight(prefHeight);

        double maxWidth = this.maxWidth(this.getWidth());
        if (maxWidth > minWidth) {
            this.setMaxWidth(maxWidth);
        }
        double maxHeight = this.maxHeight(this.getHeight());
        if (maxHeight > minHeight) {
            this.setMaxHeight(maxHeight);
        }
    }
    /*
     * The sizing is based on client area's bounds.
     */

    @Override
    protected double computePrefWidth(double d) {
        return this.clientArea.getPrefWidth() + this.SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
    }

    @Override
    protected double computePrefHeight(double d) {
        return this.clientArea.getPrefHeight() + this.SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
    }

    @Override
    protected double computeMaxHeight(double d) {
        double maxHeight = this.clientArea.getMaxHeight();
        if (maxHeight > 0) {
            return maxHeight + this.SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
        }
        return maxHeight;
    }

    @Override
    protected double computeMinHeight(double d) {
        double d2 = super.computeMinHeight(d);
        d2 += this.SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
        return d2;
    }

    @Override
    protected double computeMaxWidth(double d) {
        double maxWidth = this.clientArea.getMaxWidth();
        if (maxWidth > 0) {
            return maxWidth + this.SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
        }
        return maxWidth;
    }

    @Override
    protected double computeMinWidth(double d) {
        double d2 = super.computeMinWidth(d);
        d2 += this.SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
        return d2;
    }

    public void setStageStyle(StageStyle st) {
        this.stageStyle = st;
    }

    public StageStyle getStageStyle() {
        return this.stageStyle;
    }

    /**
     * Activate fade in transition on showing event
     */
    public void setFadeInTransition() {
        super.setOpacity(0);
        this.stage.showingProperty().addListener((ov, t, t1) -> {
            if (t1) {
                FadeTransition fadeTransition = new FadeTransition(Duration.seconds(1), Undecorator.this);
                fadeTransition.setToValue(1);
                fadeTransition.play();
            }
        });
    }

    /**
     * Launch the fade out transition. Must be invoked when the application/window is supposed to be closed
     */
    public void setFadeOutTransition() {
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(1), Undecorator.this);
        fadeTransition.setToValue(0);
        fadeTransition.play();
        fadeTransition.setOnFinished(t -> {
            Undecorator.this.stage.hide();
            if (Undecorator.this.dockFeedbackPopup != null && Undecorator.this.dockFeedbackPopup.isShowing()) {
                Undecorator.this.dockFeedbackPopup.hide();
            }
        });
    }

    public void removeDefaultBackgroundStyleClass() {
        this.shadowRectangle.getStyleClass().remove(this.shadowBackgroundStyleClass);
    }

    public Rectangle getShadowNode() {
        return this.shadowRectangle;
    }

    public Rectangle getBackgroundRectangle() {
        return this.backgroundRect;
    }

    /**
     * Background opacity
     *
     * @param opacity
     */
    public void setBackgroundOpacity(double opacity) {
        this.shadowRectangle.setOpacity(opacity);
    }

    /**
     * Manage buttons and menu items
     */
    public void initDecoration() {
        MenuItem minimizeMenuItem;
        // Menu
        this.contextMenu.setAutoHide(true);
        if (this.minimize != null) { // Utility Stage
            minimizeMenuItem = new MenuItem(LOC.getString("Minimize"));
            minimizeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN));

            minimizeMenuItem.setOnAction(e -> Undecorator.this.switchMinimize());
            this.contextMenu.getItems().add(minimizeMenuItem);
        }
        if (this.maximize != null && this.stage.isResizable()) { // Utility Stage type
            this.maximizeMenuItem = new MenuItem(LOC.getString("Maximize"));
            this.maximizeMenuItem.setOnAction(e -> {
                Undecorator.this.switchMaximize();
                Undecorator.this.contextMenu.hide(); // Stay stuck on screen
            });
            this.contextMenu.getItems().addAll(this.maximizeMenuItem, new SeparatorMenuItem());
        }

        // Fullscreen
        if (this.stageStyle != StageStyle.UTILITY && this.stage.isResizable()) {
            this.fullScreenMenuItem = new CheckMenuItem(LOC.getString("FullScreen"));
            this.fullScreenMenuItem.setOnAction(e -> {
                // fake
                //maximizeProperty().set(!maximizeProperty().get());
                Undecorator.this.switchFullscreen();
            });
            this.fullScreenMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));

            this.contextMenu.getItems().addAll(this.fullScreenMenuItem, new SeparatorMenuItem());
        }

        // Close
        MenuItem closeMenuItem = new MenuItem(LOC.getString("Close"));
        closeMenuItem.setOnAction(e -> Undecorator.this.switchClose());
        closeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));

        this.contextMenu.getItems().add(closeMenuItem);

        this.menu.setOnMousePressed(t -> {
            if (Undecorator.this.contextMenu.isShowing()) {
                Undecorator.this.contextMenu.hide();
            } else {
                Undecorator.this.contextMenu.show(Undecorator.this.menu, Side.BOTTOM, 0, 0);
            }
        });

        // Close button
        this.close.setTooltip(new Tooltip(LOC.getString("Close")));
        this.close.setOnAction(t -> Undecorator.this.switchClose());

        // Maximize button
        // If changed via contextual menu
        this.maximizeProperty().addListener((ov, t, t1) -> {
            if (!Undecorator.this.stage.isResizable()) {
                return;
            }
            Tooltip tooltip = Undecorator.this.maximize.getTooltip();
            if (tooltip.getText().equals(LOC.getString("Maximize"))) {
                tooltip.setText(LOC.getString("Restore"));
                Undecorator.this.maximizeMenuItem.setText(LOC.getString("Restore"));
                Undecorator.this.maximize.getStyleClass().add("decoration-button-restore");
                if (Undecorator.this.resize != null) {
                    Undecorator.this.resize.setVisible(false);
                }
            } else {
                tooltip.setText(LOC.getString("Maximize"));
                Undecorator.this.maximizeMenuItem.setText(LOC.getString("Maximize"));
                Undecorator.this.maximize.getStyleClass().remove("decoration-button-restore");
                if (Undecorator.this.resize != null) {
                    Undecorator.this.resize.setVisible(true);
                }
            }
        });

        if (this.maximize != null) { // Utility Stage
            this.maximize.setTooltip(new Tooltip(LOC.getString("Maximize")));
            this.maximize.setOnAction(t -> Undecorator.this.switchMaximize());
        }
        if (this.fullscreen != null) { // Utility Stage
            this.fullscreen.setTooltip(new Tooltip(LOC.getString("FullScreen")));
            this.fullscreen.setOnAction(t -> Undecorator.this.switchFullscreen());
        }

        // Minimize button
        if (this.minimize != null) { // Utility Stage
            this.minimize.setTooltip(new Tooltip(LOC.getString("Minimize")));
            this.minimize.setOnAction(t -> Undecorator.this.switchMinimize());
        }
        // Transfer stage title to undecorator tiltle label

        this.title.setText(this.stage.getTitle());
    }

    public void switchFullscreen() {
        // Invoke runLater even if it's on EDT: Crash apps on Mac
        Platform.runLater(() -> Undecorator.this.undecoratorController.setFullScreen(!Undecorator.this.stage.isFullScreen()));
    }

    public void switchMinimize() {
        this.minimizeProperty().set(!this.minimizeProperty().get());
    }

    public void switchMaximize() {
        this.maximizeProperty().set(!this.maximizeProperty().get());
    }

    public void switchClose() {
        this.closeProperty().set(!this.closeProperty().get());
    }

    /**
     * Bridge to the controller to enable the specified node to drag the stage
     *
     * @param stage
     * @param node
     */
    public void setAsStageDraggable(Stage stage, Node node) {
        this.undecoratorController.setAsStageDraggable(stage, node);
    }

    /**
     * Switch the visibility of the window's drop shadow
     */
    protected void setShadow(boolean shadow) {
        // Already removed?
        if (!shadow && this.shadowRectangle.getEffect() == null) {
            return;
        }
        // From fullscreen to maximize case
        if (shadow && this.maximizeProperty.get()) {
            return;
        }
        if (!shadow) {
            this.shadowRectangle.setEffect(null);
            this.SAVED_SHADOW_WIDTH = this.SHADOW_WIDTH;
            this.SHADOW_WIDTH = 0;
        } else {
            this.shadowRectangle.setEffect(this.dsFocused);
            this.SHADOW_WIDTH = this.SAVED_SHADOW_WIDTH;
        }
    }

    /**
     * Set on/off the stage shadow effect
     *
     * @param b
     */
    protected void setShadowFocused(boolean b) {
        // Do not change anything while maximized (in case of dialog closing for instance)
        if (this.stage.isFullScreen()) {
            return;
        }
        if (this.maximizeProperty().get()) {
            return;
        }
        if (b) {
            this.shadowRectangle.setEffect(this.dsFocused);
        } else {
            this.shadowRectangle.setEffect(this.dsNotFocused);
        }
    }

    /**
     * Set the layout of different layers of the stage
     */
    @Override
    public void layoutChildren() {
        Bounds b = super.getLayoutBounds();
        double w = b.getWidth();
        double h = b.getHeight();
        ObservableList<Node> list = super.getChildren();
//        ROUNDED_DELTA=shadowRectangle.getArcWidth()/4;
        ROUNDED_DELTA = 0;
        for (Node node : list) {
            if (node == this.shadowRectangle) {
                this.shadowRectangle.setWidth(w - this.SHADOW_WIDTH * 2);
                this.shadowRectangle.setHeight(h - this.SHADOW_WIDTH * 2);
                this.shadowRectangle.setX(this.SHADOW_WIDTH);
                this.shadowRectangle.setY(this.SHADOW_WIDTH);
            } else if (node == this.backgroundRect) {
                this.backgroundRect.setWidth(w - this.SHADOW_WIDTH * 2);
                this.backgroundRect.setHeight(h - this.SHADOW_WIDTH * 2);
                this.backgroundRect.setX(this.SHADOW_WIDTH);
                this.backgroundRect.setY(this.SHADOW_WIDTH);
            } else if (node == this.stageDecoration) {
                this.stageDecoration.resize(w - this.SHADOW_WIDTH * 2 - ROUNDED_DELTA * 2, h - this.SHADOW_WIDTH * 2 - ROUNDED_DELTA * 2);
                this.stageDecoration.setLayoutX(this.SHADOW_WIDTH + ROUNDED_DELTA);
                this.stageDecoration.setLayoutY(this.SHADOW_WIDTH + ROUNDED_DELTA);
            } //            else if (node == resizeRect) {
            //                resizeRect.setWidth(w - SHADOW_WIDTH * 2);
            //                resizeRect.setHeight(h - SHADOW_WIDTH * 2);
            //                resizeRect.setLayoutX(SHADOW_WIDTH);
            //                resizeRect.setLayoutY(SHADOW_WIDTH);
            //            } 
            else {
                node.resize(w - this.SHADOW_WIDTH * 2 - ROUNDED_DELTA * 2, h - this.SHADOW_WIDTH * 2 - ROUNDED_DELTA * 2);
                node.setLayoutX(this.SHADOW_WIDTH + ROUNDED_DELTA);
                node.setLayoutY(this.SHADOW_WIDTH + ROUNDED_DELTA);
//                node.resize(w - SHADOW_WIDTH * 2 - RESIZE_PADDING * 2, h - SHADOW_WIDTH * 2 - RESIZE_PADDING * 2);
//                node.setLayoutX(SHADOW_WIDTH + RESIZE_PADDING);
//                node.setLayoutY(SHADOW_WIDTH + RESIZE_PADDING);
            }
        }
    }

    public int getShadowBorderSize() {
        return this.SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
    }

    public UndecoratorController getController() {
        return this.undecoratorController;
    }

    public Stage getStage() {
        return this.stage;
    }

    protected Pane getGlassPane() {
        return this.glassPane;
    }

    public void addGlassPane(Node node) {
        this.glassPane.getChildren().add(node);
    }

    public void removeGlassPane(Node node) {
        this.glassPane.getChildren().remove(node);
    }

    /**
     * Returns the decoration (buttons...)
     *
     * @return
     */
    public Pane getStageDecorationNode() {
        return this.stageDecoration;
    }

    /**
     * Prepare Stage for dock feedback display
     */
    void buildDockFeedbackStage() {
        this.dockFeedbackPopup = new Stage(StageStyle.TRANSPARENT);
        this.dockFeedback = new Rectangle(0, 0, 100, 100);
        this.dockFeedback.setArcHeight(10);
        this.dockFeedback.setArcWidth(10);
        this.dockFeedback.setFill(Color.TRANSPARENT);
        this.dockFeedback.setStroke(Color.BLACK);
        this.dockFeedback.setStrokeWidth(2);
        this.dockFeedback.setCache(true);
        this.dockFeedback.setCacheHint(CacheHint.SPEED);
        this.dockFeedback.setEffect(new DropShadow(BlurType.TWO_PASS_BOX, Color.BLACK, 10, 0.2, 3, 3));
        this.dockFeedback.setMouseTransparent(true);
        BorderPane borderpane = new BorderPane();
        borderpane.setStyle("-fx-background-color:transparent"); //J8
        borderpane.setCenter(this.dockFeedback);
        Scene scene = new Scene(borderpane);
        scene.setFill(Color.TRANSPARENT);
        this.dockFeedbackPopup.setScene(scene);
        this.dockFeedbackPopup.sizeToScene();
    }

    /**
     * Activate dock feedback on screen's bounds
     *
     * @param x
     * @param y
     */
    public void setDockFeedbackVisible(double x, double y, double width, double height) {
        this.dockFeedbackPopup.setX(x);
        this.dockFeedbackPopup.setY(y);

        this.dockFeedback.setX(this.SHADOW_WIDTH);
        this.dockFeedback.setY(this.SHADOW_WIDTH);
        this.dockFeedback.setHeight(height - this.SHADOW_WIDTH * 2);
        this.dockFeedback.setWidth(width - this.SHADOW_WIDTH * 2);

        this.dockFeedbackPopup.setWidth(width);
        this.dockFeedbackPopup.setHeight(height);

        this.dockFeedback.setOpacity(1);
        this.dockFeedbackPopup.show();

        this.dockFadeTransition = new FadeTransition();
        this.dockFadeTransition.setDuration(Duration.millis(200));
        this.dockFadeTransition.setNode(this.dockFeedback);
        this.dockFadeTransition.setFromValue(0);
        this.dockFadeTransition.setToValue(1);
        this.dockFadeTransition.setAutoReverse(true);
        this.dockFadeTransition.setCycleCount(3);

        this.dockFadeTransition.play();

    }

    public void setDockFeedbackInvisible() {
        if (this.dockFeedbackPopup.isShowing()) {
            this.dockFeedbackPopup.hide();
            if (this.dockFadeTransition != null) {
                this.dockFadeTransition.stop();
            }
        }
    }

    void loadConfig() {
        Properties prop = new Properties();

        try {
            prop.load(Undecorator.class.getResourceAsStream("/skin/undecorator.properties"));
            this.SHADOW_WIDTH = Integer.parseInt(prop.getProperty("window-shadow-width"));
            RESIZE_PADDING = Integer.parseInt(prop.getProperty("window-resize-padding"));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error while loading confguration flie", ex);
        }
        LOC = ResourceBundle.getBundle("localization", Locale.getDefault());

    }
}
