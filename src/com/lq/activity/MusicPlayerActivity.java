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
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lq.entity.MusicItem;
import com.lq.service.MusicService;
import com.lq.utility.TimeUtility;

public class MusicPlayerActivity extends FragmentActivity {
	public final String TAG = MusicPlayerActivity.class.getSimpleName();

	public static final int SET_PLAY_BUTTON_IMAGE = 1;
	public static final int SET_PAUSE_BUTTON_IMAGE = 2;
	public static final int UPDATE_PLAYING_SONG_PROGRESS = 3;
	public static final int SET_PLAYING_SONG_INFO = 4;
	public static final int SET_PLAYING_INFO = 5;
	public static final int SET_PLAYING_MODE = 6;

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
	private MusicItem mPlaySong = null;

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
			case UPDATE_PLAYING_SONG_PROGRESS:
				mActivity.mView_tv_current_time.setText(TimeUtility
						.milliSecondsToFormatTimeString(msg.arg1));
				mActivity.mView_sb_song_progress.setProgress(msg.arg1
						* mActivity.mView_sb_song_progress.getMax() / msg.arg2);
				break;
			case SET_PLAYING_SONG_INFO:
				mActivity.mPlaySong = (MusicItem) msg.obj;
				mActivity.mView_tv_total_time.setText(TimeUtility
						.milliSecondsToFormatTimeString(mActivity.mPlaySong
								.getDuration()));
				mActivity.mView_tv_songtitle.setText(mActivity.mPlaySong
						.getTitle());
				break;
			case SET_PLAYING_INFO:
				Bundle info = (Bundle) msg.obj;
				mActivity.setPlayModeImage(info.getInt("playmode"));
				if (info.getInt("duration") != 0) {
					mActivity.mView_sb_song_progress.setProgress(info
							.getInt("cur_pos")
							* mActivity.mView_sb_song_progress.getMax()
							/ info.getInt("duration"));
					mActivity.mView_tv_current_time.setText(TimeUtility
							.milliSecondsToFormatTimeString(info
									.getInt("cur_pos")));
				} else {
					mActivity.mView_sb_song_progress.setProgress(0);
					mActivity.mView_tv_current_time.setText(TimeUtility
							.milliSecondsToFormatTimeString(0));
					mActivity.mView_tv_total_time.setText(TimeUtility
							.milliSecondsToFormatTimeString(0));
				}
				mActivity.mView_tv_songtitle.setText(info
						.getString("songtitle"));
				break;
			case SET_PLAYING_MODE:
				mActivity.setPlayModeImage(msg.arg1);
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
			Log.i(TAG, "onServiceConnected");

			// 保持一个对服务端信使的引用，以便向服务端发送消息
			mServiceMessenger = new Messenger(service);
			try {
				// 一旦客户端与服务端连接上，让服务端保持一个客户端信使的引用，以便服务端向客户端发送消息
				Message msg = Message.obtain(null,
						MusicService.MESSAGE_REGISTER_CLIENT_MESSENGER);
				msg.replyTo = mClientMessenger;
				msg.obj = MusicPlayerActivity.class.getSimpleName();
				mServiceMessenger.send(msg);

				// 通知服务端根据当前播放状态更新播放按钮的图片（显示为播放或者暂停）
				mServiceMessenger
						.send(Message
								.obtain(null,
										MusicService.MESSAGE_SET_PLAYBUTTON_IMAGE_BY_MEDIAPLAYER_STATE));

				// 通知服务端开始更新播放进度条
				mServiceMessenger.send(Message.obtain(null,
						MusicService.MESSAGE_UPDATE_PLAYING_SONG_PROGRESS));

				// 通知服务端让服务端把把当前播放的歌曲信息传递给本Activity(如果没有，则什么也不发生)
				mServiceMessenger.send(Message.obtain(null,
						MusicService.MESSAGE_DELIVER_PLAYING_SONG_INFO));

				// 通知服务端发送当前播放的信息(播放模式等)
				mServiceMessenger.send(Message.obtain(null,
						MusicService.MESSAGE_DELIVER_PLAYING_INFO));

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected");

			stopCommunicateWithService();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_musicplay);

		initViews();

