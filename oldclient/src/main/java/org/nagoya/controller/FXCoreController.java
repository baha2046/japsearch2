package org.nagoya.controller;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.system.event.EventContext;

public class FXCoreController {
    private static final FXCoreController instance = new FXCoreController();

    public static void addContext(@NotNull EventContext fxContext) {
        addContext(fxContext, fxContext.getClass().getSimpleName());
    }

    public static void addContext(@NotNull EventContext fxContext, String name) {
        GUICommon.debugMessage("FXCoreController - Register : " + name);
        instance.setEventContextMap(instance.getEventContextMap().put(name, fxContext));
        fxContext.registerListener();
    }

    public static void removeContext(EventContext fxContext) {
        removeContext(fxContext.getClass().getSimpleName());
    }

    public static void removeContext(String name) {
        GUICommon.debugMessage("FXCoreController - Unregister : " + name);
        instance.getEventContextMap().get(name).peek(EventContext::unregisterListener);
        instance.setEventContextMap(instance.getEventContextMap().remove(name));
    }

    private Map<String, EventContext> eventContextMap = HashMap.empty();

    private FXCoreController() {
    }

    public Map<String, EventContext> getEventContextMap() {
        return this.eventContextMap;
    }

    public void setEventContextMap(Map<String, EventContext> eventContextMap) {
        this.eventContextMap = eventContextMap;
    }
}
