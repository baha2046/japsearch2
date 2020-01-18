package org.nagoya.controller.siteparsingprofile.specific;

import io.vavr.collection.Stream;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controller.languagetranslation.Language;
import org.nagoya.controller.siteparsingprofile.SiteParsingProfile;
import org.nagoya.io.WebServiceRun;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.model.dataitem.*;
import org.nagoya.preferences.GeneralSettings;
import org.nagoya.system.Systems;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ArzonParsingProfile extends SiteParsingProfile implements SpecificProfile {
    private final boolean doGoogleTranslation;
    private URL refererURL;

    public ArzonParsingProfile() {
        super();
        this.doGoogleTranslation = (this.scrapingLanguage == Language.ENGLISH);
    }

    public ArzonParsingProfile(Document document) {
        super(document);
        this.doGoogleTranslation = (this.scrapingLanguage == Language.ENGLISH);
    }

    /**
     * Default constructor does not define a document, so be careful not to call
     * scrape methods without initializing the document first some other way.
     * This constructor is mostly used for calling createSearchString() and
     * getSearchResults()
     */
    public ArzonParsingProfile(boolean doGoogleTranslation) {
        super();
        this.doGoogleTranslation = doGoogleTranslation;
        if (this.doGoogleTranslation == false) {
            this.setScrapingLanguage(Language.JAPANESE);
        }
    }

    public ArzonParsingProfile(boolean doGoogleTranslation, boolean scrapeTrailers) {
        super();
        this.doGoogleTranslation = doGoogleTranslation;
        if (this.doGoogleTranslation == false) {
            this.setScrapingLanguage(Language.JAPANESE);
        }
    }

    public ArzonParsingProfile(Document document, boolean doGoogleTranslation) {
        super(document);
        this.doGoogleTranslation = doGoogleTranslation;
        if (this.doGoogleTranslation == false) {
            this.setScrapingLanguage(Language.JAPANESE);
        }
    }

    public static String fixUpIDFormatting(String idElementText) {
        // remove useless word
        if (idElementText.contains("廃盤")) {
            idElementText = idElementText.substring(0, idElementText.indexOf('廃') - 1);
        }
        return idElementText;
    }

    public static String parserName() {
        return "arzon.jp";
    }

    @Override
    public List<ScraperGroupName> getScraperGroupNames() {
        if (this.groupNames == null) {
            this.groupNames = Arrays.asList(ScraperGroupName.JAV_CENSORED_SCRAPER_GROUP);
        }
        return this.groupNames;
    }

    @Override
    public String scrapeTitle() {
        Element titleElement = this.document.select(ArzonCSSQuery.Q_TITLE).first();

        if (titleElement != null) {
            return titleElement.text().trim();
        } else {
            return "";
        }
    }

    @Override
    public String scrapeSet() {
        return this.defaultScrapeSet(ArzonCSSQuery.Q_SET);
    }

    @Override
    public Option<ReleaseDate> scrapeReleaseDate() {
        Element releaseDateElement = this.document.select(ArzonCSSQuery.Q_RDATE).first();

        if (releaseDateElement != null) {
            String releaseDate = releaseDateElement.text();
            //we want to convert something like 2015/04/25 to 2015-04-25
            releaseDate = StringUtils.replace(releaseDate, "/", "-");
            releaseDate = releaseDate.substring(0, 10);

            System.out.println("scrapeReleaseDate() >> " + releaseDate);

            return Option.of(new ReleaseDate(releaseDate));
        }
        return Option.none();
    }

    @Override
    public String scrapePlot() {
        Element plotElement = this.document.select(ArzonCSSQuery.Q_PLOT).first();
        return (plotElement == null) ? "" : plotElement.text().substring(5);
    }

    @Override
    public String scrapeRuntime() {
        return this.defaultScrapeRuntime(ArzonCSSQuery.Q_TIME);
    }

    @Override
    public Trailer scrapeTrailer() {
        return Trailer.BLANK_TRAILER;
    }

    @Override
    public Option<FxThumb> scrapeCover() {
        return this.defaultScrapeCover(ArzonCSSQuery.Q_COVER);
    }

    @Override
    public Stream<FxThumb> scrapeExtraImage() {
        Elements extraArtElements = this.document.select(ArzonCSSQuery.Q_THUMBS);

        if (null == extraArtElements) {
            return Stream.empty();
        }

        return Stream.ofAll(extraArtElements.stream())
                .map((ex) -> ex.attr("abs:href"))
                .filter(Objects::nonNull)
                .peek((link) -> System.out.println("scrapeExtraArt() >> " + link))
                .map(FxThumb::of)
                .flatMap(i -> i);
    }

    @Override
    public ID scrapeID() {
        Element idElement = this.document.select(ArzonCSSQuery.Q_ID).first();
        if (idElement != null) {
            String idElementText = idElement.text();
            idElementText = fixUpIDFormatting(idElementText);
            return new ID(idElementText);
        }
        //This page didn't have an ID, so just put in a empty one
        else {
            return ID.BLANK_ID;
        }
    }

    @Override
    public ArrayList<Genre> scrapeGenres() {
        int genre_num = 0;

        Element genre = this.document.select(ArzonCSSQuery.Q_GENRES).first();
        Elements genreElements = genre.select("ul");

        // Arzon genres divided into parent and child cat.,
        // if there has a child cat. add the child otherwise add the parent cat.
        ArrayList<String> genresList = new ArrayList<>(genreElements.size());
        for (Element genreElement : genreElements) {
            Element currentElement = genreElement.select("li[class=child]").first();

            if (currentElement != null) {
                String genreStr = currentElement.text();

                if (genreStr.contains("その他")) {
                    genreStr = genreStr.replaceAll("その他", "");
                }

                genresList.add(genreStr);
                genre_num++;
            } else {
                currentElement = genreElement.select("li[class=parent]").first();
                if (currentElement != null) {
                    //system.out.println("scrapeGenres" + currentElement.getLabel());
                    genresList.add(currentElement.text());
                    genre_num++;
                }
            }
        }

        ArrayList<Genre> genresReturn = new ArrayList<>(genre_num);
        for (int x = 0; x < genre_num; x++) {
            genresReturn.add(new Genre(genresList.get(x)));
        }
        return genresReturn;
    }

    public Future<List<ActorV2>> scrapeActorsAsync() {
        return Future.of(ExecuteSystem.getExecutorServices(ExecuteSystem.role.NORMAL), () ->
        {
            List<ActorV2> observableList = new ArrayList<>();

            Element actressIDElements = this.document.select(ArzonCSSQuery.Q_ACTORS).first();
            Elements actressURL = actressIDElements.select("a");

            for (Element actressIDLink : actressURL) {
                String actressIDHref = actressIDLink.attr("abs:href");
                String actressName = actressIDLink.text();

                ActorV2 actor = DmmParsingProfile.scrapeActorThumbFromDmm(actressName);

                GUICommon.debugMessage("scrapeActorsAsync() >> " + actressName + " " + actressIDHref);

                observableList.add(actor);
            }

            return observableList;
        });
    }

    public Flux<ActorV2> scrapeActorStream() {
        Element actressIDElements = this.document.select(ArzonCSSQuery.Q_ACTORS).first();

        if (actressIDElements != null) {
            Elements actressURL = actressIDElements.select("a");

            return Flux.fromStream(actressURL.stream())
                    .map(Element::text)
                    .map(DmmParsingProfile::scrapeActorThumbFromDmm)
                    //.subscribeOn(Systems.getExecuteSystem().getNormalScheduler())
                    ;
        }

        return Flux.empty();
    }

    @Override
    public Mono<List<ActorV2>> scrapeActorsMono() {
        return this.scrapeActorStream()
                .collectList();
    }

    @Override
    public String scrapeDirector() {
        return this.defaultScrapeDirectors(ArzonCSSQuery.Q_DIRECTOR);
    }

    @Override
    public String scrapeStudio() {
        return this.defaultScrapeStudio(ArzonCSSQuery.Q_STUDIO);
    }

    @Override
    public String scrapeMaker() {
        return this.defaultScrapeMaker(ArzonCSSQuery.Q_MAKER);
    }

    @Override
    public String createSearchString(String searchStr) {

        return Try.of(() -> new URLCodec().encode(searchStr))
                .flatMap(s -> Try.of(() -> new URL("https://www.arzon.jp/itemlist.html?t=&agecheck=1&m=all&s=&q=" + s)))
                .peek(url -> this.refererURL = url)
                .map(URL::toString)
                .getOrElse("");
    }


    /**
     * returns a String[] filled in with urls of each of the possible movies
     * found on the page returned from createSearchString
     */
    @Override
    public Try<Stream<SimpleMovieData>> getSearchResults(String searchString) {

        System.out.println(">> getSearchResults " + searchString);

        return Systems.getWebService().getDocument(searchString, WebServiceRun.none, false)
                .map(d -> d.select("dl.hentry"))
                .filter(Objects::nonNull)
                .map(elements -> Stream.ofAll(elements)
                        .map(this::getSingleResult)
                        .filter(Objects::nonNull)
                );
    }

    private SimpleMovieData getSingleResult(Element videoLink) {
        String currentLink = videoLink.select("a").attr("abs:href");
        String currentLinkLabel = videoLink.select("a").attr("title");
        String currentLinkImage = videoLink.select("img").attr("abs:src");

        if (currentLink.length() > 1) {
            SimpleMovieData movieData = new SimpleMovieData("", currentLinkLabel, FxThumb.of(currentLinkImage));
            movieData.setStrUrl(currentLink);
            return movieData;
        }
        return null;
    }

    @Override
    public String toString() {
        return "arzon.jp";
    }

    @Override
    public SiteParsingProfile newInstance() {
        GeneralSettings preferences = Systems.getPreferences();
        return new ArzonParsingProfile(!preferences.getScrapeInJapanese());
    }

    @Override
    public String getParserName() {
        return parserName();
    }

}
