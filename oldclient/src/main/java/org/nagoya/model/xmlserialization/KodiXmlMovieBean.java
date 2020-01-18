package org.nagoya.model.xmlserialization;

import com.thoughtworks.xstream.XStream;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.nagoya.GUICommon;
import org.nagoya.model.MovieStore;
import org.nagoya.model.MovieV2;
import org.nagoya.model.dataitem.*;
import org.nagoya.preferences.RenameSettings;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Class which handles serializing a MovieV2 object to and from XML
 */
public class KodiXmlMovieBean {

    private final String title;
    private final String originaltitle;
    private final String sorttitle;
    private final String rating;
    private final String top250;
    private final String outline;
    private final String plot;
    private final String tagline;
    private final String runtime;
    private final KodiXmlThumbBean thumb[];
    private final String mpaa;
    private final String votes;
    private final KodiXmlFanartBean fanart;
    private final String id;
    private final KodiXmlUniqueidBean uniqueid;
    private final String[] genre;
    private final KodiXmlSetBean set;
    private final String tag;
    private final String[] director;
    private final String premiered;
    private final String year;
    private final String studio;
    private final String maker;
    private final String releasedate;
    private final String trailer;
    // private String[] credits; add in later
    private final ArrayList<KodiXmlActorBean> actor;


    /**
     * Constructor - handles conversion of a MovieV2 object to a KodiXmlMovieBean object.
     * Program preferences are taken into account when performing the object conversion so that, for example,
     * certain fields will not be written to the XML
     */
    public KodiXmlMovieBean(MovieV2 movieV2) {

        MovieStore movie = new MovieStore(movieV2);

        this.id = movie.movieID.getId();
        this.uniqueid = new KodiXmlUniqueidBean(this.id);

        this.title = movie.titleString;
        this.maker = movie.makerString;
        this.originaltitle = this.title;
        this.sorttitle = "";

        this.set = new KodiXmlSetBean(movie.seriesString, "");
        this.year = movie.releaseYearString;
        this.trailer = movie.trailerString;
        //this.outline = movie.getOutlines().map(Outline::getOutline).getOrElse("");
        this.plot = movie.plotString;
        this.runtime = movie.timeString;
        this.premiered = movie.releaseDateString;
        this.studio = movie.labelString;
        this.mpaa = movie.mpaaString;
        this.votes = "";
        this.top250 = "";
        this.tagline = "";
        this.rating = "";
        this.tag = "";

        this.outline = this.plot;
        this.releasedate = this.premiered;

        //Posters
        this.thumb = new KodiXmlThumbBean[2];
        this.thumb[0] = new KodiXmlThumbBean("poster",
                RenameSettings.getFileNameFrontCover(RenameSettings.getSuitableFileName(movieV2)
                ));
        this.thumb[1] = new KodiXmlThumbBean("landscape",
                movie.imgBackCover.map(FxThumb::getThumbURL).map(URL::toString).getOrElse(""));

        if (movieV2.hasBackCover()) {
            FxThumb[] tmpThumbArray = new FxThumb[1];
            tmpThumbArray[0] = movie.imgBackCover.get();
            this.fanart = new KodiXmlFanartBean(tmpThumbArray);
        } else {
            this.fanart = new KodiXmlFanartBean(new FxThumb[0]);
        }

        // genre
        this.genre = new String[movie.genreList.size()];
        for (int i = 0; i < this.genre.length; i++) {
            this.genre[i] = movie.genreList.get(i).getGenre();
        }

        // director
        /*this.director = new String[movie.getDirectorList().size()];
        for (int i = 0; i < this.director.length; i++) {
            this.director[i] = movie.getDirectorList().get(i).getName();
        }*/
        this.director = new String[1];
        this.director[0] = movie.directorString;

        // actor
        this.actor = new ArrayList<>(movie.actorList.size());
        for (ActorLink currentActor : movie.actorList) {
            this.actor.add(new KodiXmlActorBean(currentActor.getNameString(), "", currentActor.getImageUrlString()));
        }

    }

    public static KodiXmlMovieBean makeFromXML(String xml) {
        XStream xstream = XStreamForNfo.getXStream();//KodiXmlMovieBean.getXMLSerializer();

        try {
            return (KodiXmlMovieBean) xstream.fromXML(xml);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("File read from nfo is not in Kodi XML format. This movie will not be read in.");
            return null;
        }
    }

    public MovieV2 toMovie(Path path) {

        Option<FxThumb> frontCover = Option.none();
        if (this.thumb != null && this.thumb.length >= 1) {
            String coverUrl = this.thumb[0].img;
            if (coverUrl.startsWith("file")) {
                String url = coverUrl;
                frontCover = Try.of(() -> URI.create(url))
                        .map(Paths::get)
                        .filter(Files::exists)
                        .map(FxThumb::of)
                        .toOption()
                        .onEmpty(() -> GUICommon.debugMessage("Wrong path frontCover: " + url));
            } else {
                if (coverUrl.contains("/")) {
                    coverUrl = coverUrl.substring(coverUrl.indexOf("/") + 1);
                }
                String url = coverUrl;
                frontCover = Try.of(() -> path.resolve(url))
                        .filter(Files::exists)
                        .map(FxThumb::of)
                        .toOption()
                        .onEmpty(() -> GUICommon.debugMessage("Wrong path frontCover: " + url));
            }
            //frontCover = this.thumb[0].toThumb();
        }

        Option<FxThumb> backCover = Option.none();
        if (this.fanart != null && this.fanart.getThumb() != null) {
            String backUrl = this.fanart.getThumb()[0];
            if (backUrl.startsWith("http")) {
                backCover = FxThumb.of(backUrl);
            } else if (backUrl.startsWith("file")) {
                String url = backUrl;
                backCover = Try.of(() -> URI.create(url))
                        .map(Paths::get)
                        .filter(Files::exists)
                        .map(FxThumb::of)
                        .toOption()
                        .onEmpty(() -> GUICommon.debugMessage("Wrong path backCover: " + url));
            } else {
                if (backUrl.contains("/")) {
                    backUrl = backUrl.substring(backUrl.indexOf("/") + 1);
                }
                GUICommon.debugMessage(backUrl);
                String url = backUrl;
                backCover = Try.of(() -> path.resolve(url))
                        .filter(Files::exists)
                        .map(FxThumb::of)
                        .toOption()
                        .onEmpty(() -> GUICommon.debugMessage("Wrong path backCover: " + url));
            }
        }

        ArrayList<ActorLink> actors = new ArrayList<>();
        if (this.actor != null) {
            actors = new ArrayList<>(this.actor.size());
            for (KodiXmlActorBean currentActor : this.actor) {
                actors.add(currentActor.toActor());
            }
        }

        ArrayList<Genre> genres = new ArrayList<>();
        if (this.genre != null) {
            for (String aGenre : this.genre) {
                genres.add(new Genre(aGenre));
            }
        }

        String directorName = "";
        if (this.director != null && this.director.length > 0) {
            directorName = this.director[0];
        }

        return MovieV2.of(new MovieStore(actors, directorName, Stream.empty(), genres,
                new ID(this.id), this.mpaa, this.plot,
                backCover, frontCover,
                new Rating(10, this.rating),
                this.releasedate, this.year,
                this.runtime, this.set.getName(),
                this.studio, this.maker, this.title, this.trailer));
    }

    public String toXML() {
        return XStreamForNfo.getXStream().toXML(this);
    }

}
