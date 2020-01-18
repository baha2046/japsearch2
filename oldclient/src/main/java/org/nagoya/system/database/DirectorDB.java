package org.nagoya.system.database;

import org.nagoya.model.dataitem.DirectorV2;

public class DirectorDB extends CacheDB<DirectorV2> {

    DirectorDB(String fileName) {
        super(fileName, DirectorV2.class);
    }

}
