package org.nagoya.controller.mangaparsingprofile;

import com.jfoenix.controls.JFXTextField;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.view.dialog.FXProgressDialog;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;

public class ImagebamParser extends BaseParser {

    public ImagebamParser() {

    }

    @Override
    public void loadSet(String fromURL) {
        Option<Document> document = this.downloadDocument(fromURL);

        if (document.isEmpty()) {
            return;
        }

        ExecuteSystem.useExecutors(ExecuteSystem.role.IMAGE, () -> {

            Elements imgElements = document.get().selectFirst("fieldset").select("a");

            Stream<String> imageList = Stream.ofAll(imgElements.stream().map(e -> e.attr("href")));

            String strWorkType = T_Misc;

            JFXTextField txtTitle = GUICommon.textField("", 550);

            GUICommon.showDialog("[Download]", txtTitle, "Cancel", "Confirm", () -> {

                FXProgressDialog.getInstance().startProgressDialog();

                if (!txtTitle.getText().equals("")) {

                    ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, () -> {

                        this.setTypeString(strWorkType);
                        Path outImageDirectory = this.getTypePath().resolve(txtTitle.getText().trim() + " (" + imageList.length() + ")");

                        Stream<String> thumbList = imageList
                                .flatMap(this::downloadDocument)
                                .map(d -> d.selectFirst("div[class=image-container] img"))
                                .map(e -> e.attr("src"))
                                .peek(GUICommon::debugMessage);

                        UtilCommon.tryCreateDirectory(outImageDirectory)
                                .recover(FileAlreadyExistsException.class, outImageDirectory)
                                .onSuccess(s -> this.batchDownloadUrlToFile(this.buildDownloadStream(thumbList, outImageDirectory)))
                                .onFailure(GUICommon::errorDialog);
                    });
                }
            });
        });
    }


}
