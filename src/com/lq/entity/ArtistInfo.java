package com.lq.entity;

public class ArtistInfo {
	private String artist_name;
	private int number_of_tracks;
	private int number_of_albums;

	public int getNumberOfAlbums() {
		return number_of_albums;
	}

	public void setNumberOfAlbums(int number_of_albums) {
		this.number_of_albums = number_of_albums;
	}

	public int getNumberOfTracks() {
		return number_of_tracks;
	}

	public void setNumberOfTracks(int number_of_tracks) {
		this.number_of_tracks = number_of_tracks;
	}

	public String getArtistName() {
		return artist_name;
	}

	public void setArtistName(String artist_name) {
		this.artist_name = artist_name;
	}

}
