package org.nagoya.view.dialog;

import com.jfoenix.controls.JFXButton;
import io.vavr.Tuple2;
import io.vavr.Value;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextField;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.nagoya.GUICommon;
import org.nagoya.controller.siteparsingprofile.specific.JavBusParsingProfile;
import org.nagoya.controls.FXWebViewControl;
import org.nagoya.controls.FXWebViewWindow;
import org.nagoya.io.WebServiceRun;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.system.Systems;
import reactor.core.publisher.Mono;

public class FXMovieJavBusGridWindow extends FXMovieGridWindow {
    private static FXMovieJavBusGridWindow INSTANCE = null;

    public static FXMovieJavBusGridWindow getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FXMovieJavBusGridWindow();
        }
        return INSTANCE;
    }

    public static void urlAndShow(String strUrl) {
        getInstance().clear();
        getInstance().searchUrl(strUrl);
        getInstance().displayWindow("Jav Bus Info");
    }

    public static void searchAndShow(String strSearch) {
        getInstance().clear();
        getInstance().searchKeyword(strSearch);
        getInstance().displayWindow("Jav Bus Search");
    }

    private final TextField txtSelected;
    private final JavBusParsingProfile javBusParsingProfile;

    FXMovieJavBusGridWindow() {
        super();
        this.javBusParsingProfile = new JavBusParsingProfile();
        this.txtSelected = GUICommon.textFieldWithBorder("", 250);

        ChangeListener<SimpleMovieData> movieDataConsumer = (ov, o, n) -> {
            var m = Option.of(n);
            this.txtSelected.setText(m.map(SimpleMovieData::getStrUrl).getOrElse(""));
        };

        this.gridView.selectionProperty().addListener(movieDataConsumer);
        //listView.setItems(this.movieDataObservableList);

        JFXButton btnBrowse = GUICommon.buttonWithBorder("  Browse  ", (e) -> {
            String movieUrl = this.txtSelected.getText();
            if (!movieUrl.equals("")) {
                FXWebViewWindow.show(movieUrl, Option.none(), Option.of(FXWebViewControl::modifyJavBus));
            }
        });

        JFXButton btnSeed = GUICommon.buttonWithBorder("  Seed  ", (e) -> {
            String movieUrl = this.txtSelected.getText();
            if (!movieUrl.equals("")) {
                FXSeedListDialog.show(movieUrl, Option.none());
            }
        });

        btnSeed.disableProperty().bind(Systems.getWebService().isWorkingProperty());

        this.extraBar.getChildren().addAll(this.txtSelected, btnBrowse, btnSeed);
    }

    public JavBusParsingProfile getParsingProfile() {
        return this.javBusParsingProfile;
    }

    private void searchKeyword(String search) {
        this.searchUrl(this.javBusParsingProfile.createSearchString(search));
    }

    private void searchUrl(String strUrl) {
        this.pagination.setPageCount(1);
        Mono<Document> document = this.javBusParsingProfile.downloadSearchAllDocument(strUrl);
        document.subscribe(this::parseDocument);
    }

    @Override
    public void clear() {
        super.clear();
        GUICommon.runOnFx(() -> {
            this.txtSelected.setText("");
        });
    }

    private void parseDocument(@NotNull Document document) {
        this.javBusParsingProfile.setDocument(document);

        var page0 = this.javBusParsingProfile.parseMovieList().toVector();
        Vector<Tuple2<String, String>> pageList = this.javBusParsingProfile.parsePageLinks();

        //pageList.forEach(v -> GUICommon.debugMessage(v._2));

        Vector<Mono<Vector<SimpleMovieData>>> list = pageList
                .map(data -> Systems.getWebService()
                        .getDocumentAsync(data._2, WebServiceRun.none, true)
                        .map(d -> (JavBusParsingProfile) this.javBusParsingProfile.setDocument(d))
                        .map(JavBusParsingProfile::parseMovieList)
                        .map(Value::toVector)
                        .cache()
                );

        Vector<Mono<Vector<SimpleMovieData>>> finalList = Vector.of(Mono.just(page0)).appendAll(list);

        finalList.forEach(v -> GUICommon.debugMessage(v.toString()));
        GUICommon.runOnFx(() -> {
            this.pagination.setPageCount(finalList.size());
            this.pagination.setPageFactory((i) -> {
                this.currentPage = i;
                this.clear();
                finalList.get(i).subscribe(this::showPage);
            });

            this.pagination.setCurrentPageIndex(0);
        });
    }
}

