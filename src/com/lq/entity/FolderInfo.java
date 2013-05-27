package com.lq.entity;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class FolderInfo implements Parcelable {
	private String folder_name;
	private String folder_path;
	private int num_of_tracks;

	public FolderInfo() {

	}

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

	@Override
	public int describeContents() {
		return 0;
	}

	// 写数据进行保存
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Bundle bundle = new Bundle();
		bundle.putString("folder_name", folder_name);
		bundle.putString("folder_path", folder_path);
		bundle.putInt("num_of_tracks", num_of_tracks);
		dest.writeBundle(bundle);

	}

	// 用来创建自定义的Parcelable的对象
	public static final Parcelable.Creator<FolderInfo> CREATOR = new Parcelable.Creator<FolderInfo>() {
		public FolderInfo createFromParcel(Parcel in) {
			return new FolderInfo(in);
		}

		public FolderInfo[] newArray(int size) {
			return new FolderInfo[size];
		}
	};

	// 读数据进行恢复
	private FolderInfo(Parcel in) {
		Bundle bundle = in.readBundle();
		folder_name = bundle.getString("folder_name");
		folder_path = bundle.getString("folder_path");
		num_of_tracks = bundle.getInt("num_of_tracks");

	}
}
