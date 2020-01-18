package org.nagoya.system.database;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class CacheDB<V> {

    final CacheFileIO<String, V> dataCache;

    @Contract(pure = true)
    public CacheDB(CacheFileIO<String, V> cacheFileIO) {
        this.dataCache = cacheFileIO;
        this.load();
    }

    public CacheDB(String fileName, Class<V> vClass) {
        this(new CacheFileIO<>(fileName, String.class, vClass));
    }

    public void load() {
        this.dataCache.load();
    }

    public void saveFile() {
        this.dataCache.save();
    }

    public V getOrElsePut(String name, Supplier<V> supplier) {
        Option<V> cacheData = this.dataCache.getAndCount(name);

        if (cacheData.isEmpty()) {
            V useData = supplier.get();
            this.putData(name, useData);
            return useData;
        }
        return cacheData.get();
    }

    public V getOrElsePut(@NotNull String name, V data) {
        Option<V> cacheData = this.dataCache.getAndCount(name);

        if (cacheData.isDefined()) {
            return cacheData.get();
        } else if (data != null) {
            this.putData(name, data);
        }

        return data;
    }

    public void putData(@NotNull String name, V data) {
        if (!name.equals("")) {
            this.dataCache.put(name, data);
        }
    }

    public Option<V> getData(@NotNull String name) {
        if (name.equals("")) {
            return Option.none();
        }
        return this.dataCache.get(name);
    }

    public Seq<V> getData(Predicate<? super V> predicate) {
        return this.dataCache.values().filter(predicate);
    }

    public void removeData(String name) {
        this.dataCache.remove(name);
    }
}
