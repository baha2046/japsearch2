package org.nagoya.view.skin.effects;

import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;

public class FocusBorder extends DropShadow {
    private final InnerShadow innerFocus = new InnerShadow();

    public FocusBorder() {
        this.innerFocus.setBlurType(BlurType.ONE_PASS_BOX);
        this.innerFocus.setRadius(5.0D);
        this.innerFocus.setChoke(0.8D);
        this.innerFocus.setOffsetX(0.0D);
        this.setSpread(0.6D);
        this.setBlurType(BlurType.ONE_PASS_BOX);
        this.setRadius(6.5D);
        this.setOffsetX(0.0D);
        this.setOffsetY(0.0D);
        this.setInput(this.innerFocus);
    }

    public void setInnerFocusColor(Color color) {
        this.innerFocus.setColor(color);
    }
}
