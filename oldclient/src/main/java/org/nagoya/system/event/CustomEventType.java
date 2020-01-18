package org.nagoya.system.event;

import org.jetbrains.annotations.Contract;

public class CustomEventType<T> {
    private final String name;

    @Contract(pure = true)
    public CustomEventType(String name) {
        this.name = name;
    }

    public CustomEvent<T> createEvent(T value) {
        return new CustomEvent<>(value, this);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomEventType)) {
            return false;
        }

        CustomEventType<?> that = (CustomEventType<?>) o;

        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return "CustomEventType(name=" + this.name + ")";
    }
}
