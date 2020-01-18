package org.nagoya.controller.mangaparsingprofile;

import com.jfoenix.controls.JFXTextField;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.nagoya.GUICommon;
import org.nagoya.system.Systems;
import org.nagoya.system.dialog.DialogBuilder;

public class MoeeroParser extends BaseParser {
    public MoeeroParser() {

    }

    @Override
    public void loadSet(String fromURL) {
        Option<Document> document = this.downloadDocument(fromURL);

        if (document.isEmpty()) {
            return;
        }

        Systems.useExecutors(() -> {

            Element mainContent = document.get().select("div[class=kijibox]").first();
            Elements imageLinks = mainContent.select("p a");

            Stream<String> imageList = Stream.ofAll(imageLinks.stream().map(i -> i.attr("href")));

            Text txtInfo = new Text("Please provide the following info :");
            JFXTextField txtAuthor = GUICommon.textField("A", 550);
            JFXTextField txtTitle = GUICommon.textField("T", 550);
            VBox vBox = GUICommon.vbox(15, txtInfo, new Separator(), txtAuthor, txtTitle);

            Runnable download = () -> {
                if (!txtTitle.getText().equals("")) {
                    Systems.useExecutors(() -> {
                        this.setTypeString(T_Dou);
                        this.setTitleString(txtTitle.getText());
                        this.setAuthorString(txtAuthor.getText());
                        this.resolveOutputPath();
                        this.writeImages(imageList);
                    });
                }
            };

            DialogBuilder.create()
                    .heading("[ Download ]")
                    .body(vBox)
                    .buttonYesNo(download)
                    .build().show();
        });

        //imageLinks.forEach(e -> GUICommon.debugMessage(e.attr("href")));

    }


}
