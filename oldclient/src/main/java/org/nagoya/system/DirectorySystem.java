package org.nagoya.system;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import org.apache.commons.vfs2.FileObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.preferences.GuiSettings;
import org.nagoya.system.database.MovieScanner;
import org.nagoya.system.database.MovieDB;
import org.nagoya.system.event.CustomEventSourceImp;
import org.nagoya.system.event.CustomEventType;
import org.nagoya.view.dialog.FXProgressDialog;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DirectorySystem {
    public static final CustomEventType<UpdateData> EVENT_DIRECTORY_SYSTEM_UPDATE = new CustomEventType<>("EVENT_DIRECTORY_SYSTEM_UPDATE");

    private static final Comparator<Path> COMP_FOR_PATH = (file1, file2) -> {
        // Directory before non-directory
        if (Files.isDirectory(file1) && !Files.isDirectory(file2)) {
            return -1;
        }
        // Non-directory after directory
        else if (!Files.isDirectory(file1) && Files.isDirectory(file2)) {
            return 1;
        }
        // Alphabetic order otherwise
        else {
            return file1.compareTo(file2);
        }
    };

    private static final Comparator<DirectoryEntry> COMP_FOR_DIR = (d1, d2) -> {
        // Movie before others
        if (d1.hasNfo() && !d2.hasNfo()) {
            return -1;
        }
        // Non-movie after movie
        else if (!d1.hasNfo() && d2.hasNfo()) {
            return 1;
        }
        // Both Movie - Release Date order
        else if (d1.hasNfo() && d2.hasNfo()) {
            if (d1.getMovieData().getReleaseDate().isEmpty() && !d2.getMovieData().getReleaseDate().isEmpty()) {
                return -1;
            } else if (!d1.getMovieData().getReleaseDate().isEmpty() && d2.getMovieData().getReleaseDate().isEmpty()) {
                return 1;
            } else if (!d1.getMovieData().getReleaseDate().isEmpty() && !d2.getMovieData().getReleaseDate().isEmpty()) {
                return d1.getMovieData().getReleaseDate().compareTo(d2.getMovieData().getReleaseDate());
            }
        }

        // Both not Movie or no release date
        return COMP_FOR_PATH.compare(d1.getValue(), d2.getValue());

    };

    private DirectoryEntry currentRootEntry;

    private final MovieScanner movieScanner;

    //private LiveDirs<String, Path> liveDirs;


    private HashMap<String, ChangeListener<UpdateData>> subscriberList;

    private final ObjectProperty<Tuple2<DirectoryEntry, Option<Runnable>>> currentDirectoryEntryProperty;
    private final ObjectProperty<UpdateData> updateDataObjectProperty;
    private final Disposable updatePathProcessor;

    public DirectorySystem() {
        this.movieScanner = MovieScanner.getInstance();

        this.currentRootEntry = this.movieScanner.initBaseEntry();
        this.subscriberList = HashMap.empty();

        /*this.publishProcessor = BehaviorProcessor.create();
        this.changeProcessor = PublishProcessor.create();

        this.publishProcessor.onNext(Tuple.of(this.currentRootEntry, Option.none()));

        var task = this.publishProcessor.subscribe(tuple2 -> {
            Flowable.fromIterable(tuple2._1.getChildrenEntry())
                    .parallel()
                    .runOn(Schedulers.computation())
                    .map(DirectoryEntry::apply)
                    .sequential()
                    .subscribe();
        });

        this.directoryChangeObservable = this.changeProcessor
                .replay(1)
                .refCount()
                .observeOn(JavaFxScheduler.platform())
        ;

        this.directoryObservable = this.publishProcessor
                .replay(1)
                .refCount()
                .observeOn(Schedulers.io())
                .map(d -> new UpdateData(UpdateType.FULL,
                        d._1.getChildrenEntry().toJavaList(),
                        null, null,
                        d._1.getLastSelectedIndex(),
                        d._2))
                .observeOn(JavaFxScheduler.platform())
        ;
*/
        this.currentDirectoryEntryProperty = new SimpleObjectProperty<>();
        this.updateDataObjectProperty = new SimpleObjectProperty<>() {
            @Override
            protected void invalidated() {
                CustomEventSourceImp.fire(EVENT_DIRECTORY_SYSTEM_UPDATE, DirectorySystem.this.updateDataObjectProperty.get());
            }
        };

        this.updatePathProcessor = Flux.<Tuple2<DirectoryEntry, Option<Runnable>>>create(emitter -> {
            ChangeListener<Tuple2<DirectoryEntry, Option<Runnable>>> changeListener = (ov, o, n) -> emitter.next(n);
            this.currentDirectoryEntryProperty.addListener(changeListener);
            emitter.onDispose(() -> this.currentDirectoryEntryProperty.removeListener(changeListener));
        }, FluxSink.OverflowStrategy.LATEST)
                .doOnNext(t -> Flux.fromIterable(t._1.getChildrenEntry())
                        .parallel()
                        .runOn(ExecuteSystem.get().getFileSystemScheduler())
                        .map(DirectoryEntry::apply)
                        .subscribe())
                .map(t -> new UpdateData(UpdateType.FULL,
                        t._1.getSortedChildrenEntry().toJavaList(), null, null,
                        t._1.getLastSelectedIndex(), t._2))
                .subscribeOn(ExecuteSystem.get().getIoScheduler())
                .subscribe(this.updateDataObjectProperty::setValue);
    }

    public DirectoryEntry getMovieRootEntry() {
        return this.movieScanner.getMovieRootEntry();
    }

    public void shutdown() {
        this.updatePathProcessor.dispose();
        MovieDB.getInstance().saveCacheFile();
        //this.publishProcessor.onComplete();
    }

    public static Vector<Path> readPath(Path path) {

        return Try.withResources(() -> Files.newDirectoryStream(path))
                .of(Vector::ofAll)
                .onFailure(Throwable::printStackTrace)
                .getOrElse(Vector.empty());
    }

    public static List<DirectoryEntry> loadPath(@NotNull Path path) {
        return readPath(path)/*.sorted(COMP_FOR_PATH)*/.map(DirectoryEntry::of).asJava();
    }

    private void initLiveDir() {
        Path path = GuiSettings.getInstance().getDirectory(GuiSettings.Key.avDirectory);

      /*  String EXTERNAL = "EXTERNAL";
        this.liveDirs = LiveDirs.getInstance(EXTERNAL);

        this.liveDirs.addTopLevelDirectory(path);

        // handle external changes
        this.liveDirs.model().modifications().subscribe(m -> {
            if (m.getInitiator().equals(EXTERNAL)) {
                // handle external modification, e.g. reload the modified file
                //           reload(m.getPath());
            } else {
                // modification done by this application, no extra action needed
            }
        });
*/
        //this.rootTree = this.liveDirs.getRootElements().get(0);

        // use LiveDirs as a TreeView model
        //    TreeView<Path> treeView = new TreeView<>(this.liveDirs.model().getRoot());
        //   treeView.setShowRoot(false);
        //   treeView.setCellFactory((TreeView<Path> l) -> new PathListTreeCell());
    }

    public void subscribe(String name, @NotNull Consumer<UpdateData> consumer) {
        ChangeListener<UpdateData> changeListener = (ov, o, n) -> GUICommon.runOnFx(() -> consumer.accept(n));
        this.updateDataObjectProperty.addListener(changeListener);
        this.subscriberList = this.subscriberList.put(name, changeListener);
    }

    public void unSubscribe(String name) {
        this.subscriberList.get(name).peek(this.updateDataObjectProperty::removeListener);
        this.subscriberList = this.subscriberList.remove(name);
    }

    public void sortContain() {
        this.currentRootEntry.getChildrenEntry().sortBy(COMP_FOR_DIR, i -> i);
    }

    public void changePathTo(Path newPath, Option<Runnable> run) {
        if (Files.exists(newPath) && Files.isDirectory(newPath)) {
            this.currentRootEntry.releaseChildMemory();
            this.currentRootEntry = this.getDirectoryEntryFromPath(newPath).getOrElse(DirectoryEntry.of(newPath));
            this.pushUpdate(run);
        } else {
            GUICommon.errorDialog("Directory System > Change Path Error : Invalid Path");
        }
    }

    public void changePathTo(DirectoryEntry directoryEntry, Option<Runnable> run) {
        this.currentRootEntry.releaseChildMemory();
        this.currentRootEntry = directoryEntry;
        this.pushUpdate(run);
    }

    public void upParent(int index, Option<Runnable> run) {
        this.currentRootEntry.setLastSelectedIndex(index);

        if (this.currentRootEntry.getEntryParent() != null) {
            this.changePathTo(this.currentRootEntry.getEntryParent(), run);
        } else {

            var parent = this.getBaseFileObject().flatMap(fileObject -> this.currentRootEntry.toFileObject()
                    .flatMap(fo -> Try.of(fo::getParent).toOption().filter(Objects::nonNull)))
                    .map(DirectoryEntry::of);

            parent.peek(d -> this.changePathTo(d, run));
            /*if (this.getCurrentPath().getParent() != null) {
                this.changePathTo(this.getCurrentPath().getParent(), run);
            } else if (this.getCurrentPath() != this.getCurrentPath().getRoot() && this.getCurrentPath().getRoot() != null) {
                this.changePathTo(this.getCurrentPath().getRoot(), run);
            }*/
        }
    }

    public void enterChild(DirectoryEntry directoryEntry, Option<Runnable> run) {
        int index = this.currentRootEntry.getChildrenEntry().indexOf(directoryEntry);
        if (index != -1) {
            this.enterChild(index, directoryEntry, run);
        }
    }

    public void enterChild(int index, Option<Runnable> run) {
        DirectoryEntry directoryEntry = this.currentRootEntry.getChildrenEntry().get(index);
        this.enterChild(index, directoryEntry, run);
    }

    public void enterChild(int index, @NotNull DirectoryEntry directoryEntry, Option<Runnable> run) {
        this.currentRootEntry.setLastSelectedIndex(index);
        this.currentRootEntry.releaseChildMemory();

        if (directoryEntry.getValue().equals(this.movieScanner.getMovieRootPath())) {
            directoryEntry = this.movieScanner.getMovieRootEntry();
        }

        this.changePathTo(directoryEntry, run);
    }

    //---------------------------------------------------- File IO ----------------------------------------------------


    public Mono<DirectorySystem> chainRenameFile(@NotNull DirectoryEntry directoryEntry, Path newPath) {
        Path path = directoryEntry.getValue();
        directoryEntry.clearCache();
        return Mono.fromCallable(() -> UtilCommon.tryMoveFile(path, newPath))
                .handle((t, sink) -> t.onSuccess(p -> sink.next(this)).onFailure(GUICommon::errorDialog).onFailure(sink::error))
                ;
    }

    public Mono<DirectorySystem> chainCreateDirectory(Path newDirectory) {
        return Mono.fromCallable(() -> UtilCommon.tryCreateDirectory(newDirectory))
                .handle((t, sink) -> t.onSuccess(p -> sink.next(this)).onFailure(GUICommon::errorDialog).onFailure(sink::error))
                ;
    }

    public Mono<DirectorySystem> chainCopyDirectory(@NotNull DirectoryEntry fromEntry, Path toPath) {
        return Mono.fromCallable(() -> CopyDir.run(fromEntry.getValue(), toPath))
                .handle((t, sink) -> t.onSuccess(p -> sink.next(this)).onFailure(GUICommon::errorDialog).onFailure(sink::error))
                ;
    }

    public Mono<DirectorySystem> chainDeleteDirectoryAndContents(@NotNull DirectoryEntry directoryEntry) {
        Path path = directoryEntry.getValue();
        directoryEntry.clearCache();
        return Mono.fromCallable(() -> UtilCommon.tryDeleteDirectoryAndContents(path))
                .handle((t, sink) -> t.onSuccess(p -> sink.next(this)).onFailure(GUICommon::errorDialog).onFailure(sink::error))
                ;
    }

    public Mono<DirectorySystem> chainDeleteFile(@NotNull DirectoryEntry directoryEntry, Option<Runnable> whenNotEmpty) {
        return Mono.fromCallable(() -> UtilCommon.tryDeleteFile(directoryEntry.getValue()))
                .handle((t, sink) -> t.onSuccess(p -> sink.next(this)).onFailure(e -> {
                    if (e instanceof DirectoryNotEmptyException) {
                        whenNotEmpty.peek(Runnable::run);
                    } else {
                        GUICommon.errorDialog(e);
                    }
                }).onFailure(sink::error));
    }

    public DirectorySystem chainReloadDirectory() {
        this.currentRootEntry.resetChild();
        return this;
    }

    public DirectorySystem chainLoadNewEntry(@NotNull Path path) {
        var newEntryParent = this.getDirectoryEntryFromPath(path.getParent());
        newEntryParent
                .peek(DirectoryEntry::resetChild)
                .peek(DirectoryEntry::getChildrenEntry);
        return this;
    }

    public DirectorySystem chainLoadNewEntry(DirectoryEntry parent, Path path) {
        var newEntry = DirectoryEntry.of(path, parent);
        parent.addChild(newEntry);
        return this;
    }

    public DirectorySystem chainReplaceEntry(@NotNull DirectoryEntry oldEntry, Path newPath) {
        DirectoryEntry parent = oldEntry.getEntryParent();
        if (parent != null && newPath.startsWith(parent.getValue())) {
            DirectoryEntry newEntry = DirectoryEntry.of(newPath, parent);
            parent.replaceChild(oldEntry, newEntry);
        } else {
            this.chainRemoveEntry(oldEntry).chainLoadNewEntry(newPath);
        }
        return this;
    }

    public DirectorySystem chainRemoveEntry(@NotNull DirectoryEntry directoryEntry) {
        directoryEntry.resetChild();
        if (directoryEntry.getEntryParent() != null) {
            directoryEntry.getEntryParent().removeChild(directoryEntry);
        }
        return this;
    }

    public DirectorySystem chainPushFull(Option<Runnable> run) {
        this.pushUpdate(run);
        return this;
    }

    public DirectorySystem chainPushUpdate(DirectoryEntry oldEntry, DirectoryEntry newEntry) {
        if (this.parentIsCurrentPath(oldEntry)) {
            this.sendModify(oldEntry, newEntry);
        }
        return this;
    }

    public DirectorySystem chainPushDelete(DirectoryEntry directoryEntry) {
        if (this.parentIsCurrentPath(directoryEntry)) {
            this.sendDelete(directoryEntry);
        }
        return this;
    }

    //---------------------------------------------------- File IO End ----------------------------------------------------


    public boolean parentIsCurrentPath(@NotNull DirectoryEntry directoryEntry) {
        return this.getCurrentPath().equals(directoryEntry.getValue().getParent());
    }

    public boolean parentIsCurrentPath(@NotNull Path path) {
        return this.getCurrentPath().equals(path.getParent());
    }

    public Mono<DirectorySystem> createDirectory(Path path, Option<Runnable> run) {
        return this.chainCreateDirectory(path)
                .doOnNext(ds -> ds.processAddNewEntry(path, run));
    }

    public Mono<DirectorySystem> copyDirectory(DirectoryEntry fromEntry, Path toPath, Option<Runnable> run) {
        return this.chainCopyDirectory(fromEntry, toPath)
                .doOnNext(ds -> ds.processAddNewEntry(toPath, run));
    }

    public Mono<DirectorySystem> deleteDirectoryAndContents(int index) {
        return this.getDirectoryEntryFromIndex(index)
                .map(this::deleteDirectoryAndContents).getOrElse(Mono.empty());
    }

    public Mono<DirectorySystem> deleteDirectoryAndContents(DirectoryEntry directoryEntry) {
        return this.chainDeleteDirectoryAndContents(directoryEntry)
                .doOnNext(ds -> ds.processRemoveEntry(directoryEntry));
    }

    public Mono<DirectorySystem> deleteFile(int index, Option<Runnable> whenNotEmpty) {
        return this.getDirectoryEntryFromIndex(index)
                .map(d -> this.deleteFile(d, whenNotEmpty)).getOrElse(Mono.empty());
    }

    public Mono<DirectorySystem> deleteFile(@NotNull DirectoryEntry entryToDelete, Option<Runnable> whenNotEmpty) {
        return this.chainDeleteFile(entryToDelete, whenNotEmpty)
                .doOnNext(ds -> ds.processRemoveEntry(entryToDelete));
    }

    public void reloadDirectory(Option<Runnable> run) {
        this.chainReloadDirectory().chainPushFull(run);
    }

    public void reloadFile(@NotNull DirectoryEntry entry) {
        entry.clearCache();
        entry.getEntryParent().resortChild();
        if (entry.getEntryParent().getValue().equals(this.getCurrentPath())) {
            this.chainPushFull(Option.none());
        }
        //this.sendModify(entry, entry);
        //Systems.useExecutors(ExecuteSystem.role.IO, entry);
    }

    private void processRemoveEntry(@NotNull DirectoryEntry entryToRemove) {
        this.chainRemoveEntry(entryToRemove);
        if (this.parentIsCurrentPath(entryToRemove)) {
            this.chainPushDelete(entryToRemove);
        }
    }

    private void processAddNewEntry(@NotNull Path path, Option<Runnable> run) {
        if (this.parentIsCurrentPath(path)) {
            this.chainLoadNewEntry(this.getCurrentEntry(), path)
                    .chainPushFull(run);
            //this.reloadDirectory(run);
        } else {
            this.chainLoadNewEntry(path);
        }
    }

    public int getIndexOfDirectoryEntry(DirectoryEntry directoryEntry) {
        return (directoryEntry == null) ? -1 :
                this.getIndexOfDirectoryEntry(directoryEntry.getValue());
    }

    public int getIndexOfDirectoryEntry(Path path) {
        return this.currentRootEntry.getChildrenEntry().indexWhere(d -> d.getValue().equals(path));
    }

    public Option<DirectoryEntry> getDirectoryEntryFromIndex(int index) {
        return Try.of(() -> this.currentRootEntry.getChildrenEntry().get(index))
                .toOption();
    }

    public Option<DirectoryEntry> getDirectoryEntryFromPath(Path targetPath) {
        if (targetPath != null && targetPath.startsWith(this.movieScanner.getMovieRootPath())) {
            Path rel = this.movieScanner.getMovieRootPath().relativize(targetPath);

            Option<DirectoryEntry> directoryEntry = Option.of(this.movieScanner.getMovieRootEntry());
            for (int x = 0; x < rel.getNameCount(); x++) {
                Path check = rel.getName(x);
                Vector<DirectoryEntry> v = directoryEntry.map(DirectoryEntry::getChildrenEntry).getOrElse(Vector.empty());
                directoryEntry = v.find(d -> d.getValue().getFileName().equals(check));
            }
            //GUICommon.debugMessage("getDirectoryEntryFromPath " + directoryEntry.toString());

            return directoryEntry;
        }
        return Option.none();
    }

    public Vector<DirectoryEntry> getDirectoryEntries() {
        return this.currentRootEntry.getChildrenEntry();
    }

    public DirectoryEntry getCurrentEntry() {
        return this.currentRootEntry;
    }

    public Path getCurrentPath() {
        return this.currentRootEntry.getValue();
    }

    public Option<FileObject> getBaseFileObject() {
        return this.currentRootEntry.toFileObject();
    }

    private void pushUpdate(Option<Runnable> runnable) {
        GuiSettings.getInstance().setDirectory(GuiSettings.Key.lastUsedDirectory, this.currentRootEntry.getValue());
        this.currentDirectoryEntryProperty.setValue(Tuple.of(this.currentRootEntry, runnable));
        //this.publishProcessor.onNext(Tuple.of(this.currentRootEntry, runnable));
    }

    private void sendModify(DirectoryEntry oldEntry, DirectoryEntry newEntry) {
        UpdateData data = new UpdateData(UpdateType.MODIFICATION, null, oldEntry, newEntry, -1, null);
        this.updateDataObjectProperty.setValue(data);
    }

    private void sendDelete(DirectoryEntry delEntry) {
        GUICommon.debugMessage("sendDelete");
        UpdateData data = new UpdateData(UpdateType.DELETION, null, delEntry, null, -1, null);
        this.updateDataObjectProperty.setValue(data);
    }

    public enum UpdateType {
        FULL,
        DELETION,
        MODIFICATION,
    }

    public static class UpdateData {
        private final UpdateType type;
        private final List<DirectoryEntry> list;
        private final DirectoryEntry oldD;
        private final DirectoryEntry newD;
        private final int pos;
        private final Option<Runnable> runAfter;

        public UpdateType getType() {
            return this.type;
        }

        public List<DirectoryEntry> getList() {
            return this.list;
        }

        public DirectoryEntry getOldD() {
            return this.oldD;
        }

        public DirectoryEntry getNewD() {
            return this.newD;
        }

        public int getPos() {
            return this.pos;
        }

        public Option<Runnable> getRunAfter() {
            return this.runAfter;
        }

        @Contract(pure = true)
        UpdateData(UpdateType type, List<DirectoryEntry> list, DirectoryEntry oldD, DirectoryEntry newD, int pos, Option<Runnable> runAfter) {
            this.type = type;
            this.list = list;
            this.oldD = oldD;
            this.newD = newD;
            this.pos = pos;
            this.runAfter = runAfter;
        }
    }
}

