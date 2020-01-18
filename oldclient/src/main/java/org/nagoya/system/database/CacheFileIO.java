package org.nagoya.system.database;

import com.google.gson.reflect.TypeToken;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.io.Setting;
import org.nagoya.preferences.GuiSettings;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public class CacheFileIO<K, V> {
    private Path rootPath;
    private final String fileName;
    private final Type mapType;
    private Map<K, V> cacheMap;
    private Map<K, Integer> useCountMap;

    @Contract(pure = true)
    public CacheFileIO(String fileName, Class<K> keyClass, Class<V> valueClass) {
        this.rootPath = GuiSettings.getInstance().getDirectory(GuiSettings.Key.avDirectory);
        this.fileName = fileName;
        this.cacheMap = HashMap.empty();
        this.useCountMap = HashMap.empty();
        this.mapType = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
    }

    public void load() {
        if (Files.exists(this.rootPath.resolve(this.fileName))) {
            this.cacheMap = Setting.readType(this.mapType, this.rootPath.resolve(this.fileName), HashMap.empty(), Setting.DEFAULT_GSON);
        }
    }

    public void save() {
        Setting.writeSetting(this.cacheMap, this.rootPath.resolve(this.fileName));
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    public void put(@NotNull K key, V data) {
        this.addCount(key);
        this.cacheMap = this.cacheMap.put(key, data);
    }

    public Option<V> get(K key) {
        return this.cacheMap.get(key);
    }

    public Option<V> getAndCount(K key) {
        if (key == null || key == "") {
            return Option.none();
        }
        Option<V> value = this.cacheMap.get(key);
        value.peek(v -> this.addCount(key));
        return value;
    }

    public void remove(K key) {
        this.useCountMap = this.useCountMap.remove(key);
        this.cacheMap = this.cacheMap.remove(key);
    }

    public Seq<V> values() {
        return this.cacheMap.values();
    }

    private void addCount(K key) {
        Option<Integer> counter = this.useCountMap.get(key);
        Integer count = counter.getOrElse(0) + 1;
        this.useCountMap = this.useCountMap.put(key, count);
    }
}

/*
class CustomType implements ParameterizedType {
    private final Class<?> container;
    private final Class<?>[] wrapped;

    @Contract(pure = true)
    public CustomType(Class<?> container, Class<?>... wrapped) {
        this.container = container;
        this.wrapped = wrapped;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return this.wrapped;
    }

    @Override
    public Type getRawType() {
        return this.container;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }
}
*/
