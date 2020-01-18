package org.nagoya.model.dataitem;

import java.io.Serializable;
import java.net.URL;

public class ActorLink implements Serializable {
    final String nameString;
    final String imageUrlString;

    public ActorLink(String nameString, String imageUrlString) {
        this.nameString = nameString;
        this.imageUrlString = imageUrlString;
    }

    public ActorLink(ActorV2 actorV2) {
        this.nameString = actorV2.getName();
        this.imageUrlString =  actorV2.getNetImage().map(FxThumb::getThumbURL).map(URL::toString).getOrElse("");
    }

    public ActorV2 toActor() {
        return ActorV2.of(nameString, ActorV2.Source.LOCAL, "", imageUrlString, "");
    }

    public String getNameString() {
        return nameString;
    }

    public String getImageUrlString() {
        return imageUrlString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActorLink)) return false;

        ActorLink actorLink = (ActorLink) o;

        if (!nameString.equals(actorLink.nameString)) return false;
        return imageUrlString.equals(actorLink.imageUrlString);
    }

    @Override
    public int hashCode() {
        int result = nameString.hashCode();
        result = 31 * result + imageUrlString.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ActorLink{" +
                "nameString='" + nameString + '\'' +
                ", imageUrlString='" + imageUrlString + '\'' +
                '}';
    }
}
