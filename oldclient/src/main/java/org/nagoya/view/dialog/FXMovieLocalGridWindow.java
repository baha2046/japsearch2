package org.nagoya.view.dialog;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controller.ScrapeMovieAction;
import org.nagoya.controller.siteparsingprofile.specific.JavBusParsingProfile;
import org.nagoya.controls.FXWebViewControl;
import org.nagoya.controls.FXWebViewWindow;
import org.nagoya.io.Setting;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.model.MovieV2;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.model.dataitem.ID;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class FXMovieLocalGridWindow extends FXMovieGridWindow {
    private static FXMovieDirectoryGridWindow directoryGridWindow = null;

    private static FXMovieDirectoryGridWindow getDirectoryGridWindow() {
        if (directoryGridWindow == null) {
            directoryGridWindow = new FXMovieDirectoryGridWindow();
        }
        return directoryGridWindow;
    }

    public static void show() {
        getDirectoryGridWindow().displayWindow("Movie List");
        getDirectoryGridWindow().setRoot(Systems.getDirectorySystem().getCurrentEntry());
    }

    final TextField txtSelected;
    Option<DirectoryEntry> selectedEntry = Option.none();
    Option<SimpleMovieData> selectedMovie = Option.none();
    final JavBusParsingProfile javBusParsingProfile;
    JFXButton btnBrowse;
    JFXButton btnRemove;
    Path path;

    FXMovieLocalGridWindow() {
        super();
        this.txtSelected = GUICommon.textFieldWithBorder("", 250);
        this.txtSelected.setAlignment(Pos.CENTER);

        this.javBusParsingProfile = new JavBusParsingProfile();


        ChangeListener<SimpleMovieData> movieDataConsumer = (ov, o, n) -> {
            this.selectedMovie = Option.of(n);
            this.selectedEntry = this.selectedMovie.flatMap(SimpleMovieData::getDirectoryEntry);
            this.txtSelected.setText(this.selectedMovie.map(SimpleMovieData::getStrId).getOrElse(""));
        };
        //this.listView.setCellFactory((GridView<SimpleMovieData> l) -> new MovieDBListSimpleGridCell(this.listView));
        this.gridView.selectionProperty().addListener(movieDataConsumer);

        this.btnBrowse = GUICommon.buttonWithBorder("  Browse  ", (e) -> {
            String movieUrl = this.selectedMovie.map(SimpleMovieData::getStrUrl).getOrElse("");
            if (!movieUrl.equals("")) {
                FXWebViewWindow.show(movieUrl, Option.none(), Option.of(FXWebViewControl::modifyJavBus));
            }
        });

        this.btnRemove = GUICommon.buttonWithBorder("Remove", (e) -> this.removeMovie());
    }

    protected void setPath(Path path) {
        this.path = path;
    }

    protected void removeMovie() {
        this.selectedMovie.map(this.fullData::remove)
                .peek(d -> this.fullData = d)
                .peek(e -> this.reloadData());
    }
}

class FXMovieDirectoryGridWindow extends FXMovieLocalGridWindow {
    private Map<String, Stream<String>> seriesMap;

    FXMovieDirectoryGridWindow() {
        super();
        this.seriesMap = new HashMap<>();

        JFXButton btnOpenSeries = GUICommon.buttonWithBorder("Open Series", (e) -> this.openSeries());

        this.extraBar.getChildren().addAll(this.txtSelected, this.btnBrowse, this.btnRemove, btnOpenSeries);
    }

    void setRoot(@NotNull DirectoryEntry root) {
        this.setPath(root.getValue());
        this.setSeries(root.getChildrenEntry());
        this.setData(root.getChildrenEntry().flatMap(DirectoryEntry::toSimpleMovie), 0);
    }

    private void setSeries(@NotNull Vector<DirectoryEntry> dirEntry) {
        // List<String> seriesList = new ArrayList<>();
        this.seriesMap = new HashMap<>();

        dirEntry.forEach(entry -> {
            var movieID = entry.getMovieDataOption().map(MovieV2::getMovieID).map(ID::new);
            if (movieID.isDefined()) {
                String s = movieID.getOrNull().getSeriesCode();
                String n = movieID.getOrNull().getSeriesNum();
                if (this.seriesMap.get(s) == null) {
                    this.seriesMap.put(s, Stream.of(n));
                } else {
                    var newList = this.seriesMap.get(s).append(n);
                    this.seriesMap.replace(s, newList);
                }
            }
        });
    }

    private void openSeries() {
        VBox vBox = GUICommon.vbox(10);
        ToggleGroup group = new ToggleGroup();
        this.seriesMap.forEach((s, l) -> {
            GUICommon.debugMessage(s);
            JFXRadioButton button = new JFXRadioButton(s + " (" + l.length() + ")");
            if (FXMovieSeriesGridWindow.isDataExist(FXMovieSeriesGridWindow.getDataPath(this.path, s))) {
                button.setText(button.getText() + " [Data Exist]");
            }
            button.setUserData((Runnable) () -> {
                FXMovieSeriesGridWindow window = new FXMovieSeriesGridWindow(s);
                window.displayWindow();
                window.setPath(this.path);
                window.load(this.fullData);
            });
            button.setToggleGroup(group);
            vBox.getChildren().add(button);
        });

        DialogBuilder.create()
                .body(vBox)
                .buttonOkCancel(() -> {
                    Runnable runnable = (Runnable) group.getSelectedToggle().getUserData();
                    runnable.run();
                })
                .container(this.fxWindow.getRoot())
                .build().show();
    }
}

