package org.nagoya.controller.mangaparsingprofile;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.io.WebServiceRun;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;
import org.nagoya.view.customcell.SelectorPathListTreeCell;
import org.nagoya.view.dialog.FXProgressDialog;
import org.openqa.selenium.By;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class KissgoddessParser extends BaseParser {
    private FXWindow fxWindow;

    public KissgoddessParser() {

    }

    @Override
    public void loadSet(String fromURL) {
        this.setTypeString(T_Images);
        this.setTypePath(this.getTypePath().resolve("Kissgoddess"));

        Vector<JFXTextField> textUrl = Vector.ofAll(0, 1, 2)
                .map(v -> GUICommon.textField("", 350));

        textUrl.get(0).setText(fromURL);

        JFXButton btnSingle = GUICommon.buttonWithBorder("Single", (e) -> {
            ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> this.loadSet(textUrl.get(0).getText(), true));
        });

        JFXButton btnMulti = GUICommon.buttonWithBorder("Multi", (e) -> {
            Stream<String> multiUrl = textUrl.map(TextInputControl::getText).toStream();
            multiUrl.filter(s -> !s.isEmpty()).forEachWithIndex((s, i) -> ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> this.loadSet(s, false)));
        });

        JFXButton btnToPeople = GUICommon.buttonWithBorder("Get People URL", (e) -> {
            ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () ->
                    this.getPeopleUrl(textUrl.get(0).getText()).peek(v -> GUICommon.runOnFx(() -> textUrl.get(0).setText(v))));
        });

        JFXButton btnPeople = GUICommon.buttonWithBorder("People Normal", (e) -> {
            ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> this.loadPeople(textUrl.get(0).getText(), false));
        });

        JFXButton btnPeople2 = GUICommon.buttonWithBorder("People Extra", (e) -> {
            ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> this.loadPeople(textUrl.get(0).getText(), true));
        });

        this.getTreeView().setCellFactory((TreeView<Path> l) ->
                SelectorPathListTreeCell.createCell("Update", path ->
                        UpdateInfo.readInfo(path).map(UpdateInfo::getUrl).peek(textUrl.get(0)::setText))
        );

        this.updateTreeView();

        btnToPeople.disableProperty().bind(Bindings.createBooleanBinding(() -> textUrl.get(0).getText().startsWith("https://tw.kissgoddess.com/album"), textUrl.get(0).textProperty()).not());
        btnSingle.disableProperty().bind(btnToPeople.disableProperty());
        btnMulti.disableProperty().bind(btnToPeople.disableProperty());
        btnPeople.disableProperty().bind(Bindings.createBooleanBinding(() -> textUrl.get(0).getText().startsWith("https://tw.kissgoddess.com/people"), textUrl.get(0).textProperty()).not());
        btnPeople2.disableProperty().bind(btnPeople.disableProperty());

        VBox vBox = GUICommon.vbox(5);
        vBox.setAlignment(Pos.CENTER_LEFT);
        textUrl.map(this::getPasteButton).forEach(t -> vBox.getChildren().add(t));
        vBox.getChildren().addAll(new Separator(), btnSingle, btnMulti, btnToPeople, btnPeople, btnPeople2);

        HBox hBox = GUICommon.hbox(5, this.treeView, vBox);
        hBox.setAlignment(Pos.CENTER);
        hBox.setPadding(FXWindow.getDefaultInset());
        hBox.setPrefSize(900, 400);

        this.fxWindow = WindowBuilder.create()
                .title("Kissgoddess Parser", true)
                .body(hBox)
                .build();
        this.fxWindow.show();
    }

    private void loadPeople(String fromURL, boolean extraMode) {
        String folderImg = "data-original";
        String folderImgExtra = "src";

        WebServiceRun run = new WebServiceRun();
        run.setConsumerCdp4j((s) -> s.click("//*[@id='btnViewMore']").wait(500));
        run.setConsumerSelenium((w) -> w.findElement(By.id("btnViewMore")).click());

        Document document = extraMode ? Systems.getWebService().getDocumentAsync(fromURL, run, true).block() : this.downloadDocument(fromURL).getOrNull();

        if (document == null) {
            return;
        }

        UpdateInfo info = new UpdateInfo(fromURL, LocalDate.now().toString());

        //GUICommon.debugMessage(document.toString());

        document.setBaseUri("https://tw.kissgoddess.com");

        String modelName = document.selectFirst("h1[class=person-name]").text();

        var parse1 = Stream.ofAll(document.select("div.td-related-gallery"));
        var folderImageStream = parse1.map(e -> e.select("img").attr(extraMode ? folderImgExtra : folderImg));
        var parse2 = parse1.map(e -> e.select("a[rel=bookmark]"));
        var urlStream = parse2.map(e -> e.attr("abs:href"));
        var titleStream = parse2.map(e -> e.attr("title")).map(this::titleConvert);

        Stream<Tuple3<String, String, String>> task = urlStream.zipWithIndex((u, i) -> Tuple.of(u, titleStream.get(i), folderImageStream.get(i)))
                .filter(t -> t._3.startsWith("https://pic.kissgoddess.com"))
                .distinctBy(t -> t._2);

        task.map(Tuple3::toString).forEach(GUICommon::debugMessage);

        Path modelDir = this.getTypePath().resolve(modelName);
        this.createModelDir(modelDir);

        UpdateInfo.writeInfo(info, modelDir);

        this.loadSet(task, modelDir);
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private String titleConvert(String dateString) {
        return Try.of(() -> LocalDate.parse(dateString, this.formatter)).map(LocalDate::toString).getOrElse(dateString);

    }

    private void loadSet(@NotNull Stream<Tuple3<String, String, String>> set, @NotNull Path modelPath) {
        var list = FXProgressDialog.getInstance().startProgressDialog();
        GUICommon.writeToObList(">> Model >> " + modelPath.toString(), list);

        var task = set.map(t -> t.map2(modelPath::resolve))
                .filter(t -> Files.notExists(t._2))
                .map(t -> t.map1(u -> this.getPages(Stream.of(this.downloadDocument(u).get()), u)))
                .map(t -> t.map1(s -> s.flatMap(this::getImgLink)))
                .peek(t -> GUICommon.writeToObList(">> Title >> " + t._2, list))
                .map(t -> t.map1(s -> this.buildFile(s, t._2)))
                .map(t -> t.map1(s -> this.appendFolderImage(s, t._2, t._3)))
                .flatMap(t -> t._1);

        GUICommon.writeToObList(">> Image to Download >> " + task.length(), list);

        this.batchDownloadUrlToFile(task);
    }

    private Stream<Tuple2<String, File>> appendFolderImage(Stream<Tuple2<String, File>> stream, Path path, @NotNull String url) {
        if (url.startsWith("https://pic.kissgoddess.com")) {
            stream = stream.append(Tuple.of(url, path.resolve("folder.jpg").toFile()));
        } else {
            stream = stream.append(Tuple.of(stream.get(0)._1, path.resolve("folder.jpg").toFile()));
        }
        return stream;
    }

    private Stream<Tuple2<String, File>> buildFile(@NotNull Stream<String> thumbList, Path path) {
        return thumbList.map(url -> Tuple.of(url, FilenameUtils.getName(url)))
                .map(t -> t.map2(path::resolve).map2(Path::toFile));
    }

    private Option<String> getPeopleUrl(String fromURL) {
        Option<Document> document = this.downloadDocument(fromURL);
        if (document.isEmpty()) {
            return Option.none();
        }
        return Option.of(document.get().selectFirst("div[class=td-related-peron-thumb] a")).map(e -> e.attr("abs:href"));
    }

    private void loadSet(String fromURL, boolean needConfirm) {
        var list = FXProgressDialog.getInstance().startProgressDialog();

        GUICommon.writeToObList(">> URL >> " + fromURL, list);

        Option<Document> document = this.downloadDocument(fromURL);

        if (document.isEmpty()) {
            return;
        }

        String titleString = document.get().selectFirst("title").text();
        GUICommon.debugMessage(titleString);
        GUICommon.writeToObList(">> Title >> " + titleString, list);

        var modelPageString = Option.of(document.get().selectFirst("div[class=td-related-peron-thumb] a")).map(e -> e.attr("abs:href"));
        GUICommon.debugMessage(modelPageString.getOrElse("-"));

        var modelDocument = modelPageString.flatMap(this::downloadDocument);
        String modelName = modelDocument.map(d -> d.selectFirst("h1[class=person-name]").text()).getOrElse("Unknown");
        GUICommon.debugMessage(modelName);
        GUICommon.writeToObList(">> Model >> " + modelName, list);

        Stream<Document> pageList = this.getPages(Stream.of(document.get()), fromURL);
        Stream<String> thumbList = pageList
                .flatMap(this::getImgLink);

        GUICommon.writeToObList(">> Image >> " + thumbList.length(), list);

        if (needConfirm || modelName.equals("Unknown")) {
            JFXTextField txtModel = GUICommon.textField(modelName, 550);
            JFXTextField txtTitle = GUICommon.textField(titleString, 550);
            JFXTextField txtDirectory = GUICommon.textField(this.getOutputPath().toString(), 550);

            VBox vBox = GUICommon.vbox(15, txtModel, new Separator(), txtTitle, txtDirectory);

            DialogBuilder.create()
                    .body(vBox)
                    .container(this.fxWindow.getRoot())
                    .buttonYesNo(() -> ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> this.doDownload(txtTitle.getText(), txtModel.getText(), thumbList, fromURL)))
                    .build().show();
        } else {
            this.doDownload(titleString, modelName, thumbList, fromURL);
        }
    }

    private void doDownload(String title, String modelName, @NotNull Stream<String> thumbList, String urlString) {
        if (thumbList.length() > 0) {
            Path modelDir = this.getTypePath().resolve(modelName);
            this.setOutputPath(modelDir.resolve(title));

            Stream<Tuple2<String, File>> outList = thumbList.map(url -> Tuple.of(url, FilenameUtils.getName(url)))
                    .map(t -> t.map2(this.getOutputPath()::resolve).map2(Path::toFile));

            outList = outList.append(Tuple.of(outList.get(0)._1, this.getOutputPath().resolve("folder.jpg").toFile()));

            this.createModelDir(modelDir);
            this.batchDownloadUrlToFile(outList);

            //UtilCommon.saveStringToFile(this.getOutputPath().resolve("url.txt"), urlString);
        }
    }

    private void createModelDir(Path path) {
        if (Files.notExists(path)) {
            UtilCommon.tryCreateDirectory(path);
            this.updateTreeView();
        }
    }

    private Stream<Document> getPages(@NotNull Stream<Document> inDoc, String endStr) {
        Element nextPage = inDoc.last().selectFirst("a:contains(下一頁)");

        Stream<Document> out = inDoc;

        if (nextPage != null) {
            String nextUrl = nextPage.attr("abs:href");
            if (!nextUrl.equals("") && !nextUrl.equals(endStr)) {
                GUICommon.debugMessage(nextUrl + " " + inDoc.length());
                var doc = this.downloadDocument(nextUrl);
                if (doc.isDefined()) {
                    out = this.getPages(inDoc.append(this.downloadDocument(nextUrl).get()), endStr);
                }
            }
        }

        return out;
    }

    @NotNull
    private Stream<String> getImgLink(@NotNull Document document) {
        //GUICommon.debugMessage(" " + document.select("div.td-gallery-content").select("img").toString());
        var s = Stream.ofAll(document.select("div.td-gallery-content").select("img").stream())
                .map(e -> e.attr("abs:src"));
        //s.forEach(GUICommon::debugMessage);
        return s;
    }
}
