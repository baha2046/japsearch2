package org.nagoya.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.controlsfx.control.GridView;
import org.nagoya.NagoyaResource;

public class FXGridView<T> extends GridView<T> {
    private static final String STYLE_CLASS = "custom-grid-view";
    private static final String STYLE_CLASS_BAR = "custom-scroll-bar";
    private static final String USER_AGENT_STYLESHEET = NagoyaResource.load("css/customGridView.css").toExternalForm();

    //private final ScrollBar vBar = new ScrollBar();
    //private final ScrollBar hBar = new ScrollBar();

    private final ObjectProperty<T> selection;

    public FXGridView() {
        super();
        this.selection = new SimpleObjectProperty<>(this, "selection");
/*
        this.skinProperty().addListener(it -> {
            // first bind, then add new scrollbars, otherwise the new bars will be found
            this.bindScrollBars();
            this.getChildren().addAll(this.vBar, this.hBar);
        });
*/
        this.getStyleClass().add(STYLE_CLASS);

        /*this.vBar.setManaged(false);
        this.vBar.setOrientation(Orientation.VERTICAL);
        this.vBar.getStyleClass().add(STYLE_CLASS_BAR);
        this.vBar.visibleProperty().bind(this.vBar.visibleAmountProperty().isNotEqualTo(0));

        this.hBar.setManaged(false);
        this.hBar.setOrientation(Orientation.HORIZONTAL);
        this.hBar.getStyleClass().add(STYLE_CLASS_BAR);
        this.hBar.visibleProperty().bind(this.hBar.visibleAmountProperty().isNotEqualTo(0));*/
    }

    @Override
    public String getUserAgentStylesheet() {
        return USER_AGENT_STYLESHEET;
    }

    public T getSelection() {
        return this.selection.get();
    }

    public ObjectProperty<T> selectionProperty() {
        return this.selection;
    }

    public void setSelection(T selection) {
        this.selection.set(selection);
    }

    /*private void bindScrollBars() {
        final Set<Node> nodes = this.lookupAll("VirtualScrollBar");
        for (Node node : nodes) {
            if (node instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) node;
                if (bar.getOrientation().equals(Orientation.VERTICAL)) {
                    this.bindScrollBars(this.vBar, bar);
                } else if (bar.getOrientation().equals(Orientation.HORIZONTAL)) {
                    this.bindScrollBars(this.hBar, bar);
                }
            }
        }
    }

    private void bindScrollBars(ScrollBar scrollBarA, ScrollBar scrollBarB) {
        scrollBarA.valueProperty().bindBidirectional(scrollBarB.valueProperty());
        scrollBarA.minProperty().bindBidirectional(scrollBarB.minProperty());
        scrollBarA.maxProperty().bindBidirectional(scrollBarB.maxProperty());
        scrollBarA.visibleAmountProperty().bindBidirectional(scrollBarB.visibleAmountProperty());
        scrollBarA.unitIncrementProperty().bindBidirectional(scrollBarB.unitIncrementProperty());
        scrollBarA.blockIncrementProperty().bindBidirectional(scrollBarB.blockIncrementProperty());
    }*/

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        /*Insets insets = this.getInsets();
        double w = this.getWidth();
        double h = this.getHeight();
        final double prefWidth = this.vBar.prefWidth(-1);
        this.vBar.resizeRelocate(w - prefWidth - insets.getRight(), insets.getTop(), prefWidth, h - insets.getTop() - insets.getBottom());

        final double prefHeight = this.hBar.prefHeight(-1);
        this.hBar.resizeRelocate(insets.getLeft(), h - prefHeight - insets.getBottom(), w - insets.getLeft() - insets.getRight(), prefHeight);*/
    }
}

