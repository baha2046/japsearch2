package org.nagoya.system;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.function.Function;

public abstract class PathItem<T> extends TreeItem<T> {

    private final Function<T, Path> projector;

    @Contract(pure = true)
    public final Function<T, Path> getProjector() {
        return this.projector;
    }

    public final Path getPath() {
        return this.projector.apply(this.getValue());
    }

    protected PathItem(T path, Node graphic, Function<T, Path> projector) {
        super(path, graphic);
        this.projector = projector;
    }

    @Override
    public final boolean isLeaf() {
        return !this.isDirectory();
    }

    public abstract boolean isDirectory();

    public FileItem<T> asFileItem() {
        return (FileItem<T>) this;
    }

    public DirItem<T> asDirItem() {
        return (DirItem<T>) this;
    }

    public PathItem<T> getRelChild(@NotNull Path relPath) {
        assert relPath.getNameCount() == 1;
        Path childValue = this.getPath().resolve(relPath);
        for (TreeItem<T> ch : this.getChildren()) {
            PathItem<T> pathCh = (PathItem<T>) ch;
            if (pathCh.getPath().equals(childValue)) {
                return pathCh;
            }
        }
        return null;
    }

    protected PathItem<T> resolve(@NotNull Path relPath) {
        int len = relPath.getNameCount();
        if (len == 0) {
            return this;
        } else {
            PathItem<T> child = this.getRelChild(relPath.getName(0));
            if (child == null) {
                return null;
            } else if (len == 1) {
                return child;
            } else {
                return child.resolve(relPath.subpath(1, len));
            }
        }
    }
}

class FileItem<T> extends PathItem<T> {
    @NotNull
    @Contract("_, _, _, _ -> new")
    public static <T> FileItem<T> create(T path, FileTime lastModified, @NotNull DirectoryModel.GraphicFactory graphicFactory, Function<T, Path> projector) {
        return new FileItem<>(path, lastModified, graphicFactory.createGraphic(projector.apply(path), false), projector);
    }

    private FileTime lastModified;

    private FileItem(T path, FileTime lastModified, Node graphic, Function<T, Path> projector) {
        super(path, graphic, projector);
        this.lastModified = lastModified;
    }

    @Contract(pure = true)
    @Override
    public final boolean isDirectory() {
        return false;
    }

    public boolean updateModificationTime(@NotNull FileTime lastModified) {
        if (lastModified.compareTo(this.lastModified) > 0) {
            this.lastModified = lastModified;
            return true;
        } else {
            return false;
        }
    }
}

class DirItem<T> extends PathItem<T> {

    private final Function<Path, T> injector;

    @Contract(pure = true)
    protected final Function<Path, T> getInjector() {
        return this.injector;
    }

