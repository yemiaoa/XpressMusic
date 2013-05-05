package com.lq.entity;

public class MusicItem {

	/** 在MedieStore存储的主键 */
	private long id;

	/** 不带扩展名的文件名 */
	private String title;

	/** 文件名 */
	private String display_name;

	/** 专辑名，一般为文件夹名 */
	private String album;

	/** 艺术家 */
	private String artist;

	/** 文件的绝对路径 */
	private String data;

	/** 文件大小，单位为 byte */
	private long size;

	/** 时长 */
	private long duration;

	@Override
	public String toString() {
		return "song_id:" + id + ",song_title:" + title;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getDisplayName() {
		return display_name;
	}

	public void setDisplayName(String display_name) {
		this.display_name = display_name;
	}

}
