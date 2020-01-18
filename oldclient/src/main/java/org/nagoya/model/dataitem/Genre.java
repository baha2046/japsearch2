package org.nagoya.model.dataitem;


public class Genre extends MovieDataItem {

	String genre;

	public Genre(String genre) {
		setGenre(genre);
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = sanitizeString(genre);
	}

	public Genre() {
		genre = "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Genre)) return false;

		Genre genre1 = (Genre) o;

		return genre.equals(genre1.genre);
	}

	@Override
	public int hashCode() {
		return genre.hashCode();
	}

	@Override
	public String toString() {
		return "Genre{" +
				"genre='" + genre + '\'' +
				'}';
	}
}
