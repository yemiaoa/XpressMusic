package com.lq.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.lq.adapter.TrackMutipleChooseAdapter;
import com.lq.dao.PlaylistDAO;
import com.lq.entity.TrackInfo;
import com.lq.fragment.PromptDialogFragment;
import com.lq.fragment.SelectPlaylistDialogFragment;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.util.Constant;
import com.lq.xpressmusic.R;
import com.umeng.analytics.MobclickAgent;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class MutipleEditActivity extends FragmentActivity implements
		View.OnClickListener {
	public static String ACTION_FINISH = MutipleEditActivity.class.getName()
			+ ".ACTION_FINISH";

	private final String TAG = MutipleEditActivity.class.getSimpleName();

	private ImageView mView_Close = null;
	private TextView mView_Title = null;
	private TextView mView_NumOfSelect = null;
	private CheckBox mView_SelectAll = null;
	private ListView mView_ListView = null;
	private View mView_PlayListLater = null;
	private View mView_AddToPlaylist = null;
	private View mView_Delete = null;
	private TrackMutipleChooseAdapter mAdapter = null;

	// Arguments
	private ArrayList<TrackInfo> mDataList = null;
	private String mTitle = null;
	private int mFirstVisiblePosition = 0;
	private int mParent = -1;
	private int mPlaylistId = -1;

	private int mCloseDelayTime = 500;

	private LocalBroadcastManager mLocalBroadcastManager;
	// 用于关闭本页面的广播接收器
	// 如果在其他界面操作成功，会向发送一个关闭本页面的广播，本接收器会接受到该广播并处理请求
	private BroadcastReceiver mReceiver;

	private MusicPlaybackLocalBinder mMusicServiceBinder = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mutiple_choose);

		handleArguments();
		findViews();
		initViewsSetting();
		initBroadcastReceiver();

	}

	@Override
	protected void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();
		// 本Activity界面显示时绑定服务，服务发送消息给本Activity以更新UI
		bindService(new Intent(MutipleEditActivity.this, MusicService.class),
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
		// 本界面不可见时取消绑定服务
		unbindService(mServiceConnection);

		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		mDataList = null;
		mAdapter = null;
		mMusicServiceBinder = null;
		mLocalBroadcastManager.unregisterReceiver(mReceiver);
	}

	private void handleArguments() {
		// 获取Intent中传递过来的数据
		Bundle args = getIntent().getExtras();
		mDataList = args.getParcelableArrayList(Constant.DATA_LIST);
		mTitle = args.getString(Constant.TITLE) + "(" + mDataList.size() + ")";
		mFirstVisiblePosition = args.getInt(Constant.FIRST_VISIBLE_POSITION, 0);
		mParent = args.getInt(Constant.PARENT, -1);
		mPlaylistId = args.getInt(Constant.PLAYLIST_ID, -1);

	}

	/** 配置广播接收器 */
	private void initBroadcastReceiver() {
		mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_FINISH);
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(ACTION_FINISH)) {
					// 接受到关闭本页面的请求，则关闭本页面
					// 延时关闭(等本Activity弹出的对话框都消失再关闭)
					mHandler.sendEmptyMessageDelayed(0, mCloseDelayTime);
				}
			}
		};
		mLocalBroadcastManager.registerReceiver(mReceiver, filter);
	}

	/** 获取布局中的各个View对象 */
	private void findViews() {
		mView_Close = (ImageView) findViewById(R.id.close_mutiple_edit);
		mView_Title = (TextView) findViewById(R.id.title_mutiple_edit);
		mView_NumOfSelect = (TextView) findViewById(R.id.num_of_select);
		mView_PlayListLater = (View) findViewById(R.id.play_list_later);
		mView_AddToPlaylist = (View) findViewById(R.id.add_to_playlist);
		mView_Delete = (View) findViewById(R.id.delete_selected_item);
		mView_SelectAll = (CheckBox) findViewById(R.id.select_all_cb);
		mView_ListView = (ListView) findViewById(R.id.listview_mutiple);
	}

	/** 初始化各个View的设置 */
	private void initViewsSetting() {
		// ListView的设置--------------------------------------------------------
		mAdapter = new TrackMutipleChooseAdapter(this, mDataList);
		mView_ListView.setAdapter(mAdapter);
		mView_ListView.setSelection(mFirstVisiblePosition);
		mView_ListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mView_ListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// 更新条目勾选状态
				mAdapter.toggleCheckedState(position);

				// 每次选择条目后，更新已选择的数目
				mView_NumOfSelect.setText(getResources().getString(
						R.string.has_selected)
						+ mAdapter.getSelectedItemPositions().length
						+ getResources().getString(R.string.a_piece_of_song));

			}
		});

		// 关闭按钮的设置--------------------------------------------------------
		mView_Close.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 点击关闭按钮，关闭本Activity
				MutipleEditActivity.this.finish();
			}
		});

		// 标题设置--------------------------------------------------------
		mView_Title.setText(mTitle);

		// 已选歌曲数量的设置-----------------------------------------------------
		mView_NumOfSelect.setText(getResources().getString(
				R.string.has_selected)
				+ 0 + getResources().getString(R.string.a_piece_of_song));

		// 全选按钮的设置-----------------------------------------------------
		mView_SelectAll
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						// 根据全选按钮状态，全选或全不选所有条目
						mAdapter.selectAllItem(isChecked);

						// 更新已选择的数目
						mView_NumOfSelect.setText(getResources().getString(
								R.string.has_selected)
								+ mAdapter.getSelectedItemPositions().length
								+ getResources().getString(
										R.string.a_piece_of_song));
					}
				});

		// 三个批量操作按钮的设置-----------------------------------------------------
		mView_AddToPlaylist.setOnClickListener(MutipleEditActivity.this);
		mView_PlayListLater.setOnClickListener(MutipleEditActivity.this);
		mView_Delete.setOnClickListener(MutipleEditActivity.this);

	}

	// 处理歌曲的批量操作
	@Override
	public void onClick(View v) {
		Log.i(TAG, "onClick");
		switch (v.getId()) {
		case R.id.add_to_playlist:
			if (mAdapter.getSelectedItemPositions().length == 0) {
				// 如果尚未选择任何条目,提示一下
				Toast.makeText(MutipleEditActivity.this,
						R.string.please_select_add_song_first,
						Toast.LENGTH_SHORT).show();
			} else {
				// 弹出选择播放列表的窗口
				long[] selectedAudioIds = mAdapter.getSelectedAudioIds();
				DialogFragment df = SelectPlaylistDialogFragment
						.newInstance(selectedAudioIds);
				df.show(getSupportFragmentManager(), null);
			}
			break;
		case R.id.play_list_later:
			if (mAdapter.getSelectedItemPositions().length == 0) {
				// 如果尚未选择任何条目,提示一下
				Toast.makeText(MutipleEditActivity.this,
						R.string.please_select_play_song_first,
						Toast.LENGTH_SHORT).show();
			} else {
				// 追加选择的歌曲条目到后台的正在播放的列表中
				mMusicServiceBinder.appendToCurrentPlayList(mAdapter
						.getSelectedItems());

				// 关闭本界面
				mHandler.sendEmptyMessageDelayed(0, mCloseDelayTime);
			}
			break;
		case R.id.delete_selected_item:
			if (mAdapter.getSelectedItemPositions().length == 0) {
				// 如果尚未选择任何条目,提示一下
				Toast.makeText(MutipleEditActivity.this,
						R.string.please_select_delete_song_first,
						Toast.LENGTH_SHORT).show();
			} else {
				DialogFragment df;
				if (mParent == Constant.START_FROM_PLAYLIST) {
					df = PromptDialogFragment
							.newInstance(
									getResources()
											.getString(
													R.string.confirm_remove_song_from_playlist),
									mDeletePromptListener);
				} else {
					df = PromptDialogFragment.newInstance(getResources()
							.getString(R.string.confirm_delete_song_file),
							mDeletePromptListener);

				}
				df.show(getSupportFragmentManager(), null);
			}
			break;

		default:
			break;
		}

	}

	private MyHandler mHandler = new MyHandler(MutipleEditActivity.this);

	private static class MyHandler extends Handler {
		// 使用弱引用，避免Handler造成的内存泄露(Message持有Handler的引用，内部定义的Handler类持有外部类的引用)
		WeakReference<MutipleEditActivity> mWeakReference = null;
		MutipleEditActivity mActivity = null;

		public MyHandler(MutipleEditActivity a) {
			mWeakReference = new WeakReference<MutipleEditActivity>(a);
			mActivity = mWeakReference.get();
		}

		public void handleMessage(Message msg) {
			// 关闭本页面
			mActivity.finish();
		}
	}

	private DialogInterface.OnClickListener mDeletePromptListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			boolean isDeleted = false;
			switch (mParent) {
			case Constant.START_FROM_PLAYLIST:
				// 从播放列表移除歌曲，不会删除文件
				isDeleted = PlaylistDAO.removeTrackFromPlaylist(
						getContentResolver(), mPlaylistId,
						mAdapter.getSelectedAudioIds());
				if (isDeleted) {
					// 提示移除成功
					Toast.makeText(MutipleEditActivity.this,
							getResources().getString(R.string.remove_success),
							Toast.LENGTH_SHORT).show();
				}
				break;

			default:
				// 删除指定的歌曲,在存储器上的文件和数据库里的记录都要删除
				PlaylistDAO.removeTrackFromDatabase(getContentResolver(),
						mAdapter.getSelectedAudioIds());
				isDeleted = PlaylistDAO.deleteFiles(mAdapter
						.getSelectedAudioPaths());
				if (isDeleted) {
					// 提示删除成功
					Toast.makeText(MutipleEditActivity.this,
							getResources().getString(R.string.delete_success),
							Toast.LENGTH_SHORT).show();
				}
				break;
			}

			if (!isDeleted) {
				// 删除失败，提示失败信息
				Toast.makeText(MutipleEditActivity.this,
						getResources().getString(R.string.delete_failed),
						Toast.LENGTH_SHORT).show();
			} else {
				mHandler.sendEmptyMessageDelayed(0, mCloseDelayTime);
			}
		}
	};

	/** 与Service连接时交互的类 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "onServiceConnected");

			// 保持对Service的Binder引用，以便调用Service提供给客户端的方法
			mMusicServiceBinder = (MusicPlaybackLocalBinder) service;

		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
			Log.i(TAG, "onServiceDisconnected");

		}
	};
}
