package org.nagoya.system;

import io.vavr.control.Option;
import javafx.scene.control.TreeItem;
import org.reactfx.EventSource;
import org.reactfx.EventStream;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

class LiveDirsModel<I, T> implements DirectoryModel<I, T> {

    private final TreeItem<T> root = new TreeItem<>();
    private final EventSource<Update<I>> creations = new EventSource<>();
    private final EventSource<Update<I>> deletions = new EventSource<>();
    private final EventSource<Update<I>> modifications = new EventSource<>();
    private final EventSource<Throwable> errors = new EventSource<>();
    private final Reporter<I> reporter;
    private final I defaultInitiator;
    private final Function<T, Path> projector;
    private final Function<Path, T> injector;

    private GraphicFactory graphicFactory = DEFAULT_GRAPHIC_FACTORY;

    public LiveDirsModel(I defaultInitiator, Function<T, Path> projector, Function<Path, T> injector) {
        this.defaultInitiator = defaultInitiator;
        this.projector = projector;
        this.injector = injector;
        this.reporter = new Reporter<I>() {
            @Override
            public void reportCreation(Path baseDir, Path relPath, I initiator) {
                LiveDirsModel.this.creations.push(Update.creation(baseDir, relPath, initiator));
            }

            @Override
            public void reportDeletion(Path baseDir, Path relPath, I initiator) {
                LiveDirsModel.this.deletions.push(Update.deletion(baseDir, relPath, initiator));
            }

            @Override
            public void reportModification(Path baseDir, Path relPath, I initiator) {
                LiveDirsModel.this.modifications.push(Update.modification(baseDir, relPath, initiator));
            }

            @Override
            public void reportError(Throwable error) {
                LiveDirsModel.this.errors.push(error);
            }
        };
    }

    @Override
    public TreeItem<T> getRoot() {
        return this.root;
    }

    @Override
    public EventStream<Update<I>> creations() {
        return this.creations;
    }

    @Override
    public EventStream<Update<I>> deletions() {
        return this.deletions;
    }

    @Override
    public EventStream<Update<I>> modifications() {
        return this.modifications;
    }

    public EventStream<Throwable> errors() {
        return this.errors;
    }

    @Override
    public void setGraphicFactory(GraphicFactory factory) {
        this.graphicFactory = factory != null ? factory : DEFAULT_GRAPHIC_FACTORY;
    }

    @Override
    public boolean contains(Path path) {
        return this.topLevelAncestorStream(path).anyMatch(root ->
                root.contains(root.getPath().relativize(path)));
    }

    @Override
    public Option<TreeItem<T>> getItem(Path path) {
        return Option.ofOptional(this.topLevelAncestorStream(path).filter(root ->
                root.getPath().equals(path)).findFirst());
    }

    public boolean containsPrefixOf(Path path) {
        return this.root.getChildren().stream()
                .anyMatch(item -> path.startsWith(this.projector.apply(item.getValue())));
    }

    void addTopLevelDirectory(Path dir) {
        this.root.getChildren().add(new TopLevelDirItem<>(this.injector.apply(dir), this.graphicFactory, this.projector, this.injector, this.reporter));
    }

    void updateModificationTime(Path path, FileTime lastModified, I initiator) {
        for (TopLevelDirItem<I, T> root : this.getTopLevelAncestorsNonEmpty(path)) {
            Path relPath = root.getPath().relativize(path);
            root.updateModificationTime(relPath, lastModified, initiator);
        }
    }

    void addDirectory(Path path, I initiator) {
        this.topLevelAncestorStream(path).forEach(root -> {
            Path relPath = root.getPath().relativize(path);
            root.addDirectory(relPath, initiator);
        });
    }

    void addFile(Path path, I initiator, FileTime lastModified) {
        this.topLevelAncestorStream(path).forEach(root -> {
            Path relPath = root.getPath().relativize(path);
            root.addFile(relPath, lastModified, initiator);
        });
    }

    void delete(Path path, I initiator) {
        for (TopLevelDirItem<I, T> root : this.getTopLevelAncestorsNonEmpty(path)) {
            Path relPath = root.getPath().relativize(path);
            root.remove(relPath, initiator);
        }
    }

    void sync(PathNode tree) {
        Path path = tree.getPath();
        this.topLevelAncestorStream(path)
                .forEach(root -> root.sync(tree, this.defaultInitiator));
    }

    private Stream<TopLevelDirItem<I, T>> topLevelAncestorStream(Path path) {
        return this.root.getChildren().stream()
                .filter(item -> path.startsWith(this.projector.apply(item.getValue())))
                .map(item -> (TopLevelDirItem<I, T>) item);
    }

    private List<TopLevelDirItem<I, T>> getTopLevelAncestors(Path path) {
        return Arrays.asList(this.topLevelAncestorStream(path)
                .<TopLevelDirItem<I, T>>toArray(TopLevelDirItem[]::new));
    }

    private List<TopLevelDirItem<I, T>> getTopLevelAncestorsNonEmpty(Path path) {
        List<TopLevelDirItem<I, T>> roots = this.getTopLevelAncestors(path);
        assert !roots.isEmpty() : "path resolved against a dir that was reported to be in the model does not have a top-level ancestor in the model";
        return roots;
    }
}