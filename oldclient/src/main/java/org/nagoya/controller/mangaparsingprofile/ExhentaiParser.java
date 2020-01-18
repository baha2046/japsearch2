package org.nagoya.controller.mangaparsingprofile;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.collections.ObservableList;
import javafx.scene.control.Separator;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.system.Systems;
import org.nagoya.view.customcell.SelectorDirectoryOnlyTreeItem;
import org.nagoya.view.customcell.SelectorPathListTreeCell;
import org.nagoya.view.dialog.FXProgressDialog;

import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExhentaiParser extends BaseParser {
    public static final Vector<Vector<String>> mapCookie = Vector.of(
            Vector.of("ipb_member_id", "346490"),
            Vector.of("ipb_pass_hash", "da808be315bc4a9a35319117c1573591"),
            Vector.of("igneous", "b556a8d1b"));

    public static Map<String, List<String>> getHeader() {
        var headers = new LinkedHashMap<String, List<String>>();
        headers.put("Set-Cookie", Arrays.asList(mapCookie.get(0).get(0) + "=" + mapCookie.get(0).get(1),
                mapCookie.get(1).get(0) + "=" + mapCookie.get(1).get(1),
                mapCookie.get(2).get(0) + "=" + mapCookie.get(2).get(1)));
        return headers;
    }

    public static void setupCookie() {
        URI uri = URI.create("https://exhentai.org/");
        Try.run(() -> java.net.CookieHandler.getDefault().put(uri, ExhentaiParser.getHeader()))
                .onFailure(e -> GUICommon.debugMessage(">> Error at set cookie >> " + e.getMessage()));
    }

    private final String strCookie;


    public ExhentaiParser() {
        this.strCookie = mapCookie.get(0).get(0) + "=" + mapCookie.get(0).get(1) + ";"
                + mapCookie.get(1).get(0) + "=" + mapCookie.get(1).get(1) + ";"
                + mapCookie.get(2).get(0) + "=" + mapCookie.get(2).get(1);
    }

    @Override
    public void loadSet(String fromURL) {
        Option<Document> document = this.downloadDocument(fromURL + "?nw=always");

        if (document.isEmpty()) {
            return;
        }

        //GUICommon.debugMessage(document.toString());

        String strWorkTitle = processTitle(getTextWhenNotNull(document.get().select("#gj").first()));
        String strWorkType = getTextWhenNotNull(document.get().select("div[id=gdc] div").first(), "Misc");
        String strWorkAuthor = processAuthor(strWorkTitle);

        Element elementCover = document.get().select("div[id=gd1] div").first();
        String strCover = elementCover.attr("style");
        strCover = strCover.substring(strCover.indexOf("(") + 1, strCover.indexOf(")"));

        //GUICommon.debugMessage(strCover);

        GUICommon.debugMessage(strWorkTitle + " " + strWorkAuthor);

        this.setTypeString(strWorkType);
        this.setAuthorString(strWorkAuthor);

        JFXTextField txtType = GUICommon.textField(strWorkType, 550);
        JFXTextField txtAuthor = GUICommon.textField(strWorkAuthor, 550);
        JFXTextField txtTitle = GUICommon.textField(strWorkTitle, 550);
        JFXTextField txtAuthorDirectory = GUICommon.textField("", 550);
        txtAuthorDirectory.textProperty().bind(this.authorPathProperty().asString());
        txtAuthorDirectory.setEditable(false);
        txtType.setDisable(true);

        HBox hBox = GUICommon.hbox(15, txtAuthorDirectory);

        TreeView<Path> treeView = new TreeView<>();

        if (Files.notExists(this.getAuthorPath())) {
            txtAuthorDirectory.setMinWidth(450);
            txtAuthorDirectory.setMaxWidth(450);
            JFXButton btnAdd = GUICommon.buttonWithBorder("Create", e -> {
            });
            btnAdd.setOnAction(e -> {
                UtilCommon.tryCreateDirectory(this.getAuthorPath()).onSuccess(p -> {
                    treeView.setRoot(SelectorDirectoryOnlyTreeItem.createNode(p));
                    btnAdd.setVisible(false);
                    txtAuthorDirectory.setMinWidth(550);
                    txtAuthorDirectory.setMaxWidth(550);
                });
            });
            hBox.getChildren().add(btnAdd);
        }

        treeView.setRoot(SelectorDirectoryOnlyTreeItem.createNode(Files.exists(this.getAuthorPath()) ? this.getAuthorPath() : this.getAuthorPath().getParent()));
        treeView.setShowRoot(false);
        treeView.setCellFactory((TreeView<Path> l) ->
                SelectorPathListTreeCell.createCell("Use Folder", this::setAuthorPath)
        );

        VBox vBox = GUICommon.vbox(15, txtType, new Separator(), txtAuthor, txtTitle, new Separator(), hBox, treeView);

        var fxThumb = FxThumb.of(strCover);
        ImageView imgCover = new ImageView();
        imgCover.setPreserveRatio(true);
        imgCover.setFitWidth(250.0);
        imgCover.setFitHeight(600.0);
        fxThumb.peek(v -> v.setCookie(this.strCookie)).peek(v -> v.getImage(imgCover::setImage));

        HBox hBox2 = GUICommon.hbox(15, imgCover, vBox);

        Systems.useExecutors(() -> {
            treeView.getRoot().setExpanded(true);
        });

        GUICommon.showDialog("[Download]", hBox2, "Cancel", "Confirm", () -> {

            var observableList = FXProgressDialog.getInstance().startProgressDialog();

            Systems.useExecutors(() -> {

                GUICommon.writeToObList(txtTitle.getText(), observableList);

                Element firstImageURL = document.get().select("div[id=gdt] div a").first();

                this.setTitleString(txtTitle.getText());
                this.resolveOutputPath();

                //GUICommon.checkAndCreateDir(outImageDirectory);
                //DecimalFormat df = new DecimalFormat("000");

                GUICommon.writeToObList("Scan for images", observableList);
                GUICommon.writeToObList("", observableList);
                Stream<FxThumb> thumbList = this.buildDownloadList(Stream.empty(), firstImageURL.attr("href"), observableList);

                UtilCommon.tryCreateDirectory(this.getOutputPath())
                        .recover(FileAlreadyExistsException.class, this.getOutputPath())
                        .onSuccess(s -> this.writeImages(thumbList, s, observableList))
                        .onFailure(GUICommon::errorDialog);

            });
        });

        // https://exhentai.org/g/5222/13f214a9e1/
    }

    private Stream<FxThumb> buildDownloadList(Stream<FxThumb> thumbStream, String fromURL, Option<ObservableList<String>> observableList) {
        Option<Document> document = this.downloadDocument(fromURL);

        if (document.isEmpty()) {
            document = this.downloadDocument(fromURL);
        }
        if (document.isEmpty()) {
            return thumbStream;
        }

        Element controlBar = document.get().select("div.sn").first();
        Element count = controlBar.select("div div").first();

        int currentNum = Integer.parseInt(count.select("span").first().text());
        int totalNum = Integer.parseInt(count.select("span").last().text());

        String nextImage = controlBar.select("#next").attr("href");
        String imageURL = document.get().select("div[id=i3] img").first().attr("src");

        GUICommon.writeToObListWithoutNewLine("Parsing " + currentNum + " / " + totalNum + " | " + imageURL, observableList);

        Option<FxThumb> thumb = FxThumb.of(imageURL).peek(t -> t.setCookie(this.strCookie));

        if (thumb.isDefined()) {
            thumbStream = thumbStream.append(thumb.get());
        }

        if (currentNum < totalNum) {
            thumbStream = this.buildDownloadList(thumbStream, nextImage, observableList);
        }

        return thumbStream;
    }

    @Override
    protected Option<Document> downloadDocument(String url) {

        return Try.of(() -> Jsoup.connect(url).userAgent(this.strUserAgent)
                .cookie(mapCookie.get(0).get(0), mapCookie.get(0).get(1))
                .cookie(mapCookie.get(1).get(0), mapCookie.get(1).get(1))
                .cookie(mapCookie.get(2).get(0), mapCookie.get(2).get(1))
                .ignoreHttpErrors(true).timeout(this.intTimeOut).get()
        ).onFailure(GUICommon::errorDialog).toOption();
    }

}
