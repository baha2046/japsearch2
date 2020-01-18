package org.nagoya.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controls.FXActorDetailWindow;
import org.nagoya.controls.FXActressDatabaseWindow;
import org.nagoya.controls.FXImageViewerWindow;
import org.nagoya.controls.FXListView;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.model.MovieFolder;
import org.nagoya.model.MovieV2;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.model.dataitem.ID;
import org.nagoya.preferences.GuiSettings;
import org.nagoya.system.DirectorySystem;
import org.nagoya.system.MovieLock;
import org.nagoya.system.Systems;
import org.nagoya.system.database.MovieScanner;
import org.nagoya.system.database.MovieDB;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.system.event.CustomEventType;
import org.nagoya.system.event.FXContextImp;
import org.nagoya.view.customcell.FileListCell;
import org.nagoya.view.dialog.*;
import org.nagoya.view.editor.FXRenameFormatEditor;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.nagoya.system.DirectorySystem.EVENT_DIRECTORY_SYSTEM_UPDATE;

public class FXFileListControl extends FXContextImp {

    public static final CustomEventType<DirectoryEntry> EVENT_LIST_SELECTED = new CustomEventType<>("EVENT_LIST_SELECTED");
    public static final CustomEventType<Void> EVENT_FOCUS = new CustomEventType<>("EVENT_FOCUS");
    public static final CustomEventType<Void> EVENT_CUSTOM_FLAG_EDIT = new CustomEventType<>("EVENT_CUSTOM_FLAG_EDIT");

    private static FXFileListControl instance = null;

    @FXML
    private JFXButton btnRenameAll;

    @FXML
    private Pane listViewPane;

    @FXML
    private SplitMenuButton spMoveMenu, spFilterMenu, spSearchMenu, spShortcutMenu;

    private FXListView<DirectoryEntry> listView;
    private final ObservableList<DirectoryEntry> listViewItem;
    private final FilteredList<DirectoryEntry> listViewFilteredItem;

    private final StringProperty filter;
    private final Predicate<DirectoryEntry> predicateAll;
    private final Predicate<DirectoryEntry> predicateCustom;

    private final ContextMenu menuFolder;
    private final ContextMenu menuFile;

    private final ObjectProperty<DirectoryEntry> selectedEntry = new SimpleObjectProperty<>();
    private final IntegerProperty selectedIndex = new SimpleIntegerProperty();
    private boolean nextScrollTo = false;

    public FXFileListControl() {
        //super(DirectoryEntry.class, Void.class);
        this.filter = new SimpleStringProperty("All");
        this.predicateAll = s -> true;
        this.predicateCustom = s -> s.getMovieDataOption().filter(m -> m.getMovieID().contains(this.filter.get())).isDefined();

        this.listViewItem = FXCollections.observableArrayList(DirectoryEntry.extractor());
        this.listViewFilteredItem = new FilteredList<>(this.listViewItem, this.predicateAll);

        MenuItem d1 = new MenuItem("Enter");
        d1.setOnAction(e -> this.enterFolderAction());
        MenuItem d2 = new MenuItem("Show as Gallery");
        d2.setOnAction(e -> this.showGalleryAction());
        MenuItem d11 = new MenuItem("Rename Folder");
        d11.setOnAction(e -> this.renameAction());
        MenuItem d12 = new MenuItem("Delete Folder");
        d12.setOnAction(e -> this.delAction());
        MenuItem d21 = new MenuItem("Move to AV Database");
        d21.setOnAction(e -> this.moveAction());
        MenuItem d22 = new MenuItem("Move to Need Process");
        d22.setOnAction(e -> this.markAsNeedProcess());

        this.menuFolder = new ContextMenu(d1, d2, new SeparatorMenuItem(), d11, d12, new SeparatorMenuItem(), d21, d22);

        MenuItem f1 = new MenuItem("Open");
        f1.setOnAction(e -> this.openFileAction());
        MenuItem f2 = new MenuItem("To Folder");
        f2.setOnAction(e -> this.packDirectoryAction());
        MenuItem f11 = new MenuItem("Rename");
        f11.setOnAction(e -> this.renameAction());
        MenuItem f12 = new MenuItem("Delete");
        f12.setOnAction(e -> this.delAction());

        this.menuFile = new ContextMenu(f1, new SeparatorMenuItem(), f2, f11, f12);
    }

    public static FXFileListControl getInstance() {
        if (null == instance) {
            instance = GUICommon.loadFXMLController(FXFileListControl.class);
        }
        return instance;
    }

    public void requestFocus() {
        this.listView.requestFocus();
    }

