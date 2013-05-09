package com.lq.adapter;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

public class MainPagerAdapter extends FragmentStatePagerAdapter {

	List<Fragment> mFragmentList = null;
	FragmentManager mFragmentManager = null;

	public MainPagerAdapter(FragmentManager fm, List<Fragment> fragmentList) {
		super(fm);
		mFragmentManager = fm;
		mFragmentList = fragmentList;
	}

	@Override
	public Fragment getItem(int position) {
		return mFragmentList.get(position);
	}

	@Override
	public int getCount() {
		return mFragmentList.size();
	}

	@Override
	public int getItemPosition(Object object) {
		for (int i = 0; i < getCount(); i++) {
			if (mFragmentList.get(i) == object) {
				return PagerAdapter.POSITION_UNCHANGED;
			}
		}
		return PagerAdapter.POSITION_NONE;
	}
}
