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
import com.lq.entity.ArtistInfo;
/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class ArtistAdapter extends BaseAdapter {
	private List<ArtistInfo> mData = null;
	private Context mContext = null;

	/** 默认初始化构造一个长度为0的数据列表 */
	public ArtistAdapter(Context context) {
		mContext = context;
		mData = new ArrayList<ArtistInfo>();
	}

	public void setData(List<ArtistInfo> data) {
		mData.clear();
		if (data != null) {
			mData.addAll(data);
		}
		notifyDataSetChanged();
	}

	public List<ArtistInfo> getData() {
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
	public ArtistInfo getItem(int position) {
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
					R.layout.list_item_artist, null);
			holder.artist_name = (TextView) convertView
					.findViewById(R.id.artist_name);
			holder.num_of_tracks = (TextView) convertView
					.findViewById(R.id.num_of_tracks);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		if (getItem(position).getArtistName().equals("<unknown>")) {
			holder.artist_name.setText(mContext.getResources().getString(
					R.string.unknown_artist));
		} else {
			holder.artist_name.setText(mData.get(position).getArtistName());
		}
		holder.num_of_tracks.setText(""
				+ mData.get(position).getNumberOfTracks());

		return convertView;
	}

	static class ViewHolder {
		TextView artist_name;
		TextView num_of_tracks;
	}
}
