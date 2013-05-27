package com.lq.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lq.activity.R;
import com.lq.entity.AlbumInfo;
/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class AlbumAdapter extends BaseAdapter {
	private static final String TAG = AlbumAdapter.class.getSimpleName();

	private List<AlbumInfo> mData = null;
	private Context mContext = null;

	/** 默认初始化构造一个长度为0的数据列表 */
	public AlbumAdapter(Context context) {
		mContext = context;
		mData = new ArrayList<AlbumInfo>();
	}

	public void setData(List<AlbumInfo> data) {
		mData.clear();
		if (data != null) {
			mData.addAll(data);
		}
		notifyDataSetChanged();
	}

	public List<AlbumInfo> getData() {
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
	public AlbumInfo getItem(int position) {
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
					R.layout.list_item_album, null, false);
			holder.album_name = (TextView) convertView
					.findViewById(R.id.album_name);
			holder.num_of_tracks = (TextView) convertView
					.findViewById(R.id.num_of_tracks_in_album);
			holder.album_picture = (ImageView) convertView
					.findViewById(R.id.album_picture);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.album_name.setText(getItem(position).getAlbumName());
		holder.num_of_tracks.setText("" + getItem(position).getNumberOfSongs());

		// 加载专辑封面
		if (getItem(position).getArtWork() == null) {
			holder.album_picture
					.setImageResource(R.drawable.default_list_album);
		} else {
			Bitmap bm = BitmapFactory
					.decodeFile(getItem(position).getArtWork());
			BitmapDrawable bmpDraw = new BitmapDrawable(bm);
			holder.album_picture.setImageDrawable(bmpDraw);
			Log.i(TAG, "getView()--->ArtWork=" + getItem(position).getArtWork());
		}

		return convertView;
	}

	static class ViewHolder {
		TextView album_name;
		TextView num_of_tracks;
		ImageView album_picture;
	}
}
