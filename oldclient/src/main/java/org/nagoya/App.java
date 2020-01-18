package org.nagoya;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXTextField;
import io.vavr.control.Option;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.control.MaskerPane;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.controller.*;
import org.nagoya.controller.siteparsingprofile.specific.ArzonCSSQuery;
import org.nagoya.controller.siteparsingprofile.specific.DmmCSSQuery;
import org.nagoya.controller.siteparsingprofile.specific.DugaCSSQuery;
import org.nagoya.controller.siteparsingprofile.specific.JavBusCSSQuery;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.fx.scene.Undecorator;
import org.nagoya.fx.scene.UndecoratorScene;
import org.nagoya.io.Setting;
import org.nagoya.preferences.GuiSettings;
import org.nagoya.system.Systems;
import org.nagoya.system.cache.IconCache;
import org.nagoya.system.database.MovieScanner;
import org.nagoya.video.player.VLCPlayerNano;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Stage currentStage = null;
    private static BorderPane mainScreen;

    @Contract(pure = true)
    public static BorderPane getMainScreen() {
        return mainScreen;
    }

    @Contract(pure = true)
    public static Option<Stage> getCurrentStage() {
        return Option.of(currentStage);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(@NotNull Stage stage) {

        if (this.getParameters() != null && this.getParameters().getNamed() != null
                && "true".equals(this.getParameters().getNamed().get("debug"))) {
            org.nagoya.commons.GUICommon.DEBUG_MODE = true;
        }

        stage.setTitle("JAV DATA SEARCHER");

        currentStage = stage;
        //scene = new Scene(loadFXML("primary"));

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        UndecoratorScene.setClassicDecoration();
        UndecoratorScene loadingScene = new UndecoratorScene(stage, this.initLoadingScene());
        //loadingScene.setFadeInTransition();

        stage.setScene(loadingScene);

        Undecorator undecorator = loadingScene.getUndecorator();
        stage.setWidth(undecorator.getPrefWidth());
        stage.setHeight(undecorator.getPrefHeight());

        GUICommon.debugMessage("stage setWidth " + undecorator.getPrefWidth());
        GUICommon.debugMessage("stage setHeight " + undecorator.getPrefHeight());

        stage.show();
        stage.toFront();

        Systems.useExecutors(() ->
        {
            this.loadSettings();
            IconCache.setIconProvider(GuiSettings.getInstance().getUseContentBasedTypeIcons() ? IconCache.IconProviderType.CONTENT : IconCache.IconProviderType.SYSTEM);

            Region mainPane = this.initMainScene();

            Platform.runLater(() ->
            {
                UndecoratorScene mainScene = new UndecoratorScene(stage, mainPane);
                GUICommon.loadCssFile(mainScene, "font.css");
                GUICommon.loadProjectCss(mainScene);

                stage.setScene(mainScene);
                stage.sizeToScene();
                stage.centerOnScreen();

                GUICommon.debugMessage("---------- FONT ----------");
                Font.getFontNames().forEach(GUICommon::debugMessage);
                GUICommon.debugMessage("---------- FONT ----------");

                MovieScanner.getInstance().doAutoScan();
                Systems.getDirectorySystem().changePathTo(GuiSettings.getInstance().getLastUsedDirectory(), Option.none());
            });
        });

        //this makes all stages close and the app exit when the main stage is closed
        stage.setOnCloseRequest(e -> {
            GUICommon.debugMessage("Stage - OnCloseRequest()");
            Systems.shutdown();
            Platform.exit();
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        GUICommon.debugMessage("App - stop()");
        VLCPlayerNano.getInstance().shutdown();
    }

    private void loadSettings() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT);
        Gson gson = gsonBuilder.create();

        Path path = Paths.get("");

        if (Files.exists(path.resolve("dmmQuery.ini"))) {
            //UtilCommon.saveStringToFile(path.resolve("dmmQuery.ini"), gson.toJson(new DmmCSSQuery()));
            DmmCSSQuery dummy = Setting.readSetting(DmmCSSQuery.class, "dmmQuery.ini", gson);
        }

        if (Files.exists(path.resolve("arzonQuery.ini"))) {
            ArzonCSSQuery dummy = Setting.readSetting(ArzonCSSQuery.class, "arzonQuery.ini", gson);
        }

        if (Files.exists(path.resolve("dugaQuery.ini"))) {
            DugaCSSQuery dummy = Setting.readSetting(DugaCSSQuery.class, "dugaQuery.ini", gson);
        }

        if (Files.exists(path.resolve("javbusQuery.ini"))) {
            JavBusCSSQuery dummy = Setting.readSetting(JavBusCSSQuery.class, "javbusQuery.ini", gson);
        }
    }

    /*static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }*/

    @NotNull
    @Contract(" -> new")
    private Region initLoadingScene() {
        GridPane pane = new GridPane();
        pane.setAlignment(Pos.CENTER);
        //pane.setStyle("-fx-background-color:#336699; -fx-opacity:1;");
        pane.setStyle("-fx-background-color: transparent");
        pane.setPrefWidth(1400);
        pane.setPrefHeight(760);
        pane.add(new Text("Loading..."), 0, 0);
        //Group root = new Group(pane);
        return pane;//new Scene(root, 1400, 760);
    }

    @NotNull
    @Contract(" -> new")
    private Region initMainScene() {
        mainScreen = new BorderPane();
        mainScreen.setPrefWidth(1400);
        mainScreen.setPrefHeight(760);
        mainScreen.getStyleClass().add("custom-window");
        //mainScreen.setStyle("-fx-background-color: transparent");
        //mainScreen.setStyle("-fx-background-color: #336699;");
        mainScreen.setBorder(Border.EMPTY);
        mainScreen.setBackground(Background.EMPTY);

        mainScreen.setCenter(FXMoviePanelControl.getInstance().getPane());
        mainScreen.setLeft(FXFileListControl.getInstance().getPane());
        //mainScreen.setRight(FXArtPanelControl.getInstance().getPane());
        mainScreen.setTop(FXTaskBarControl.getInstance().getPane());

        FXCoreController.addContext(FXMoviePanelControl.getInstance());
        FXCoreController.addContext(FXFileListControl.getInstance());
        //FXCoreController.getInstance().addContext(FXArtPanelControl.getInstance());
        FXCoreController.addContext(FXTaskBarControl.getInstance());
        FXCoreController.addContext(FXMangaPanelControl.getInstance());

        //  FXMangaList.getInstance().loadWeb();

        StackPane basePane = new StackPane();
        basePane.setAlignment(Pos.CENTER);
        basePane.setPrefWidth(1400);
        basePane.setPrefHeight(760);
        basePane.setMaxWidth(1400);
        basePane.setMaxHeight(760);
        basePane.setStyle("-fx-background-color: transparent");

        BorderPane vlcPane = VLCPlayerNano.getInstance().getPane();

        MaskerPane progressPane = GUICommon.getProgressPane();
        basePane.getChildren().setAll(mainScreen, progressPane, vlcPane);
        progressPane.setVisible(false);

        Systems.getDialogPool().initialize(5, basePane);

        return basePane;
    }

    private void setupInfoBox(StackPane base) {
        JFXTextField textField = FXFactory.textField("", 300);
        JFXDialog infoDialog = new JFXDialog(base, textField, JFXDialog.DialogTransition.BOTTOM);
        infoDialog.getDialogContainer().setAlignment(Pos.BOTTOM_RIGHT);
    }


}