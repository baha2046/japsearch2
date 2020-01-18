package org.nagoya.commons;

import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.nio.file.Path;
import java.util.function.BiPredicate;
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
}
