package org.nagoya.model.dataitem;


public class Studio extends MovieDataItem {

    private String studio;
    public static final Studio BLANK_STUDIO = new Studio("");

    public String getStudio() {
        return this.studio;
    }

    public void setStudio(String studio) {
        this.studio = sanitizeString(studio);
    }

    @Override
    public String toString() {
        return "Studio [studio=\"" + this.studio + "\"" + this.dataItemSourceToString() + "]";
    }

    public Studio(String studio) {
		this.setStudio(studio);
    }


    public Studio() {
		this.studio = "";
    }

}
