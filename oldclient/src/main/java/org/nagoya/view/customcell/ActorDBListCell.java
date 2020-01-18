package org.nagoya.view.customcell;

import com.jfoenix.controls.JFXListCell;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.system.database.MovieDB;
import org.nagoya.view.FXImageViewWrapper;

public class ActorDBListCell extends JFXListCell<ActorV2> {

    private final FXActorDBListCell fxActorDBCell;

    public ActorDBListCell() {
        this.fxActorDBCell = new FXActorDBListCell();
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(Insets.EMPTY);
        this.setPrefWidth(488);
        this.setPrefHeight(100);
    }

    @Override
    public void updateItem(ActorV2 item, boolean empty) {
        //system.out.println("------------ ActorListCell update ------------");
        super.updateItem(item, empty);

        this.setText("");
        if (!empty) {
            this.fxActorDBCell.setActor(item);
            this.setGraphic(this.fxActorDBCell);
        } else {
            this.setGraphic(null);
        }
    }
}

class FXActorDBListCell extends AnchorPane {

    private ActorV2 actor;

    @FXML
    Label txtName, txtYomi, txtCount, txtBirth, txtSize, txtAge;

    @FXML
    ImageView imgActor;

    private final FXImageViewWrapper imageViewWrapper;


    FXActorDBListCell() {
        GUICommon.loadFXMLRoot(this);
        this.imageViewWrapper = new FXImageViewWrapper(this.imgActor);
    }

    @FXML
    void removeActorAction() {
        MovieDB.actorDB().removeData(this.txtName.getText());
    }

    void setActor(@NotNull ActorV2 actor) {

        this.imageViewWrapper.setKey(actor.getName());
        this.actor = actor;

        this.txtName.setText(actor.getName());
        this.txtYomi.setText(actor.getYomi());
        this.txtBirth.setText(actor.getBirth());
        this.txtSize.setText(actor.getSize());
        this.txtAge.setText(actor.getAge());
        this.txtCount.setText("");//String.valueOf(actor.getMovieList().length()));

        actor.getImage()
                .peek(t -> t.getImageForceAsync(this.imageViewWrapper::setImage, actor.getName()))
                .onEmpty(() -> this.imgActor.setImage(ActorV2.NO_PHOTO.getImage()));
    }

    @FXML
    void doDMMUpdate() {
            /*Systems.useExecutors(() -> {
                ActorDB.doDMMUpdate(this.actor);
                Platform.runLater(() -> this.setActor(this.actor));
            });*/
    }
}

