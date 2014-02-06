package com.lq.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lq.xpressmusic.R;
import com.lq.entity.FolderInfo;
/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class FolderAdapter extends BaseAdapter {
	private List<FolderInfo> mData = null;
	private Context mContext = null;

	/** 默认初始化构造一个长度为0的数据列表 */
	public FolderAdapter(Context context) {
		mContext = context;
		mData = new ArrayList<FolderInfo>();
	}

	public void setData(List<FolderInfo> data) {
		mData.clear();
		if (data != null) {
			mData.addAll(data);
		}
		notifyDataSetChanged();
	}

	public List<FolderInfo> getData() {
		return mData;
	}

	@Override
	public boolean isEmpty() {
		return mData.size() == 0;
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public FolderInfo getItem(int position) {
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.list_item_folder, null);
			holder.folder_name = (TextView) convertView
					.findViewById(R.id.folder_name);
			holder.folder_path = (TextView) convertView
					.findViewById(R.id.folder_path);
			holder.song_count_of_folder = (TextView) convertView
					.findViewById(R.id.song_count_of_folder);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.folder_name.setText(mData.get(position).getFolderName());
		holder.folder_path.setText(mData.get(position).getFolderPath());
		holder.song_count_of_folder.setText(""
				+ mData.get(position).getNumOfTracks());

		return convertView;
	}

	static class ViewHolder {
		TextView folder_name;
		TextView folder_path;
		TextView song_count_of_folder;
	}
}
