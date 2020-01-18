package org.nagoya;

import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.view.dialog.FXProgressDialog;
import reactor.core.publisher.Mono;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class UtilCommon {

    public static final BiPredicate<Path, String> checkFileExt = (path, string) -> path.getFileName().toString().toLowerCase().endsWith(string);

    public static <T> Mono<T> tryToMono(Supplier<Try<T>> trySupplier) {
        return Mono.create(emitter -> trySupplier.get()
                .onSuccess(emitter::success)
                .onFailure(emitter::error)
                .onFailure(GUICommon::errorDialog));
    }

    public static Option<URL> pathToUrl(Path path) {
        return Try.of(() -> path.toUri().toURL())
                .onFailure(Throwable::printStackTrace)
                .toOption();
    }

    @NotNull
    @Contract(pure = true)
    public static String fileSizeString(long size) {
        size = size / 1024 / 1024;
        if (size < 1L) {
            size = 0L;
        }
        return size > 0L ? size + " MB" : "";
    }

    @NotNull
    public static Try<Path> tryCreateDirectory(Path path) {
        return Try.run(() -> FileUtils.forceMkdir(path.toFile())).map(v -> path);
    }

    @NotNull
    public static Boolean createDirectory(Path path, Option<ObservableList<String>> outText) {
        if (Files.exists(path) && Files.isDirectory(path)) {
            return true;
        }
        return tryCreateDirectory(path)
                .onSuccess(p -> GUICommon.writeToObList(">> Create Directory >> " + p.getFileName().toString(), outText))
                .onFailure(e -> GUICommon.writeToObList(">> Error at Create Directory : " + e.toString(), outText))
                .isSuccess();
    }

    @NotNull
    public static Boolean saveImageToFile(@NotNull Path path, @NotNull Function<File, Boolean> function, Option<ObservableList<String>> outText) {
        if (function.apply(path.toFile())) {
            GUICommon.writeToObListWithoutNewLine("Write Image >> " + path.getFileName().toString(), outText);
            return true;
        } else {
            GUICommon.writeToObList("Unable to writing Image File : " + path.getFileName().toString(), outText);
            return false;
        }
    }

    @NotNull
    public static Try<Void> trySaveUrlToFile(URL url, File file) {
        return Try.run(() -> FileUtils.copyURLToFile(url, file, 20000, 20000));
    }

    @NotNull
    public static Boolean saveUrlToFile(URL url, File file, Option<ObservableList<String>> outText) {
        return trySaveUrlToFile(url, file)
                .onSuccess(v -> GUICommon.writeToObList("Write Image >> " + file.toString() + " << " + url.toString(), outText))
                .onFailure(e -> GUICommon.writeToObList("Unable to Write Image : " + file.getName() + " << " + url.toString(), outText))
                .onFailure(e -> GUICommon.writeToObList(e.toString(), outText))
                .isSuccess();
    }

    @NotNull
    public static Try<Path> tryMoveFile(@NotNull Path oldName, @NotNull Path newName) {
        return Try.of(() -> Files.move(oldName, newName, StandardCopyOption.ATOMIC_MOVE));
    }

    public static Boolean moveFile(Path oldName, @NotNull Path newName, Option<ObservableList<String>> outText) {
        return tryMoveFile(oldName, newName)
                .onSuccess(v -> GUICommon.writeToObListWithoutNewLine("Move File >> " + newName.getFileName().toString(), outText))
                .onFailure(e -> GUICommon.writeToObList("Unable to Move File : " + newName.getFileName().toString(), outText))
                .onFailure(e -> GUICommon.writeToObList(e.toString(), outText))
                .isSuccess();
    }

    public static Try<Boolean> tryCopyFile(@NotNull Path source, @NotNull Path dest, Option<FXProgressDialog> fxProgressDialog) {
        return Try.of(() -> copyFile(source, dest, fxProgressDialog))
                .onFailure(GUICommon::errorDialog);
    }

    private static boolean copyFile(@NotNull Path source, @NotNull Path dest, @NotNull Option<FXProgressDialog> fxProgressDialog) throws IOException {

        FileChannel src = new FileInputStream(source.toFile()).getChannel();
        FileChannel dst = new FileOutputStream(dest.toFile()).getChannel();

        long pos = 0;
        long totalSize = src.size();
        long blockSize = 20000000;

        var list = fxProgressDialog.map(FXProgressDialog::getList);

        GUICommon.writeToObList("Copy File >> " + dest.toString() + " << " + source.toString(), list);

        while (pos < totalSize) {

            GUICommon.debugMessage("Pos " + pos + " T " + totalSize);

            double progress = (float) pos / totalSize;
            fxProgressDialog.peek(d -> d.setProgress(progress));

            long add = src.transferTo(pos, blockSize, dst);

            if (add == 0) {
                break;
            }
            pos += add;
        }

        dst.close();
        src.close();

        return (pos == totalSize);
    }


    @NotNull
    public static Try<Void> tryDeleteDirectoryAndContents(Path path) {
        return Try.run(() -> FileUtils.deleteDirectory(path.toFile()));
        /*return Try.withResources(() -> Files.walk(path))
                .of(s -> s.sorted(Comparator.reverseOrder()).map(Path::toFile).map(File::delete).allMatch(i -> i == true)
                );*/
    }

    @NotNull
    public static Try<Void> tryDeleteFile(Path file) {
        return Try.run(() -> Files.delete(file));
    }

    public static Try<Void> tryDeleteFileIfExist(Path file) {
        return Try.run(() -> Files.deleteIfExists(file));
    }

    public static Try<Void> saveFile(@NotNull URL url, File outputFile) {

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(url.toString());
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.11 Safari/537.36");
        httpGet.addHeader("Referer", GUICommon.customReferrer(url, null));

        Try<Void> result = Try.withResources(() -> httpClient.execute(httpGet))
                .of(HttpResponse::getEntity)
                .filter(Objects::nonNull)
                .flatMap(entity -> trySaveEntityContent(entity, outputFile));

        httpGet.releaseConnection();

        return result;
    }

    private static Try<Void> trySaveEntityContent(@NotNull HttpEntity entity, File destination) {
        return Try.withResources(entity::getContent)
                .of(s -> trySaveInputStream(s, destination))
                .flatMap(i -> i);
    }

    @NotNull
    private static Try<Void> trySaveInputStream(InputStream source, File destination) {
        return Try.run(() -> FileUtils.copyInputStreamToFile(source, destination));
    }

    public static void saveStringToFile(@NotNull Path path, @NotNull String strJSon) {

        //GUICommon.debugMessage(() -> strJSon);
        Try.withResources(() -> new FileOutputStream(path.toFile()))
                .of(s -> Try.of(() -> {
                    s.write(strJSon.getBytes());
                    s.flush();
                    return true;
                }))
                .flatMap(i -> i)
                .onFailure(e -> GUICommon.debugMessage(() -> ">> Error >> UtilCommon >> saveStringToFile >> " + e.getMessage()))
                .onFailure(GUICommon::errorDialog)
                .onSuccess(b -> GUICommon.debugMessage(() -> ">> Success >> UtilCommon >> saveStringToFile " + path.getFileName().toString()))
        ;
    }

    public static String readStringFromFile(@NotNull Path path) {

        StringBuilder stringBuilder = new StringBuilder();

        return Try.withResources(() -> new FileInputStream(path.toFile()))
                .of(s -> {
                    BufferedReader bReader = new BufferedReader(new InputStreamReader(s));
                    String receiveString;
                    while ((receiveString = bReader.readLine()) != null) {
                        stringBuilder.append(receiveString);
                    }
                    return stringBuilder.toString();
                })
                .onFailure(e -> GUICommon.debugMessage(() -> ">> Error >> UtilCommon >> readStringFromFile >> " + e.getMessage()))
                .onSuccess(b -> GUICommon.debugMessage(() -> ">> Success >> UtilCommon >> readStringFromFile " + path.getFileName().toString()))
                .getOrElse("")
                ;
    }

    @NotNull
    public static <T> Future<T> futureFromTry(@NotNull final Executor ex, final Supplier<Try<T>> s) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        ex.execute(() -> s.get().fold(cf::completeExceptionally, cf::complete));
        return Future.fromCompletableFuture(cf);
    }

    public static Option<Method> tryGetMethod(Class<?> c, String methodName, Class<?>... parameterTypes) {
        return Try.of(() -> c.getMethod(methodName, parameterTypes))
                .onFailure(e -> GUICommon.debugMessage(e.getMessage()))
                .toOption();
    }

    public static <N> Option<N> tryInvoke(Method method, Class<N> nClass, Object obj, Object... args) {
        return Try.of(() -> method.invoke(obj, args))
                .map(nClass::cast)
                .onFailure(GUICommon::errorDialog)
                .toOption();
    }
}
