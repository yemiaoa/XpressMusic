package com.lq.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lq.activity.R;
import com.lq.entity.TrackInfo;

public class TrackAdapter extends BaseAdapter {
	private Context mContext = null;
	/** 数据源 */
	private List<TrackInfo> mData = null;

	/** 播放时为相应播放条目显示一个播放标记 */
	private int mActivateItemPos = -1;

	public TrackAdapter(Context context) {
		mContext = context;
		mData = new ArrayList<TrackInfo>();
	}

	public void setData(List<TrackInfo> data) {
		mData.clear();
		if (data != null) {
			mData.addAll(data);
		}
		mActivateItemPos = -1;
		notifyDataSetChanged();
	}

	public List<TrackInfo> getData() {
		return mData;
	}

	/** 让指定位置的条目显示一个正在播放标记（活动状态标记） */
	public void setSpecifiedIndicator(int position) {
		mActivateItemPos = position;
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
	public TrackInfo getItem(int position) {
		return mData.get((int) getItemId(position));
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.list_item_local_music, null);
			holder = new ViewHolder();
			holder.indicator = convertView.findViewById(R.id.play_indicator);
			holder.title = (TextView) convertView
					.findViewById(R.id.textview_music_title);
			holder.artist = (TextView) convertView
					.findViewById(R.id.textview_music_singer);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		if (mActivateItemPos == position) {
			holder.indicator.setVisibility(View.VISIBLE);
		} else {
			holder.indicator.setVisibility(View.INVISIBLE);
		}
		holder.title.setText(getItem(position).getTitle());

		if (getItem(position).getArtist().equals("<unknown>")) {
			holder.artist.setText(mContext.getResources().getString(
					R.string.unknown_artist));
		} else {
			holder.artist.setText(getItem(position).getArtist());
		}

		return convertView;
	}

	public static class ViewHolder {
		TextView title;
		TextView artist;
		View indicator;
	}
}
