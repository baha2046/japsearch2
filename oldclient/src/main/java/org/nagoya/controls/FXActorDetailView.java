package org.nagoya.controls;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;
import io.vavr.control.Option;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.model.dataitem.*;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.view.FXImageViewWrapper;
import org.nagoya.view.customcell.MovieDBListCell;
import org.nagoya.view.dialog.FXMovieJavBusGridWindow;

import java.net.URL;
import java.time.LocalDate;

public class FXActorDetailView extends AnchorPane {
    @FXML
    JFXTextField txtName, txtUrlDmm, txtUrlJav;

    @FXML
    Label txtYomi, txtBirth, txtSize, txtAge;

    @FXML
    ImageView imgDmm, imgJav, imgLocal, imgHd, imgCustom;

    @FXML
    Pane listMovie;

    private final FXListView<DirectoryEntry> listViewMovie;
    private final FXImageViewWrapper imageViewWrapper;

    private StackPane actorWindow = null;

    private ActorV2 actorV2;

    public FXActorDetailView() {
        GUICommon.loadFXMLRoot(this);

        this.listViewMovie = new FXListView<>();
        this.listViewMovie.getStyleClass().add("list-view-transparent");
        this.listViewMovie.setPrefSize(this.listMovie.getPrefWidth(), this.listMovie.getPrefHeight());
        this.listViewMovie.setCellFactory((ListView<DirectoryEntry> l) -> new MovieDBListCell());
        this.listViewMovie.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.listViewMovie.setOnMouseClicked((event) -> {
            if (event.getClickCount() == 2) {
                DirectoryEntry entry = this.listViewMovie.getSelectionModel().getSelectedItem();
                int idx = this.listViewMovie.getSelectionModel().getSelectedIndex();
                GUICommon.debugMessage(String.valueOf(idx));

                if (entry != null) {
                    DirectoryEntry parent = entry.getEntryParent();

                    if (parent != null) {
                        parent.setLastSelectedIndex(parent.getChildrenEntry().indexOf(entry));
                        Systems.getDirectorySystem().changePathTo(parent, Option.none());
                    }
                }
            }
        });

        this.listMovie.getChildren().add(this.listViewMovie);

        this.imageViewWrapper = new FXImageViewWrapper();
    }

    public void setWindowContainer(StackPane actorWindow) {
        this.actorWindow = actorWindow;
    }

    @FXML
    public void doOpenWiki() {
        String cleanName = this.actorV2.getName();

        int idx = this.actorV2.getName().indexOf("（");
        if (idx != -1) {
            cleanName = this.actorV2.getName().substring(0, idx);
        }

        FXWebViewWindow.show("https://ja.wikipedia.org/wiki/" + cleanName, Option.none(), Option.none());
    }

    @FXML
    public void doOpenJav() {
        Option<String> javUrl = ActorJavBusID.getActorWebUrl(this.actorV2.getActorJavBusID());//this.actorV2.getRecordURL(ActorV2.Source.JAVBUS);
        //javUrl.peek(url -> FXWebViewDialog.show(url, Option.of(FXWebViewDialog::modifyJavBus)));
        javUrl.peek(FXMovieJavBusGridWindow::urlAndShow);
    }

