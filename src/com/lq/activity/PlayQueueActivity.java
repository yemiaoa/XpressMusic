package com.lq.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.lq.entity.TrackInfo;
import com.lq.fragment.PromptDialogFragment;
import com.lq.listener.OnPlaybackStateChangeListener;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.service.MusicService.State;
import com.lq.util.Constant;
import com.lq.xpressmusic.R;
import com.umeng.analytics.MobclickAgent;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PlayQueueActivity extends FragmentActivity implements
		OnItemClickListener {
	private static final String TAG = PlayQueueActivity.class.getSimpleName();

	private ListView mListView = null;
	private View mClear = null;
	private TextView mTitle = null;
	private ArrayList<TrackInfo> mDataList = new ArrayList<TrackInfo>();
	private ArrayList<String> mShownList = new ArrayList<String>();
	private int mPlayingSongPosition = -1;
	private MyArrayAdapter mAdapter = null;
	/** 与MusicService交互的类 */
	private MusicPlaybackLocalBinder mMusicServiceBinder = null;
	private TrackInfo mPlayingTrack = null;
	private int mPlayingState = State.Stopped;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_playqueue);
		mListView = (ListView) findViewById(R.id.listview_play_queue);
		mClear = findViewById(R.id.playqueue_clear);
		mTitle = (TextView) findViewById(R.id.playqueue_title);
	}

	@Override
	protected void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();
		// 本Activity界面显示时绑定服务，服务发送消息给本Activity以更新UI
		bindService(new Intent(PlayQueueActivity.this, MusicService.class),
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
	protected void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();
		// 本界面不可见时取消绑定服务，服务端无需发送消息过来，不可见时无需更新界面
		mMusicServiceBinder
				.unregisterOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
		unbindService(mServiceConnection);
		mDataList = null;
		mMusicServiceBinder = null;

		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		mShownList.clear();
		mShownList = null;
	}

	@Override
	public void onAttachedToWindow() {
		// 设置本Activity在父窗口的位置
		super.onAttachedToWindow();
		View view = getWindow().getDecorView();
		WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view
				.getLayoutParams();
		lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
		lp.x = getResources().getDimensionPixelSize(
				R.dimen.playqueue_dialog_marginright);
		lp.y = getResources().getDimensionPixelSize(
				R.dimen.playqueue_dialog_marginbottom);
		lp.width = getResources().getDimensionPixelSize(
				R.dimen.playqueue_dialog_width);
		lp.height = getResources().getDimensionPixelSize(
				R.dimen.playqueue_dialog_height);
		getWindowManager().updateViewLayout(view, lp);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// 点击音乐进行播放
		Intent intent = new Intent(MusicService.ACTION_PLAY);
		intent.putExtra(Constant.REQUEST_PLAY_ID, mDataList.get(position)
				.getId());
		intent.putExtra(Constant.CLICK_ITEM_IN_LIST, true);
		startService(intent);
		PlayQueueActivity.this.finish();
	}

	/** 获取传递过来的数据 */
	private void handleArguments() {
		Bundle args = mMusicServiceBinder.getCurrentPlayInfo();
		mPlayingState = args.getInt(Constant.PLAYING_STATE, State.Stopped);

		List<TrackInfo> list = args.getParcelableArrayList(Constant.DATA_LIST);
		if (list != null) {
			mDataList.addAll(list);
		}

		mPlayingTrack = args.getParcelable(Constant.PLAYING_MUSIC_ITEM);
		if (mPlayingTrack != null) {
			mPlayingSongPosition = MusicService.seekPosInListById(mDataList,
					mPlayingTrack.getId());
		}
		if (mDataList != null) {
			for (int i = 0; i < mDataList.size(); i++) {
				mShownList.add(mDataList.get(i).getTitle());
			}
		}
	}

	/** 设置各个视图控件 */
	private void initViewsSetting() {
		// 根据数据源设置标题
		if (mDataList == null) {
			mTitle.setText(getResources().getString(R.string.playqueue) + "(0)");
		} else if (mDataList.size() == 0) {
			mTitle.setText(getResources().getString(R.string.playqueue) + "(0)");
		} else {
			mTitle.setText(getResources().getString(R.string.playqueue) + "("
					+ mDataList.size() + ")");
		}

		// 设置清空按钮
		mClear.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mShownList.size() != 0) {
					DialogFragment df = PromptDialogFragment.newInstance(
							getResources().getString(
									R.string.confirm_clear_playqueue),
							mDeletePromptListener);
					df.show(getSupportFragmentManager(), null);
				}
			}
		});

		// 设置ListView
		mAdapter = new MyArrayAdapter(PlayQueueActivity.this,
				android.R.layout.simple_list_item_1, mShownList);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(PlayQueueActivity.this);
		if (mPlayingSongPosition != -1) {
			mListView.setSelectionFromTop(
					mPlayingSongPosition,
					getResources().getDimensionPixelSize(
							R.dimen.playqueue_dialog_select_item_from_top));
		}
	}

	/** 与Service连接时交互的类 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "onServiceConnected");

			// 保持对Service的Binder引用，以便调用Service提供给客户端的方法
			mMusicServiceBinder = (MusicPlaybackLocalBinder) service;

			mMusicServiceBinder
					.registerOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);

			handleArguments();
			initViewsSetting();
		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected");
		}
	};

	// 删除提醒对话框处理------------------------------------------------------------------
	private DialogInterface.OnClickListener mDeletePromptListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {

			startService(new Intent(MusicService.ACTION_STOP));
			mMusicServiceBinder.setCurrentPlayList(null);
			mShownList.clear();
			mAdapter.notifyDataSetChanged();
			mTitle.setText(getResources().getString(R.string.playqueue) + "(0)");
			PlayQueueActivity.this.finish();
		}
	};

	private OnPlaybackStateChangeListener mOnPlaybackStateChangeListener = new OnPlaybackStateChangeListener() {

		@Override
		public void onMusicPlayed() {
			mPlayingState = State.Playing;
		}

		@Override
		public void onMusicPaused() {
			mPlayingState = State.Paused;
		}

		@Override
		public void onMusicStopped() {
			mPlayingState = State.Stopped;
		}

		@Override
		public void onPlayNewSong(TrackInfo playingSong) {
			mPlayingSongPosition = MusicService.seekPosInListById(mDataList,
					playingSong.getId());
			mAdapter.notifyDataSetChanged();
		}

		@Override
		public void onPlayModeChanged(int playMode) {

		}

		@Override
		public void onPlayProgressUpdate(int currentMillis) {

		}

	};

	class MyArrayAdapter extends ArrayAdapter<String> {

		public MyArrayAdapter(Context context, int textViewResourceId,
				List<String> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = (TextView) super.getView(position, convertView,
					parent);
			if (position == mPlayingSongPosition) {
				// 对正在播放的歌曲设置高亮
				tv.setTextColor(getResources().getColor(
						R.color.holo_orange_dark));
			} else {
				tv.setTextColor(getResources().getColor(R.color.black));
			}
			return tv;
		}
	}

}
