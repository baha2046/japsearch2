package org.nagoya;

import java.net.URL;

public final class NagoyaResource {

    public static URL load(String path) {
        return NagoyaResource.class.getResource(path);
    }

    private NagoyaResource() {
    }

}
