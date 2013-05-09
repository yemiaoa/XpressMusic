package com.lq.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.lq.adapter.MainPagerAdapter;
import com.lq.fragment.LocalMusicFragment;
import com.lq.fragment.MenuFragment;
import com.lq.fragment.MusicPlayFragment;
import com.lq.service.MusicService;
import com.slidingmenu.lib.SlidingMenu;

public class MainContentActivity extends SherlockFragmentActivity {
	private static final String TAG = MainContentActivity.class.getSimpleName();

	public static final int MESSAGE_SWITCH_TO_PLAY_IMAGE = 0;
	public static final int MESSAGE_SWITCH_TO_PAUSE_IMAGE = 1;

	/** 侧滑菜单控件 */
	private SlidingMenu mSlidingMenu = null;

	/** 总共就两页，第一页为主页面，可以替换掉，第二页始终为音乐播放界面 */
	private ViewPager mViewPager = null;
	private boolean mViewPagerSlidable = true;
	MainPagerAdapter mMainPagerAdapter = null;

	private List<Fragment> mFragmentShowList = null;
	private List<Fragment> mFragmentList = null;

	private MusicPlayFragment mMusicPlayFragment = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_content);

		// 初始化SlidingMenu，并为其填充Fragment
		initSlidingMenu();
		initPopulateFragment();
		initViewPager();

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
		LocalMusicFragment localMusicFragment = new LocalMusicFragment();
		mMusicPlayFragment = new MusicPlayFragment();

		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.replace(R.id.frame_menu, menuFragment,
				MenuFragment.class.getName());
		fragmentTransaction.commit();

		mFragmentList = new ArrayList<Fragment>();
		mFragmentShowList = new ArrayList<Fragment>(2);
		mFragmentList.add(localMusicFragment);
		mFragmentList.add(mMusicPlayFragment);
		mFragmentShowList.add(localMusicFragment);
		mFragmentShowList.add(mMusicPlayFragment);
	}

	private void initViewPager() {
		mViewPager = (ViewPager) findViewById(R.id.viewpager_main);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int position) {
				switch (position) {
				case 0:
					getSlidingMenu().setTouchModeAbove(
							SlidingMenu.TOUCHMODE_FULLSCREEN);
					break;
				default:
					getSlidingMenu().setTouchModeAbove(
							SlidingMenu.TOUCHMODE_NONE);
					break;
				}
			}
		});

		// 拦截viewpager的触摸事件
		mViewPager.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 根据自定义的变量决定viewpager是否响应Touch事件，来达到控制禁止滑动的目的
				if (mViewPagerSlidable) {
					// 允许滑动，返回false，会将触摸事件分发到viewpager包含的子控件中
					return false;
				} else {
					// 不允许滑动，返回true，viewpager的子控件将不会接受到触摸事件
					return true;
				}
			}
		});
		mMainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(),
				mFragmentShowList);

		mViewPager.setAdapter(mMainPagerAdapter);
		mViewPager.setCurrentItem(0);
	}

	public SlidingMenu getSlidingMenu() {
		return mSlidingMenu;
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
		mFragmentList.clear();
		mFragmentShowList.clear();
		mFragmentList = null;
		mFragmentShowList = null;
	}

	/**
	 * 切换主页内容
	 */
	public void switchContent(String fragmentName) {
		Fragment tFragment = null;
		boolean existed = false;

		// 遍历当前已打开的Fragment集合
		for (int i = 0; i < mFragmentList.size(); i++) {
			// 如果要求切换至的fragment已经存在，则直接把已经存在的显示出来
			if (mFragmentList.get(i).getClass().getName().equals(fragmentName)) {
				existed = true;
				mFragmentShowList.set(0, mFragmentList.get(i));
				mMainPagerAdapter.notifyDataSetChanged();
				break;
			}
		}
		// 如果不存在，则新建
		if (!existed) {
			tFragment = Fragment.instantiate(getApplicationContext(),
					fragmentName);
			mFragmentList.add(tFragment);
			mFragmentShowList.set(0, tFragment);
			mMainPagerAdapter.notifyDataSetChanged();
		}

		mSlidingMenu.showContent();
	}

	public void switchToMain() {
		mViewPager.setCurrentItem(0, true);
	}

	public void switchToMusicPlayer() {
		mViewPager.setCurrentItem(1, true);
	}

	public void switchToSlidingMenu() {
		switchToMain();
		mSlidingMenu.toggle();
	}

	public void forbidSlide() {
		mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
		mViewPagerSlidable = false;
	}

	public void allowSlide() {
		mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		mViewPagerSlidable = true;
	}

	public void exit() {
		stopService(new Intent(MainContentActivity.this, MusicService.class));
		MainContentActivity.this.finish();
	}

	@Override
	public void onBackPressed() {
		// 什么也不做
		// 默认执行父类方法，会finish本Activity
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mViewPager.getCurrentItem() == 0) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				if (mViewPagerSlidable) {
					mSlidingMenu.toggle();
				}
				break;
			case KeyEvent.KEYCODE_BACK:
				// 规定在显示菜单时才可退出程序，按返回键弹出侧滑菜单
				if (mSlidingMenu.isMenuShowing()) {
					// 显示菜单时，按返回键退出程序
					this.finish();
				} else {
					// 菜单没有显示，就弹出菜单
					mSlidingMenu.showMenu();
				}
				break;
			default:
				break;
			}
		} else {
			// 分享onKeyDown事件给播放界面
			mMusicPlayFragment.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}

}
