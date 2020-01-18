package org.nagoya.model.dataitem;

import io.vavr.collection.Stream;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.system.database.MovieDB;
import org.nagoya.system.database.MovieScanner;

public class DirectorV2 {
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static DirectorV2 of(String name) {
        return MovieDB.directorDB().getOrElsePut(name, () -> new DirectorV2(name));
    }

    private final StringProperty strName;
    private String strDmmUrl;
    private String strJavUrl;

    private transient Stream<DirectoryEntry> movieList;

    @Contract(pure = true)
    public DirectorV2(String strName) {
        this.strName = new SimpleStringProperty(strName);
        this.strDmmUrl = "";
        this.strJavUrl = "";
        this.movieList = Stream.empty();
    }

    public String getStrName() {
        return this.strName.get();
    }

    public StringProperty strNameProperty() {
        return this.strName;
    }

    public void setStrName(String strName) {
        this.strName.set(strName);
    }

    public String getStrDmmUrl() {
        return this.strDmmUrl;
    }

    public void setStrDmmUrl(String strDmmUrl) {
        this.strDmmUrl = strDmmUrl;
    }

    public String getStrJavUrl() {
        return this.strJavUrl;
    }

    public void setStrJavUrl(String strJavUrl) {
        this.strJavUrl = strJavUrl;
    }

    public void refreshMovieList() {
        this.movieList = MovieDB.getInstance()
                .findByActor(this.getStrName())
                .peek(p -> GUICommon.debugMessage(p.toString()))
                .flatMap(p -> MovieScanner.getInstance().getEntryFromPath(p));
    }

    public Stream<DirectoryEntry> getMovieList() {
        return this.movieList;
    }
}