		// 左滑切换至主页
		mDetector = new GestureDetector(new LeftGestureListener());

	}

	@Override
	protected void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();

		// 本Activity界面显示时绑定服务，服务发送消息给本Activity以更新UI
		bindService(new Intent(MusicPlayerActivity.this, MusicService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();

		// 本Activity界面不可见时取消绑定服务，服务端无需发送消息过来，本Activity不可见时无需更新界面
		unbindService(mServiceConnection);
		stopCommunicateWithService();
		MusicPlayerActivity.this.finish();
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
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

		mView_tv_current_time.setText(TimeUtility
				.milliSecondsToFormatTimeString(0));
		mView_tv_total_time.setText(TimeUtility
				.milliSecondsToFormatTimeString(0));

		mView_ib_back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				backToMain();
			}
		});

		mView_ib_play_mode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					mServiceMessenger.send(Message.obtain(null,
							MusicService.MESSAGE_CHANGE_PLAY_MODE));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		mView_ib_play_previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					mServiceMessenger.send(Message.obtain(null,
							MusicService.MESSAGE_PLAY_PREVIOUS_SONG));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
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

		mView_ib_play_next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					mServiceMessenger.send(Message.obtain(null,
							MusicService.MESSAGE_PLAY_NEXT_SONG));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});

		mView_sb_song_progress
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// 拖动播放进度条后发送消息给服务端，指示从指定进度开始播放
						try {
							mServiceMessenger.send(Message
									.obtain(null,
											MusicService.MESSAGE_SET_PLAYING_SONG_PROGRESS,
											seekBar.getProgress(),
											seekBar.getMax()));
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						if (fromUser && mPlaySong != null) {
							// 根据滑动的进度计算出对应的播放时刻
							mView_tv_current_time.setText(TimeUtility
									.milliSecondsToFormatTimeString(progress
											* mPlaySong.getDuration()
											/ seekBar.getMax()));
						}
					}
				});
	}

	/** 告知服务端停止通信 */
	private void stopCommunicateWithService() {
		// 客户端与服务端取消连接时，告知服务端停止向本客户端发送消息
		try {
			// 通知服务端停止更新播放进度条
			mServiceMessenger.send(Message.obtain(null,
					MusicService.MESSAGE_STOP_UPDATE_PLAYING_SONG_PROGRESS));

			// 通知服务端客户端已经不存在
			Message msg = Message.obtain(null,
					MusicService.MESSAGE_UNREGISTER_CLIENT_MESSENGER);
			msg.obj = MusicPlayerActivity.class.getSimpleName();
			mServiceMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		mServiceMessenger = null;
	}

	private void backToMain() {
		startActivity(new Intent(MusicPlayerActivity.this,
				MainContentActivity.class));
		MusicPlayerActivity.this.finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
	}

	private void setPlayModeImage(int mode) {
		switch (mode) {
		case MusicService.PLAYMODE_REPEAT_SINGLE:
			mView_ib_play_mode
					.setImageResource(R.drawable.button_playmode_repeat_single);
			// Toast.makeText(getApplicationContext(),
			// getResources().getString(R.string.playmode_repeat_single),
			// Toast.LENGTH_SHORT).show();
			break;
		case MusicService.PLAYMODE_REPEAT:
			mView_ib_play_mode
					.setImageResource(R.drawable.button_playmode_repeat);
			// Toast.makeText(getApplicationContext(),
			// getResources().getString(R.string.playmode_repeat),
			// Toast.LENGTH_SHORT).show();
			break;
		case MusicService.PLAYMODE_SEQUENTIAL:
			mView_ib_play_mode
					.setImageResource(R.drawable.button_playmode_sequential);
			// Toast.makeText(getApplicationContext(),
			// getResources().getString(R.string.playmode_sequential),
			// Toast.LENGTH_SHORT).show();
			break;
		case MusicService.PLAYMODE_SHUFFLE:
			mView_ib_play_mode
					.setImageResource(R.drawable.button_playmode_shuffle);
			// Toast.makeText(getApplicationContext(),
			// getResources().getString(R.string.playmode_shuffle),
			// Toast.LENGTH_SHORT).show();
			break;
		default:
			break;
		}
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
