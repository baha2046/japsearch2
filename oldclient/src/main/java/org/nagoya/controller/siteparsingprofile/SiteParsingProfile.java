package org.nagoya.controller.siteparsingprofile;

import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.nagoya.controller.AbstractMovieScraper;
import org.nagoya.controller.GenericMovieScraper;
import org.nagoya.controller.languagetranslation.Language;
import org.nagoya.io.WebServiceRun;
import org.nagoya.model.SearchResult;
import org.nagoya.model.SimpleMovieData;
import org.nagoya.model.dataitem.*;
import org.nagoya.preferences.GeneralSettings;
import org.nagoya.system.Systems;
import reactor.core.publisher.Mono;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SiteParsingProfile implements DataItemSource {

    public static final int CONNECTION_TIMEOUT_VALUE = 15000;
    public Document document; // the base page to start parsing from

    protected List<ScraperGroupName> groupNames;
    protected Language scrapingLanguage;
    protected File scrapedMovieFile;
    protected GeneralSettings scrapingPreferences;
    private boolean extraFanartScrapingEnabled = false;
    private boolean isDisabled = false;
    private boolean firstWordOfFileIsID = false;
    private ImageIcon profileIcon;

    /**
     * If this has a value when scraping, will use overridenSearchResult
     * from a user provided URL without looking at file name
     */
    private SearchResult overridenSearchResult;
    /**
     * do we want to ignore scraping from this scraper. typically done when the user has hit cancel from a dialog box because none of the seen results were valid
     */
    private boolean discardResults;

    public SiteParsingProfile(Document document) {
        this.document = document;
        this.scrapingLanguage = Language.JAPANESE;
        this.scrapingPreferences = Systems.getPreferences();
        this.setScrapingLanguage(this.scrapingPreferences);
        this.firstWordOfFileIsID = this.scrapingPreferences.getIsFirstWordOfFileID();
        this.isDisabled = false;
    }

    public SiteParsingProfile() {
        this.scrapingLanguage = Language.JAPANESE;
        this.scrapingPreferences = Systems.getPreferences();
        this.setScrapingLanguage(this.scrapingPreferences);
        this.firstWordOfFileIsID = this.scrapingPreferences.getIsFirstWordOfFileID();
        this.isDisabled = false;
    }

    /**
     * Gets the ID number from the file and considers stripped out multipart file identifiers like CD1, CD2, etc
     * The ID number needs to be the last word in the filename or the next to the last word in the file name if the file name
     * ends with something like CD1 or Disc 1
     * So this filename "My Movie ABC-123 CD1" would return the id as ABC-123
     * This filename "My Movie ABC-123" would return the id as ABC-123
     *
     * @param path                - file to find the ID tag from
     * @param firstWordOfFileIsID - if true, just uses the first word in the file (seperated by space) as the ID number
     *                            otherwise use the method described above
     * @return
     */
    public static String findIDTagFromFile(Path path, boolean firstWordOfFileIsID) {
        String fileNameNoExtension;

        if (Files.isDirectory(path)) {
            fileNameNoExtension = path.getFileName().toString();
        } else {
            fileNameNoExtension = FilenameUtils.removeExtension(path.getFileName().toString());
        }

        return findIDTagFromFile(fileNameNoExtension, firstWordOfFileIsID);
    }

    public static String findIDTagFromFile(String fileNameNoExtension, boolean firstWordOfFileIsID) {
        String fileNameNoExtensionNoDiscNumber = stripDiscNumber(fileNameNoExtension);

        String[] splitFileName = fileNameNoExtensionNoDiscNumber.split(" ");
        String lastWord = "";

        for (String s : splitFileName) {
            if (s.startsWith("[")) {
                lastWord = s;
                break;
            }
        }

        if (lastWord.equals("")) {
            if (firstWordOfFileIsID && splitFileName.length > 0) {
                lastWord = splitFileName[0];
            } else {
                lastWord = splitFileName[splitFileName.length - 1];
            }
        }
        //Some people like to enclose the ID number in parenthesis or brackets like this (ABC-123) or this [ABC-123] so this gets rid of that
        //TODO: Maybe consolidate these lines of code using a single REGEX?
        lastWord = lastWord.replace("(", "");
        lastWord = lastWord.replace(")", "");
        lastWord = lastWord.replace("[", "");
        lastWord = lastWord.replace("]", "");

        return lastWord;
    }

    @NotNull
    public static String stripDiscNumber(String fileNameNoExtension) {
        //replace <cd/dvd/part/pt/disk/disc/d> <0-N>  (case insensitive) with empty
        String discNumberStripped = fileNameNoExtension.replaceAll("(?i)[ _.]+(?:cd|dvd|p(?:ar)?t|dis[ck]|d)[ _.]*[0-9]+$", "");
        //replace <cd/dvd/part/pt/disk/disc/d> <a-d> (case insensitive) with empty
        discNumberStripped = discNumberStripped.replaceAll("(?i)[ _.]+(?:cd|dvd|p(?:ar)?t|dis[ck]|d)[ _.]*[a-d]$", "");
        return discNumberStripped.trim();
    }

    protected static boolean fileExistsAtURL(String URLName) {
        return fileExistsAtURL(URLName, false);
    }

    protected static boolean fileExistsAtURL(String URLName, Boolean allow_redirects) {
        try {
            HttpURLConnection.setFollowRedirects(allow_redirects);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection();
            con.setInstanceFollowRedirects(allow_redirects);
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(CONNECTION_TIMEOUT_VALUE);
            con.setReadTimeout(CONNECTION_TIMEOUT_VALUE);
            con.setRequestProperty("User-Agent", getRandomUserAgent());
            if (!allow_redirects) {
                return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
            } else {
                return (con.getResponseCode() == HttpURLConnection.HTTP_OK || con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP);
            }
        } catch (SocketTimeoutException e) {
            // Non-existing DMM trailers usually time out
            System.err.println("Connection timed out: " + URLName);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * If your file is called "Movie Name Here (2001)" this method returns "Movie Name Here"
     *
     * @param file the file to process
     * @return The movie name without the year in parenthesis next to it
     */
    public static String getMovieNameFromFileWithYear(@NotNull File file) {
        String movieName = FilenameUtils.removeExtension(FilenameUtils.getName(file.getName()));
        movieName = movieName.replaceFirst("\\(\\d{4}\\)$", "").trim();
        return movieName;
    }

    /**
     * If your file is called "Movie Name Here (2001)" this method returns "2001"
     *
     * @param file the file to process
     * @return A length 4 string representing the year, if it exists. Otherwise an empty String
     */
    @NotNull
    public static String getYearFromFileWithYear(File file) {
        String movieName = FilenameUtils.removeExtension(FilenameUtils.getName(file.getName()));
        String patternString = "\\(\\d{4}\\)$";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(movieName);
        if (matcher.find()) {
            return matcher.group().replace("(", "").replace(")", "").trim();
        }
        return "";
    }

    /**
     * Maybe we are less likely to of blocked on google if we don't always use the same user agent when searching,
     * so this method is designed to pick a random one from a list of valid user agent strings
     *
     * @return a random user agent string that can be passed to .userAgent() when calling Jsoup.connect
     */
    public static String getRandomUserAgent() {
        String[] userAgent = {"Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6",
                "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0",
                "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36", "Opera/9.80 (Windows NT 6.0) Presto/2.12.388 Version/12.14",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0) Opera 12.14",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A",
                "Mozilla/5.0 (Windows; U; Windows NT 6.1; tr-TR) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27",
                "Mozilla/5.0 (Windows; U; Windows NT 5.0; en-en) AppleWebKit/533.16 (KHTML, like Gecko) Version/4.1 Safari/533.16"};
        return userAgent[new Random().nextInt(userAgent.length)];
    }

    public Try<Document> downloadDocument(@NotNull SimpleMovieData searchResult) {
        return Systems.getWebService().getDocument(searchResult.getStrUrl(), WebServiceRun.none, false);
    }

    public List<ScraperGroupName> getScraperGroupNames() {
        if (this.groupNames == null) {
            this.groupNames = Arrays.asList(ScraperGroupName.DEFAULT_SCRAPER_GROUP);
        }
        return this.groupNames;
    }

    public boolean isExtraFanartScrapingEnabled() {
        return this.extraFanartScrapingEnabled;
    }

    public void setExtraFanartScrapingEnabled(boolean extraFanartScrapingEnabled) {
        this.extraFanartScrapingEnabled = extraFanartScrapingEnabled;
    }

    public Document getDocument() {
        return this.document;
    }

    public SiteParsingProfile setDocument(Document document) {
        this.document = document;
        return this;
    }

    public void setSearchOption1(boolean option1) {
    }

    public void setSearchOption2(boolean option2) {
    }

    /**
     * @return {@link SiteParsingProfile#overridenSearchResult}
     */
    public SearchResult getOverridenSearchResult() {
        return this.overridenSearchResult;
    }

    /**
     * Sets the {@link SiteParsingProfile#overridenSearchResult} to the URL defined by @param urlPath
     * This will cause the scraper to ignore the file name of the file when scraping
     *
     * @param urlPath
     */
    public void setOverridenSearchResult(String urlPath) {
        // this.overridenSearchResult = new SearchResult(urlPath);

    }


    public abstract String scrapeTitle();

    public abstract ID scrapeID();

    public abstract String scrapeMaker();

    public abstract String scrapeSet();

    public abstract Option<ReleaseDate> scrapeReleaseDate();

    public abstract String scrapePlot();

    public abstract String scrapeStudio();

    public abstract String scrapeRuntime();

    public abstract Option<FxThumb> scrapeCover();

    public abstract String scrapeDirector();

    public abstract ArrayList<Genre> scrapeGenres();

    public abstract Mono<List<ActorV2>> scrapeActorsMono();

    public Stream<FxThumb> scrapeExtraImage() {
        return Stream.empty();
    }

    public Option<MPAARating> scrapeMPAA() {
        return Option.of(MPAARating.RATING_XXX);
    }

    public String scrapeYear() {
        return this.scrapeReleaseDate().map(ReleaseDate::getYear).map(Year::getYear).getOrElse("");
    }
    @Deprecated
    public Rating scrapeRating() {
        return Rating.BLANK_RATING;
    }

    public Trailer scrapeTrailer() {
        return Trailer.BLANK_TRAILER;
    }

    protected String defaultScrapeSet(String cssQuery) {
        return this.SelectFirstElement(cssQuery)
                .map(Element::text)
                .getOrElse("");
    }

    protected Option<ReleaseDate> defaultScrapeReleaseDate(String cssQuery) {
        return this.SelectFirstElement(cssQuery)
                .map(Element::text)
                .map(s -> new ReleaseDate(StringUtils.replace(s, "/", "-")));
    }

    protected String defaultScrapeRuntime(String cssQuery) {
        return this.SelectFirstElement(cssQuery)
                .map(Element::text)
                .map(s -> s.replaceAll("分", ""))
                .getOrElse("");
    }

    protected String defaultScrapeStudio(String cssQuery) {
        return this.SelectFirstElement(cssQuery)
                .map(Element::text)
                .getOrElse("");
    }

    protected String defaultScrapeMaker(String cssQuery) {
        return this.SelectFirstElement(cssQuery)
                .map(Element::text)
                .getOrElse("");
    }

    protected Option<FxThumb> defaultScrapeCover(String cssQuery) {
        return this.SelectFirstElement(cssQuery)
                .map(e -> e.attr("abs:href"))
                .flatMap(FxThumb::of);
    }

    protected String defaultScrapeDirectors(String cssQuery) {
        return this.SelectFirstElement(cssQuery)
                .map(Element::text)
                .getOrElse("");
    }

    public abstract String createSearchString(String searchStr);

    public String createSearchString(Path path) {
        String fileNameNoExtension = findIDTagFromFile(path, this.isFirstWordOfFileIsID());
        return this.createSearchString(fileNameNoExtension);
    }


    public abstract Try<Stream<SimpleMovieData>> getSearchResults(String searchString);

    public SearchResult[] getLinksFromGoogle(String searchQuery, String site) {
        //system.out.println("calling of links from google with searchQuery = " + searchQuery);
        ArrayList<SearchResult> linksToReturn = new ArrayList<>();
        try {
            String encodingScheme = "UTF-8";
            String queryToEncode = "site:" + site + " " + searchQuery;
            String encodedSearchQuery = URLEncoder.encode(queryToEncode, encodingScheme);
            Document doc = Jsoup.connect("https://www.google.com/search?q=" + encodedSearchQuery).userAgent(getRandomUserAgent()).referrer("http://www.google.com")
                    .ignoreHttpErrors(true).timeout(SiteParsingProfile.CONNECTION_TIMEOUT_VALUE).get();
            Elements sorryLink = doc.select("form[action=CaptchaRedirect] input");
            Map<String, String> captchaData = new HashMap<>();
            for (Element element : sorryLink) {
                String key = element.attr("name");
                String value = element.attr("value");
                captchaData.put(key, value);
            }
            if (captchaData.size() > 0) {
                System.out.println("Found Captchadata : " + captchaData);
                System.out.println("Google has temporarily blocked us. Trying on bing instead.");
                return this.getLinksFromBing(searchQuery, site);
            }

            Elements links = doc.select("div.g");
            for (Element link : links) {
                Elements hrefs = link.select("h3.r a");
                String href = hrefs.attr("href");
                href = URLDecoder.decode(href, encodingScheme);
                href = href.replaceFirst(Pattern.quote("/url?q="), "");
                href = href.replaceFirst(Pattern.quote("http://www.google.com/url?url="), "");
                //remove some junk referrer stuff
                int startIndexToRemove = href.indexOf("&rct=");
                if (startIndexToRemove > -1) {
                    href = href.substring(0, startIndexToRemove);
                }
                linksToReturn.add(new SearchResult(href, hrefs.text()));
            }
            if (linksToReturn.size() == 0) {
                //maybe we will have better luck with bing since we found nothing on google
                return this.getLinksFromBing(encodedSearchQuery, site);
            }
            return linksToReturn.toArray(new SearchResult[linksToReturn.size()]);
        } catch (IOException e) {
            e.printStackTrace();
            return linksToReturn.toArray(new SearchResult[linksToReturn.size()]);
        }
    }

    /**
     * A backup search provider in case google search fails. This method is marked private and is called from getLinksFromGoogle. It should not be called in any other class.
     */
    private SearchResult[] getLinksFromBing(String searchQuery, String site) {
        ArrayList<SearchResult> linksToReturn = new ArrayList<>();
        String encodingScheme = "UTF-8";
        String queryToEncode = "site:" + site + " " + searchQuery;
        String encodedSearchQuery;
        try {
            encodedSearchQuery = URLEncoder.encode(queryToEncode, encodingScheme);
            Document bingResultDocument = Jsoup.connect("https://www.bing.com/search?q=" + encodedSearchQuery).userAgent(getRandomUserAgent()).referrer("http://www.bing.com")
                    .ignoreHttpErrors(true).timeout(SiteParsingProfile.CONNECTION_TIMEOUT_VALUE).get();
            Elements links = bingResultDocument.select("a[href*=" + site);
            for (Element link : links) {
                linksToReturn.add(new SearchResult(link.attr("href")));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return linksToReturn.toArray(new SearchResult[linksToReturn.size()]);
        }
        return linksToReturn.toArray(new SearchResult[linksToReturn.size()]);
    }

    public AbstractMovieScraper getMovieScraper() {
        return new GenericMovieScraper(this);
    }

    /**
     * @return a new copy of the parser by calling the parser's constructor.
     * used to instantiate a parser when the type of the object is not known
     */
    public abstract SiteParsingProfile newInstance();

    public Language getScrapingLanguage() {
        return this.scrapingLanguage;
    }

    public void setScrapingLanguage(GeneralSettings preferences) {
        if (preferences.getScrapeInJapanese()) {
            this.scrapingLanguage = Language.JAPANESE;
        } else {
            this.scrapingLanguage = Language.ENGLISH;
        }
    }

    public void setScrapingLanguage(Language scrapingLanguage) {
        this.scrapingLanguage = scrapingLanguage;
    }


    /**
     * @return The name of the parser used when displaying the parser in drop down menus or console output.
     * For example if the parser parses a site called, "MySite.com"
     * this function may return "My Site".
     */
    public abstract String getParserName();

    @Override
    public String toString() {
        return this.getParserName();
    }

    public boolean isFirstWordOfFileIsID() {
        return this.firstWordOfFileIsID;
    }

    public void setFirstWordOfFileIsID(boolean firstWordOfFileIsID) {
        this.firstWordOfFileIsID = firstWordOfFileIsID;
    }

    @Override
    public String getDataItemSourceName() {
        return this.getParserName();
    }

    @Override
    public DataItemSource createInstanceOfSameType() {
        DataItemSource newInstance = this.newInstance();
        newInstance.setDisabled(this.isDisabled());
        return newInstance;
    }

    @Override
    public boolean isDisabled() {
        return this.isDisabled;
    }

    @Override
    public void setDisabled(boolean value) {
        this.isDisabled = value;
    }

    @Override
    public ImageIcon getProfileIcon() {
        if (this.profileIcon != null) {
            return this.profileIcon;
        } else {
            String profileName = this.getClass().getSimpleName();
            String siteName = profileName.replace("ParsingProfile", "");
            return this.initializeResourceIcon("/res/sites/" + siteName + ".png", 16, 16);
        }
    }

    private ImageIcon initializeResourceIcon(String resourceName, int iconSizeX, int iconSizeY) {
        /*try {
            URL url = GUIMain.class.getResource(resourceName);
            if (url != null) {
                BufferedImage iconBufferedImage = ImageIO.read(url);
                if (iconBufferedImage != null) {
                    iconBufferedImage = Scalr.resize(iconBufferedImage, Method.QUALITY, iconSizeX, iconSizeY, Scalr.OP_ANTIALIAS);
                    return new ImageIcon(iconBufferedImage);
                } else {
                    return new ImageIcon();
                }
            }
            return new ImageIcon();
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }*/
        return null;
    }

    public boolean getDiscardResults() {
        return this.discardResults;
    }

    public void setDiscardResults(boolean value) {
        this.discardResults = value;
    }

    /* Any group of SiteParsingProfiles which return the same type of information for a given file and which
     * will be compatible for amalgamation should return the same ScraperGroupName by implementing getScraperGroupName()
     */
    public enum ScraperGroupName {
        JAV_CENSORED_SCRAPER_GROUP {
            @Override
            public String toString() {
                return "JAV Censored Group";
            }
        },
        AMERICAN_ADULT_DVD_SCRAPER_GROUP {
            @Override
            public String toString() {
                return "American Adult DVD";
            }
        },
        DEFAULT_SCRAPER_GROUP {
            @Override
            public String toString() {
                return "Default Group";
            }
        }
    }

    protected Option<Element> SelectFirstElement(String query) {
        return Option.of(this.document.select(query).first());
    }
}
