package org.nagoya.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vavr.control.Option;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.model.xmlserialization.PathTypeAdapter;
import org.nagoya.model.xmlserialization.TreeItemTypeAdapter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.stream.Stream;

public class DirectoryTreeManager {

    private final Gson gson;
    private TreeItem<Path> rootTree;
    private final Path cacheFile;

    private boolean isReady = false;

    public DirectoryTreeManager(Path rootPath, Path inCacheFile) {
        this.cacheFile = inCacheFile;

        TreeItemTypeAdapter<Path> adapter = new TreeItemTypeAdapter<>(Path.class);
        this.gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter())
                .registerTypeAdapter(TreeItem.class, adapter)
                .create();
        adapter.setGson(this.gson);

        // create root
        this.rootTree = new TreeItem<>();

        if (Files.exists(this.cacheFile, LinkOption.NOFOLLOW_LINKS)) {
            this.loadFromCacheFile();
            GUICommon.debugMessage(() -> ">> Loadec " + this.cacheFile.getFileName().toString());
        } else {
            this.buildDirectoryTree(rootPath);
        }
    }

    private void loadFromCacheFile() {
        this.rootTree = this.gson.fromJson(UtilCommon.readStringFromFile(this.cacheFile), TreeItem.class);
        this.isReady = true;
    }

    private void saveToCacheFile() {
        UtilCommon.saveStringToFile(this.cacheFile, this.gson.toJson(this.rootTree, TreeItem.class));
    }

    public void buildDirectoryTree(Path rootPath) {

        this.rootTree.setValue(rootPath);
        this.rootTree.setExpanded(true);

        ExecuteSystem.useExecutors(ExecuteSystem.role.IMAGE, () -> {

            GUICommon.debugMessage(() -> "Start build tree");

            // create tree structure
            try {
                this.createTree(this.rootTree);
            } catch (IOException e) {
                e.printStackTrace();
            }

            GUICommon.debugMessage(() -> "Finish build tree");

            this.saveToCacheFile();
            this.isReady = true;
        });
    }

    /**
     * Recursively create the tree
     */
    private void createTree(@NotNull TreeItem<Path> rootItem) throws IOException {

        //GUICommon.debugMessage(rootItem.getValue().toString());
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rootItem.getValue())) {

            for (Path path : directoryStream) {
                TreeItem<Path> newItem = new TreeItem<>(path);
                newItem.setExpanded(true);

                rootItem.getChildren().add(newItem);

                if (Files.isDirectory(path)) {
                    this.createTree(newItem);
                }
            }
        }
    }

    private Stream<TreeItem<Path>> getTopStream(Path path) {
        return this.rootTree.getChildren().stream()
                .filter(item -> path.startsWith(item.getValue()));
    }

    public Option<TreeItem<Path>> getItem(Path path) {
        return Option.ofOptional(this.getTopStream(path).filter(root ->
                root.getValue().equals(path)).findFirst());
    }

    public boolean isReady() {
        return this.isReady;
    }
}
