package com.lq.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.lq.activity.R;
import com.lq.entity.TrackInfo;

public class TrackMutipleChooseAdapter extends BaseAdapter {
	private ArrayList<TrackInfo> mData = null;
	private SparseBooleanArray mCheckedStates = null;
	private Context mContext = null;

	public TrackMutipleChooseAdapter(Context c, ArrayList<TrackInfo> list) {
		mData = list;
		mContext = c;
		mCheckedStates = new SparseBooleanArray();
		if (mData != null) {
			for (int i = 0; i < mData.size(); i++) {
				mCheckedStates.put(i, false);
			}
		}
	}

	public void selectAllItem(boolean selectAll) {
		if (selectAll) {
			for (int i = 0; i < mData.size(); i++) {
				mCheckedStates.put(i, true);
			}
		} else {
			for (int i = 0; i < mData.size(); i++) {
				mCheckedStates.put(i, false);
			}
		}
		notifyDataSetChanged();
	}

	public int[] getCheckedItemPositions() {
		int count = 0;
		for (int i = 0; i < mCheckedStates.size(); i++) {
			if (mCheckedStates.get(i)) {
				count++;
			}
		}
		int[] checkedPostions = new int[count];
		for (int i = 0, j = 0; i < mCheckedStates.size(); i++) {
			if (mCheckedStates.get(i)) {
				checkedPostions[j] = i;
				j++;
			}
		}
		return checkedPostions;
	}

	public void toggleCheckedState(int position) {
		if (position >= 0 && position < mCheckedStates.size()) {
			if (mCheckedStates.get(position)) {
				mCheckedStates.put(position, false);
			} else {
				mCheckedStates.put(position, true);
			}
			notifyDataSetChanged();
		}
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public TrackInfo getItem(int position) {
		return mData.get(position);
	}

	@Override
	public boolean isEmpty() {
		if (mData == null) {
			return true;
		} else if (mData.size() == 0) {
			return true;
		} else {
			return false;
		}
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
					R.layout.list_item_track_mutiple, parent, false);
			holder.track_name = (TextView) convertView
					.findViewById(R.id.textview_music_title);
			holder.artist = (TextView) convertView
					.findViewById(R.id.textview_music_singer);
			holder.checkbox = (CheckBox) convertView
					.findViewById(R.id.cb_track_mutiple);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.track_name.setText(getItem(position).getTitle());
		holder.artist.setText(getItem(position).getArtist());
		if (mCheckedStates.get(position)) {
			holder.checkbox.setChecked(true);
		} else {
			holder.checkbox.setChecked(false);
		}
		return convertView;
	}

	static class ViewHolder {
		TextView track_name;
		TextView artist;
		CheckBox checkbox;
	}
}