    public final T inject(Path path) {
        return this.injector.apply(path);
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    public static <T> DirItem<T> create(T path, @NotNull DirectoryModel.GraphicFactory graphicFactory, Function<T, Path> projector, Function<Path, T> injector) {
        return new DirItem<>(path, graphicFactory.createGraphic(projector.apply(path), true), projector, injector);
    }

    protected DirItem(T path, Node graphic, Function<T, Path> projector, Function<Path, T> injector) {
        super(path, graphic, projector);
        this.injector = injector;
    }

    @Contract(pure = true)
    @Override
    public final boolean isDirectory() {
        return true;
    }

    public FileItem<T> addChildFile(@NotNull Path fileName, FileTime lastModified, DirectoryModel.GraphicFactory graphicFactory) {
        assert fileName.getNameCount() == 1;
        int i = this.getFileInsertionIndex(fileName.toString());

        FileItem<T> child = FileItem.create(this.inject(this.getPath().resolve(fileName)), lastModified, graphicFactory, this.getProjector());
        this.getChildren().add(i, child);
        return child;
    }

    public DirItem<T> addChildDir(@NotNull Path dirName, DirectoryModel.GraphicFactory graphicFactory) {
        assert dirName.getNameCount() == 1;
        int i = this.getDirInsertionIndex(dirName.toString());

        DirItem<T> child = DirItem.create(this.inject(this.getPath().resolve(dirName)), graphicFactory, this.getProjector(), this.getInjector());
        this.getChildren().add(i, child);
        return child;
    }

    private int getFileInsertionIndex(String fileName) {
        ObservableList<TreeItem<T>> children = this.getChildren();
        int n = children.size();
        for (int i = 0; i < n; ++i) {
            PathItem<T> child = (PathItem<T>) children.get(i);
            if (!child.isDirectory()) {
                String childName = child.getPath().getFileName().toString();
                if (childName.compareToIgnoreCase(fileName) > 0) {
                    return i;
                }
            }
        }
        return n;
    }

    private int getDirInsertionIndex(String dirName) {
        ObservableList<TreeItem<T>> children = this.getChildren();
        int n = children.size();
        for (int i = 0; i < n; ++i) {
            PathItem<T> child = (PathItem<T>) children.get(i);
            if (child.isDirectory()) {
                String childName = child.getPath().getFileName().toString();
                if (childName.compareToIgnoreCase(dirName) > 0) {
                    return i;
                }
            } else {
                return i;
            }
        }
        return n;
    }
}

class ParentChild<T> {
    private final DirItem<T> parent;
    private final PathItem<T> child;

    @Contract(pure = true)
    public ParentChild(DirItem<T> parent, PathItem<T> child) {
        this.parent = parent;
        this.child = child;
    }

    public DirItem<T> getParent() {
        return this.parent;
    }

    public PathItem<T> getChild() {
        return this.child;
    }
}

interface Reporter<I> {
    void reportCreation(Path baseDir, Path relPath, I initiator);

    void reportDeletion(Path baseDir, Path relPath, I initiator);

    void reportModification(Path baseDir, Path relPath, I initiator);

    void reportError(Throwable error);
}

class TopLevelDirItem<I, T> extends DirItem<T> {
    private final DirectoryModel.GraphicFactory graphicFactory;
    private final Reporter<I> reporter;

    TopLevelDirItem(T path, @NotNull DirectoryModel.GraphicFactory graphicFactory, Function<T, Path> projector, Function<Path, T> injector, Reporter<I> reporter) {
        super(path, graphicFactory.createGraphic(projector.apply(path), true), projector, injector);
        this.graphicFactory = graphicFactory;
        this.reporter = reporter;
    }

    @NotNull
    @Contract("_ -> new")
    private ParentChild<T> resolveInParent(@NotNull Path relPath) {
        int len = relPath.getNameCount();
        if (len == 0) {
            return new ParentChild<>(null, this);
        } else if (len == 1) {
            if (this.getPath().resolve(relPath).equals(this.getValue())) {
                return new ParentChild<>(null, this);
            } else {
                return new ParentChild<>(this, this.getRelChild(relPath.getName(0)));
            }
        } else {
            PathItem<T> parent = this.resolve(relPath.subpath(0, len - 1));
            if (parent == null || !parent.isDirectory()) {
                return new ParentChild<>(null, null);
            } else {
                PathItem<T> child = parent.getRelChild(relPath.getFileName());
                return new ParentChild<>(parent.asDirItem(), child);
            }
        }
    }

    private void updateFile(Path relPath, FileTime lastModified, I initiator) {
        PathItem<T> item = this.resolve(relPath);
        if (item == null || item.isDirectory()) {
            this.sync(PathNode.file(this.getPath().resolve(relPath), lastModified), initiator);
        }
    }

    public boolean contains(Path relPath) {
        return this.resolve(relPath) != null;
    }

    public void addFile(Path relPath, FileTime lastModified, I initiator) {
        this.updateFile(relPath, lastModified, initiator);
    }

    public void updateModificationTime(Path relPath, FileTime lastModified, I initiator) {
        this.updateFile(relPath, lastModified, initiator);
    }

