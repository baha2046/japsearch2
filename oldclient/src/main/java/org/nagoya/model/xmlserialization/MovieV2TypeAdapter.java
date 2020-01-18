package org.nagoya.model.xmlserialization;

import com.google.gson.JsonElement;
import org.nagoya.GUICommon;
import org.nagoya.model.MovieV2;

import java.util.ArrayList;

public class MovieV2TypeAdapter extends CustomizedTypeAdapterFactory<MovieV2> {
    public MovieV2TypeAdapter() {
        super(MovieV2.class);
    }

    @Override
    protected void beforeWrite(MovieV2 source, JsonElement toSerialize) {
        GUICommon.debugMessage("beforeWrite");
        var objToRemove = new ArrayList<>();

        for (var valueEntry : toSerialize.getAsJsonObject().entrySet()) {
            switch (valueEntry.getKey()) {
                //case "imgFrontCover":
                //case "imgBackCover":
                //case "imgExtras":
                case "actorList":
                    objToRemove.add(valueEntry);
                    break;
            }
        }

        objToRemove.forEach(v -> toSerialize.getAsJsonObject().entrySet().remove(v));
        GUICommon.debugMessage("beforeWrite End");
        //toSerialize.getAsJsonObject().entrySet() = toSerialize.getAsJsonObject().entrySet().remove(source.getImgFrontCover());
        //toSerialize.getAsJsonObject().entrySet().remove(source.getImgBackCover());
        //toSerialize.getAsJsonObject().entrySet().remove(source.getImgExtras());
    }

    @Override
    protected void afterRead(JsonElement deserialized) {
        //JsonObject custom = deserialized.getAsJsonObject().get("custom").getAsJsonObject();
        //custom.remove("size");
        GUICommon.debugMessage("afterRead End");
    }
}
