package org.nagoya.view.dialog;

import io.vavr.collection.Vector;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import org.controlsfx.control.GridView;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.controls.FXGridView;
import org.nagoya.controls.FXPagination;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.preferences.CustomOptions;
import org.nagoya.preferences.AppSetting;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;
import org.nagoya.view.customcell.MovieDBListSimpleGridCell;

public class FXMovieGridWindow {
    public static int MAX_PER_PAGE = 48;
    public static DoubleProperty VIEW_WIDTH = new SimpleDoubleProperty(1055);
    public static DoubleProperty VIEW_HEIGHT = new SimpleDoubleProperty(670);

    public static final CustomOptions OPTIONS = getOptions();

    @NotNull
    private static CustomOptions getOptions() {
        String cName = FXMovieGridWindow.class.getSimpleName();
        CustomOptions customOptions = new CustomOptions(cName);
        customOptions.addOption(cName + "-maxMoviePerPage", MAX_PER_PAGE, (b) -> MAX_PER_PAGE = b, "Movie per Page : ");
        customOptions.addRow("");
        customOptions.addOption(cName + "-viewWidth", VIEW_WIDTH, "Width : ");
        customOptions.addOption(cName + "-viewHeight", VIEW_HEIGHT, "Height : ");
        return customOptions;
    }

    protected final ObservableList<SimpleMovieData> movieDataObservableList = FXCollections.observableArrayList(SimpleMovieData.extractor());
    protected final VBox displayPane;
    protected final FXGridView<SimpleMovieData> gridView;
    protected final FXPagination pagination;
    protected HBox extraBar;
    protected Vector<SimpleMovieData> fullData;
    protected int currentPage = 0;

    FXWindow fxWindow;

    FXMovieGridWindow() {
        //currentPage = new SimpleIntegerProperty(0);

        this.gridView = new FXGridView<>();
        this.gridView.setCellHeight(MovieDBListSimpleGridCell.CELL_HEIGHT);
        this.gridView.setCellWidth(MovieDBListSimpleGridCell.CELL_WIDTH);
        this.gridView.setHorizontalCellSpacing(4);
        this.gridView.setVerticalCellSpacing(4);
        this.gridView.setCellFactory((GridView<SimpleMovieData> l) -> new MovieDBListSimpleGridCell(this.gridView));
        this.gridView.setItems(this.movieDataObservableList);

        this.extraBar = GUICommon.hbox(10);
        this.extraBar.setAlignment(Pos.CENTER);

        this.pagination = new FXPagination(20);

        var inset = FXWindow.getDefaultInset();

        VBox.setVgrow(this.gridView, Priority.ALWAYS);
        this.displayPane = new VBox();
        this.displayPane.setSpacing(0);
        this.displayPane.setPadding(inset);
        this.displayPane.setFillWidth(true);
        this.displayPane.setMinSize(600, 400);//Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        //this.displayPane.setPrefSize(VIEW_WIDTH.get() + inset.getLeft() + inset.getRight(),
        //        VIEW_HEIGHT.get() + inset.getTop() + inset.getBottom());
        this.displayPane.getChildren().addAll(this.gridView, this.pagination, this.extraBar);
    }


    public void display() {
        DialogBuilder.create()
                .body(this.displayPane)
                .build().show();
    }

    public void displayWindow(String titleString) {
        this.fxWindow = WindowBuilder.create()
                .style(StageStyle.UTILITY)
                .title(titleString, true)
                .runOnClose(() -> {
                    GUICommon.debugMessage("FXMovieGridWindow Close - " + this.fxWindow.getWidth() + " " + this.fxWindow.getHeight());
                    VIEW_WIDTH.set(this.fxWindow.getWidth());
                    VIEW_HEIGHT.set(this.fxWindow.getHeight());
                    AppSetting.getInstance().saveSetting();
                })
                .body(this.displayPane)
                .prefSize(VIEW_WIDTH.get(), VIEW_HEIGHT.get())
                .resizable(true)
                .buildSingle();
        this.fxWindow.show();

    }

    public void clear() {
        GUICommon.runOnFx(this.movieDataObservableList::clear);
    }

    public void setData(@NotNull Vector<SimpleMovieData> movieDataList, int page) {
        this.currentPage = page;
        this.setData(movieDataList);
    }

    public void setData(@NotNull Vector<SimpleMovieData> movieDataList) {
        this.fullData = movieDataList;
        this.reloadData();
    }

    public void reloadData() {
        var d = this.fullData;

        Vector<Vector<SimpleMovieData>> pageList = Vector.empty();
        while (d.length() > MAX_PER_PAGE) {
            var ds = d.splitAt(MAX_PER_PAGE);
            pageList = pageList.append(ds._1);
            d = ds._2;
        }
        pageList = pageList.append(d);

        Vector<Vector<SimpleMovieData>> finalPageList = pageList;
        this.pagination.setPageCount(pageList.length());
        this.pagination.setPageFactory((i) -> {
            this.currentPage = i;
            this.renderResult(finalPageList.get(i));
        });

        if (this.currentPage > pageList.length() - 1) {
            this.currentPage = pageList.length() - 1;
        }

        this.pagination.setCurrentPageIndex(this.currentPage);
        //this.renderResult(pageList.get(this.currentPage));
    }

    public void renderResult(@NotNull Vector<SimpleMovieData> pageData) {
        this.clear();
        this.showPage(pageData);
    }

    protected void showPage(@NotNull Vector<SimpleMovieData> pageData) {
        GUICommon.runOnFx(() -> {
            this.movieDataObservableList.addAll(pageData.toJavaList());
        });
    }
}
