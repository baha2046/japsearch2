package org.nagoya.preferences.options;

import java.util.function.Consumer;

public class OptionFactory {
    public static <T> OptionBase<?> create(String key, T value, Consumer<T> func) {
        if (value instanceof Boolean) {
            return new BooleanOption(key, (Boolean) value, (Consumer<Boolean>) func);
        } else if (value instanceof Integer) {
            return new IntegerOption(key, (Integer) value, (Consumer<Integer>) func);
        }
        return new StringOption(key, (String) value, (Consumer<String>) func);
    }
}
