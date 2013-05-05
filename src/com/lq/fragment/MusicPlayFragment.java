package com.lq.fragment;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lq.activity.MainContentActivity;
import com.lq.activity.R;
import com.lq.adapter.LyricAdapter;
import com.lq.entity.LyricSentence;
import com.lq.entity.MusicItem;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.service.MusicService.OnPlaybackStateChangeListener;
import com.lq.service.MusicService.OnServiceConnectionListener;
import com.lq.service.MusicService.State;
import com.lq.util.LyricLoadHelper.LyricListener;
import com.lq.util.TimeHelper;

public class MusicPlayFragment extends Fragment {
	public static final String TAG = MusicPlayFragment.class.getSimpleName();

	public static final int MSG_SET_LYRIC_INDEX = 1;

	private MainContentActivity mActivity = null;

	private ImageButton mView_ib_back = null;
	private ImageButton mView_ib_favorite = null;
	private TextView mView_tv_songtitle = null;
	private TextView mView_tv_current_time = null;
	private TextView mView_tv_total_time = null;
	private TextView mView_tv_lyric_empty = null;
	private SeekBar mView_sb_song_progress = null;
	private ImageButton mView_ib_play_mode = null;
	private ImageButton mView_ib_play_previous = null;
	private ImageButton mView_ib_play_or_pause = null;
	private ImageButton mView_ib_play_next = null;
	private ImageButton mView_ib_list = null;
	private ListView mView_lv_lyricshow = null;

	private LyricAdapter mLyricAdapter = null;

	private boolean mIsPlay = false;
	private MusicItem mPlaySong = null;

	private MusicPlaybackLocalBinder mMusicServiceBinder = null;

	private ClientIncomingHandler mHandler = new ClientIncomingHandler(this);

	/** 处理来自服务端的消息 */
	private static class ClientIncomingHandler extends Handler {
		// 使用弱引用，避免Handler造成的内存泄露(Message持有Handler的引用，内部定义的Handler类持有外部类的引用)
		WeakReference<MusicPlayFragment> mFragmentWeakReference = null;
		MusicPlayFragment mFragment = null;

		public ClientIncomingHandler(MusicPlayFragment fragment) {
			mFragmentWeakReference = new WeakReference<MusicPlayFragment>(
					fragment);
			mFragment = mFragmentWeakReference.get();
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SET_LYRIC_INDEX:
				mFragment.mView_lv_lyricshow.smoothScrollToPositionFromTop(
						msg.arg1, mFragment.mView_lv_lyricshow.getHeight() / 2);
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

			// 传递OnServiceConnectionListener对象给Service，以便其发生变化时通知本Activity
			mMusicServiceBinder
					.registerOnServiceConnectionListener(mOnServiceConnectionListener);

			// 传递OnPlaybackStateChangeListener对象给Service，以便音乐回放状态发生变化时通知本Activity
			mMusicServiceBinder
					.registerOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);

		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected");

