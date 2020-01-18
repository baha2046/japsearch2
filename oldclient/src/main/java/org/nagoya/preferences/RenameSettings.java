package org.nagoya.preferences;

import io.vavr.collection.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.io.Setting;
import org.nagoya.model.MovieFolder;
import org.nagoya.model.MovieV2;

public class RenameSettings {
    private static final String fileName = "naming.ini";
    private static RenameSettings INSTANCE = null;

    public enum Key {
        //PATH_DOU, PATH_COMIC, PATH_MOVIE, PATH_COSPLAY
    }

    private String[] company;
    private String[] renameDirectoryFormat;
    private String[] renameFileFormat;

    private RenameSettings() {
        // use default setting
        this.company = new String[1];
        this.company[0] = "TEST1|TEST2";
        this.renameDirectoryFormat = new String[]{"#date", "space", "[", "#id", "]", "space", "#moviename"};
        this.renameFileFormat = new String[]{"[", "#id", "]"};
    }

    public static synchronized RenameSettings getInstance() {
        if (INSTANCE == null) {
            INSTANCE = Setting.readSetting(RenameSettings.class, fileName);
        }
        return INSTANCE;
    }

    public static void writeSetting() {
        Setting.writeSetting(getInstance(), fileName);
    }

    @NotNull
    public static String getSuitableDirectoryName(MovieV2 movieV2) {
        StringBuilder stringBuilder = new StringBuilder();

        Stream.of(getInstance().getRenameDirectoryFormat())
                .map((s) -> MovieV2.getFormatUnit(movieV2, s))
                .forEach(stringBuilder::append);

        return stringBuilder.toString().replace(":", " ");
    }

    @NotNull
    public static String getSuitableFileName(MovieV2 movieV2) {
        StringBuilder stringBuilder = new StringBuilder();

        Stream.of(getInstance().getRenameFileFormat())
                .map((s) -> MovieV2.getFormatUnit(movieV2, s))
                .forEach(stringBuilder::append);

        return stringBuilder.toString();
    }

    @NotNull
    @Contract(pure = true)
    public static String getFileNameNfo(String inMovieName) {
        if (MovieFolder.NAMING_FIXED_NFO) {
            return "movie.nfo";
        } else {
            return (inMovieName + ".nfo");
        }
    }

    @NotNull
    @Contract(pure = true)
    public static String getFileNameFrontCover(String inMovieName) {
        if (MovieFolder.NAMING_FIXED_POSTER) {
            return "poster.jpg";
        } else {
            return (inMovieName + "-poster.jpg");
        }
    }

    @NotNull
    @Contract(pure = true)
    public static String getFileNameBackCover(String inMovieName) {
        if (MovieFolder.NAMING_FIXED_POSTER) {
            return "fanart.jpg";
        } else {
            return (inMovieName + "-fanart.jpg");
        }
    }

    @NotNull
    @Contract(pure = true)
    public static String getFolderNameActors() {
        return ".actors";
    }

    @NotNull
    @Contract(pure = true)
    public static String getFileNameFolderJpg() {
        return "folder.jpg";
    }

    @NotNull
    @Contract(pure = true)
    public static String getFileNameExtraImage() {
        return "thumb";
    }

    @NotNull
    @Contract(pure = true)
    public static String getFolderNameExtraImage() {
        return "extrathumbs";
    }

    @NotNull
    @Contract(pure = true)
    public static String getFileNameTrailer(String inMovieName) {
        return (inMovieName + "-trailer.mp4");
    }


    public String renameCompany(String inString) {
        return Stream.of(this.company).filter(s -> s.substring(0, s.indexOf("|")).equals(inString))
                .map(s -> s.substring(s.indexOf("|") + 1)).getOrElse(inString);
    }

    public void updateRenameMapping(String inString) {
        String[] strings = this.getCompany();
        boolean isUpdated = false;
        for (int x = 0; x < strings.length; x++) {
            if (inString.substring(0, inString.indexOf("|")).equals(strings[x].substring(0, strings[x].indexOf("|")))) {
                strings[x] = inString;
                isUpdated = true;
                break;
            }
        }
        if (!isUpdated) {
            String[] newStrings = new String[strings.length + 1];
            System.arraycopy(strings, 0, newStrings, 0, strings.length);
            newStrings[newStrings.length - 1] = inString;
            this.setCompany(newStrings);
        }
    }

    public String[] getCompany() {
        return this.company;
    }

    public String[] getRenameDirectoryFormat() {
        return this.renameDirectoryFormat;
    }

    public String[] getRenameFileFormat() {
        return this.renameFileFormat;
    }

    public void setCompany(String[] company) {
        this.company = company;
    }

    public void setRenameDirectoryFormat(String[] renameDirectoryFormat) {
        this.renameDirectoryFormat = renameDirectoryFormat;
    }

    public void setRenameFileFormat(String[] renameFileFormat) {
        this.renameFileFormat = renameFileFormat;
    }


}
