package org.nagoya.model;

import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;
import org.nagoya.model.dataitem.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MovieStore implements Serializable {
    public ID movieID;
    public String makerString = "";
    public String labelString = "";
    public String trailerString = "";
    public String mpaaString = "";
    public String directorString = "";
    public String plotString = "";
    public String releaseDateString = "";
    public String releaseYearString = "";
    public String titleString = "";
    public String seriesString = "";
    public String timeString = "";

    public List<Genre> genreList = new ArrayList<>();
    public List<ActorLink> actorList = new ArrayList<>();

    public Option<FxThumb> imgFrontCover = Option.none();
    public Option<FxThumb> imgBackCover = Option.none();
    public Stream<FxThumb> imgExtras = Stream.empty();

    public MovieStore() {

    }

    public MovieStore(@NotNull MovieV2 movie) {
        this.movieID = new ID(movie.movieIdProperty.get());

        this.makerString = movie.makerProperty.get();
        this.labelString = movie.studioProperty.get();
        this.titleString = movie.titleProperty.get();
        this.releaseDateString = movie.dateProperty.get();
        this.releaseYearString = movie.yearProperty.get();
        this.seriesString = movie.seriesProperty.get();
        this.plotString = movie.plotProperty.get();
        this.timeString = movie.timeProperty.get();
        this.mpaaString = movie.mpaaProperty.get();
        this.trailerString = movie.trailerProperty.get();
        this.directorString = movie.directorProperty.get();

        this.imgFrontCover = movie.imgFrontProperty.get();
        this.imgBackCover = movie.imgBackProperty.get();
        this.imgExtras = movie.extraImgProperty.get();

        this.genreList = movie.genresProperty.get();
        this.actorList = movie.actorProperty.get().stream().map(ActorLink::new).collect(Collectors.toList());
    }

    public MovieStore(List<ActorLink> actors, String director,
                      Stream<FxThumb> extraArt, List<Genre> genres,
                      ID id, String mpaa, String plot,
                      Option<FxThumb> backCover, Option<FxThumb> frontCover,
                      Rating rating, String releaseDate, String year,
                      String runtime, String set, String label, String maker,
                      String title, String trailer) {
        this();
        this.movieID = id;
        this.makerString = maker;
        this.titleString = title;

        this.seriesString = set;
        this.labelString = label;
        this.plotString = plot;


        this.releaseDateString = releaseDate;
        this.timeString = runtime;

        this.releaseYearString = year;
        this.mpaaString = mpaa;
        this.trailerString = trailer;

        this.imgFrontCover = frontCover;
        this.imgBackCover = backCover;

        this.directorString = director;

        this.actorList = actors;
        this.imgExtras = extraArt;
        this.genreList = genres;
    }

    public ID getMovieID() {
        return this.movieID;
    }


    public void versionFix() {
        //this.directorString = this.director.map(DirectorV2::getStrName).getOrElse("");

        // this.plotString = this.plots.map(Plot::getPlot).getOrElse("");
        //this.releaseDateString = this.releaseDates.map(ReleaseDate::getReleaseDate).getOrElse("");
        //this.releaseYearString = this.years.map(Year::getYear).getOrElse("");
        //this.titleString = this.movieTitle.getTitle();
        //this.seriesString = this.sets.map(Set::getSet).getOrElse("");
    }
}
