package com.lq.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.lq.fragment.LocalMusicFragment;
import com.lq.fragment.MenuFragment;
import com.lq.service.MusicService;
import com.slidingmenu.lib.SlidingMenu;

public class MainContentActivity extends SherlockFragmentActivity {
	private static final String TAG = MainContentActivity.class.getSimpleName();

	public static final int MESSAGE_SWITCH_TO_PLAY_IMAGE = 0;
	public static final int MESSAGE_SWITCH_TO_PAUSE_IMAGE = 1;

	private List<String> mFragmentsName = new ArrayList<String>();

	/** 侧滑菜单控件 */
	private SlidingMenu mSlidingMenu = null;

	/** 手势检测 */
	private GestureDetector mDetector = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_content);

		// 初始化SlidingMenu，并为其填充Fragment
		initSlidingMenu();
		initPopulateFragment();

		// 设置ActionBar
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// 设置滑动手势
		mDetector = new GestureDetector(new RightGestureListener());
	}

	/** 设置SlidingMenu */
	private void initSlidingMenu() {
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

	/** 为SlidingMenu和Content填充Fragment */
	private void initPopulateFragment() {

		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.replace(R.id.frame_menu, new MenuFragment(),
				MenuFragment.class.getName());
		fragmentTransaction.replace(R.id.frame_content,
				new LocalMusicFragment(), LocalMusicFragment.class.getName());
		fragmentTransaction.commit();
		mFragmentsName.add(LocalMusicFragment.class.getName());

	}

	public SlidingMenu getSlidingMenu() {
		return mSlidingMenu;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main_content, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			mSlidingMenu.toggle();
			return true;
		case R.id.go_to_play:
			switchToMusicPlayer();
			return true;
		}
		return false;
	}

	/**
	 * 切换主页内容。<br>
	 * 如果要切换的Fragment已经存在，则隐藏其他已经存在的Fragment，显示已开启过的Fragment。<br>
	 * 按返回键可以在Fragment之间回退。
	 */
	public void switchContent(String fragmentName) {
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentByTag(fragmentName);
		if (null != fragment) {
			FragmentTransaction ft = fm.beginTransaction();
			for (int i = 0; i < mFragmentsName.size(); i++) {
				if (!mFragmentsName.get(i)
						.equals(fragment.getClass().getName())) {
					ft.hide(fm.findFragmentByTag(mFragmentsName.get(i)));
				}
			}
			ft.show(fragment);
			ft.commit();
		} else {
			fragment = Fragment.instantiate(getApplicationContext(),
					fragmentName);
			FragmentTransaction ft = fm.beginTransaction();
			for (int i = 0; i < mFragmentsName.size(); i++) {
				ft.hide(fm.findFragmentByTag(mFragmentsName.get(i)));
			}
			ft.add(R.id.frame_content, fragment, fragmentName).addToBackStack(
					fragmentName);
			ft.show(fragment);
			ft.commit();
			mFragmentsName.add(fragmentName);
		}
		getSlidingMenu().showContent();
	}

	public void exit() {
		stopService(new Intent(MainContentActivity.this, MusicService.class));
		MainContentActivity.this.finish();
	}

	@Override
	public void onBackPressed() {
		FragmentManager fm = getSupportFragmentManager();
		// 规定在显示菜单时才可退出程序，按返回键弹出侧滑菜单
		if (mSlidingMenu.isMenuShowing()) {
			// 显示菜单时，按返回键退出程序
			this.finish();
		} else if (fm.getBackStackEntryCount() > 0) {
			// 如果已经打开多个Fragment，允许返回键将Fragment回退
			String name = fm.getBackStackEntryAt(
					fm.getBackStackEntryCount() - 1).getName();
			mFragmentsName.remove(name);
			if (!fm.popBackStackImmediate()) {
				finish();
			}
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
			mSlidingMenu.toggle();
			break;

		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void switchToMusicPlayer() {
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
