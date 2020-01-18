package org.nagoya.controller.siteparsingprofile.specific;

import io.vavr.Value;
import io.vavr.collection.Stream;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.controller.languagetranslation.Language;
import org.nagoya.controller.languagetranslation.TranslateString;
import org.nagoya.controller.siteparsingprofile.SiteParsingProfile;
import org.nagoya.io.WebServiceRun;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.model.dataitem.*;
import org.nagoya.preferences.GeneralSettings;
import org.nagoya.system.Systems;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DmmParsingProfile extends SiteParsingProfile implements SpecificProfile {

    final static double dmmMaxRating = 5.00;
    private final boolean doGoogleTranslation;
    private boolean scrapeTrailers;
    private boolean searchDVDOnly = false;

    public DmmParsingProfile() {
        super();
        this.doGoogleTranslation = (this.scrapingLanguage == Language.ENGLISH);
        this.scrapeTrailers = true;
    }

    public DmmParsingProfile(Document document) {
        super(document);
        this.doGoogleTranslation = (this.scrapingLanguage == Language.ENGLISH);
    }

    /**
     * Default constructor does not define a document, so be careful not to call
     * scrape methods without initializing the document first some other way.
     * This constructor is mostly used for calling createSearchString() and
     * getSearchResults()
     */
    public DmmParsingProfile(boolean doGoogleTranslation) {
        super();
        this.doGoogleTranslation = doGoogleTranslation;
        if (!this.doGoogleTranslation) {
            this.setScrapingLanguage(Language.JAPANESE);
        }
        this.scrapeTrailers = true;
    }

    public DmmParsingProfile(boolean doGoogleTranslation, boolean scrapeTrailers) {
        super();
        this.doGoogleTranslation = doGoogleTranslation;
        if (!this.doGoogleTranslation) {
            this.setScrapingLanguage(Language.JAPANESE);
        }
        this.scrapeTrailers = scrapeTrailers;
    }

    public DmmParsingProfile(Document document, boolean doGoogleTranslation) {
        super(document);
        this.doGoogleTranslation = doGoogleTranslation;
        if (!this.doGoogleTranslation) {
            this.setScrapingLanguage(Language.JAPANESE);
        }
    }

    public static String fixUpIDFormatting(String idElementText) {
        //DMM sometimes has a letter and underscore then followed by numbers. numbers will be stripped in the next step, so let's strip out the underscore prefix part of the string
        if (idElementText.contains("_")) {
            idElementText = idElementText.substring(idElementText.indexOf('_') + 1);
        }

        String orgString = idElementText;

        try {
            //DMM sometimes includes numbers before the ID, so we're going to strip them out to use
            //the same convention that other sites use for the id number
            idElementText = idElementText.substring(StringUtils.indexOfAnyBut(idElementText, "0123456789"));
            //Dmm has everything in lowercase for this field; most sites use uppercase letters as that follows what shows on the cover so will uppercase the string
            //English locale used for uppercasing just in case user is in some region that messes with the logic of this code...
            idElementText = idElementText.toUpperCase(Locale.ENGLISH);
            //insert the dash between the getLabel and number part
            int firstNumberIndex = StringUtils.indexOfAny(idElementText, "0123456789");
            idElementText = idElementText.substring(0, firstNumberIndex) + "-" + idElementText.substring(firstNumberIndex);
        } catch (IndexOutOfBoundsException e) {
            return orgString;
        }

        //remove extra zeros in case we of a 5 or 6 digit numerical part
        //(For example ABC-00123 will become ABC-123)
        Pattern patternID = Pattern.compile("([0-9]*\\D+)(\\d{5,6})");
        Matcher matcher = patternID.matcher(idElementText);
        String groupOne = "";
        String groupTwo = "";
        while (matcher.find()) {
            groupOne = matcher.group(1);
            groupTwo = matcher.group(2);
        }
        if (groupOne.length() > 0 && groupTwo.length() > 0) {
            groupTwo = String.format("%03d", Integer.parseInt(groupTwo));
            return groupOne + groupTwo;
        }

        if (idElementText.endsWith("SO")) {
            idElementText = idElementText.substring(0, idElementText.length() - 2);
        }
        return idElementText;
    }

    @NotNull
    @Contract(pure = true)
    public static String parserName() {
        return "DMM.co.jp";
    }

    @Override
    public List<ScraperGroupName> getScraperGroupNames() {
        if (this.groupNames == null) {
            this.groupNames = Collections.singletonList(ScraperGroupName.JAV_CENSORED_SCRAPER_GROUP);
        }
        return this.groupNames;
    }

    @Override
    public String scrapeTitle() {
        return this.SelectFirstElement(DmmCSSQuery.Q_TITLE)
                .map(e -> e.attr("content"))
                .getOrElse("");
    }

    @Override
    public String scrapeSet() {
        return this.defaultScrapeSet(DmmCSSQuery.Q_SET);
    }

    @Override
    public Rating scrapeRating() {
        Element ratingElement = this.document.select(".d-review__average strong").first();
        if (ratingElement != null) {
            return new Rating(dmmMaxRating, ratingElement.text().replace("点", ""));
        } else {
            return Rating.BLANK_RATING;
        }
    }

    @Override
    public Option<ReleaseDate> scrapeReleaseDate() {
        return this.defaultScrapeReleaseDate(DmmCSSQuery.Q_RDATE);
    }

    public Votes scrapeVotes() {
        Element votesElement = this.document.select(DmmCSSQuery.Q_VOTE).first();
        return (votesElement == null) ? Votes.BLANK_VOTES : new Votes(votesElement.text());
    }

    @Override
    public String scrapePlot() {

        //dvd mode
        Option<Element> plotElement = this.SelectFirstElement("p.mg-b20");

        if (plotElement.isEmpty() || this.document.baseUri().contains("/digital/video")) {
            // rental mode
            plotElement = this.SelectFirstElement("tbody .mg-b20.lh4");
        }

        return plotElement.map(Element::text).getOrElse("");
    }

    @Override
    public String scrapeRuntime() {
        return this.defaultScrapeRuntime(DmmCSSQuery.Q_TIME);
    }

    @Override
    public Trailer scrapeTrailer() {
	/*	try {
			//we can return no trailers if scraping trailers is not enabled or the page we are scraping does not have a button to link to the trailer
			Element buttonElement;

			if (scrapeTrailers && (buttonElement = document.select("a.d-btn[onclick*=sampleplay]").first()) != null) {
				system.out.println("There should be a trailer, searching now...");

				// First, scrape the contents of the 'play trailer' button action. It's a small ajax document containing
				// an iframe that hosts the flash video player. Then scrape that iframe contents obtaining trailer information.

				String playerPath = buttonElement.attr("onclick").replaceFirst("^.*sampleplay\\('([^']+).*$", "$1");
				playerPath = StringEscapeUtils.unescapeJava(playerPath);
				URL playerURL = new URI(document.location()).resolve(playerPath).toURL();
				Document playerDocument = Jsoup.parse(playerURL, CONNECTION_TIMEOUT_VALUE);
				URL iframeURL = new URL(playerDocument.select("iframe").first().attr("abs:src"));
				Document iframeDocument = Jsoup.parse(iframeURL, CONNECTION_TIMEOUT_VALUE);
				String flashPlayerScript = iframeDocument.select("script").last().data();
				Pattern pattern = Pattern.compile(".*flashvars.fid\\s*=\\s*\"([^\"]+).*flashvars.bid\\s*=\\s*\"(\\d)(w|s)\".*", Pattern.DOTALL);
				Matcher matcher = pattern.matcher(flashPlayerScript);

				if (matcher.matches()) {
					String cid = matcher.group(1);
					int bitrates = Integer.parseInt(matcher.group(2));
					String ratio = matcher.group(3);
					String quality = (bitrates & 0b100) != 0 ? "dmb" : (bitrates & 0b010) != 0 ? "dm" : "sm";
					String firstLetterOfCid = cid.substring(0, 1);
					String threeLetterCidCode = cid.substring(0, 3);

					String potentialTrailerURL = String.format("http://cc3001.dmm.co.jp/litevideo/freepv/%1$s/%2$s/%3$s/%3$s_%4$s_%5$s.mp4", firstLetterOfCid, threeLetterCidCode,
							cid, quality, ratio);

					if (SiteParsingProfile.fileExistsAtURL(potentialTrailerURL)) {
						system.out.println("Trailer existed at: " + potentialTrailerURL);
						return new Trailer(potentialTrailerURL);
					}
				}

				system.err.println("I expected to find a trailer and did not at " + document.location());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/

        return Trailer.BLANK_TRAILER;
    }

    @Override
    public Option<FxThumb> scrapeCover() {
        return this.defaultScrapeCover(DmmCSSQuery.Q_COVER);
    }

    @Override
    public String scrapeStudio() {
        return this.defaultScrapeStudio(DmmCSSQuery.Q_STUDIO);
    }

    @Override
    public String scrapeMaker() {
        return this.defaultScrapeMaker(DmmCSSQuery.Q_MAKER);
    }

    @Override
    public ID scrapeID() {
        return this.SelectFirstElement(DmmCSSQuery.Q_ID)
                .map(Element::text)
                .map(DmmParsingProfile::fixUpIDFormatting)
                .map(ID::of)
                .getOrElse(ID.BLANK_ID);
    }

    @Override
    public ArrayList<Genre> scrapeGenres() {
        Elements genreElements = this.document.select(DmmCSSQuery.Q_GENRES);
        ArrayList<Genre> genres = new ArrayList<>(genreElements.size());
        for (Element genreElement : genreElements) {
            // of the link so we can examine the id and do some sanity cleanup
            // and perhaps some better translation that what google has, if we
            // happen to know better
            String href = genreElement.attr("abs:href");
            String genreID = href.substring(href.indexOf("id=") + 3, href.length() - 1);
            if (this.isAcceptGenre(genreID)) {
                genres.add(new Genre(genreElement.text()));
            }
        }
        return genres;
    }

    // Return false on any genres we don't want scraping in. This can later be
    // something the user configures, but for now I'll use it
    // to of rid of weird stuff like DVD toaster
    // the genreID comes from the href to the genre keyword from DMM
    // Example: <a href="/mono/dvd/-/list/=/article=keyword/id=6004/">
    // The genre ID would be 6004 which is passed in as the String
    @Contract(pure = true)
    private boolean isAcceptGenre(@NotNull String genreID) {
        switch (genreID) {
            case "6529": // "DVD Toaster" WTF is this? Nuke it!
            case "6984":
            case "6983":
            case "6976":
                return false;
            case "6102": // "Sample Video" This is not a genre!
                return false;
            default:
                break;
        }
        return true;
    }

    public ArrayList<Actor> scrapeActors() {
        // scrape all the actress IDs
        Elements actressIDElements = this.document.select("span#performer a[href*=article=actress/id=]");
        ArrayList<Actor> actorList = new ArrayList<>(actressIDElements.size());
        List<Runnable> tasks = new ArrayList<>();
        for (Element actressIDLink : actressIDElements) {
            tasks.add(() ->
            {
                String actressIDHref = actressIDLink.attr("abs:href");
                String actressNameKanji = actressIDLink.text();
                System.out.println("scrapeActors = " + actressNameKanji);
                String actressID = actressIDHref.substring(actressIDHref.indexOf("id=") + 3, actressIDHref.length() - 1);
                String actressPageURL = "http://actress.dmm.co.jp/-/detail/=/actress_id=" + actressID + "/";
                try {
                    String actressThumbnailPath;
                    switch (actressID) {
                        case "23130"://川上ゆう
                            actressThumbnailPath = "http://pics.dmm.co.jp/mono/actjpgs/kawakami_yuu.jpg";
                            break;
                        case "30130"://大槻ひびき
                            actressThumbnailPath = "http://pics.dmm.co.jp/mono/actjpgs/ootuki_hibiki.jpg";
                            break;
                        default:
                            Document actressPage = Jsoup.connect(actressPageURL).timeout(SiteParsingProfile.CONNECTION_TIMEOUT_VALUE * 2).get();
                            //Element actressNameElement = actressPage.select("td.t1 h1").first();
                            Element actressThumbnailElement = actressPage.select("tr.area-av30.top td img").first();
                            actressThumbnailPath = actressThumbnailElement.attr("abs:src");
                            break;
                    }

                    if (!actressThumbnailPath.contains("nowprinting.gif")) {
                        actorList.add(new Actor(actressNameKanji, "", FxThumb.of(actressThumbnailPath).getOrNull()));
                    } else {
                        actorList.add(new Actor(actressNameKanji, "", null));
                    }

                } catch (SocketTimeoutException e) {
                    System.err.println("Cannot download from " + actressPageURL + ": Socket timed out: " + e.getLocalizedMessage());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            });
        }

        //Get actors that are just a "Name" and have no page of their own (common on some web releases)
        Elements nameOnlyActors = this.document.select("table.mg-b20 tr td:contains(�??�?：) + td");
        for (Element currentNameOnlyActor : nameOnlyActors) {
            String actorName = currentNameOnlyActor.text().trim();
            //for some reason, they sometimes list the age of the person after their name, so let's of rid of that
            actorName = actorName.replaceFirst("\\([0-9]{2}\\)", "");
            if (this.doGoogleTranslation) {
                actorName = TranslateString.translateJapanesePersonNameToRomaji(actorName);
            }
            actorList.add(new Actor(actorName, "", null));
        }

        CompletableFuture<?>[] futures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, ExecuteSystem.getExecutorServices()))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();

        return actorList;
    }

    @Override
    public Stream<FxThumb> scrapeExtraImage() {
        Elements extraArtElementsSmallSize = this.document.select(DmmCSSQuery.Q_THUMBS);

        if (extraArtElementsSmallSize.size() > 20) {
            return Stream.empty();
        }

        // We need to do some string manipulation and put a "jp" before the
        // last dash in the URL to of the full size picture
        return Stream.ofAll(extraArtElementsSmallSize.stream())
                .filter(e -> e.attr("name").equals("sample-image"))
                .map(e -> e.selectFirst("img"))
                .filter(Objects::nonNull)
                .map(e -> e.attr("abs:src"))
                .filter(s -> s.length() > 0)
                .map(s -> s.substring(0, s.lastIndexOf('-')) + "jp" + s.substring(s.lastIndexOf('-')))
                .peek((link) -> System.out.println("scrapeExtraArt() >> " + link))
                .map(FxThumb::of)
                .flatMap(Option::toStream);
    }

    public Flux<ActorV2> scrapeActorStream() {
        Elements actressIDElements = this.document.select("span#performer a[href*=article=actress/id=]");

        return Flux.fromStream(actressIDElements.stream())
                .parallel()
                .map(e -> ActorV2.of(e.text(), ActorDmmID.fromUrl(e.attr("abs:href"))))
                .map(Value::getOrNull)
                .filter(Objects::nonNull)
                .runOn(ExecuteSystem.get().getNormalScheduler())
                .sequential()
                ;
    }

    public Flux<ActorV2> scrapeActorTextOnlyStream() {
        Elements nameOnlyActors = this.document.select("table.mg-b20 tr td:contains(�??�?：) + td");

        return Flux.fromStream(nameOnlyActors.stream())
                .map(Element::text)
                .map(String::trim)
                .map(n -> n.replaceFirst("\\([0-9]{2}\\)", ""))
                .map(ActorV2::of)
                .subscribeOn(ExecuteSystem.get().getNormalScheduler())
                ;
    }

    @Override
    public Mono<List<ActorV2>> scrapeActorsMono() {
        return this.scrapeActorStream().mergeWith(this.scrapeActorTextOnlyStream())
                .collectList();
    }

    public Future<List<ActorV2>> scrapeActorsAsync() {
        return Future.fromCompletableFuture(this.scrapeActorsMono().toFuture());
/*
        return Future.of(Systems.getExecutorServices(ExecuteSystem.role.MOVIE), () ->
        {
            List<ActorV2> actorList = this.scrapeActorStream().collectList().subscribe().block();

            //Get actors that are just a "Name" and have no page of their own (common on some web releases)
            Elements nameOnlyActors = this.document.select("table.mg-b20 tr td:contains(�??�?：) + td");

            Stream.ofAll(nameOnlyActors)
                    .map(Element::text)
                    .map(String::trim)
                    .map(n -> n.replaceFirst("\\([0-9]{2}\\)", ""))
                    .map(ActorV2::of)
                    .forEach(actorList::add);

            return actorList;*/

            /*
            for (Element actressIDLink : actressIDElements) {
                tasks.add(() ->
                {
                    String actressIDHref = actressIDLink.attr("abs:href");
                    String actressName = actressIDLink.text();

                    GUICommon.debugMessage("scrapeActorsAsync >> " + actressName + " " + actressIDHref);

                    //String actressID = actressIDHref.substring(actressIDHref.indexOf("id=") + 3, actressIDHref.length() - 1);
                    ActorWebID webID = ActorDmmID.fromUrl(actressIDHref);

                    ActorDB.getInstance()
                            .getData(actressName)
                            .peek(actorList::add)
                            .onEmpty(() -> {
                                Try.of(() -> Jsoup.connect(actressPageURL).timeout(SiteParsingProfile.CONNECTION_TIMEOUT_VALUE * 10).get())
                                        .onFailure(e -> GUICommon.debugMessage(() -> "Cannot download actor - Socket timed out"))
                                        .map(actressPage -> {
                                            Element actressElement = actressPage.select("tr.area-av30.top td img").first();
                                            String thumb = actressElement.attr("abs:src");

                                            if (thumb.contains("nowprinting.gif")) {
                                                thumb = "";
                                            }

                                            ActorV2 actorV2 = ActorV2.of(actressNameKanji, ActorV2.Source.DMM, actressPageURL, thumb, "");
                                            actorV2.setActorDmmID(ActorDmmID.of(actressID));
                                            actorV2.updateFromDmmDocument(actressPage);
                                            return actorV2;
                                        })
                                getNewActor(webID, actressName)
                                        .peek(actorList::add);
                            });
                });
            }

            CompletableFuture<?>[] futures = tasks.stream()
                    .map(task -> CompletableFuture.runAsync(task, Systems.getExecutorServices()))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();

            for (Element currentNameOnlyActor : nameOnlyActors) {
                String actorName = currentNameOnlyActor.text().trim();
                //for some reason, they sometimes list the age of the person after their name, so let's of rid of that
                actorName = actorName.replaceFirst("\\([0-9]{2}\\)", "");

                ActorV2 actorV2 = ActorV2.of(actorName);
                actorList.add(actorV2);
            }*/
        // });

    }

    public static Option<ActorV2> createActorFromDmm(ActorWebID webID, String actressName) {
        return ActorDmmID.getActorWebUrl(webID)
                .flatMap(i -> Try.of(() -> Jsoup.connect(i).timeout(SiteParsingProfile.CONNECTION_TIMEOUT_VALUE * 10).get())
                        .onFailure(e -> GUICommon.debugMessage(() -> "Cannot download actor - Socket timed out"))
                        .toOption())
                .map(document -> createActorFromDocument(webID, actressName, document));
    }

    @NotNull
    private static ActorV2 createActorFromDocument(ActorWebID webID, String actressName, Document document) {
        ActorV2 actorV2 = ActorV2.of(actressName, ActorV2.Source.DMM, "", getActorThumbUrl(document), "");
        actorV2.setActorDmmID(webID);
        actorV2.updateFromDmmDocument(document);
        return actorV2;
    }

    private static String getActorThumbUrl(@NotNull Document document) {
        return Option.of(document.select("tr.area-av30.top td img").first())
                .map(e -> e.attr("abs:src"))
                .filter(s -> !s.contains("nowprinting.gif"))
                .getOrElse("");
    }

    public static String scrapeActorPageURL(@NotNull String actorName) {
        String cleanName = actorName;
        int idx = actorName.indexOf("（");
        if (idx != -1) {
            cleanName = actorName.substring(0, idx);
        }
        String dmmUrl = "http://actress.dmm.co.jp/-/search/=/searchstr=" + cleanName + "/";

        return scrapeActorURL(dmmUrl, actorName).getOrElse("");
    }

    private static Option<String> scrapeActorURL(String dmmUrl, String actorName) {
        return Systems.getWebService().getDocument(dmmUrl, WebServiceRun.none, false)
                .onFailure(e -> GUICommon.debugMessage(() -> "Cannot get Actor DMM Page URL"))
                .toOption()
                .map(a -> a.select("tr[class=list] td + td a"))
                .map(Stream::ofAll)
                .flatMap(es -> es.find(e -> e.text().equals(actorName)))
                .map(e -> e.attr("href"))
                .peek(GUICommon::debugMessage)
                ;
    }

    public static ActorV2 scrapeActorThumbFromDmm(@NotNull String actorName) {
        try {
            String cleanName = actorName;

            int idx = actorName.indexOf("（");
            if (idx != -1) {
                cleanName = actorName.substring(0, idx);
            }

            System.out.println("dmm search >> " + cleanName);

            String dmmUrl = "http://actress.dmm.co.jp/-/search/=/searchstr=" + cleanName + "/";

            Document actressPage = Jsoup.connect(dmmUrl).userAgent("Mozilla").timeout(SiteParsingProfile.CONNECTION_TIMEOUT_VALUE).get();

            Elements actressThumbnailElement = actressPage.select("tr[class=list] td + td a");

            System.out.println("dmm actressThumbnailElement >> " + actressThumbnailElement.toString());

            for (Element actElement : actressThumbnailElement) {
                String link = actElement.attr("href");
                String imgPath = actElement.select("img").attr("src");

                if (!Objects.equals(imgPath, "")) {
                    if (!imgPath.contains("nowprinting.gif")) {
                        // Use normal size image instead of thumbnail
                        if (imgPath.contains("thumbnail/")) {
                            imgPath = imgPath.replaceAll("thumbnail/", "");
                        }
                    } else {
                        imgPath = "";
                    }

                    System.out.println("scrapeActorThumbFromDmm() >> " + imgPath);
                    return ActorV2.of(actorName, ActorV2.Source.DMM, link, imgPath, "");
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Cannot download from DMM Socket timed out: " + e.getLocalizedMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ActorV2.of(actorName);
    }


    @Override
    public String scrapeDirector() {
        return this.defaultScrapeDirectors(DmmCSSQuery.Q_DIRECTOR);
    }

    @Override
    public String createSearchString(String searchStr) {
        //system.out.println("fileNameNoExtension in DMM: " + fileNameNoExtension);

        return Try.of(() -> new URLCodec().encode(searchStr))
                .map(s -> "http://www.dmm.co.jp/search/=/searchstr=" + s + "/")
                .getOrElse("");
    }

    @Override
    public void setSearchOption1(boolean searchDVDOnly) {
        this.searchDVDOnly = searchDVDOnly;
    }

    private Try<Stream<SimpleMovieData>> getSearchResultsDVDOnly(String searchString) {

        Try<Stream<SimpleMovieData>> allSearchResult = this.getSearchResultStream(searchString);

        return allSearchResult.map(s -> s.filter(sr -> sr.getStrUrl().contains("/mono/dvd/")));
    }

    /**
     * returns a String[] filled in with urls of each of the possible movies
     * found on the page returned from createSearchString
     */
    @Override
    public Try<Stream<SimpleMovieData>> getSearchResults(String searchString) {
        System.out.println(">> getSearchResults " + searchString);

        if (this.searchDVDOnly) {
            return this.getSearchResultsDVDOnly(searchString);
        }
        return this.getSearchResultStream(searchString);
    }

    private Try<Stream<SimpleMovieData>> getSearchResultStream(String searchString) {

        ArrayList<String> pagesVisited = new ArrayList<>();

        Option<String> nextPageURL = Option.of(searchString);
        Try<Stream<SimpleMovieData>> searchResults = Try.of(Stream::empty);

        while (nextPageURL.isDefined() && !pagesVisited.contains(nextPageURL.get())) {

            Try<Document> searchResultPage = Systems.getWebService().getDocument(nextPageURL.get(), WebServiceRun.none, false);
            searchResultPage.peek(p -> pagesVisited.add(p.baseUri()));

            nextPageURL = searchResultPage
                    .map(s -> s.select("div.list-capt div.list-boxcaptside.list-boxpagenation ul li:not(.terminal) a").last())
                    .filter(Objects::nonNull)
                    .map(e -> e.attr("abs:href"))
                    .toOption();

            searchResults = searchResults.flatMap((r) -> searchResultPage.map(this::getResultFromPage).map(nr -> Stream.concat(r, nr)));
        }

        return searchResults;
    }

    private Stream<SimpleMovieData> getResultFromPage(@NotNull Document searchResultsPage) {

        Elements dvdLinks = searchResultsPage.select("p.tmb a[href*=/mono/dvd/]");
        Elements rentalElements = searchResultsPage.select("p.tmb a[href*=/rental/ppr/]");
        Elements digitalElements = searchResultsPage.select("p.tmb a[href*=/digital/videoa/], p.tmb a[href*=/digital/videoc/]");

        Stream<SimpleMovieData> searchResults = Stream.empty();

        searchResults = searchResults.appendAll(
                Stream.ofAll(dvdLinks).map(this::mapSearchResult).filter(Objects::nonNull)
        );
        searchResults = searchResults.appendAll(
                Stream.ofAll(rentalElements).map(this::mapSearchResult).filter(Objects::nonNull)
        );
        searchResults = searchResults.appendAll(
                Stream.ofAll(digitalElements).map(this::mapSearchResult).filter(Objects::nonNull)
        );

        /*
        //of /mono/dvd links
        for (Element dvdLink : dvdLinks) {
            String currentLink = dvdLink.attr("abs:href");
            if (!".*dod/.*".matches(currentLink)) {
                Element imageLinkElement = dvdLink.select("img").first();
                if (imageLinkElement != null) {
                    var currentPosterThumbnail = FxThumb.of(imageLinkElement.attr("abs:src"));
                    var title = imageLinkElement.attr("alt");
                    searchResults = searchResults.append(new SearchResult(currentLink, title, currentPosterThumbnail));
                } else {
                    searchResults = searchResults.append(new SearchResult(currentLink));
                }
            }
        }
        //of /rental/ppr links
        for (Element rentalElement : rentalElements) {
            String currentLink = rentalElement.attr("abs:href");
            Element imageLinkElement = rentalElement.select("img").first();
            if (imageLinkElement != null) {
                var currentPosterThumbnail = FxThumb.of(imageLinkElement.attr("abs:src"));
                var title = imageLinkElement.attr("alt");
                searchResults = searchResults.append(new SearchResult(currentLink, title, currentPosterThumbnail));
            } else {
                searchResults = searchResults.append(new SearchResult(currentLink));
            }
        }
        //of /digital/videoa links
        for (Element digitalElement : digitalElements) {
            String currentLink = digitalElement.attr("abs:href");
            System.out.println("currentLink = " + currentLink);
            Element imageLinkElement = digitalElement.select("img").first();
            if (imageLinkElement != null) {
                //System.out.println("imageLinkElement " + imageLinkElement.select("img").attr("src"));//imageLinkElement.attr("abs:src"));
                var currentPosterThumbnail = FxThumb.of(imageLinkElement.attr("abs:src"));
                var title = imageLinkElement.attr("alt");
                searchResults = searchResults.append(new SearchResult(currentLink, title, currentPosterThumbnail));
            } else {
                searchResults = searchResults.append(new SearchResult(currentLink));
            }
        }
*/
        return searchResults;
    }

    private SimpleMovieData mapSearchResult(Element element) {
        String strUrl = element.attr("abs:href");
        Element imgElement = element.selectFirst("img");
        if (imgElement != null) {
            String strTitle = imgElement.attr("alt");
            Option<FxThumb> imgCover = FxThumb.of(imgElement.attr("abs:src"));
            SimpleMovieData movieData = new SimpleMovieData("", strTitle, imgCover);
            movieData.setStrUrl(strUrl);
            return movieData;
        }
        return null;
    }

    @Override
    public String toString() {
        return "DMM.co.jp";
    }

    @Override
    public SiteParsingProfile newInstance() {
        GeneralSettings preferences = Systems.getPreferences();
        return new DmmParsingProfile(!preferences.getScrapeInJapanese());
    }

    @Override
    public String getParserName() {
        return parserName();
    }

}