class FXMovieSeriesGridWindow extends FXMovieLocalGridWindow {

    public static String fileName = "-list.ini";

    @NotNull
    public static Path getDataPath(@NotNull Path path, String seriesString) {
        return path.resolve(seriesString + fileName);
    }

    public static boolean isDataExist(@NotNull Path dataPath) {
        return Files.exists(dataPath) && Files.isRegularFile(dataPath);
    }

    public static Vector<SimpleMovieData> loadData(Path dataPath) {
        if (isDataExist(dataPath)) {
            return Setting.readSetting(SimpleMovieStore.class, dataPath).getMovieData();
        }
        return Vector.empty();
    }

    public static void saveData(Vector<SimpleMovieData> data, Path dataPath) {
        Setting.writeSetting(new SimpleMovieStore(data), dataPath);
    }

    private final String seriesString;

    FXMovieSeriesGridWindow(String seriesString) {
        super();

        this.seriesString = seriesString;

        JFXButton btnBlackList = GUICommon.buttonWithBorder("Black List", (e) -> this.blackList());
        JFXButton btnGenAll = GUICommon.buttonWithBorder("Gen All", (e) -> this.genMovieAll());
        JFXButton btnSave = GUICommon.buttonWithBorder("Save List", (e) -> this.save());

        this.extraBar.getChildren().setAll(this.txtSelected, this.btnBrowse, this.btnRemove, btnBlackList, btnGenAll, btnSave);
    }

    void load(@NotNull Vector<SimpleMovieData> movieDataList) {

        Path listPath = getDataPath(this.path, this.seriesString);

        Vector<SimpleMovieData> data = movieDataList.filter(v -> v.getStrId().startsWith(this.seriesString));
        data = data.appendAll(loadData(listPath).filter(m -> !movieDataList.exists(v -> v.getStrId().equals(m.getStrId()))));
        this.setData(data.sorted(Comparator.comparing(SimpleMovieData::getStrId)), 0);
    }

    void save() {
        saveData(this.fullData.filter(SimpleMovieData::isTemp), getDataPath(this.path, this.seriesString));
    }

    void blackList() {
        this.selectedMovie.peek(m -> m.setBlackList(!m.isBlackList())).peek(SimpleMovieData::refreshView);
    }

    private void genMovieAll() {
        if (this.fullData.length() < 2) {
            return;
        }

        var s1 = this.fullData.get(0).getStrId();
        var s2 = this.fullData.get(this.fullData.length() - 1).getStrId();

        String[] parts1 = s1.split("(?=\\d+$)", 2);
        String[] parts2 = s2.split("(?=\\d+$)", 2);

        final int L = parts1[1].length();
        final int numS = Integer.parseInt(parts1[1]) + 1;
        final int numE = Integer.parseInt(parts2[1]) - 1;

        JFXTextField t1 = GUICommon.textField("" + numS, 100);
        JFXTextField t2 = GUICommon.textField("" + numE, 100);

        HBox hBox = GUICommon.hbox(15, t1, t2);

        Runnable runnable = () -> {
            var out = FXProgressDialog.getInstance().startProgressDialog();

            int startIdx = Integer.parseInt(t1.getText());
            int endIdx = Integer.parseInt(t2.getText());

            if (parts1[0].equals(parts2[0]) && endIdx >= startIdx) {
                var list = Stream.range(startIdx, endIdx + 1)
                        .map(n -> parts1[0] + String.format("%0" + L + "d", n))
                        .filter(id -> !this.fullData.exists(m -> m.getStrId().equals(id)))
                        .map(id -> ScrapeMovieAction.scrapeMovie(this.javBusParsingProfile, id));
                var mergedList = Flux.fromIterable(list)
                        .flatMapSequential(Function.identity());

                List<SimpleMovieData> newMovieList = new ArrayList<>();

                Runnable run = () -> {
                    GUICommon.writeToObList("Done", out);
                    if (newMovieList.size() > 0) {
                        this.fullData = this.fullData.appendAll(newMovieList).sorted(Comparator.comparing(SimpleMovieData::getStrId));
                        this.save();
                        GUICommon.runOnFx(this::reloadData);
                    }
                };

                mergedList.subscribeOn(ExecuteSystem.get().getNormalScheduler())
                        .subscribe((m) -> {
                            m.find(v -> v.getStrId().startsWith(this.seriesString))
                                    .peek(movie -> {
                                        movie.setTemp(true);
                                        GUICommon.writeToObList(movie.getStrId() + " - " + movie.getStrTitle(), out);
                                        newMovieList.add(movie);
                                    });
                        }, (e) -> {
                        }, run);
            }
        };

        DialogBuilder.create()
                .body(hBox)
                .buttonOkCancel(runnable)
                .container(this.fxWindow.getRoot())
                .build().show();
    }

    void displayWindow() {
        super.displayWindow("Movie : " + this.seriesString);
    }

    static class SimpleMovieStore {
        Vector<SimpleMovieData> movieData;

        SimpleMovieStore(Vector<SimpleMovieData> data) {
            this.movieData = data;
        }

        public Vector<SimpleMovieData> getMovieData() {
            return this.movieData;
        }
    }
}