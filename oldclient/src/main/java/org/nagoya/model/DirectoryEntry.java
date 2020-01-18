package org.nagoya.model;


import io.vavr.CheckedFunction0;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.io.LocalVFS;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.model.dataitem.MakerData;
import org.nagoya.system.Systems;
import org.nagoya.system.cache.IconCache;
import org.nagoya.system.database.MovieDB;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.view.dialog.FXMoveMappingDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;


public class DirectoryEntry extends TreeItem<Path> implements CheckedFunction0<Boolean>, Runnable {

    private Option<FileObject> fileObject = Option.none();

    private final DirectoryEntry parent;
    private final ChildListManager childListManager = new ChildListManager(this);
    private final boolean directory;
    private FileTime lastModified = null;

    private Option<MovieFolder> movieFolder = Option.none();
    private Option<MakerData> makerData = Option.none();
    private Option<GalleryFolder> galleryFolder = Option.none();

    private boolean needCheck = true;
    private final BooleanProperty needCheckProperty = new SimpleBooleanProperty(true);
    private final StringProperty fileNameProperty = new SimpleStringProperty("");
    private final StringProperty fileExtensionProperty = new SimpleStringProperty("");

    private Option<Image> fileIcon;
    private Option<Long> size = Option.none();

    private int lastSelectedIndex = -1;

