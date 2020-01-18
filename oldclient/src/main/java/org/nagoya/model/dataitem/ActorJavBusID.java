package org.nagoya.model.dataitem;

import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;

public class ActorJavBusID {
    @NotNull
    public static ActorWebID of(String id) {
        ActorWebID actorJavBusID = new ActorWebID();
        actorJavBusID.set(id);
        return actorJavBusID;
    }

    @NotNull
    public static ActorWebID fromUrl(String url) {
        ActorWebID actorJavBusID = new ActorWebID();
        actorJavBusID.set(getIdFromUrl(url));
        return actorJavBusID;
    }

    public static Option<String> getIdFromUrl(String url) {
        Try<String> actressID = Try.of(() -> url.substring(url.lastIndexOf("/") + 1))
                .onFailure(e -> GUICommon.debugMessage(e.getMessage()));
        return actressID.toOption();
    }

    public static Option<String> getActorWebUrl(@NotNull ActorWebID webID) {
        return webID.getWebID().map(i -> "https://www.javbus.com/ja/star/" + i);
    }

    public static Option<String> getActorMovieWebUrl(ActorWebID webID) {
        return getActorWebUrl(webID);
    }
}
