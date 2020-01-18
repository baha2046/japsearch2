package org.nagoya.preferences;

import com.jfoenix.controls.JFXButton;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.nagoya.preferences.options.*;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class CustomOptions {
    private Vector<Tuple2<String, Object>> optionMap;
    private final String textString;
    private Vector<Mono<HBox>> hBoxes = Vector.empty();
    private StackPane dialogPane = null;

    public CustomOptions(String text) {
        this.textString = text;
        this.optionMap = Vector.empty();
        this.addRow(text, "label-main");
    }

    public void addRow(String text) {
        this.addRow(text, "label-sub");
    }

    public void addRow(String text, String css) {
        var label = new Label(text);

        if (!css.equals("")) {
            label.getStyleClass().add(css);
        }
        this.hBoxes = this.hBoxes.append(
                Mono.fromSupplier(this::newHBox)
                        .cache()
                        .doOnNext(m -> m.getChildren().setAll(label)));
    }

    @NotNull
    private HBox newHBox() {
        var hbox = new HBox();
        hbox.getStyleClass().add("hbox");
        return hbox;
    }

    public void addOption(String key, Boolean dVal, Consumer<Boolean> setFunc, String text) {
        BooleanOption option = new BooleanOption(key, dVal, setFunc);
        option.setOptionText(text);
        this.optionMap = this.optionMap.append(Tuple.of(key, option));
        this.addOptionToRow(option);
        setFunc.accept(option.getOption());
    }

    public void addOption(String key, Integer dVal, Consumer<Integer> setFunc, String text) {
        IntegerOption option = new IntegerOption(key, dVal, setFunc);
        option.setOptionText(text);
        this.optionMap = this.optionMap.append(Tuple.of(key, option));
        this.addOptionToRow(option);
        setFunc.accept(option.getOption());
    }

    public void addOption(String key, IntegerProperty property, String text) {
        this.addOption(key, property.get(), property::setValue, text);
        property.addListener((ov, o, n) -> {
            this.optionMap.filter(t -> t._1.equals(key)).map(t -> (IntegerOption) t._2).peek(op -> op.updateOption(n.intValue()));
        });
    }

    public void addOption(String key, DoubleProperty property, String text) {
        this.addOption(key, (int) property.get(), property::setValue, text);
        property.addListener((ov, o, n) -> {
            //GUICommon.debugMessage("updateVal " + n);
            this.optionMap.filter(t -> t._1.equals(key)).map(t -> (IntegerOption) t._2).peek(op -> op.updateOption(n.intValue()));
        });
    }

    public void addOption(String key, String dVal, Consumer<String> setFunc, String text) {
        StringOption option = new StringOption(key, dVal, setFunc);
        option.setOptionText(text);
        this.optionMap = this.optionMap.append(Tuple.of(key, option));
        this.addOptionToRow(option);
        setFunc.accept(option.getOption());
    }

    public void addOption(String key, StringProperty property, String text) {
        this.addOption(key, property.get(), property::setValue, text);
    }

    public void addOption(GuiSettings.Key key, String text) {
        PathOption option = new PathOption(key);
        option.setOptionText(text);
        this.optionMap = this.optionMap.append(Tuple.of("", option));
        this.addOptionToRow(option);
    }

    public void addOption(JFXButton button) {
        this.optionMap = this.optionMap.append(Tuple.of("", button));
        this.addButtonToRow(button);
    }

    private void addOptionToRow(OptionBase<?> option) {
        var hBoxMono = this.hBoxes.last().doOnNext(m -> m.getChildren().add(option.toButton(this.dialogPane)));
        this.hBoxes = this.hBoxes.dropRight(1).append(hBoxMono);
    }

    private void addButtonToRow(JFXButton button) {
        var hBoxMono = this.hBoxes.last().doOnNext(m -> m.getChildren().add(button));
        this.hBoxes = this.hBoxes.dropRight(1).append(hBoxMono);
    }

    public void setDialogPane(StackPane dialogPane) {
        this.dialogPane = dialogPane;
    }

    public void addAllPane(@NotNull VBox vBox) {
        vBox.getChildren().add(new Separator());
        vBox.getChildren().addAll(this.hBoxes.map(Mono::block).asJava());
    }

    public Vector<Tuple2<String, Object>> getOptionMap() {
        return this.optionMap;
    }

    public String getTextString() {
        return this.textString;
    }
}

/*class SingleOption {
    protected String keyString = "";
}*/

