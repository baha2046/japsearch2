package org.nagoya.system.event;

public interface CustomEventListener {
    <T> void executeEvent(CustomEvent<T> e);
}