class CopyDir extends SimpleFileVisitor<Path> {
    private static FXProgressDialog progressDialog;

    public static Try<Path> run(Path sourceDir, Path targetDir) {
        progressDialog = FXProgressDialog.getInstance();
        progressDialog.startProgressDialog();
        return Try.of(() -> Files.walkFileTree(sourceDir, new CopyDir(sourceDir, targetDir)));
    }

    private final Path sourceDir;
    private final Path targetDir;

    CopyDir(Path sourceDir, Path targetDir) {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
    }

    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attributes) {

        Path targetFile = this.targetDir.resolve(this.sourceDir.relativize(file));
        //Files.copy(file, targetFile);
        UtilCommon.tryCopyFile(file, targetFile, Option.of(progressDialog));

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir,
                                             BasicFileAttributes attributes) {
        try {
            Path newDir = this.targetDir.resolve(this.sourceDir.relativize(dir));
            Files.createDirectory(newDir);
        } catch (IOException ex) {
            GUICommon.debugMessage(() -> ">> Error >> Copy " + ex.toString());
        }

        return FileVisitResult.CONTINUE;
    }

    /*private static Option<ObservableList<String>> createProgressDialog() {
        JFXListView<String> textArea = GUICommon.getTextArea(700, 160, true);
        //GUICommon.showDialog("[Progress]", textArea);
        DialogBuilder.create()
                .heading("[Progress]")
                .body(textArea)
                .buttonClose()
                .build()
                .show();
        return Option.of(textArea.getItems());
    }*/
}

