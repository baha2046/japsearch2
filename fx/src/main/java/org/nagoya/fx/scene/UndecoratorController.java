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

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.util.logging.Level;

/**
 * @author in-sideFX
 */
public class UndecoratorController {

    static final int DOCK_NONE = 0x0;
    static final int DOCK_LEFT = 0x1;
    static final int DOCK_RIGHT = 0x2;
    static final int DOCK_TOP = 0x4;
    int lastDocked = DOCK_NONE;
    private static double initX = -1;
    private static double initY = -1;
    private static double newX;
    private static double newY;
    private static int RESIZE_PADDING;
    private static int SHADOW_WIDTH;
    Undecorator undecorator;
    BoundingBox savedBounds, savedFullScreenBounds;
    boolean maximized = false;
    static boolean isMacOS = false;
    static final int MAXIMIZE_BORDER = 20;  // Allow double click to maximize on top of the Scene

    {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("mac") != -1) {
            isMacOS = true;
        }
    }

    public UndecoratorController(Undecorator ud) {
        this.undecorator = ud;
    }


    /*
     * Actions
     */
    protected void maximizeOrRestore() {

        Stage stage = this.undecorator.getStage();

        if (this.maximized) {
            this.restoreSavedBounds(stage, false);
            this.undecorator.setShadow(true);
            this.savedBounds = null;
            this.maximized = false;
        } else {
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = screensForRectangle.get(0);
            Rectangle2D visualBounds = screen.getVisualBounds();

            this.savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

            this.undecorator.setShadow(false);

            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());
            this.maximized = true;
        }
    }

    public void saveBounds() {
        Stage stage = this.undecorator.getStage();
        this.savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    public void saveFullScreenBounds() {
        Stage stage = this.undecorator.getStage();
        this.savedFullScreenBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    public void restoreSavedBounds(Stage stage, boolean fullscreen) {

        stage.setX(this.savedBounds.getMinX());
        stage.setY(this.savedBounds.getMinY());
        stage.setWidth(this.savedBounds.getWidth());
        stage.setHeight(this.savedBounds.getHeight());
        this.savedBounds = null;
    }

    public void restoreFullScreenSavedBounds(Stage stage) {

        stage.setX(this.savedFullScreenBounds.getMinX());
        stage.setY(this.savedFullScreenBounds.getMinY());
        stage.setWidth(this.savedFullScreenBounds.getWidth());
        stage.setHeight(this.savedFullScreenBounds.getHeight());
        this.savedFullScreenBounds = null;
    }

    protected void setFullScreen(boolean value) {
        Stage stage = this.undecorator.getStage();
        stage.setFullScreen(value);
    }

    public void close() {
        final Stage stage = this.undecorator.getStage();
        Platform.runLater(() -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));

    }

    public void minimize() {

        if (!Platform.isFxApplicationThread()) // Ensure on correct thread else hangs X under Unbuntu
        {
            Platform.runLater(UndecoratorController.this::_minimize);
        } else {
            this._minimize();
        }
    }

    private void _minimize() {
        Stage stage = this.undecorator.getStage();
        stage.setIconified(true);
    }

    /**
     * Stage resize management
     *
     * @param stage
     * @param node
     * @param PADDING
     * @param SHADOW
     */
    public void setStageResizableWith(final Stage stage, final Node node, int PADDING, int SHADOW) {

        RESIZE_PADDING = PADDING;
        SHADOW_WIDTH = SHADOW;
        // Maximize on double click
        node.setOnMouseClicked(mouseEvent -> {
            if (UndecoratorController.this.undecorator.getStageStyle() != StageStyle.UTILITY && !stage.isFullScreen() && mouseEvent.getClickCount() > 1) {
                if (mouseEvent.getSceneY() - SHADOW_WIDTH < MAXIMIZE_BORDER) {
                    UndecoratorController.this.undecorator.maximizeProperty().set(!UndecoratorController.this.undecorator.maximizeProperty().get());
                    mouseEvent.consume();
                }
            }
        });

        node.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.isPrimaryButtonDown()) {
                initX = mouseEvent.getScreenX();
                initY = mouseEvent.getScreenY();
                mouseEvent.consume();
            }
        });
        node.setOnMouseDragged(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown() || (initX == -1 && initY == -1)) {
                return;
            }
            if (stage.isFullScreen()) {
                return;
            }
            /*
             * Long press generates drag event!
             */
            if (mouseEvent.isStillSincePress()) {
                return;
            }
            if (UndecoratorController.this.maximized) {
                // Remove maximized state
                UndecoratorController.this.undecorator.maximizeProperty.set(false);
                return;
            } // Docked then moved, so restore state
            else if (UndecoratorController.this.savedBounds != null) {
                UndecoratorController.this.undecorator.setShadow(true);
            }

            newX = mouseEvent.getScreenX();
            newY = mouseEvent.getScreenY();
            double deltax = newX - initX;
            double deltay = newY - initY;

            Cursor cursor = node.getCursor();
            if (Cursor.E_RESIZE.equals(cursor)) {
                UndecoratorController.this.setStageWidth(stage, stage.getWidth() + deltax);
                mouseEvent.consume();
            } else if (Cursor.NE_RESIZE.equals(cursor)) {
                if (UndecoratorController.this.setStageHeight(stage, stage.getHeight() - deltay)) {
                    UndecoratorController.this.setStageY(stage, stage.getY() + deltay);
                }
                UndecoratorController.this.setStageWidth(stage, stage.getWidth() + deltax);
                mouseEvent.consume();
            } else if (Cursor.SE_RESIZE.equals(cursor)) {
                UndecoratorController.this.setStageWidth(stage, stage.getWidth() + deltax);
                UndecoratorController.this.setStageHeight(stage, stage.getHeight() + deltay);
                mouseEvent.consume();
            } else if (Cursor.S_RESIZE.equals(cursor)) {
                UndecoratorController.this.setStageHeight(stage, stage.getHeight() + deltay);
                mouseEvent.consume();
            } else if (Cursor.W_RESIZE.equals(cursor)) {
                if (UndecoratorController.this.setStageWidth(stage, stage.getWidth() - deltax)) {
                    stage.setX(stage.getX() + deltax);
                }
                mouseEvent.consume();
            } else if (Cursor.SW_RESIZE.equals(cursor)) {
                if (UndecoratorController.this.setStageWidth(stage, stage.getWidth() - deltax)) {
                    stage.setX(stage.getX() + deltax);
                }
                UndecoratorController.this.setStageHeight(stage, stage.getHeight() + deltay);
                mouseEvent.consume();
            } else if (Cursor.NW_RESIZE.equals(cursor)) {
                if (UndecoratorController.this.setStageWidth(stage, stage.getWidth() - deltax)) {
                    stage.setX(stage.getX() + deltax);
                }
                if (UndecoratorController.this.setStageHeight(stage, stage.getHeight() - deltay)) {
                    UndecoratorController.this.setStageY(stage, stage.getY() + deltay);
                }
                mouseEvent.consume();
            } else if (Cursor.N_RESIZE.equals(cursor)) {
                if (UndecoratorController.this.setStageHeight(stage, stage.getHeight() - deltay)) {
                    UndecoratorController.this.setStageY(stage, stage.getY() + deltay);
                }
                mouseEvent.consume();
            }

        });
        node.setOnMouseMoved(mouseEvent -> {
            if (UndecoratorController.this.maximized) {
                UndecoratorController.this.setCursor(node, Cursor.DEFAULT);
                return; // maximized mode does not support resize
            }
            if (stage.isFullScreen()) {
                return;
            }
            if (!stage.isResizable()) {
                return;
            }
            double x = mouseEvent.getX();
            double y = mouseEvent.getY();
            Bounds boundsInParent = node.getBoundsInParent();
            if (UndecoratorController.this.isRightEdge(x, y, boundsInParent)) {
                if (y < RESIZE_PADDING + SHADOW_WIDTH) {
                    UndecoratorController.this.setCursor(node, Cursor.NE_RESIZE);
                } else if (y > boundsInParent.getHeight() - (double) (RESIZE_PADDING + SHADOW_WIDTH)) {
                    UndecoratorController.this.setCursor(node, Cursor.SE_RESIZE);
                } else {
                    UndecoratorController.this.setCursor(node, Cursor.E_RESIZE);
                }

            } else if (UndecoratorController.this.isLeftEdge(x, y, boundsInParent)) {
                if (y < RESIZE_PADDING + SHADOW_WIDTH) {
                    UndecoratorController.this.setCursor(node, Cursor.NW_RESIZE);
                } else if (y > boundsInParent.getHeight() - (double) (RESIZE_PADDING + SHADOW_WIDTH)) {
                    UndecoratorController.this.setCursor(node, Cursor.SW_RESIZE);
                } else {
                    UndecoratorController.this.setCursor(node, Cursor.W_RESIZE);
                }
            } else if (UndecoratorController.this.isTopEdge(x, y, boundsInParent)) {
                UndecoratorController.this.setCursor(node, Cursor.N_RESIZE);
            } else if (UndecoratorController.this.isBottomEdge(x, y, boundsInParent)) {
                UndecoratorController.this.setCursor(node, Cursor.S_RESIZE);
            } else {
                UndecoratorController.this.setCursor(node, Cursor.DEFAULT);
            }
        });
    }

    /**
     * Under Windows, the undecorator Stage could be been dragged below the Task bar and then no way to grab it again...
     * On Mac, do not drag above the menu bar
     *
     * @param y
     */
    void setStageY(Stage stage, double y) {
        try {
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            if (screensForRectangle.size() > 0) {
                Screen screen = screensForRectangle.get(0);
                Rectangle2D visualBounds = screen.getVisualBounds();
                if (y < visualBounds.getHeight() - 30 && y + SHADOW_WIDTH >= visualBounds.getMinY()) {
                    stage.setY(y);
                }
            }
        } catch (Exception e) {
            Undecorator.LOGGER.log(Level.SEVERE, "setStageY issue", e);
        }
    }

    boolean setStageWidth(Stage stage, double width) {
        if (width >= stage.getMinWidth()) {
            stage.setWidth(width);
            initX = newX;
            return true;
        }
        return false;
    }

    boolean setStageHeight(Stage stage, double height) {
        if (height >= stage.getMinHeight()) {
            stage.setHeight(height);
            initY = newY;
            return true;
        }
        return false;
    }

    /**
     * Allow this node to drag the Stage
     *
     * @param stage
     * @param node
     */
    public void setAsStageDraggable(final Stage stage, final Node node) {

        node.setOnMouseClicked(new EventHandler<MouseEvent>() {
            // Maximize on double click
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (UndecoratorController.this.undecorator.getStageStyle() != StageStyle.UTILITY && !stage.isFullScreen() && stage.isResizable() && mouseEvent.getClickCount() > 1) {
                    if (mouseEvent.getSceneY() - SHADOW_WIDTH < MAXIMIZE_BORDER) {
                        UndecoratorController.this.undecorator.maximizeProperty().set(!UndecoratorController.this.undecorator.maximizeProperty().get());
                        mouseEvent.consume();
                    }
                }
            }
        });
        node.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.isPrimaryButtonDown()) {
                    initX = mouseEvent.getScreenX();
                    initY = mouseEvent.getScreenY();
                    mouseEvent.consume();
                } else {
                    initX = -1;
                    initY = -1;
                }
            }
        });
        node.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (!mouseEvent.isPrimaryButtonDown() || initX == -1) {
                    return;
                }
                if (stage.isFullScreen()) {
                    return;
                }
                /*
                 * Long press generates drag event!
                 */
                if (mouseEvent.isStillSincePress()) {
                    return;
                }
                if (UndecoratorController.this.maximized) {
                    // Remove Maximized state
                    UndecoratorController.this.undecorator.maximizeProperty.set(false);
                    // Center 
                    stage.setX(mouseEvent.getScreenX() - stage.getWidth() / 2);
                    stage.setY(mouseEvent.getScreenY() - SHADOW_WIDTH);
                } // Docked then moved, so restore state
                else if (UndecoratorController.this.savedBounds != null) {
                    UndecoratorController.this.restoreSavedBounds(stage, false);
                    UndecoratorController.this.undecorator.setShadow(true);
                    // Center
                    stage.setX(mouseEvent.getScreenX() - stage.getWidth() / 2);
                    stage.setY(mouseEvent.getScreenY() - SHADOW_WIDTH);
                }
                double newX = mouseEvent.getScreenX();
                double newY = mouseEvent.getScreenY();
                double deltax = newX - initX;
                double deltay = newY - initY;
                initX = newX;
                initY = newY;
                UndecoratorController.this.setCursor(node, Cursor.HAND);
                stage.setX(stage.getX() + deltax);
                UndecoratorController.this.setStageY(stage, stage.getY() + deltay);

                UndecoratorController.this.testDock(stage, mouseEvent);
                mouseEvent.consume();

                ///////////////////////
