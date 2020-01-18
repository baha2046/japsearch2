package org.nagoya.model.xmlserialization;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

public class TreeItemTypeAdapter<T> extends TypeAdapter<TreeItem<T>> {

    private Gson gson;

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    private final Class<T> valueClass;

    @Contract("null -> fail")
    public TreeItemTypeAdapter(Class<T> valueClass) {
        if (valueClass == null) {
            throw new IllegalArgumentException();
        }
        this.valueClass = valueClass;
    }

    @NotNull
    @Contract(" -> new")
    public static TreeItemTypeAdapter<String> createStringTreeItemAdapter() {
        return new TreeItemTypeAdapter<>(String.class);
    }

    private void writeValue(JsonWriter writer, T t) throws IOException {
        if (this.gson == null) {
            writer.value(Objects.toString(t, null));
        } else {
            this.gson.toJson(t, this.valueClass, writer);
        }
    }

    private T readValue(JsonReader reader) throws IOException {
        if (this.gson == null) {
            Object value = reader.nextString();
            return (T) value;
        } else {
            return this.gson.fromJson(reader, this.valueClass);
        }
    }

    @Override
    public void write(@NotNull JsonWriter writer, @NotNull TreeItem<T> t) throws IOException {
        writer.beginObject().name("value");
        this.writeValue(writer, t.getValue());
        writer.name("children").beginArray();
        LinkedList<Iterator<TreeItem<T>>> iterators = new LinkedList<>();
        iterators.add(t.getChildren().iterator());
        while (!iterators.isEmpty()) {
            Iterator<TreeItem<T>> last = iterators.peekLast();
            if (last.hasNext()) {
                TreeItem<T> ti = last.next();
                writer.beginObject().name("value");
                this.writeValue(writer, ti.getValue());
                writer.name("children").beginArray();
                iterators.add(ti.getChildren().iterator());
            } else {
                writer.endArray().endObject();
                iterators.pollLast();
            }
        }
    }

    @Override
    public TreeItem<T> read(JsonReader reader) throws IOException {
        if (this.gson == null && !this.valueClass.getName().equals("java.lang.String")) {
            throw new IllegalStateException("cannot parse classes other than String without gson provided");
        }
        reader.beginObject();
        if (!"value".equals(reader.nextName())) {
            throw new IOException("value expected");
        }
        TreeItem<T> root = new TreeItem<>(this.readValue(reader));
        TreeItem<T> item = root;
        if (!"children".equals(reader.nextName())) {
            throw new IOException("children expected");
        }
        reader.beginArray();
        int depth = 1;
        while (depth > 0) {
            if (reader.hasNext()) {
                reader.beginObject();
                if (!"value".equals(reader.nextName())) {
                    throw new IOException("value expected");
                }
                TreeItem<T> newItem = new TreeItem<>(this.readValue(reader));
                item.getChildren().add(newItem);
                item = newItem;
                if (!"children".equals(reader.nextName())) {
                    throw new IOException("children expected");
                }
                reader.beginArray();
                depth++;
            } else {
                depth--;
                reader.endArray();
                reader.endObject();
                item = item.getParent();
            }

        }
        return root;
    }

}