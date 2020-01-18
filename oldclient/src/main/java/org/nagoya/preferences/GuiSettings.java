package org.nagoya.preferences;

import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GuiSettings extends SettingBase {

    private static GuiSettings INSTANCE = null;
    public static Path homePath = Paths.get(System.getProperty("user.home"));

    public enum Key implements Settings.Key {
        lastUsedDirectory, avDirectory, doujinshiDirectory, mangaDirectory, cosplayDirectory, photoDirectory,
        downloadDirectory, musicDirectory, showOutputPanel, showToolbar, useContentBasedTypeIcons, pathToExternalMediaPlayer, width, height;

        @NotNull
        @Override
        public String getKey() {
            // prefix setting key to avoid clashing
            return "Gui:" + this.toString();
        }
    }

    public static synchronized GuiSettings getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GuiSettings();
        }
        return INSTANCE;
    }


    private GuiSettings() {
        //prevent people from using this
    }

    /*
        public boolean getShowToolbar() {
            return getBooleanValue(Key.showToolbar, Boolean.TRUE).booleanValue();
        }

        public void setShowToolbar(boolean preferenceValue) {
            setBooleanValue(Key.showToolbar, Boolean.valueOf(preferenceValue));
        }

        public boolean getShowOutputPanel() {
            return getBooleanValue(Key.showOutputPanel, false);
        }

        public void setShowOutputPanel(boolean preferenceValue) {
            setBooleanValue(Key.showOutputPanel, preferenceValue);
        }
    */
    public Path getDirectory(@NotNull Key key) {
        switch (key) {
            case avDirectory:
                return this.getDirectory("G-AVDirectory", "X:\\Movies\\AV");
            case mangaDirectory:
                return this.getDirectory("G-ComicDirectory", "X:\\[COMIC]");
            case doujinshiDirectory:
                return this.getDirectory("G-DoujinshiDirectory", "X:\\[DOUJINSHI]");
            case cosplayDirectory:
                return this.getDirectory("G-CosplayDirectory", "X:\\Cosplay");
            case photoDirectory:
                return this.getDirectory("G-PhotoDirectory", "X:\\Image");
            case downloadDirectory:
                return this.getDirectory("G-DownloadDirectory", "X:\\Download");
            case lastUsedDirectory:
            default:
                return this.getDirectory("G-LastDirectory", "X:\\Movies\\AV");
        }
    }

    public void setDirectory(@NotNull Key key, Path path) {
        switch (key) {
            case avDirectory:
                this.setDirectory("G-AVDirectory", path);
                break;
            case mangaDirectory:
                this.setDirectory("G-ComicDirectory", path);
                break;
            case doujinshiDirectory:
                this.setDirectory("G-DoujinshiDirectory", path);
                break;
            case cosplayDirectory:
                this.setDirectory("G-CosplayDirectory", path);
                break;
            case photoDirectory:
                this.setDirectory("G-PhotoDirectory", path);
                break;
            case downloadDirectory:
                this.setDirectory("G-DownloadDirectory", path);
                break;
            case lastUsedDirectory:
                this.setDirectory("G-LastDirectory", path);
                break;
        }
    }

    public Path getLastUsedDirectory() {
        return this.getDirectory(Key.lastUsedDirectory);
    }

    public void setLastUsedDirectory(@NotNull Path path) {
        this.setDirectory(Key.lastUsedDirectory, path);
    }

    public Path getDirectory(String key, String defaultValue) {
        String pathString = this.settings.getStringValue(key, "");
        if (pathString.equals("")) {
            pathString = defaultValue;
            this.setDirectory(key, pathString);
        }
        return this.checkDirectory(pathString, homePath);
    }

    private Path checkDirectory(@NotNull String pathString, Path defaultPath) {
        if (!pathString.equals("")) {
            Path path = Paths.get(pathString);
            if (Files.exists(path)) {
                return path;
            }
        }
        return defaultPath;
    }

    private void setDirectory(String key, @NotNull Path path) {
        this.setDirectory(key, path.toString());
    }

    private void setDirectory(String key, String pathString) {
        this.settings.setStringValue(key, pathString);
        this.settings.saveSetting();
    }

    public boolean getUseContentBasedTypeIcons() {
        /*
         * Use icons in res/mime instead of system icons.
         * Needed for linux as system icons only show two types of icons otherwise (files and folders)
         * There's no menu option for this preference, but you can manually modify the settings file yourself to enable it
         * this option is also automatically enabled on linux
         */

        // if we're on linux we want the content based icons as default
        boolean defaultValue = SystemUtils.IS_OS_LINUX;

        return this.settings.getBooleanValue("G-ContentBasedTypeIcons", defaultValue);
    }

    public void setUseContentBasedTypeIcons(boolean preferenceValue) {
        this.settings.setBooleanValue("G-ContentBasedTypeIcons", preferenceValue);
    }

    /*
        public String getPathToExternalMediaPlayer() {
            return getStringValue(Key.pathToExternalMediaPlayer, null);
        }

        public void setPathToExternalMediaPlayer(String externalMediaPlayer) {
            setStringValue(Key.pathToExternalMediaPlayer, externalMediaPlayer);
        }
    */
    public Integer getWidth() {
        return this.settings.getIntValue("G-Width", 1056);
    }

    public void setWidth(Integer value) {
        this.settings.setIntValue("G-Width", value);
    }

    public Integer getHeight() {
        return this.settings.getIntValue("G-Height", 678);
    }

    public void setHeight(Integer value) {
        this.settings.setIntValue("G-Height", value);
    }
}
