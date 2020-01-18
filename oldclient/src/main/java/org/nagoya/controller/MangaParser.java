package org.nagoya.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import io.vavr.control.Option;
import javafx.geometry.Pos;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.nagoya.GUICommon;
import org.nagoya.controller.mangaparsingprofile.*;
import org.nagoya.controls.FXWebViewWindow;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.system.dialog.DialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MangaParser {

    public static void showWebView(String strUrl) {

        ExhentaiParser.setupCookie();

        FXWebViewWindow.show(strUrl, Option.of(MangaParser::getCustomButton), Option.none());
    }

    public static List<JFXButton> getCustomButton(WebView webView) {
        List<JFXButton> buttonList = new ArrayList<>();

        buttonList.add(FXFactory.buttonWithBorder("  Download  ", e -> {
            String targetUrl = webView.getEngine().getLocation();
            if (targetUrl.contains("exhentai.org/")) {
                new ExhentaiParser().loadSet(targetUrl);
            } else if (targetUrl.contains("moeero-library.com")) {
                new MoeeroParser().loadSet(targetUrl);
            } else if (targetUrl.contains("eromangacafe.com")) {
                new EromangacafeParser().loadSet(targetUrl);
            } else if (targetUrl.contains("nhentai.net")) {
                new NhentaiParser().loadSet(targetUrl);
            } else if (targetUrl.contains("imagebam.com")) {
                new ImagebamParser().loadSet(targetUrl);
            } else if (targetUrl.contains("kissgoddess.com")) {
                new KissgoddessParser().loadSet(targetUrl);
            } else if (targetUrl.contains("bi-girl.net")) {
                new BigirlParser().loadSet(targetUrl);
            }
            //loadSet(addressBar.getText());
        }));

        return buttonList;
    }


    public static void loadHImage(String strUrl) {
        JFXListView<String> textArea = FXFactory.textArea(700, 500, true);

        JFXTextField txtInput = FXFactory.textField(strUrl, 560);

        JFXButton btnEdit = GUICommon.buttonWithCheck("Download", e -> {
            textArea.getItems().clear();
            // loadSet(txtInput.getText(), Option.of(textArea.getItems()));
        });

        //GUICommon.debugMessage(strCookie);

        HBox hBox = FXFactory.hbox(15, txtInput, btnEdit);
        hBox.setMinWidth(700);
        hBox.setAlignment(Pos.CENTER);

        VBox vBox = FXFactory.vbox(15, hBox, new Separator(), textArea);

        DialogBuilder.create()
                .heading("[ e-hentai downloader ]")
                .body(vBox)
                .button("Done", null, null);
    }


    public static void load() {
        // http://www.dmm.co.jp/mono/dvd/-/list/=/article=maker/id=6329/
        String mid = "4818";

/*
        Systems.useExecutors(() -> {
            Document document = SiteParsingProfile.downloadDocumentFromURLString("http://www.dmm.co.jp/mono/dvd/-/list/=/article=label/id=" + mid + "/");
            Element nextPageLink;

            ArrayList<String> pagesVisited = new ArrayList<>();
            while (true) {

                nextPageLink = document.select("div.list-capt div.list-boxcaptside.list-boxpagenation ul li:not(.terminal) a").last();
                String currentPageURL = document.baseUri();
                String nextPageURL = "";
                if (nextPageLink != null) {
                    nextPageURL = nextPageLink.attr("abs:href");
                }
                pagesVisited.add(currentPageURL);

                Elements dvdLinks = document.select("p.tmb a[href*=/mono/dvd/]");

                for (Element dvdLink : dvdLinks) {
                    String currentLink = dvdLink.attr("abs:href");
                    if (!currentLink.matches(".*dod/.*")) {
                        VirtualEntry virtualEntry = new VirtualEntry();
                        virtualEntry.set(currentLink);

                        var currentList = Systems.getDirectorySystem().getDirectoryEntries();
                        boolean isAvailable = false;
                        for (DirectoryEntry d : currentList) {
                            if (d.hasNfo()) {
                                if (d.getMovieData().getId().getId().equals(virtualEntry.getMovieData().getId().getId())) {
                                    isAvailable = true;
                                    break;
                                }
                            }
                        }

                        if (!isAvailable) {
                            Platform.runLater(() -> {
                                Systems.getDirectorySystem().getDirectoryEntries().add(virtualEntry);
                                Systems.getDirectorySystem().sortContain();
                            });
                        }
                    }
                }

                if (nextPageLink != null && !pagesVisited.contains(nextPageURL)) {
                    document = SiteParsingProfile.downloadDocumentFromURLString(nextPageURL);
                } else {
                    break;
                }

            }
        });

        */
    }
}
