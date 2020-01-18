package org.nagoya.system;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.fxmisc.livedirs.InitiatorTrackingIOFacility;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * LiveDirs combines a directory watcher, a directory-tree model and a simple
 * I/O facility. The added value of this combination is:
 * <ol>
 * <li>the directory-tree model is updated automatically to reflect the
 * current state of the file-system;</li>
 * <li>the application can distinguish file-system changes made via the
 * I/O facility from external changes.</li>
 * </ol>
 *
 * <p>The directory model can be used directly as a model for {@link TreeView}.
 *
 * @param <I> type of the initiator of I/O actions.
 * @param <T> type for {@link TreeItem#getValue()}
 */
public class LiveDirs<I, T> {

    /**
     * Creates a LiveDirs instance to be used from the JavaFX application
     * thread.
     *
     * @param externalInitiator object to represent an initiator of an external
     *                          file-system change.
     * @throws IOException
     */
    @NotNull
    public static <I> LiveDirs<I, Path> getInstance(I externalInitiator) {
        return getInstance(externalInitiator, Platform::runLater);
    }

    /**
     * Creates a LiveDirs instance to be used from a designated thread.
     *
     * @param externalInitiator    object to represent an initiator of an external
     *                             file-system change.
     * @param clientThreadExecutor executor to execute actions on the caller
     *                             thread. Used to publish updates and errors on the caller thread.
     * @throws IOException
     */
    @NotNull
    @Contract("_, _ -> new")
    public static <I> LiveDirs<I, Path> getInstance(I externalInitiator, Executor clientThreadExecutor) {
        return new LiveDirs<>(externalInitiator, Function.identity(), Function.identity(), clientThreadExecutor);
    }

    private final EventSource<Throwable> localErrors = new EventSource<>();
    private final EventStream<Throwable> errors;
    private final Executor clientThreadExecutor;
    private final DirWatcher dirWatcher;
    private final LiveDirsModel<I, T> model;
    private final LiveDirsIO<I> io;
    private final I externalInitiator;

    /**
     * Creates a LiveDirs instance to be used from a designated thread.
     *
     * @param projector            converts the ({@link T}) {@link TreeItem#getValue()} into a {@link Path} object
     * @param injector             converts a given {@link Path} object into {@link T}. The reverse of {@code projector}
     * @param externalInitiator    object to represent an initiator of an external
     *                             file-system change.
     * @param clientThreadExecutor executor to execute actions on the caller
     *                             thread. Used to publish updates and errors on the caller thread.
     */
    public LiveDirs(I externalInitiator, Function<T, Path> projector, Function<Path, T> injector, Executor clientThreadExecutor) {
        this.externalInitiator = externalInitiator;
        this.clientThreadExecutor = clientThreadExecutor;
        this.dirWatcher = new DirWatcher();
        this.model = new LiveDirsModel<>(externalInitiator, projector, injector);
        this.io = new LiveDirsIO<>(this.dirWatcher, this.model, clientThreadExecutor);

        this.dirWatcher.signalledKeys().subscribe(this::processKey);
        this.errors = EventStreams.merge(this.dirWatcher.errors(), this.model.errors(), this.localErrors);
    }

    /**
     * Stream of asynchronously encountered errors.
     */
    public EventStream<Throwable> errors() {
        return this.errors;
    }

    /**
     * Observable directory model.
     */
    public DirectoryModel<I, T> model() {
        return this.model;
    }

    public ObservableList<TreeItem<T>> getRootElements() {
        return this.model.getRoot().getChildren();
    }

    /**
     * Asynchronous I/O facility. All I/O operations performed by this facility
     * are performed on a single thread. It is the same thread that is used to
     * watch the file-system for changes.
     */
    public InitiatorTrackingIOFacility<I> io() {
        return this.io;
    }

    /**
     * Adds a directory to watch. The directory will be added to the directory
     * model and watched for changes.
     */
    public void addTopLevelDirectory(@NotNull Path dir) {
        if (!dir.isAbsolute()) {
            throw new IllegalArgumentException(dir + " is not absolute. Only absolute paths may be added as top-level directories.");
        }

        try {
            this.dirWatcher.watch(dir);
            this.model.addTopLevelDirectory(dir);
            this.refresh(dir);
        } catch (IOException e) {
            this.localErrors.push(e);
        }
    }

    /**
     * Used to refresh the given subtree of the directory model in case
     * automatic synchronization failed for any reason.
     *
     * <p>Guarantees given by {@link WatchService} are weak and the behavior
     * may vary on different operating systems. It is possible that the
     * automatic synchronization is not 100% reliable. This method provides a
     * way to request synchronization in case any inconsistencies are observed.
     */
    public CompletionStage<Void> refresh(Path path) {
        return this.wrap(this.dirWatcher.getTree(path))
                .thenAcceptAsync(tree -> {
                    this.model.sync(tree);
                    this.watchTree(tree);
                }, this.clientThreadExecutor);
    }

    /**
     * Releases resources used by this LiveDirs instance. In particular, stops
     * the I/O thread (used for I/O operations as well as directory watching).
     */
    public void dispose() {
        this.dirWatcher.shutdown();
    }

    private void processKey(@NotNull WatchKey key) {
        Path dir = (Path) key.watchable();
        if (!this.model.containsPrefixOf(dir)) {
            key.cancel();
        } else {
            List<WatchEvent<?>> events = key.pollEvents();
            if (events.stream().anyMatch(evt -> evt.kind() == OVERFLOW)) {
                this.refreshOrLogError(dir);
            } else {
                for (WatchEvent<?> evt : key.pollEvents()) {
                    WatchEvent<Path> event = (WatchEvent<Path>) evt;
                    this.processEvent(dir, event);
                }
            }

            if (!key.reset()) {
                this.model.delete(dir, this.externalInitiator);
            }
        }
    }

    private void processEvent(@NotNull Path dir, @NotNull WatchEvent<Path> event) {
        // Context for directory entry event is the file name of entry
        Path relChild = event.context();
        Path child = dir.resolve(relChild);

        WatchEvent.Kind<Path> kind = event.kind();

        if (kind == ENTRY_MODIFY) {
            this.handleModification(child, this.externalInitiator);
        } else if (kind == ENTRY_CREATE) {
            this.handleCreation(child, this.externalInitiator);
        } else if (kind == ENTRY_DELETE) {
            this.model.delete(child, this.externalInitiator);
        } else {
            throw new AssertionError("unreachable code");
        }
    }

    private void handleCreation(Path path, I initiator) {
        if (Files.isDirectory(path)) {
            this.handleDirCreation(path, initiator);
        } else {
            this.handleFileCreation(path, initiator);
        }
    }

    private void handleFileCreation(Path path, I initiator) {
        try {
            FileTime timestamp = Files.getLastModifiedTime(path);
            this.model.addFile(path, initiator, timestamp);
        } catch (IOException e) {
            this.localErrors.push(e);
        }
    }

    private void handleDirCreation(Path path, I initiator) {
        if (this.model.containsPrefixOf(path)) {
            this.model.addDirectory(path, initiator);
            this.dirWatcher.watchOrLogError(path);
        }
        this.refreshOrLogError(path);
    }

    private void handleModification(Path path, I initiator) {
        try {
            FileTime timestamp = Files.getLastModifiedTime(path);
            this.model.updateModificationTime(path, timestamp, initiator);
        } catch (IOException e) {
            this.localErrors.push(e);
        }
    }

    private void watchTree(@NotNull PathNode tree) {
        if (tree.isDirectory()) {
            this.dirWatcher.watchOrLogError(tree.getPath());
            for (PathNode child : tree.getChildren()) {
                this.watchTree(child);
            }
        }
    }

    private void refreshOrLogError(Path path) {
        this.refresh(path).whenComplete((nothing, ex) -> {
            if (ex != null) {
                this.localErrors.push(ex);
            }
        });
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    private <U> CompletionStage<U> wrap(CompletionStage<U> stage) {
        return new CompletionStageWithDefaultExecutor<>(stage, this.clientThreadExecutor);
    }
}
