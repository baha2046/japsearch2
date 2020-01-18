package org.nagoya.controller.mangaparsingprofile;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controls.FXImageViewerWindow;
import org.nagoya.controls.FXWebViewWindow;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;
import org.nagoya.view.customcell.SelectorPathListTreeCell;
import org.nagoya.view.dialog.FXProgressDialog;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;

public class BigirlParser extends BaseParser {

    @Override
    public void loadSet(String fromURL) {
        this.setTypeString(T_Images);
        this.setTypePath(this.getTypePath().resolve("Twitter"));

        JFXTextField txtFUrl = GUICommon.textField(fromURL, 350);
        var txtInUrl = this.getPasteButton(txtFUrl);

        JFXTextField txtUrl = GUICommon.textField("", 450);
        JFXTextField txtName = GUICommon.textField("", 350);
        var txtInName = this.getPasteButton(txtName);

        DatePicker datePicker = new DatePicker(LocalDate.parse("1980-01-01"));

        JFXButton btnUseUrl = GUICommon.buttonWithBorder(" Use ", (e) -> {
            txtUrl.setText(txtFUrl.getText());
        });

        JFXButton btnOpenUrl = GUICommon.buttonWithBorder(" Open Link ", (e) -> {
            FXWebViewWindow.show(txtFUrl.getText(), Option.none(), Option.none());
        });

        Consumer<UpdateInfo> infoConsumer = info -> {
            txtUrl.setText(info.getUrl());
            datePicker.setValue(LocalDate.parse(info.getLastUpdate()));
        };

        JFXButton btnCheck = GUICommon.buttonWithBorder(" Check Folder ", (e) -> {
            if (!txtName.getText().isEmpty()) {
                txtName.setText(txtName.getText().replace("のTwitter", ""));
                Path targetPath = this.getTypePath().resolve(txtName.getText());
                UpdateInfo.readInfo(targetPath).peek(infoConsumer).onEmpty(() -> {
                    datePicker.setValue(LocalDate.parse("1980-01-01"));
                    int v1 = txtName.getText().indexOf("@");
                    int v2 = txtName.getText().lastIndexOf(")");
                    if (v1 > 0 && v2 > v1) {
                        txtUrl.setText("https://bi-girl.net/" + txtName.getText().substring(v1 + 1, v2));
                    }
                });
            }
        });

        Vector<Tuple2<String, Consumer<Path>>> menu = Vector.of(
                Tuple.of("Update", path -> {
                    txtName.setText(path.getFileName().toString());
                    UpdateInfo.readInfo(path).peek(infoConsumer);
                }),
                Tuple.of("View", path -> FXImageViewerWindow.show(DirectoryEntry.of(path)))
        );

        this.getTreeView().setCellFactory((TreeView<Path> l) -> SelectorPathListTreeCell.createCell(menu));

        this.updateTreeView();

        JFXButton btnDown = GUICommon.buttonWithBorder(" Download ", (e) -> {
            if (!txtName.getText().isEmpty() && !txtUrl.getText().isEmpty()) {
                Path targetPath = this.getTypePath().resolve(txtName.getText());
                this.createModelDir(targetPath);
                this.updateTreeView();

                UpdateInfo info = UpdateInfo.readInfo(targetPath)
                        .getOrElse(new UpdateInfo(txtUrl.getText(), datePicker.getValue().toString()));

                this.loadSet(targetPath, info);
            }
        });

        btnDown.disableProperty().bind(txtName.textProperty().isNotEmpty().and(txtUrl.textProperty().isNotEmpty()).not());

        VBox vBox = GUICommon.vbox(15,
                txtInUrl, GUICommon.hbox(15, btnOpenUrl, btnUseUrl),
                new Separator(), txtUrl, txtInName, btnCheck, datePicker,
                new Separator(), btnDown);
        vBox.setAlignment(Pos.CENTER_LEFT);

        HBox hBox = GUICommon.hbox(15, this.treeView, vBox);
        hBox.setAlignment(Pos.CENTER);
        hBox.setPadding(FXWindow.getDefaultInset());
        hBox.setPrefSize(1000, 400);

        WindowBuilder.create()
                .title("bi-girl.net Downloader", true)
                .body(hBox)
                .buildSingle().show();
    }

