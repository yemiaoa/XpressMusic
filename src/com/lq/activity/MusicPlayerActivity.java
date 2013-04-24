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
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.service.MusicService.OnPlaybackStateChangeListener;
import com.lq.service.MusicService.OnServiceConnectionListener;
import com.lq.service.MusicService.State;
import com.lq.utility.TimeUtility;

public class MusicPlayerActivity extends FragmentActivity {
	public final String TAG = MusicPlayerActivity.class.getSimpleName();

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

	private MusicService mMusicService = null;

	private ClientIncomingHandler mHandler = new ClientIncomingHandler(
			MusicPlayerActivity.this);

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

			// 保持对Service的引用，以便调用Service提供的公共方法访问后台数据
			mMusicService = ((MusicPlaybackLocalBinder) service).getService();

			// 传递OnServiceConnectionListener对象给Service，以便其发生变化时通知本Activity
			mMusicService
					.registerOnServiceConnectionListener(mOnServiceConnectionListener);

			// 传递OnPlaybackStateChangeListener对象给Service，以便其发生变化时通知本Activity
			mMusicService
					.registerOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);

		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected");

			if (mMusicService != null) {
				mMusicService
						.unregisterOnServiceConnectionListener(mOnServiceConnectionListener);
				mMusicService
						.unregisterOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
			}
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
		if (mMusicService != null) {
			mMusicService
					.unregisterOnServiceConnectionListener(mOnServiceConnectionListener);
			mMusicService
					.unregisterOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
		}
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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return this.mDetector.onTouchEvent(event);
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
				if (mMusicService != null) {
					mMusicService.changePlayMode();
				}
			}
		});

		mView_ib_play_previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startService(new Intent(MusicService.ACTION_PREVIOUS));
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
				startService(new Intent(MusicService.ACTION_NEXT));
			}
		});

		mView_sb_song_progress
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// 拖动播放进度条后发送消息给服务端，指示从指定进度开始播放
						if (mMusicService != null && mPlaySong != null) {
							mMusicService.seekToSpecifiedPosition(seekBar
									.getProgress()
									* (int) mPlaySong.getDuration()
									/ seekBar.getMax());
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

	private void backToMain() {
		startActivity(new Intent(MusicPlayerActivity.this,
				MainContentActivity.class));
		MusicPlayerActivity.this.finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
	}

	/**
	 * 根据播放模式设置播放模式按钮的图标
	 * 
	 * @param mode
	 *            音乐播放模式
	 * */
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

	private OnServiceConnectionListener mOnServiceConnectionListener = new OnServiceConnectionListener() {

		@Override
		public void onServiceConnected(State currentPlayerState,
				MusicItem playingSong, int currenPlayPosition, int playMode) {
			// 根据播放状态，设置播放按钮的图片
			if (currentPlayerState == State.Playing
					|| currentPlayerState == State.Preparing) {
				mIsPlay = true;
				mView_ib_play_or_pause
						.setImageResource(R.drawable.button_pause);
			} else {
				mIsPlay = false;
				mView_ib_play_or_pause.setImageResource(R.drawable.button_play);
			}

			// 设置歌曲标题、时长、当前播放时间、当前播放进度
			mPlaySong = playingSong;
			if (playingSong != null) {
				mView_tv_total_time.setText(TimeUtility
						.milliSecondsToFormatTimeString(playingSong
								.getDuration()));
				mView_tv_songtitle.setText(playingSong.getTitle());
				mView_tv_current_time.setText(TimeUtility
						.milliSecondsToFormatTimeString(currenPlayPosition));
				mView_sb_song_progress.setProgress(currenPlayPosition
						* mView_sb_song_progress.getMax()
						/ (int) playingSong.getDuration());
			}

			// 设置播放模式按钮图片
			setPlayModeImage(playMode);
		}

		@Override
		public void onServiceDisconnected() {

		}

	};

	private OnPlaybackStateChangeListener mOnPlaybackStateChangeListener = new OnPlaybackStateChangeListener() {

		@Override
		public void onMusicPlayed() {
			// 音乐播放时，播放按钮设置为暂停的图标（意为点击暂停）
			mIsPlay = true;
			mView_ib_play_or_pause.setImageResource(R.drawable.button_pause);
		}

		@Override
		public void onMusicPaused() {
			// 音乐暂停时，播放按钮设置为播放的图标（意为点击播放）
			mIsPlay = false;
			mView_ib_play_or_pause.setImageResource(R.drawable.button_play);
		}

		@Override
		public void onMusicStopped() {
			// 音乐播放停止时，清空歌曲信息的显示
			mIsPlay = false;
			mView_ib_play_or_pause.setImageResource(R.drawable.button_play);
			mView_tv_total_time.setText(TimeUtility
					.milliSecondsToFormatTimeString(0));
			mView_tv_songtitle.setText("");
			mView_tv_current_time.setText(TimeUtility
					.milliSecondsToFormatTimeString(0));
			mView_sb_song_progress.setProgress(0);
			mPlaySong = null;
		}

		@Override
		public void onPlayNewSong(MusicItem playingSong) {
			// 播放新的歌曲时，更新显示的歌曲信息
			mPlaySong = playingSong;
			if (playingSong != null) {
				mView_tv_total_time.setText(TimeUtility
						.milliSecondsToFormatTimeString(playingSong
								.getDuration()));
				mView_tv_songtitle.setText(playingSong.getTitle());
				mView_tv_current_time.setText(TimeUtility
						.milliSecondsToFormatTimeString(0));
				mView_sb_song_progress.setProgress(0);
			}
		}

		@Override
		public void onPlayModeChanged(int playMode) {
			setPlayModeImage(playMode);
		}

		@Override
		public void onPlayProgressUpdate(int currentMillis) {
			// 更新当前播放时间
			mView_tv_current_time.setText(TimeUtility
					.milliSecondsToFormatTimeString(currentMillis));
			// 更新当前播放进度
			mView_sb_song_progress.setProgress(currentMillis
					* mView_sb_song_progress.getMax()
					/ (int) mPlaySong.getDuration());
		}
	};

}
