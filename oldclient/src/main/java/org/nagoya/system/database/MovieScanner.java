package org.nagoya.system.database;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Separator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controller.FXFileListControl;
import org.nagoya.controller.FXTaskBarControl;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.model.dataitem.ID;
import org.nagoya.preferences.CustomOptions;
import org.nagoya.preferences.GuiSettings;
import org.nagoya.preferences.options.OptionBase;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.system.event.CustomEventSourceImp;
import org.nagoya.view.dialog.FXSelectPathDialog;
import org.nagoya.view.editor.FXSettingEditor;

import java.nio.file.Path;
import java.util.Objects;

public class MovieScanner {

    private static MovieScanner INSTANCE = null;
    private static boolean AUTO_RUN = true;

    @NotNull
    private static CustomOptions getOptions() {
        String classname = "MovieDB";
        CustomOptions customOptions = new CustomOptions("Movie Data Cache");
        customOptions.addOption(classname + "-isAutoRun", AUTO_RUN, (b) -> AUTO_RUN = b, "Auto Scan on Start : ");

        JFXButton button = FXFactory.buttonWithBorder("Manuel Scan", e -> getInstance().buildAVDB());
        button.disableProperty().bind(getInstance().isLoadDoneProperty().or(getInstance().isLoadingProperty()));
        button.setMinWidth(OptionBase.DEFAULT_BUTTON_WIDTH);
        customOptions.addOption(button);

        return customOptions;
    }

