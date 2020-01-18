package org.nagoya.model;

import io.vavr.collection.Vector;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;
import org.nagoya.model.dataitem.FxThumb;

public class GalleryFolder {
    private static final int NUM_JPG_REQUIRE = 4;
    private final Vector<FxThumb> galleryImages;

    @NotNull
    public static Option<GalleryFolder> create(DirectoryEntry entry, Vector<FxThumb> list) {
        if (entry == null) {
            return Option.none();
        }

        float count = list.length();
        float diff = count / entry.getChildrenEntry().length();

        return (count > NUM_JPG_REQUIRE && diff > 0.8) ? Option.of(new GalleryFolder(list)) : Option.none();
    }

    private GalleryFolder(Vector<FxThumb> list) {
        this.galleryImages = list;
    }

    public Vector<FxThumb> getGalleryImages() {
        return this.galleryImages;
    }
}
