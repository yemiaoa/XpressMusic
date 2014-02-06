package com.lq.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lq.xpressmusic.R;
import com.lq.util.Constant;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class FrameLocalMusicFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.frame_for_nested_fragment,
				container, false);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle args = new Bundle();
		args.putInt(Constant.PARENT, Constant.START_FROM_LOCAL_MUSIC);

		getChildFragmentManager()
				.beginTransaction()
				.replace(
						R.id.frame_for_nested_fragment,
						Fragment.instantiate(getActivity(),
								TrackBrowserFragment.class.getName(), args))
				.commit();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {

	}
}
