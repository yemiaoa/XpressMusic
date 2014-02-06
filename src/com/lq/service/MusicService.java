/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lq.service;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Audio.Media;
import android.util.Log;
import android.widget.Toast;

import com.lq.activity.MainContentActivity;
import com.lq.xpressmusic.R;
import com.lq.entity.LyricSentence;
import com.lq.entity.TrackInfo;
import com.lq.fragment.SettingFragment;
import com.lq.listener.OnPlaybackStateChangeListener;
import com.lq.receiver.MediaButtonReceiver;
import com.lq.util.AudioFocusHelper;
import com.lq.util.AudioFocusHelper.MusicFocusable;
import com.lq.util.Constant;
import com.lq.util.LyricDownloadManager;
import com.lq.util.LyricLoadHelper;
import com.lq.util.LyricLoadHelper.LyricListener;

/**
 * 这是处理音乐回放的服务，在应用中对媒体的所有处理都交给这个服务。
 * 
 * @author lq 2013-6-1 lq2625304@gmail.com
 */
public class MusicService extends Service implements OnCompletionListener,
		OnPreparedListener, OnErrorListener, MusicFocusable, LyricListener {

	public class MusicPlaybackLocalBinder extends Binder {

		public void registerOnPlaybackStateChangeListener(
				OnPlaybackStateChangeListener listener) {
			mOnPlaybackStateChangeListeners.add(listener);
		}

		public void unregisterOnPlaybackStateChangeListener(
				OnPlaybackStateChangeListener listener) {
			mOnPlaybackStateChangeListeners.remove(listener);
		}

		public void registerLyricListener(LyricListener listener) {
			mLyricListener = listener;
		}

		public void unRegisterLyricListener() {
			mLyricListener = null;
		}

		/**
		 * 让MediaPlayer将当前播放跳转到指定播放位置
		 * 
		 * @param milliSeconds
		 *            指定的已播放的毫秒数
		 * */
		public void seekToSpecifiedPosition(int milliSeconds) {
			if (mState != State.Stopped) {
				mMediaPlayer.seekTo(milliSeconds);
			}
		}

		/** 改变播放模式 */
		public void changePlayMode() {
			mPlayMode = (mPlayMode + 1) % 4;

			if (mMediaPlayer != null) {
				// 如果正在播放歌曲
				switch (mPlayMode) {
				case PlayMode.REPEAT_SINGLE:
					// 如果是单曲循环，给MediaPlayer启动单曲播放
					mMediaPlayer.setLooping(true);
					break;
				default:
					// 如果不是单曲循环，取消MediaPlayer的单曲播放
					mMediaPlayer.setLooping(false);
					break;
				}
			}

			// 通知各个OnPlaybackStateChangeListener播放模式已经改变，并传递新的播放
			for (int i = 0; i < mOnPlaybackStateChangeListeners.size(); i++) {
				mOnPlaybackStateChangeListeners.get(i).onPlayModeChanged(
						mPlayMode);
			}
		}

		/**
		 * 设置当前的播放列表
		 * 
		 * @param list
		 *            播放列表,每项包含每首歌曲的详细信息
		 * */
		public void setCurrentPlayList(List<TrackInfo> list) {
			mPlayList.clear();
			if (list != null) {
				mPlayList.addAll(list);
				mHasPlayList = true;
				mRequestPlayPos = 0;
			} else {
				mHasPlayList = false;
				mRequestPlayPos = -1;
				mRequsetPlayId = -1;
			}

		}

		/** 向当前播放队列追加歌曲 */
		public void appendToCurrentPlayList(List<TrackInfo> list) {
			if (list == null) {
				return;
			}

			// 只添加当前播放列表中没有的歌曲
			boolean existed = false;
			for (int i = 0; i < list.size(); i++) {
				existed = false;
				for (int j = 0; j < mPlayList.size(); j++) {
					if (mPlayList.get(j).equals(list.get(i))) {
						existed = true;
						break;
					}
				}
				if (!existed) {
					mPlayList.add(list.get(i));
				}
			}
			mHasPlayList = true;
			if (mState == State.Stopped) {
				mRequestPlayPos = 0;
				processPlayRequest();
			}
		}

		/** 获取当前播放的信息 */
		public Bundle getCurrentPlayInfo() {
			Bundle bundle = new Bundle();
			TrackInfo item = null;
			int currentPlayPos = 0;

			if (mState == State.Playing || mState == State.Paused) {
				item = mPlayingSong;
				currentPlayPos = mMediaPlayer.getCurrentPosition();
			}
			bundle.putParcelable(Constant.PLAYING_MUSIC_ITEM, item);
			bundle.putInt(Constant.CURRENT_PLAY_POSITION, currentPlayPos);
			bundle.putInt(Constant.PLAYING_STATE, mState);
			bundle.putInt(Constant.PLAY_MODE, mPlayMode);
			bundle.putInt(Constant.PLAYING_SONG_POSITION_IN_LIST,
					mPlayingSongPos);
			bundle.putParcelableArrayList(Constant.DATA_LIST, mPlayList);

			return bundle;
		}

		/** 如果当前正在播放歌曲，通知LyricListener载入歌词 */
		public void requestLoadLyric() {
			Log.i(TAG, "requestLoadLyric");
			if (mPlayingSong != null && mState != State.Stopped) {
				Log.i(TAG, "requestLoadLyric--->loadLyric");
				loadLyric(mPlayingSong.getData());
				mLyricLoadHelper.notifyTime(mMediaPlayer.getCurrentPosition());
			}
		}

		/** 从当前播放队列移除指定歌曲 */
		public void removeSongFromCurrenPlaylist(long trackId) {
			if (mHasPlayList) {
				for (int i = 0; i < mPlayList.size(); i++) {
					if (mPlayList.get(i).getId() == trackId) {
						if (i < mPlayingSongPos) {
							mRequestPlayPos = mPlayingSongPos--;
						}
						mPlayList.remove(i);
						break;
					}
				}
				if (mPlayList.size() == 0) {
					mHasPlayList = false;
					processStopRequest();
				} else if (trackId == mPlayingSong.getId()) {
					--mPlayingSongPos;
					// mPlayingSong = mPlayList.get(mPlayingSongPos);
					if (mPlayMode == PlayMode.SHUFFLE && mPlayQueue.size() != 0) {
						mRequestPlayPos = mPlayQueue.pop();
					} else {
						mRequestPlayPos--;
					}
				}
			}
		}
	}

	// 打印调试信息用的标记
	final static String TAG = MusicService.class.getSimpleName();

	// 这里定义的是我们准备处理的Intent的各种动作标记，在这里定义仅仅是为了方便引用，
	// 真正要让本service处理这些动作，要在AndroidManifest.xml中<service>里的<action>标签内声明它们
	public static final String ACTION_PLAY = "com.lq.musicplayer.action.PLAY";
	public static final String ACTION_PAUSE = "com.lq.musicplayer.action.PAUSE";
	public static final String ACTION_STOP = "com.lq.musicplayer.action.STOP";
	public static final String ACTION_PREVIOUS = "com.lq.musicplayer.action.PREVIOUS";
	public static final String ACTION_NEXT = "com.lq.musicplayer.action.NEXT";

	// 消息类型
	public static final int MESSAGE_UPDATE_PLAYING_SONG_PROGRESS = 1;
	public static final int MESSAGE_ON_LYRIC_LOADED = 2;
	public static final int MESSAGE_ON_LYRIC_SENTENCE_CHANGED = 3;

	/**
	 * 歌曲播放的状态(Preparing,Playing,Paused,Stopped)
	 * */
	public class State {
		public static final int Stopped = 0; // MediaPlayer已经停止工作，不再准备播放
		public static final int Preparing = 1; // MediaPlayer正在准备中
		public static final int Playing = 2; // 正在播放（MediaPlayer已经准备好了）
		// （但是当丢失音频焦点时，MediaPlayer在此状态下实际上也许已经暂停了，
		// 但是我们仍然保持这个状态，以表明我们必须在一获得音频焦点时就返回播放状态）
		public static final int Paused = 3; // 播放暂停 (MediaPlayer处于准备好了的状态)
	};

	/**
	 * 播放模式<br>
	 * 0代表单曲循环，1代表列表循环，2代表顺序播放，3代表随机播放
	 */
	public class PlayMode {
		public static final int REPEAT_SINGLE = 0;
		public static final int REPEAT = 1;
		public static final int SEQUENTIAL = 2;
		public static final int SHUFFLE = 3;
	}

	private int mState = State.Stopped;

	private int mPlayMode = 1;

	/** 回放状态变化的观察者集合 */
	private ArrayList<OnPlaybackStateChangeListener> mOnPlaybackStateChangeListeners = new ArrayList<OnPlaybackStateChangeListener>();

	private MusicPlaybackLocalBinder mBinder = new MusicPlaybackLocalBinder();

	private LyricListener mLyricListener = null;

	// 当前播放列表
	private ArrayList<TrackInfo> mPlayList = new ArrayList<TrackInfo>();

	// 丢失音频焦点，我们为媒体播放设置一个低音量(1.0f为最大)，而不是停止播放
	private static final float DUCK_VOLUME = 0.1f;

	// 我们的媒体播放控制器
	private MediaPlayer mMediaPlayer = null;

	// 音频焦点的辅助类（API LEVEL > 8 时才能使用）
	private AudioFocusHelper mAudioFocusHelper = null;

	/** 当前播放列表的播放队列，记录当前播放列表歌曲播放顺序 */
	private LinkedList<Integer> mPlayQueue = new LinkedList<Integer>();

	private boolean mHasPlayList = false;
	private boolean mHasLyric = false;

	private TrackInfo mPlayingSong = null;
	private int mPlayingSongPos = 0;// XXX 使用这个变量也许会有未知BUG
	private int mRequestPlayPos = -1;
	private long mRequsetPlayId = -1;

	// 我们要播放的音乐是否是来自网络的媒体流
	private boolean mIsStreaming = false;

	// 当从网络获取媒体流时保持一个Wifi锁，防止设备突然与Wifi连接的音频断开
	private WifiLock mWifiLock;

	// 提示播放的通知的ID
	private final int NOTIFICATION_ID = 1;

	private AudioManager mAudioManager;
	private NotificationManager mNotificationManager;

	private Notification mNotification = null;

	private ComponentName mAudioBecomingNoisyReceiverName = null;

	private Random mRandom = new Random();

	private LyricLoadHelper mLyricLoadHelper = new LyricLoadHelper();
	private LyricDownloadManager mLyricDownloadManager = null;

	/** Service的Handler,可以延迟指定时间发送消息，而messenger不可以延时发送消息 */
	private ServiceIncomingHandler mServiceHandler = new ServiceIncomingHandler(
			MusicService.this);

	/** 处理来自客户端的消息 */
	private static class ServiceIncomingHandler extends Handler {
		// 使用弱引用，避免Handler造成的内存泄露(Message持有Handler的引用，内部定义的Handler类持有外部类的引用)
		WeakReference<MusicService> mServiceWeakReference = null;
		MusicService mService = null;

		public ServiceIncomingHandler(MusicService service) {
			mServiceWeakReference = new WeakReference<MusicService>(service);
			mService = mServiceWeakReference.get();
		}

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_UPDATE_PLAYING_SONG_PROGRESS:
				if (mService.mState == State.Playing) {
					int millisecond = mService.mMediaPlayer
							.getCurrentPosition();
					// 通知所有音乐播放的观察者，进度更新了唉
					for (int i = 0; i < mService.mOnPlaybackStateChangeListeners
							.size(); i++) {
						mService.mOnPlaybackStateChangeListeners.get(i)
								.onPlayProgressUpdate(millisecond);
					}

					if (mService.mHasLyric) {
						// 通知歌词载入辅助类当前播放时间
						mService.mLyricLoadHelper.notifyTime(millisecond);
					}

					mService.mServiceHandler.sendEmptyMessageDelayed(
							MESSAGE_UPDATE_PLAYING_SONG_PROGRESS, 500);
				}
				break;
			case MESSAGE_ON_LYRIC_LOADED:
				if (mService.mLyricListener != null) {
					mService.mLyricListener.onLyricLoaded(
							(List<LyricSentence>) msg.obj, msg.arg1);
				}
				break;
			case MESSAGE_ON_LYRIC_SENTENCE_CHANGED:
				if (mService.mLyricListener != null) {
					mService.mLyricListener.onLyricSentenceChanged(msg.arg1);
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	@Override
	public void onCreate() {
		Log.i(TAG, "debug: Creating MusicService");

		// 创建Wifi锁（并没有获得该锁，仅仅是创建出来）
		mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
				.createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		mAudioBecomingNoisyReceiverName = new ComponentName(getPackageName(),
				MediaButtonReceiver.class.getName());
		mAudioManager
				.registerMediaButtonEventReceiver(mAudioBecomingNoisyReceiverName);

		// 创建音频焦点辅助类，API LEVEL > 8 时SDK才支持音频焦点这个特性
		mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);

		// 本Service实现LyricListener接口，只是为了做个代理延迟一下歌词事件的调用。
		// 因为bindService启动过程稍慢，客户端的LyricListener来不及注册歌词就已经加载好了。
		mLyricLoadHelper.setLyricListener(MusicService.this);

		mLyricDownloadManager = new LyricDownloadManager(
				getApplicationContext());
		startWatchingExternalStorage();
	}

	/**
	 * 通过startService()启动本服务会调用此方法，在此接受并处理发送方传递的Intent。
	 * 根据传递过来的Intent的action来作出其要求的处理。
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();

		if (action.equals(ACTION_PLAY)) {
			if (mHasPlayList) {
				// 如果是列表中点击的
				if (intent.getBooleanExtra(Constant.CLICK_ITEM_IN_LIST, false)) {
					// 获取到点击的歌曲的ID
					mRequsetPlayId = intent.getLongExtra(
							Constant.REQUEST_PLAY_ID, 0);
					mRequestPlayPos = seekPosInListById(mPlayList,
							mRequsetPlayId);
				} else {
					mRequsetPlayId = mPlayList.get(mRequestPlayPos).getId();
				}
				if (mRequestPlayPos != -1) {
					processPlayRequest();
				}
			}
		} else if (action.equals(ACTION_PAUSE)) {
			processPauseRequest();
		} else if (action.equals(ACTION_STOP)) {
			processStopRequest();
		} else if (action.equals(ACTION_PREVIOUS)) {
			if (mHasPlayList) {
				processPreviousRequest(true);
			}
		} else if (action.equals(ACTION_NEXT)) {
			if (mHasPlayList) {
				processNextRequest(true);
			}
		}
		return START_NOT_STICKY; // 当本服务被系统关闭后，不必再重启启动
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		// Service关闭时释放所有持有的资源
		mState = State.Stopped;
		relaxResources(true);
		mAudioFocusHelper.giveUpAudioFocus();
		if (mAudioManager != null && mAudioBecomingNoisyReceiverName != null) {
			mAudioManager
					.unregisterMediaButtonEventReceiver(mAudioBecomingNoisyReceiverName);
		}
		if (mState != State.Stopped) {
			processStopRequest();
		}
		mLyricListener = null;
		mOnPlaybackStateChangeListeners.clear();
		mOnPlaybackStateChangeListeners = null;
		mPlayList.clear();
		mPlayList = null;
		unregisterReceiver(mExternalStorageReceiver);
	}

	/** MediaPlayer完成了一首歌曲的播放时调用此方法 */
	public void onCompletion(MediaPlayer player) {
		mHasLyric = false;
		mLyricLoadHelper.setIndexOfCurrentSentence(-1);
		// 当前播放完成，播放下一首
		processNextRequest(false);
	}

	/** MediaPlayer完成了准备时调用此方法 */
	public void onPrepared(MediaPlayer player) {
		// 准备完成了，可以播放歌曲了
		mState = State.Playing;
		updateNotification(mPlayingSong.getTitle() + " (playing)");
		configAndStartMediaPlayer();
		if (!mServiceHandler.hasMessages(MESSAGE_UPDATE_PLAYING_SONG_PROGRESS)) {
			mServiceHandler
					.sendEmptyMessage(MESSAGE_UPDATE_PLAYING_SONG_PROGRESS);
		}
	}

	/**
	 * 播放媒体时发生了错误调用此方法,错误发生后停止播放.
	 */
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Toast.makeText(getApplicationContext(),
				"Media player error! Resetting.", Toast.LENGTH_SHORT).show();
		switch (what) {
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			Log.e(TAG, "Error: "
					+ "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK"
					+ ", extra=" + String.valueOf(extra));
			break;
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			Log.e(TAG, "Error: " + "MEDIA_ERROR_SERVER_DIED" + ", extra="
					+ String.valueOf(extra));
			break;
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			Log.e(TAG,
					"Error: " + "MEDIA_ERROR_UNKNOWN" + ", extra="
							+ String.valueOf(extra));
			break;
		case -38:
			Log.e(TAG,
					"Error: what"
							+ what
							+ ", extra="
							+ extra
							+ ",see at http://blog.sina.com.cn/s/blog_632b619d01012991.html");
			break;
		default:
			Log.e(TAG, "Error: what" + what + ", extra=" + extra);
			break;
		}

		processStopRequest(false);

		return true; // true表示我们处理了发生的错误
	}

	public void onGainedAudioFocus() {
		Log.i(TAG, "gained audio focus.");

		// 用新的音频焦点状态来重置MediaPlayer
		if (mState == State.Playing)
			configAndStartMediaPlayer();
	}

	public void onLostAudioFocus() {
		Log.i(TAG, "lost audio focus.");

		// 以新的焦点参数启动/重启/暂停MediaPlayer
		if (mMediaPlayer != null && mMediaPlayer.isPlaying())
			configAndStartMediaPlayer();
	}

	/**
	 * 确保MediaPlayer存在，并且已经被重置。 这个方法将会在需要时创建一个MediaPlayer，
	 * 或者重置一个已存在的MediaPlayer。
	 */
	private void createMediaPlayerIfNeeded() {
		if (mMediaPlayer == null) {
			mMediaPlayer = new MediaPlayer();

			// 确保我们的MediaPlayer在播放时获取了一个唤醒锁，
			// 如果不这样做，当歌曲播放很久时，CPU进入休眠从而导致播放停止
			// 要使用唤醒锁，要确保在AndroidManifest.xml中声明了android.permission.WAKE_LOCK权限
			mMediaPlayer.setWakeMode(getApplicationContext(),
					PowerManager.PARTIAL_WAKE_LOCK);

			// 在MediaPlayer在它准备完成时、完成播放时、发生错误时通过监听器通知我们，
			// 以便我们做出相应处理
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnErrorListener(this);
		} else
			mMediaPlayer.reset();
	}

	private void processPlayRequest() {
		mAudioFocusHelper.tryToGetAudioFocus();

		// 如果处于“停止”状态，直接播放下一首歌曲
		// 如果处于“播放”或者“暂停”状态，并且请求播放的歌曲与当前播放的歌曲不同，则播放请求的歌曲
		if (mState != State.Stopped && mPlayingSong == null) {
			mPlayingSongPos = mRequestPlayPos;
			playSong();
		} else if (mState == State.Stopped
				|| (mState != State.Stopped && mPlayingSong.getId() != mRequsetPlayId)) {
			mPlayingSongPos = mRequestPlayPos;
			playSong();
		} else if (mState == State.Paused
				&& mPlayingSong.getId() == mRequsetPlayId) {
			// 如果处于“暂停”状态，则继续播放，并且恢复“前台服务”的状态
			mState = State.Playing;
			setUpAsForeground(mPlayingSong.getTitle() + " (playing)");
			configAndStartMediaPlayer();
		} else if (mMediaPlayer.isLooping()) {
			mMediaPlayer.start();
		}

		// 通知所有的观察者音乐开始播放了
		for (int i = 0; i < mOnPlaybackStateChangeListeners.size(); i++) {
			mOnPlaybackStateChangeListeners.get(i).onMusicPlayed();
		}

		// 更新进度条
		if (!mServiceHandler.hasMessages(MESSAGE_UPDATE_PLAYING_SONG_PROGRESS)) {
			mServiceHandler
					.sendEmptyMessage(MESSAGE_UPDATE_PLAYING_SONG_PROGRESS);
		}
	}

	private void processPauseRequest() {
		if (mState == State.Playing) {
			mState = State.Paused;
			mMediaPlayer.pause();
			relaxResources(false); // 暂停时，取消“前台服务”的状态，但依然保持MediaPlayer的资源
			// 仍然保持着音频焦点

			// 通知所有的观察者音乐暂停了
			for (int i = 0; i < mOnPlaybackStateChangeListeners.size(); i++) {
				mOnPlaybackStateChangeListeners.get(i).onMusicPaused();
			}
		}
	}

	/**
	 * 播放上一首歌曲。根据播放模式计算出上一首歌的ID，然后调用播放方法。
	 * 
	 * @param fromUser
	 *            是否是来自用户的请求
	 * */
	private void processPreviousRequest(boolean fromUser) {
		if (mState == State.Playing || mState == State.Paused
				|| mState == State.Stopped) {
			switch (mPlayMode) {
			case PlayMode.REPEAT:
			case PlayMode.SEQUENTIAL:
				if (--mRequestPlayPos < 0) {
					mRequestPlayPos = mPlayList.size() - 1;
				}
				break;
			case PlayMode.SHUFFLE:
				if (mPlayQueue.size() != 0) {
					mRequestPlayPos = mPlayQueue.pop();
				} else {
					mRequestPlayPos = mRandom.nextInt(mPlayList.size());
				}
				break;
			case PlayMode.REPEAT_SINGLE:
				if (fromUser) {
					// 如果是用户请求播放上一首，就顺序播放上一首
					if (--mRequestPlayPos < 0) {
						mRequestPlayPos = mPlayList.size() - 1;
					}
				} else {
					// 如果不是用户请求，循环播放
					mMediaPlayer.setLooping(true);
				}
			default:
				break;
			}
			mRequsetPlayId = mPlayList.get(mRequestPlayPos).getId();
			processPlayRequest();
		}
	}

	/**
	 * 播放下一首歌曲。根据播放模式计算出下一首歌的ID，然后调用播放方法
	 * 
	 * @param fromUser
	 *            是否是来自用户的请求
	 * */
	private void processNextRequest(boolean fromUser) {
		if (mState == State.Playing || mState == State.Paused
				|| mState == State.Stopped) {
			switch (mPlayMode) {
			case PlayMode.REPEAT:
				mRequestPlayPos = (mPlayingSongPos + 1) % mPlayList.size();
				break;
			case PlayMode.SEQUENTIAL:
				mRequestPlayPos = (mPlayingSongPos + 1) % mPlayList.size();
				if (mRequestPlayPos == 0) {
					if (fromUser) {
						mRequestPlayPos = 0;
					} else {
						// 播放到当前播放列表的最后一首便停止播放
						mRequestPlayPos = mPlayList.size() - 1;
						processStopRequest();
						return;
					}
				}
				break;
			case PlayMode.SHUFFLE:
				mPlayQueue.push(mPlayingSongPos);
				mRequestPlayPos = mRandom.nextInt(mPlayList.size());
				break;
			case PlayMode.REPEAT_SINGLE:
				if (fromUser) {
					// 如果是用户请求，就顺序播放下一首
					mRequestPlayPos = (mPlayingSongPos + 1) % mPlayList.size();
				} else {
					// 如果不是用户请求，循环播放
					mMediaPlayer.setLooping(true);
				}
				break;
			default:
				break;
			}
			mRequsetPlayId = mPlayList.get(mRequestPlayPos).getId();
			processPlayRequest();
		}
	}

	private void processStopRequest() {
		processStopRequest(false);
	}

	private void processStopRequest(boolean force) {
		if (mState == State.Playing || mState == State.Paused || force) {
			mState = State.Stopped;
			mRequsetPlayId = -1;
			mPlayingSongPos = 0;
			mRequestPlayPos = 0;
			// 释放所有持有的资源
			relaxResources(true);
			mAudioFocusHelper.giveUpAudioFocus();

			// 本服务已经不再使用，终结它
			// stopSelf();

			// 通知所有的观察者音乐停止播放了
			for (int i = 0; i < mOnPlaybackStateChangeListeners.size(); i++) {
				mOnPlaybackStateChangeListeners.get(i).onMusicStopped();
			}
		}
	}

	/**
	 * 释放本服务所使用的资源，包括“前台服务”状态，通知，唤醒锁，和MediaPlayer
	 * 
	 * @param releaseMediaPlayer
	 *            指示MediaPlayer是否要释放掉
	 */
	private void relaxResources(boolean releaseMediaPlayer) {
		// 取消 "foreground service"的状态
		stopForeground(true);

		// 停止并释放MediaPlayer
		if (releaseMediaPlayer && mMediaPlayer != null) {
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}

		// 如果持有Wifi锁，也将其释放
		if (mWifiLock.isHeld())
			mWifiLock.release();
	}

	/**
	 * 根据音频焦点的设置重新设置MediaPlayer的参数，然后启动或者重启它。 如果我们拥有音频焦点，则正常播放;
	 * 如果没有音频焦点，根据当前的焦点设置将MediaPlayer切换为“暂停”状态或者低声播放。
	 * 这个方法已经假设mPlayer不为空，所以如果要调用此方法，确保正确的使用它。
	 */
	private void configAndStartMediaPlayer() {
		if (mAudioFocusHelper.getAudioFocus() == AudioFocusHelper.NoFocusNoDuck) {
			// 如果丢失了音频焦点也不允许低声播放，我们必须让播放暂停，即使mState处于State.Playing状态。
			// 但是我们并不修改mState的状态，因为我们会在获得音频焦点时返回立即返回播放状态。
			if (mMediaPlayer.isPlaying())
				mMediaPlayer.pause();
			return;
		} else if (mAudioFocusHelper.getAudioFocus() == AudioFocusHelper.NoFocusCanDuck)
			mMediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME); // 设置一个较为安静的音量
		else
			mMediaPlayer.setVolume(1.0f, 1.0f); // 设置大声播放

		if (!mMediaPlayer.isPlaying())
			mMediaPlayer.start();
	}

	/**
	 * 播放mPlayingSongPos指定的歌曲.
	 */
	private void playSong() {
		mPlayingSong = mPlayList.get(mPlayingSongPos);
		mState = State.Stopped;
		relaxResources(false); // 除了MediaPlayer，释放所有资源

		try {
			if (mHasPlayList) {
				createMediaPlayerIfNeeded();
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.setDataSource(getApplicationContext(), ContentUris
						.withAppendedId(Media.EXTERNAL_CONTENT_URI,
								mPlayingSong.getId()));
			} else {
				processStopRequest(true);
				return;
			}

			mState = State.Preparing;

			if (mLyricListener != null) {
				loadLyric(mPlayingSong.getData());
			}

			setUpAsForeground(mPlayingSong.getTitle() + " (loading)");

			// 在后台准备MediaPlayer，准备完成后会调用OnPreparedListener的onPrepared()方法。
			// 在MediaPlayer准备好之前，我们不能调用其start()方法
			mMediaPlayer.prepareAsync();

			// 如果正在从网络获取歌曲，我们要持有一个Wifi锁，以防止在音乐播放时系统休眠暂停了音乐。
			// 另一方面，如果没有从网络获取歌曲，并且持有一个Wifi锁，我们要将它释放掉。
			if (mIsStreaming)
				mWifiLock.acquire();
			else if (mWifiLock.isHeld())
				mWifiLock.release();
		} catch (Exception ex) {
			Log.e("MusicService",
					"IOException playing next song: " + ex.getMessage());
			ex.printStackTrace();
		}

		// 每次播放新的歌曲的时候，把当前播放的歌曲信息传递给播放观察者
		for (int i = 0; i < mOnPlaybackStateChangeListeners.size(); i++) {
			mOnPlaybackStateChangeListeners.get(i).onPlayNewSong(mPlayingSong);
		}
	}

	/** 更新通知栏. */
	private void updateNotification(String text) {
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
				0, new Intent(getApplicationContext(),
						MainContentActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		mNotification.setLatestEventInfo(getApplicationContext(),
				"LQMusicPlayer", text, pi);
		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}

	/**
	 * 将本服务设置为“前台服务”。“前台服务”是一个与用户正在交互的服务， 必须在通知栏显示一个通知表示正在交互
	 */
	private void setUpAsForeground(String text) {
		Intent intent = new Intent(getApplicationContext(),
				MainContentActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
				0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		mNotification = new Notification();
		mNotification.tickerText = text;
		mNotification.icon = R.drawable.ic_stat_playing;
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotification.setLatestEventInfo(getApplicationContext(),
				getResources().getString(R.string.app_name), text, pi);
		startForeground(NOTIFICATION_ID, mNotification);
	}

	/**
	 * 根据歌曲的ID，寻找出歌曲在当前播放列表中的位置
	 * 
	 * @param list
	 *            歌曲列表
	 * @param songId
	 *            歌曲ID
	 * @return 返回-1表示未找到
	 */
	public static int seekPosInListById(List<TrackInfo> list, long songId) {
		int result = -1;
		if (list != null) {

			for (int i = 0; i < list.size(); i++) {
				if (songId == list.get(i).getId()) {
					result = i;
					break;
				}
			}
		}
		return result;
	}

	@Override
	public void onLyricLoaded(List<LyricSentence> lyricSentences, int index) {
		if (mLyricListener != null) {
			mLyricListener.onLyricLoaded(lyricSentences, index);
		} else {
			// 来自客户端的LyricListener还没来的及注册，就延迟一会等它注册好了再把参数传递给它
			mServiceHandler.sendMessageDelayed(Message.obtain(null,
					MESSAGE_ON_LYRIC_LOADED, index, 0, lyricSentences), 500);
		}
	}

	@Override
	public void onLyricSentenceChanged(int indexOfCurSentence) {
		if (mLyricListener != null) {
			mLyricListener.onLyricSentenceChanged(indexOfCurSentence);
		} else {
			// 来自客户端的LyricListener还没来的及注册，就延迟一会等它注册好了再把参数传递给它
			mServiceHandler.sendMessageDelayed(Message.obtain(null,
					MESSAGE_ON_LYRIC_LOADED, indexOfCurSentence, 0), 500);
		}
	}

	/**
	 * 读取歌词文件
	 * 
	 * @param path
	 *            歌曲文件的路径
	 */
	private void loadLyric(String path) {
		// 取得歌曲同目录下的歌词文件绝对路径
		String lyricFilePath = Constant.LYRIC_SAVE_FOLDER_PATH + "/"
				+ mPlayingSong.getTitle() + "_" + mPlayingSong.getArtist()
				+ ".lrc";
		File lyricfile = new File(lyricFilePath);

		if (lyricfile.exists()) {
			// 本地有歌词，直接读取
			Log.i(TAG, "loadLyric()--->本地有歌词，直接读取");
			mHasLyric = mLyricLoadHelper.loadLyric(lyricFilePath);
		} else {
			// 获取系统设置，是否自动下载歌词
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			boolean downloadLyricAutomatically = sp.getBoolean(
					SettingFragment.KEY_DOWNLOAD_LYRIC_AUTOMATICALLY, true);
			Log.i(TAG, "loadLyric()--->获取系统设置->是否自动下载歌词:"
					+ downloadLyricAutomatically);
			if (downloadLyricAutomatically) {
				// 尝试网络获取歌词
				Log.i(TAG, "loadLyric()--->本地无歌词，尝试从网络获取");
				new LyricDownloadAsyncTask().execute(mPlayingSong.getTitle(),
						mPlayingSong.getArtist());
			} else {
				// 设置歌词为空
				mHasLyric = mLyricLoadHelper.loadLyric(null);
			}
		}
	}

	private void startWatchingExternalStorage() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.setPriority(1000);
		intentFilter.addDataScheme("file");
		registerReceiver(mExternalStorageReceiver, intentFilter);
	}

	private BroadcastReceiver mExternalStorageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)
					|| intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)
					|| intent.getAction().equals(
							Intent.ACTION_MEDIA_BAD_REMOVAL)) {
				if (mState != State.Stopped) {
					// SD卡移除，停止音乐播放
					processStopRequest(true);

					// 清空当前播放队列
					mBinder.setCurrentPlayList(null);

					// 提示SD卡不可用
					Toast.makeText(getApplicationContext(),
							R.string.sdcard_cannot_use, Toast.LENGTH_SHORT)
							.show();
				}
			} else if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
				// SD卡正常挂载

			}

		}
	};

	class LyricDownloadAsyncTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			// 从网络获取歌词，然后保存到本地
			String lyricFilePath = mLyricDownloadManager.searchLyricFromWeb(
					params[0], params[1]);
			// 返回本地歌词路径
			return lyricFilePath;
		}

		@Override
		protected void onPostExecute(String result) {
			Log.i(TAG, "网络获取歌词完毕，歌词保存路径:" + result);
			// 读取保存到本地的歌曲
			mHasLyric = mLyricLoadHelper.loadLyric(result);
		};

	};
}
