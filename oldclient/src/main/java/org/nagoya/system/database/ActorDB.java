package org.nagoya.system.database;


import io.vavr.control.Option;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.model.dataitem.ActorWebID;

public class ActorDB extends CacheDB<ActorV2> {

    public ActorDB(String fileName) {
        super(fileName, ActorV2.class);
    }

    public Option<ActorV2> getData(ActorWebID webID) {
        return this.dataCache.values().find(a -> a.getActorDmmID().equals(webID));
    }
}
