package org.nagoya;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import io.vavr.collection.Stream;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.MaskerPane;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.fx.scene.FXUtil;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.system.FXMLController;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Common utility methods used in the various GUI classes. Methods should be static.
 */

public class GUICommon {
    private static String PROJECT_CSS = null;
    private static MaskerPane progressPane = null;

    public static final Boolean DEBUG_MODE = true;//Boolean.getBoolean("debug");

    public static MaskerPane getProgressPane() {
        if (null == progressPane) {
            progressPane = new MaskerPane();
        }
        return progressPane;
    }

    public static void loadProjectCss(Scene scene) {
        if (PROJECT_CSS == null) {
            PROJECT_CSS = GUICommon.class.getResource("/org/nagoya/css/japsearch.css").toExternalForm();
        }
        scene.getStylesheets().add(PROJECT_CSS);
    }

    public static void setLoading(boolean loading) {
        Platform.runLater(() -> getProgressPane().setVisible(loading));
    }

    public static Image getProgramIcon() {
        //initialize the icons used in the program
        URL programIconURL = GUICommon.class.getResource("/res/AppIcon.png");

        //Used for icon in the title bar
        Image programIcon = null;
        try {
            programIcon = ImageIO.read(programIconURL);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return programIcon;
    }

    private static io.vavr.control.Option<Node> loadFXML(@NotNull FXMLLoader fxmlLoader) {
        return io.vavr.control.Try.of(fxmlLoader::<Node>load)
                .onFailure(Throwable::printStackTrace)
                .toOption();
    }

    private static <T extends FXMLController> T getController(@NotNull FXMLLoader fxmlLoader) {
        return loadFXML(fxmlLoader).map(node -> fxmlLoader.<T>getController().<T>setPane(node))
                .getOrNull();
    }

    @Nullable
    public static <T extends FXMLController> T loadFXMLController(String fxml) {
        FXMLLoader fxmlLoader = new FXMLLoader(GUICommon.class.getResource(fxml));
        return getController(fxmlLoader);
    }

    @Nullable
    public static <T extends FXMLController> T loadFXMLController(@NotNull Class<T> targetClass) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(targetClass.getResource("/org/nagoya/fxml/" + targetClass.getSimpleName()
                .replace("Control", "").replace("View", "") + ".fxml"));
        GUICommon.debugMessage("Loading FXML from : " + fxmlLoader.getLocation().toString());
        return getController(fxmlLoader);
    }

    public static <T> void loadFXMLRoot(String fxml, @NotNull T controller, Node root) {
        FXMLLoader fxmlLoader = new FXMLLoader(controller.getClass().getResource(fxml));
        loadFXMLRoot(fxmlLoader, controller, root);
    }

    public static <T extends Node> void loadFXMLRoot(@NotNull T controller) {
        loadFXMLRoot(controller, controller);
    }

    public static <T> void loadFXMLRoot(@NotNull T controller, Node root) {
        FXMLLoader fxmlLoader = new FXMLLoader(controller.getClass().getResource("/org/nagoya/fxml/" + controller.getClass().getSimpleName() + ".fxml"));
        loadFXMLRoot(fxmlLoader, controller, root);
    }

    private static <T> void loadFXMLRoot(@NotNull FXMLLoader fxmlLoader, T controller, Node root) {
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(controller);
        loadFXML(fxmlLoader);
    }

    public static void loadCssFile(@NotNull Parent node, String file) {
        node.getStylesheets().add(GUICommon.class.getResource("/org/nagoya/css/" + file).toExternalForm());
    }

    public static String loadCss(String file) {
        return GUICommon.class.getResource(file).toExternalForm();
    }

    public static void loadCssFile(@NotNull Scene scene, String file) {
        scene.getStylesheets().add(GUICommon.class.getResource("/org/nagoya/css/" + file).toExternalForm());
    }

