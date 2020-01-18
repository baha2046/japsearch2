package org.nagoya.system.database;

import org.nagoya.model.dataitem.MakerData;

public class MakerDB extends CacheDB<MakerData> {

    MakerDB(String fileName) {
        super(fileName, MakerData.class);
    }

}
