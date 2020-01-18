package org.nagoya.system;

import io.vavr.control.Option;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.reactfx.EventStream;

import java.nio.file.Path;
import java.util.function.BiFunction;

/**
 * Observable model of multiple directory trees.
 *
 * @param <I> type of initiator of changes to the model.
 * @param <T> type for {@link TreeItem#getValue()}
 */
public interface DirectoryModel<I, T> {

    /**
     * Factory to create graphics for {@link TreeItem}s in a
     * {@link DirectoryModel}.
     */
    @FunctionalInterface
    interface GraphicFactory extends BiFunction<Path, Boolean, Node> {
        Node createGraphic(Path path, boolean isDirectory);

        @Override
        default Node apply(Path path, Boolean isDirectory) {
            return this.createGraphic(path, isDirectory);
        }
    }

    /**
     * Types of updates to the director model.
     */
    enum UpdateType {
        /**
         * Indicates a new directory entry.
         */
        CREATION,

        /**
         * Indicates removal of a directory entry.
         */
        DELETION,

        /**
         * Indicates file modification.
         */
        MODIFICATION,
    }

    /**
     * Represents an update to the directory model.
     *
     * @param <I> type of initiator of changes to the model.
     */
    class Update<I> {
        @NotNull
        @Contract(value = "_, _, _ -> new", pure = true)
        static <I> Update<I> creation(Path baseDir, Path relPath, I initiator) {
            return new Update<>(baseDir, relPath, initiator, UpdateType.CREATION);
        }

        @NotNull
        @Contract(value = "_, _, _ -> new", pure = true)
        static <I> Update<I> deletion(Path baseDir, Path relPath, I initiator) {
            return new Update<>(baseDir, relPath, initiator, UpdateType.DELETION);
        }

        @NotNull
        @Contract(value = "_, _, _ -> new", pure = true)
        static <I> Update<I> modification(Path baseDir, Path relPath, I initiator) {
            return new Update<>(baseDir, relPath, initiator, UpdateType.MODIFICATION);
        }

        private final Path baseDir;
        private final Path relativePath;
        private final I initiator;
        private final UpdateType type;

        @Contract(pure = true)
        private Update(Path baseDir, Path relPath, I initiator, UpdateType type) {
            this.baseDir = baseDir;
            this.relativePath = relPath;
            this.initiator = initiator;
            this.type = type;
        }

        public I getInitiator() {
            return this.initiator;
        }

        public Path getBaseDir() {
            return this.baseDir;
        }

        public Path getRelativePath() {
            return this.relativePath;
        }

        public Path getPath() {
            return this.baseDir.resolve(this.relativePath);
        }

        public UpdateType getType() {
            return this.type;
        }
    }

    /**
     * Graphic factory that always returns {@code null}.
     */
    final GraphicFactory NO_GRAPHIC_FACTORY = (path, isDir) -> null;

    /**
     * Graphic factory that returns a folder icon for a directory and
     * a document icon for a regular file.
     */
    final GraphicFactory DEFAULT_GRAPHIC_FACTORY = new DefaultGraphicFactory();


    /**
     * Returns a tree item that can be used as a root of a {@link TreeView}.
     * The returned TreeItem does not contain any Path (its
     * {@link TreeItem#getValue()} method returns {@code null}), but its
     * children are roots of directory trees represented in this model.
     * As a consequence, the returned TreeItem shall be used with
     * {@link TreeView#showRootProperty()} set to {@code false}.
     */
    TreeItem<T> getRoot();

    /**
     * Indicates whether this directory model contains the given path.
     */
    boolean contains(Path path);

    Option<TreeItem<T>> getItem(Path relatedPath);

    /**
     * Returns an observable stream of additions to the model.
     */
    EventStream<Update<I>> creations();

    /**
     * Returns an observable stream of removals from the model.
     */
    EventStream<Update<I>> deletions();

    /**
     * Returns an observable stream of file modifications in the model.
     */
    EventStream<Update<I>> modifications();

    /**
     * Sets graphic factory used to create graphics of {@link TreeItem}s
     * in this directory model.
     */
    void setGraphicFactory(GraphicFactory factory);
}


class DefaultGraphicFactory implements DirectoryModel.GraphicFactory {
    private static final Image FOLDER_IMAGE = new Image(DefaultGraphicFactory.class.getResource("folder-16.png").toString());
    private static final Image FILE_IMAGE = new Image(DefaultGraphicFactory.class.getResource("file-16.png").toString());

    @Override
    public Node createGraphic(Path path, boolean isDirectory) {
        return isDirectory ? new ImageView(FOLDER_IMAGE) : new ImageView(FILE_IMAGE);
    }
}