package org.nagoya.system.event;

public interface EventContext extends CustomEventListener, CustomEventSource {
    void registerListener();

    void unregisterListener();
}
