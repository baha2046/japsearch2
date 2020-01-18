package org.nagoya.controls;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import org.nagoya.NagoyaResource;

import java.util.Set;

/**
 * Represents a stylized version of a {@link ScrollPane}, with a more modern design.
 *
 * @author Dirk Lemmermann
 * @author François Martin
 * @author Marco Sanfratello
 * @see <a href="http://thisisnthappiness.com/post/32333764835/the-history-of-the-scroll-bar">Apple
 * Scroll Bar</a>
 */
public class FXScrollPane extends ScrollPane {
    private static final String STYLE_CLASS = "custom-scroll-pane";
    private static final String STYLE_CLASS_BAR = "custom-scroll-bar";
    private static final String USER_AGENT_STYLESHEET = NagoyaResource.load("css/customScrollPane.css").toExternalForm();

    private final ScrollBar vBar = new ScrollBar();
    private final ScrollBar hBar = new ScrollBar();

    /**
     * Creates an empty {@link ScrollPane}.
     */
    public FXScrollPane() {
        super();
        this.init();
    }

    /**
     * Creates a {@link ScrollPane} with {@code content}.
     *
     * @param content to be shown inside of the {@link ScrollPane}
     */
    public FXScrollPane(Node content) {
        super(content);
        this.init();
    }

    private void init() {

        this.skinProperty().addListener(it -> {
            // first bind, then add new scrollbars, otherwise the new bars will be found
            //        this.bindScrollBars();
            //        this.getChildren().addAll(this.vBar, this.hBar);
        });

        this.getStyleClass().add(STYLE_CLASS);
        //    this.setVbarPolicy(ScrollBarPolicy.NEVER);
        //    this.setHbarPolicy(ScrollBarPolicy.NEVER);

        this.vBar.setManaged(false);
        this.vBar.setOrientation(Orientation.VERTICAL);
        //this.vBar.getStyleClass().add(STYLE_CLASS_BAR);
        this.vBar.visibleProperty().bind(this.vBar.visibleAmountProperty().lessThan(1));

        this.hBar.setManaged(false);
        this.hBar.setOrientation(Orientation.HORIZONTAL);
        //this.hBar.getStyleClass().add(STYLE_CLASS_BAR);
        this.hBar.visibleProperty().bind(this.hBar.visibleAmountProperty().lessThan(1));
    }

    @Override
    public String getUserAgentStylesheet() {
        return USER_AGENT_STYLESHEET;
    }

    public void setFixWidth() {
        this.hBar.visibleProperty().unbind();
        this.hBar.visibleProperty().set(false);
        this.setFitToWidth(true);
    }

    private void bindScrollBars() {
        final Set<Node> nodes = this.lookupAll("ScrollBar");
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
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        Insets insets = this.getInsets();
        double w = this.getWidth();
        double h = this.getHeight();
        final double prefWidth = this.vBar.prefWidth(-1);
        this.vBar.resizeRelocate(
                w - prefWidth - insets.getRight(),
                insets.getTop(),
                prefWidth,
                h - insets.getTop() - insets.getBottom());

        final double prefHeight = this.hBar.prefHeight(-1);
        this.hBar.resizeRelocate(
                insets.getLeft(),
                h - prefHeight - insets.getBottom(),
                w - insets.getLeft() - insets.getRight(),
                prefHeight);
    }
}