    public static MovieScanner getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MovieScanner();
            FXSettingEditor.add(getOptions());
        }
        return INSTANCE;
    }

    private Path rootPath;
    private DirectoryEntry movieRootEntry;
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty loadDone = new SimpleBooleanProperty(false);

    @Contract(pure = true)
    private MovieScanner() {
    }

    public DirectoryEntry initBaseEntry() {

        Path root = GuiSettings.getInstance().getDirectory(GuiSettings.Key.avDirectory);

        if (root.equals(GuiSettings.homePath)) {
            Option<Path> getRoot;
            do {
                getRoot = FXSelectPathDialog.show("Select your root directory of Movie Collections :", root);
            } while (getRoot.isEmpty());

            root = getRoot.get();
            GuiSettings.getInstance().setDirectory(GuiSettings.Key.avDirectory, root);
        }

        this.rootPath = root;
        this.movieRootEntry = DirectoryEntry.of(this.rootPath);

        return this.movieRootEntry;
    }

    public void doAutoScan() {
        if (AUTO_RUN) {
            this.buildAVDB();
        }
    }

    public void buildAVDB() {
        if (!this.loading.getValue()) {
            this.loading.setValue(true);
            CustomEventSourceImp.fire(FXTaskBarControl.EVENT_ADD_TASK, "Scanning Movie ...");

            ExecuteSystem.useExecutors(ExecuteSystem.role.MOVIE, () -> {
                GUICommon.debugMessage("Systems >> MovieDB >> buildAVDB start");

                this.preloadTree(this.movieRootEntry, 0);
                this.loadDone.setValue(true);
                this.loading.setValue(false);
                MovieDB.getInstance().clearUpUnusedCache();

                CustomEventSourceImp.fire(FXTaskBarControl.EVENT_REMOVE_TASK, "Scanning Movie ...");
                GUICommon.debugMessage("Systems >> MovieDB >> buildAVDB end");
            });
        }
    }

    private void count() {
        DirectoryEntry base = Systems.getDirectorySystem().getMovieRootEntry();
        base.getChildrenEntry().peek(d -> {
            int i = d.getChildrenEntry().count(e -> e.getMovieDataOption().isDefined());
            d.getMakerData().peek(md -> md.setMovieCount(i));
        });
    }

    public boolean isLoadDone() {
        return this.loadDone.getValue();
    }

    public BooleanProperty isLoadDoneProperty() {
        return this.loadDone;
    }

    public BooleanProperty isLoadingProperty() {
        return this.loading;
    }

    private void preloadTree(@NotNull DirectoryEntry d, int level) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        //GUICommon.debugMessage("preloadTree " + d.getFileName());
        Vector<DirectoryEntry> v = d.getChildrenEntry();

        if (level >= 2 || v.isEmpty()) {
            d.apply();
        } else {
            v.forEach(e -> this.preloadTree(e, level + 1));
        }
    }

    public DirectoryEntry getMovieRootEntry() {
        return this.movieRootEntry;
    }

    public Path getMovieRootPath() {
        return this.rootPath;
    }

    public Option<DirectoryEntry> getEntryFromPath(Path path) {
        if (path != null && path.startsWith(this.rootPath)) {

            Path rel = this.rootPath.relativize(path);
            Option<DirectoryEntry> directoryEntry = Option.of(this.movieRootEntry);

            for (int x = 0; x < rel.getNameCount(); x++) {
                Path check = rel.getName(x);
                Vector<DirectoryEntry> v = directoryEntry.map(DirectoryEntry::getChildrenEntry).getOrElse(Vector.empty());
                directoryEntry = v.find(d -> d.getValue().getFileName().equals(check));
            }
            //GUICommon.debugMessage("getEntryFromPath " + path.toString());

            return directoryEntry;
        }

        return Option.none();
    }

    public static void searchIDDialog() {
        searchIDDialog("");
    }

    public static void searchIDDialog(String strId) {
        JFXTextField textField = FXFactory.textField(strId, 300);

        JFXListView<String> textArea = FXFactory.textArea(600, 100, true);
        Option<ObservableList<String>> observableList = Option.of(textArea.getItems());

        JFXButton button = FXFactory.buttonWithBorder("Search", (e) -> {
            Systems.useExecutors(() -> {
                GUICommon.writeToObList("Start searching...", observableList);
                MovieDB.getInstance().findByMovieID(new ID(textField.getText()))
                        .peek(p -> GUICommon.writeToObList("Result : " + p.toString(), observableList))
                        .peek(p -> Systems.getDirectorySystem().changePathTo(p.getParent(),
                                FXFileListControl.getInstance().getSelectEntryRunnable(p)))
                        .onEmpty(() -> GUICommon.writeToObList("Not Found", observableList));
            });
        });

        DialogBuilder.create()
                .heading("[ Search by Movie ID ]")
                .body(GUICommon.vbox(10, textField, button, new Separator(), textArea))
                .build().show();
    }

    public static void searchActorDialog(String actorName) {
        JFXTextField textField = FXFactory.textField(actorName, 300);

        JFXListView<String> textArea = FXFactory.textArea(900, 500, true);
        Option<ObservableList<String>> observableList = Option.of(textArea.getItems());

        textArea.setOnMouseClicked((event) -> {
            if (event.getClickCount() == 2) {
                String s = textArea.getSelectionModel().getSelectedItem();
                if (s.startsWith("> ")) {
                    Path targetPath = Path.of(s.replace("> ", ""));
                    Option<DirectoryEntry> directoryEntry = Systems.getDirectorySystem().getDirectoryEntryFromPath(targetPath);
                    if (directoryEntry.isDefined()) {
                        DirectoryEntry parent = directoryEntry
                                .map(DirectoryEntry::getEntryParent)
                                .filter(Objects::nonNull)
                                .peek(d -> d.setLastSelectedIndex(d.getChildrenEntry().indexOf(directoryEntry.get())))
                                .getOrNull();

                        if (parent != null) {
                            Systems.getDirectorySystem().changePathTo(parent, null);
                        }
                    }
                }
            }
        });

        JFXButton button = FXFactory.buttonWithBorder("Search", (e) -> {
            Systems.useExecutors(() -> {
                GUICommon.writeToObList("Start searching...", observableList);
                Stream<Path> path = MovieDB.getInstance().findByActor(textField.getText());
                if (path.size() > 0) {
                    path.forEach(p -> GUICommon.writeToObList("> " + p.toString(), observableList));
                    GUICommon.writeToObList("End", observableList);
                    //Systems.getDirectorySystem().changePathTo(path.get(), GUICommon.getGuiSettings()::setLastUsedDirectory, -1);
                } else {
                    GUICommon.writeToObList("Not Found", observableList);
                }
            });
        });

        DialogBuilder.create()
                .heading("[ Search by Actor Name ]")
                .body(GUICommon.vbox(10, textField, button, new Separator(), textArea))
                .build().show();
    }
}
