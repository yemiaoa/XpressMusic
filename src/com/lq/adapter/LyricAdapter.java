package com.lq.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lq.activity.R;
import com.lq.entity.LyricSentence;

public class LyricAdapter extends BaseAdapter {
	/** 歌词句子集合 */
	List<LyricSentence> mLyricSentences = null;
	
	Context mContext = null;
	
	/** 当前的句子索引号 */
	int mIndexOfCurrentSentence = 0;
	
	float mCurrentSize = 20;
	float mNotCurrentSize = 17;

	public LyricAdapter(Context context) {
		mContext = context;
		mLyricSentences = new ArrayList<LyricSentence>();
		mIndexOfCurrentSentence = 0;
	}

	/** 设置歌词，由外部调用， */
	public void setLyric(List<LyricSentence> lyric) {
		mLyricSentences.clear();
		mLyricSentences.addAll(lyric);
		mIndexOfCurrentSentence = 0;
		notifyDataSetChanged();
	}

	@Override
	public boolean isEmpty() {
		// 歌词为空时，让ListView显示EmptyView
		return mLyricSentences.size() == 0;
	}

	@Override
	public boolean isEnabled(int position) {
		// 禁止在列表条目上点击
		return false;
	}

	@Override
	public int getCount() {
		return mLyricSentences.size();
	}

	@Override
	public Object getItem(int position) {
		return mLyricSentences.get(position).getContentText();
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
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.lyric_line, null);
			holder.lyric_line = (TextView) convertView
					.findViewById(R.id.lyric_line_text);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.lyric_line.setText(mLyricSentences.get(position)
				.getContentText());
		if (mIndexOfCurrentSentence == position) {
			// 当前播放到的句子设置为白色，字体大小更大
			holder.lyric_line.setTextColor(Color.WHITE);
			holder.lyric_line.setTextSize(mCurrentSize);
		} else {
			// 其他的句子设置为暗色，字体大小较小
			holder.lyric_line.setTextColor(mContext.getResources().getColor(
					R.color.white_translucence2));
			holder.lyric_line.setTextSize(mNotCurrentSize);
		}
		return convertView;
	}

	public void setCurrentSentenceIndex(int index) {
		mIndexOfCurrentSentence = index;
		notifyDataSetChanged();
	}

	static class ViewHolder {
		TextView lyric_line;
	}
}
