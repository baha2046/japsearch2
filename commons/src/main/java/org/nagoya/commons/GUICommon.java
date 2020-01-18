package org.nagoya.commons;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class GUICommon {
    public static Boolean DEBUG_MODE = false;

    private static Consumer<String> errorHandle = null;

    public static void setErrorHandle(Consumer<String> errorHandle) {
        GUICommon.errorHandle = errorHandle;
    }

    public static void debugMessage(String string) {
        if (DEBUG_MODE) {
            System.out.println(string);
        }
    }

    public static void debugThread() {
        if (DEBUG_MODE) {
            System.out.println(Thread.currentThread().getName());
        }
    }

    public static void debugMessage(@NotNull Supplier<String> string) {
        if (DEBUG_MODE) {
            System.out.println(string.get());
        }
    }

    public static void errorDialog(String errorMsg) {
        if (errorHandle != null) {
            errorHandle.accept(errorMsg);
        }
    }

    public static void errorDialog(@NotNull Throwable throwable) {
        throwable.printStackTrace();
        errorDialog(throwable.toString());
    }
}
