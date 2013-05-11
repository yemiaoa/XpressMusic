package com.lq.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;

public class TwoPagerAdapter extends FragmentStatePagerAdapter {
	private final int ITEM_COUNT = 2;
	private Fragment mFirstPageFragment = null;
	private Fragment mSecondPageFragment = null;

	public TwoPagerAdapter( FragmentManager fm,
			Fragment first, Fragment second) {
		super(fm);
		mFirstPageFragment = first;
		mSecondPageFragment = second;
	}

	public void setFirstPage(Fragment f) {
		if (mFirstPageFragment != f) {
			mFirstPageFragment = f;
			notifyDataSetChanged();
		}
	}

	@Override
	public Fragment getItem(int position) {
		switch (position) {
		case 0:
			return mFirstPageFragment;
		case 1:
			return mSecondPageFragment;
		default:
			return null;
		}
	}

	@Override
	public int getCount() {
		return ITEM_COUNT;
	}

	@Override
	public int getItemPosition(Object object) {
		if (object == mSecondPageFragment) {
			return PagerAdapter.POSITION_UNCHANGED;
		} else {
			return PagerAdapter.POSITION_NONE;
		}
	}

}