//                Robot robot = null;
//                try {
//                    robot = new Robot();
//                } catch (AWTException ex) {
//                }
//                stage.getScene().getRoot().setVisible(false);
//                BufferedImage screenShot = robot.createScreenCapture(new Rectangle((int) stage.getX(), (int) stage.getY(), (int) stage.getWidth(), (int) 40));
//                stage.getScene().getRoot().setVisible(true);
//                Image background = SwingFXUtils.toFXImage(screenShot, null);
//
//                ImagePattern imagePattern = new ImagePattern(background);
////                undecorator.getBackgroundRectangle().setEffect(new BoxBlur());
//                undecorator.getBackgroundRectangle().setFill(imagePattern);
                ////////////////
            }
        });
        node.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if (stage.isResizable()) {
                    UndecoratorController.this.undecorator.setDockFeedbackInvisible();
                    UndecoratorController.this.setCursor(node, Cursor.DEFAULT);
                    initX = -1;
                    initY = -1;
                    UndecoratorController.this.dockActions(stage, t);
                }
            }
        });

        node.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                //setCursor(node, Cursor.DEFAULT);
            }
        });

    }

    /**
     * (Humble) Simulation of Windows behavior on screen's edges Feedbacks
     */
    void testDock(Stage stage, MouseEvent mouseEvent) {

        if (!stage.isResizable()) {
            return;
        }

        int dockSide = this.getDockSide(mouseEvent);
        // Dock Left
        if (dockSide == DOCK_LEFT) {
            if (this.lastDocked == DOCK_LEFT) {
                return;
            }
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = screensForRectangle.get(0);
            Rectangle2D visualBounds = screen.getVisualBounds();
            // Dock Left
            double x = visualBounds.getMinX();
            double y = visualBounds.getMinY();
            double width = visualBounds.getWidth() / 2;
            double height = visualBounds.getHeight();

            this.undecorator.setDockFeedbackVisible(x, y, width, height);
            this.lastDocked = DOCK_LEFT;
        } // Dock Right
        else if (dockSide == DOCK_RIGHT) {
            if (this.lastDocked == DOCK_RIGHT) {
                return;
            }
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = screensForRectangle.get(0);
            Rectangle2D visualBounds = screen.getVisualBounds();
            // Dock Right (visualBounds = (javafx.geometry.Rectangle2D) Rectangle2D [minX = 1440.0, minY=300.0, maxX=3360.0, maxY=1500.0, width=1920.0, height=1200.0])
            double x = visualBounds.getMinX() + visualBounds.getWidth() / 2;
            double y = visualBounds.getMinY();
            double width = visualBounds.getWidth() / 2;
            double height = visualBounds.getHeight();

            this.undecorator.setDockFeedbackVisible(x, y, width, height);
            this.lastDocked = DOCK_RIGHT;
        } // Dock top
        else if (dockSide == DOCK_TOP) {
            if (this.lastDocked == DOCK_TOP) {
                return;
            }
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = screensForRectangle.get(0);
            Rectangle2D visualBounds = screen.getVisualBounds();
            // Dock Left
            double x = visualBounds.getMinX();
            double y = visualBounds.getMinY();
            double width = visualBounds.getWidth();
            double height = visualBounds.getHeight();
            this.undecorator.setDockFeedbackVisible(x, y, width, height);
            this.lastDocked = DOCK_TOP;
        } else {
            this.undecorator.setDockFeedbackInvisible();
            this.lastDocked = DOCK_NONE;
        }
    }

    /**
     * Based on mouse position returns dock side
     *
     * @param mouseEvent
     * @return DOCK_LEFT, DOCK_RIGHT, DOCK_TOP
     */
    int getDockSide(MouseEvent mouseEvent) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = 0;
        double maxY = 0;

        // Get "big" screen bounds
        ObservableList<Screen> screens = Screen.getScreens();
        for (Screen screen : screens) {
            Rectangle2D visualBounds = screen.getVisualBounds();
            minX = Math.min(minX, visualBounds.getMinX());
            minY = Math.min(minY, visualBounds.getMinY());
            maxX = Math.max(maxX, visualBounds.getMaxX());
            maxY = Math.max(maxY, visualBounds.getMaxY());
        }
        // Dock Left
        if (mouseEvent.getScreenX() == minX) {
            return DOCK_LEFT;
        } else if (mouseEvent.getScreenX() >= maxX - 1) { // MaxX returns the width? Not width -1 ?!
            return DOCK_RIGHT;
        } else if (mouseEvent.getScreenY() <= minY) {   // Mac menu bar
            return DOCK_TOP;
        }
        return 0;
    }

    /**
     * (Humble) Simulation of Windows behavior on screen's edges Actions
     */
    void dockActions(Stage stage, MouseEvent mouseEvent) {
        ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Screen screen = screensForRectangle.get(0);
        Rectangle2D visualBounds = screen.getVisualBounds();
        // Dock Left
        if (mouseEvent.getScreenX() == visualBounds.getMinX()) {
            this.savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            // Respect Stage Max size
            double width = visualBounds.getWidth() / 2;
            if (stage.getMaxWidth() < width) {
                width = stage.getMaxWidth();
            }

            stage.setWidth(width);

            double height = visualBounds.getHeight();
            if (stage.getMaxHeight() < height) {
                height = stage.getMaxHeight();
            }

            stage.setHeight(height);
            this.undecorator.setShadow(false);
        } // Dock Right (visualBounds = [minX = 1440.0, minY=300.0, maxX=3360.0, maxY=1500.0, width=1920.0, height=1200.0])
        else if (mouseEvent.getScreenX() >= visualBounds.getMaxX() - 1) { // MaxX returns the width? Not width -1 ?!
            this.savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

            stage.setX(visualBounds.getWidth() / 2 + visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            // Respect Stage Max size
            double width = visualBounds.getWidth() / 2;
            if (stage.getMaxWidth() < width) {
                width = stage.getMaxWidth();
            }

            stage.setWidth(width);

            double height = visualBounds.getHeight();
            if (stage.getMaxHeight() < height) {
                height = stage.getMaxHeight();
            }

            stage.setHeight(height);
            this.undecorator.setShadow(false);
        } else if (mouseEvent.getScreenY() <= visualBounds.getMinY()) { // Mac menu bar
            this.undecorator.maximizeProperty.set(true);
        }

    }

    public boolean isRightEdge(double x, double y, Bounds boundsInParent) {
        if (x < boundsInParent.getWidth() && x > boundsInParent.getWidth() - RESIZE_PADDING) {
            return true;
        }
        return false;
    }

    public boolean isTopEdge(double x, double y, Bounds boundsInParent) {
        if (y >= 0 && y < RESIZE_PADDING) {
            return true;
        }
        return false;
    }

    public boolean isBottomEdge(double x, double y, Bounds boundsInParent) {
        if (y < boundsInParent.getHeight() && y > boundsInParent.getHeight() - RESIZE_PADDING) {
            return true;
        }
        return false;
    }

    public boolean isLeftEdge(double x, double y, Bounds boundsInParent) {
        if (x >= 0 && x < RESIZE_PADDING) {
            return true;
        }
        return false;
    }

    public void setCursor(Node n, Cursor c) {
        n.setCursor(c);
    }
}
