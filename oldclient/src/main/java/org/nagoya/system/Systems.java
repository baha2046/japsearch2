package org.nagoya.system;

import javafx.scene.input.Clipboard;
import org.jetbrains.annotations.Contract;
import org.nagoya.GUICommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.io.WebService;
import org.nagoya.preferences.GeneralSettings;
import org.nagoya.system.dialog.JFXDialogPool;
import org.nagoya.system.event.EventDispatcher;

import java.util.concurrent.Future;

public class Systems {
    private static final JFXDialogPool dialogPool = new JFXDialogPool();
    private static final EventDispatcher eventDispatcher = new EventDispatcher();
    private static final DirectorySystem directorySystem = new DirectorySystem();

    private static final Clipboard clipboard = Clipboard.getSystemClipboard();

    @Contract(pure = true)
    public static GeneralSettings getPreferences() {
        return GeneralSettings.getInstance();
    }

    @Contract(pure = true)
    public static JFXDialogPool getDialogPool() {
        return Systems.dialogPool;
    }

    @Contract(pure = true)
    public static EventDispatcher getEventDispatcher() {
        return Systems.eventDispatcher;
    }

    @Contract(pure = true)
    public static WebService getWebService() {
        return WebService.get();
    }

    @Contract(pure = true)
    public static Clipboard getClipboard() {
        return clipboard;
    }


    public static DirectorySystem getDirectorySystem() {
        return Systems.directorySystem;
    }

    public static Future<?> useExecutors(Runnable run) {
        return ExecuteSystem.useExecutors(ExecuteSystem.role.NORMAL, run);
    }


    public static void shutdown() {
        GUICommon.debugMessage("Systems >> shutdown start");
        getDirectorySystem().shutdown();
        ExecuteSystem.get().shutdown();
        WebService.get().shutdown();
        GUICommon.debugMessage("Systems >> shutdown end");
    }
}
