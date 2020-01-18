package org.nagoya.system.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.Stream;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.gson.VavrGson;
import org.hildan.fxgson.FxGson;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.io.Setting;
import org.nagoya.model.MovieStore;
import org.nagoya.model.MovieV2;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.model.dataitem.ID;
import org.nagoya.model.xmlserialization.FutureTypeAdapter;
import org.nagoya.model.xmlserialization.PathTypeAdapter;
import org.nagoya.preferences.GuiSettings;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

public class MovieDB {

    static final String cacheFileName = "movie.ini";
    static final String actorFileName = "actors.ini";
    static final String directorFileName = "director_cache.ini";
    static final String makerFileName = "maker_cache.ini";

    private static MovieDB INSTANCE = null;

    private final ActorDB actorDB;
    private final DirectorDB directorDB;
    private final MakerDB makerDB;

    public final Gson gson;
    private HashMap<String, Tuple3<FileTime, MovieStore, Integer>> map;

    @Contract(pure = true)
    public static MovieDB getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MovieDB();
        }
        return INSTANCE;
    }

    public static ActorDB actorDB() {
        return getInstance().actorDB;
    }

    public static DirectorDB directorDB() {
        return getInstance().directorDB;
    }

    public static MakerDB makerDB() {
        return getInstance().makerDB;
    }

    private MovieDB() {
        this.actorDB = new ActorDB(actorFileName);
        this.directorDB = new DirectorDB(directorFileName);
        this.makerDB = new MakerDB(makerFileName);

        GsonBuilder builder = FxGson.coreBuilder().setPrettyPrinting();
        VavrGson.registerAll(builder);
        builder.registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter());
        Type actorList = TypeToken.getParameterized(ArrayList.class, ActorV2.class).getType();
        builder.registerTypeHierarchyAdapter(Future.class, new FutureTypeAdapter<>(/*new TypeToken<ArrayList<ActorV2>>() {
        }.getType())*/actorList));

        GUICommon.debugMessage(">> Load Movie Cache");
        // builder.registerTypeAdapterFactory(new FxThumbTypeAdapterFactory());
        this.gson = builder.create();

        Path root = GuiSettings.getInstance().getDirectory(GuiSettings.Key.avDirectory);

        if (Files.exists(root.resolve(cacheFileName))) {
            Type tuple3Type = TypeToken.getParameterized(Tuple3.class, FileTime.class, MovieStore.class, Integer.class).getType();
            Type hashMapType = TypeToken.getParameterized(HashMap.class, String.class, tuple3Type).getType();

            this.map = Setting.readType(hashMapType, root.resolve(cacheFileName), HashMap.empty(), this.gson);
            this.map = this.map.mapValues(v -> v.update3(0));

            //this.map.valuesIterator().forEach(t -> t._2.versionFix());
            // GUICommon.debugMessage(mtest.toString());
        } else {
            this.map = HashMap.empty();
        }

    }

    public void saveCacheFile() {
        this.actorDB.saveFile();
        this.makerDB.saveFile();
        this.directorDB.saveFile();

        this.map.filterValues(v -> v._3 == 0).forEach(v -> GUICommon.debugMessage("Remove " + v._1));

        if (MovieScanner.getInstance().isLoadDone()) {
            this.map = this.map.filterValues(v -> v._3 > 0);
        }

        Path root = GuiSettings.getInstance().getDirectory(GuiSettings.Key.avDirectory);

        UtilCommon.saveStringToFile(root.resolve(cacheFileName), this.gson.toJson(this.map));
    }

    public void clearUpUnusedCache() {
        //this.map = this.map.filterValues(v -> v._3 > 0);
    }

    public Option<MovieV2> loadFromCache(@NotNull Path path) {
        return this.map.get(path.toString())
                .filter(t -> t._1.equals(Try.of(() -> Files.getLastModifiedTime(path)).getOrNull()))
                .peek(t -> this.map = this.map.replaceValue(path.toString(), t.update3(t._3 + 1)))
                //.peek(t -> GUICommon.debugMessage(() -> "Load movie from cache " + path.toString()))
                .map(Tuple3::_2)
                .map(MovieV2::of);
    }

    public void putCache(Path path, MovieV2 movieV2, FileTime fileTime) {
        if (movieV2 == null || fileTime == null) {
            return;
        }

        if (this.map.get(path.toString()).isDefined()) {
            this.removeCache(path.toString());
        }

        GUICommon.debugMessage("MovieV2Cache >> put cache >> " + movieV2.getMovieTitle());
        this.map = this.map.put(path.toString(), Tuple.of(fileTime, movieV2.toStore(), 1));
    }

    public void removeCache(@NotNull String path) {
        GUICommon.debugMessage("MovieV2Cache >> remove cache >> " + path);
        this.map = this.map.remove(path);
    }

    public Option<Path> findByMovieID(ID movieID) {
        return this.map
                .find(v -> (v._2._3 > 0) && v._2._2.movieID.equalsJavID(movieID))
                .map(i -> i._1)
                .map(Path::of)
                .map(Path::getParent);
    }

    public boolean isMovieExist(String strId) {
        return this.map.find(v -> (v._2._3 > 0) && v._2._2.movieID.getId().equals(strId)).isDefined();
    }

    public Stream<Path> findByActor(String actorName) {
        return this.map
                .filter(v -> (v._2._3 > 0) && v._2._2.actorList.stream().anyMatch(a -> a.getNameString().contains(actorName)))
                .map(i -> i._1)
                .map(Path::of)
                .peek(p -> GUICommon.debugMessage(p.toString()))
                .map(Path::getParent)
                .toStream();
    }

    public int getActorCount(String actorName) {
        return this.map
                .map(v -> v._2._2.actorList)
                .filter(v -> v.stream().anyMatch(a -> a.getNameString().equals(actorName)))
                .length();
    }

  /*  public static final class Adapter extends TypeAdapter<Tuple3<FileTime, MovieV2, Integer>> {
        private final Gson gson;

        public Adapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Tuple3<FileTime, MovieV2, Integer> value) throws IOException {
            out.beginArray();
            this.gson.getAdapter(FileTime.class).write(out, value._1);
            this.gson.getAdapter(MovieV2.class).write(out, value._2);
            this.gson.getAdapter(Integer.class).write(out, value._3);
            out.endArray();
        }

        @Override
        public Tuple3<FileTime, MovieV2, Integer> read(JsonReader in) throws IOException {
            in.beginArray();
            //String path = in.nextString();
            FileTime p1 = this.gson.getAdapter(FileTime.class).read(in);
            MovieV2 p2 = this.gson.getAdapter(MovieV2.class).read(in);
            Integer p3 = this.gson.getAdapter(Integer.class).read(in);
            in.endArray();

            return Tuple.of(p1, p2, p3);
        }
    }

    public static final TypeAdapterFactory Factory = new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!type.getRawType().equals(Tuple3.class)) {
                return null;
            }

            TypeAdapter<T> casted = (TypeAdapter<T>) new Adapter(gson);

            return casted;
        }
    };
*/
}
