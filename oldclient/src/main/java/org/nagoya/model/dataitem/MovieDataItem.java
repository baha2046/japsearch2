package org.nagoya.model.dataitem;

import java.io.Serializable;

public abstract class MovieDataItem implements Serializable {

	public static String sanitizeString(String inputString) {
		if (inputString != null)
			return inputString.replace("\u00a0", " ").trim(); //replace non breaking space (&nbsp) with regular space then trim things
		else
			return null;
	}

	public String dataItemSourceToString() {
		return "";
	}

	public boolean isStringValueEmpty() {
		String toStringValue = this.toString();
		if (toStringValue.contains("=\"\""))
			return false;
		else
			return true;
	}

}
