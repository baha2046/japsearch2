package org.nagoya.model;

import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class PathEntry {
    private final Path path;
    private final boolean isDirectory;
    private final List<PathEntry> children;
    private final FileTime lastModified;

    @Contract(pure = true)
    private PathEntry(Path path, boolean isDirectory, List<PathEntry> children, FileTime lastModified) {
        this.path = path;
        this.isDirectory = isDirectory;
        this.children = children;
        this.lastModified = lastModified;
    }

    public Path getPath() {
        return this.path;
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public List<PathEntry> getChildren() {
        return this.children;
    }

    public FileTime getLastModified() {
        return this.lastModified;
    }


    public static PathEntry getTree(Path root) throws IOException {
        if (Files.isDirectory(root)) {
            Path[] childPaths;
            try (Stream<Path> dirStream = Files.list(root)) {
                childPaths = dirStream
                        .sorted(PATH_COMPARATOR)
                        .toArray(Path[]::new);
            }
            List<PathEntry> children = new ArrayList<>(childPaths.length);
            for (Path p : childPaths) {
                children.add(getTree(p));
            }
            return directory(root, children);
        } else {
            return file(root, Files.getLastModifiedTime(root));
        }
    }

    private static final Comparator<Path> PATH_COMPARATOR = (p, q) -> {
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

    static PathEntry file(Path path, FileTime lastModified) {
        return new PathEntry(path, false, Collections.emptyList(), lastModified);
    }

    static PathEntry directory(Path path, List<PathEntry> children) {
        return new PathEntry(path, true, children, null);
    }

}
