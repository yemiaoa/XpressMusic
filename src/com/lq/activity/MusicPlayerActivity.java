package com.lq.activity;

import java.lang.ref.WeakReference;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lq.service.MusicService;

public class MusicPlayerActivity extends FragmentActivity {
	public static final int SET_PLAY_BUTTON_IMAGE = 1;
	public static final int SET_PAUSE_BUTTON_IMAGE = 2;

	private ImageButton mView_ib_back = null;
	private ImageButton mView_ib_favorite = null;
	private TextView mView_tv_songtitle = null;
	private TextView mView_tv_current_time = null;
	private TextView mView_tv_total_time = null;
	private SeekBar mView_sb_song_progress = null;
	private ImageButton mView_ib_play_mode = null;
	private ImageButton mView_ib_play_previous = null;
	private ImageButton mView_ib_play_or_pause = null;
	private ImageButton mView_ib_play_next = null;
	private ImageButton mView_ib_list = null;

	private boolean mIsPlay = false;

	private GestureDetector mDetector = null;

	/** 服务端的信使，通过它发送消息来与MusicService交互 */
	private Messenger mServiceMessenger = null;

	/** 客户端的信使，公布给服务端，服务端通过它发送消息给IncomingHandler处理 */
	private final Messenger mClientMessenger = new Messenger(
			new ClientIncomingHandler(MusicPlayerActivity.this));

	/** 处理来自服务端的消息 */
	private static class ClientIncomingHandler extends Handler {
		// 使用弱引用，避免Handler造成的内存泄露(Message持有Handler的引用，内部定义的Handler类持有外部类的引用)
		WeakReference<MusicPlayerActivity> mFragmentWeakReference = null;
		MusicPlayerActivity mActivity = null;

		public ClientIncomingHandler(MusicPlayerActivity activity) {
			mFragmentWeakReference = new WeakReference<MusicPlayerActivity>(
					activity);
			mActivity = mFragmentWeakReference.get();
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SET_PLAY_BUTTON_IMAGE:
				mActivity.mIsPlay = false;
				mActivity.mView_ib_play_or_pause
						.setImageResource(R.drawable.button_play);
				break;
			case SET_PAUSE_BUTTON_IMAGE:
				mActivity.mIsPlay = true;
				mActivity.mView_ib_play_or_pause
						.setImageResource(R.drawable.button_pause);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}

	/** 与Service连接时交互的类 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// 保持一个对服务端信使的引用，以便向服务端发送消息
			mServiceMessenger = new Messenger(service);
			try {
				// 一旦客户端与服务端连接上，让服务端保持一个客户端信使的引用，以便服务端向客户端发送消息
				Message msg = Message.obtain(null,
						MusicService.MESSAGE_REGISTER_CLIENT_MESSENGER);
				msg.replyTo = mClientMessenger;
				mServiceMessenger.send(msg);
				mServiceMessenger.send(Message.obtain(null,
						MusicService.MESSAGE_SET_PLAYBUTTON_IMAGE_BY_MEDIAPLAYER_STATE));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// 客户端与服务端取消连接时，告知服务端停止向本客户端发送消息
			try {
				Message msg = Message.obtain(null,
						MusicService.MESSAGE_UNREGISTER_CLIENT_MESSENGER);
				mServiceMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			mServiceMessenger = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_musicplay);

		initViews();

		// 左滑切换至主页
		mDetector = new GestureDetector(new LeftGestureListener());

	}

	@Override
	protected void onStart() {
		super.onStart();

		// 本Activity界面显示时绑定服务，服务发送消息给本Activity以更新UI
		bindService(new Intent(MusicPlayerActivity.this, MusicService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();

		// 本Activity界面不可见时取消绑定服务，服务端无需发送消息过来，本Activity不可见时无需更新界面
		unbindService(mServiceConnection);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		backToMain();
	}

	/** 获取布局的各个控件，并设置相关参数、监听器等 */
	private void initViews() {
		mView_ib_back = (ImageButton) findViewById(R.id.play_button_back);
		mView_ib_favorite = (ImageButton) findViewById(R.id.play_favorite);
		mView_ib_list = (ImageButton) findViewById(R.id.play_list);
		mView_ib_play_mode = (ImageButton) findViewById(R.id.play_mode);
		mView_ib_play_next = (ImageButton) findViewById(R.id.play_playnext);
		mView_ib_play_previous = (ImageButton) findViewById(R.id.play_playprevious);
		mView_ib_play_or_pause = (ImageButton) findViewById(R.id.play_playbutton);
		mView_sb_song_progress = (SeekBar) findViewById(R.id.play_progress);
		mView_tv_current_time = (TextView) findViewById(R.id.play_current_time);
		mView_tv_total_time = (TextView) findViewById(R.id.play_song_total_time);
		mView_tv_songtitle = (TextView) findViewById(R.id.play_song_title);

		mView_ib_back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				backToMain();
			}
		});

		mView_ib_play_or_pause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsPlay) {
					startService(new Intent(MusicService.ACTION_PAUSE));
				} else {
					startService(new Intent(MusicService.ACTION_PLAY));
				}
			}
		});
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