			if (mMusicServiceBinder != null) {
				mMusicServiceBinder
						.unregisterOnServiceConnectionListener(mOnServiceConnectionListener);
				mMusicServiceBinder
						.unregisterOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
			}
		}
	};

	public void onAttach(Activity activity) {
		Log.i(TAG, "onAttach");
		super.onAttach(activity);
		mActivity = (MainContentActivity) activity;
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.layout_musicplay, container,
				false);
		mView_ib_back = (ImageButton) rootView
				.findViewById(R.id.play_button_back);
		mView_ib_favorite = (ImageButton) rootView
				.findViewById(R.id.play_favorite);
		mView_ib_list = (ImageButton) rootView.findViewById(R.id.play_list);
		mView_ib_play_mode = (ImageButton) rootView
				.findViewById(R.id.play_mode);
		mView_ib_play_next = (ImageButton) rootView
				.findViewById(R.id.play_playnext);
		mView_ib_play_previous = (ImageButton) rootView
				.findViewById(R.id.play_playprevious);
		mView_ib_play_or_pause = (ImageButton) rootView
				.findViewById(R.id.play_playbutton);
		mView_sb_song_progress = (SeekBar) rootView
				.findViewById(R.id.play_progress);
		mView_tv_current_time = (TextView) rootView
				.findViewById(R.id.play_current_time);
		mView_tv_total_time = (TextView) rootView
				.findViewById(R.id.play_song_total_time);
		mView_tv_songtitle = (TextView) rootView
				.findViewById(R.id.play_song_title);
		mView_lv_lyricshow = (ListView) rootView.findViewById(R.id.lyricshow);
		mView_tv_lyric_empty = (TextView) rootView
				.findViewById(R.id.lyric_empty);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		initViews();
	}

	@Override
	public void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();

		// 本Activity界面显示时绑定服务，服务发送消息给本Activity以更新UI
		getActivity().bindService(
				new Intent(getActivity(), MusicService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();

		// 本界面不可见时取消绑定服务，服务端无需发送消息过来，不可见时无需更新界面
		getActivity().unbindService(mServiceConnection);
		if (mMusicServiceBinder != null) {
			mMusicServiceBinder
					.unregisterOnServiceConnectionListener(mOnServiceConnectionListener);
			mMusicServiceBinder
					.unregisterOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
		}
	}

	@Override
	public void onDetach() {
		Log.i(TAG, "onDetach");
		super.onDetach();
		mActivity = null;
	}

	/** 由包含本实例的Activity调用此方法 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
			mActivity.switchToSlidingMenu();
			break;
		case KeyEvent.KEYCODE_BACK:
			mActivity.switchToMain();
			break;
		default:
			break;
		}
		return true;
	}

	/** 对各个控件设置相关参数、监听器等 */
	private void initViews() {
		mLyricAdapter = new LyricAdapter(getActivity());
		mView_lv_lyricshow.setAdapter(mLyricAdapter);
		mView_lv_lyricshow.setEmptyView(mView_tv_lyric_empty);
		mView_lv_lyricshow.startAnimation(AnimationUtils.loadAnimation(
				getActivity(), R.anim.fade_in));

		mView_tv_current_time.setText(TimeHelper
				.milliSecondsToFormatTimeString(0));
		mView_tv_total_time.setText(TimeHelper
				.milliSecondsToFormatTimeString(0));

		mView_ib_back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.switchToMain();
			}
		});

		mView_ib_play_mode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mMusicServiceBinder != null) {
					mMusicServiceBinder.changePlayMode();
				}
			}
		});

		mView_ib_play_previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().startService(
						new Intent(MusicService.ACTION_PREVIOUS));
			}
		});

		mView_ib_play_or_pause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsPlay) {
					getActivity().startService(
							new Intent(MusicService.ACTION_PAUSE));
				} else {
					getActivity().startService(
							new Intent(MusicService.ACTION_PLAY));
				}
			}
		});

		mView_ib_play_next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity()
						.startService(new Intent(MusicService.ACTION_NEXT));
			}
		});

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
				mView_tv_total_time.setText(TimeHelper
						.milliSecondsToFormatTimeString(playingSong
								.getDuration()));
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
			mView_tv_total_time.setText(TimeHelper
					.milliSecondsToFormatTimeString(0));
			mView_tv_songtitle.setText("");
			mView_tv_current_time.setText(TimeHelper
					.milliSecondsToFormatTimeString(0));
			mView_sb_song_progress.setProgress(0);
			mPlaySong = null;
		}

		@Override
		public void onPlayNewSong(MusicItem playingSong) {
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
			if (lyricSentences != null) {
				mLyricAdapter.setLyric(lyricSentences);
				mLyricAdapter.setCurrentSentenceIndex(index);
				// 本方法执行时，lyricshow的控件还没有加载完成，所以延迟下再执行相关命令
				mHandler.sendMessageDelayed(
						Message.obtain(null, MSG_SET_LYRIC_INDEX, index, 0),
						100);
			}
		}

		@Override
		public void onLyricSentenceChanged(int indexOfCurSentence) {
			mLyricAdapter.setCurrentSentenceIndex(indexOfCurSentence);
			mView_lv_lyricshow
					.smoothScrollToPositionFromTop(indexOfCurSentence,
							mView_lv_lyricshow.getHeight() / 2, 500);
		}
	};

}
