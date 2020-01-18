package org.nagoya.controls;

import io.vavr.Tuple2;
import io.vavr.collection.Vector;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.model.dataitem.FxThumb;

import java.util.function.Consumer;

public class FXImageGridControl {
    private final int imagePerPage;

    private final ObjectProperty<Vector<FxThumb>> item;
    private final ObjectProperty<Vector<Tuple2<FXScreenImage, Integer>>> displayItem;
    private Vector<Vector<Tuple2<FXScreenImage, Integer>>> pageItem;

    private Consumer<Integer> onImageClick = (i) -> {
    };

    private final FXImageGridViewV2 fxImageGridView;

    public FXImageGridControl(int imagePerPage) {

        this.imagePerPage = imagePerPage;

        this.fxImageGridView = new FXImageGridViewV2(this);

        this.item = new SimpleObjectProperty<>(Vector.empty()) {
            @Override
            protected void invalidated() {
                ///GUICommon.debugMessage("item invalidated");
                FXImageGridControl.this.pageItem = FXImageGridControl.this.genPageList(FXImageGridControl.this.item.get().zipWithIndex());
            }
        };

        this.displayItem = new SimpleObjectProperty<>(Vector.empty()) {
            @Override
            protected void invalidated() {
                //GUICommon.debugMessage("displayItem invalidated " + this.get().length());
                double num = FXImageGridControl.this.displayItem.get().length();
                if (num == 3) {
                    num = 4;
                }
                double cal = Math.sqrt(num);
                cal = cal + (cal / 10);

                int colCount = ((int) cal) + 1;
                int rowCount = (int) Math.ceil(num / colCount);

                FXImageGridControl.this.fxImageGridView.setItem(FXImageGridControl.this.displayItem.get(), colCount, rowCount);
            }
        };
    }

    public GridPane getPane() {
        return this.fxImageGridView;
    }

    public void setOnImageClick(Consumer<Integer> onImageClick) {
        this.onImageClick = onImageClick;
    }

    public Consumer<Integer> getOnImageClick() {
        return this.onImageClick;
    }

    public void setItem(Vector<FxThumb> item) {
        this.item.set(item);
    }

    public void showPage(int pageNum) {
        if (pageNum < 0) {
            pageNum = 0;
        }
        if (pageNum >= this.getPageCount()) {
            pageNum = this.getPageCount() - 1;
        }

        GUICommon.debugMessage("SHOW PAGE " + pageNum);
        //StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        //System.out.println(">>> " + stackTraceElements[3].toString().substring(25) + ": " + "showPage");

        this.displayItem.setValue(this.pageItem.get(pageNum));

        //GUICommon.debugMessage("HBox viewWidth" + this.viewWidth.get());
        //GUICommon.debugMessage("HBox viewHeight" + this.viewHeight.get());
    }

    public int getPageCount() {
        return this.pageItem.length();
    }

    public Vector<FxThumb> getItem() {
        return this.item.get();
    }

    public ObjectProperty<Vector<FxThumb>> itemProperty() {
        return this.item;
    }

    public Vector<Tuple2<FXScreenImage, Integer>> getDisplayItem() {
        return this.displayItem.get();
    }

    public ObjectProperty<Vector<Tuple2<FXScreenImage, Integer>>> displayItemProperty() {
        return this.displayItem;
    }

    public int getImagePerPage() {
        return this.imagePerPage;
    }

    private Vector<Vector<Tuple2<FXScreenImage, Integer>>> genPageList(@NotNull Vector<Tuple2<FxThumb, Integer>> fullList) {
        Vector<Vector<Tuple2<FXScreenImage, Integer>>> pageList = Vector.empty();

        var listUse = fullList.map(t -> t.map1(FXScreenImage::of));

        while (listUse.length() > this.imagePerPage) {
            var tuple2 = listUse.splitAt(this.imagePerPage);
            pageList = pageList.append(tuple2._1);
            listUse = tuple2._2;
        }
        return pageList.append(listUse);
    }
}
