package org.nagoya.system.event;


import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.jetbrains.annotations.NotNull;
import org.nagoya.system.FXMLController;
import org.nagoya.system.Systems;

import java.util.function.Consumer;

public abstract class FXContextImp extends FXMLController implements EventContext {

    protected Map<CustomEventType<?>, Consumer<Object>> eventConsumerMap = HashMap.empty();

    @Override
    public abstract void registerListener();

    @Override
    public void unregisterListener() {
        this.eventConsumerMap.forEach((t, c) -> Systems.getEventDispatcher().unregister(t, this));
    }

    protected <T> void registerListener(CustomEventType<T> eventType, Consumer<CustomEvent<T>> eventConsumer) {
        Systems.getEventDispatcher().register(eventType, this);
        Consumer<Object> consumer = o -> eventConsumer.accept((CustomEvent<T>) o);
        this.eventConsumerMap = this.eventConsumerMap.put(eventType, consumer);
    }

    @Override
    public <T> void fireEvent(CustomEventType<T> eventType, T object) {
        CustomEventSourceImp.fire(eventType, object);
    }

    @Override
    public <T> void executeEvent(@NotNull CustomEvent<T> e) {
        this.eventConsumerMap.get(e.getType()).peek(consumer -> consumer.accept(e));
    }
}