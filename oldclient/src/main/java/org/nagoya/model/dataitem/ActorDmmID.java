package org.nagoya.model.dataitem;

import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;

public class ActorDmmID {
    @NotNull
    public static ActorWebID of(String id) {
        ActorWebID actorDmmID = new ActorWebID();
        actorDmmID.set(id);
        return actorDmmID;
    }

    @NotNull
    public static ActorWebID fromUrl(String url) {
        ActorWebID actorDmmID = new ActorWebID();
        actorDmmID.set(getIdFromUrl(url));
        return actorDmmID;
    }

    public static Option<String> getIdFromUrl(String url) {
        Try<String> actressID = Try.of(() -> url.substring(url.indexOf("id=") + 3, url.length() - 1))
                .onFailure(e -> GUICommon.debugMessage(e.getMessage()));
        return actressID.toOption();
    }

    public static Option<String> getActorWebUrl(@NotNull ActorWebID webID) {
        return webID.getWebID().map(i -> "http://actress.dmm.co.jp/-/detail/=/actress_id=" + i + "/");
    }

    public static Option<String> getActorMovieWebUrl(@NotNull ActorWebID webID) {
        return webID.getWebID().map(i -> "http://www.dmm.co.jp/mono/dvd/-/list/=/article=actress/id=" + i + "/");
    }
}
