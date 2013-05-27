package com.lq.entity;

import com.lq.util.StringHelper;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class TrackInfo implements Parcelable {

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

	/** 歌曲标题索引，用来搜索、排序用 */
	private String title_key;

	/** 艺术家名称索引，用来搜索、排序用 */
	private String artist_key;

	public TrackInfo() {

	}

	public String getArtistKey() {
		return artist_key;
	}

	public String getTitleKey() {
		return title_key;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TrackInfo) {
			TrackInfo object = (TrackInfo) o;
			return object.getId() == this.id;
		} else {
			return super.equals(o);
		}
	}

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
		this.title_key = StringHelper.getPingYin(title);
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
		this.artist_key = StringHelper.getPingYin(artist);
	}

	public String getDisplayName() {
		return display_name;
	}

	public void setDisplayName(String display_name) {
		this.display_name = display_name;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	// 写数据进行保存
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Bundle bundle = new Bundle();
		bundle.putLong("id", id);
		bundle.putString("title", title);
		bundle.putString("display_name", display_name);
		bundle.putString("album", album);
		bundle.putString("artist", artist);
		bundle.putString("data", data);
		bundle.putLong("size", size);
		bundle.putLong("duration", duration);
		dest.writeBundle(bundle);
	}

	// 用来创建自定义的Parcelable的对象
	public static final Parcelable.Creator<TrackInfo> CREATOR = new Parcelable.Creator<TrackInfo>() {
		public TrackInfo createFromParcel(Parcel in) {
			return new TrackInfo(in);
		}

		public TrackInfo[] newArray(int size) {
			return new TrackInfo[size];
		}
	};

	// 读数据进行恢复
	private TrackInfo(Parcel in) {
		Bundle bundle = in.readBundle();
		id = bundle.getLong("id");
		title = bundle.getString("title");
		display_name = bundle.getString("display_name");
		album = bundle.getString("album");
		artist = bundle.getString("artist");
		data = bundle.getString("data");
		size = bundle.getLong("size");
		duration = bundle.getLong("duration");
	}

}
