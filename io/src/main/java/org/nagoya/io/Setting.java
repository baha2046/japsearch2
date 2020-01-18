package org.nagoya.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.gson.VavrGson;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.hildan.fxgson.FxGson;
import org.jetbrains.annotations.NotNull;
import org.nagoya.commons.GUICommon;
import org.nagoya.io.serialization.PathTypeAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Setting {
    public static final @NotNull Gson DEFAULT_GSON = getDefaultGson();

    @NotNull
    public static Gson getDefaultGson() {
        GsonBuilder builder = FxGson.coreBuilder().setPrettyPrinting();
        VavrGson.registerAll(builder);
        builder.registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter());
        return builder.create();
    }

    public static <R> R readSetting(Class<R> rClass, String strFileName) {
        return readSetting(rClass, Paths.get(strFileName), DEFAULT_GSON);
    }

    public static <R> R readSetting(Class<R> rClass, String strFileName, Gson gson) {
        return readSetting(rClass, Paths.get(strFileName), gson);
    }

    public static <R> R readSetting(Class<R> rClass, Path path) {
        return readSetting(rClass, path, DEFAULT_GSON);
    }

    public static <R> R readSetting(Class<R> rClass, Path path, Gson gson) {

        if (Files.exists(path)) {
            GUICommon.debugMessage(() -> ">> SettingsV2 >> Read Setting File : " + path.toString());
            String str = FileIO.readStringFromFile(path);

            return Try.of(() -> gson.fromJson(str, rClass))
                    .onFailure(e -> GUICommon.debugMessage(() -> ">> SettingsV2 >> Error build GSon >> " + e.getMessage()))
                    .getOrNull();
        }

        return Try.of(() -> rClass.getDeclaredConstructor().newInstance()).getOrNull();
    }

    public static <R> R readType(Type type, Path path, R whenEmpty, Gson gson) {
        if (Files.exists(path)) {
            GUICommon.debugMessage(() -> ">> SettingsV2 >> Read File : " + path.toString());

            return Try.of(() -> (R) gson.fromJson(FileIO.readStringFromFile(path), type))
                    .onFailure(e -> GUICommon.debugMessage(() -> ">> SettingsV2 >> Error build GSon >> " + e.getMessage()))
                    .getOrElse(whenEmpty);
        }
        return whenEmpty;
    }

    public static <T> void writeSetting(T c, String strFileName) {
        writeSetting(c, Paths.get(strFileName), DEFAULT_GSON);
    }

    public static <T> void writeSetting(T c, Path path) {
        writeSetting(c, path, DEFAULT_GSON);
    }

    public static <T> void writeSetting(T c, String strFileName, @NotNull Gson gson) {
        writeSetting(c, Paths.get(strFileName), gson);
    }

    public static <T> void writeSetting(T c, Path path, @NotNull Gson gson) {
        //GUICommon.debugMessage(() -> ">> SettingsV2 >> Save Setting File : " + path.getFileName().toString());
        FileIO.saveStringToFile(path, gson.toJson(c));
    }

    public static Option<Void> savePropertiesOption(FileObject fileObject, Properties properties) {
        return trySaveProperties(fileObject, properties).toOption();
    }

    @NotNull
    public static Try<Void> trySaveProperties(FileObject fileObject, Properties properties) {
        return Try.run(() -> saveProperties(fileObject, properties));
    }

    public static void saveProperties(@NotNull FileObject fileObject, @NotNull Properties properties) throws IOException {
        FileContent fileContent = fileObject.getContent();
        OutputStream outputStream = fileContent.getOutputStream();
        properties.storeToXML(outputStream, "");
        outputStream.close();
        fileContent.close();
    }

    public static Option<Properties> loadPropertiesOption(FileObject fileObject) {
        return tryLoadProperties(fileObject).toOption();
    }

    @NotNull
    public static Try<Properties> tryLoadProperties(FileObject fileObject) {
        return Try.of(() -> loadProperties(fileObject));
    }

    @NotNull
    public static Properties loadProperties(@NotNull FileObject fileObject) throws IOException {
        FileContent fileContent = fileObject.getContent();
        InputStream inputStream = fileContent.getInputStream();
        Properties properties = new Properties();
        properties.loadFromXML(inputStream);
        inputStream.close();
        fileContent.close();
        return properties;
    }
}
