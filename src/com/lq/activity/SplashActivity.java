package com.lq.activity;

import java.lang.ref.WeakReference;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;

import com.google.analytics.tracking.android.EasyTracker;
import com.lq.xpressmusic.R;
import com.umeng.analytics.MobclickAgent;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class SplashActivity extends FragmentActivity {

	private MyHandler mHandler = new MyHandler(this);
	private final int mDelayMillis = 2000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);
		mHandler.sendEmptyMessageDelayed(0, mDelayMillis);

		initUmengSDK();
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
	public void onBackPressed() {
		// 什么也不做，在欢迎界面禁止用户回退
	}

	private void initUmengSDK() {
		MobclickAgent.openActivityDurationTrack(true);
		MobclickAgent.updateOnlineConfig(this);
		MobclickAgent.onError(this);
	}

	private static class MyHandler extends Handler {
		// 使用弱引用，避免Handler造成的内存泄露(Message持有Handler的引用，内部定义的Handler类持有外部类的引用)
		WeakReference<SplashActivity> mFragmentWeakReference = null;
		SplashActivity mActivity = null;

		public MyHandler(SplashActivity a) {
			mFragmentWeakReference = new WeakReference<SplashActivity>(a);
			mActivity = mFragmentWeakReference.get();
		}

		@Override
		public void handleMessage(Message msg) {
			mActivity.startActivity(new Intent(mActivity,
					MainContentActivity.class));
			mActivity.finish();
		}
	}
}
