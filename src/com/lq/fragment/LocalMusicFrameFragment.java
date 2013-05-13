package com.lq.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lq.activity.R;

public class LocalMusicFrameFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater
				.inflate(R.layout.frame_empty, container, false);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getChildFragmentManager().beginTransaction()
				.replace(R.id.frame_empty, new LocalMusicFragment()).commit();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

	}

	public void switchContent(int id) {
		switch (id) {
		case R.id.classify_by_artist:
			getChildFragmentManager().beginTransaction()
					.replace(R.id.frame_empty, new ArtistBrowserFragment())
					.addToBackStack(null).commit();
			break;
		case R.id.classify_by_folder:
			getChildFragmentManager().beginTransaction()
					.replace(R.id.frame_empty, new FolderBrowserFragment())
					.addToBackStack(null).commit();
			break;

		default:
			break;
		}

	}
}
