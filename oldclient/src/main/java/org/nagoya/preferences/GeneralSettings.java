package org.nagoya.preferences;


public class GeneralSettings extends SettingBase {

    private static GeneralSettings INSTANCE;

    public static synchronized GeneralSettings getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GeneralSettings();
        }
        return INSTANCE;
    }

    private GeneralSettings() {
    }

    public Boolean getScrapeInJapanese() {
        return this.settings.getBooleanValue("P-LangJapanese", true);
    }

    public void setScrapeInJapanese(Boolean preferenceValue) {
        this.settings.setBooleanValue("P-LangJapanese", preferenceValue);
    }

    public Boolean getIsFirstWordOfFileID() {
        return this.settings.getBooleanValue("P-FrontID", true);
    }

    public void setIsFirstWordOfFileID(Boolean preferenceValue) {
        this.settings.setBooleanValue("P-FrontID", preferenceValue);
    }


    /*
    enum Key implements Settings.Key {
        writeFanartAndPosters, //fanart and poster files will be downloaded and then written to disk when writing the movie's metadata.
        overwriteFanartAndPosters, //overwrites existing fanart and poster files when writing the metadata to disk
        downloadActorImagesToActorFolder, //creates .actor thumbnail files when writing the metadata
        extraFanartScrapingEnabled, //will attempt to scrape and write extrafanart
        createFolderJpg, //Folder.jpg will be created when writing the file. This is a copy of the movie's poster file. Used in windows to show a thumbnail of the folder in Windows Explorer.
        noMovieNameInImageFiles, //fanart and poster will be called fanart.jpg and poster.jpg instead of also containing with the movie's name within the file
        writeTrailerToFile, //Download the trailer file from the internet and write it to a file when writing the rest of the metadata.
        nfoNamedMovieDotNfo, //.nfo file written out will always be called "movie.nfo"
        useIAFDForActors, //No longer used. Replaced by Amalgamation settings.
        sanitizerForFilename, //Used to help remove illegal characters when renaming the file. For the most part, the user does not need to change this.
        renamerString, //Renamer string set in the renamer configuration gui to apply a renamer rule to the file's name
        folderRenamerString, ////Renamer string set in the renamer configuration gui to apply a renamer rule to the file's folder name
        renameMovieFile, //File will be renamed according to renamer rules when writing the movie file's metadata out to disk.
        scrapeInJapanese, //For sites that support it, downloaded info will be in Japanese instead of English
        scrapeActor, //Prompt user to manually provide their own url when scraping a file. Useful if search just can't find a file, but the user knows what to use anyways. Not intended to be left on all the time.
        considerUserSelectionOneURLWhenScraping, //Consider all selected items to be one 'movie'.  To keep from being prompted for each CD/Scene
        isFirstWordOfFileID, //Usually the scraper expects the last word of the file to be the ID. This option if enabled will instead look at the first word.
        appendIDToStartOfTitle, //Scraped ID will be put as the first word of the title if enabled. Useful for people who like to keep releases from the same company alphabetically together.
        useFilenameAsTitle, //Filename will be writen to the title field of the nfo file instead of using the scraped result
        selectArtManuallyWhenScraping, //Confirmation dialog to allow user to select art will be shown. If false, art is still picked, but it will be automatically chosen.
        selectSearchResultManuallyWhenScraping, //Confirmation dialog to allow user to pick which search result they want to use will be shown.
        confirmCleanUpFileNameNameBeforeRenaming, // Show a dialog asking the user to confirm the rename of a file each time using the File Name Cleanup feature
        frequentlyUsedGenres, //Used in genre editing to store user's list of frequently used genres to aid in quickly adding genres to a movie
        frequentlyUsedTags, //Used in tag editing to store user's list of frequently used tags to aid in quickly adding tags to a movie
        writeThumbTagsForPosterAndFanartToNfo //Whether to write the <thumb> tag into the nfo
        ;

        @Override
        public String getKey() {
            return "Preferences:" + this.toString();
        }
    }*/
}
