package org.nagoya.view.dialog;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import io.vavr.control.Option;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.jetbrains.annotations.Contract;
import org.jsoup.select.Elements;
import org.nagoya.GUICommon;
import org.nagoya.io.WebServiceRun;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;

public class FXSeedListDialog {
    public static void show(String strUrl, Option<Runnable> runnable) {
        Systems.useExecutors(() -> {
            Systems.getWebService().getDocument(strUrl, WebServiceRun.none, true)
                    .peek(document -> {

                        Elements chromeElements = document.select("a[style='color:#333']");

                        //List<WebElement> chromeElements = driver.findElements(By.xpath("//a[@style='color:#333']"));

                        VBox vbox = GUICommon.vbox(6);

                        if (chromeElements.size() < 2) {
                            vbox.getChildren().add(new Text("No Seed Found!"));
                        } else {
                            for (int i = 2; i < chromeElements.size(); i += 3) {
                                Seed seed = new Seed(chromeElements.get(i).text(),
                                        chromeElements.get(i - 2).text(),
                                        chromeElements.get(i - 1).text(),
                                        chromeElements.get(i - 2).attr("href"));

                                vbox.getChildren().addAll(seed.render(), new Separator());
                            }
                        }

                        ScrollPane scrollPane = new ScrollPane(vbox);
                        scrollPane.setMinSize(1000, 600);
                        scrollPane.setPrefSize(1000, 600);
                        scrollPane.setFitToWidth(true);

                        StackPane pane = new StackPane(scrollPane);
                        pane.setPadding(FXWindow.getDefaultInset());

                        GUICommon.runOnFx(() -> {
                            WindowBuilder.create()
                                    .title("Seed: " + strUrl, true)
                                    .body(pane)
                                    .resizable(false)
                                    .build().show();
                        });
                        //DialogBuilder.create().body(scrollPane).build().show();
                    });

            Platform.runLater(() -> runnable.peek(Runnable::run));
        });
    }
}

class Seed {
    private final String sDate;
    private final String sName;
    private final String sSize;
    private final String sUrl;

    @Contract(pure = true)
    Seed(String date, String name, String size, String url) {
        this.sDate = date;
        this.sName = name;
        this.sSize = size;
        this.sUrl = url;
    }

    public VBox render() {
        JFXTextField txtLink = GUICommon.textField(this.sUrl, 800);
        JFXButton btnCopy = GUICommon.buttonWithBorder("Copy", (e) -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(txtLink.getText());
            Systems.getClipboard().setContent(clipboardContent);
        });
        btnCopy.setMinWidth(50);
        HBox hBox1 = GUICommon.hbox(10, btnCopy, txtLink);
        HBox hBox2 = GUICommon.hbox(30, new Text(this.sDate), new Text(this.sSize), new Text(this.sName));
        hBox1.setPadding(new Insets(0, 0, 0, 20));
        hBox2.setPadding(new Insets(5, 0, 0, 20));
        return GUICommon.vbox(10, hBox2, hBox1);
    }
}
