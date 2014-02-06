package com.lq.activity;

import java.lang.ref.WeakReference;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.lq.adapter.LyricAdapter;
import com.lq.entity.LyricSentence;
import com.lq.entity.TrackInfo;
import com.lq.fragment.SelectPlaylistDialogFragment;
import com.lq.fragment.TrackDetailDialogFragment;
import com.lq.listener.OnPlaybackStateChangeListener;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.service.MusicService.PlayMode;
import com.lq.service.MusicService.State;
import com.lq.util.Constant;
import com.lq.util.LyricLoadHelper.LyricListener;
import com.lq.util.TimeHelper;
import com.lq.xpressmusic.R;
import com.umeng.analytics.MobclickAgent;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PlayerActivity extends FragmentActivity {
	public static final String TAG = PlayerActivity.class.getSimpleName();

	public static final int MSG_SET_LYRIC_INDEX = 1;

	/** 手势检测 */
	private GestureDetector mDetector = null;

	private ImageButton mView_ib_back = null;
	private ImageButton mView_ib_more_functions = null;
	private TextView mView_tv_songtitle = null;
	private TextView mView_tv_current_time = null;
	private TextView mView_tv_total_time = null;
	private TextView mView_tv_lyric_empty = null;
	private SeekBar mView_sb_song_progress = null;
	private ImageButton mView_ib_play_mode = null;
	private ImageButton mView_ib_play_previous = null;
	private ImageButton mView_ib_play_or_pause = null;
	private ImageButton mView_ib_play_next = null;
	private ImageButton mView_ib_playqueue = null;
	private ListView mView_lv_lyricshow = null;

	private PopupMenu mOverflowPopupMenu = null;

	private LyricAdapter mLyricAdapter = null;

	private boolean mIsPlay = false;
	private TrackInfo mPlaySong = null;

	private MusicPlaybackLocalBinder mMusicServiceBinder = null;

	private ClientIncomingHandler mHandler = new ClientIncomingHandler(this);

	/** 处理来自服务端的消息 */
	private static class ClientIncomingHandler extends Handler {
		// 使用弱引用，避免Handler造成的内存泄露(Message持有Handler的引用，内部定义的Handler类持有外部类的引用)
		WeakReference<PlayerActivity> mFragmentWeakReference = null;
		PlayerActivity mActivity = null;

		public ClientIncomingHandler(PlayerActivity a) {
			mFragmentWeakReference = new WeakReference<PlayerActivity>(a);
			mActivity = mFragmentWeakReference.get();
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SET_LYRIC_INDEX:
				if (mActivity.mLyricAdapter.isEmpty()) {
					Log.i(TAG, "歌词为空");
					mActivity.mView_tv_lyric_empty
							.setText(R.string.there_is_no_lyric_yet);
				} else {
					mActivity.mView_lv_lyricshow.setSelectionFromTop(msg.arg1,
							mActivity.mView_lv_lyricshow.getHeight() / 2);
				}
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

			// 保持对Service的Binder引用，以便调用Service提供给客户端的方法
			mMusicServiceBinder = (MusicPlaybackLocalBinder) service;

			// 传递LyricListener对象给Service，以便歌词发生变化时通知本Activity
			mMusicServiceBinder.registerLyricListener(mLyricListener);

			// 传递OnPlaybackStateChangeListener对象给Service，以便音乐回放状态发生变化时通知本Activity
			mMusicServiceBinder
					.registerOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);

			// 请求加载歌词
			mMusicServiceBinder.requestLoadLyric();

			initCurrentPlayInfo(mMusicServiceBinder.getCurrentPlayInfo());
		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected");

			if (mMusicServiceBinder != null) {
				mMusicServiceBinder
						.unregisterOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
			}
		}
	};

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		findViews();
		initViewsSetting();
	};

	@Override
	public void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();

		// 本Activity界面显示时绑定服务，服务发送消息给本Activity以更新UI
		bindService(new Intent(PlayerActivity.this, MusicService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE);

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
	public void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();

		// 本界面不可见时取消绑定服务，服务端无需发送消息过来，不可见时无需更新界面
		unbindService(mServiceConnection);
		if (mMusicServiceBinder != null) {
			mMusicServiceBinder
					.unregisterOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
			mMusicServiceBinder.unRegisterLyricListener();
			mMusicServiceBinder = null;
		}

		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public void onBackPressed() {
		if (!getSupportFragmentManager().popBackStackImmediate()) {
			switchToMain();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return this.mDetector.onTouchEvent(event);
	}

	/** 对各个控件设置相关参数、监听器等 */
	private void initViewsSetting() {
		// 手势设置----------------------------------------------
		// 左滑切换至主页
		mDetector = new GestureDetector(new SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				// 从左向右滑动
				if (e1.getX() - e2.getX() < -120) {
					switchToMain();
					return true;
				}
				return false;
			}
		});
		View.OnTouchListener gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (mDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};
		mView_lv_lyricshow.setOnTouchListener(gestureListener);

		// 歌词秀设置---------------------------------------------------------------
		mLyricAdapter = new LyricAdapter(this);
		mView_lv_lyricshow.setAdapter(mLyricAdapter);
		mView_lv_lyricshow.setEmptyView(mView_tv_lyric_empty);
		mView_lv_lyricshow.startAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in));

		// 当前播放信息-----------------------------------------------------
		mView_tv_current_time.setText(TimeHelper
				.milliSecondsToFormatTimeString(0));
		mView_tv_total_time.setText(TimeHelper
				.milliSecondsToFormatTimeString(0));

		// 回退按键----------------------------------------------------------
		mView_ib_back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switchToMain();
			}
		});

		// 播放控制-----------------------------------------------------------------
		// 播放模式--
		mView_ib_play_mode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mMusicServiceBinder != null) {
					mMusicServiceBinder.changePlayMode();
				}
			}
		});

		// 上一首--
		mView_ib_play_previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayerActivity.this.startService(new Intent(
						MusicService.ACTION_PREVIOUS));
			}
		});

		// 播放、暂停
		mView_ib_play_or_pause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsPlay) {
					PlayerActivity.this.startService(new Intent(
							MusicService.ACTION_PAUSE));
				} else {
					PlayerActivity.this.startService(new Intent(
							MusicService.ACTION_PLAY));
				}
			}
		});

		// 下一首--
		mView_ib_play_next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayerActivity.this.startService(new Intent(
						MusicService.ACTION_NEXT));
			}
		});

		// 可拖动的进度条
		mView_sb_song_progress
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// 拖动播放进度条后发送消息给服务端，指示从指定进度开始播放
						if (mMusicServiceBinder != null && mPlaySong != null) {
							mMusicServiceBinder.seekToSpecifiedPosition(seekBar
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
							mView_tv_current_time.setText(TimeHelper
									.milliSecondsToFormatTimeString(progress
											* mPlaySong.getDuration()
											/ seekBar.getMax()));
						}
					}
				});

		// 显示当前播放队列的按钮--
		mView_ib_playqueue.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 显示当前播放队列
				startActivity(new Intent(PlayerActivity.this,
						PlayQueueActivity.class));
			}
		});

		// 播放界面的功能列表--
		mView_ib_more_functions.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 弹出播放界面的功能列表
				mOverflowPopupMenu.show();
			}
		});
		mOverflowPopupMenu
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (mPlaySong != null) {
							// 播放界面功能列表项目
							DialogFragment df = null;

							switch (item.getItemId()) {
							case R.id.track_addto:
								// 弹出选择播放列表的窗口
								df = SelectPlaylistDialogFragment
										.newInstance(new long[] { mPlaySong
												.getId() });
								df.show(getSupportFragmentManager(), null);
								break;
							case R.id.track_info:
								// 弹出歌曲详细信息的窗口
								if (mPlaySong != null) {
									df = TrackDetailDialogFragment
											.newInstance(mPlaySong);
									df.show(getSupportFragmentManager(), null);
								}
								break;

							default:
								break;
							}
							return true;
						}
						return false;
					}
				});
	}

	/**
	 * 根据播放模式设置播放模式按钮的图标
	 * 
	 * @param mode
	 *            音乐播放模式
	 * */
	private void setPlayModeImage(int mode) {
		switch (mode) {
		case PlayMode.REPEAT_SINGLE:
			mView_ib_play_mode
					.setImageResource(R.drawable.button_playmode_repeat_single);
			// Toast.makeText(getApplicationContext(),
			// getResources().getString(R.string.playmode_repeat_single),
			// Toast.LENGTH_SHORT).show();
			break;
		case PlayMode.REPEAT:
			mView_ib_play_mode
					.setImageResource(R.drawable.button_playmode_repeat);
			// Toast.makeText(getApplicationContext(),
			// getResources().getString(R.string.playmode_repeat),
			// Toast.LENGTH_SHORT).show();
			break;
		case PlayMode.SEQUENTIAL:
			mView_ib_play_mode
					.setImageResource(R.drawable.button_playmode_sequential);
			// Toast.makeText(getApplicationContext(),
			// getResources().getString(R.string.playmode_sequential),
			// Toast.LENGTH_SHORT).show();
			break;
		case PlayMode.SHUFFLE:
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

	/** 初始化当前播放信息 */
	private void initCurrentPlayInfo(Bundle bundle) {
		int playMode = bundle.getInt(Constant.PLAY_MODE);
		int currentPlayerState = bundle.getInt(Constant.PLAYING_STATE);
		int currenPlayPosition = bundle.getInt(Constant.CURRENT_PLAY_POSITION,
				0);
		TrackInfo playingSong = bundle
				.getParcelable(Constant.PLAYING_MUSIC_ITEM);

		// 根据播放状态，设置播放按钮的图片
		if (currentPlayerState == State.Playing
				|| currentPlayerState == State.Preparing) {
			mIsPlay = true;
			mView_ib_play_or_pause.setImageResource(R.drawable.button_pause);
		} else {
			mIsPlay = false;
			mView_ib_play_or_pause.setImageResource(R.drawable.button_play);
		}

		// 设置歌曲标题、时长、当前播放时间、当前播放进度
		mPlaySong = playingSong;
		if (playingSong != null) {
			mView_tv_total_time.setText(TimeHelper
					.milliSecondsToFormatTimeString(playingSong.getDuration()));
			mView_tv_songtitle.setText(playingSong.getTitle());
			mView_tv_current_time.setText(TimeHelper
					.milliSecondsToFormatTimeString(currenPlayPosition));
			mView_sb_song_progress.setProgress(currenPlayPosition
					* mView_sb_song_progress.getMax()
					/ (int) playingSong.getDuration());
		}

		// 设置播放模式按钮图片
		setPlayModeImage(playMode);
	}

	private void findViews() {
		setContentView(R.layout.layout_musicplay);
		mView_ib_back = (ImageButton) findViewById(R.id.play_button_back);
		mView_ib_more_functions = (ImageButton) findViewById(R.id.play_more_functions);
		mView_ib_playqueue = (ImageButton) findViewById(R.id.play_list);
		mView_ib_play_mode = (ImageButton) findViewById(R.id.play_mode);
		mView_ib_play_next = (ImageButton) findViewById(R.id.play_playnext);
		mView_ib_play_previous = (ImageButton) findViewById(R.id.play_playprevious);
		mView_ib_play_or_pause = (ImageButton) findViewById(R.id.play_playbutton);
		mView_sb_song_progress = (SeekBar) findViewById(R.id.play_progress);
		mView_tv_current_time = (TextView) findViewById(R.id.play_current_time);
		mView_tv_total_time = (TextView) findViewById(R.id.play_song_total_time);
		mView_tv_songtitle = (TextView) findViewById(R.id.play_song_title);
		mView_lv_lyricshow = (ListView) findViewById(R.id.lyricshow);
		mView_tv_lyric_empty = (TextView) findViewById(R.id.lyric_empty);
		mOverflowPopupMenu = new PopupMenu(PlayerActivity.this,
				mView_ib_more_functions);
		mOverflowPopupMenu.getMenuInflater()
				.inflate(R.menu.track_operations_in_player,
						mOverflowPopupMenu.getMenu());

	}

	private void switchToMain() {
		Intent intent = new Intent(PlayerActivity.this,
				MainContentActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
		this.finish();
	}

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
			mView_tv_total_time.setText(TimeHelper
					.milliSecondsToFormatTimeString(0));
			mView_tv_songtitle.setText("");
			mView_tv_current_time.setText(TimeHelper
					.milliSecondsToFormatTimeString(0));
			mView_sb_song_progress.setProgress(0);
			mLyricAdapter.setLyric(null);
			mLyricAdapter.notifyDataSetChanged();
			mPlaySong = null;
		}

		@Override
		public void onPlayNewSong(TrackInfo playingSong) {
			// 播放新的歌曲时，更新显示的歌曲信息
			mPlaySong = playingSong;
			if (playingSong != null) {
				mView_tv_total_time.setText(TimeHelper
						.milliSecondsToFormatTimeString(playingSong
								.getDuration()));
				mView_tv_songtitle.setText(playingSong.getTitle());
				mView_tv_current_time.setText(TimeHelper
						.milliSecondsToFormatTimeString(0));
				mView_sb_song_progress.setProgress(0);
			}
			// 歌词秀清空
			mView_tv_lyric_empty.setText(R.string.lyric_Loading);
		}

		@Override
		public void onPlayModeChanged(int playMode) {
			setPlayModeImage(playMode);
		}

		@Override
		public void onPlayProgressUpdate(int currentMillis) {
			// 更新当前播放时间
			mView_tv_current_time.setText(TimeHelper
					.milliSecondsToFormatTimeString(currentMillis));
			// 更新当前播放进度
			mView_sb_song_progress.setProgress(currentMillis
					* mView_sb_song_progress.getMax()
					/ (int) mPlaySong.getDuration());
		}
	};

	private LyricListener mLyricListener = new LyricListener() {

		@Override
		public void onLyricLoaded(List<LyricSentence> lyricSentences, int index) {
			Log.i(TAG, "onLyricLoaded");
			if (lyricSentences != null) {
				Log.i(TAG, "onLyricLoaded--->歌词句子数目=" + lyricSentences.size()
						+ ",当前句子索引=" + index);
				mLyricAdapter.setLyric(lyricSentences);
				mLyricAdapter.setCurrentSentenceIndex(index);
				mLyricAdapter.notifyDataSetChanged();
				// 本方法执行时，lyricshow的控件还没有加载完成，所以延迟下再执行相关命令
				mHandler.sendMessageDelayed(
						Message.obtain(null, MSG_SET_LYRIC_INDEX, index, 0),
						100);
			}
		}

		@Override
		public void onLyricSentenceChanged(int indexOfCurSentence) {
			Log.i(TAG, "onLyricSentenceChanged--->当前句子索引=" + indexOfCurSentence);
			mLyricAdapter.setCurrentSentenceIndex(indexOfCurSentence);
			mLyricAdapter.notifyDataSetChanged();
			mView_lv_lyricshow
					.smoothScrollToPositionFromTop(indexOfCurSentence,
							mView_lv_lyricshow.getHeight() / 2, 500);
		}
	};

}
