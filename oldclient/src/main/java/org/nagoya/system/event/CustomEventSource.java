package org.nagoya.system.event;

public interface CustomEventSource {
    <T> void fireEvent(CustomEventType<T> eventType, T object);
}