   /* public void updateListAction(List<DirectoryEntry> entryList) {
        system.out.println("------------ ReadDirectoryAndUpdateList ------------");
        system.out.println(this.data.size());
        //system.out.println(this.loadCache.estimatedSize());

        if (this.data.size() != entryList.size()) {
            this.listView.scrollTo(0);
        }
        this.data.clear();
        this.data.addAll(entryList);
        //this.listView.scrollTo(0);
    }*/


    private Mono<Integer> getSelectEntryProcess(Path path) {
        return Mono.just(path)
                .<Integer>handle((p, sink) -> {
                    if (p == null) {
                        sink.error(new Exception("Path is null"));
                    } else {
                        sink.next(Systems.getDirectorySystem().getIndexOfDirectoryEntry(p));
                    }
                })
                .doOnNext(this::listViewSelectByIndex);
    }

    private Option<Tuple2<Integer, DirectoryEntry>> getCurrentSelection() {
        DirectoryEntry select = this.listView.getSelectionModel().getSelectedItem();
        if (select == null) {
            return Option.none();
        }
        return Option.of(Tuple.of(this.listViewFilteredItem.getSourceIndex(this.listView.getSelectionModel().getSelectedIndex()), select));
    }

    private Option<Integer> getSelectedIndex() {
        int viewIndex = this.selectedIndex.get();
        if (viewIndex > -1) {
            int index = this.listViewFilteredItem.getSourceIndex(viewIndex);
            return (index > -1) ? Option.of(index) : Option.none();
        }

        return Option.none();
    }

