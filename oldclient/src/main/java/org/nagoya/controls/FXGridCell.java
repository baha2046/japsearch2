package org.nagoya.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import org.controlsfx.control.GridCell;
import org.jetbrains.annotations.NotNull;
import org.nagoya.NagoyaResource;

public class FXGridCell<T> extends GridCell<T> {
    private static final String USER_AGENT_STYLESHEET = NagoyaResource.load("css/customGridView.css").toExternalForm();

    protected final BooleanProperty cellSelectedProperty;

    public FXGridCell() {
        super();

        this.cellSelectedProperty = new SimpleBooleanProperty(false) {
            @Override
            protected void invalidated() {
                FXGridCell.this.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), FXGridCell.this.cellSelectedProperty.get());
            }
        };

        this.hoverProperty().addListener((ov, o, n) -> this.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), n));
        this.getStyleClass().add("custom-grid-cell");
    }

    public FXGridCell(@NotNull FXGridView<T> gridView) {
        this();

        // must re-calculate when selection change or item of cell change
        this.cellSelectedProperty.bind(Bindings.createBooleanBinding(
                () -> gridView.getSelection() != null && gridView.getSelection().equals(this.getItem()),
                gridView.selectionProperty(), this.itemProperty()));

        this.setOnMouseClicked((e) -> gridView.setSelection(this.getItem()));
    }

    @Override
    public String getUserAgentStylesheet() {
        return USER_AGENT_STYLESHEET;
    }
}
