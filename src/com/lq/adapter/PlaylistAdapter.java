package com.lq.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lq.xpressmusic.R;
import com.lq.entity.PlaylistInfo;
/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PlaylistAdapter extends BaseAdapter implements OnClickListener {
	private List<PlaylistInfo> mData = null;
	private Context mContext = null;
	private boolean mMenuVisible = true;

	public PlaylistAdapter(Context c) {
		mContext = c;
		mData = new ArrayList<PlaylistInfo>();
	}

	public void setData(List<PlaylistInfo> data) {
		mData.clear();
		if (data != null) {
			mData.addAll(data);
		}
		notifyDataSetChanged();
	}

	public List<PlaylistInfo> getData() {
		return mData;
	}

	public void setPopupMenuVisible(boolean visible) {
		mMenuVisible = visible;
		notifyDataSetChanged();
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
	public PlaylistInfo getItem(int position) {
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
					R.layout.list_item_playlist, null);
			holder.playlist_name = (TextView) convertView
					.findViewById(R.id.playlist_name);
			holder.playlist_members_count = (TextView) convertView
					.findViewById(R.id.song_count_of_playlist);
			holder.popup_menu = convertView
					.findViewById(R.id.playlist_popup_menu);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.playlist_name.setText(mData.get(position).getPlaylistName());
		holder.playlist_members_count.setText(""
				+ mData.get(position).getNumOfMembers());
		if (mMenuVisible) {
			holder.popup_menu.setVisibility(View.VISIBLE);
			holder.popup_menu.setOnClickListener(PlaylistAdapter.this);
		} else {
			holder.popup_menu.setVisibility(View.INVISIBLE);
		}
		return convertView;
	}

	static class ViewHolder {
		TextView playlist_name;
		TextView playlist_members_count;
		View popup_menu;
	}

	@Override
	public void onClick(View v) {
		v.showContextMenu();
	}

}
