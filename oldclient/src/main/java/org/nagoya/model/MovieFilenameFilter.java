package org.nagoya.model;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;

public class MovieFilenameFilter implements FilenameFilter {

    public static final String[] acceptedMovieExtensions = {"avi", "mp4", "mpg", "wmv", "asf", "flv", "mkv", "mka", "mov", "qt", "mpeg", "m4v", "m4a", "aac", "nut", "ogg", "ogm",
            "rmvb", "rm", "ram", "ra", "3gp", "divx", "pva", "nuv", "nsa", "fli", "flc", "dvr-ms", "wtv", "iso", "vob"};

    @Override
    public boolean accept(File dir, String name) {
        if (this.allowedSuffix(FilenameUtils.getExtension(name))) {
            return true;
        } else {
            return false;
        }
    }

    private boolean allowedSuffix(String suffix) {
        for (String currentSuffix : acceptedMovieExtensions) {
            if (suffix.equalsIgnoreCase(currentSuffix)) {
                return true;
            }
        }
        return false;
    }

    public static String getGlobString() {
        return ("*.{" + String.join(",", acceptedMovieExtensions) + "}");
    }
}
