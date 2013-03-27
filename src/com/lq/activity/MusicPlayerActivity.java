package com.lq.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageButton;

public class MusicPlayerActivity extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_musicplay);
		ImageButton ib_back = (ImageButton) findViewById(R.id.play_button_back);
		ib_back.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO 返回主页
				backToMain();
			}
		});
	}

	@Override
	public void onBackPressed() {
		// TODO 返回主页
		super.onBackPressed();
		backToMain();
	}

	private void backToMain() {
		startActivity(new Intent(MusicPlayerActivity.this,
				MainContentActivity.class));
		MusicPlayerActivity.this.finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
	}
}
