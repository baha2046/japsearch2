package org.nagoya.view.editor;

import com.jfoenix.controls.JFXButton;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.controls.FXEditableButton;
import org.nagoya.controls.FXImageFlowWindow;
import org.nagoya.controls.FXImageViewerWindow;
import org.nagoya.controls.FXScrollPane;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.model.MovieFolder;
import org.nagoya.model.MovieV2;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.preferences.CustomOptions;
import org.nagoya.preferences.GuiSettings;
import org.nagoya.preferences.options.OptionBase;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.view.dialog.FXMovieGridWindow;
import org.nagoya.view.dialog.FXSelectPathDialog;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FXSettingEditor {
    private static Vector<CustomOptions> optionsVector = Vector.empty();

    public static void add(CustomOptions customOptions) {
        optionsVector = optionsVector.append(customOptions);
    }

    private static final CustomOptions PATH_OPTIONS = getPathOptions();

    private static CustomOptions getPathOptions() {
        CustomOptions customOptions = new CustomOptions("Path");
        customOptions.addRow("AV Path");
        customOptions.addOption(GuiSettings.Key.avDirectory, "");
        customOptions.addRow("Doujinshi Path");
        customOptions.addOption(GuiSettings.Key.doujinshiDirectory, "");
        customOptions.addRow("Manga Path");
        customOptions.addOption(GuiSettings.Key.mangaDirectory, "");
        customOptions.addRow("Photo Path");
        customOptions.addOption(GuiSettings.Key.photoDirectory, "");
        customOptions.addRow("Download Path");
        customOptions.addOption(GuiSettings.Key.downloadDirectory, "");
        return customOptions;
    }

    public static void showSettingEditor() {
        VBox vBox = new VBox();
        vBox.getStyleClass().add("vbox");

        optionsVector
                .append(PATH_OPTIONS)
                .append(MovieV2.OPTIONS)
                .append(MovieFolder.OPTIONS)
                .append(FxThumb.OPTIONS)
                .append(FXMovieGridWindow.OPTIONS)
                .append(FXImageViewerWindow.OPTIONS)
                .append(FXImageFlowWindow.OPTIONS)
                .forEach(o -> o.addAllPane(vBox));

        /*CheckBox Sel2 = new CheckBox("Is First Word FileID");
        Sel2.setSelected(preferences.getIsFirstWordOfFileID());
        dialogVbox1.getChildren().addAll(Sel2);*/

        FXScrollPane fxScrollPane = new FXScrollPane(vBox);
        fxScrollPane.setFixWidth();
        fxScrollPane.setId("setting-editor");
        //fxScrollPane.setPrefSize(900, 580);

        DialogBuilder.create()
                .heading("[ Setting ]")
                .body(fxScrollPane)
                .button("Cancel", "Apply Change", () -> {
                    //preferences.setIsFirstWordOfFileID(Sel2.isSelected());
                    Systems.getPreferences().saveSetting();
                })
                .build()
                .show();
    }

    private static Option<HBox> getOptionFromClass(@NotNull CustomOptions customOptions) {
        Vector<JFXButton> buttonStream = customOptions.getOptionMap()
                .flatMap(t -> {
                    if (t._1.equals(Boolean.class) || t._1.equals(Integer.class) || t._1.equals(String.class)) {
                        return Option.of(((OptionBase<?>) t._2).toButton());
                    } else if (t._1.equals(JFXButton.class)) {
                        return Option.of(((JFXButton) t._2));
                    }
                    return Option.none();
                });

        if (buttonStream.length() > 0) {
            var hBox = GUICommon.hbox(15, getText(customOptions.getTextString() + " : "));
            buttonStream.forEach(hBox.getChildren()::add);
            return Option.of(hBox);
        }

        return Option.none();
    }

    private static Option<HBox> getOptionFromClass(Class<?> targetClass) {

        Stream<JFXButton> buttonStream = Stream.of("Option0", "Option1", "Option2")
                .map(o -> Tuple.of(o, UtilCommon.tryGetMethod(targetClass, "get" + o)))
                .filter(t -> t._2.isDefined())
                .map(t -> t.map2(Option::get))
                .flatMap(t -> {
                    if (t._2.getReturnType().equals(Boolean.class)) {
                        return getBooleanOption(targetClass, t);
                    } else if (t._2.getReturnType().equals(JFXButton.class)) {
                        return UtilCommon.tryInvoke(t._2, JFXButton.class, null);
                    }
                    return Option.none();
                });

        if (buttonStream.length() > 0) {
            var hBox = GUICommon.hbox(15, new Text(targetClass.getSimpleName() + " : "));
            buttonStream.forEach(hBox.getChildren()::add);
            return Option.of(hBox);
        }

        return Option.none();
    }

    private static Option<JFXButton> getBooleanOption(Class<?> targetClass, @NotNull Tuple2<String, Method> tuple2) {
        return Option.of(Tuple.of(tuple2._2,
                UtilCommon.tryGetMethod(targetClass, "set" + tuple2._1, tuple2._2.getReturnType()),
                UtilCommon.tryGetMethod(targetClass, "get" + tuple2._1 + "Text")))
                .flatMap(t -> t._2.isEmpty() || t._3.isEmpty() ? Option.none() : Option.of(Tuple.of(t._1, t._2.get(), t._3.get())))
                .map(t -> getBoolButton(UtilCommon.tryInvoke(t._3, String.class, null).getOrElse(""),
                        (v) -> UtilCommon.tryInvoke(t._2, Void.class, null, v),
                        () -> UtilCommon.tryInvoke(t._1, Boolean.class, null).get()));
    }

    @NotNull
    private static JFXButton getBoolButton(String caption, Consumer<Boolean> set, @NotNull Supplier<Boolean> supplier) {
        FXEditableButton<Boolean> button = new FXEditableButton<>(caption, Boolean::parseBoolean, supplier.get());
        button.setOnEditAction((EventHandler<ActionEvent>) (e) -> {
            button.setEditableVal(!button.getEditableVal());
            set.accept(button.getEditableVal());
        });
        button.setMinWidth(200);
        return button;
    }

    @NotNull
    private static HBox genButtonPath(String caption, GuiSettings.Key key) {
        var setting = GuiSettings.getInstance();
        JFXButton button = FXFactory.buttonWithBorder(setting.getDirectory(key).toString(), (e) -> {
            Option<Path> getPath = FXSelectPathDialog.show(caption, setting.getDirectory(key));
            getPath.peek(p -> setting.setDirectory(key, p));
            ((JFXButton) e.getSource()).setText(setting.getDirectory(key).toString());
        });
        button.setMinWidth(250);
        return FXFactory.hbox(15, getText(caption), button);
    }

    private static Label getText(String t) {
        Label text = new Label(t);
        text.setMinWidth(150);
        return text;
    }
}
