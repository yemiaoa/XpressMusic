package com.lq.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.lq.util.Constant;
import com.lq.xpressmusic.R;
import com.umeng.analytics.MobclickAgent;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class FeedbackActivity extends FragmentActivity implements
		OnClickListener {

	private EditText mView_FeedbackContent = null;
	private Button mView_Submit = null;
	private View mView_Close = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feedback);
		findViews();
		initViewsSetting();
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

	private void findViews() {
		mView_FeedbackContent = (EditText) findViewById(R.id.feedback_content);
		mView_Submit = (Button) findViewById(R.id.submit_feedback);
		mView_Close = findViewById(R.id.close_feedback);
	}

	private void initViewsSetting() {
		mView_Close.setOnClickListener(this);
		mView_Submit.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.close_feedback:
			FeedbackActivity.this.finish();
			break;
		case R.id.submit_feedback:
			if (mView_FeedbackContent.getText().toString().equals("")) {
				Toast.makeText(getApplicationContext(),
						R.string.please_input_feedback, Toast.LENGTH_SHORT)
						.show();
			} else {
				// 将反馈内容发送邮件给作者

				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("message/rfc822");
				// 设置邮件收件人
				intent.putExtra(Intent.EXTRA_EMAIL,
						new String[] { Constant.AUTHOR_EMAIL });
				// 设置邮件标题
				intent.putExtra(Intent.EXTRA_SUBJECT,
						getResources().getString(R.string.email_title));
				// 设置邮件内容
				intent.putExtra(Intent.EXTRA_TEXT, mView_FeedbackContent
						.getText().toString());
				// 调用系统的邮件系统
				startActivity(Intent.createChooser(intent, getResources()
						.getString(R.string.select_email_app)));
			}
			break;
		default:
			break;
		}

	}
}