    private Option<DirectoryEntry> getSelectedItem() {
        return Option.of(this.listView.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void sortAction() {
        FXActressDatabaseWindow.show();
        //   ActorDB.getInstance().show();
        //Systems.getDirectorySystem().sortContain();
        //MangaParser.loadHImage();
    }

    @FXML
    public void searchAction() {
        MovieScanner.searchIDDialog();
    }

    @FXML
    public void searchActorAction() {
        JFXTextField textField = GUICommon.textField("", 200);

        Runnable searchAction = () -> {
            Option<ActorV2> actor = MovieDB.actorDB().getData(textField.getText());
            if (actor.isDefined()) {
                FXActorDetailWindow.show(actor.get());
            } else {
                GUICommon.errorDialog("Actor not found");
            }
        };

        DialogBuilder.create()
                .heading("[ Search Actor ]")
                .body(textField)
                .button("Cancel", "Search", searchAction)
                .build().show();
    }

    @FXML
    public void upDirectoryAction() {
        var current = this.getSelectedItem();
        Runnable run = () -> {
            this.clearFilterAction();
            current.peek(this::listViewSelectByEntry);
        };
        Systems.getDirectorySystem().upParent(this.selectedIndex.get(), Option.of(run));
    }

    @FXML
    public void movieHomeDirectoryAction() {
    /*    VBox vBox = GUICommon.getVBox(10, b1, b2, b3, b4, b5, b6);
        vBox.setAlignment(Pos.CENTER);
        DialogBuilder.create()
                .body(vBox)
                .build().show();*/
        //Systems.getDirectorySystem().changePathTo(Systems.getDirectorySystem().getMovieRootEntry(), Option.of(this::clearFilterAction));
    }

    @NotNull
    private MenuItem genChangePathButton(String str, GuiSettings.Key key) {
        return FXFactory.menuItem(str, () ->
                Systems.getDirectorySystem().changePathTo(GuiSettings.getInstance().getDirectory(key), Option.of(this::clearFilterAction)));
    }

    @FXML
    public void delAction() {
        this.getSelectedIndex().peek(this::executeDelAction);
    }

    @FXML
    public void reloadAction() {
        Systems.getDirectorySystem().reloadDirectory(Option.none());//this.getSelectedItem().flatMap(this::getSelectEntryRunnable));
    }

    @FXML
    public void packDirectoryAction() {
        this.getSelectedItem().peek(this::executePackDirectoryAction);
    }

    @FXML
    public void renameAction() {
        this.getSelectedItem().peek(this::executeRenameAction);
    }

    @FXML
    public void renameAllAction() {
        FXRenameAllDialog.show();
    }

    @FXML
    public void browseAction() {
        Option<Path> path = FXSelectPathDialog.show("Change to :", Systems.getDirectorySystem().getCurrentPath());
        path.peek(p -> {
            Systems.getDirectorySystem().changePathTo(p, Option.of(this::clearFilterAction));
        });
    }

    @FXML
    public void moveAction() {
        this.getSelectedItem()
                .filter(DirectoryEntry::isDirectory)
                .filter(DirectoryEntry::hasNfo)
                .filter(select -> !select.getMovieData().getMovieMaker().isEmpty())
                .peek(this::exeMoveAction);
    }

    @FXML
    public void markAsNeedProcess() {
        this.getSelectedItem()
                .filter(DirectoryEntry::hasMovie)
                .peek(directoryEntry -> {
                    Path path = Paths.get("X:\\Download\\need process");
                    if (Files.exists(path) && Files.isDirectory(path)) {
                        DialogBuilder.create()
                                .heading("[ Confirm ]")
                                .body(new Text("Move to Need Process Folder ?"))
                                .buttonYesNo(() -> this.exeMoveAction(directoryEntry, path.resolve(directoryEntry.getValue().getFileName())))
                                .build().show();
                    }
                });
    }

    @FXML
    public void editMakerAction() {
        this.getSelectedItem()
                .filter(DirectoryEntry::isDirectory)
                .filter(Predicate.not(DirectoryEntry::hasNfo))
                .peek(this::exeEditMaker);
    }

    @FXML
    public void createDirectoryAction() {
        this.executeCreateMultiDirectoryAction(Systems.getDirectorySystem().getCurrentPath(), this.getSelectedItem());
    }

    @FXML
    public void createDirectoryMultiAction() {
        /*FXMovieGridWindow.getInstance().setData(
                Systems.getDirectorySystem().getCurrentEntry().getChildrenEntry().flatMap(DirectoryEntry::toSimpleMovie));
        FXMovieGridWindow.getInstance().displayWindow();*/
        FXMovieLocalGridWindow.show();
    }

    public void updateFilterItem() {
        Map<String, Integer> map = new HashMap<>();

        Stream.ofAll(this.listViewItem.stream())
                .filter(DirectoryEntry::hasMovie)
                .flatMap(DirectoryEntry::getMovieDataOption)
                .map(MovieV2::getMovieID)
                .forEach(id -> {
                    String s = new ID(id).getSeriesCode();
                    if (map.get(s) == null) {
                        map.put(s, 1);
                    } else {
                        map.replace(s, map.get(s) + 1);
                    }
                });

        List<MenuItem> list = new ArrayList<>();
        list.add(FXFactory.menuItem("All", this::clearFilterAction));

        map.forEach((s, l) -> {
            Runnable runnable = () -> {
                this.filter.set(s);
                this.selectFilterAction();
            };

            MenuItem menu = FXFactory.menuItem(s + " (" + l + ")", runnable);
            list.add(menu);
        });

        list.add(FXFactory.menuItem("Refresh", this::updateFilterItem));

        this.spFilterMenu.hide();
        this.spFilterMenu.getItems().setAll(list);
        this.spFilterMenu.show();
    }

    @FXML
    public void showSeriesAction() {
        // List<String> seriesList = new ArrayList<>();
        var dirEntry = Systems.getDirectorySystem().getDirectoryEntries();

        Map<String, Stream<String>> map = new HashMap<>();

        dirEntry.forEach(entry -> {
            var movieID = entry.getMovieDataOption().map(MovieV2::getMovieID).map(ID::new);
            if (movieID.isDefined()) {
                String s = movieID.getOrNull().getSeriesCode();
                String n = movieID.getOrNull().getSeriesNum();
                if (map.get(s) == null) {
                    map.put(s, Stream.of(n));
                } else {
                    var newList = map.get(s).append(n);
                    map.replace(s, newList);
                }
            }
        });

        VBox vBox = GUICommon.vbox(10);
        ToggleGroup group = new ToggleGroup();
        RadioButton buttonAll = new RadioButton("ALL");
        buttonAll.setUserData((Runnable) this::clearFilterAction);
        buttonAll.setToggleGroup(group);
        buttonAll.setSelected(true);
        vBox.getChildren().add(buttonAll);
        map.forEach((s, l) -> {
            GUICommon.debugMessage(s);
            JFXRadioButton button = new JFXRadioButton(s + " (" + l.length() + ")");
            button.setUserData((Runnable) () -> {
                this.filter.set(s);
                this.selectFilterAction();
            });
            button.setToggleGroup(group);
            vBox.getChildren().add(button);
        });

        DialogBuilder.create()
                .heading("[ Filter ]")
                .body(vBox)
                .buttonOkCancel(() -> {
                    Runnable runnable = (Runnable) group.getSelectedToggle().getUserData();
                    runnable.run();
                })
                .build()
                .show();
    }

    @FXML
    public void clearFilterAction() {
        this.filter.set("All");
        this.listViewFilteredItem.setPredicate(this.predicateAll);
    }

    public void selectFilterAction() {
        this.listViewFilteredItem.setPredicate(this.predicateAll);
        if (!this.filter.get().equals("All")) {
            this.listViewFilteredItem.setPredicate(this.predicateCustom);
        }
    }

    @FXML
    private void showGalleryAction() {
        this.getSelectedItem()
                .filter(select -> MovieLock.getInstance().notInList(select.getValue()))
                .filter(DirectoryEntry::isDirectory)
                .peek(this.exeShowGallery);
    }

    private void enterFolderAction() {
        this.getSelectedItem()
                .filter(DirectoryEntry::isDirectory)
                .filter(select -> MovieLock.getInstance().notInList(select.getValue()))
                .peek(this::exeEnterFolder);
    }

    private void openFileAction() {
        this.getSelectedItem()
                .filter(Predicate.not(DirectoryEntry::isDirectory))
                .peek(this.exeOpenFile);
    }

    private void doubleClickListAction() {
        this.getSelectedItem()
                .filter(select -> MovieLock.getInstance().notInList(select.getValue()))
                .peek(select -> {
                    if (select.isDirectory()) {
                        if (select.isGalleryFolder()) {
                            this.exeShowGallery.accept(select);
                        } else {
                            this.exeEnterFolder(select);
                        }
                    } else {
                        this.exeOpenFile.accept(select);
                    }
                });
    }

    //-------------------------------------------------------------------------------------------------------------------------

    @Override
    public void registerListener() {
        this.registerListener(EVENT_FOCUS, e -> this.requestFocus());
        this.registerListener(EVENT_DIRECTORY_SYSTEM_UPDATE, e -> this.updateFileList(e.getParam()));
        this.registerListener(EVENT_CUSTOM_FLAG_EDIT, e -> this.editCustomFlagAction());
        //Systems.getDirectorySystem().subscribe(this.getClass().getSimpleName(), this::updateFileList);
        // Systems.getDirectorySystem().requestFullSync(this::updateListView);
    }

    public void shutdown() {
        //Systems.getDirectorySystem().unSubscribe(this.getClass().getSimpleName());
    }

    private void editCustomFlagAction() {
        var mv = this.getSelectedItem().flatMap(DirectoryEntry::getMovieFolder);
        if (mv.isDefined()) {
            JFXRadioButton isHd = new JFXRadioButton("HD     ");
            JFXRadioButton isWater = new JFXRadioButton("Watermark");
            mv.flatMap(MovieFolder::getCustomFlag).peek(t -> isHd.setSelected(t._1)).peek(t -> isWater.setSelected(t._2));
            DialogBuilder.create()
                    .heading(new Text("[ Custom Flag ]"))
                    .body(new HBox(isHd, isWater))
                    .buttonSaveCancel(() -> mv.peek(m -> m.saveCustomFlag(isHd.isSelected(), isWater.isSelected())))
                    .build().show();
        }
    }


    private void updateFileList(@NotNull DirectorySystem.UpdateData updateData) {

        if (updateData.getType() == DirectorySystem.UpdateType.FULL) {
            var currentSelected = this.getSelectedItem();

            this.listViewItem.clear();
            this.listViewItem.addAll(updateData.getList());
            this.listViewSelectByIndex(updateData.getPos());

            updateData.getRunAfter().getOrElse(() -> currentSelected
                    .peek(this::listViewSelectByEntry)).run();

            this.spFilterMenu.getItems().clear();

        } else if (updateData.getType() == DirectorySystem.UpdateType.MODIFICATION) {
            int index = this.listViewItem.indexOf(updateData.getOldD());
            if (index > -1) {
                this.listViewItem.set(index, updateData.getNewD());
            }
        } else if (updateData.getType() == DirectorySystem.UpdateType.DELETION) {
            GUICommon.debugMessage("DirectorySystem.UpdateType.DELETION");
            this.listViewItem.remove(updateData.getOldD());
        }
    }

    private void listViewSelectByEntry(DirectoryEntry entry) {
        this.nextScrollTo = true;
        this.listView.getSelectionModel().select(entry);
    }

    private void listViewSelectByIndex(int index) {
        if (index > -1) {
            int viewIndex = this.listViewFilteredItem.getViewIndex(index);
            if (viewIndex > -1) {
                this.nextScrollTo = true;
                this.listView.getSelectionModel().select(viewIndex);
            }
        }
    }

    private void listViewSelectByPath(Path path) {
        int idx = Systems.getDirectorySystem().getIndexOfDirectoryEntry(path);
        this.listViewSelectByIndex(idx);
    }

    private Option<Runnable> getSelectEntryRunnable(DirectoryEntry entry) {
        return (entry == null) ? Option.none() : Option.of(() -> this.listViewSelectByEntry(entry));
    }

    public Option<Runnable> getSelectEntryRunnable(Path path) {
        return (path == null) ? Option.none() : Option.of(() -> this.listViewSelectByPath(path));
    }

    private void exeEditMaker(@NotNull DirectoryEntry maker) {
        FXMakerEditDialog.show(maker.getMakerData(), maker.getValue());
    }

    private void exeEnterFolder(DirectoryEntry parent) {
        Systems.getDirectorySystem().enterChild(parent, Option.of(this::clearFilterAction));
    }

    private final Consumer<DirectoryEntry> exeShowGallery = FXImageViewerWindow::show;
    private final Consumer<DirectoryEntry> exeOpenFile = FXOpenFileAction::run;

    private void exeMoveAction(DirectoryEntry source) {
        FXMoveMappingDialog.show(null, source, this::exeMoveAction);
    }

    private void exeMoveAction(@NotNull DirectoryEntry sourceEntry, @NotNull Path destPath) {
        Path sourcePath = sourceEntry.getValue();
        boolean sameRoot = sourcePath.getRoot().equals(destPath.getRoot());

        JFXTextField t1 = new JFXTextField("F - " + sourcePath.toString());
        JFXTextField t2 = new JFXTextField("T - " + destPath.toString());
        Text t3 = new Text("");
        Text t4 = new Text();

        if (!sameRoot) {
            t4.setText("Warning : Source and Target is not on the same drive, move take times!");
        }

        t1.setEditable(false);
        t2.setEditable(false);

        VBox vBox = GUICommon.vbox(15, t1, t2, new Separator(), t3, t4);
        vBox.setMinWidth(800);

        Consumer<DirectorySystem> delVirtualDir = (ds) -> {
            /* delete existing virtual directory if exist */
            Path possibleVirtualDirectory = destPath.resolveSibling(destPath.getFileName().toString() + " V");
            if (Files.exists(possibleVirtualDirectory)) {
                ds.getDirectoryEntryFromPath(possibleVirtualDirectory)
                        .peek(ds::deleteDirectoryAndContents);
            }
        };

        Consumer<DirectorySystem> askSwitchDir = (ds) -> DialogBuilder.create()
                .heading("[ Confirm ]")
                .body(new Text("Move Completed. Change to new directory ?"))
                .overlayClose(false)
                .buttonYesNo(() -> {
                    this.clearFilterAction();
                    ds.changePathTo(destPath.getParent(), this.getSelectEntryRunnable(destPath));
                })
                .build().show();

        Consumer<DirectorySystem> runWhenSuccess = (ds) -> {
            if (ds.parentIsCurrentPath(destPath)) {
                ds.chainPushFull(this.getSelectEntryRunnable(destPath));
            } else {
                delVirtualDir.accept(ds);
                askSwitchDir.accept(ds);
            }
        };

        Runnable moveAction = () -> {
            this.fireEvent(EVENT_LIST_SELECTED, null);
            Mono<DirectorySystem> runMono = Systems.getDirectorySystem().chainCreateDirectory(destPath.getParent());

            if (sameRoot) {
                runMono = runMono.then(Systems.getDirectorySystem().chainRenameFile(sourceEntry, destPath));
            } else {
                runMono = runMono
                        .doFirst(() -> {
                            MovieLock.getInstance().addToList(sourcePath);
                            this.fireEvent(FXTaskBarControl.EVENT_ADD_TASK, "Moving File ...");
                            this.spMoveMenu.setVisible(false);
                            this.btnRenameAll.setDisable(true);
                        })
                        .then(Systems.getDirectorySystem().chainCopyDirectory(sourceEntry, destPath))
                        .then(Systems.getDirectorySystem().chainDeleteDirectoryAndContents(sourceEntry))
                        .doFinally((signalType) -> {
                            this.fireEvent(FXTaskBarControl.EVENT_REMOVE_TASK, "Moving File ...");
                            MovieLock.getInstance().removeFromList(sourcePath);
                            this.spMoveMenu.setVisible(true);
                            this.btnRenameAll.setDisable(false);
                        });
            }

            runMono.doOnNext((ds) -> ds.chainLoadNewEntry(destPath))
                    .doOnNext((ds) -> ds.chainRemoveEntry(sourceEntry))
                    .doOnNext((ds) -> ds.chainPushDelete(sourceEntry))
                    .subscribeOn(ExecuteSystem.get().getIoScheduler())
                    .subscribe(runWhenSuccess);
        };

        DialogBuilder.create()
                .heading("[ Task ]")
                .body(vBox)
                .buttonOkCancel(moveAction)
                .build().show();
    }

    private void executeRenameAction(@NotNull DirectoryEntry selectedEntry) {

        Path selectedPath = selectedEntry.getValue();

        JFXTextField txtInput = GUICommon.textField(selectedPath.getFileName().toString(), 550);

        HBox hBox = GUICommon.hbox(15, txtInput);
        hBox.setMinWidth(750);
        hBox.setAlignment(Pos.CENTER);

        if (selectedEntry.hasNfo() && selectedEntry.isDirectory()) {

            JFXButton btnEdit = FXFactory.buttonWithBorder("Setting", e -> {
                FXRenameFormatEditor.show(FXRenameFormatEditor.Type.DIRECTORY_NAME);
            });
            hBox.getChildren().add(btnEdit);

            JFXButton btnAuto = FXFactory.buttonWithBorder("Auto", e -> {
                txtInput.setText(FXMoveMappingDialog.getSuitableFolderName(selectedEntry));
            });
            hBox.getChildren().add(btnAuto);
        }

        Runnable rename = () -> {
            Path newPath = selectedPath.resolveSibling(txtInput.getText().replace(":", " "));

            if (Files.notExists(newPath)) {
                Systems.getDirectorySystem().chainRenameFile(selectedEntry, newPath)
                        .doOnNext(ds -> ds.chainReplaceEntry(selectedEntry, newPath))
                        .doOnNext(ds -> ds.chainPushFull(this.getSelectEntryRunnable(newPath)))
                        //.doOnNext(ds -> ds.reloadDirectory(this.getSelectEntryRunnable(newPath)))
                        .subscribeOn(ExecuteSystem.get().getIoScheduler())
                        .subscribe();
            } else {
                GUICommon.errorDialog("Already Exist!");
            }
        };

        DialogBuilder.create()
                .heading("[ Rename to ]")
                .body(hBox)
                .buttonOkCancel(rename)
                .build().show();
    }

    private void executeCreateDirectoryAction(@NotNull Path parentDirectoryPath, @NotNull Option<DirectoryEntry> directoryEntry) {
        JFXTextField textField = GUICommon.textField("NEW FOLDER", 150);

        // Auto gen new folder name according to selected movie
        directoryEntry
                .flatMap(DirectoryEntry::getMovieDataOption)
                .map(MovieV2::getMovieID)
                .map(strId -> {
                    String[] parts = strId.split("(?=\\d+$)", 2);
                    final int L = parts[1].length();
                    final int num = Integer.parseInt(parts[1]) + 1;
                    return parts[0] + String.format("%0" + L + "d", num);
                })
                .peek(textField::setText);

        Runnable createDir = () -> {
            Path newDirectoryPath = parentDirectoryPath.resolve(textField.getText());
            Systems.getDirectorySystem().chainCreateDirectory(newDirectoryPath)
                    .doOnNext(ds -> ds.chainLoadNewEntry(Systems.getDirectorySystem().getCurrentEntry(), newDirectoryPath))
                    .doOnNext(ds -> ds.chainPushFull(this.getSelectEntryRunnable(newDirectoryPath)))
                    .subscribeOn(ExecuteSystem.get().getIoScheduler())
                    .subscribe();
        };

        DialogBuilder.create()
                .heading("[ Directory Naming ]")
                .body(textField)
                .buttonOkCancel(createDir)
                .build().show();
    }

    private void executeCreateMultiDirectoryAction(@NotNull Path parentDirectoryPath, @NotNull Option<DirectoryEntry> directoryEntry) {
        TextField textFieldS = GUICommon.textFieldWithBorder("", 50);
        textFieldS.setAlignment(Pos.CENTER);

        IntegerProperty fromNum = new SimpleIntegerProperty();
        IntegerProperty toNum = new SimpleIntegerProperty();

        final int numLen = directoryEntry
                .flatMap(DirectoryEntry::getMovieDataOption)
                .map(MovieV2::getMovieID)
                .map(ID::new)
                .map(movieID -> {
                    final int L = movieID.getSeriesNum().length();
                    final int num = Integer.parseInt(movieID.getSeriesNum()) + 1;
                    textFieldS.setText(movieID.getSeriesCode());
                    fromNum.setValue(num);
                    toNum.setValue(num + 1);
                    return L;
                })
                .getOrElse(0);

        if (numLen > 0) {
            Function<Integer, String> intToString = (n) -> String.format("%0" + numLen + "d", n);

            TextField textFieldF = GUICommon.textFieldWithBorder("", 50);
            TextField textFieldT = GUICommon.textFieldWithBorder("", 50);
            textFieldF.setAlignment(Pos.CENTER);
            textFieldT.setAlignment(Pos.CENTER);
            textFieldF.setEditable(false);
            textFieldT.setEditable(false);
            textFieldF.textProperty().bind(Bindings.createStringBinding(() -> intToString.apply(fromNum.get()), fromNum));
            textFieldT.textProperty().bind(Bindings.createStringBinding(() -> intToString.apply(toNum.get()), toNum));

            JFXButton b1 = FXFactory.buttonWithBorder("Single", (e) -> toNum.setValue(fromNum.get()));
            JFXButton b2 = FXFactory.buttonWithBorder("2", (e) -> toNum.setValue(fromNum.get() + 1));
            JFXButton b3 = FXFactory.buttonWithBorder("3", (e) -> toNum.setValue(fromNum.get() + 2));
            JFXButton b5 = FXFactory.buttonWithBorder("5", (e) -> toNum.setValue(fromNum.get() + 4));
            JFXButton bAdd = FXFactory.buttonWithBorder("+", (e) -> toNum.setValue(toNum.get() + 1));
            JFXButton bSub = FXFactory.buttonWithBorder("-", (e) -> {
                if (toNum.get() > fromNum.get()) {
                    toNum.setValue(toNum.get() - 1);
                }
            });
            JFXButton bFree = FXFactory.buttonWithBorder("Custom", (e) -> this.executeCreateDirectoryAction(parentDirectoryPath, directoryEntry));

            HBox hBox1 = FXFactory.hbox(10, textFieldS, new Text(" - "), textFieldF, new Text(" <-> "), textFieldT);
            HBox hBox2 = FXFactory.hbox(10, b1, b2, b3, b5, bAdd, bSub, bFree);
            hBox1.setAlignment(Pos.CENTER);
            hBox2.setAlignment(Pos.CENTER);
            VBox vBox = FXFactory.vbox(20, hBox1, hBox2);

            Runnable createMultiDir = () -> {
                Mono<DirectorySystem> runMono = Mono.empty();

                for (int i = fromNum.get(); i <= toNum.get(); i++) {
                    Path newDirectoryPath = parentDirectoryPath.resolve(textFieldS.getText() + "-" + intToString.apply(i));

                    runMono = runMono.then(Systems.getDirectorySystem().chainCreateDirectory(newDirectoryPath))
                            .doOnNext(ds -> ds.chainLoadNewEntry(Systems.getDirectorySystem().getCurrentEntry(), newDirectoryPath));

                    if (i == toNum.get()) {
                        runMono.doOnNext(ds -> ds.chainPushFull(this.getSelectEntryRunnable(newDirectoryPath)))
                                .subscribeOn(ExecuteSystem.get().getIoScheduler())
                                .subscribe();
                    }
                }
            };

            DialogBuilder.create()
                    .heading("[ Directory Naming ]")
                    .body(vBox)
                    .buttonOkCancel(createMultiDir)
                    .build().show();
        } else {
            this.executeCreateDirectoryAction(parentDirectoryPath, directoryEntry);
        }
    }

    private void executePackDirectoryAction(@NotNull DirectoryEntry needRenameEntry) {
        if (!needRenameEntry.isDirectory()) {
            String dirName = needRenameEntry.getValue().getFileName().toString();
           /* dirName = dirName.replace("[Thz.la]", "");
            dirName = dirName.replace("[44x.me]", "");
            dirName = dirName.replace("[7sht.me]", "");
            dirName = dirName.replace("[99u.me]", "");
            dirName = dirName.replace("[88q.me]", "");
*/
            int i = dirName.lastIndexOf(".");
            dirName = dirName.substring(0, i);

            String newName = dirName.replaceAll("^\\[.+\\]", "");
            if (!newName.equals("")) {
                dirName = newName;
            }

            dirName = dirName.replaceAll("^HD-", "");
            dirName = dirName.replaceAll(".HD$", "");
            dirName = dirName.replaceAll("_[0-9]$", "");

            JFXTextField textField = GUICommon.textField(dirName.trim(), 150);

            Runnable pack = () -> {
                Path newDirectoryPath = needRenameEntry.getValue().getParent().resolve(textField.getText());
                Path newFilePath = newDirectoryPath.resolve(needRenameEntry.getValue().getFileName());

                Systems.getDirectorySystem().chainCreateDirectory(newDirectoryPath)
                        .then(Systems.getDirectorySystem().chainRenameFile(needRenameEntry, newFilePath))
                        .doOnNext(ds -> ds.chainReplaceEntry(needRenameEntry, newDirectoryPath))
                        .doOnNext(ds -> ds.chainPushFull(this.getSelectEntryRunnable(newDirectoryPath)))
                        .subscribeOn(ExecuteSystem.get().getIoScheduler())
                        .subscribe();
            };

            DialogBuilder.create()
                    .heading("[ Directory Naming ]")
                    .body(textField)
                    .buttonOkCancel(pack)
                    .build().show();
        }
    }

    private void executeDelAction(int index) {
        Runnable delAll = () -> Systems.getDirectorySystem().deleteDirectoryAndContents(index)
                .subscribeOn(ExecuteSystem.get().getIoScheduler())
                .subscribe();

        Runnable askForDelAll = () -> DialogBuilder.create()
                .heading("[ Confirm ]")
                .body("Directory is not Empty, Continue?")
                .overlayClose(false)
                .buttonYesNo(delAll)
                .build().show();

        Runnable delSingle = () -> Systems.getDirectorySystem().deleteFile(index, Option.of(askForDelAll))
                .subscribeOn(ExecuteSystem.get().getIoScheduler())
                .subscribe();

        DialogBuilder.create()
                .heading("[ Confirm ]")
                .body("Are you sure you want to Delete this file?")
                .overlayClose(false)
                .buttonYesNo(delSingle)
                .build().show();
    }

    private void changeInSelection(DirectoryEntry new_val) {

        this.fireEvent(EVENT_LIST_SELECTED, new_val);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.listView = new FXListView<>();
        this.listView.setBorder(false);
        this.listView.disableHorizontalScroll();
        this.listView.fitToPane(this.listViewPane);

        this.listView.setCellFactory((ListView<DirectoryEntry> l) -> new FileListCell(this.menuFolder, this.menuFile));

        this.selectedEntry.bind(this.listView.getSelectionModel().selectedItemProperty());
        this.selectedEntry.addListener((ov, old_val, new_val) -> this.changeInSelection(new_val));

        this.selectedIndex.bind(this.listView.getSelectionModel().selectedIndexProperty());
        this.selectedIndex.addListener((ov, o, n) -> {
            if (this.nextScrollTo) {
                this.nextScrollTo = false;
                GUICommon.debugMessage("-- scroll to : " + n.intValue());
                if (n.intValue() != -1) {
                    this.listView.scrollTo(n.intValue());
                }
            }
        });

        this.listView.setOnMouseClicked((event) -> {
            if (event.getClickCount() == 2) {
                //GUICommon.debugMessage(event.getSource().toString());
                this.doubleClickListAction();
            }
        });

        this.listView.setItems(this.listViewFilteredItem);

        MenuItem b1 = this.genChangePathButton("AV", GuiSettings.Key.avDirectory);
        MenuItem b2 = this.genChangePathButton("Doujinshi", GuiSettings.Key.doujinshiDirectory);
        MenuItem b3 = this.genChangePathButton("Comic", GuiSettings.Key.mangaDirectory);
        MenuItem b4 = this.genChangePathButton("Photo", GuiSettings.Key.photoDirectory);
        MenuItem b5 = this.genChangePathButton("Cosplay", GuiSettings.Key.cosplayDirectory);
        MenuItem b6 = this.genChangePathButton("Download", GuiSettings.Key.downloadDirectory);
        this.spShortcutMenu.getItems().setAll(b1, b2, b3, b4, b5, b6);

        MenuItem menuSearchID = FXFactory.menuItem("By ID", this::searchAction);
        MenuItem menuSearchActor = FXFactory.menuItem("By Actor", this::searchActorAction);
        this.spSearchMenu.getItems().setAll(menuSearchID, menuSearchActor);

        MenuItem menuRename = FXFactory.menuItem("Rename", this::renameAction);
        MenuItem menuDelete = FXFactory.menuItem("Delete", this::delAction);
        MenuItem menuGallery = FXFactory.menuItem("Show as Gallery", this::showGalleryAction);

        MenuItem menuPack = FXFactory.menuItem("Pack in Folder", this::packDirectoryAction);
        MenuItem menuToMovieDB = FXFactory.menuItem("To Movie Database", this::moveAction);
        MenuItem menuToNeedProcess = FXFactory.menuItem("To Need Process", this::markAsNeedProcess);
        MenuItem menuEditMaker = FXFactory.menuItem("Edit Maker Info", this::editMakerAction);

        menuEditMaker.visibleProperty().bind(Bindings.createBooleanBinding(() -> this.selectedEntry.get() != null && this.selectedEntry.get().getMakerData().isDefined(), this.selectedEntry));

        menuPack.disableProperty().bind(Bindings.createBooleanBinding(() -> this.selectedEntry.get() == null || this.selectedEntry.get().isDirectory(), this.selectedEntry));
        menuToMovieDB.disableProperty().bind(menuPack.disableProperty().not());
        menuToNeedProcess.disableProperty().bind(menuToMovieDB.disableProperty());
        menuGallery.disableProperty().bind(menuToMovieDB.disableProperty());

        this.spMoveMenu.disableProperty().bind(this.selectedEntry.isNull());
        this.spMoveMenu.getItems().setAll(menuRename, menuDelete, menuGallery, menuPack, menuToMovieDB, menuToNeedProcess, menuEditMaker);

        this.spFilterMenu.textProperty().bind(this.filter);
        this.spFilterMenu.setOnShown((e) -> {
            if (this.spFilterMenu.getItems().size() == 0) {
                this.updateFilterItem();
            }
        });
    }
}
