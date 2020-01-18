package org.nagoya.model.dataitem;

public class Runtime extends MovieDataItem {

	private String runtime;
	public static final Runtime BLANK_RUNTIME = new Runtime("");

	@Override
	public String toString() {
		return "Runtime [runtime=\"" + runtime + "\"" + dataItemSourceToString() + "]";
	}

	public String getRuntime() {
		return runtime;
	}

	public void setRuntime(String runtime) {
		this.runtime = sanitizeString(runtime);
	}

	public Runtime(String runtime) {
		super();
		setRuntime(runtime);
	}


	public Runtime() {
		runtime = "";
	}

}
