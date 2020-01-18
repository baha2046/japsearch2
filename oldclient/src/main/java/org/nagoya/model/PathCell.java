package org.nagoya.model;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Callback;

import java.util.Objects;

public class PathCell {
    private final String path;
    private BooleanProperty isUse = new SimpleBooleanProperty(false);

    @java.beans.ConstructorProperties({"path"})
    public PathCell(String path) {
        this.path = path;
    }

    public static Callback<PathCell, Observable[]> extractor() {
        return (PathCell p) -> new Observable[]{p.getIsUse()};
    }

    public String getPath() {
        return this.path;
    }

    public BooleanProperty getIsUse() {
        return this.isUse;
    }

    public void setIsUse(BooleanProperty isUse) {
        this.isUse = isUse;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PathCell)) return false;
        final PathCell other = (PathCell) o;
        if (!other.canEqual((Object) this)) return false;
        if (!Objects.equals(this.path, other.path)) return false;
        final Object this$isUse = this.isUse;
        final Object other$isUse = other.isUse;
        return Objects.equals(this$isUse, other$isUse);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PathCell;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $path = this.path;
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final Object $isUse = this.isUse;
        result = result * PRIME + ($isUse == null ? 43 : $isUse.hashCode());
        return result;
    }

    public String toString() {
        return "PathCell(path=" + this.path + ", isUse=" + this.isUse + ")";
    }
}
