package org.nagoya.controller.siteparsingprofile.specific;


import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.codec.net.URLCodec;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controller.languagetranslation.JapaneseCharacter;
import org.nagoya.controller.languagetranslation.Language;
import org.nagoya.controller.languagetranslation.TranslateString;
import org.nagoya.controller.siteparsingprofile.SiteParsingProfile;
import org.nagoya.io.WebServiceRun;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.model.dataitem.*;
import org.nagoya.system.Systems;
import org.openqa.selenium.By;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavBusParsingProfile extends SiteParsingProfile implements SpecificProfile {

    private static final String urlLanguageEnglish = "en";
    private static final String urlLanguageJapanese = "ja";
    //JavBus divides movies into two categories - censored and uncensored.
    //All censored movies need cropping of their poster
    private boolean isCensoredSearch = true;
    private Document japaneseDocument;

    @NotNull
    @Contract(pure = true)
    public static String parserName() {
        return "JavBus.com";
    }

    @Override
    public List<ScraperGroupName> getScraperGroupNames() {
        if (this.groupNames == null) {
            this.groupNames = Collections.singletonList(ScraperGroupName.JAV_CENSORED_SCRAPER_GROUP);
        }
        return this.groupNames;
    }

    private void initializeJapaneseDocument() {
        if (this.japaneseDocument == null) {
            String urlOfCurrentPage = this.document.location();
            if (urlOfCurrentPage != null && urlOfCurrentPage.contains("/en/")) {
                //the genres are only available on the japanese version of the page
                urlOfCurrentPage = urlOfCurrentPage.replaceFirst(Pattern.quote("//www.javbus.com/en/"), "//www.javbus.com/ja/");
                if (urlOfCurrentPage.length() > 1) {
                    this.japaneseDocument = Systems.getWebService().getDocument(urlOfCurrentPage, WebServiceRun.none, false).getOrNull();
                }
            } else if (this.document != null) {
                this.japaneseDocument = this.document;
            }
        }
    }

    @Override
    public String scrapeTitle() {
        Element titleElement = this.document.select(JavBusCSSQuery.Q_TITLE).first();
        if (titleElement != null) {
            String titleText = titleElement.text();
            titleText = titleText.replace("- JavBus", "");
            //Remove the ID from the front of the title
            if (titleText.contains(" ")) {
                titleText = titleText.substring(titleText.indexOf(" "));
            }
            //Translate the element using google translate if needed
            if (this.scrapingLanguage == Language.ENGLISH && JapaneseCharacter.containsJapaneseLetter(titleText)) {
                titleText = TranslateString.translateStringJapaneseToEnglish(titleText);
            }
            return titleText;
        } else {
            return "";
        }
    }


    @Override
    public String scrapeSet() {
        String seriesWord = (this.scrapingLanguage == Language.ENGLISH) ? "Series:" : "シリーズ:";
        return this.defaultScrapeSet("span.header:containsOwn(" + seriesWord + ") ~ a");
    }

    @Override
    public Option<ReleaseDate> scrapeReleaseDate() {
        String releaseDateWord = (this.scrapingLanguage == Language.ENGLISH) ? "Release Date:" : "発売日:";

        return this.SelectFirstElement("p:contains(" + releaseDateWord + ")")
                .filter(e -> e.ownText().trim().length() > 4)
                .map(e -> e.ownText().trim())
                .map(ReleaseDate::of);
    }

    @Override
    public String scrapePlot() {
        return "";
    }

    @Override
    public String scrapeRuntime() {
        String lengthWord = (this.scrapingLanguage == Language.ENGLISH) ? "Length:" : "収録時間:";

        return this.SelectFirstElement("p:contains(" + lengthWord + ")")
                .map(e -> e.ownText().trim().replace("min", ""))
                .map(s -> s.replace("分", ""))
                .getOrElse("");
    }

    @Override
    public Option<FxThumb> scrapeCover() {
        return this.defaultScrapeCover(JavBusCSSQuery.Q_COVER);
    }

    @Override
    public Stream<FxThumb> scrapeExtraImage() {
        Elements extraImageElements = this.document.select(JavBusCSSQuery.Q_THUMBS);

        if (extraImageElements == null || extraImageElements.size() > 20) {
            return Stream.empty();
        }

        return Stream.ofAll(extraImageElements.stream())
                .map((ex) -> ex.attr("href"))
                .filter(Objects::nonNull)
                .peek((link) -> System.out.println("scrapeExtraArt() >> " + link))
                .map(FxThumb::of)
                .flatMap(Option::toStream);
    }

    @Override
    public ID scrapeID() {
        return this.SelectFirstElement(JavBusCSSQuery.Q_ID)
                .map(Element::text)
                .map(ID::of).getOrElse(ID.BLANK_ID);
    }

    @Override
    public ArrayList<Genre> scrapeGenres() {
        Elements genreElements = this.document.select(JavBusCSSQuery.Q_GENRES);

        return genreElements.stream()
                .map(Element::text)
                .filter(this::isAcceptGenre)
                .map(Genre::new)
                .collect(Collectors.toCollection(ArrayList::new));


        /*if (genreElements != null) {
            for (Element genreElement : genreElements) {
                String genreText = genreElement.text();
                if (genreElement.text().length() > 0) {
                    //some genre elements are untranslated, even on the english site, so we need to do it ourselves
                    if (this.scrapingLanguage == Language.ENGLISH && JapaneseCharacter.containsJapaneseLetter(genreText)) {
                        genreText = TranslateString.translateStringJapaneseToEnglish(genreText);
                    }
                    genreList.add(new Genre(WordUtils.capitalize(genreText)));
                }
            }
        }
        return genreList;*/
    }

    @Contract(pure = true)
    private boolean isAcceptGenre(@NotNull String strGenre) {
        switch (strGenre) {
            case "ハイビジョン": // "DVD Toaster" WTF is this? Nuke it!
            case "独占配信":
            case "":
                return false;
            default:
                break;
        }
        return true;
    }

    public Flux<ActorV2> scrapeActorStream() {
        Elements actorElements = this.document.select(JavBusCSSQuery.Q_ACTORS);

        return Flux.fromStream(actorElements.stream())
                .map(e -> e.select("a").first())
                .filter(Objects::nonNull)
                .map(this::getOrCreateActor)
                //.subscribeOn(Systems.getExecuteSystem().getNormalScheduler())
                ;
    }

    public Future<List<ActorV2>> scrapeActorsAsync() {
        return Future.of(ExecuteSystem.getExecutorServices(ExecuteSystem.role.NORMAL), () ->
                this.scrapeActorStream().collectList().block());
    }

    @Override
    public Mono<List<ActorV2>> scrapeActorsMono() {
        return this.scrapeActorStream()
                .collectList();
    }

    private ActorV2 getOrCreateActor(@NotNull Element actor) {
        String url = actor.attr("href");
        String actorName = actor.select("img").attr("title");
        String actorImage = actor.select("img").attr("src");
        if (actorImage.contains("printing.gif") || !fileExistsAtURL(actorImage)) {
            actorImage = "";
        }
        return ActorV2.of(actorName, ActorV2.Source.JAVBUS, url, actorImage, "");
    }

    public static String scrapeActorPageURL(@NotNull String actorName) {
        String cleanName = actorName;

        int idx = actorName.indexOf("（");
        if (idx != -1) {
            cleanName = actorName.substring(0, idx);
        }

        String javUrl = "https://www.javbus.com/searchstar/" + cleanName;

        return Systems.getWebService().getDocument(javUrl, WebServiceRun.none, false)
                .map(d -> d.select("a[class=avatar-box text-center]").first())
                .filter(Objects::nonNull)
                .map(e -> e.attr("href"))
                .getOrElse("");
    }

    @Override
    public String scrapeDirector() {
        String directorWord = (this.scrapingLanguage == Language.ENGLISH) ? "Director:" : "監督:";
        return this.defaultScrapeDirectors("span.header:containsOwn(" + directorWord + ") ~ a");
    }

    @Override
    public String scrapeStudio() {
        String studioWord = (this.scrapingLanguage == Language.ENGLISH) ? "Studio:" : "レーベル:";
        return this.defaultScrapeStudio("span.header:containsOwn(" + studioWord + ") ~ a");
    }

    @Override
    public String scrapeMaker() {
        String makerWord = (this.scrapingLanguage == Language.ENGLISH) ? "Studio:" : "メーカー:";
        return this.defaultScrapeMaker("span.header:containsOwn(" + makerWord + ") ~ a");
    }


    @Override
    public String createSearchString(String searchStr) {
        return Try.of(() -> new URLCodec().encode(searchStr))
                .map(s -> "https://www.javbus.com/" + this.getUrlLanguageToUse() + "/search/" + s)
                .getOrElse("");
    }

    @Contract(pure = true)
    private String getUrlLanguageToUse() {
        return (this.scrapingLanguage == Language.ENGLISH) ? urlLanguageEnglish : urlLanguageJapanese;
    }

    public String getUrlFromID(String movieID) {
        return "http://www.javbus.com/" + this.getUrlLanguageToUse() + "/" + movieID;
    }

    public Mono<Document> downloadSearchAllDocument(@NotNull String strUrl) {
        WebServiceRun run = new WebServiceRun();
        run.setConsumerCdp4j((s) -> s.click("//*[@id='resultshowall']").wait(1000));
        run.setConsumerSelenium((w) -> w.findElement(By.id("resultshowall")).click());

        return Systems.getWebService().getDocumentAsync(strUrl, run, true);
    }

    @Override
    public SiteParsingProfile setDocument(Document document) {
        this.document = document;
        this.document.setBaseUri("https://www.javbus.com");
        return this;
    }

    @Override
    public Try<Stream<SimpleMovieData>> getSearchResults(String searchString) {

        System.out.println(">> getSearchResults " + searchString);

        return Systems.getWebService().getDocument(searchString, WebServiceRun.none, false)
                .map(d -> d.select("div.item"))
                .flatMap(videoLinksElements -> {
                    if (videoLinksElements.size() == 0) {
                        String secondPage = searchString.replace("/search/", "/uncensored/search/");
                        this.isCensoredSearch = false;
                        return Systems.getWebService().getDocument(secondPage, WebServiceRun.none, false)
                                .map(d -> d.select("div.item"));
                    } else {
                        return Try.of(() -> videoLinksElements);
                    }
                })
                .map(elements -> Stream.ofAll(elements)
                        .map(e -> e.selectFirst("a"))
                        .map(this::parseSimpleMovieData)
                        .filter(Objects::nonNull)
                );
    }

    @Override
    public SiteParsingProfile newInstance() {
        return new JavBusParsingProfile();
    }

    @Override
    public String getParserName() {
        return parserName();
    }

    public Stream<SimpleMovieData> parseMovieList() {
        Elements elements = this.document.select("a[class=movie-box]");
        GUICommon.debugMessage("parseMovieList >> Size >> " + elements.size());

        return Stream.ofAll(elements)
                .map(this::parseSimpleMovieData)
                .filter(Objects::nonNull);
    }

    @Nullable
    private SimpleMovieData parseSimpleMovieData(@NotNull Element movieElement) {
        String strUrl = movieElement.attr("href");
        Option<Element> imgElement = Option.of(movieElement.select("div[class=photo-frame] img").first());
        String strTitle = imgElement.map(e -> e.attr("title").trim()).getOrElse("");
        Option<FxThumb> fxThumb = imgElement.flatMap(e -> FxThumb.of(e.attr("src")));
        String strId = Option.of(movieElement.select("div[class=photo-info] span date").first()).map(Element::text).getOrElse("");

        if (strId.equals("")) {
            return null;
        }

        SimpleMovieData movieData = new SimpleMovieData(strId, strTitle, fxThumb);
        movieData.setStrUrl(strUrl);

        return movieData;
    }

    public Vector<Tuple2<String, String>> parsePageLinks() {
        Option<Element> multiPageElement = Option.of(this.document.select("ul[class='pagination pagination-lg']").first());

        if (multiPageElement.isEmpty()) {
            GUICommon.debugMessage(this.document.toString());
        }

        GUICommon.debugMessage(multiPageElement.toString());

        return multiPageElement
                .map(element -> element.select("li"))
                .map(elements -> Vector.ofAll(elements)
                                .filter(e -> !e.attr("class").equals("active"))
                                .map(e -> e.select("a").first())
                                .filter(Objects::nonNull)
                                .peek(e -> GUICommon.debugMessage(e.toString()))
                                .filter(e -> !e.text().equals("下一頁"))
                                .map(e -> Tuple.of(e.text(), e.attr("abs:href")))
                        /*.collect(Collectors.toList())*/)
                .getOrElse(Vector.empty());
    }
}
