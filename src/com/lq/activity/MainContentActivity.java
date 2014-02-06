package com.lq.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.analytics.tracking.android.EasyTracker;
import com.lq.fragment.FrameLocalMusicFragment;
import com.lq.fragment.MenuFragment;
import com.lq.fragment.PromptDialogFragment;
import com.lq.service.MusicService;
import com.lq.xpressmusic.R;
import com.slidingmenu.lib.SlidingMenu;
import com.umeng.analytics.MobclickAgent;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class MainContentActivity extends FragmentActivity implements
		OnBackStackChangedListener {
	public interface OnBackKeyPressedListener {
		public abstract void onBackKeyPressed();
	}

	private static final String TAG = MainContentActivity.class.getSimpleName();

	public static final int MESSAGE_SWITCH_TO_PLAY_IMAGE = 0;
	public static final int MESSAGE_SWITCH_TO_PAUSE_IMAGE = 1;

	/** 手势检测 */
	private GestureDetector mDetector = null;

	/** 侧滑菜单控件 */
	private SlidingMenu mSlidingMenu = null;

	private List<Fragment> mFragmentList = new ArrayList<Fragment>();
	private Fragment mCurrentFragment = null;

	private int mBackStackEntryCount = 0;

	private List<OnBackKeyPressedListener> mBackKeyPressedListeners = new ArrayList<OnBackKeyPressedListener>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_content);

		getSupportFragmentManager().addOnBackStackChangedListener(this);
		mBackStackEntryCount = getSupportFragmentManager()
				.getBackStackEntryCount();

		// 初始化SlidingMenu，并为其填充Fragment
		initSlidingMenu();
		initPopulateFragment();
		// 设置滑动手势
		mDetector = new GestureDetector(new SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				// 从左向右滑动
				if (e1 != null && e2 != null) {
					if (e1.getX() - e2.getX() > 120) {
						switchToPlayer();
						return true;
					}
				}
				return false;
			}
		});
	}

	/** 设置SlidingMenu */
	private void initSlidingMenu() {
		mSlidingMenu = new SlidingMenu(this);
		// 1.为SlidingMenu宿主一个Activity
		mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
		// 2.为SlidingMenu指定布局
		mSlidingMenu.setMenu(R.layout.layout_menu);
		// 3.设置SlidingMenu从何处可以滑出
		mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
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
		MenuFragment menuFragment = new MenuFragment();
		mCurrentFragment = new FrameLocalMusicFragment();

		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.replace(R.id.frame_menu, menuFragment, menuFragment
				.getClass().getName());
		fragmentTransaction.replace(R.id.frame_main, mCurrentFragment,
				mCurrentFragment.getClass().getName());
		fragmentTransaction.commit();

		mFragmentList.add(mCurrentFragment);

	}

	public SlidingMenu getSlidingMenu() {
		return mSlidingMenu;
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		mFragmentList.clear();
		mFragmentList = null;
		mBackKeyPressedListeners.clear();
		mBackKeyPressedListeners = null;
	}

	public void registerBackKeyPressedListener(OnBackKeyPressedListener listener) {
		if (!mBackKeyPressedListeners.contains(listener)) {
			mBackKeyPressedListeners.add(listener);
		}
	}

	public void unregisterBackKeyPressedListener(
			OnBackKeyPressedListener listener) {
		mBackKeyPressedListeners.remove(listener);
	}

	public void switchToPlayer() {
		startActivity(new Intent(MainContentActivity.this, PlayerActivity.class));
		overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
	}

	/**
	 * 切换主页内容
	 */
	public void switchContent(String fragmentName) {
		Fragment f = null;
		boolean existed = false;

		if (!mCurrentFragment.getClass().getName().equals(fragmentName)) {

			// 遍历当前已打开的Fragment集合
			for (int i = 0; i < mFragmentList.size(); i++) {
				// 如果要求切换至的fragment已经存在，则直接把已经存在的显示出来
				if (mFragmentList.get(i).getClass().getName()
						.equals(fragmentName)) {
					existed = true;
					f = mFragmentList.get(i);
					getSupportFragmentManager().beginTransaction()
							.hide(mCurrentFragment).show(f).commit();
					mCurrentFragment = f;
					break;
				}
			}

			// 如果不存在，则新建
			if (!existed) {
				f = Fragment.instantiate(getApplicationContext(), fragmentName);
				mFragmentList.add(f);
				getSupportFragmentManager().beginTransaction()
						.hide(mCurrentFragment).add(R.id.frame_main, f)
						.commit();
				mCurrentFragment = f;
			}
		}

		mSlidingMenu.showContent();
	}

	public void setSlideEnable(boolean slidable) {
		if (slidable) {
			mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		} else {
			mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
		}
	}

	public void exit() {
		stopService(new Intent(MainContentActivity.this, MusicService.class));
		MainContentActivity.this.finish();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return this.mDetector.onTouchEvent(event);
	}

	@Override
	public void onBackPressed() {
		if (mBackKeyPressedListeners.size() != 0) {
			for (OnBackKeyPressedListener listener : mBackKeyPressedListeners) {
				listener.onBackKeyPressed();
			}
		}

		if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
			getSupportFragmentManager().popBackStackImmediate();
		} else if (mCurrentFragment != null
				&& mCurrentFragment.getChildFragmentManager()
						.getBackStackEntryCount() > 0) {
			if (mCurrentFragment.getView() != null) {
				mCurrentFragment.getChildFragmentManager()
						.popBackStackImmediate();
			}
		} else {
			// 规定在显示菜单时才可退出程序，按返回键弹出侧滑菜单
			if (mSlidingMenu.isMenuShowing()) {
				// 显示菜单时，按返回键退出程序
				PromptDialogFragment f = PromptDialogFragment
						.newInstance(
								getResources().getString(
										R.string.are_you_sure_to_exit),
								new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										MainContentActivity.this.exit();
									}
								});
				f.show(getSupportFragmentManager(), null);
			} else {
				// 菜单没有显示，就弹出菜单
				mSlidingMenu.showMenu();
			}
		}
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

	@Override
	public void onBackStackChanged() {

		// 如果后退栈条目数目增加了
		if (mBackStackEntryCount < getSupportFragmentManager()
				.getBackStackEntryCount()) {
			mBackStackEntryCount++;

		} else {// 如果后退栈条目数目减少了
			mBackStackEntryCount--;
		}
	}

}
