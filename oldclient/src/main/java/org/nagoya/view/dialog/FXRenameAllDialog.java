package org.nagoya.view.dialog;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import javafx.collections.ObservableList;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.system.DirectorySystem;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.view.editor.FXRenameFormatEditor;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public class FXRenameAllDialog {

    public static void show() {
        JFXListView<String> textArea = FXFactory.textArea(500, 500, true);

        JFXButton btnEdit = FXFactory.buttonWithBorder("Setting", e -> FXRenameFormatEditor.show(FXRenameFormatEditor.Type.DIRECTORY_NAME));

        JFXButton btnRun = GUICommon.buttonWithCheck("Rename All", null);

        btnRun.setOnAction(e -> {
            textArea.getItems().clear();
            Option<ObservableList<String>> outs = Option.of(textArea.getItems());

            Stream<Mono<DirectorySystem>> monoStream = Systems.getDirectorySystem()
                    .getDirectoryEntries()
                    .toStream()
                    .filter(DirectoryEntry::isDirectory)
                    .filter(DirectoryEntry::hasNfo)
                    .map(directoryEntry -> Mono.fromCallable(() -> FXMoveMappingDialog.getSuitableFolderName(directoryEntry))
                            .<Path>handle((newName, sink) -> {
                                if (!newName.equals(directoryEntry.getValue().getFileName().toString())) {
                                    Path path = directoryEntry.getValue().resolveSibling(newName);
                                    GUICommon.writeToObList(path.getFileName().toString(), outs);
                                    sink.next(path);
                                } else {
                                    GUICommon.writeToObList("Skip - same name (" + newName + ")", outs);
                                    sink.complete();
                                }
                            }).flatMap(path -> Systems.getDirectorySystem().chainRenameFile(directoryEntry, path)));

            Mono.when(monoStream)
                    .doFinally(v -> Systems.getDirectorySystem().reloadDirectory(Option.none()))
                    .doFinally(v -> GUICommon.writeToObList("Finish", outs))
                    .subscribeOn(ExecuteSystem.get().getIoScheduler())
                    .subscribe();
        });

        VBox vBox = GUICommon.vbox(15, textArea, new Separator(), btnEdit, btnRun);
        vBox.setMinWidth(502);

        DialogBuilder.create()
                .heading("[ Rename Movies ]")
                .body(vBox)
                .build().show();
    }
}
