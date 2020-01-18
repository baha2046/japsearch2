package org.nagoya.controller.mangaparsingprofile;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.io.Setting;
import org.nagoya.io.WebServiceRun;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.preferences.GuiSettings;
import org.nagoya.preferences.RenameSettings;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.view.customcell.SelectorDirectoryOnlyTreeItem;
import org.nagoya.view.dialog.FXProgressDialog;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class BaseParser {

    static final String T_Dou = "Doujinshi";
    static final String T_Man = "Manga";
    static final String T_Misc = "Misc";
    static final String T_Images = "Images";

    StringProperty typeString;
    StringProperty authorString;
    StringProperty titleString;
    ObjectProperty<Path> typePath;
    ObjectProperty<Path> authorPath;
    ObjectProperty<Image> coverImage;
    Option<Path> outputPath;

    TreeView<Path> treeView = null;

    String strUserAgent;
    int intTimeOut;

    @Contract(pure = true)
    public BaseParser() {
        this.strUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.11 Safari/537.36";
        this.intTimeOut = 20000;
        this.outputPath = Option.none();

        this.typeString = new SimpleStringProperty();
        this.authorString = new SimpleStringProperty();
        this.titleString = new SimpleStringProperty();
        this.typePath = new SimpleObjectProperty<>();
        this.authorPath = new SimpleObjectProperty<>();
        this.coverImage = new SimpleObjectProperty<>();

        this.typeString.addListener((ov, so, sn) -> this.typePath.setValue(this.changeTypePath()));
        this.authorString.addListener((ov, so, sn) -> this.authorPath.setValue(this.changeAuthorPath()));
    }

    public String getTypeString() {
        return this.typeString.get();
    }

    public StringProperty typeStringProperty() {
        return this.typeString;
    }

    public void setTypeString(String typeString) {
        this.typeString.set(typeString);
    }

    public String getAuthorString() {
        return this.authorString.get();
    }

    public StringProperty authorStringProperty() {
        return this.authorString;
    }

    public void setAuthorString(String authorString) {
        this.authorString.set(authorString);
    }

    public String getTitleString() {
        return this.titleString.get();
    }

    public StringProperty titleStringProperty() {
        return this.titleString;
    }

    public void setTitleString(String titleString) {
        this.titleString.set(titleString);
    }

    public Path getTypePath() {
        return this.typePath.get();
    }

    public ObjectProperty<Path> typePathProperty() {
        return this.typePath;
    }

    public void setTypePath(Path typePath) {
        this.typePath.set(typePath);
    }

    public Path getAuthorPath() {
        return this.authorPath.get();
    }

    public ObjectProperty<Path> authorPathProperty() {
        return this.authorPath;
    }

    public void setAuthorPath(Path authorPath) {
        this.authorPath.set(authorPath);
    }

    protected void resolveOutputPath() {
        if (Files.notExists(this.getAuthorPath())) {
            this.setOutputPath(this.getTypePath().resolve(this.getTitleString()));
        } else {
            this.setOutputPath(this.getAuthorPath().resolve(this.getTitleString()));
        }
    }

    protected void setOutputPath(Path path) {
        this.outputPath = Option.of(path);
    }

    protected Path getOutputPath() {
        return this.outputPath.get();
    }

    protected void writeImages(Stream<String> imageList) {
        GUICommon.debugMessage("outputPathoutputPath " + this.outputPath.toString());
        this.outputPath.peek(outImageDirectory -> {
            var observableList = FXProgressDialog.getInstance().startProgressDialog();
            GUICommon.writeToObList("Starting Download to " + outImageDirectory.toString(), observableList);

            UtilCommon.tryCreateDirectory(outImageDirectory)
                    .recover(FileAlreadyExistsException.class, outImageDirectory)
                    .onSuccess(s -> this.batchDownloadUrlToFile(this.buildDownloadStream(imageList, outImageDirectory)))
                    .onFailure(GUICommon::errorDialog);
        });
    }

    public abstract void loadSet(String fromURL);

    static String getTextWhenNotNull(Element element) {
        return getTextWhenNotNull(element, "unknown");
    }

    static String getTextWhenNotNull(Element element, String useWhenNull) {
        if (element != null) {
            return element.text();
        } else {
            return useWhenNull;
        }
    }

    @NotNull
    static String processTitle(String strWorkTitle) {
        strWorkTitle = strWorkTitle.replace("/", " ");
        strWorkTitle = strWorkTitle.replace(":", " ");
        strWorkTitle = strWorkTitle.replace("?", "");
        strWorkTitle = strWorkTitle.replace(" [DL版]", "");
        return strWorkTitle;
    }

    static String processAuthor(@NotNull String strWorkTitle) {
        String strWorkAuthor = "[unknown]";
        int i1 = strWorkTitle.indexOf("[");
        int i2 = strWorkTitle.indexOf("]");
        if (i1 > -1 && i2 > -1) {
            strWorkAuthor = strWorkTitle.substring(i1, i2 + 1);
        }
        return strWorkAuthor;
    }

    protected TreeView<Path> getTreeView() {
        if (this.treeView == null) {
            this.treeView = new TreeView<>();
            this.treeView.setShowRoot(false);
            this.treeView.setMinWidth(500);
            this.typePath.addListener((ov, o, n) -> {
                if (n != null) {
                    this.updateTreeView();
                }
            });
            /*this.authorPath.addListener((ov, o, n) -> {
                if (n != null) {
                    this.updateTreeView();
                }
            });*/
        }
        return this.treeView;
    }

    protected void updateTreeView() {
        GUICommon.runOnFx(() -> this.treeView.setRoot(SelectorDirectoryOnlyTreeItem.createNode(
                this.getAuthorPath() == null || Files.notExists(this.getAuthorPath()) ? this.getTypePath() : this.getAuthorPath())));
    }

    // example [なかよひ (いづるみ)]
    @NotNull
    protected Path getAuthorPath(String strType, Path pathTarget, @NotNull String strAuthor) {
        if (!strAuthor.startsWith("[")) {
            strAuthor = "[" + strAuthor;
        }

        if (!strAuthor.endsWith("]")) {
            strAuthor = strAuthor + "]";
        }

        if (Objects.equals(strType, T_Dou) && strAuthor.indexOf("(") > 2) {
            String strGroup = strAuthor.substring(0, strAuthor.indexOf("(") - 1) + "]";
            //GUICommon.debugMessage(strGroup);
            if (Files.isDirectory(pathTarget.resolve(strGroup))) {
                return pathTarget.resolve(strGroup);
            }
        }
        return pathTarget.resolve(RenameSettings.getInstance().renameCompany(strAuthor));
    }

    private Path changeTypePath() {
        String strType = this.getTypeString();

        switch (strType) {
            case T_Dou:
                return GuiSettings.getInstance().getDirectory(GuiSettings.Key.doujinshiDirectory);
            case T_Man:
                return GuiSettings.getInstance().getDirectory(GuiSettings.Key.mangaDirectory);
            case T_Images:
                return GuiSettings.getInstance().getDirectory(GuiSettings.Key.photoDirectory);
        }

        return GuiSettings.getInstance().getDirectory(GuiSettings.Key.downloadDirectory);
    }

    @NotNull
    private Path changeAuthorPath() {
        String strAuthor = this.getAuthorString();
        if (!strAuthor.startsWith("[")) {
            strAuthor = "[" + strAuthor;
        }

        if (!strAuthor.endsWith("]")) {
            strAuthor = strAuthor + "]";
        }

        if (Objects.equals(this.getTypeString(), T_Dou) && strAuthor.indexOf("(") > 2) {
            String strGroup = strAuthor.substring(0, strAuthor.indexOf("(") - 1) + "]";
            if (Files.isDirectory(this.getTypePath().resolve(strGroup))) {
                return this.getTypePath().resolve(strGroup);
            }
        }
        return this.getTypePath().resolve(RenameSettings.getInstance().renameCompany(strAuthor));
    }

    protected Option<Document> downloadDocument(String url) {
        return Systems.getWebService()
                .getDocument(url, WebServiceRun.none, false)
                .toOption();
    }

    protected Option<ObservableList<String>> createProgressDialog() {
        JFXListView<String> textArea = FXFactory.textArea(700, 130, true);
        DialogBuilder.create().heading("[ Progress ]").body(textArea).build().show();
        return Option.of(textArea.getItems());
    }

    void writeImages(@NotNull Stream<FxThumb> imageList, @NotNull Path outPath, Option<ObservableList<String>> observableList) {
        DecimalFormat df = new DecimalFormat("000");

        GUICommon.debugMessage("Start Download to " + outPath.toString());
        GUICommon.writeToObList("Start Download to " + outPath.toString(), observableList);
        GUICommon.writeToObList("", observableList);

        Stream<Tuple2<FxThumb, File>> downloadStream = imageList.zipWithIndex((thumb, idx) -> {
            String fileName = df.format(idx + 1) + ".jpg";
            if (idx == 0) {
                thumb.writeURLToFile(new File(outPath.toFile(), "folder.jpg"));
            }
            return Tuple.of(thumb, new File(outPath.toFile(), fileName));
        });

        //this.batchDownloadThumbToFile(downloadStream, observableList);

        downloadStream.forEach(t -> this.downloadThumbWithRetry(t._1, t._2, observableList));

        GUICommon.writeToObList("Completed", observableList);
    }

    protected Stream<Tuple2<String, File>> buildDownloadStream(@NotNull Stream<String> imageList, @NotNull Path outPath) {
        DecimalFormat df = new DecimalFormat("000");

        return imageList.zipWithIndex((url, idx) -> {
            String fileName = df.format(idx + 1) + ".jpg";
            if (idx == 0) {
                FxThumb.of(url).peek(t -> t.writeURLToFile(new File(outPath.toFile(), "folder.jpg")));
            }
            return Tuple.of(url, new File(outPath.toFile(), fileName));
        });
    }


    protected void batchDownloadThumbToFile(@NotNull Stream<Tuple2<FxThumb, File>> fileList, Option<ObservableList<String>> observableList) {
        Flux.fromStream(fileList.toJavaStream())
                .parallel()
                .runOn(Schedulers.parallel())
                .subscribe(t -> this.downloadThumbWithRetry(t._1, t._2, observableList),
                        e -> {
                        }, () -> GUICommon.writeToObList("Completed", observableList)
                );
    }

    protected void batchDownloadUrlToFile(@NotNull Stream<Tuple2<String, File>> fileList) {
        var progressDialog = FXProgressDialog.getInstance();
        Runnable onComplete = () -> {
            progressDialog.setThreadState(Thread.currentThread().getName(), "");
        };

        ProgressCount progressCount = new ProgressCount(0, fileList.length());

        Consumer<Tuple2<String, File>> onRun = t -> {
            progressDialog.setThreadState(Thread.currentThread().getName(), " Run ");
            Try.of(() -> new URL(t._1))
                    .peek(url -> UtilCommon.saveUrlToFile(url, t._2, Option.of(progressDialog.getList())));

            progressDialog.setProgress(progressCount.addProgress());
        };
                            /*Try.run(() -> FileUtils.copyURLToFile(url, t._2))
                            .onSuccess(v -> progressDialog.write(t._2.getName() + " << " + url.toString()))
                            .onFailure(e -> progressDialog.write(t._2.getName() + " << " + e.getMessage()))*/

        Flux.fromStream(fileList.toJavaStream())
                .parallel()
                .runOn(Schedulers.parallel())
                .subscribe(onRun, GUICommon::errorDialog, onComplete);
    }


    protected void downloadThumbWithRetry(@NotNull FxThumb thumb, File file, Option<ObservableList<String>> observableList) {
        boolean b = thumb.writeURLToFile(file);
        if (!b) {
            int retry = 5;

            while (retry > 0 && !b) {
                b = thumb.writeURLToFile(file);
                retry--;
            }
        }

        if (b) {
            GUICommon.writeToObListWithoutNewLine(file.getName() + " << " + thumb.getThumbURL().toString(), observableList);
        } else {
            GUICommon.writeToObList(file.getName() + " << " + "FAILED", observableList);
            GUICommon.writeToObList("", observableList);
        }
    }

    @NotNull
    protected HBox getPasteButton(JFXTextField textField) {
        JFXButton btnC = FXFactory.button(" Clear ", (e) -> textField.setText(""));
        btnC.setMinWidth(60);
        JFXButton btnP = FXFactory.button(" Paste ", (e) -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            DataFlavor flavor = DataFlavor.stringFlavor;
            if (clipboard.isDataFlavorAvailable(flavor)) {
                try {
                    String text = (String) clipboard.getData(flavor);
                    textField.setText(text);
                    System.out.println(text);
                } catch (UnsupportedFlavorException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        btnP.setMinWidth(60);
        return GUICommon.hbox(15, textField, btnC, btnP);
    }
}

class ProgressCount {
    int c;
    int end;

    ProgressCount(int s, int e) {
        this.c = s;
        this.end = e;
    }

    double addProgress() {
        if (this.c < this.end) {
            this.c++;
        }
        return (float) this.c / this.end;
    }
}

class UpdateInfo {
    static final String fileName = "info.txt";
    String url;
    String lastUpdate;

    public static Option<UpdateInfo> readInfo(@NotNull Path path) {
        Path infoPath = path.resolve(fileName);
        if (Files.exists(infoPath)) {
            return Option.of(Setting.readSetting(UpdateInfo.class, infoPath));
        }
        return Option.none();
    }

    public static boolean isExist(@NotNull Path path) {
        Path infoPath = path.resolve(fileName);
        return Files.exists(infoPath);
    }

    public static void writeInfo(UpdateInfo info, @NotNull Path path) {
        Setting.writeSetting(info, path.resolve(fileName));
    }

    public UpdateInfo(String url, String lastUpdate) {
        this.url = url;
        this.lastUpdate = lastUpdate;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastUpdate() {
        return this.lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}