    private void loadSet(Path path, @NotNull UpdateInfo info) {
        String url = this.removeTail(info.getUrl());
        Option<Document> document = this.downloadDocument(url);

        if (document.isEmpty()) {
            return;
        }

        int maxPage = document
                .map(e -> e.selectFirst("div[class=current_page]"))
                .filter(Objects::nonNull)
                .map(Element::text)
                .peek(GUICommon::debugMessage)
                .map(this::getMaxPage)
                .getOrElse(0);

        Stream<String> pageList = Stream.of(info.getUrl());

        for (int p = 2; p <= maxPage; p++) {
            pageList = pageList.append(info.getUrl() + "/page/" + p);
        }

        Stream<String> finalPageList = pageList;

        var observableList = FXProgressDialog.getInstance().startProgressDialog();

        ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> {
            var result = finalPageList.map((u) -> this.loadPage(u, path, LocalDate.parse(info.getLastUpdate()), observableList));
            String lastUpdate = result.map(t -> t._1).sorted().last();
            Stream<Tuple2<String, File>> downloadStream = result.flatMap(t -> t._2).distinctBy(t -> t._1);

            GUICommon.writeToObList("Image to download : " + downloadStream.length(), observableList);

            this.batchDownloadUrlToFile(downloadStream);

            info.setUrl(url);
            info.setLastUpdate(lastUpdate);
            UpdateInfo.writeInfo(info, path);
        });
    }

    private int getMaxPage(@NotNull String str) {
        int x = str.indexOf("/");
        int y = str.indexOf(" Pages");
        if (x == -1 || y == -1) {
            return 0;
        }

        return Integer.parseInt(str.substring(x + 2, y));
    }

    @NotNull
    private Tuple2<String, Stream<Tuple2<String, File>>> loadPage(String fromURL, Path path, LocalDate dateAfter, Option<ObservableList<String>> observableList) {
        GUICommon.writeToObList(fromURL, observableList);

        Option<Document> document = this.downloadDocument(fromURL);

        if (document.isEmpty()) {
            return Tuple.of("", Stream.empty());
        }

        Stream<Element> elementList = Stream.ofAll(document.get().select("div[class^=img_wrapper]").stream());

        Stream<String> dateList = elementList.map(s -> s.selectFirst("div[class=img_footer] span"))
                .filter(Objects::nonNull)
                .map(Element::text)
                .map(s -> s.replace("/", "-"));

        Stream<String> imgList = elementList.map(s -> s.selectFirst("a"))
                .filter(Objects::nonNull)
                .map(i -> i.attr("href"))
                //.peek(GUICommon::debugMessage)
                ;

        Stream<Tuple2<String, File>> fileList = this.buildFileList(imgList, dateList, dateAfter, path);

        //GUICommon.writeToObList("Parse Done", observableList);

        return Tuple.of(dateList.sorted().last(), fileList);
    }

    private Stream<Tuple2<String, File>> buildFileList(@NotNull Stream<String> imgList, Stream<String> dateList, LocalDate dateAfter, Path path) {
        return imgList.map(url -> Tuple.of(url, FilenameUtils.getName(url)))
                .map(t -> t.map2(this::removeTail))
                .zipWithIndex((t, i) -> this.filterDate(dateList.get(i), dateAfter, t._1, t._2))
                .filter(Objects::nonNull)
                .peek(t -> GUICommon.debugMessage(t.toString()))
                .map(t -> t.map2(path::resolve).map2(Path::toFile))
                ;
    }

    @Nullable
    private Tuple2<String, String> filterDate(String dateString, LocalDate dateAfter, String urlString, String fileString) {
        LocalDate fileDate = LocalDate.parse(dateString);
        //GUICommon.debugMessage(fileDate.toString() + " - " + dateAfter.toString());
        if (fileDate.isAfter(dateAfter)) {
            return Tuple.of(urlString, dateString + " " + fileString);
        } else {
            return null;
        }
    }

    @NotNull
    private String removeTail(@NotNull String inStr) {
        int i = inStr.indexOf("?");
        inStr = i > 0 ? inStr.substring(0, i) : inStr;
        i = inStr.indexOf("/page");
        return i > 0 ? inStr.substring(0, i) : inStr;
    }

    private void createModelDir(Path path) {
        if (Files.notExists(path)) {
            UtilCommon.tryCreateDirectory(path);
        }
    }
}
