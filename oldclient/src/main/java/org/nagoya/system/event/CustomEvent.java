package org.nagoya.system.event;

import org.jetbrains.annotations.Contract;

public class CustomEvent<T> {
    public final T value;
    public final CustomEventType<T> type;

    @Contract(pure = true)
    public CustomEvent(T value, CustomEventType<T> type) {
        this.value = value;
        this.type = type;
    }

    public T getParam() {
        return this.value;
    }

    public CustomEventType<T> getType() {
        return this.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CustomEvent)) {
            return false;
        }

        CustomEvent<?> that = (CustomEvent<?>) o;

        if (!this.value.equals(that.value)) {
            return false;
        }
        return this.type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = this.value.hashCode();
        result = 31 * result + this.type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CustomEvent{" +
                "value=" + this.value +
                ", type=" + this.type +
                '}';
    }
}
