package org.nagoya.system.dialog;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.controller.FXTaskBarControl;
import org.nagoya.fx.scene.Undecorator;
import org.nagoya.fx.scene.UndecoratorScene;
import org.nagoya.system.event.CustomEventSourceImp;

public class FXWindow {
    private static Map<String, FXWindow> windowBuilderMap = HashMap.empty();

    @NotNull
    @Contract("_ -> new")
    public static FXWindow create(WindowBuilder windowBuilder) {
        return new FXWindow(windowBuilder);
    }

    public static FXWindow createSingle(@NotNull WindowBuilder windowBuilder) {
        return getWindow(windowBuilder.getTitle()).getOrElse(() -> {
            FXWindow w = new FXWindow(windowBuilder);
            windowBuilderMap = windowBuilderMap.put(w.title, w);
            return w;
        });
    }

    public static Insets getDefaultInset() {
        return new Insets(30, 10, 10, 10);
    }

    public static Option<FXWindow> getWindow(String title) {
        return windowBuilderMap.get(title);
    }

    private final StackPane root;
    private final boolean releaseOnHide;

    private Stage stage;
    private Runnable runOnClose;
    private String title;
    private double x = -1;
    private double y = -1;

    private double widthOffset = 0;
    private double heightOffset = 0;
    private double prefWidth = 0;
    private double prefHeight = 0;


    FXWindow(@NotNull WindowBuilder windowBuilder) {
        this.title = windowBuilder.getTitle();
        this.releaseOnHide = windowBuilder.isReleaseOnHide();
        this.runOnClose = windowBuilder.getRunOnClose();
        var screen = windowBuilder.getContainer();

        this.prefWidth = windowBuilder.getPrefWidth();
        this.prefHeight = windowBuilder.getPrefHeight();

        this.root = new StackPane(screen);

        double minWidth = screen.getMinWidth();
        double minHeight = screen.getMinHeight();

        if (!windowBuilder.isSizeToScene()) {
            this.root.setPrefSize(this.prefWidth, this.prefHeight);
            this.root.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }

        this.stage = new Stage(windowBuilder.getStageStyle());
        this.stage.setResizable(windowBuilder.isResizable());

        if (windowBuilder.isShowTitle()) {
            this.stage.setTitle(this.title);
        }

        this.stage.setOnCloseRequest((e) -> {
            this.runOnClose.run();
            if (this.releaseOnHide) {
                this.release();
            }
        });

       /* if (!windowBuilder.isSizeToScene()) {
            this.container.setPrefWidth(windowBuilder.getPrefWidth());
            this.container.setPrefHeight(windowBuilder.getPrefHeight());
        }*/

        //UndecoratorScene.setClassicDecoration();
        UndecoratorScene scene = new UndecoratorScene(this.stage, this.stage.getStyle(), this.root, null);
        scene.setFadeInTransition();

        GUICommon.loadProjectCss(scene);
        this.root.getStyleClass().add("custom-window");

        this.stage.setScene(scene);

        Undecorator undecorator = scene.getUndecorator();

        if (this.root.getPrefWidth() == Region.USE_COMPUTED_SIZE) {
            this.stage.sizeToScene();
        } else {
            this.stage.setWidth(undecorator.getPrefWidth());
            this.stage.setHeight(undecorator.getPrefHeight());
        }

        GUICommon.debugMessage("--------- Build Window ---------");
        GUICommon.debugMessage("PrefWidth " + screen.prefWidth(-1));
        GUICommon.debugMessage("PreHeight " + screen.prefHeight(-1));
        GUICommon.debugMessage("undecorator PrefWidth " + undecorator.getPrefWidth());
        GUICommon.debugMessage("undecorator PreHeight " + undecorator.getPrefHeight());
        GUICommon.debugMessage("Stage Width " + this.stage.getWidth());
        GUICommon.debugMessage("Stage Height " + this.stage.getHeight());
        GUICommon.debugMessage("Root Width " + this.root.getWidth());
        GUICommon.debugMessage("Root Height " + this.root.getHeight());
        GUICommon.debugMessage("--------- ------------ ---------");

        if (windowBuilder.isResizable()) {
            this.widthOffset = this.stage.getWidth() - this.prefWidth;
            this.heightOffset = this.stage.getHeight() - this.prefHeight;

            if (minWidth > 0 && minHeight > 0) {
                this.stage.setMinWidth(minWidth);
                this.stage.setMinHeight(minHeight);
            }
        }
        //screen.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        /*if (!windowBuilder.isSizeToScene()) {
            Undecorator undecorator = scene.getUndecorator();
            GUICommon.debugMessage("container W " + this.container.getPrefWidth());
            GUICommon.debugMessage("container H " + this.container.getPrefHeight());
            GUICommon.debugMessage("undecorator W " + undecorator.getPrefWidth());
            GUICommon.debugMessage("undecorator H " + undecorator.getPrefHeight());
            this.stage.setMinWidth(undecorator.getMinWidth());
            this.stage.setMinHeight(undecorator.getMinHeight());
            this.stage.setWidth(undecorator.getPrefWidth());
            this.stage.setHeight(undecorator.getPrefHeight());
        } else {
            //this.stage.sizeToScene();
        }*/
        //stage.setX((Screen.getPrimary().getBounds().getWidth() - stage.getWidth())/2);

        CustomEventSourceImp.fire(FXTaskBarControl.EVENT_ADD_WINDOW, this);
    }

    public FXWindow pos(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public FXWindow runOnClose(Runnable runOnClose) {
        this.runOnClose = runOnClose;
        return this;
    }

    public void show() {
        if (this.stage != null) {
            GUICommon.debugMessage("Window - show");
            if (!this.stage.isShowing()) {
                GUICommon.runOnFx(() -> {
                    this.stage.show();
                    if (this.x == -1 || this.y == -1) {
                        this.stage.centerOnScreen();
                    } else {
                        this.stage.setX(this.x);
                        this.stage.setY(this.y);
                    }
                });
            } else {
                GUICommon.runOnFx(() -> {
                    if (this.stage.isIconified()) {
                        this.stage.setIconified(false);
                    }
                    this.stage.toFront();
                });
                GUICommon.debugMessage("stage W " + this.stage.getWidth());
                GUICommon.debugMessage("stage H " + this.stage.getHeight());
            }
        }
    }

    public void hide() {
        if (this.stage != null) {
            if (this.stage.isShowing()) {
                this.x = this.stage.getX();
                this.y = this.stage.getY();
                GUICommon.debugMessage("Window - hide");
                GUICommon.runOnFx(() -> {
                    this.stage.hide();
                    if (this.releaseOnHide) {
                        this.release();
                    }
                });
            }
        }
    }

    public void release() {
        if (this.stage != null) {
            GUICommon.debugMessage("Window - release");
            CustomEventSourceImp.fire(FXTaskBarControl.EVENT_REMOVE_WINDOW, this);
            this.stage.close();
            this.stage = null;
        }
        windowBuilderMap.find(t -> t._2.equals(this))
                .peek(t -> windowBuilderMap = windowBuilderMap.remove(t._1));
    }

    public void terminate() {
        this.hide();
        if (this.root.getScene() != null) {
            this.root.getScene().getWindow().getOnCloseRequest().handle(null);
        }
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public StackPane getRoot() {
        return this.root;
    }

    public Stage getStage() {
        return this.stage;
    }

    public double getWidth() {
        return this.stage.getWidth() - this.widthOffset;
    }

    public double getHeight() {
        return this.stage.getHeight() - this.heightOffset;
    }
}
