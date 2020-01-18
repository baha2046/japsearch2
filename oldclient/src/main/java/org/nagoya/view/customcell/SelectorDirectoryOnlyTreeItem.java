package org.nagoya.view.customcell;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;

import static org.nagoya.system.DirectoryModel.DEFAULT_GRAPHIC_FACTORY;

public class SelectorDirectoryOnlyTreeItem extends TreeItem<Path> {
    private boolean isFirstTimeChildren = true;

    private SelectorDirectoryOnlyTreeItem(Path path) {
        super(path);

        this.setGraphic(DEFAULT_GRAPHIC_FACTORY.createGraphic(path, true));
    }

    @NotNull
    @Contract("_ -> new")
    public static TreeItem<Path> createNode(Path path) {
        return new SelectorDirectoryOnlyTreeItem(path);
    }

    @Override
    public ObservableList<TreeItem<Path>> getChildren() {
        if (this.isFirstTimeChildren) {
            this.isFirstTimeChildren = false;
            super.getChildren().setAll(this.buildChildren(this));
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        if (!this.isFirstTimeChildren) {
            return this.getChildren().isEmpty();
        }
        return false;
    }

    private ObservableList<TreeItem<Path>> buildChildren(@NotNull TreeItem<Path> treeItem) {
        Path path = treeItem.getValue();
        if (path != null && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            ObservableList<TreeItem<Path>> children = FXCollections.observableArrayList();
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(path, Files::isDirectory)) {
                for (Path dir : dirs) {
                    children.add(createNode(dir));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return children;
        }
        return FXCollections.emptyObservableList();
    }
}