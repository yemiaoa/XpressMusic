package com.lq.entity;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PlaylistInfo implements Parcelable {
	private int id;
	private String playlist_name;
	private long date_added;
	private long date_modified;
	private int num_of_members;

	public PlaylistInfo() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getNumOfMembers() {
		return num_of_members;
	}

	public void setNumOfMembers(int num_of_members) {
		this.num_of_members = num_of_members;
	}

	public String getPlaylistName() {
		return playlist_name;
	}

	public void setPlaylistName(String name) {
		this.playlist_name = name;
	}

	public long getDateAdded() {
		return date_added;
	}

	public void setDateAdded(long date_added) {
		this.date_added = date_added;
	}

	public long getDateModified() {
		return date_modified;
	}

	public void setDateModified(long date_modified) {
		this.date_modified = date_modified;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	// 写数据进行保存
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Bundle bundle = new Bundle();
		bundle.putInt("id", id);
		bundle.putString("name", playlist_name);
		bundle.putLong("date_added", date_added);
		bundle.putLong("date_modified", date_modified);
		bundle.putInt("num_of_members", num_of_members);
		dest.writeBundle(bundle);

	}

	// 用来创建自定义的Parcelable的对象
	public static final Parcelable.Creator<PlaylistInfo> CREATOR = new Parcelable.Creator<PlaylistInfo>() {
		public PlaylistInfo createFromParcel(Parcel in) {
			return new PlaylistInfo(in);
		}

		public PlaylistInfo[] newArray(int size) {
			return new PlaylistInfo[size];
		}
	};

	// 读数据进行恢复
	private PlaylistInfo(Parcel in) {
		Bundle bundle = in.readBundle();
		id = bundle.getInt("id");
		playlist_name = bundle.getString("name");
		date_added = bundle.getLong("date_added");
		date_modified = bundle.getLong("date_modified");
		num_of_members = bundle.getInt("num_of_members");
	}
}
