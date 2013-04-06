package com.lq.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class MusicPlayerActivity extends FragmentActivity {
	private GestureDetector mDetector = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_musicplay);
		ImageButton ib_back = (ImageButton) findViewById(R.id.play_button_back);
		ib_back.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				backToMain();
			}
		});
		// 左滑切换至主页
		mDetector = new GestureDetector(new LeftGestureListener());
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		backToMain();
	}

	private void backToMain() {
		// TODO 返回主页面
		startActivity(new Intent(MusicPlayerActivity.this,
				MainContentActivity.class));
		MusicPlayerActivity.this.finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return this.mDetector.onTouchEvent(event);
	}

	private class LeftGestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// 从右向左滑动
			if (e1.getX() - e2.getX() < -120) {
				backToMain();
				return true;
			}
			return false;
		}

	}

}