    @Contract("_ -> param1")
    public static Path checkAndCreateDir(Path dir) {
        if (Files.isRegularFile(dir)) {
            try {
                Files.delete(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (Files.notExists(dir)) {
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return dir;
    }


    public static Stream<File> getDirectoryPathStream(Path path) {
        File[] list = io.vavr.control.Try.of(() -> path.toFile().listFiles()).toOption().getOrElse(new File[0]);
        return Stream.of(list);
    }

    public static void errorDialog(String errorMsg) {
        DialogBuilder.create().heading("[ Error ]").body(errorMsg).build().show();
    }

    public static void errorDialog(@NotNull Throwable throwable) {
        throwable.printStackTrace();
        errorDialog(throwable.toString());
    }

    public static void showDialog(String heading, Node body, String strBtnCancel, String strBtnOkay, Runnable runnable) {
        Systems.getDialogPool().showDialog(heading, body, strBtnCancel, strBtnOkay, runnable);
    }

    public static void loadImageFromURL(URL url, double w, double h, Consumer<javafx.scene.image.Image> imageConsumer) {
        if (imageConsumer != null) {
            FxThumb.monoLoadImageFromURLImp(url, w, h, FxThumb.DEFAULT_USER_AGENT, "", customReferrer(url, null))
                    .subscribeOn(FxThumb.getAsyncScheduler())
                    .subscribe(i -> Platform.runLater(() -> imageConsumer.accept(i)),
                            e -> GUICommon.debugMessage(() -> ">> Error at GUICommon::loadImageFromURL : " + e.toString()));
        }
    }

    public static void loadImageFromLocal(Path path, double w, double h, Consumer<javafx.scene.image.Image> imageConsumer) {
        if (imageConsumer != null) {
            FxThumb.monoLoadImageFromPathImp(path, w, h)
                    .subscribeOn(FxThumb.getAsyncScheduler())
                    .subscribe(i -> Platform.runLater(() -> imageConsumer.accept(i)),
                            e -> GUICommon.debugMessage(() -> ">> Error at GUICommon::loadImageFromLocal : " + e.toString()));
        }
    }

    public static void writeToObList(String text, io.vavr.control.Option<ObservableList<String>> observableListOption) {
        FXUtil.runOnFx(() -> observableListOption.peek(t -> {
            t.add(text);
            if (t.size() > 500) {
                t.remove(0, 100);
            }
        }));
    }

    public static void writeToObListWithoutNewLine(String text, io.vavr.control.Option<ObservableList<String>> observableListOption) {
        //if (!Thread.currentThread().getName().equals("JavaFX Application Thread")) {
        FXUtil.runOnFx(() -> {
            observableListOption.filter(a -> !a.isEmpty()).peek(a -> a.remove(a.size() - 1));
            observableListOption.peek(a -> a.add(text));
        });
    }

    public static void runOnFx(Runnable runnable) {
        FXUtil.runOnFx(runnable);
    }

    public static Function<URL, String> referrerModifyFunction = (url) -> customReferrer(url, null);

    public static String customReferrer(URL url, URL refer) {
        String referrerString = "";

        if (refer != null) {
            referrerString = refer.toString();
        }

        if (url != null) {
            String urlString = url.toString();

            if (urlString.contains(".arzon.jp")) {
                referrerString = "https://www.arzon.jp/item_140797.html";

                GUICommon.debugMessage("referer = true");
            }
        }

        return referrerString;
    }

    public static void debugMessage(String string) {
        if (DEBUG_MODE) {
            System.out.println(string);
        }
    }

    public static void debugThread() {
        if (DEBUG_MODE) {
            System.out.println(Thread.currentThread().getName());
        }
    }

    public static void debugMessage(@NotNull Supplier<String> string) {
        if (DEBUG_MODE) {
            System.out.println(string.get());
        }
    }

    public static JFXButton button(String text, EventHandler<ActionEvent> eventHandler) {
        return FXFactory.button(text, eventHandler);
    }

    public static JFXButton buttonWithBorder(String text, EventHandler<ActionEvent> eventHandler) {
        return FXFactory.buttonWithBorder(text, eventHandler);
    }

    public static JFXButton buttonWithCheck(String text, EventHandler<ActionEvent> eventHandler) {
        JFXButton jfxButton = buttonWithBorder(text, eventHandler);
        jfxButton.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.CHECK_CIRCLE));
        return jfxButton;
    }

    public static VBox vbox(double spacing, Node... elements) {
        return FXFactory.vbox(spacing, elements);
    }

    public static HBox hbox(double spacing, Node... elements) {
        return FXFactory.hbox(spacing, elements);
    }

    public static JFXTextField textField(String text, double width) {
        return FXFactory.textField(text, width);
    }

    public static TextField textFieldWithBorder(String text, double width) {
        TextField textField = FXFactory.textField(text, width);
        textField.getStyleClass().add("n-text-field");
        return textField;
    }
}
