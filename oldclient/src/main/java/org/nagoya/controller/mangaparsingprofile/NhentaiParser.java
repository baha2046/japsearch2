package org.nagoya.controller.mangaparsingprofile;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.Tuple;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.preferences.RenameSettings;
import org.nagoya.system.dialog.WindowBuilder;
import org.nagoya.view.customcell.SelectorPathListTreeCell;
import org.nagoya.view.editor.FXPathMappingEditor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class NhentaiParser extends BaseParser {

    Stream<String> imageList = Stream.empty();

    public NhentaiParser() {

    }

    @Override
    public void loadSet(String fromURL) {

        JFXTextField txtType = GUICommon.textField("", 220);
        txtType.textProperty().bind(this.typeString);

        JFXTextField txtAuthor = GUICommon.textField("", 220);
        txtAuthor.textProperty().bindBidirectional(this.authorStringProperty());

        JFXTextField txtTitle = GUICommon.textField("", 500);
        txtTitle.textProperty().bindBidirectional(this.titleStringProperty());

        JFXTextField txtAuthorDirectory = GUICommon.textField("", 400);
        txtAuthorDirectory.textProperty().bind(
                Bindings.createStringBinding(() -> this.getAuthorPath() == null ? "" : this.getAuthorPath().toString(), this.authorPathProperty()));
        txtAuthorDirectory.setEditable(false);

        JFXButton btnAdd = GUICommon.buttonWithBorder("Create", e -> {
        });
        btnAdd.setOnAction(e ->
                UtilCommon.tryCreateDirectory(this.getAuthorPath()).onSuccess((p) -> {
                    this.updateTreeView();
                    btnAdd.setDisable(true);
                }));
        btnAdd.disableProperty().bind(Bindings.createBooleanBinding(() -> Files.exists(this.getAuthorPath()), this.authorPathProperty()));

        Consumer<Path> useFolder = this::setAuthorPath;
        Consumer<Path> addNewMapping = path -> {
            if (path != null) {
                String targetPathName = path.getFileName().toString();

                final String newMapping = this.getAuthorString() + " >> " + targetPathName;
                GUICommon.showDialog("Confirm :", new Text("Set [  " + newMapping + "  ] as new mapping ?"), "Cancel",
                        "Okay", () -> {
                            RenameSettings.getInstance().updateRenameMapping(newMapping.replace(" >> ", "|"));
                            RenameSettings.writeSetting();
                            useFolder.accept(path);
                        });
            }
        };
        var treeCellMenu = Vector.of(Tuple.of("Use Folder", useFolder), Tuple.of("Add New Mapping", addNewMapping));
        this.getTreeView().setCellFactory((TreeView<Path> l) -> SelectorPathListTreeCell.createCell(treeCellMenu));
        this.treeView.setPrefHeight(250);

        JFXButton btnMapEditor = GUICommon.buttonWithBorder("Mapping Editor", (e) -> FXPathMappingEditor.show(() -> {
        }));

        HBox hBox = GUICommon.hbox(15, txtAuthorDirectory, btnAdd, btnMapEditor);

        JFXTextField txtUrl = GUICommon.textField(fromURL, 400);
        var txtInUrl = this.getPasteButton(txtUrl);

        JFXButton btnCheck = GUICommon.buttonWithBorder("Check", (e) ->
                ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> this.check(txtUrl.getText())));
        btnCheck.disableProperty().bind(
                Bindings.createBooleanBinding(() -> txtUrl.getText().contains("nhentai.net"), txtUrl.textProperty()).not());

        JFXButton btnDownload = GUICommon.buttonWithBorder("Download", (e) -> {
            ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> {
                this.resolveOutputPath();
                this.writeImages(this.imageList);
            });
        });
        btnDownload.disableProperty().bind(this.titleString.isNotEmpty().not());

        VBox vBoxR = GUICommon.vbox(15, hBox, this.treeView, new Separator(), txtTitle);
        vBoxR.setAlignment(Pos.CENTER_LEFT);
        vBoxR.getChildren().addAll(new Separator(), txtInUrl, GUICommon.hbox(15, btnCheck, btnDownload));

        ImageView imgCover = new ImageView();
        imgCover.setPreserveRatio(true);
        imgCover.setFitWidth(250.0);
        imgCover.setFitHeight(400.0);
        imgCover.prefWidth(250);
        imgCover.prefHeight(400);
        imgCover.imageProperty().bind(this.coverImage);
        HBox imgBox = GUICommon.hbox(0, imgCover);
        imgBox.setPrefHeight(400);
        imgBox.setMinHeight(400);
        imgBox.setMaxHeight(400);
        imgBox.setAlignment(Pos.CENTER);

        VBox vBoxL = GUICommon.vbox(15, txtType, txtAuthor, imgBox);
        vBoxL.setAlignment(Pos.CENTER);
        vBoxL.setPrefSize(300, 450);
        //vBoxL.setMaxSize(280, 450);

        HBox hBox2 = GUICommon.hbox(15, vBoxL, vBoxR);
        hBox2.setAlignment(Pos.CENTER);
        hBox2.setPrefSize(960, 540);

        WindowBuilder.create()
                .title("Nhentai Downloader", true)
                .body(hBox2)
                .build().show();


        //imageLinks.forEach(e -> GUICommon.debugMessage(e.attr("href")));

    }

    private void check(String fromURL) {
        Option<Document> document = this.downloadDocument(fromURL);

        if (document.isEmpty()) {
            return;
        }

        Element coverElement = document.get().selectFirst("div[id=cover]").selectFirst("a img[is=lazyload-image]");

        Element titleElement = document.get().selectFirst("div[id=info]").selectFirst("h2");
        if (titleElement == null) {
            titleElement = document.get().selectFirst("div[id=info]").selectFirst("h1");
        }

        Elements typeElement = document.get().select("span[class=tags] a");
        Stream<String> tagsStream = Stream.ofAll(typeElement.stream().map(Element::text));

        String strWorkType = T_Misc;
        if (tagsStream.find(s -> s.contains("doujinshi")).isDefined()) {
            strWorkType = T_Dou;
        } else if (tagsStream.find(s -> s.contains("manga")).isDefined()) {
            strWorkType = T_Man;
        }

        Elements imageLinks = document.get().selectFirst("div[id=thumbnail-container]").select("div a img[is=lazyload-image]");
        this.imageList = Stream.ofAll(imageLinks.stream().map(i -> i.attr("data-src")))
                .map(s -> s.replace("t.nhentai.net", "i.nhentai.net"))
                .map(s -> s.replace("t.jpg", ".jpg"))
                .map(s -> s.replace("t.png", ".png"))
                .peek(GUICommon::debugMessage);

        this.setTypeString(strWorkType);
        this.setTitleString(processTitle(getTextWhenNotNull(titleElement)));
        this.setAuthorString(processAuthor(this.getTitleString()));
        this.updateTreeView();

        String strCover = coverElement.attr("data-src");
        var fxThumb = FxThumb.of(strCover);
        fxThumb.peek(v -> v.getImage(this.coverImage::setValue));
    }
}
