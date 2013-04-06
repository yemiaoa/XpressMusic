package com.lq.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.lq.fragment.LocalMusicFragment;
import com.lq.fragment.MenuFragment;
import com.slidingmenu.lib.SlidingMenu;

public class MainContentActivity extends SherlockFragmentActivity {
	private SlidingMenu mSlidingMenu = null;
	private GestureDetector mDetector = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_content);

		// 初始化SlidingMenu，并为其填充Fragment
		initSlidingMenu();
		initPopulateFragment();

		// 设置ActionBar
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// 设置主页底部的播放控制，处理响应事件
		View info_frame = findViewById(R.id.bottom_info_frame);
		info_frame.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				switchToMusicPlayer();
			}
		});

		// 设置滑动手势
		mDetector = new GestureDetector(new RightGestureListener());
	}

	private void initSlidingMenu() {
		// TODO 设置SlidingMenu
		mSlidingMenu = new SlidingMenu(this);
		// 1.为SlidingMenu宿主一个Activity
		mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
		// 2.为SlidingMenu指定布局
		mSlidingMenu.setMenu(R.layout.layout_menu);
		// 3.设置SlidingMenu从何处可以滑出
		mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		// 4.设置SlidingMenu的滑出方向
		mSlidingMenu.setMode(SlidingMenu.LEFT);
		// 5.设置SlidingMenu的其他参数
		mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
		mSlidingMenu.setShadowDrawable(R.drawable.shadow);
		mSlidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		mSlidingMenu.setFadeDegree(0.35f);
		mSlidingMenu.setBehindScrollScale(0.0f);// 滑动时侧滑菜单的内容静止不动

	}

	private void initPopulateFragment() {
		// TODO 为SlidingMenu和Content填充Fragment
		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.replace(R.id.frame_menu, new MenuFragment());
		fragmentTransaction.replace(R.id.frame_content,
				new LocalMusicFragment());
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
				.replace(R.id.frame_content, fragment).addToBackStack(null)
				.commit();
		getSlidingMenu().showContent();
	}

	public void exit() {
		// TODO 结束所有Activity和Service
		// ActivityManager am = (ActivityManager)
		// getSystemService(Context.ACTIVITY_SERVICE);
		// am.killBackgroundProcesses(getPackageName());
		MainContentActivity.this.finish();
	}

	@Override
	public void onBackPressed() {
		// 规定在显示菜单时才可退出程序，按返回键弹出侧滑菜单
		if (mSlidingMenu.isMenuShowing()) {
			// 显示菜单时，按返回键退出程序
			this.finish();
		} else if (this.getSupportFragmentManager().getBackStackEntryCount() > 0) {
			// 如果已经打开多个Fragment，允许返回键将Fragment回退
			super.onBackPressed();
		} else {
			// Fragment已经回退完，此时菜单没有显示，就弹出菜单
			mSlidingMenu.showMenu();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return MainContentActivity.this.mDetector.onTouchEvent(event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
			mSlidingMenu.showMenu();
			break;

		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void switchToMusicPlayer() {
		startActivity(new Intent(MainContentActivity.this,
				MusicPlayerActivity.class));
		overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
	}

	protected class RightGestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// 从左向右滑动
			if (e1 != null && e2 != null) {
				if (e1.getX() - e2.getX() > 120) {
					switchToMusicPlayer();
					return true;
				}
			}
			return false;
		}
	}
}
