package org.nagoya.controls;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

public class FXZoomableScrollPane extends ScrollPane {
    private static double zoomIntensity = 0.02;

    public static double getZoomIntensity() {
        return zoomIntensity;
    }

    public static void setZoomIntensity(double z) {
        zoomIntensity = z;
    }

    private double scaleValue = 1;
    private double minScaleValue = 1;
    private final Node target;
    private final Node zoomNode;

    public FXZoomableScrollPane(Node target) {
        super();

        this.getStyleClass().add("zoomable-scroll-pane");

        this.target = target;
        this.zoomNode = new Group(target);
        this.setContent(this.outerNode(this.zoomNode));

        this.setPannable(true);
        this.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.setFitToHeight(true); //center
        this.setFitToWidth(true); //center

        this.widthProperty().addListener((ov, o, n) -> this.sizeUpdated());
        this.heightProperty().addListener((ov, o, n) -> this.sizeUpdated());

        this.updateScale();
    }

    public void sizeUpdated() {
        var rw = this.getWidth() / this.target.getLayoutBounds().getWidth();
        var rh = this.getHeight() / this.target.getLayoutBounds().getHeight();
        var newScale = Math.min(rw, rh);
        var scaleDiff = this.minScaleValue - newScale;
        this.scaleValue = this.scaleValue - scaleDiff;
        this.minScaleValue = newScale;
        this.updateScale();
        this.layout();
    }

    public void contentUpdated(double w, double h) {
        var rw = this.getWidth() / w;
        var rh = this.getHeight() / h;
        this.scaleValue = Math.min(rw, rh);
        this.minScaleValue = this.scaleValue;
        this.updateScale();
        this.layout();
    }

    @NotNull
    private Node outerNode(Node node) {
        Node outerNode = this.centeredNode(node);
        outerNode.setOnScroll(e -> {
            e.consume();
            this.onScroll(e.getTextDeltaY(), new Point2D(e.getX(), e.getY()));
        });
        return outerNode;
    }

    @NotNull
    private Node centeredNode(Node node) {
        VBox vBox = new VBox(node);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }

    private void updateScale() {
        this.target.setScaleX(this.scaleValue);
        this.target.setScaleY(this.scaleValue);
    }

    private void onScroll(double wheelDelta, Point2D mousePoint) {
        double zoomFactor = Math.exp(wheelDelta * zoomIntensity);

        Bounds innerBounds = this.zoomNode.getLayoutBounds();
        Bounds viewportBounds = this.getViewportBounds();

        // calculate pixel offsets from [0, 1] range
        double valX = this.getHvalue() * (innerBounds.getWidth() - viewportBounds.getWidth());
        double valY = this.getVvalue() * (innerBounds.getHeight() - viewportBounds.getHeight());

        this.scaleValue = this.scaleValue * zoomFactor;
        if (this.scaleValue < this.minScaleValue) {
            this.scaleValue = this.minScaleValue;
        }
        this.updateScale();
        this.layout(); // refresh ScrollPane scroll positions & target bounds

        // convert target coordinates to zoomTarget coordinates
        Point2D posInZoomTarget = this.target.parentToLocal(this.zoomNode.parentToLocal(mousePoint));

        // calculate adjustment of scroll position (pixels)
        Point2D adjustment = this.target.getLocalToParentTransform().deltaTransform(posInZoomTarget.multiply(zoomFactor - 1));

        // convert back to [0, 1] range
        // (too large/small values are automatically corrected by ScrollPane)
        Bounds updatedInnerBounds = this.zoomNode.getBoundsInLocal();
        this.setHvalue((valX + adjustment.getX()) / (updatedInnerBounds.getWidth() - viewportBounds.getWidth()));
        this.setVvalue((valY + adjustment.getY()) / (updatedInnerBounds.getHeight() - viewportBounds.getHeight()));
    }
}