    public void addDirectory(Path relPath, I initiator) {
        PathItem<T> item = this.resolve(relPath);
        if (item == null || !item.isDirectory()) {
            this.sync(PathNode.directory(this.getPath().resolve(relPath), Collections.emptyList()), initiator);
        }
    }

    public void sync(@NotNull PathNode tree, I initiator) {
        Path path = tree.getPath();
        Path relPath = this.getPath().relativize(path);
        ParentChild<T> pc = this.resolveInParent(relPath);
        DirItem<T> parent = pc.getParent();
        PathItem<T> item = pc.getChild();
        if (parent != null) {
            this.syncChild(parent, relPath.getFileName(), tree, initiator);
        } else if (item == null) { // neither path nor its parent present in model
            this.raise(new NoSuchElementException("Parent directory for " + relPath + " does not exist within " + this.getValue()));
        } else { // resolved to top-level dir
            assert item == this;
            if (tree.isDirectory()) {
                this.syncContent(this, tree, initiator);
            } else {
                this.raise(new IllegalArgumentException("Cannot replace top-level directory " + this.getValue() + " with a file"));
            }
        }
    }

    private void syncContent(DirItem<T> dir, @NotNull PathNode tree, I initiator) {
        Set<Path> desiredChildren = new HashSet<>();
        for (PathNode ch : tree.getChildren()) {
            desiredChildren.add(ch.getPath());
        }

        ArrayList<TreeItem<T>> actualChildren = new ArrayList<>(dir.getChildren());

        // remove undesired children
        for (TreeItem<T> ch : actualChildren) {
            if (!desiredChildren.contains(this.getProjector().apply(ch.getValue()))) {
                this.removeNode(ch, null);
            }
        }

        // synchronize desired children
        for (PathNode ch : tree.getChildren()) {
            this.sync(ch, initiator);
        }
    }

    private void syncChild(@NotNull DirItem<T> parent, Path childName, PathNode tree, I initiator) {
        PathItem<T> child = parent.getRelChild(childName);
        if (child != null && child.isDirectory() != tree.isDirectory()) {
            this.removeNode(child, null);
        }
        if (child == null) {
            if (tree.isDirectory()) {
                DirItem<T> dirChild = parent.addChildDir(childName, this.graphicFactory);
                this.reporter.reportCreation(this.getPath(), this.getPath().relativize(dirChild.getPath()), initiator);
                this.syncContent(dirChild, tree, initiator);
            } else {
                FileItem<T> fileChild = parent.addChildFile(childName, tree.getLastModified(), this.graphicFactory);
                this.reporter.reportCreation(this.getPath(), this.getPath().relativize(fileChild.getPath()), initiator);
            }
        } else {
            if (child.isDirectory()) {
                this.syncContent(child.asDirItem(), tree, initiator);
            } else {
                if (child.asFileItem().updateModificationTime(tree.getLastModified())) {
                    this.reporter.reportModification(this.getPath(), this.getPath().relativize(child.getPath()), initiator);
                }
            }
        }
    }

    public void remove(Path relPath, I initiator) {
        PathItem<T> item = this.resolve(relPath);
        if (item != null) {
            this.removeNode(item, initiator);
        }
    }

    private void removeNode(TreeItem<T> node, I initiator) {
        this.signalDeletionRecursively(node, initiator);
        node.getParent().getChildren().remove(node);
    }

    private void signalDeletionRecursively(@NotNull TreeItem<T> node, I initiator) {
        for (TreeItem<T> child : node.getChildren()) {
            this.signalDeletionRecursively(child, initiator);
        }
        this.reporter.reportDeletion(this.getPath(), this.getPath().relativize(this.getProjector().apply(node.getValue())), initiator);
    }

    private void raise(Throwable t) {
        try {
            throw t;
        } catch (Throwable e) {
            this.reporter.reportError(e);
        }
    }
}
