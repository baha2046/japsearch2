package org.nagoya.controls;

import com.jfoenix.controls.JFXButton;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nagoya.GUICommon;
import org.nagoya.fx.scene.FXFactory;
import org.nagoya.io.WebServiceRun;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.system.Systems;
import org.nagoya.system.database.MovieDB;
import org.nagoya.view.customcell.ActorDBListCell;

import java.util.List;

public class FXActressDatabaseControl extends HBox {
    private static Try<Document> document1 = null;
    private static Try<Document> document2 = null;

    public FXActressDatabaseControl() {
        String url1 = "https://www.dmm.co.jp/mono/dvd/-/exclusive-actress/";
        String url2 = "https://www.dmm.co.jp/mono/dvd/-/actress/";

        if (document1 == null) {
            document1 = Systems.getWebService().getDocument(url1, WebServiceRun.none, false);
        }

        FXListView<ActorV2> lvActors = new FXListView<>();
        lvActors.setMinSize(500, 500);
        lvActors.setCellFactory((ListView<ActorV2> l) -> new ActorDBListCell());
        VBox vBox0 = GUICommon.vbox(4);

        ObservableList<ActorV2> actorObservableList = FXCollections.observableArrayList(ActorV2.extractor());
        lvActors.setItems(actorObservableList);

        Stream<String> makerSteam = document1
                .map(d -> d.select("div[class=capt-exclusive]"))
                .map(Stream::ofAll)
                .getOrElse(Stream.empty())
                .map(e -> e.attr("id"));
        Stream<List<ActorV2>> actorSteam1 = getActorListStream(document1);

        makerSteam.forEachWithIndex((m, i) -> {
            JFXButton button = getButton(m, actorSteam1.get(i), actorObservableList);
            vBox0.getChildren().add(button);
        });

        if (document2 == null) {
            document2 = Systems.getWebService().getDocument(url2, WebServiceRun.none, false);
        }

        Stream<List<ActorV2>> actorSteam2 = getActorListStream(document2);
        if (actorSteam2.length() >= 2) {
            JFXButton button1 = getButton("新人AV女優", actorSteam2.get(0), actorObservableList);
            JFXButton button2 = getButton("おすすめAV女優", actorSteam2.get(1), actorObservableList);
            vBox0.getChildren().addAll(new Separator(), button1, button2);
        }

        VBox vBox1 = GUICommon.vbox(4,
                getButtonLocal("あ", actorObservableList),
                getButtonLocal("い", actorObservableList),
                getButtonLocal("う", actorObservableList),
                getButtonLocal("え", actorObservableList),
                getButtonLocal("お", actorObservableList),
                new Separator(),
                getButtonLocal("か", actorObservableList),
                getButtonLocal("き", actorObservableList),
                getButtonLocal("く", actorObservableList),
                getButtonLocal("け", actorObservableList),
                getButtonLocal("こ", actorObservableList)
        );

        VBox vBox2 = GUICommon.vbox(4,
                getButtonLocal("さ", actorObservableList),
                getButtonLocal("し", actorObservableList),
                getButtonLocal("す", actorObservableList),
                getButtonLocal("せ", actorObservableList),
                getButtonLocal("そ", actorObservableList),
                new Separator(),
                getButtonLocal("た", actorObservableList),
                getButtonLocal("ち", actorObservableList),
                getButtonLocal("つ", actorObservableList),
                getButtonLocal("て", actorObservableList),
                getButtonLocal("と", actorObservableList)
        );

        VBox vBox3 = GUICommon.vbox(4,
                getButtonLocal("な", actorObservableList),
                getButtonLocal("に", actorObservableList),
                getButtonLocal("ぬ", actorObservableList),
                getButtonLocal("ね", actorObservableList),
                getButtonLocal("の", actorObservableList),
                new Separator(),
                getButtonLocal("は", actorObservableList),
                getButtonLocal("ひ", actorObservableList),
                getButtonLocal("ふ", actorObservableList),
                getButtonLocal("へ", actorObservableList),
                getButtonLocal("ほ", actorObservableList)
        );

        VBox vBox4 = GUICommon.vbox(4,
                getButtonLocal("ま", actorObservableList),
                getButtonLocal("み", actorObservableList),
                getButtonLocal("む", actorObservableList),
                getButtonLocal("め", actorObservableList),
                getButtonLocal("も", actorObservableList),
                new Separator(),
                getButtonLocal("や", actorObservableList),
                getButtonLocal("ゆ", actorObservableList),
                getButtonLocal("よ", actorObservableList)
        );

        VBox vBox5 = GUICommon.vbox(4,
                getButtonLocal("ら", actorObservableList),
                getButtonLocal("り", actorObservableList),
                getButtonLocal("る", actorObservableList),
                getButtonLocal("れ", actorObservableList),
                getButtonLocal("ろ", actorObservableList),
                new Separator(),
                getButtonLocal("わ", actorObservableList),
                getButtonLocal("ん", actorObservableList)
        );

        lvActors.setOnMouseClicked((e) -> {
            FXActorDetailWindow.show(lvActors.getSelectionModel().getSelectedItem());
        });

        this.setSpacing(10);
        this.getChildren().setAll(vBox0, vBox5, vBox4, vBox3, vBox2, vBox1, lvActors);
        this.setPadding(new Insets(35, 15, 15, 15));
    }

    public HBox show() {
        return this;
    }

    private static Stream<List<ActorV2>> getActorListStream(@NotNull Try<Document> document) {
        return document.map(d -> d.select("div[class=act-box]"))
                .map(Stream::ofAll)
                .getOrElse(Stream.empty())
                .map(e -> e.select("li a"))
                .map(Stream::ofAll)
                .map(es -> es.map(Element::text)
                        .filter(s -> !s.isEmpty())
                        .map(ActorV2::of)
                        .asJava());
        //.map(FXCollections::observableList);
    }

    @NotNull
    private static JFXButton getButton(String str, List<ActorV2> list, ObservableList<ActorV2> observableList) {
        JFXButton button = FXFactory.buttonWithBorder(str, (e) -> {
            observableList.clear();
            observableList.addAll(list);
        });

        button.setMinWidth(140);
        return button;
    }

    @NotNull
    private static JFXButton getButtonLocal(String str, ObservableList<ActorV2> observableList) {
        JFXButton button = FXFactory.buttonWithBorder(str, (e) -> {
            observableList.clear();
            Systems.useExecutors(() -> {
                Seq<ActorV2> list = MovieDB.actorDB()
                        .getData(a -> a.getYomi().startsWith(str))
                        .sorted(ActorV2.ACTOR_COMPARATOR)
                        .peek(a -> {
                            if (a.getActorDmmID().getWebID().equals(Option.none())) {
                                Systems.useExecutors(() -> a.updateWebID(false));
                            }
                        });
                Platform.runLater(() -> observableList.addAll(list.asJava()));
            });
        });

        button.setMinWidth(40);
        return button;
    }
}
