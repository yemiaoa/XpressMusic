package com.lq.entity;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class AlbumInfo implements Parcelable {
	/** 专辑名称 */
	private String album_name;

	/** 专辑在数据库中的ID */
	private int album_id;

	/** 专辑的歌曲数目 */
	private int number_of_songs;

	/** 专辑封面图片路径 */
	private String art_work;

	public String getArtWork() {
		return art_work;
	}

	public void setArtWork(String art_work) {
		this.art_work = art_work;
	}

	public AlbumInfo() {

	}

	public String getAlbumName() {
		return album_name;
	}

	public void setAlbumName(String album_name) {
		this.album_name = album_name;
	}

	public int getAlbumId() {
		return album_id;
	}

	public void setAlbumId(int album_id) {
		this.album_id = album_id;
	}

	public int getNumberOfSongs() {
		return number_of_songs;
	}

	public void setNumberOfSongs(int number_of_songs) {
		this.number_of_songs = number_of_songs;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	// 写数据进行保存
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Bundle bundle = new Bundle();
		bundle.putString("album_name", album_name);
		bundle.putString("art_work", art_work);
		bundle.putInt("number_of_songs", number_of_songs);
		bundle.putInt("album_id", album_id);
		dest.writeBundle(bundle);

	}

	// 用来创建自定义的Parcelable的对象
	public static final Parcelable.Creator<AlbumInfo> CREATOR = new Parcelable.Creator<AlbumInfo>() {
		public AlbumInfo createFromParcel(Parcel in) {
			return new AlbumInfo(in);
		}

		public AlbumInfo[] newArray(int size) {
			return new AlbumInfo[size];
		}
	};

	// 读数据进行恢复
	private AlbumInfo(Parcel in) {
		Bundle bundle = in.readBundle();
		album_name = bundle.getString("album_name");
		art_work = bundle.getString("art_work");
		number_of_songs = bundle.getInt("number_of_songs");
		album_id = bundle.getInt("album_id");
	}
}
