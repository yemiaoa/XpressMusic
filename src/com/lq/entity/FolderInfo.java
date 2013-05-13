package com.lq.entity;

public class FolderInfo {
	private String folder_name;
	private String folder_path;
	private int num_of_tracks;

	public String getFolderName() {
		return folder_name;
	}

	public void setFolderName(String folder_name) {
		this.folder_name = folder_name;
	}

	public String getFolderPath() {
		return folder_path;
	}

	public void setFolderPath(String folder_path) {
		this.folder_path = folder_path;
	}

	public int getNumOfTracks() {
		return num_of_tracks;
	}

	public void setNumOfTracks(int num_of_tracks) {
		this.num_of_tracks = num_of_tracks;
	}
}
