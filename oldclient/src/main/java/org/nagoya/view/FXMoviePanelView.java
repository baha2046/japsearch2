package org.nagoya.view;

import com.jfoenix.controls.JFXButton;
import io.vavr.control.Option;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.nagoya.GUICommon;
import org.nagoya.controller.FXCoreController;
import org.nagoya.controller.FXMoviePanelControl;
import org.nagoya.controller.siteparsingprofile.specific.ArzonParsingProfile;
import org.nagoya.controller.siteparsingprofile.specific.DmmParsingProfile;
import org.nagoya.controller.siteparsingprofile.specific.DugaParsingProfile;
import org.nagoya.controller.siteparsingprofile.specific.JavBusParsingProfile;
import org.nagoya.model.MovieV2;
import org.nagoya.system.event.FXContextImp;
import org.nagoya.view.editor.FXSettingEditor;

import java.net.URL;
import java.util.ResourceBundle;

import static org.nagoya.controller.FXMoviePanelControl.*;

public class FXMoviePanelView extends FXContextImp {//FXMLController {

    private static final Rectangle2D boxBounds = new Rectangle2D(0, 0, 860, 660);
    private static final double ACTION_BOX_HGT = 0;
    @FXML
    public JFXButton btnSAll, btnSAllCustom;
    @FXML
    public JFXButton btnSDmm, btnSArzon, btnSDuga, btnSJavBus;
    @FXML
    public JFXButton btnSaveMovie, btnExtraFanArt;
    private Rectangle clipRect;
    private Timeline timelineUp;
    private Timeline timelineDown;
    @FXML
    private Pane detailPane;
    private FXMovieDetailView movieDetailDisplay;
    private Runnable doChangeDetail;


    public FXMoviePanelView() {
    }

    private void setAnimation() {
        /* Initial position setting for Top Pane*/
        this.clipRect = new Rectangle();
        this.clipRect.setWidth(ACTION_BOX_HGT);
        this.clipRect.setHeight(boxBounds.getHeight());
        this.clipRect.translateXProperty().set(boxBounds.getWidth() - ACTION_BOX_HGT);
        this.movieDetailDisplay.getPane().setClip(this.clipRect);
        this.movieDetailDisplay.getPane().translateXProperty().set(-(boxBounds.getWidth() - ACTION_BOX_HGT));

        /* Animation for bouncing effect. */
        final Timeline timelineDown1 = new Timeline();
        timelineDown1.setCycleCount(2);
        timelineDown1.setAutoReverse(true);
        final KeyValue kv1 = new KeyValue(this.clipRect.widthProperty(), (boxBounds.getWidth() - 15));
        final KeyValue kv2 = new KeyValue(this.clipRect.translateXProperty(), 15);
        final KeyValue kv3 = new KeyValue(this.movieDetailDisplay.getPane().translateXProperty(), -15);
        final KeyFrame kf1 = new KeyFrame(Duration.millis(100), kv1, kv2, kv3);
        timelineDown1.getKeyFrames().add(kf1);

        /* Event handler to call bouncing effect after the scroll down is finished. */
        EventHandler<ActionEvent> onFinished = t -> timelineDown1.play();

        this.timelineDown = new Timeline();
        this.timelineUp = new Timeline();

        /* Animation for scroll down. */
        this.timelineDown.setCycleCount(1);
        this.timelineDown.setAutoReverse(true);
        final KeyValue kvDwn1 = new KeyValue(this.clipRect.widthProperty(), boxBounds.getWidth());
        final KeyValue kvDwn2 = new KeyValue(this.clipRect.translateXProperty(), 0);
        final KeyValue kvDwn3 = new KeyValue(this.movieDetailDisplay.getPane().translateXProperty(), 0);
        final KeyFrame kfDwn = new KeyFrame(Duration.millis(500), onFinished, kvDwn1, kvDwn2, kvDwn3);
        this.timelineDown.getKeyFrames().add(kfDwn);

        /* Animation for scroll up. */
        this.timelineUp.setCycleCount(1);
        this.timelineUp.setAutoReverse(true);
        final KeyValue kvUp1 = new KeyValue(this.clipRect.widthProperty(), ACTION_BOX_HGT);
        final KeyValue kvUp2 = new KeyValue(this.clipRect.translateXProperty(), boxBounds.getWidth() - ACTION_BOX_HGT);
        final KeyValue kvUp3 = new KeyValue(this.movieDetailDisplay.getPane().translateXProperty(), -(boxBounds.getWidth() - ACTION_BOX_HGT));
        final KeyFrame kfUp = new KeyFrame(Duration.millis(500), (event) -> {
            this.doChangeDetail.run();
            //this.timelineDown.play();
        }, kvUp1, kvUp2, kvUp3);
        this.timelineUp.getKeyFrames().add(kfUp);
    }

    public void runChangeEffect(Boolean hasMovie, Runnable onChange) {

        this.doChangeDetail = () -> {
            onChange.run();
            if (hasMovie) {
                this.timelineDown.play();
            }
            // this.controller.getFXFileListControl().requestFocus();
        };

        this.timelineUp.play();
    }

    @FXML
    private void settingAction() {
        FXSettingEditor.showSettingEditor();
    }

    @FXML
    private void showExtraArtAction() {
        this.fireEvent(EVENT_SHOW_EXTRA_ART, null);
    }

    @FXML
    private void saveAction() {
        this.movieDetailDisplay.getCurrentMovie().peek(m -> this.fireEvent(EVENT_ACTION_SAVE_MOVIE, m));
    }

    @FXML
    private void scrapeAllCustom() {
        this.fireEvent(EVENT_ACTION_SCRAPE_MOVIE, true);
    }

    @FXML
    private void scrapeAll() {
        this.fireEvent(EVENT_ACTION_SCRAPE_MOVIE, false);
    }

    @FXML
    private void scrapeArzon() {
        this.fireEvent(EVENT_ACTION_SCRAPE_DETAIL, ArzonParsingProfile.parserName());
    }

    @FXML
    private void scrapeDMM() {
        this.fireEvent(EVENT_ACTION_SCRAPE_DETAIL, DmmParsingProfile.parserName());
    }

    @FXML
    private void scrapeDuga() {
        this.fireEvent(EVENT_ACTION_SCRAPE_DETAIL, DugaParsingProfile.parserName());
    }

    @FXML
    private void scrapeJavBus() {
        this.fireEvent(EVENT_ACTION_SCRAPE_DETAIL, JavBusParsingProfile.parserName());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        this.movieDetailDisplay = GUICommon.loadFXMLController(FXMovieDetailView.class);
        this.detailPane.getChildren().add(this.movieDetailDisplay.getPane());
        //this.detailPane.setVisible(false);
        this.setAnimation();

        this.btnSAll.setDisable(true);
        this.btnSAllCustom.disableProperty().bind(this.btnSAll.disableProperty());

        FXCoreController.addContext(this);
    }

    public void setup(FXMoviePanelControl control, ObjectProperty<Option<MovieV2>> property) {
        this.movieDetailDisplay.setup(control, property);
    }

    public void setCustomFlag(boolean v1, boolean v2) {
        this.movieDetailDisplay.setCustomFlag(v1, v2);
    }

    public void clearCustomFlag() {
        this.movieDetailDisplay.clearCustomFlag();
    }

    @Override
    public void registerListener() {
        this.registerListener(EVENT_MOVIE_CHANGE, e -> this.runChangeEffect(true, e.getParam()));
        this.registerListener(EVENT_MOVIE_EMPTY, e -> this.runChangeEffect(false, e.getParam()));
    }
}
