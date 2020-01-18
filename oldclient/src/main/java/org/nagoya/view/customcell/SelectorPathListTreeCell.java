package org.nagoya.view.customcell;

import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class SelectorPathListTreeCell extends PathListTreeCell {

    private ContextMenu useMenu = null;


    public SelectorPathListTreeCell() {
    }

    public SelectorPathListTreeCell(String menuText, Consumer<Path> pathConsumer) {
        if (menuText != null) {
            MenuItem addMenuItem = new MenuItem(menuText);
            this.useMenu = new ContextMenu();
            this.useMenu.getItems().add(addMenuItem);
            addMenuItem.setOnAction((e) -> pathConsumer.accept(this.getTreeItem().getValue()));
        }
    }

    public SelectorPathListTreeCell(@NotNull Vector<Tuple2<String, Consumer<Path>>> menus) {
        if (menus.size() > 0) {
            this.useMenu = new ContextMenu();
            menus.forEach(t -> {
                var item = new MenuItem(t._1);
                this.useMenu.getItems().add(item);
                item.setOnAction((e) -> t._2.accept(this.getTreeItem().getValue()));
            });
        }
    }

    @NotNull
    @Contract("_, _ -> new")
    public static SelectorPathListTreeCell createCell(String menuText, Consumer<Path> pathConsumer) {
        return new SelectorPathListTreeCell(menuText, pathConsumer);
    }

    @NotNull
    @Contract("_ -> new")
    public static SelectorPathListTreeCell createCell(Vector<Tuple2<String, Consumer<Path>>> menus) {
        return new SelectorPathListTreeCell(menus);
    }

    @Override
    public void updateItem(Path data, boolean empty) {
        super.updateItem(data, empty);

        if (!empty && !this.isEditing() && data != null) {
            Path infoPath = data.resolve("info.txt");
            if (Files.exists(infoPath)) {
                this.setText(data.getFileName().toString() + " < * >" /*this.getTreeItem().getChildren().size()*/);
            }
        }

        if (this.useMenu != null) {
            this.setContextMenu(this.useMenu);
        }
    }
}
