package org.nagoya.model;

import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controller.siteparsingprofile.SiteParsingProfile;
import org.nagoya.io.LocalVFS;
import org.nagoya.model.dataitem.*;
import org.nagoya.model.xmlserialization.KodiXmlMovieBean;
import org.nagoya.preferences.CustomOptions;
import org.nagoya.preferences.RenameSettings;
import org.nagoya.system.database.MovieDB;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MovieV2 {
    public static boolean SCRAPE_EXTRA_IMAGE = true;
    public static boolean SCRAPE_ACTOR = true;
    public static CustomOptions OPTIONS = getScrapeOptions();

    public static CustomOptions getScrapeOptions() {
        CustomOptions options = new CustomOptions("Movie Scape Option");
        options.addOption("P-ScrapeExtraImage", SCRAPE_EXTRA_IMAGE, (v) -> SCRAPE_EXTRA_IMAGE = v, "Scrape Extra Image : ");
        options.addOption("P-ScrapeActor", SCRAPE_ACTOR, (v) -> SCRAPE_ACTOR = v, "Scrape Actors : ");
        return options;
    }

    public final StringProperty movieIdProperty = new SimpleStringProperty();
    public final StringProperty titleProperty = new SimpleStringProperty();
    public final StringProperty makerProperty = new SimpleStringProperty();

    public final StringProperty studioProperty = new SimpleStringProperty("");
    public final StringProperty directorProperty = new SimpleStringProperty("");
    public final StringProperty plotProperty = new SimpleStringProperty("");
    public final StringProperty seriesProperty = new SimpleStringProperty("");
    public final StringProperty dateProperty = new SimpleStringProperty("");
    public final StringProperty yearProperty = new SimpleStringProperty("");
    public final StringProperty timeProperty = new SimpleStringProperty("");
    public final StringProperty mpaaProperty = new SimpleStringProperty("");
    public final StringProperty trailerProperty = new SimpleStringProperty("");

    public final BooleanProperty hasExtra = new SimpleBooleanProperty(false);

    public final ObjectProperty<Option<FxThumb>> imgFrontProperty = new SimpleObjectProperty<>(Option.none());
    public final ObjectProperty<Option<FxThumb>> imgBackProperty = new SimpleObjectProperty<>(Option.none());
    public final ObjectProperty<Stream<FxThumb>> extraImgProperty = new SimpleObjectProperty<>(Stream.empty());

    public final ListProperty<ActorV2> actorProperty = new SimpleListProperty<>(FXCollections.observableArrayList(ActorV2.extractor()));
    public final ListProperty<Genre> genresProperty = new SimpleListProperty<>(FXCollections.observableArrayList());


    @NotNull
    @Contract("_, _, _ -> new")
    public static MovieV2 of(String strID, String strTitle, String strMaker) {
        return new MovieV2(strID, strTitle, strMaker);
    }

    public static MovieV2 of(MovieStore store) {
        return new MovieV2(store);
    }

    public static MovieV2 clone(MovieV2 movieV2) {
        return new MovieV2(movieV2.toStore());
    }

    public static Option<MovieV2> fromNfoFile(Path nfoPath) {
        return Try.withResources(() -> Files.lines(nfoPath))
                .of(stream -> stream.collect(Collectors.joining("\n")))
                .map(MovieV2::fixXMLStringFront)
                .map(KodiXmlMovieBean::makeFromXML)
                .filter(Objects::nonNull)
                .map(k -> k.toMovie(nfoPath.getParent()))
                .toOption();
    }

    private static String fixXMLStringFront(String xmlStr) {
        if (xmlStr.contains("<?xml")) {
            while (!xmlStr.startsWith("<?xml")) {
                if (xmlStr.length() > 1) {
                    xmlStr = xmlStr.substring(1);
                } else {
                    break;
                }
            }
        }
        return xmlStr;
    }

    @NotNull
    public static Try<MovieV2> fromSearchResult(SimpleMovieData searchResultToUse, @NotNull SiteParsingProfile siteToParseFrom) {

        return siteToParseFrom.downloadDocument(searchResultToUse)
                .map(siteToParseFrom::setDocument)
                .map(MovieV2::new);
    }

    public static Vector<SimpleMovieData> scrapeMovie(SiteParsingProfile siteToParseFrom, String strSearch) {

        //If the user manually canceled the results on this scraper in a dialog box, just return a null movie


        if (siteToParseFrom.getDiscardResults()) {
            return Vector.empty();


        }

        String strURL = siteToParseFrom.createSearchString(strSearch);

        //no URL was passed in so we gotta figure it ourselves
        Try<Stream<SimpleMovieData>> tryGetSearchResult = siteToParseFrom.getSearchResults(strURL);

        GUICommon.debugMessage("Movie scrapeMovie -----------------------------");
        tryGetSearchResult
                .onFailure(e -> GUICommon.debugMessage("Fail at scrapeMovie >> " + e.getMessage()))
                .onSuccess(r -> GUICommon.debugMessage("Search Result Count >> " + r.size()))
                .onSuccess(r -> GUICommon.debugMessage("Search Result From >> " + siteToParseFrom.toString()))
        ;
        GUICommon.debugMessage("Movie scrapeMovie -----------------------------");

        Vector<SimpleMovieData> searchResults = tryGetSearchResult.getOrElse(Stream.empty()).toVector();
        //SearchResult[] searchResults = tryGetSearchResult.map(r -> r.toArray(new SearchResult[0])).getOrElse(new SearchResult[0]);

        int searchResultNumberToUse = 0;
        //loop through search results and see if URL happens to contain ID number in the URL. This will improve accuracy!
        int levDistanceOfCurrentMatch = 999999; // just some super high number
        String idFromMovieFile = SiteParsingProfile.findIDTagFromFile(strSearch, siteToParseFrom.isFirstWordOfFileIsID());
        String idFromMovieFileToMatch = idFromMovieFile.toLowerCase().replaceAll("-", "");

        for (int i = 0; i < searchResults.length(); i++) {
            String urlToMatch = searchResults.get(i).getStrUrl().toLowerCase().replaceAll("-", "");

            System.out.println("Movie scrapeMovie urlToMatch " + urlToMatch);

            //system.out.println("Comparing " + searchResults[i].toLowerCase() + " to " + idFromMovieFile.toLowerCase().replaceAll("-", ""));
            if (urlToMatch.contains(idFromMovieFileToMatch)) {
                //let's do some fuzzy logic searching to try to of the "best" match in case we got some that are pretty close
                //and update the variables accordingly so we know what our best match so far is
                int candidateLevDistanceOfCurrentMatch =
                        LevenshteinDistance.getDefaultInstance().apply(urlToMatch, idFromMovieFileToMatch);
                if (candidateLevDistanceOfCurrentMatch < levDistanceOfCurrentMatch) {
                    levDistanceOfCurrentMatch = candidateLevDistanceOfCurrentMatch;
                    searchResultNumberToUse = i;
                }
            }
        }

        if (searchResultNumberToUse != 0) {
            SimpleMovieData sr = searchResults.get(searchResultNumberToUse);
            searchResults = searchResults.removeAt(searchResultNumberToUse);
            searchResults = Vector.of(sr).appendAll(searchResults);
        }

        return searchResults;
    }

    //returns the movie file path without anything like CD1, Disc A, etc and also gets rid of the file extension
    //Example: MyMovie ABC-123 CD1.avi returns MyMovie ABC-123
    //Example2: MyMovie ABC-123.avi returns MyMovie ABC-123
    public static String getUnstackedMovieName(String file) {
        String fileName = file;
        fileName = FilenameUtils.removeExtension(fileName);
        fileName = SiteParsingProfile.stripDiscNumber(fileName);
        //fileName = replaceLast(fileName, file.getName(), SiteParsingProfile.stripDiscNumber(fileName));
        return fileName;
    }

    public static String getUnstackedMovieName(@NotNull Path file) {
        return getUnstackedMovieName(file.getFileName().toString());
    }

    public static String getUnstackedMovieName(@NotNull File file) {
        return getUnstackedMovieName(file.getName());
    }


    public static String getFileNameOfTrailer(File selectedValue) {
        //sometimes the trailer has a different extension
        //than the movie so we will try to brute force a find by trying all movie name extensions
      /*  for (String extension : MovieFilenameFilter.acceptedMovieExtensions) {
            String potentialTrailer = tryToFindActualTrailerHelper(selectedValue, "." + extension);
            if (potentialTrailer != null) {
                return potentialTrailer;
            }
        }
        return getTargetFilePath(selectedValue, "-trailer.mp4");*/
        return "";
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    public static String[] getSupportRenameElement() {
        return new String[]{"[", "]", "(", ")", "space", "#id", "#moviename", "#year", "#date", "#studio", "#maker"};
    }

    public static String getFormatUnit(MovieV2 movie, @NotNull String input) {
        String string = "";

        switch (input) {
            case "[":
            case "]":
            case "(":
            case ")":
                string = input;
                break;
            case "space":
                string = " ";
                break;
            case "#id":
                string = movie.getMovieID();
                break;
            case "#moviename":
                string = movie.getMovieTitle();
                string = string.replace("...", "");
                string = string.replace("/", "／");
                string = string.replace("?", "？");
                string = string.replace("!", "！");
                string = string.replace(">", "＞");
                string = string.replace("<", "＜　" +
                        "");
                if (string.length() > 62) {
                    string = string.substring(0, 62);
                }
                break;
            case "#year":
                string = movie.getYear();
                break;
            case "#date":
                string = movie.getReleaseDate();
                break;
            case "#studio":
                string = movie.getStudio();
                break;
            case "#maker":
                string = movie.getMovieMaker();
                break;
        }

        return string;
    }

    private MovieV2() {
    }

    private MovieV2(String strID, String strTitle, String strMaker) {
        this();
        this.movieIdProperty.set(strID);
        this.titleProperty.set(strTitle);
        this.makerProperty.set(strMaker);
    }

    private MovieV2(@NotNull MovieStore store) {
        this();
        this.movieIdProperty.set(store.movieID.getId());

        this.titleProperty.set(store.titleString);
        this.makerProperty.set(store.makerString);

        this.studioProperty.set(store.labelString);
        this.directorProperty.set(store.directorString);
        this.plotProperty.set(store.plotString);
        this.seriesProperty.set(store.seriesString == null? "" : store.seriesString);
        this.dateProperty.set(store.releaseDateString);
        this.yearProperty.set(store.releaseYearString);

        this.timeProperty.set(store.timeString);
        this.mpaaProperty.set(store.mpaaString);
        this.trailerProperty.set(store.trailerString);

        this.imgFrontProperty.set(store.imgFrontCover);
        this.imgBackProperty.set(store.imgBackCover);
        this.genresProperty.setAll(store.genreList);

        this.actorProperty.setAll(store.actorList.stream().map(ActorLink::toActor).collect(Collectors.toList()));
        this.extraImgProperty.set(store.imgExtras);

        MovieDB.directorDB().getOrElsePut(store.directorString, ()->new DirectorV2(store.directorString));
    }

    public MovieV2(@NotNull SiteParsingProfile siteToScrapeFrom) {
        this();

        this.movieIdProperty.set(siteToScrapeFrom.scrapeID().getId());
        this.titleProperty.set(siteToScrapeFrom.scrapeTitle());
        this.makerProperty.set(siteToScrapeFrom.scrapeMaker());

        this.seriesProperty.set(siteToScrapeFrom.scrapeSet());
        this.yearProperty.set(siteToScrapeFrom.scrapeYear());
        this.dateProperty.set(siteToScrapeFrom.scrapeReleaseDate().map(ReleaseDate::getReleaseDate).getOrElse(""));
        this.timeProperty.set(siteToScrapeFrom.scrapeRuntime());
        this.plotProperty.set(siteToScrapeFrom.scrapePlot());
        this.studioProperty.set(siteToScrapeFrom.scrapeStudio());
        this.mpaaProperty.set(siteToScrapeFrom.scrapeMPAA().map(MPAARating::getMPAARating).getOrElse(""));
        this.trailerProperty.set("");
        this.directorProperty.set(siteToScrapeFrom.scrapeDirector());

        this.imgFrontProperty.set(Option.none());
        this.imgBackProperty.set(siteToScrapeFrom.scrapeCover());

        this.extraImgProperty.set(SCRAPE_EXTRA_IMAGE ? siteToScrapeFrom.scrapeExtraImage() : Stream.empty());

        if (SCRAPE_ACTOR) {
            siteToScrapeFrom.scrapeActorsMono().subscribe(this.actorProperty::setAll);
        }

        this.genresProperty.setAll(siteToScrapeFrom.scrapeGenres());

        MovieDB.directorDB().getOrElsePut(directorProperty.get(), ()->new DirectorV2(directorProperty.get()));
    }

    public SimpleMovieData toSimpleMovieData() {
        return new SimpleMovieData(this.getMovieID(), this.getMovieTitle(), this.getImgFrontCover());
    }

    public void releaseMemory() {
        this.getImgFrontCover().peek(FxThumb::releaseMemory);
        this.getImgBackCover().peek(FxThumb::releaseMemory);
    }

    public boolean hasValidTitle() {
        return (this.titleProperty.get().length() > 0);
    }


    public void useLocalPathForFrontCover(Path pathFrontCover) {
        this.getImgFrontCover().peek(t -> t.setThumbURL(UtilCommon.pathToUrl(pathFrontCover)));
    }

    public void writeNfoFile(Path pathNfo, Option<ObservableList<String>> outText) {

        if (pathNfo != null) {
            String xml = new KodiXmlMovieBean(this).toXML();
            // add the xml header since xstream doesn't do this
            String finalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>" + "\n" + xml;
            //system.out.println("Xml I am writing to file: \n" + xml);

            Try.withResources(() -> Files.newBufferedWriter(pathNfo, StandardCharsets.UTF_8))
                    .of(writer -> {
                        writer.write(finalXml);
                        return true;
                    })
                    .onFailure(e -> GUICommon.writeToObList(">> Error at writing : " + pathNfo.getFileName().toString() + " " + e.getMessage(), outText))
                    .onSuccess(b -> GUICommon.writeToObList(">> " + pathNfo.getFileName().toString(), outText))
            ;
        }
    }

    public void writeCoverToFile(Path pathFront, Path pathBack, Path pathFolder, Option<ObservableList<String>> outText) {

        if (pathFront != null) {
            if (this.getImgFrontCover().map((t) -> t.writeImageToFile(pathFront.toFile())).getOrElse(false)) {
                GUICommon.writeToObList(">> " + pathFront.getFileName().toString(), outText);
            }
        }

        if (pathBack != null) {
            if (this.getImgBackCover().map((t) -> t.writeImageToFile(pathBack.toFile())).getOrElse(false)) {
                GUICommon.writeToObList(">> " + pathBack.getFileName().toString(), outText);
            }
        }

        if (pathFolder != null) {
            if (this.getImgFrontCover().map((t) -> t.writeImageToFile(pathFolder.toFile())).getOrElse(false)) {
                GUICommon.writeToObList(">> " + pathFolder.getFileName().toString(), outText);
            }
        }
    }

    public MovieV2 writeCoverToFileAsync(Path pathFront, Path pathBack, Path pathFolder, Option<ObservableList<String>> outText) {

        /*if (pathFront != null) {
            this.getImgFrontCover()
                    .map((t) -> t.writeImageToFileAsync(pathFront.toFile()))
                    .peek(f -> f.onSuccess(b -> GUICommon.writeToObList(">> " + pathFront.getFileName().toString(), outText)))
                    .peek(f -> f.onFailure(e -> GUICommon.writeToObList(">> " + pathFront.getFileName().toString() + " Failed >> " + e.getMessage(), outText)))
            ;
        }

        if (pathBack != null) {
            this.getImgBackCover()
                    .map((t) -> t.writeImageToFileAsync(pathBack.toFile()))
                    .peek(f -> f.onSuccess(b -> GUICommon.writeToObList(">> " + pathBack.getFileName().toString(), outText)))
                    .peek(f -> f.onFailure(e -> GUICommon.writeToObList(">> " + pathBack.getFileName().toString() + " Failed >> " + e.getMessage(), outText)))
            ;
        }

        if (pathFolder != null) {
            this.getImgFrontCover()
                    .map((t) -> t.writeImageToFileAsync(pathFolder.toFile()))
                    .peek(f -> f.onSuccess(b -> GUICommon.writeToObList(">> " + pathFolder.getFileName().toString(), outText)))
                    .peek(f -> f.onFailure(e -> GUICommon.writeToObList(">> " + pathFolder.getFileName().toString() + " Failed >> " + e.getMessage(), outText)))
            ;
        }*/

        return this;
    }

    public void writeExtraImages(Path pathExtraImage, Option<ObservableList<String>> outText) {
        var extraImage = this.getImgExtras()
                .map(FxThumb::getThumbURL)
                .filter(Objects::nonNull)
                .filter(url -> !url.getProtocol().equals("file"));

        if (pathExtraImage != null && extraImage.length() > 0) {

            /*Try.withResources(() -> Files.walk(pathExtraImage)).of(s ->
                    s.sorted(Comparator.reverseOrder()).map(Path::toFile).map(File::delete).allMatch(i -> i == true)
            );*/

            File descDir = pathExtraImage.toFile();
            Try.run(() -> FileUtils.cleanDirectory(descDir));

            if (!UtilCommon.createDirectory(pathExtraImage, outText)) {
                return;
            }

            Stream<Supplier<Boolean>> run = extraImage.zipWithIndex((url, i) -> {
                Path path = pathExtraImage.resolve(RenameSettings.getFileNameExtraImage() + i + ".jpg");
                return () -> UtilCommon.saveUrlToFile(url, path.toFile(), outText);
            });

            Flux.fromStream(run.toJavaStream())
                    .parallel()
                    .runOn(ExecuteSystem.get().getNormalScheduler())
                    .map(Supplier::get)
                    .sequential()
                    .blockLast();
        }
    }

    public void writeExtraImages(FileObject pathExtraImage, Option<ObservableList<String>> outText) {
        var extraImage = this.getImgExtras()
                .map(FxThumb::getThumbURL)
                .filter(Objects::nonNull)
                .filter(url -> !url.getProtocol().equals("file"));

        if (pathExtraImage != null && extraImage.length() > 0) {

            try {
                if (!pathExtraImage.exists()) {
                    pathExtraImage.createFolder();
                }
                if (pathExtraImage.isFile()) {
                    return;
                }
            } catch (FileSystemException e) {
                org.nagoya.commons.GUICommon.errorDialog(e);
                return;
            }

            Stream<Boolean> run = extraImage.zipWithIndex((url, i) -> {
                try {
                    FileObject target = pathExtraImage.resolveFile(RenameSettings.getFileNameExtraImage() + i + ".jpg");
                    FileSystemManager fsm = LocalVFS.get().getOrElseThrow(() -> new Exception("Cannot get File Manager"));
                    fsm.resolveFile(url).moveTo(target);
                    return true;
                } catch (Exception e) {
                    org.nagoya.commons.GUICommon.errorDialog(e);
                    return false;
                }
            });

            Flux.fromStream(run.toJavaStream())
                    .parallel()
                    .runOn(ExecuteSystem.get().getNormalScheduler())
                    //.map(Supplier::get)
                    .sequential()
                    .blockLast();
        }
    }


    public void writeActorImages(Path pathActorImage, Option<ObservableList<String>> outText) {

        //Don't create an empty .actors folder with no actors underneath it
        if (this.getActorList().size() > 0 && pathActorImage != null) {

            if (!UtilCommon.createDirectory(pathActorImage, outText)) {
                return;
            }

            try {
                //on windows this new folder should have the hidden attribute; on unix it is already "hidden" by having a . in front of the name
                //if statement needed for Linux checking .actors hidden flag when .actors is a symlink
                if (!Files.isHidden(pathActorImage)) {
                    Boolean hidden = (Boolean) Files.getAttribute(pathActorImage, "dos:hidden", LinkOption.NOFOLLOW_LINKS);
                    if (hidden != null && !hidden) {
                        try {
                            Files.setAttribute(pathActorImage, "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
                        } catch (AccessDeniedException e) {
                            System.err.println("I was not allowed to make .actors folder hidden. This is not a big deal - continuing with write of actor files...");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.getActorList().forEach(a -> {
                String currentActorToFileName = a.getName().replace(' ', '_');
                Path path = pathActorImage.resolve(currentActorToFileName + ".jpg");
                UtilCommon.saveImageToFile(path, a::writeImageToFile, outText);
            });
        }
    }

    public String getMovieID() {
        return this.movieIdProperty.get();
    }

    public void setMovieID(String movieID) {
        this.movieIdProperty.set(movieID);
    }

    public String getMovieTitle() {
        return this.titleProperty.get();
    }

    public void setMovieTitle(String movieTitle) {
        this.titleProperty.set(movieTitle);
    }

    public String getMovieMaker() {
        return this.makerProperty.get();
    }

    public void setMovieMaker(String movieMaker) {
        this.makerProperty.set(movieMaker);
    }

    public String getReleaseDate() {
        return this.dateProperty.get();
    }

    public Option<LocalDate> getReleaseDateAsLocalDate() {
        return Try.of(() -> LocalDate.parse(this.dateProperty.get())).toOption();
    }

    public void setReleaseDate(String releaseDates) {
        this.dateProperty.set(releaseDates);
    }

    public String getStudio() {
        return this.studioProperty.get();
    }

    public void setStudio(String studios) {
        this.studioProperty.set(studios);
    }

    public String getSeries() {
        return this.seriesProperty.get();
    }

    public void setSeries(String sets) {
        this.seriesProperty.set(sets);
    }

    public String getYear() {
        return this.yearProperty.get();
    }

    public void setYear(String years) {
        this.yearProperty.set(years);
    }

    public String getMpaaRating() {
        return this.mpaaProperty.get();
    }

    public void setMpaaRating(String mpaaRatings) {
        this.mpaaProperty.set(mpaaRatings);
    }

    public String getPlot() {
        return this.plotProperty.get();
    }

    public void setPlot(String plots) {
        this.plotProperty.set(plots);
    }

    public String getRuntime() {
        return this.timeProperty.get();
    }

    public void setRuntime(String time) {
        this.timeProperty.set(time);
    }

    public String getTrailers() {
        return this.trailerProperty.get();
    }

    public void setTrailers(String trailers) {
        this.trailerProperty.set(trailers);
    }

    public ObservableList<Genre> getGenreList() {
        return this.genresProperty.get();
    }

    public void setGenreList(List<Genre> genreList) {
        this.genresProperty.setAll(genreList);
    }

    public DirectorV2 getDirector() {
        return DirectorV2.of(this.directorProperty.get());
    }

    public String getDirectorString() {
        return this.directorProperty.get();
    }

    public void setDirector(String director) {
        this.directorProperty.set(director);
    }

    public ObservableList<ActorV2> getActorList() {
        return this.actorProperty.get();
    }

    public void setActorList(List<ActorV2> actorList) {
        this.actorProperty.setAll(actorList);
    }

    public Option<FxThumb> getImgFrontCover() {
        return this.imgFrontProperty.get();
    }

    public void setImgFrontCover(FxThumb imgFrontCover) {
        this.setImgFrontCover(Option.of(imgFrontCover));
    }

    public void setImgFrontCover(Option<FxThumb> imgFrontCover) {
        this.imgFrontProperty.set(imgFrontCover);
    }

    public Option<FxThumb> getImgBackCover() {
        return this.imgBackProperty.get();
    }

    public void setImgBackCover(FxThumb imgBackCover) {
        this.setImgBackCover(Option.of(imgBackCover));
    }

    public void setImgBackCover(Option<FxThumb> imgBackCover) {
        this.imgBackProperty.set(imgBackCover);
    }

    public Stream<FxThumb> getImgExtras() {
        return this.extraImgProperty.get();
    }

    public void setImgExtras(Stream<FxThumb> imgExtras) {
        this.extraImgProperty.set(imgExtras);
    }

    public boolean hasBackCover() {
        return this.imgBackProperty.get().isDefined();
    }

    public boolean hasFrontCover() {
        return this.imgFrontProperty.get().isDefined();
    }

    public SimpleMovieData toSimpleMovie() {
        return new SimpleMovieData(this);
    }

    public MovieStore toStore() {
        return new MovieStore(this);
    }
}

