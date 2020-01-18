package org.nagoya.model.dataitem;


public class Votes extends MovieDataItem {

	private String votes;
	public static final Votes BLANK_VOTES = new Votes("");

	public String getVotes() {
		return votes;
	}

	public void setVotes(String votes) {
		this.votes = sanitizeString(votes);
	}

	public Votes(String votes) {
		super();
		setVotes(votes);
	}

	@Override
	public String toString() {
		return "Votes [votes=\"" + votes + "\"" + dataItemSourceToString() + "]";
	}


	public Votes() {
		votes = "";
	}

}
