package com.lq.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.lq.xpressmusic.R;
import com.lq.entity.TrackInfo;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class TrackMutipleChooseAdapter extends BaseAdapter {

	/** 数据源 */
	private ArrayList<TrackInfo> mData = null;

	/** 上下文对象 */
	private Context mContext = null;

	/** 存储每个条目勾选的状态 */
	private SparseBooleanArray mCheckedStates = null;

	public TrackMutipleChooseAdapter(Context c, ArrayList<TrackInfo> list) {
		mData = list;
		mContext = c;
		mCheckedStates = new SparseBooleanArray();
		if (mData != null) {
			// 初始状态时所有条目都不勾选
			for (int i = 0; i < mData.size(); i++) {
				mCheckedStates.put(i, false);
			}
		}
	}

	/**
	 * 全选或全不选
	 * 
	 * @param selectAll
	 *            true表示全选,false表示全不选
	 */
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

	public long[] getSelectedAudioIds() {
		int[] checkedPostions = getSelectedItemPositions();
		long[] selectedAudioIds = new long[checkedPostions.length];
		for (int i = 0; i < checkedPostions.length; i++) {
			selectedAudioIds[i] = getItem(checkedPostions[i]).getId();
		}
		return selectedAudioIds;
	}

	public String[] getSelectedAudioPaths() {
		int[] checkedPostions = getSelectedItemPositions();
		String[] selectedAudioPaths = new String[checkedPostions.length];
		for (int i = 0; i < checkedPostions.length; i++) {
			selectedAudioPaths[i] = getItem(checkedPostions[i]).getData();
		}
		return selectedAudioPaths;
	}

	public List<TrackInfo> getSelectedItems() {
		int[] checkedPostions = getSelectedItemPositions();
		List<TrackInfo> list = new ArrayList<TrackInfo>();
		for (int i = 0; i < checkedPostions.length; i++) {
			list.add(getItem(checkedPostions[i]));
		}
		return list;
	}

	/**
	 * 获得已选择的条目们在列表中的位置
	 * 
	 * @return 所有已选择的条目在列表中的位置
	 */
	public int[] getSelectedItemPositions() {
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

	/**
	 * 改变指定位置条目的选择状态，如果已经处于勾选状态则取消勾选，如果处于没有勾选则勾选
	 * 
	 * @param position
	 *            要改变的条目选择状态的位置
	 */
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
