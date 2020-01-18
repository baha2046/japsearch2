package org.nagoya.view;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import org.nagoya.controller.FXMangaPanelControl;
import org.nagoya.controller.mangaparsingprofile.BigirlParser;
import org.nagoya.controller.mangaparsingprofile.KissgoddessParser;
import org.nagoya.controller.mangaparsingprofile.NhentaiParser;
import org.nagoya.system.FXMLController;

import java.net.URL;
import java.util.ResourceBundle;

public class FXMangaPanelView extends FXMLController {
    @FXML
    JFXButton btnClear, btnSearch;
    @FXML
    Pane detailPane;
    @FXML
    JFXTextField txtUrl;

    private FXMangaPanelControl controller;

    public FXMangaPanelView() {
    }

    public void setController(FXMangaPanelControl controller) {
        this.controller = controller;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void actionClear() {
        this.txtUrl.setText("");
    }

    public void actionSearch() {
        this.controller.executeSearch(this.txtUrl.getText());
    }

    @FXML
    void bigirlAction() {
        new BigirlParser().loadSet("");
    }

    @FXML
    void kissgoddessAction() {
        new KissgoddessParser().loadSet("");
    }

    @FXML
    void nhentaiAction() {
        new NhentaiParser().loadSet("");
    }
}
