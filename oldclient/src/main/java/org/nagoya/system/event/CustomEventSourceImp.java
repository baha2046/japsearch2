package org.nagoya.system.event;

import org.jetbrains.annotations.NotNull;
import org.nagoya.system.Systems;

public class CustomEventSourceImp implements CustomEventSource {
    public static <T> void fire(@NotNull CustomEventType<T> eventType, T object) {
        Systems.getEventDispatcher().submit(eventType.createEvent(object));
    }

    @Override
    public <T> void fireEvent(@NotNull CustomEventType<T> eventType, T object) {
        CustomEventSourceImp.fire(eventType, object);
    }
}