    private DirectoryEntry(FileObject fileObject, DirectoryEntry parent) {
        this.fileObject = Option.of(fileObject);
        FileName fileName = fileObject.getName();

        Path path = Paths.get(LocalVFS.fileNameToPath(fileName));

/*
        GUICommon.debugMessage("fileObject.getPublicURIString() " + fileObject.getPublicURIString());
        GUICommon.debugMessage("fileObject.getName().getPath() " + fileObject.getName().getPath());
        GUICommon.debugMessage("fileObject.getName().getBaseName() " + fileObject.getName().getBaseName());
        GUICommon.debugMessage("fileObject.getName().getExtension() " + fileObject.getName().getExtension());
        GUICommon.debugMessage("fileObject.getName().getFriendlyURI() " + fileObject.getName().getFriendlyURI());
        try {
            GUICommon.debugMessage("fileObject.getName().getPathDecoded() " + fileObject.getName().getPathDecoded());
        } catch (FileSystemException e) {
            e.printStackTrace();
        }
*/

        this.parent = parent;
        this.setValue(path);

        this.needCheckProperty.setValue(true);
        this.fileNameProperty.set(fileName.getBaseName());
        this.directory = Try.of(() -> fileObject.getType().equals(FileType.FOLDER)).getOrElse(Files.isDirectory(path));

        if (this.isDirectory()) {
            this.fileExtensionProperty.set("Folder");
            this.size = Option.none();
        } else {
            this.fileExtensionProperty.set("File");
            this.size = Try.of(() -> Files.size(this.getValue())).toOption();
        }

        /*if (MakerData.isExist(this.getValue())) {
            GUICommon.debugMessage("MD " + this.getValue());
            this.makerData = Option.of(MakerData.of(this.getValue()));
        }*/

        //GUICommon.debugMessage(Systems.getDirectorySystem().getMakerDB());

        this.makerData = MovieDB.makerDB().getData(this.getValue().toString());
        this.fileIcon = Try.of(() -> (ImageIcon) IconCache.getIconFromCache(this.getValue().toFile()))
                .map(ic -> {
                    BufferedImage bImg;
                    if (ic.getImage() instanceof BufferedImage) {
                        bImg = (BufferedImage) ic.getImage();
                    } else {
                        bImg = new BufferedImage(ic.getImage().getWidth(null), ic.getImage().getHeight(null), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D graphics = bImg.createGraphics();
                        graphics.drawImage(ic.getImage(), 0, 0, null);
                        graphics.dispose();
                    }
                    return bImg;
                })
                .map(bImg -> (Image) SwingFXUtils.toFXImage(bImg, null))
                .toOption();

        this.setGraphic(new ImageView(this.fileIcon.getOrNull()));
    }

    private DirectoryEntry(DirectoryEntry parent) {
        this.parent = parent;
        this.directory = false;
    }

    private DirectoryEntry(@NotNull Path inPath, DirectoryEntry parent) {
        this.parent = parent;
        this.setValue(inPath);

        this.fileObject = LocalVFS.resolveFile(inPath.toUri());

        this.needCheckProperty.setValue(true);
        this.fileNameProperty.set(this.fileObject.map(FileObject::getName).map(FileName::getBaseName).getOrElse(""));//Option.of(inPath.getFileName()).map(Path::toString).getOrElse(""));
        this.directory = Files.isDirectory(inPath);

        if (this.isDirectory()) {
            this.fileExtensionProperty.set("Folder");
            this.size = Option.none();
        } else {
            this.fileExtensionProperty.set(this.fileObject.map(FileObject::getName).map(FileName::getExtension).getOrElse("File"));
            this.size = Try.of(() -> Files.size(this.getValue())).toOption();
        }

        /*if (MakerData.isExist(this.getValue())) {
            GUICommon.debugMessage("MD " + this.getValue());
            this.makerData = Option.of(MakerData.of(this.getValue()));
        }*/

        //GUICommon.debugMessage(Systems.getDirectorySystem().getMakerDB());

        this.makerData = MovieDB.makerDB().getData(this.getValue().toString());
        this.fileIcon = Try.of(() -> (ImageIcon) IconCache.getIconFromCache(this.getValue().toFile()))
                .map(ic -> {
                    BufferedImage bImg;
                    if (ic.getImage() instanceof BufferedImage) {
                        bImg = (BufferedImage) ic.getImage();
                    } else {
                        bImg = new BufferedImage(ic.getImage().getWidth(null), ic.getImage().getHeight(null), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D graphics = bImg.createGraphics();
                        graphics.drawImage(ic.getImage(), 0, 0, null);
                        graphics.dispose();
                    }
                    return bImg;
                })
                .map(bImg -> (Image) SwingFXUtils.toFXImage(bImg, null))
                .toOption();

        this.setGraphic(new ImageView(this.fileIcon.getOrNull()));
    }
    // standard constructors/getters

    public DirectoryEntry getEntryParent() {
        return this.parent;
    }

    public Vector<DirectoryEntry> getChildrenEntry() {
        return this.childListManager.getEntryList();
    }

    public Vector<DirectoryEntry> getSortedChildrenEntry() {
        return this.childListManager.getSortedEntryList();
    }

    public int preCalChildLength() {
        return this.childListManager.preCalEntryLength();
    }

    public void removeChild(DirectoryEntry entry) {
        this.childListManager.removeEntryFromList(entry);
    }

    public void addChild(DirectoryEntry entry) {
        this.childListManager.addEntryToList(entry);
    }

    public void replaceChild(DirectoryEntry oldEntry, DirectoryEntry newEntry) {
        this.childListManager.replaceEntryFromList(oldEntry, newEntry);
    }

    public void resortChild() {
        this.childListManager.resortList();
    }

    public void releaseMemory() {
        this.movieFolder.peek(MovieFolder::releaseMemory);
    }

    public void releaseChildMemory() {
        this.getChildrenEntry().forEach(DirectoryEntry::releaseMemory);
    }

    public void resetChild() {
        this.childListManager.reset();
    }

    @NotNull
    @Contract("_ -> new")
    public static DirectoryEntry of(Path inPath) {
        return new DirectoryEntry(inPath, null);
    }

    @Contract("_, _ -> new")
    @NotNull
    public static DirectoryEntry of(Path inPath, DirectoryEntry parent) {
        return new DirectoryEntry(inPath, parent);
    }

    public static DirectoryEntry of(FileObject fileObject) {
        return of(fileObject, null);
    }

    public static DirectoryEntry of(FileObject fileObject, DirectoryEntry parent) {
        DirectoryEntry directoryEntry = new DirectoryEntry(fileObject, parent);
        Systems.useExecutors(directoryEntry);
        return directoryEntry;
    }

    @NotNull
    public static DirectoryEntry getAndExe(Path inPath) {
        DirectoryEntry directoryEntry = new DirectoryEntry(inPath, null);
        Systems.useExecutors(directoryEntry);
        return directoryEntry;
    }

    @NotNull
    @Contract(pure = true)
    public static Callback<DirectoryEntry, Observable[]> extractor() {
        return (DirectoryEntry p) -> new Observable[]{p.getNeedCheckProperty()};
    }

    private Future<Void> tryLoadLocalActorImageAsync(DirectoryEntry actorImagePath, List<ActorV2> actorList) {

        Stream<ActorV2> actorV2Stream = Stream.ofAll(actorList);

        return Future.run(ExecuteSystem.getExecutorServices(ExecuteSystem.role.IMAGE),
                () -> actorImagePath.getChildrenEntry().toStream()
                        .filter(DirectoryEntry::isFile)
                        .map(TreeItem::getValue)
                        //.peek(p -> GUICommon.debugMessage("--Actor " + p.getFileName().toString()))
                        .peek(path -> {
                            actorV2Stream
                                    .find(a -> path.getFileName().toString().startsWith(a.getName().replace(' ', '_')))
                                    .peek(a -> a.setLocalImage(FxThumb.of(path)))
                                    .peek(a -> GUICommon.debugMessage("Actor Image Found on Local"))
                            ;
                        })
        );
    }

    private void extraCheckForFile() {
        String strExtension;
        try {
            this.lastModified = Files.getLastModifiedTime(this.getValue());
            strExtension = Files.probeContentType(this.getValue());

            if (strExtension == null) {
                InputStream is = new BufferedInputStream(new FileInputStream(this.getValue().toFile()));
                strExtension = URLConnection.guessContentTypeFromStream(is);
                is.close();
            }
            if (strExtension != null) {
                String ext = strExtension;
                Platform.runLater(() -> this.fileExtensionProperty.set(ext));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extraCheckForDirectory() {
        // Check if there has movie files and then build paths data
        this.movieFolder = MovieFolder.create(this);

        this.size = this.movieFolder.map(v -> v.getMovieFilesPath().map(Tuple2::_2).sum().longValue());
        if (this.size.getOrElse(0L) < 1L) {
            this.size = Option.none();
        }

        //GUICommon.debugMessage("extraCheckForDirectory return " + this.getValue().getFileName().toString());
        //if (this.movieFolder.isEmpty()) {
        //this.galleryFolder = GalleryFolder.create(this);
        //}
    }

    public void clearCache() {
        this.movieFolder.peek(MovieFolder::clear);
        this.movieFolder = Option.none();
        this.resetChild();
        this.setNeedCheck(true);
        this.setNeedCheckProperty(true);
    }

    public void writeMovieDataToFile(MovieV2 movieV2, Option<ObservableList<String>> outText, Runnable runWhenFinish) {
        Runnable writeAction = () -> {
            this.setMovieData(movieV2);
            this.doWriteMovieData(outText, runWhenFinish);
        };

        if (this.movieFolder.isEmpty()) {
            DialogBuilder.create()
                    .heading("[ Confirm ]")
                    .body("No movie files, create virtual movie?")
                    .buttonYesNo(() -> {
                        this.movieFolder = MovieFolder.createVirtual(this, movieV2.getMovieID());
                        writeAction.run();
                    })
                    .build().show();
        } else {
            writeAction.run();
        }
    }

    private void doWriteMovieData(Option<ObservableList<String>> outText, Runnable runWhenFinish) {

        MovieFolder.writeMovieProcess(this.movieFolder.get(), outText)
                .subscribeOn(ExecuteSystem.get().getIoScheduler())
                .subscribe(b -> {
                    String folderName = FXMoveMappingDialog.getSuitableFolderName(this);
                    Path newPath = this.getValue().resolveSibling(folderName.replace(":", " "));
                    if (!Files.exists(newPath)) {
                        UtilCommon.tryMoveFile(this.getValue(), newPath)
                                .onSuccess(this::setValue)
                                .onSuccess(p -> GUICommon.runOnFx(() -> this.fileNameProperty.set(Option.of(p.getFileName()).map(Path::toString).getOrElse(""))))
                                .onSuccess(p -> GUICommon.writeToObList(">> Rename folder to " + p.toString(), outText));
                    }

                    Platform.runLater(() ->
                    {
                        Systems.getDirectorySystem().reloadFile(this);

                        if (runWhenFinish != null) {
                            runWhenFinish.run();
                        }
                    });
                });
    }


    public Image getFileIcon() {
        return this.fileIcon.getOrNull();
    }

    public String getFileName() {
        return this.fileNameProperty.get();
    }

    public Boolean getNeedCheck() {
        return this.needCheck;
    }

    private void setNeedCheck(boolean needCheck) {
        this.needCheck = needCheck;
    }

    public String getFileExtension() {
        return this.fileExtensionProperty.get();
    }

    public Boolean hasMovie() {
        return this.movieFolder.map(v -> v.getMovieFilesPath().length() > 0).getOrElse(false);
    }

    public Boolean hasNfo() {
        return this.movieFolder.map(MovieFolder::hasNfo).getOrElse(false);
    }

    public boolean isGalleryFolder() {
        return this.galleryFolder.isDefined();
    }

    public Vector<FxThumb> getGalleryImages() {
        return this.childListManager.getFxThumbList();
    }

    public Option<Long> getMovieSize() {
        return this.size;
    }

    public MovieV2 getMovieData() {
        return this.movieFolder.flatMap(MovieFolder::getMovieData).getOrNull();
    }

    public Option<MovieV2> getMovieDataOption() {
        return this.movieFolder.flatMap(MovieFolder::getMovieData);
    }

    public void setMovieData(MovieV2 movieData) {
        this.movieFolder.peek(f -> f.setMovieData(movieData));
        //MovieCacheInstance.cache().put(this.getDirPath(), movieData);
    }

    public Option<MakerData> getMakerData() {
        return this.makerData;
    }

    public void setMakerData(Option<MakerData> makerData) {
        this.makerData = makerData;
    }

    public void setMakerData(MakerData makerData) {
        this.makerData = Option.of(makerData);
    }

    @Override
    public Boolean apply() {
        //return this.DoCheckFile(this.getNeedCheck());

        if (this.getNeedCheck()) {
            //Benchmark b = new Benchmark(false, "DirectorEntry - " + this.getValue().getFileName().toString());
            // system.out.println("CHECKING~~~~~~~~~~~~~~~~ ");

            if (this.isFile()) {
                this.extraCheckForFile();
            } else {
                //GUICommon.debugMessage(this.getValue().toString());
                this.extraCheckForDirectory();
            }
            //b.B("F ");
            this.setNeedCheck(false);
            Platform.runLater(() -> this.setNeedCheckProperty(false));

            // GUICommon.debugMessage(this.getValue().getFileName().toString() + " || " + this.movieData.map(MovieV2::getMovieTitle).map(Title::getTitle).getOrElse(""));
        }
        return true;
    }

    @Override
    public void run() {
        this.apply();
    }

    public Path getFilePath() {
        return this.getValue();
    }

    public boolean isDirectory() {
        return this.directory;
    }

    public boolean isFile() {
        return !this.directory;
    }

    public Path getDirPath() {
        return this.isDirectory() ? this.getValue() : this.getValue().getParent();
    }

    public Option<MovieFolder> getMovieFolder() {
        return this.movieFolder;
    }

    public BooleanProperty getNeedCheckProperty() {
        return this.needCheckProperty;
    }

    public void setNeedCheckProperty(boolean b) {
        if (b) {
            Platform.runLater(() -> this.needCheckProperty.set(true));
        } else {
            this.needCheckProperty.set(false);
        }
    }

    public StringProperty getFileNameProperty() {
        return this.fileNameProperty;
    }

    public StringProperty getFileExtensionProperty() {
        return this.fileExtensionProperty;
    }

    public int getLastSelectedIndex() {
        return this.lastSelectedIndex;
    }

    public void setLastSelectedIndex(int lastSelectedIndex) {
        this.lastSelectedIndex = lastSelectedIndex;
    }

    public FileTime getLastModified() {
        return this.lastModified;
    }

    public Option<SimpleMovieData> toSimpleMovie() {
        return this.getMovieDataOption().map(m -> new SimpleMovieData(m, this));
    }

    public Vector<SimpleMovieData> getChildSimpleMovieList() {
        return this.getSortedChildrenEntry().map(DirectoryEntry::toSimpleMovie)
                .filter(Option::isDefined)
                .map(Option::get);
    }

    public Option<FxThumb> toFxThumb() {
        return Option.of(this.getValue())
                .filter(path -> UtilCommon.checkFileExt.test(path, ".jpg") || UtilCommon.checkFileExt.test(path, ".png"))
                .map(FxThumb::of);
    }

    public Vector<FxThumb> getChildFxThumb() {
        var list = this.getSortedChildrenEntry().map(DirectoryEntry::toFxThumb)
                .filter(Option::isDefined)
                .map(Option::get);
        this.galleryFolder = GalleryFolder.create(this, list);
        return list;
    }

    public URI toURI() {
        return this.getValue().toUri();
    }

    public Option<FileObject> toFileObject() {
        return this.fileObject.orElse(LocalVFS.resolveFile(this.toURI()));
    }

    public Option<FileObject> getFileObject() {
        return this.fileObject;
    }
}

class ChildListManager {
    private final DirectoryEntry parent;
    private Vector<Path> pathList;
    private Vector<DirectoryEntry> entryList;
    private Vector<FxThumb> galleryList;
    private boolean bSorted;

    ChildListManager(DirectoryEntry parent) {
        this.reset();
        this.parent = parent;
    }

    void reset() {
        this.entryList = null;
        this.pathList = null;
        this.galleryList = null;
        this.bSorted = false;
    }

    Vector<DirectoryEntry> getEntryList() {
        if (this.entryList == null) {
            this.entryList = this.buildEntryList();
        }
        return this.entryList;
    }

    Vector<DirectoryEntry> getSortedEntryList() {
        this.getEntryList();
        if (!this.bSorted) {
            this.bSorted = true;
            this.entryList = this.entryList.sorted(ENTRY_COMPARATOR);
        }
        return this.entryList;
    }

    Vector<FxThumb> getFxThumbList() {
        if (this.galleryList == null) {
            this.getSortedEntryList();
            this.galleryList = this.entryList.map(DirectoryEntry::toFxThumb)
                    .filter(Option::isDefined)
                    .map(Option::get);
        }
        return this.galleryList;
    }

    void removeEntryFromList(DirectoryEntry entry) {
        this.entryList = this.getEntryList().remove(entry);
        this.galleryList = null;
    }

    void addEntryToList(DirectoryEntry entry) {
        this.entryList = this.getEntryList().append(entry);
        this.resortList();
    }

    void resortList() {
        this.bSorted = false;
        this.galleryList = null;
    }

    void replaceEntryFromList(DirectoryEntry oldEntry, DirectoryEntry newEntry) {
        this.addEntryToList(newEntry);
        this.removeEntryFromList(oldEntry);
    }

    int preCalEntryLength() {
        if (this.entryList == null) {
            return this.buildPathList().length();
        } else {
            return this.entryList.length();
        }
    }

    private Vector<Path> buildPathList() {
        if (this.pathList == null) {
            /*this.pathList = this.parent.toFileObject().flatMap(fo ->
                    Try.of(fo::getChildren).onFailure(org.nagoya.commons.GUICommon::errorDialog).toOption())
                    .map(Vector::of)
                    .getOrElse(Vector.empty());
            */
            this.pathList = Try.withResources(() -> Files.newDirectoryStream(this.parent.getValue()))
                    .of(Vector::ofAll)
                    .onFailure(Throwable::printStackTrace)
                    .getOrElse(Vector.empty());
        }
        return this.pathList;
    }

    private Vector<DirectoryEntry> buildEntryList() {
        Path path = this.parent.getValue();

        if (path != null && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            return this.buildPathList().map(p -> DirectoryEntry.of(p, this.parent));
        }
        return Vector.empty();
    }

    static final Comparator<Path> PATH_COMPARATOR = (p, q) -> {
        boolean pd = Files.isDirectory(p);
        boolean qd = Files.isDirectory(q);

        if (pd && !qd) {
            return -1;
        } else if (!pd && qd) {
            return 1;
        } else {
            return p.getFileName().toString().compareToIgnoreCase(q.getFileName().toString());
        }
    };

    static final Comparator<DirectoryEntry> ENTRY_COMPARATOR = (p, q) -> {
        boolean pd = p.isDirectory();
        boolean qd = q.isDirectory();

        if (pd && !qd) {
            return -1;
        } else if (!pd && qd) {
            return 1;
        } else {
            return p.getFileName().compareToIgnoreCase(q.getFileName());
        }
    };

}