package org.nagoya.model.dataitem;

import io.vavr.control.Option;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ActorWebID {
    private Option<String> webID;

    @Contract(pure = true)
    public ActorWebID() {
        this.webID = Option.none();
    }

    public Option<String> getWebID() {
        return this.webID;
    }

    public void set(Option<String> webID) {
        this.webID = webID;
    }

    public void set(@NotNull String id) {
        this.set(id.equals("") ? Option.none() : Option.of(id));
    }

    public boolean isDefined() {
        return this.getWebID().isDefined();
    }

    public String get() {
        return this.getWebID().getOrElse("");
    }

    public String getOrElse(String other) {
        return this.getWebID().getOrElse(other);
    }

    // public abstract Option<String> getActorWebUrl();

    //public abstract Option<String> getActorMovieWebUrl();

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActorWebID)) {
            return false;
        }

        ActorWebID webID1 = (ActorWebID) o;

        return this.webID.equals(webID1.webID);
    }

    @Override
    public int hashCode() {
        return this.webID.hashCode();
    }

    @Override
    public String toString() {
        return "ActorWebID{" +
                "webID=" + this.webID +
                '}';
    }
}