/*class JavaFxScheduler extends Scheduler {
    private static final JavaFxScheduler INSTANCE = new JavaFxScheduler();

    JavaFxScheduler() {
    }

    @Contract(pure = true)
    public static Scheduler platform() {
        return INSTANCE;
    }

    private static void assertThatTheDelayIsValidForTheJavaFxTimer(long delay) {
        if (delay < 0L || delay > 2147483647L) {
            throw new IllegalArgumentException(String.format("The JavaFx timer only accepts non-negative delays up to %d milliseconds.", 2147483647));
        }
    }

    @Override
    public Scheduler.Worker createWorker() {
        return new JavaFxScheduler.JavaFxWorker();
    }

    private static class JavaFxWorker extends Scheduler.Worker implements Runnable {
        private volatile QueuedRunnable head;
        private final AtomicReference<QueuedRunnable> tail;

        private JavaFxWorker() {
            this.head = new QueuedRunnable(null);
            this.tail = new AtomicReference<>(this.head);
        }

        @Override
        public void dispose() {
            this.tail.set(null);

            for (QueuedRunnable qr = this.head; qr != null; qr = qr.getAndSet(null)) {
                qr.dispose();
            }

        }

        @Override
        public boolean isDisposed() {
            return this.tail.get() == null;
        }

        @Override
        public Disposable schedule(Runnable action, long delayTime, @NotNull TimeUnit unit) {
            long delay = Math.max(0L, unit.toMillis(delayTime));
            JavaFxScheduler.assertThatTheDelayIsValidForTheJavaFxTimer(delay);
            QueuedRunnable queuedRunnable = new QueuedRunnable(action);
            if (delay == 0L) {
                return this.schedule(queuedRunnable);
            } else {
                Timeline timer = new Timeline(new KeyFrame(Duration.millis((double) delay), (event) -> this.schedule(queuedRunnable)));
                timer.play();
                return Disposables.fromRunnable(() -> {
                    queuedRunnable.dispose();
                    timer.stop();
                });
            }
        }

        @Override
        public Disposable schedule(Runnable action) {
            if (this.isDisposed()) {
                return Disposables.disposed();
            } else {
                QueuedRunnable queuedRunnable = action instanceof QueuedRunnable ? (QueuedRunnable) action : new QueuedRunnable(action);

                QueuedRunnable tailPivot;
                do {
                    tailPivot = this.tail.get();
                } while (tailPivot != null && !tailPivot.compareAndSet(null, queuedRunnable));

                if (tailPivot == null) {
                    queuedRunnable.dispose();
                } else {
                    this.tail.compareAndSet(tailPivot, queuedRunnable);
                    if (tailPivot == this.head) {
                        if (Platform.isFxApplicationThread()) {
                            this.run();
                        } else {
                            Platform.runLater(this);
                        }
                    }
                }

                return queuedRunnable;
            }
        }

        @Override
        public void run() {
            for (QueuedRunnable qr = this.head.get(); qr != null; qr = qr.get()) {
                qr.run();
                this.head = qr;
            }

        }

        private static class QueuedRunnable extends AtomicReference<QueuedRunnable> implements Disposable, Runnable {
            private volatile Runnable action;

            private QueuedRunnable(Runnable action) {
                this.action = action;
            }

            @Override
            public void dispose() {
                this.action = null;
            }

            @Override
            public boolean isDisposed() {
                return this.action == null;
            }

            @Override
            public void run() {
                Runnable action = this.action;
                if (action != null) {
                    action.run();
                }

                this.action = null;
            }
        }
    }
}*/

