package org.nagoya.view.dialog;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.control.Option;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.controls.FXWebViewWindow;
import org.nagoya.model.dataitem.MakerData;
import org.nagoya.system.Systems;
import org.nagoya.system.database.MovieDB;
import org.nagoya.system.dialog.DialogBuilder;

import java.nio.file.Path;

public class FXMakerEditDialog {

    public static void show(@NotNull Option<MakerData> makerData, Path path) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefSize(870, 370);
        scrollPane.setMinSize(870, 370);

        JFXTextField txtName = GUICommon.textField(makerData.map(MakerData::getMakerName).getOrElse(""), 500);
        JFXTextField txtDesc = GUICommon.textField(makerData.map(MakerData::getMakerDesc).getOrElse(""), 500);
        JFXTextField txtLogo = GUICommon.textField(makerData.map(MakerData::getMakerLogoUrl).getOrElse(""), 500);
        JFXTextField txtDmmUrl = GUICommon.textField(makerData.map(MakerData::getMakerDMMUrl).getOrElse(""), 500);
        JFXTextField txtJavUrl = GUICommon.textField(makerData.map(MakerData::getMakerJavUrl).getOrElse(""), 500);
        JFXButton btnDmm = GUICommon.buttonWithBorder("  Dmm  ", (e) -> {
            scrollPane.setContent(FXWebViewWindow.loadWithoutWindow(txtDmmUrl.getText(), Option.none(), Option.none()));
        });
        JFXButton btnJav = GUICommon.buttonWithBorder("  Jav  ", (e) -> {
            scrollPane.setContent(FXWebViewWindow.loadWithoutWindow(txtJavUrl.getText(), Option.none(), Option.none()));
        });

        VBox vBox = GUICommon.vbox(10, scrollPane, txtName, txtDesc, txtLogo,
                GUICommon.hbox(15, txtDmmUrl, btnDmm),
                GUICommon.hbox(15, txtJavUrl, btnJav));

        Runnable saveAction = () -> {
            MakerData newMakerData = MakerData.of(path, txtName.getText(), txtDesc.getText(), txtDmmUrl.getText(), txtJavUrl.getText(), txtLogo.getText());
            MovieDB.makerDB().putData(path.toString(), newMakerData);
            Systems.getDirectorySystem().reloadDirectory(Option.none());
        };

        DialogBuilder.create()
                .heading("[ Maker Editor ]")
                .body(vBox)
                .buttonSaveCancel(saveAction)
                .build().show();
    }
}
