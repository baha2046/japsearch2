package org.nagoya.view.skin;

import javafx.beans.property.ObjectProperty;
import javafx.scene.paint.Paint;

public interface AquaFocusBorder {
    ObjectProperty<Paint> innerFocusColorProperty();

    void setInnerFocusColor(Paint var1);

    Paint getInnerFocusColor();

    ObjectProperty<Paint> outerFocusColorProperty();

    void setOuterFocusColor(Paint var1);

    Paint getOuterFocusColor();
}