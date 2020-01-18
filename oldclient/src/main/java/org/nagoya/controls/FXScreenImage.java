package org.nagoya.controls;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.model.dataitem.FxThumb;

public class FXScreenImage extends FxThumb {
    @NotNull
    @Contract("_ -> new")
    public static FXScreenImage of(FxThumb thumb) {
        return new FXScreenImage(thumb);
    }

    public FXScreenImage(@NotNull FxThumb fromThumb) {
        super();
        this.thumbURL = fromThumb.urlOption();
        this.localPath = fromThumb.localPathOption();
        this.strCookie = fromThumb.cookieOption();
        this.thumbLabel = fromThumb.getThumbLabel();

        this.widthLimit = 200;
        this.heightLimit = 200;
    }

    public void setMaxSize(double w, double h) {
        this.setWidthLimit(w);
        this.setHeightLimit(h);
    }
}
