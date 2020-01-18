package org.nagoya.controls;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextArea;
import io.vavr.control.Option;
import javafx.animation.FadeTransition;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.util.Callback;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.NagoyaResource;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.system.dialog.DialogBuilder;
import org.nagoya.system.dialog.FXWindow;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class FXWebViewControl extends StackPane {

    private final TabPane webPane;

    public FXWebViewControl() {
        //this.maxWidth = App.getCurrentStage().map(Window::getWidth).getOrElse((double) 400) - 70;
        //this.maxHeight = App.getCurrentStage().map(Window::getHeight).getOrElse((double) 300) - 170;

        this.webPane = new TabPane();
        this.setPadding(FXWindow.getDefaultInset());
        this.getChildren().add(this.webPane);
    }

    public void loadUrl(String strUrl, Option<Function<WebView, List<JFXButton>>> customButton, Option<Consumer<WebView>> runModify) {
        this.newTab(customButton, runModify).load(strUrl);
    }

    public WebEngine newTab(Option<Function<WebView, List<JFXButton>>> customButton, Option<Consumer<WebView>> runModify) {
        WebTab tab = new WebTab();
        tab.setRunModify(runModify);
        tab.setOnCloseRequest((e) -> {
            if (this.webPane.getTabs().size() <= 1) {
                e.consume();
            }
        });
        this.webPane.getTabs().add(tab);
        return tab.prepare(customButton);
    }

    public static void modifyJavBus(@NotNull WebView webView) {
        Document doc = webView.getEngine().getDocument();

        var div = doc.getElementsByTagName("DIV");
        if (div != null && div.getLength() > 0) {
            for (int i = 0; i < div.getLength(); i++) {
                var divElement = (Element) div.item(i);
                var divElementClass = divElement.getAttribute("class");

                if (divElementClass != null) {
                    switch (divElementClass) {
                        case "col-xs-12 col-md-4 text-center ptb10":
                        case "ad-list":
                            // case "container-fluid":
                        case "row visible-xs-inline footer-bar":
                            divElement.setTextContent("");
                            //div.item(indx).getParentNode().removeChild(div.item(indx));
                            break;
                    }
                }
            }
        }

        var nav = doc.getElementsByTagName("NAV");
        if (nav != null && nav.getLength() > 0) {
            nav.item(0).getParentNode().removeChild(nav.item(0));
        }

        var footer = doc.getElementsByTagName("FOOTER");
        if (footer != null && footer.getLength() > 0) {
            footer.item(0).getParentNode().removeChild(footer.item(0));
        }

        webView.getEngine().executeScript("document.body.style.background = 'white'");

        //int height = (Integer) webView.getEngine().executeScript("document.body.scrollHeight");
        //webView.setPrefHeight(height + 20);

    }

    static String printDocument(Document doc) {

        OutputStream outputStream = new ByteArrayOutputStream();

        TransformerFactory tf = TransformerFactory.newInstance();

        try {
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(doc),
                    new StreamResult(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return outputStream.toString();
    }

    static void onWebBack(@NotNull WebView webView) {
        WebEngine engine = webView.getEngine();
        WebHistory history = engine.getHistory();
        if (history.getCurrentIndex() > 0) {
            history.go(-1);
        }
    }

    static void onWebNext(@NotNull WebView webView) {
        WebEngine engine = webView.getEngine();
        WebHistory history = engine.getHistory();
        if (history.getCurrentIndex() < history.getEntries().size() - 1) {
            history.go(1);
        }
    }

    //URLロード
    static void onWebLoad(WebView webView, @NotNull String search) {
        if (!search.matches("^https{0,1}://.+")) {
            //httpじゃないならgoogle検索を実行
            search = "https://www.google.co.jp/search?q=" + search;
        }
        webView.getEngine().load(search);
    }
}

class WebTab extends Tab {
    private WebView webView;
    private HBox controlBar;
    private TextField addressBar;
    private FadeTransition fadeIn;
    private Option<Consumer<WebView>> runModify;
    private boolean popUpAllow;

    WebTab() {
        this.popUpAllow = true;
        this.runModify = Option.none();
        this.initWebView();
    }

    private void initWebView() {
        this.webView = new WebView();
        this.webView.setMaxHeight(Screen.getPrimary().getBounds().getHeight());
        this.webView.getEngine().setUserStyleSheetLocation(NagoyaResource.load("css/webView.css").toExternalForm());
        this.webView.setVisible(false);

        this.fadeIn = new FadeTransition(Duration.millis(1000));
        this.fadeIn.setNode(this.webView);
        this.fadeIn.setFromValue(0.0);
        this.fadeIn.setToValue(1.0);
        this.fadeIn.setCycleCount(1);
        this.fadeIn.setAutoReverse(false);

        this.addressBar = GUICommon.textFieldWithBorder("", 400);
        this.addressBar.textProperty().bind(this.webView.getEngine().locationProperty());

        this.textProperty().bind(this.addressBar.textProperty());

        this.webView.getEngine().getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        this.runModify.peek(c -> c.accept(this.webView));
                        this.webView.setVisible(true);
                        this.fadeIn.playFromStart();
                    } else {
                        this.webView.setVisible(false);
                    }
                }
        );

        this.controlBar = GUICommon.hbox(10);
        this.controlBar.setAlignment(Pos.CENTER);
        this.controlBar.setMinHeight(40);
        this.controlBar.setPrefHeight(40);
        this.controlBar.setMaxHeight(40);

        VBox vBox = GUICommon.vbox(10, this.webView, this.controlBar);
        vBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(this.webView, Priority.ALWAYS);

        this.setContent(vBox);
    }

    public void setRunModify(Option<Consumer<WebView>> runModify) {
        this.runModify = runModify;
    }

    public void setPopUpAllow(boolean popUpAllow) {
        this.popUpAllow = popUpAllow;
    }

    public void setupControlBar(@NotNull Option<List<JFXButton>> buttonList) {
        JFXComboBox<String> historyCombo = new JFXComboBox<>();
        historyCombo.setPrefWidth(50);

        final WebHistory history = this.webView.getEngine().getHistory();
        history.getEntries().addListener((ListChangeListener<WebHistory.Entry>) c -> {
            c.next();
            for (WebHistory.Entry e : c.getRemoved()) {
                historyCombo.getItems().remove(e.getUrl());
            }
            for (WebHistory.Entry e : c.getAddedSubList()) {
                historyCombo.getItems().add(e.getUrl());
            }
        });

        //set the behavior for the history combobox
        historyCombo.setOnAction(ev -> {
            int offset = historyCombo.getSelectionModel().getSelectedIndex() - history.getCurrentIndex();
            if (offset >= 0) {
                history.go(offset);
            }
        });

        JFXButton btnBack = FXFactory.buttonWithBorder(" < ", e -> FXWebViewControl.onWebBack(this.webView));
        JFXButton btnNext = FXFactory.buttonWithBorder(" > ", e -> FXWebViewControl.onWebNext(this.webView));
        JFXButton btnGo = FXFactory.buttonWithBorder(" Go ", e -> FXWebViewControl.onWebLoad(this.webView, this.addressBar.getText()));

        JFXButton btnSource = FXFactory.buttonWithBorder("  Source  ", e -> {
            String strSource = FXWebViewControl.printDocument(this.webView.getEngine().getDocument());
            JFXTextArea textArea = new JFXTextArea(strSource);
            textArea.setMinSize(1000, 450);

            DialogBuilder.create().body(textArea).container(FXWindow.getWindow("Web Browser").map(FXWindow::getRoot).getOrElse(() -> null)).build().show();
        });

        this.controlBar.getChildren().setAll(historyCombo, btnBack, btnNext, this.addressBar, btnGo, btnSource);
        buttonList.peek(l -> this.controlBar.getChildren().addAll(l));
    }

    public WebEngine prepare(@NotNull Option<Function<WebView, List<JFXButton>>> customButton) {
        this.setupControlBar(customButton.map(v -> v.apply(this.webView)));
        this.webView.setVisible(false);

        this.webView.getEngine().setCreatePopupHandler((Callback<PopupFeatures, WebEngine>) config -> {
            if (this.popUpAllow) {
                return FXWebViewWindow.newTab(customButton, this.runModify);
            }
            return null;
        });

        return this.webView.getEngine();
    }
}