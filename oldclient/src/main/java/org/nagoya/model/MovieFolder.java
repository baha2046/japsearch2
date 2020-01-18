package org.nagoya.model;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.io.LocalVFS;
import org.nagoya.io.Setting;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.preferences.CustomOptions;
import org.nagoya.preferences.RenameSettings;
import org.nagoya.system.database.MovieDB;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class MovieFolder {
    public static int MAX_FILES_IN_FOLDER = 30;
    public static int MAX_MOVIES_COUNT = 10;

    public static boolean SAVE_POSTER = true;
    public static boolean SAVE_ACTOR = true;
    public static boolean SAVE_RENAME_MOVIE = true;

    public static boolean NAMING_FIXED_NFO = false;
    public static boolean NAMING_FIXED_POSTER = false;

    public static CustomOptions OPTIONS = getOptions();

    public static String CUSTOM_FLAG_FILE = "flag.ini";

    @NotNull
    public static CustomOptions getOptions() {
        CustomOptions options = new CustomOptions("Movie Folder");
        options.addOption(MovieFolder.class.getSimpleName() + "-MaxFileInFolder", MAX_FILES_IN_FOLDER, (v) -> MAX_FILES_IN_FOLDER = v, "Max File Count : ");
        options.addOption(MovieFolder.class.getSimpleName() + "-MaxMovieCount", MAX_MOVIES_COUNT, (v) -> MAX_MOVIES_COUNT = v, "Max Movie File : ");

        options.addRow("Naming Option");
        options.addOption("P-NfoNameFixed", NAMING_FIXED_NFO, (v) -> NAMING_FIXED_NFO = v, "Fixed Nfo Name : ");
        options.addOption("P-PosterNameFixed", NAMING_FIXED_POSTER, (v) -> NAMING_FIXED_POSTER = v, "Fixed Poster Name : ");

        options.addRow("Save Option");
        options.addOption("P-RenameMovieFiles", SAVE_RENAME_MOVIE, (v) -> SAVE_RENAME_MOVIE = v, "Rename Movie Files : ");
        options.addOption("P-WritePoster", SAVE_POSTER, (v) -> SAVE_POSTER = v, "Write Posters : ");
        options.addOption("P-WriteActor", SAVE_ACTOR, (v) -> SAVE_ACTOR = v, "Write Actor Image : ");
        return options;
    }

    private final DirectoryEntry directoryEntry;

    private Vector<Tuple2<FileObject, Long>> movieFilesPath;
    private Path nfoPath = null;
    private Path frontCoverPath = null;
    private Path backCoverPath = null;
    private Path folderImagePath = null;
    private Path trailerPath = null;
    private Path extraArtFolderPath = null;
    private Path actorFolderPath = null;

    private final boolean isVirtual;

    private Option<DirectoryEntry> dFrontCover = Option.none();
    private Option<DirectoryEntry> dBackCover = Option.none();
    private Option<DirectoryEntry> dFolderImage = Option.none();
    private Option<DirectoryEntry> dActorFolder = Option.none();
    private Option<DirectoryEntry> dExtraImageFolder = Option.none();
    private Option<DirectoryEntry> dNfoFile = Option.none();

    private Option<MovieV2> movieData = Option.none();

    private transient Option<Supplier<CustomFlag>> customFlagSupplier = Option.none();

    static class CustomFlag {
        boolean hd;
        boolean waterMark;

        CustomFlag() {
            this.hd = false;
            this.waterMark = false;
        }

        CustomFlag(boolean hd, boolean waterMark) {
            this.hd = hd;
            this.waterMark = waterMark;
        }

        public boolean isHd() {
            return this.hd;
        }

        public void setHd(boolean hd) {
            this.hd = hd;
        }

        public boolean isWaterMark() {
            return this.waterMark;
        }

        public void setWaterMark(boolean waterMark) {
            this.waterMark = waterMark;
        }
    }

    @NotNull
    public static Option<MovieFolder> create(@NotNull DirectoryEntry entry) {
        if (entry.preCalChildLength() > MAX_FILES_IN_FOLDER) {
            return Option.none();
        }

        var scanMovie = getMovieFilePaths(entry.getChildrenEntry());
        var nfoFile = getNfoFile(entry.getChildrenEntry());

        if (scanMovie.isEmpty() || scanMovie.length() > MAX_MOVIES_COUNT) {
            if (nfoFile.isDefined()) {
                String movieName = FilenameUtils.removeExtension(nfoFile.get().getFileName());
                return createVirtual(entry, movieName);
            } else {
                return Option.none();
            }
        } else {
            return Option.of(new MovieFolder(entry, scanMovie));
        }

    }

    @Contract("_, _ -> !null")
    public static Option<MovieFolder> createVirtual(@NotNull DirectoryEntry entry, String movieName) {
        return Option.of(new MovieFolder(entry, movieName));
    }

    public static Mono<MovieV2> writeMovieProcess(@NotNull MovieFolder moviePath, Option<ObservableList<String>> outText) {
        return Mono.just(moviePath.getMovieData().getOrNull())
                .<MovieV2>handle((m, sink) -> {
                    if (m == null) {
                        GUICommon.writeToObList(">> Error >> No movie data exist", outText);
                        sink.error(new Exception("No movie data"));
                    } else {
                        if (!m.hasValidTitle()) {
                            GUICommon.writeToObList(">> Error >> No match for this movie in the array or there was no title filled in; skipping writing", outText);
                            sink.error(new Exception("No movie title"));
                        } else {
                            sink.next(m);
                        }
                    }
                })
                .doOnNext(m -> {
                    GUICommon.writeToObList("=======================================================", outText);
                    GUICommon.writeToObList("Writing movie: " + m.getMovieTitle(), outText);
                    GUICommon.writeToObList("=======================================================", outText);
                })
                .doOnNext(m -> {
                    if (SAVE_RENAME_MOVIE) {
                        renameMovieFiles(moviePath, RenameSettings.getSuitableFileName(m), outText);
                    }
                })
                .doOnNext(m -> m.useLocalPathForFrontCover(moviePath.getFrontCoverPath()))
                .doOnNext(m -> m.writeNfoFile(moviePath.getNfoPath(), outText))
                .doOnNext(m -> m.writeCoverToFile(
                        SAVE_POSTER ? moviePath.getFrontCoverPath() : null,
                        SAVE_POSTER ? moviePath.getBackCoverPath() : null,
                        SAVE_POSTER ? moviePath.getFolderImagePath() : null,
                        outText))
                .doOnNext(m -> m.writeExtraImages(
                        MovieV2.SCRAPE_EXTRA_IMAGE ? moviePath.getExtraArtFolderPath() : null,
                        outText))
                .doOnNext(m -> m.writeActorImages(
                        SAVE_ACTOR ? moviePath.getActorFolderPath() : null,
                        outText))
                .doOnNext(m -> {
                    GUICommon.writeToObList("=======================================================", outText);
                    GUICommon.writeToObList("FINISH WRITE TO FILE", outText);
                    GUICommon.writeToObList("=======================================================", outText);
                });
    }

    private static void renameMovieFiles(@NotNull MovieFolder moviePaths, String movieName, Option<ObservableList<String>> outText) {

        boolean bSuccess = moviePaths.getDirectoryEntry().toFileObject().flatMap(fileObject ->
                moviePaths.getMovieFilesPath()
                        .map(Tuple2::_1)
                        .zipWithIndex((p, i) -> {
                            int index = moviePaths.getMovieFilesPath().size() > 1 ? i + 1 : 0;
                            return Try.run(() -> renameMovieFile(fileObject, movieName, p, index, outText)).map(v -> true).getOrElse(false);
                            //return renameMovieFile(basePath, movieName, p, index, outText);
                        })
                        .find(b -> !b))
                .getOrElse(true);

        if (bSuccess && moviePaths.hasNfo()) {
            UtilCommon.tryDeleteFileIfExist(moviePaths.getNfoPath());
            UtilCommon.tryDeleteFileIfExist(moviePaths.getNfoPath());
            UtilCommon.tryDeleteFileIfExist(moviePaths.getFrontCoverPath());
            UtilCommon.tryDeleteFileIfExist(moviePaths.getBackCoverPath());
            MovieDB.getInstance().removeCache(moviePaths.getNfoPath().toString());
        }

        moviePaths.resolvePaths(moviePaths.getDirectoryEntry().getValue(), movieName);
       /* try {
            if (!moviePath.isVirtual()) {
                int Naming = 1;
                for (Path p : moviePath.getMovieFilesPath().map(Tuple2::_1)) {
                    String ext = FilenameUtils.getExtension(p.getFileName().toString());
                    Path idealPath;
                    if (moviePath.getMovieFilesPath().size() > 1) {
                        idealPath = basePath.resolve(movID + " pt" + Naming + "." + ext);
                        Naming++;
                    } else {
                        idealPath = basePath.resolve(movID + "." + ext);
                    }
                    GUICommon.writeToObList(">> " + idealPath.getFileName(), outText);
                    Files.move(p, idealPath);
                }
            }
            if (moviePath.hasNfo()) {
                Files.deleteIfExists(moviePath.getNfoPath());
                Files.deleteIfExists(moviePath.getFrontCoverPath());
                Files.deleteIfExists(moviePath.getBackCoverPath());
                MovieV2Cache.getInstance().removeCache(moviePath.getNfoPath().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    private static void renameMovieFile(FileObject baseFolder, String movieID, @NotNull FileObject oldFile, int index, Option<ObservableList<String>> outText) throws FileSystemException {
        String ext = oldFile.getName().getExtension();//FilenameUtils.getExtension(oldPath.getFileName().toString());
        FileObject idealFile;
        if (index > 0) {
            idealFile = baseFolder.resolveFile(movieID + " pt" + index + "." + ext);
        } else {
            idealFile = baseFolder.resolveFile(movieID + "." + ext);
        }
        oldFile.moveTo(idealFile);
    }

    private MovieFolder(DirectoryEntry entry, String movieName) {
        this.isVirtual = true;
        this.directoryEntry = entry;
        this.movieFilesPath = Vector.empty();

        this.resolvePaths(this.directoryEntry.getValue(), movieName);
        this.mapPathsToLocalChild(this.directoryEntry.getChildrenEntry());
    }

    private MovieFolder(DirectoryEntry entry, Vector<Tuple2<FileObject, Long>> moviePaths) {
        this.isVirtual = false;
        this.directoryEntry = entry;
        this.movieFilesPath = moviePaths;

        String movieName = MovieV2.getUnstackedMovieName(this.movieFilesPath.get()._1.getName().getBaseName());//this.movieFilesPath.get()._1);

        this.resolvePaths(this.directoryEntry.getValue(), movieName);

        // Temp fix
    /*    Path oldExtraPath = this.getDirectoryEntry().getValue().resolve("extrafanart");
        if (Files.exists(oldExtraPath) && Files.isDirectory(oldExtraPath)) {
            UtilCommon.tryMoveFile(oldExtraPath, this.extraArtFolderPath);
            this.directoryEntry.resetChild();
        }
        Path oldBackCoverPath = this.getDirectoryEntry().getValue().resolve(movieName + "-landscape.jpg");
        if (Files.exists(oldBackCoverPath)) {
            UtilCommon.tryMoveFile(oldBackCoverPath, this.backCoverPath);
            this.directoryEntry.resetChild();
        }*/

        this.mapPathsToLocalChild(this.directoryEntry.getChildrenEntry());
    }

    private void resolvePaths(@NotNull Path dirPath, String movName) {
        this.nfoPath = dirPath.resolve(RenameSettings.getFileNameNfo(movName));
        this.frontCoverPath = dirPath.resolve(RenameSettings.getFileNameFrontCover(movName));
        this.backCoverPath = dirPath.resolve(RenameSettings.getFileNameBackCover(movName));
        this.folderImagePath = dirPath.resolve(RenameSettings.getFileNameFolderJpg());
        this.trailerPath = dirPath.resolve(RenameSettings.getFileNameTrailer(movName));
        this.actorFolderPath = dirPath.resolve(RenameSettings.getFolderNameActors());
        this.extraArtFolderPath = dirPath.resolve(RenameSettings.getFolderNameExtraImage());
    }

    private void mapPathsToLocalChild(Vector<DirectoryEntry> entries) {
        this.dNfoFile = pathToEntry(entries, this.nfoPath);

        if (this.dNfoFile.isEmpty()) {
            this.dNfoFile = entries.find(entry -> UtilCommon.checkFileExt.test(entry.getValue(), ".nfo"));
        }

        this.dFrontCover = pathToEntry(entries, this.frontCoverPath);
        this.dBackCover = pathToEntry(entries, this.backCoverPath);
        this.dFolderImage = pathToEntry(entries, this.folderImagePath);
        this.dExtraImageFolder = pathToEntry(entries, this.extraArtFolderPath);
        this.dActorFolder = pathToEntry(entries, this.actorFolderPath);

        //movieData = MovieV2Cache.getInstance().loadFromCache(dNfoFile)

        if (this.hasNfo()) {
            Path nfoPath = this.dNfoFile.get().getValue();

            //GUICommon.debugMessage(nfoPath.toString());

            this.movieData = MovieDB.getInstance().loadFromCache(nfoPath);

            if (this.movieData.isEmpty()) {
                Mono.fromCallable(this::tryLoadMovie)
                        .<MovieV2>handle((m, sink) -> {
                            if (m.isEmpty()) {
                                sink.error(new Exception("No movie data"));
                            } else {
                                sink.next(m.get());
                            }
                        })
                        .map(this::patchFrontCoverWithLocal)
                        .map(this::patchBackCoverWithLocal)
                        .map(this::loadLocalExtraImage)
                        .map(this::loadLocalActorImage)
                        .map(Option::of)
                        .subscribe(this::setMovieData, GUICommon::errorDialog);

                if (this.movieData.isDefined()) {
                    MovieDB.getInstance().putCache(nfoPath, this.movieData.getOrNull(),
                            Try.of(() -> Files.getLastModifiedTime(nfoPath)).getOrNull());
                }
            } else {
                movieData.peek(this::loadLocalExtraImage);
                movieData.peek(this::loadLocalActorImage);
            }

            if (Files.exists(this.directoryEntry.getValue().resolve(CUSTOM_FLAG_FILE))) {
                this.customFlagSupplier = Option.of(() -> Setting.readSetting(CustomFlag.class, this.directoryEntry.getValue().resolve(CUSTOM_FLAG_FILE)));
            } else {
                this.customFlagSupplier = Option.none();
            }
        }
    }

    public Option<Tuple2<Boolean, Boolean>> getCustomFlag() {
        Option<CustomFlag> flag = this.customFlagSupplier.map(Supplier::get);
        flag.peek(f -> this.customFlagSupplier = Option.of(() -> f));
        return flag.map(f -> Tuple.of(f.isHd(), f.isWaterMark()));
    }

    public void saveCustomFlag(boolean hd, boolean water) {
        CustomFlag flag = new CustomFlag(hd, water);
        Setting.writeSetting(flag, this.directoryEntry.getValue().resolve(CUSTOM_FLAG_FILE));
        this.customFlagSupplier = Option.of(() -> flag);
    }

    public Option<MovieV2> getMovieData() {
        return this.movieData;
    }

    public void setMovieData(Option<MovieV2> movieData) {
        this.movieData = movieData;
    }

    public void setMovieData(MovieV2 movieData) {
        this.setMovieData(Option.of(movieData));
    }

    public DirectoryEntry getDirectoryEntry() {
        return this.directoryEntry;
    }

    void clear() {
        this.movieFilesPath = Vector.empty();
        if (this.hasNfo()) {
            MovieDB.getInstance().removeCache(this.nfoPath.toString());
            this.nfoPath = null;
        }
        this.dFrontCover = Option.none();
        this.dBackCover = Option.none();
        this.dFolderImage = Option.none();
        this.dActorFolder = Option.none();
        this.dExtraImageFolder = Option.none();
        this.dNfoFile = Option.none();
    }

    void releaseMemory() {
        this.movieData.peek(MovieV2::releaseMemory);
    }

    public Vector<Tuple2<FileObject, Long>> getMovieFilesPath() {
        return this.movieFilesPath;
    }

    public boolean hasNfo() {
        return this.dNfoFile.isDefined();
    }

    public boolean isVirtual() {
        return this.isVirtual;
    }

    public Path getNfoPath() {
        return this.nfoPath;
    }

    public Path getFrontCoverPath() {
        return this.frontCoverPath;
    }

    public Path getBackCoverPath() {
        return this.backCoverPath;
    }

    public Path getFolderImagePath() {
        return this.folderImagePath;
    }

    public Path getTrailerPath() {
        return this.trailerPath;
    }

    public Path getExtraArtFolderPath() {
        return this.extraArtFolderPath;
    }

    public Path getActorFolderPath() {
        return this.actorFolderPath;
    }

    public Option<DirectoryEntry> getdFrontCover() {
        return this.dFrontCover;
    }

    public Option<DirectoryEntry> getdBackCover() {
        return this.dBackCover;
    }

    public Option<DirectoryEntry> getdFolderImage() {
        return this.dFolderImage;
    }

    public Option<DirectoryEntry> getdActorFolder() {
        return this.dActorFolder;
    }

    public Option<DirectoryEntry> getdExtraImageFolder() {
        return this.dExtraImageFolder;
    }

    public Option<DirectoryEntry> getdNfoFile() {
        return this.dNfoFile;
    }

    public void setdNfoFile(Option<DirectoryEntry> dNfoFile) {
        this.dNfoFile = dNfoFile;
    }

    public Option<MovieV2> tryLoadMovie() {
        return MovieV2.fromNfoFile(this.getNfoPath()).onEmpty(() -> this.setdNfoFile(Option.none()));
    }

    @Override
    public String toString() {
        return "MovieFolder{" +
                "movieFilesPath=" + this.movieFilesPath +
                ", nfoPath=" + this.nfoPath +
                ", frontCoverPath=" + this.frontCoverPath +
                ", backCoverPath=" + this.backCoverPath +
                ", folderImagePath=" + this.folderImagePath +
                ", trailerPath=" + this.trailerPath +
                ", extraArtFolderPath=" + this.extraArtFolderPath +
                ", actorFolderPath=" + this.actorFolderPath +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        MovieFolder that = (MovieFolder) o;
        return this.movieFilesPath.equals(that.movieFilesPath) &&
                Objects.equals(this.nfoPath, that.nfoPath) &&
                Objects.equals(this.frontCoverPath, that.frontCoverPath) &&
                Objects.equals(this.backCoverPath, that.backCoverPath) &&
                Objects.equals(this.folderImagePath, that.folderImagePath) &&
                Objects.equals(this.trailerPath, that.trailerPath) &&
                Objects.equals(this.extraArtFolderPath, that.extraArtFolderPath) &&
                Objects.equals(this.actorFolderPath, that.actorFolderPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.movieFilesPath, this.nfoPath, this.frontCoverPath, this.backCoverPath, this.folderImagePath, this.trailerPath, this.extraArtFolderPath, this.actorFolderPath);
    }

    private static Option<DirectoryEntry> pathToEntry(Vector<DirectoryEntry> entries, Path path) {
        if (entries == null || path == null) {
            return Option.none();
        }
        return entries.find(c -> c.getValue().equals(path));
    }

    public static final BiPredicate<String, String> checkFileExt = (fileExt, string) -> fileExt.toLowerCase().equals(string);


    private static Vector<Tuple2<FileObject, Long>> getMovieFilePaths(@NotNull Vector<DirectoryEntry> pathVector) {
        Vector<String> movExt = Vector.of(MovieFilenameFilter.acceptedMovieExtensions);

        return pathVector.flatMap(DirectoryEntry::toFileObject)
                .filter(fo -> movExt.find(mExt -> checkFileExt.test(fo.getName().getExtension(), mExt)).isDefined())
                .map(fo -> Tuple.of(fo, LocalVFS.getSize(fo)));
        /*return pathVector
                .map(TreeItem::getValue)
                .filter(path -> movExt.find(ext -> UtilCommon.checkFileExt.test(path, ext)).isDefined())
                //.peek(p->GUICommon.debugMessage(p.getFileNameProperty().toString()))
                .map(path -> Tuple.of(path, (Try.of(() -> Files.size(path)).getOrElse(0L))));*/
    }

    private static Option<DirectoryEntry> getNfoFile(@NotNull Vector<DirectoryEntry> entries) {
        return entries.find(entry -> UtilCommon.checkFileExt.test(entry.getValue(), ".nfo"));
    }

    @NotNull
    @Contract("_ -> param1")
    private MovieV2 patchFrontCoverWithLocal(@NotNull MovieV2 movieV2) {
        Path poster = movieV2.getImgFrontCover()
                .filter(FxThumb::isLocal)
                .map(FxThumb::getLocalPath)
                .getOrElse(this.frontCoverPath);

        if (Files.exists(poster)) {
            movieV2.setImgFrontCover(FxThumb.of(poster));
        } else {
            movieV2.setImgFrontCover(Option.none());
        }
        return movieV2;
    }

    private MovieV2 patchBackCoverWithLocal(@NotNull MovieV2 movieV2) {
        if (Files.exists(this.backCoverPath)) {
            //GUICommon.debugMessage("patchBackCoverWithLocal Files.exists");
            movieV2.getImgBackCover().peek((t) -> {
                if (!t.isLocal()) {
                    t.setLocalPath(this.backCoverPath);
                }
            });
        }
        return movieV2;
    }

    private MovieV2 loadLocalExtraImage(@NotNull MovieV2 movieV2) {
        this.getdExtraImageFolder()
                .map(MovieFolder::tryLoadExtraImage)
                .peek(s -> s.onSuccess(movieV2::setImgExtras));
        return movieV2;
    }

    private MovieV2 loadLocalActorImage(@NotNull MovieV2 movieV2) {

        this.getdActorFolder()
                .peek(d -> {
                    tryLoadLocalActorImage(d, movieV2.getActorList());
                    //movieV2.setActorList(a.asJava());
                });

        return movieV2;
    }

    @NotNull
    private static Future<Stream<FxThumb>> tryLoadExtraImage(DirectoryEntry extraImagePath) {
        return Future.of(ExecuteSystem.getExecutorServices(ExecuteSystem.role.IMAGE),
                () -> extraImagePath.getChildrenEntry().toStream()
                        .filter(DirectoryEntry::isFile)
                        .map(TreeItem::getValue)
                        .filter(path -> UtilCommon.checkFileExt.test(path, ".jpg"))
                        .map(FxThumb::of)
        );
    }

    private static Vector<ActorV2> tryLoadLocalActorImage(@NotNull DirectoryEntry actorImagePath, List<ActorV2> actorList) {
        Vector<ActorV2> actorV2s = Vector.ofAll(actorList);//.filter(a -> !a.hasLocalImage());

        var needCheck = actorV2s.filter(a -> !a.hasLocalImage());
        actorImagePath.getChildrenEntry().filter(DirectoryEntry::isFile).map(TreeItem::getValue).forEach(path ->
                        needCheck.find(a -> path.getFileName().toString().startsWith(a.getName().replace(' ', '_')))
                                .peek(a -> a.setLocalImage(FxThumb.of(path)))
                //.peek(a -> GUICommon.debugMessage(() -> "Actor Image Found on Local"))
        );

        return actorV2s;
    }
}

