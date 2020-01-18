package org.nagoya.model;

import io.vavr.control.Option;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Callback;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.system.database.MovieDB;

public class SimpleMovieData {
    @NotNull
    @Contract(pure = true)
    public static Callback<SimpleMovieData, Observable[]> extractor() {
        return (SimpleMovieData p) -> new Observable[]{p.refreshProperty()};
    }

    private final String strId;
    private final String strTitle;
    private final Option<FxThumb> imgCover;
    private String strUrl;
    private boolean isExist;
    private boolean isTemp;
    private boolean isBlackList;

    private transient BooleanProperty refreshProperty;
    private transient Option<DirectoryEntry> directoryEntry;

    public SimpleMovieData() {
        this.strId = "";
        this.strTitle = "";
        this.imgCover = Option.none();
        this.init();
    }

    @Contract(pure = true)
    public SimpleMovieData(String strId, String strTitle, Option<FxThumb> imgCover) {
        this.init();
        this.strId = strId;
        this.strTitle = strTitle;
        this.imgCover = imgCover;
        this.isExist = MovieDB.getInstance().isMovieExist(strId);
        //GUICommon.debugMessage(String.valueOf(this.isExist));
    }

    public SimpleMovieData(@NotNull MovieV2 movieV2) {
        this.init();
        this.strId = movieV2.getMovieID();
        this.strTitle = movieV2.getMovieTitle();
        this.imgCover = movieV2.getImgFrontCover();
        this.isExist = true;
    }

    public SimpleMovieData(@NotNull MovieV2 movieV2, DirectoryEntry directoryEntry) {
        this(movieV2);
        this.directoryEntry = Option.of(directoryEntry);
        this.isExist = directoryEntry.hasMovie();
    }

    private void init() {
        this.strUrl = "";
        this.isTemp = false;
        this.isBlackList = false;
        this.directoryEntry = Option.none();
        this.refreshProperty = new SimpleBooleanProperty(true);
    }

    public void refreshView() {
        Platform.runLater(() -> this.refreshProperty.set(!this.refreshProperty.getValue()));
    }

    public BooleanProperty refreshProperty() {
        return this.refreshProperty;
    }

    public String getStrId() {
        return this.strId;
    }

    public String getStrTitle() {
        return this.strTitle;
    }

    public String getStrUrl() {
        return this.strUrl;
    }

    public void setStrUrl(String strUrl) {
        this.strUrl = strUrl;
    }

    public boolean isExist() {
        return this.isExist;
    }

    public boolean isTemp() {
        return this.isTemp;
    }

    public void setTemp(boolean temp) {
        this.isTemp = temp;
    }

    public void setExist(boolean exist) {
        this.isExist = exist;
    }

    public boolean isBlackList() {
        return this.isBlackList;
    }

    public void setBlackList(boolean blackList) {
        this.isBlackList = blackList;
    }

    public Option<FxThumb> getImgCover() {
        return this.imgCover;
    }

    public Option<DirectoryEntry> getDirectoryEntry() {
        return this.directoryEntry;
    }

    public void setDirectoryEntry(Option<DirectoryEntry> directoryEntry) {
        this.directoryEntry = directoryEntry;
    }
}
