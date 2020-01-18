package org.nagoya.io;

import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.nagoya.commons.GUICommon;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class FileIO {
    public static void saveStringToFile(@NotNull Path path, @NotNull String strJSon) {

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
}