    @FXML
    public void doFindDmm() {
        ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> {
            this.actorV2.updateWebID(true);
            this.actorV2.updateFromWeb();
            Platform.runLater(this::updateView);
        });
    }

    @FXML
    public void doOpenDmm() {
        Option<String> dmmUrl = ActorDmmID.getActorWebUrl(this.actorV2.getActorDmmID());//this.actorV2.getRecordURL(ActorV2.Source.DMM);
        dmmUrl.peek(url -> FXWebViewWindow.show(url, Option.none(), Option.none()));
    }

    private void updateView() {
        this.updateText();
        this.updateDmm();
        this.updateJav();
    }


    @FXML
    public void doCustomize() {
        ActorV2.CustomData customData = this.actorV2.getCustomData();
        JFXTextField txtYomi = FXFactory.textField(customData.getCustomYomi(), 200);

        JFXCheckBox unknownBirth = new JFXCheckBox();
        unknownBirth.setText("unknown");
        unknownBirth.setSelected(customData.getCustomBirthDate() == null);

        String imgUrl = customData.getCustomImage().map(FxThumb::getThumbURL).map(URL::toString).getOrElse("");
        JFXTextField txtImage = FXFactory.textField(imgUrl, 400);

        DatePicker datePicker = new DatePicker();
        if (customData.getCustomBirthDate() != null) {
            datePicker.setValue(customData.getCustomBirthDate());
        } else {
            datePicker.setValue(LocalDate.now());
        }

        HBox hBox = FXFactory.hbox(15, datePicker, unknownBirth);

        JFXTextField txtT = FXFactory.textField(customData.getTall(), 40);
        JFXTextField txtB = FXFactory.textField(customData.getBSize(), 40);
        JFXTextField txtW = FXFactory.textField(customData.getWSize(), 40);
        JFXTextField txtH = FXFactory.textField(customData.getHSize(), 40);
        JFXTextField txtC = FXFactory.textField(customData.getCupSize(), 40);
        Separator s = new Separator(Orientation.VERTICAL);

        HBox sizeHBox = FXFactory.hbox(15, txtT, txtB, txtW, txtH, s, txtC);

        VBox vBox = FXFactory.vbox(15, txtYomi, hBox, sizeHBox, txtImage);

        DialogBuilder.create()
                .heading("[ Custom Data ]")
                .body(vBox)
                .container(this.actorWindow)
                .buttonSaveCancel(() -> {
                    customData.setCustomYomi(txtYomi.getText());

                    customData.setTall(txtT.getText());
                    customData.setBSize(txtB.getText());
                    customData.setWSize(txtW.getText());
                    customData.setHSize(txtH.getText());
                    customData.setCupSize(txtC.getText());

                    customData.setCustomBirthDate(unknownBirth.isSelected() ? null : datePicker.getValue());

                    if (!txtImage.getText().equals("")) {
                        customData.setCustomImage(FxThumb.of(txtImage.getText()));
                    } else {
                        customData.setCustomImage(Option.none());
                    }
                    this.actorV2.setCustomData(Option.of(customData));
                    this.actorV2.refreshView();
                    this.updateText();
                })
                .build().show();
    }

    @FXML
    public void doSaveDmm() {
        if (this.txtUrlDmm.getText().equals("")) {
            this.actorV2.removeRecord(ActorV2.Source.DMM);
            this.actorV2.setActorDmmID(new ActorWebID());
            this.updateDmm();
        } else {
            //Option<FxThumb> dmmImage = this.actorV2.getRecordImage(ActorV2.Source.DMM);
            //this.actorV2.addRecord(ActorV2.Source.DMM, this.txtUrlDmm.getText(), dmmImage, "");
            var dmmId = this.actorV2.getActorDmmID();
            dmmId.set(this.txtUrlDmm.getText());
            this.actorV2.setActorDmmID(dmmId);
            ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> {
                this.actorV2.updateFromWeb();
                Platform.runLater(this::updateView);
            });
        }
    }

    @FXML
    public void doSaveJav() {
        //Option<FxThumb> javImage = this.actorV2.getRecordImage(ActorV2.Source.JAVBUS);
        //this.actorV2.addRecord(ActorV2.Source.JAVBUS, this.txtUrlJav.getText(), javImage, "");
        if (this.txtUrlJav.getText().equals("")) {
            this.actorV2.removeRecord(ActorV2.Source.JAVBUS);
            this.actorV2.setActorJavBusID(new ActorWebID());
        } else {
            var webID = this.actorV2.getActorJavBusID();
            webID.set(this.txtUrlJav.getText());
            this.actorV2.setActorJavBusID(webID);
        }
    }

    @FXML
    public void doReloadMovieList() {
        Systems.useExecutors(() -> {
            this.actorV2.refreshMovieList();
            Platform.runLater(this::updateList);
        });
    }

    public AnchorPane show(ActorV2 actorV2) {
        if (actorV2 == null) {
            return null;
        }

        this.actorV2 = actorV2;
        actorV2.refreshMovieList();

        this.clear();

        this.imageViewWrapper.setKey(actorV2.getName());

        this.updateView();
        this.updateList();

        var localRecord = actorV2.getRecord(ActorV2.Source.LOCAL);
        localRecord.flatMap(ActorV2.NetRecord::getActorImage).peek(f -> f.getImage(this.imageViewWrapper::setImage, actorV2.getName(), this.imgLocal)).onEmpty(() -> this.imgLocal.setImage(null));

        this.imgHd.setImage(actorV2.getLocalImage().map(FxThumb::getImage).getOrNull());
        actorV2.getCustomData().getCustomImage().peek(f -> f.getImage(this.imageViewWrapper::setImage, actorV2.getName(), this.imgCustom)).onEmpty(() -> this.imgLocal.setImage(null));

        //this.imgCustom.setImage(actorV2.getCustomData().getCustomImage().map(FxThumb::getImage).getOrNull());

        //GUICommon.showDialog("", this, "Close", "Search Movie",
        //        () -> MovieDB.searchActorDialog(actorV2.getName()));

        Systems.useExecutors(() -> {
            actorV2.updateWebID(false);
            Platform.runLater(() -> {
                this.updateDmm();
                this.updateJav();
            });

            actorV2.updateFromWeb();
            Platform.runLater(this::updateView);
        });

        return this;
    }

    private void clear() {
        this.imgDmm.setImage(null);
        this.imgJav.setImage(null);
        this.imgCustom.setImage(null);
        this.imgLocal.setImage(null);
        this.imgHd.setImage(null);
    }

    private void updateList() {
        ObservableList<DirectoryEntry> observableList = FXCollections.observableArrayList();
        observableList.addAll(this.actorV2.getMovieList().toJavaList());
        this.listViewMovie.setItems(observableList);
    }

    private void updateText() {
        this.txtName.setText(this.actorV2.getName());
        this.txtYomi.setText(this.actorV2.getYomi());
        this.txtBirth.setText(this.actorV2.getBirth());
        this.txtSize.setText(this.actorV2.getSize());
        this.txtAge.setText(this.actorV2.getAge());
    }

    private void updateDmm() {
        var dmmRecord = this.actorV2.getRecord(ActorV2.Source.DMM);
        dmmRecord.flatMap(ActorV2.NetRecord::getActorImage).peek(f -> f.getImage(this.imageViewWrapper::setImage, this.actorV2.getName(), this.imgDmm)).onEmpty(() -> this.imgDmm.setImage(null));
        //this.imgDmm.setImage(dmmRecord.flatMap(ActorV2.NetRecord::getActorImage).map(FxThumb::getImage).getOrNull());
        //this.txtUrlDmm.setText(dmmRecord.map(ActorV2.NetRecord::getUrl).getOrElse(""));
        this.txtUrlDmm.setText(this.actorV2.getActorDmmID().get());
    }

    private void updateJav() {
        var javRecord = this.actorV2.getRecord(ActorV2.Source.JAVBUS);
        javRecord.flatMap(ActorV2.NetRecord::getActorImage).peek(f -> f.getImage(this.imageViewWrapper::setImage, this.actorV2.getName(), this.imgJav)).onEmpty(() -> this.imgJav.setImage(null));
        //this.imgJav.setImage(javRecord.flatMap(ActorV2.NetRecord::getActorImage).map(FxThumb::getImage).getOrNull());
        //this.txtUrlJav.setText(javRecord.map(ActorV2.NetRecord::getUrl).getOrElse(""));
        this.txtUrlJav.setText(this.actorV2.getActorJavBusID().get());
    }


}
