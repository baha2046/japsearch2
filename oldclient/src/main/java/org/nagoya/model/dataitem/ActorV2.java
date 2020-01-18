package org.nagoya.model.dataitem;

import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Callback;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nagoya.GUICommon;
import org.nagoya.controller.siteparsingprofile.specific.DmmParsingProfile;
import org.nagoya.controller.siteparsingprofile.specific.JavBusParsingProfile;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.system.Systems;
import org.nagoya.system.database.MovieDB;
import org.nagoya.system.database.MovieScanner;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActorV2 implements Runnable {

    public enum Source {
        DUGA, DMM, JAVBUS, LOCAL, NONE
    }

    public static final FxThumb NO_PHOTO = FxThumb.empty();//FxThumb.of("http://pics.dmm.co.jp/mono/actjpgs/nowprinting.gif").getOrElse(FxThumb.BLANK_THUMB);
    public static final ActorV2 LOADING_ACTOR = new ActorV2("Loading...", Source.NONE, "", "", "");

    public static ActorV2 of(String strName) {
        return ActorV2.of(strName, Source.NONE, "", "", "");
    }

    public static ActorV2 of(String strName, Source source, String strUrl, String imgUrl, String strDesc) {
        return MovieDB.actorDB().getData(strName)
                .peek(a -> a.addRecord(source, strUrl, imgUrl, strDesc))
                .getOrElse(() -> {
                    GUICommon.debugMessage("NEW ACTOR " + strName);
                    ActorV2 a = new ActorV2(strName, source, strUrl, imgUrl, strDesc);
                    Systems.useExecutors(a);
                    MovieDB.actorDB().putData(strName, a);
                    return a;
                });
    }

    public static Option<ActorV2> of(String strName, @NotNull ActorWebID actorDmmID) {
        GUICommon.debugMessage("getOrCreateActor >> " + strName + " " + actorDmmID.toString());
        return MovieDB.actorDB().getData(actorDmmID)
                .orElse(() -> MovieDB.actorDB().getData(strName)
                        .peek(a -> a.setActorDmmID(actorDmmID)))
                .orElse(() -> DmmParsingProfile.createActorFromDmm(actorDmmID, strName));
    }

    @Override
    public void run() {
        if (this.needUpdate) {
            this.updateWebID(false);
            this.updateFromWeb();
        }
    }

    public ActorWebID getActorDmmID() {
        if (this.actorDmmID == null) {
            this.actorDmmID = new ActorWebID();
        }
        return this.actorDmmID;
    }

    public ActorWebID getActorJavBusID() {
        if (this.actorJavBusID == null) {
            this.actorJavBusID = new ActorWebID();
        }
        return this.actorJavBusID;
    }

    public void setActorDmmID(ActorWebID actorDmmID) {
        this.actorDmmID = actorDmmID;
    }

    public void setActorJavBusID(ActorWebID actorJavBusID) {
        this.actorJavBusID = actorJavBusID;
    }

    public void updateFromWeb() {
        ActorDmmID.getActorWebUrl(this.getActorDmmID()).peek(u -> {
            GUICommon.debugMessage("Get Info from DMM " + u);
            Document actressPage = Try.of(() ->
                    Jsoup.connect(u).timeout(300000).get()).getOrNull();
            this.updateFromDmmDocument(actressPage);
            this.refreshView();
        });
    }

    public void updateWebID(boolean forceUpdate) {
        if (forceUpdate || !this.getActorDmmID().isDefined()) {
            String strUrl = DmmParsingProfile.scrapeActorPageURL(this.getName());
            this.actorDmmID = ActorDmmID.fromUrl(strUrl);
        }

        if (forceUpdate || !this.getActorJavBusID().isDefined()) {
            String strUrl = JavBusParsingProfile.scrapeActorPageURL(this.getName());
            this.actorJavBusID = ActorJavBusID.fromUrl(strUrl);
        }
    }

    public void updateFromDmmDocument(Document actressPage) {
        this.needUpdate = false;

        Option<NetRecord> dmmRecord = this.getRecord(Source.DMM);

        if (actressPage != null) {
            Element actressImage = actressPage.select("td[style=padding:15px 50px 15px 12px;] img").first();
            if (actressImage != null) {
                GUICommon.debugMessage("updateFromDmmDocument >> " + actressImage.attr("src"));

                Option<FxThumb> thumb = FxThumb.of(actressImage.attr("src"));
                this.addRecord(Source.DMM, dmmRecord.map(NetRecord::getUrl).getOrElse(""),
                        thumb, dmmRecord.map(NetRecord::getDesc).getOrElse(""));
            }

            Element actressYomi = actressPage.select("td[class=t1] h1").first();

            if (actressYomi != null) {
                GUICommon.debugMessage(actressYomi.text());

                String yomi = actressYomi.text()
                        .replace(")", "）")
                        .replaceFirst(this.getName().replace(")", "）"), "");

                GUICommon.debugMessage(yomi);

                this.setYomi(yomi.substring(1, yomi.length() - 1));
            }

            Element actressBirth = actressPage.select("tr td:contains(生年月日 ：) + td").first();
            if (actressBirth != null) {

                LocalDate date = null;

                if (!actressBirth.text().equals("----")) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.ENGLISH);
                    date = Try.of(() -> LocalDate.parse(actressBirth.text(), formatter))
                            .onSuccess(d -> GUICommon.debugMessage(d.toString()))
                            .getOrNull();
                }

                this.setBirth(date);
            }

            Element actressSize = actressPage.select("tr td:contains(サイズ ：) + td").first();
            if (actressSize != null) {
                GUICommon.debugMessage(actressSize.text());

                String sizeString = actressSize.text();

                Matcher t = Pattern.compile("T(\\d+)cm").matcher(sizeString);
                if (t.find()) {
                    this.tall = t.group(1);
                    GUICommon.debugMessage("T " + t.group(1));
                }

                Matcher bwh = Pattern.compile("B(\\d+)cm.*W(\\d+)cm.*H(\\d+)cm").matcher(sizeString);
                if (bwh.find()) {
                    this.bSize = bwh.group(1);
                    this.wSize = bwh.group(2);
                    this.hSize = bwh.group(3);
                    GUICommon.debugMessage("BWH " + bwh.group(1) + " " + bwh.group(2) + " " + bwh.group(3));
                }

                Matcher cup = Pattern.compile("\\((\\D)カップ\\)").matcher(sizeString);
                if (cup.find()) {
                    this.cupSize = cup.group(1);
                    GUICommon.debugMessage("cup " + cup.group(1));
                }

                this.setSize(actressSize.text());
            }

        }
    }

    private String name;
    private String yomi = "";
    private LocalDate birthDate = null;
    private String size = "";

    private String tall = "";
    private String bSize = "";
    private String wSize = "";
    private String hSize = "";
    private String cupSize = "";


    private final List<Option<NetRecord>> data;

    private Option<CustomData> customData;

    private ActorWebID actorDmmID;
    private ActorWebID actorJavBusID;

    private transient Option<FxThumb> localImage;
    private transient Stream<DirectoryEntry> movieList;
    private transient final BooleanProperty refreshProperty;
    private transient int movieCount = -1;
    private transient boolean needUpdate;

    public ActorV2() {
        this.actorDmmID = new ActorWebID();
        this.actorJavBusID = new ActorWebID();
        this.data = new ArrayList<>();
        this.customData = Option.none();
        this.localImage = Option.none();
        this.movieList = Stream.empty();
        this.refreshProperty = new SimpleBooleanProperty(true);
        this.clearSource();
        this.needUpdate = true;
    }

    public ActorV2(String strName, Source siteID, String url, String imgUrl, String desc) {

        this();
        //GUICommon.debugMessage("Actor2 new " + strName);

        this.setName(strName);
        this.addRecord(siteID, url, imgUrl, desc);
    }

    public void versionFix() {
        /*String sizeString = this.getSize();

        if (sizeString.equals("") || sizeString.equals("----")) {
            return;
        }

        Matcher t = Pattern.compile("T(\\d+)cm").matcher(sizeString);
        if (t.find()) {
            this.tall = t.group(1);
            GUICommon.debugMessage("T " + t.group(1));
        }

        Matcher bwh = Pattern.compile("B(\\d+)cm.*W(\\d+)cm.*H(\\d+)cm").matcher(sizeString);
        if (bwh.find()) {
            this.bSize = bwh.group(1);
            this.wSize = bwh.group(2);
            this.hSize = bwh.group(3);
            GUICommon.debugMessage("BWH " + bwh.group(1) + " " + bwh.group(2) + " " + bwh.group(3));
        }

        Matcher cup = Pattern.compile("\\((\\D)カップ\\)").matcher(sizeString);
        if (cup.find()) {
            this.cupSize = cup.group(1);
            GUICommon.debugMessage("cup " + cup.group(1));
        }*/
    }

    @NotNull
    @Contract(pure = true)
    public static Callback<ActorV2, Observable[]> extractor() {
        return (ActorV2 p) -> new Observable[]{p.refreshProperty()};
    }

    public static final Comparator<ActorV2> ACTOR_COMPARATOR = (p, q) -> {
        int pc = p.getMovieCount();
        int qc = q.getMovieCount();

        if (pc > qc) {
            return -1;
        } else if (pc < qc) {
            return 1;
        } else {
            return p.getYomi().compareToIgnoreCase(q.getYomi());
        }
    };

    public BooleanProperty refreshProperty() {
        return this.refreshProperty;
    }

    public void refreshView() {
        Platform.runLater(() -> this.refreshProperty.set(!this.refreshProperty.getValue()));
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getYomi() {
        return this.customData.map(CustomData::getCustomYomi).filter(s -> s.length() > 0).getOrElse(this.yomi);
    }

    public void setYomi(String yomi) {
        this.yomi = yomi;
    }

    public String getTall() {
        return this.customData.map(CustomData::getTall).filter(s -> s.length() > 0).getOrElse(this.tall);
    }

    public String getBSize() {
        return this.customData.map(CustomData::getBSize).filter(s -> s.length() > 0).getOrElse(this.bSize);
    }

    public String getWSize() {
        return this.customData.map(CustomData::getWSize).filter(s -> s.length() > 0).getOrElse(this.wSize);
    }

    public String getHSize() {
        return this.customData.map(CustomData::getHSize).filter(s -> s.length() > 0).getOrElse(this.hSize);
    }

    public String getCupSize() {
        return this.customData.map(CustomData::getCupSize).filter(s -> s.length() > 0).getOrElse(this.cupSize);
    }

    public String getBirth() {

        if (this.customData.map(CustomData::getCustomBirthDate).filter(Objects::nonNull).isDefined()) {
            return this.customData.map(CustomData::getCustomBirthDate).map(LocalDate::toString).get();
        }

        if (this.birthDate != null) {
            return this.birthDate.toString();
        }

        return "";
    }

    public void setBirth(LocalDate birthDate) {
        this.birthDate = birthDate;

            /*String y = birth.substring(0, birth.indexOf("年"));
            String d = birth.substring(birth.indexOf("月") + 1, birth.length() - 1);
            String m = birth.substring(birth.indexOf("年") + 1, birth.indexOf("月"));
            GUICommon.debugMessage(y + " " + m + " " + d);
            this.birthDate = LocalDate.of(Integer.valueOf(y), Integer.valueOf(m), Integer.valueOf(d));*/

    }

    public Option<LocalDate> getAgeAsLocalDate() {
        if (this.customData.map(CustomData::getCustomBirthDate).filter(Objects::nonNull).isDefined()) {
            return Option.of(this.customData.map(CustomData::getCustomBirthDate).get());
        }
        return Option.of(this.birthDate);
    }

    public String getAge() {

        if (this.customData.map(CustomData::getCustomBirthDate).filter(Objects::nonNull).isDefined()) {
            return String.valueOf(ChronoUnit.YEARS.between(this.customData.map(CustomData::getCustomBirthDate).get(), LocalDate.now()));
        }

        if (this.birthDate != null) {
            return String.valueOf(ChronoUnit.YEARS.between(this.birthDate, LocalDate.now()));
        }
        return "";
    }

    public String getSize() {
        return buildSizeString(this.getTall(), this.getBSize(), this.getWSize(), this.getHSize(), this.getCupSize());
    }

    @NotNull
    public static String buildSizeString(@NotNull String T, String B, String W, String H, String cup) {
        StringBuilder stringBuilder = new StringBuilder(27);

        if (T.length() > 0) {
            stringBuilder.append("T").append(T).append(" ");
        }
        if (B.length() > 0) {
            stringBuilder.append("B").append(B).append(" ");
        }
        if (cup.length() > 0) {
            stringBuilder.append("(").append(cup).append(" Cup) ");
        }
        if (W.length() > 0) {
            stringBuilder.append("W").append(W).append(" ");
        }
        if (H.length() > 0) {
            stringBuilder.append("H").append(H);
        }
        return stringBuilder.toString().trim();
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void clearSource() {
        for (int x = 0; x < Source.values().length; x++) {
            this.data.add(Option.none());
        }
    }

    public void setLocalImage(FxThumb imgLocal) {
        //GUICommon.debugMessage("Actor2 setLocalImage " + getName());
        this.localImage = (imgLocal == null) ? Option.none() : Option.of(imgLocal);
    }

    public boolean hasLocalImage() {
        return this.localImage.isDefined();
    }

    public Option<FxThumb> getLocalImage() {
        return this.localImage;
    }

    public void addRecord(Source siteID, String url, String imgUrl, String desc) {
        if (siteID == Source.NONE) {
            return;
        }

        Option<FxThumb> fxThumb = Option.none();
        if (!Objects.equals(imgUrl, "")) {
            fxThumb = FxThumb.of(imgUrl);
        }

        this.data.set(siteID.ordinal(), Option.of(new NetRecord(siteID, fxThumb, url, desc)));
    }

    public void addRecord(Source siteID, String url, Option<FxThumb> img, String desc) {
        if (siteID == Source.NONE) {
            return;
        }

        this.data.set(siteID.ordinal(), Option.of(new NetRecord(siteID, img, url, desc)));
    }

    public void removeRecord(@NotNull Source siteID) {
        this.data.set(siteID.ordinal(), Option.none());
    }

    public Option<String> getRecordURL(@NotNull Source siteID) {
        return this.data.get(siteID.ordinal()).map(NetRecord::getUrl);
    }

    public Option<FxThumb> getRecordImage(@NotNull Source siteID) {
        return this.data.get(siteID.ordinal()).flatMap(NetRecord::getActorImage);
    }

    public Option<NetRecord> getRecord(@NotNull Source siteID) {
        return this.data.get(siteID.ordinal());
    }

    public Option<FxThumb> getImage() {
        //GUICommon.debugMessage("Actor2 getImage " + getName());
        return this.getImage(this.localImage);
    }

    public Option<FxThumb> getNetImage() {
        //GUICommon.debugMessage("Actor2 getNetImage " + getName());
        return this.getImage(Option.none());
    }

    private Option<FxThumb> getImage(Option<FxThumb> fxThumbs) {
        if (this.customData.flatMap(CustomData::getCustomImage).isDefined()) {
            return this.customData.flatMap(CustomData::getCustomImage);
        }

        for (Option<NetRecord> record : this.data) {
            if (fxThumbs.isEmpty()) {
                fxThumbs = record.map(NetRecord::getActorImage).getOrElse(Option.none());
            }
        }

        return fxThumbs;
    }

    public Boolean writeImageToFile(File fileNameToWrite) {
        return this.getImage()
                .map(t -> t.writeURLToFile(fileNameToWrite))
                .getOrElse(false);
    }

    public void register(DirectoryEntry movieDE) {
        this.movieList = this.movieList.remove(movieDE).append(movieDE);
    }

    public void unregister(DirectoryEntry movieDE) {
        this.movieList = this.movieList.remove(movieDE);
    }

    public void setMovieList(Stream<DirectoryEntry> list) {
        this.movieList = list;
    }

    public void refreshMovieList() {
        this.movieList = MovieDB.getInstance()
                .findByActor(this.getName())
                .peek(p -> GUICommon.debugMessage(p.toString()))
                .flatMap(p -> MovieScanner.getInstance().getEntryFromPath(p));
        this.movieCount = this.movieList.length();
    }

    private int getMovieCount() {
        if (this.movieCount < 0) {
            this.movieCount = MovieDB.getInstance().getActorCount(this.getName());
        }
        return this.movieCount;
    }

    public Stream<DirectoryEntry> getMovieList() {
        return this.movieList;
    }

    public void setCustomData(Option<CustomData> data) {
        this.customData = data;
    }

    public CustomData getCustomData() {
        return this.customData.getOrElse(new CustomData());
    }

    public boolean hasInfo() {
        if (this.yomi == null) {
            this.yomi = "";
        }

        return !this.getYomi().equals("");
    }

    public static class NetRecord {
        private final Source siteID;
        private final Option<FxThumb> actorImage;
        private final String url;
        private final String desc;

        @Contract(pure = true)
        NetRecord() {
            this.actorImage = Option.none();
            this.siteID = Source.NONE;
            this.url = "";
            this.desc = "";
        }

        @Contract(pure = true)
        @java.beans.ConstructorProperties({"siteID", "actorImage", "url", "desc"})
        NetRecord(Source siteID, Option<FxThumb> actorImage, String url, String desc) {
            this.siteID = siteID;
            this.actorImage = actorImage;
            this.url = url;
            this.desc = desc;
        }

        public Source getSiteID() {
            return this.siteID;
        }

        public Option<FxThumb> getActorImage() {
            return this.actorImage;
        }

        public String getUrl() {
            return this.url;
        }

        public String getDesc() {
            return this.desc;
        }

        @Override
        public String toString() {
            return "ActorV2.NetRecord(siteID=" + this.siteID + ", actorImage=" + this.actorImage + ", url=" + this.url + ", desc=" + this.desc + ")";
        }
    }

    public static class CustomData {
        private String customYomi = "";
        private LocalDate customBirthDate = null;
        private String customSize = "";
        private Option<FxThumb> customImage;

        private String tall = "";
        private String bSize = "";
        private String wSize = "";
        private String hSize = "";
        private String cupSize = "";

        @Contract(pure = true)
        CustomData() {
            this.customImage = Option.none();
        }

        public String getCustomYomi() {
            return this.customYomi;
        }

        public void setCustomYomi(String customYomi) {
            this.customYomi = customYomi;
        }

        public LocalDate getCustomBirthDate() {
            return this.customBirthDate;
        }

        public void setCustomBirthDate(LocalDate customBirthDate) {
            this.customBirthDate = customBirthDate;
        }

        public String getCustomSize() {
            return this.customSize;
        }

        public void setCustomSize(String customSize) {
            this.customSize = customSize;
        }

        public String getTall() {
            if (this.tall == null) {
                this.tall = "";
            }
            return this.tall;
        }

        public void setTall(String tall) {
            this.tall = tall;
        }

        public String getBSize() {
            if (this.bSize == null) {
                this.bSize = "";
            }
            return this.bSize;
        }

        public void setBSize(String bSize) {
            this.bSize = bSize;
        }

        public String getWSize() {
            if (this.wSize == null) {
                this.wSize = "";
            }
            return this.wSize;
        }

        public void setWSize(String wSize) {
            this.wSize = wSize;
        }

        public String getHSize() {
            if (this.hSize == null) {
                this.hSize = "";
            }
            return this.hSize;
        }

        public void setHSize(String hSize) {
            this.hSize = hSize;
        }

        public String getCupSize() {
            if (this.cupSize == null) {
                this.cupSize = "";
            }
            return this.cupSize;
        }

        public void setCupSize(String cupSize) {
            this.cupSize = cupSize;
        }

        public Option<FxThumb> getCustomImage() {
            if (this.customImage == null) {
                this.customImage = Option.none();
            }
            return this.customImage;
        }

        public void setCustomImage(Option<FxThumb> customImage) {
            this.customImage = customImage;
        }
    }

}
