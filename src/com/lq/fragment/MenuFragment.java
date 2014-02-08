package com.lq.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.lq.activity.FeedbackActivity;
import com.lq.activity.MainContentActivity;
import com.lq.activity.MyPreferenceActivity;
import com.lq.xpressmusic.R;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class MenuFragment extends ListFragment {
	private ListView mListView = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListView = (ListView) getListView();
		mListView
				.setBackgroundColor(getResources().getColor(R.color.grey_dark));
		setListAdapter(new SectionAdapter());
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		String newContent = null;
		switch (position) {
		// 对应res/array.xml中选项的顺序
		case 0:// TODO 本地音乐
			newContent = FrameLocalMusicFragment.class.getName();
			break;
		case 1:// TODO 歌手
			newContent = FrameArtistFragment.class.getName();
			break;
		case 2:// TODO 专辑
			newContent = FrameAlbumFragment.class.getName();
			break;
		case 3:// TODO 文件夹
			newContent = FrameFolderFragment.class.getName();
			break;
		case 4:// TODO 收藏列表
			newContent = FramePlaylistFragment.class.getName();
			break;
		case 5:// TODO 系统设置
			startActivity(new Intent(getActivity(), MyPreferenceActivity.class));
			return;
		case 6:// TODO 意见反馈
			startActivity(new Intent(getActivity(), FeedbackActivity.class));
			break;
		case 7:// TODO 支持作者
			supportMe();
			break;
		case 8:// TODO 退出
			((MainContentActivity) getActivity()).exit();
			break;
		}
		if (newContent != null) {
			switchFragment(newContent);
		}
	}

	private void supportMe() {
		new AlertDialog.Builder(getActivity())
				.setMessage(R.string.support_developer)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Uri uri = Uri
										.parse(getString(R.string.my_alipay));
								Intent intent = new Intent(Intent.ACTION_VIEW,
										uri);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						}).create().show();
	}

	// the meat of switching the above fragment
	private void switchFragment(String fragment) {
		if (getActivity() == null)
			return;
		else if (getActivity() instanceof MainContentActivity) {
			MainContentActivity a = (MainContentActivity) getActivity();
			a.switchContent(fragment);
		}
	}

	private List<Pair<String, List<String>>> getData() {
		List<Pair<String, List<String>>> mDataList = new ArrayList<Pair<String, List<String>>>();
		Resources res = getResources();
		String[] section_titles = { res.getString(R.string.my_music),
				res.getString(R.string.other_functions) };
		String[][] menu_titles = { res.getStringArray(R.array.menu_mymusic),
				res.getStringArray(R.array.menu_othersettings) };
		for (int i = 0; i < section_titles.length; i++) {
			mDataList.add(new Pair<String, List<String>>(section_titles[i],
					Arrays.asList(menu_titles[i])));
		}
		return mDataList;
	}

	private class SectionAdapter extends BaseAdapter implements SectionIndexer {
		List<Pair<String, List<String>>> dataList = getData();

		@Override
		public int getCount() {
			int res = 0;
			for (int i = 0; i < dataList.size(); i++) {
				res += dataList.get(i).second.size();
			}
			return res;
		}

		@Override
		public String getItem(int position) {
			int c = 0;
			for (int i = 0; i < dataList.size(); i++) {
				if (position >= c
						&& position < c + dataList.get(i).second.size()) {
					return dataList.get(i).second.get(position - c);
				}
				c += dataList.get(i).second.size();
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = LayoutInflater.from(getActivity()).inflate(
						R.layout.list_item_menu, null);
				holder = new ViewHolder();
				holder.menu_title = (TextView) convertView
						.findViewById(R.id.menu_title);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.menu_title.setText(getItem(position));

			final int section = getSectionForPosition(position);
			boolean displaySectionHeaders = (getPositionForSection(section) == position);

			bindSectionHeader(convertView, position, displaySectionHeaders);
			return convertView;
		}

		protected void bindSectionHeader(View view, int position,
				boolean displaySectionHeader) {
			View section_item = view.findViewById(R.id.list_item_section);
			if (displaySectionHeader) {
				section_item.setVisibility(View.VISIBLE);
				TextView lSectionTitle = (TextView) view
						.findViewById(R.id.list_item_section_text);
				lSectionTitle
						.setText(getSections()[getSectionForPosition(position)]);
			} else {
				section_item.setVisibility(View.GONE);
			}
		}

		@Override
		public int getPositionForSection(int section) {
			if (section < 0)
				section = 0;
			if (section >= dataList.size())
				section = dataList.size() - 1;
			int c = 0;
			for (int i = 0; i < dataList.size(); i++) {
				if (section == i) {
					return c;
				}
				c += dataList.get(i).second.size();
			}
			return 0;
		}

		@Override
		public int getSectionForPosition(int position) {
			int c = 0;
			for (int i = 0; i < dataList.size(); i++) {
				if (position >= c
						&& position < c + dataList.get(i).second.size()) {
					return i;
				}
				c += dataList.get(i).second.size();
			}
			return -1;
		}

		@Override
		public String[] getSections() {
			String[] res = new String[dataList.size()];
			for (int i = 0; i < dataList.size(); i++) {
				res[i] = dataList.get(i).first;
			}
			return res;
		}

	}

	private static class ViewHolder {
		public TextView menu_title;
	}
}
