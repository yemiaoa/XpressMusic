package com.lq.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.lq.activity.MainContentActivity;
import com.lq.activity.R;

public class MenuFragment extends ListFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.list, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		String[] menu_settings = getResources().getStringArray(
				R.array.menu_settings);
		ArrayAdapter<String> meunuAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_list_item_1,
				android.R.id.text1, menu_settings);
		setListAdapter(meunuAdapter);
	}

	@Override
	public void onListItemClick(ListView lv, View v, int position, long id) {
		Fragment newContent = null;
		switch (position) {
		case 0:// TODO 本地音乐
			newContent=new ColorFragment(R.color.holo_blue_dark);
			break;
		case 1:// TODO 喜爱
			newContent=new ColorFragment(R.color.holo_green_dark);
			break;
		case 2:// TODO 播放列表
			newContent=new ColorFragment(R.color.holo_orange_dark);
			break;
		case 3:// TODO 最近播放
			newContent=new ColorFragment(R.color.holo_purple);
			break;
		case 4:// TODO 系统设置
			newContent=new ColorFragment(R.color.holo_red_dark);
			break;
		case 5:// TODO 退出
			getActivity().finish();
			break;
		}
		if (newContent != null)
			switchFragment(newContent);
	}

	// the meat of switching the above fragment
	private void switchFragment(Fragment fragment) {
		if (getActivity() == null)
			return;
		else if (getActivity() instanceof MainContentActivity) {
			MainContentActivity a = (MainContentActivity) getActivity();
			a.switchContent(fragment);
		}
	}
}
