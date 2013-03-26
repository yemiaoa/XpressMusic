package com.lq.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.lq.fragment.ColorFragment;
import com.lq.fragment.MenuFragment;
import com.slidingmenu.lib.SlidingMenu;

public class MainContentActivity extends FragmentActivity {
	SlidingMenu mSlidingMenu = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_content);

		initSlidingMenu();
		initPopulateFragment();

		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private void initSlidingMenu() {
		// TODO 初始化SlidingMenu
		mSlidingMenu = new SlidingMenu(this);
		// 1.设置SlidingMenu的宿主Activity
		mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
		// 2.设置SlidingMenu的布局
		mSlidingMenu.setMenu(R.layout.layout_menu);
		// 3.设置SlidingMenu以何种手势弹出
		mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		// 4.设置SlidingMenu从屏幕的哪边弹出
		mSlidingMenu.setMode(SlidingMenu.LEFT);
		// 5.设置其他SlidingMenu参数
		mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
		mSlidingMenu.setShadowDrawable(R.drawable.shadow);
		mSlidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		mSlidingMenu.setFadeDegree(0.35f);
	}

	private void initPopulateFragment() {
		// TODO 为Menu和Content指定Fragment
		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.replace(R.id.frame_menu, new MenuFragment());
		fragmentTransaction.replace(R.id.frame_content, new ColorFragment(
				R.color.holo_blue_dark));
		fragmentTransaction.commit();

	}

	public SlidingMenu getSlidingMenu() {
		return mSlidingMenu;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			mSlidingMenu.toggle();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void switchContent(Fragment fragment) {
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.frame_content, fragment).commit();
		getSlidingMenu().showContent();
	}

}
