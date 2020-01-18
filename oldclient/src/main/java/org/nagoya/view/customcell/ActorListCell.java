package org.nagoya.view.customcell;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;
import io.vavr.control.Option;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import org.nagoya.GUICommon;
import org.nagoya.controls.FXActorDetailWindow;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.view.FXImageViewWrapper;
import org.nagoya.view.FXMovieDetailView;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ActorListCell extends ListCell<ActorV2> {

    private final FXActorListCell actorCell;
    private FXMovieDetailView movieDetailView = null;

    public ActorListCell() {
        this.actorCell = new FXActorListCell();
        this.actorCell.setParent(this);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(Insets.EMPTY);
        this.setPrefWidth(248);
        this.setPrefHeight(100);
        //system.out.println("------------ ActorListCell Cont ------------");
    }

    public ActorListCell(FXMovieDetailView movieDetailView) {
        this();
        this.movieDetailView = movieDetailView;
    }

    public ActorListCell(boolean useButton) {
        this();
        if (!useButton) {
            this.actorCell.btnEdit.setVisible(false);
            this.actorCell.btnSave.setVisible(false);
            this.actorCell.btnDel.setVisible(false);
        }
    }

    Option<FXMovieDetailView> getMovieDetailView() {
        return Option.of(this.movieDetailView);
    }

    @Override
    public void updateItem(ActorV2 item, boolean empty) {
        //system.out.println("------------ ActorListCell update ------------");
        super.updateItem(item, empty);

        this.setText("");
        if (!empty) {
            this.actorCell.setActor(item);
            this.setGraphic(this.actorCell);
        } else {
            this.setGraphic(null);
        }
    }
}

class FXActorListCell extends AnchorPane {

    @FXML
    ImageView imgActor;
    @FXML
    JFXToggleButton btnEdit;
    @FXML
    JFXTextField txtName;
    @FXML
    JFXButton btnSave;
    @FXML
    JFXButton btnDel;
    @FXML
    JFXButton btnImage;
    @FXML
    Rectangle mask;
    @FXML
    Label txtYomi, txtAge;

    private ActorListCell parent;
    private ActorV2 inActor = null;

    private final FXImageViewWrapper imageViewWrapper;

    FXActorListCell() {
        GUICommon.loadFXMLRoot(this);

        this.imageViewWrapper = new FXImageViewWrapper(this.imgActor);
        this.imgActor.setPreserveRatio(true);
        this.txtName.editableProperty().bind(this.btnEdit.selectedProperty());
        this.btnSave.disableProperty().bind(this.txtName.editableProperty().not());
        this.btnDel.disableProperty().bind(this.txtName.editableProperty().not());
        //system.out.println("------------ FXActorListCell Cont ------------");
    }

    @FXML
    void saveAction() {
        //this.inActor.setName(this.txtName.getText());
        this.parent.getListView().getItems().add(ActorV2.of(this.txtName.getText()));
        this.parent.getListView().getItems().remove(this.parent.getItem());
        //this.btnEdit.setSelected(false);
    }

    @FXML
    void deleteAction() {
        Runnable action = () -> this.parent.getListView().getItems().remove(this.parent.getItem());

        DialogBuilder.create()
                .heading("[ Confirm ]")
                .body("Are you sure you want to delete the actor?")
                .buttonYesNo(action)
                .build()
                .show();
    }

    @FXML
    void editImageAction() {

            /*if (this.btnEdit.isSelected()) {
                JFXTextField newUrl = new JFXTextField();

                newUrl.setText(this.inActor.getNetImage().map(FxThumb::getThumbURL).map(URL::toString).getOrElse(""));

                GUICommon.showDialog("Edit actor image URL : (Auto Save)", newUrl, "Cancel", "Confirm", () -> {
                    this.btnEdit.setSelected(false);
                    if (newUrl.getText().equals("")) {
                        this.imgActor.setImage(null);
                        this.inActor.clearSource();
                    } else {
                        Systems.useExecutors(() -> {
                            boolean success = false;
                            try {
                                if (FxThumb.fileExistsAtUrl(newUrl.getText())) {
                                    this.inActor.setLocalImage(null);
                                    this.inActor.addRecord(ActorV2.Source.LOCAL, "", newUrl.getText(), "");
                                    this.imgActor.setImage(this.inActor.getImage().map(FxThumb::getImage).getOrNull());
                                    success = true;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (!success) {
                                //Thread.sleep(800);
                                Platform.runLater(() -> {
                                    GUICommon.showDialog("Error :", new Text("Image not found!"), "Close", null, null);
                                });
                            }
                        });
                    }
                });
            } else*/
        if (this.inActor != null) {
            FXActorDetailWindow.show(this.inActor);
        }
    }

    void setActor(ActorV2 actor) {
        if (actor == null) {
            return;
        }

        this.inActor = actor;
        this.txtName.setText(actor.getName());
        this.imageViewWrapper.setKey(actor.getName());
        this.btnEdit.setSelected(false);

        this.txtYomi.setText(actor.getYomi());
        //GUICommon.debugMessage("getYomi" + actor.getYomi());

        Option<LocalDate> movieDate = this.parent.getMovieDetailView().flatMap(FXMovieDetailView::getMovieReleaseDate);
        Option<LocalDate> actorDate = actor.getAgeAsLocalDate();
        if (movieDate.isDefined() && actorDate.isDefined()) {
            this.txtAge.setText(String.valueOf(ChronoUnit.YEARS.between(actorDate.get(), movieDate.get())));
        } else {
            this.txtAge.setText(actor.getAge());
        }

        this.mask.setVisible(!actor.hasInfo());

        //GUICommon.debugMessage(actor.getName() + " " + actor.getImage().map(FxThumb::getThumbURL).map(URL::toString).getOrElse(""));

        //this.imgActor.setImage(actor.getImage().map(FxThumb::getImage).getOrElse(ActorV2.NO_PHOTO.getImage()));

        actor.getImage()
                .peek(t -> t.getImageForceAsync(this.imageViewWrapper::setImage, actor.getName()))
                .onEmpty(() -> this.imgActor.setImage(ActorV2.NO_PHOTO.getImage()));
    }

    void setParent(ActorListCell parent) {
        this.parent = parent;
    }
}

