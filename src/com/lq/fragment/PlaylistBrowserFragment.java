package com.lq.fragment;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.lq.activity.MainContentActivity;
import com.lq.xpressmusic.R;
import com.lq.adapter.PlaylistAdapter;
import com.lq.dao.PlaylistDAO;
import com.lq.entity.PlaylistInfo;
import com.lq.entity.TrackInfo;
import com.lq.fragment.EditTextDialogFragment.OnMyDialogInputListener;
import com.lq.loader.MusicRetrieveLoader;
import com.lq.loader.PlaylistInfoRetrieveLoader;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.util.Constant;
import com.lq.util.StringHelper;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PlaylistBrowserFragment extends Fragment implements
		LoaderCallbacks<List<PlaylistInfo>> {
	private final String TAG = this.getClass().getSimpleName();

	private final String SORT_ORDER = "sort_order";
	private final int SORT_ORDER_CREATED_TIME = 1;
	private final int SORT_ORDER_MODIFIED_TIME = 2;

	private final int PLAYLIST_RETRIEVE_LOADER = 0;
	private final int TRACK_RETRIEVE_LOADER = 1;
	private final int CONTEXT_MENU_RENAME = 1;
	private final int CONTEXT_MENU_DELETE = 2;
	private final int CONTEXT_MENU_PLAYLATER = 3;

	/** 手势检测 */
	private GestureDetector mDetector = null;

	private ImageView mView_MenuNavigation = null;
	private ImageView mView_MoreFunctions = null;
	private ImageView mView_GoToPlayer = null;
	private TextView mView_Title = null;
	private View mView_CreatePlaylist = null;
	private ListView mView_ListView = null;
	private PopupMenu mOverflowPopupMenu = null;

	private PlaylistAdapter mAdapter = null;
	private MainContentActivity mActivity = null;

	private EditTextDialogFragment mEditTextDialogFragment = null;

	private int mSelectedPlaylistId = -1;

	private MusicPlaybackLocalBinder mMusicServiceBinder = null;

	@Override
	public void onAttach(Activity activity) {
		Log.i(TAG, "onAttach");
		super.onAttach(activity);
		if (activity instanceof MainContentActivity) {
			mActivity = (MainContentActivity) activity;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.list_playlist, container,
				false);
		mView_ListView = (ListView) rootView
				.findViewById(R.id.listview_playlist);
		mView_MenuNavigation = (ImageView) rootView
				.findViewById(R.id.menu_navigation);
		mView_Title = (TextView) rootView.findViewById(R.id.title_of_top);
		mView_GoToPlayer = (ImageView) rootView
				.findViewById(R.id.switch_to_player);
		mView_CreatePlaylist = (View) rootView.findViewById(R.id.add_playlist);
		mView_MoreFunctions = (ImageView) rootView
				.findViewById(R.id.more_functions);
		mOverflowPopupMenu = new PopupMenu(getActivity(), mView_MoreFunctions);
		mOverflowPopupMenu.getMenuInflater().inflate(
				R.menu.popup_playlist_list, mOverflowPopupMenu.getMenu());
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		initViewsSetting();

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// sd card 可用
			// 加载数据
			getLoaderManager().initLoader(PLAYLIST_RETRIEVE_LOADER, null, this);
		} else {
			// 当前不可用
			Toast.makeText(getActivity(),
					getResources().getString(R.string.sdcard_cannot_use),
					Toast.LENGTH_SHORT).show();
		}
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
	public void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// sd card 可用
			// 显示操作栏
			mView_MoreFunctions.setClickable(true);
			mView_CreatePlaylist.setVisibility(View.VISIBLE);
			mView_Title.setText("");
			// 初始化一个装载器，根据第一个参数，要么连接一个已存在的装载器，要么以此ID创建一个新的装载器
			getLoaderManager().initLoader(PLAYLIST_RETRIEVE_LOADER, null, this);
		} else {
			// 当前不可用
			// 隐藏操作栏
			mView_MoreFunctions.setClickable(false);
			mView_CreatePlaylist.setVisibility(View.GONE);
			// 提示SD卡不可用
			Toast.makeText(getActivity(), R.string.sdcard_cannot_use,
					Toast.LENGTH_SHORT).show();
		}

		startWatchingExternalStorage();
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();
		// 本界面不可见时取消绑定服务
		getActivity().unbindService(mServiceConnection);
		getActivity().unregisterReceiver(mExternalStorageReceiver);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mActivity = null;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(mAdapter.getItem(info.position).getPlaylistName());
		menu.add(0, CONTEXT_MENU_PLAYLATER, Menu.NONE, R.string.play_later);
		menu.add(0, CONTEXT_MENU_RENAME, Menu.NONE, R.string.rename);
		menu.add(0, CONTEXT_MENU_DELETE, Menu.NONE, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		mSelectedPlaylistId = mAdapter.getItem(menuInfo.position).getId();
		DialogFragment dialogF;
		switch (item.getItemId()) {
		case CONTEXT_MENU_PLAYLATER:
			// 追加选择的歌曲条目到后台的正在播放的列表中
			getLoaderManager().restartLoader(TRACK_RETRIEVE_LOADER, null,
					mTracksLoaderCallbacks);
			break;
		case CONTEXT_MENU_RENAME:
			// 弹出重命名的对话框
			mEditTextDialogFragment = EditTextDialogFragment.newInstance(
					getResources().getString(R.string.rename), mAdapter
							.getItem(menuInfo.position).getPlaylistName(),
					null, mUpdatePlaylistListener);
			mEditTextDialogFragment.show(getFragmentManager(), null);
			break;
		case CONTEXT_MENU_DELETE:
			// 弹出确认删除的提醒对话框
			String title = getResources().getString(
					R.string.are_you_sure_to_delete)
					+ "\""
					+ mAdapter.getItem(menuInfo.position).getPlaylistName()
					+ "\"" + getResources().getString(R.string.question_mark);
			dialogF = PromptDialogFragment.newInstance(title,
					mDeletePromptListener);
			dialogF.show(getFragmentManager(), null);
			break;

		default:
			return false;
		}
		return false;
	}

	private void initViewsSetting() {
		// 设置滑动手势
		mDetector = new GestureDetector(new SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				// 从右向左滑动
				if (e1 != null && e2 != null) {
					if (e1.getX() - e2.getX() > 120) {
						mActivity.switchToPlayer();
						return true;
					}
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
		mView_ListView.setOnTouchListener(gestureListener);

		mAdapter = new PlaylistAdapter(getActivity());
		mView_ListView.setAdapter(mAdapter);
		registerForContextMenu(mView_ListView);
		mView_ListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO列表点击，进入音乐列表
				if (getParentFragment() instanceof FramePlaylistFragment) {
					Bundle args = new Bundle();
					args.putInt(Constant.PARENT, Constant.START_FROM_PLAYLIST);
					args.putParcelable(PlaylistInfo.class.getSimpleName(),
							mAdapter.getItem(position));
					getFragmentManager()
							.beginTransaction()
							.replace(
									R.id.frame_for_nested_fragment,
									Fragment.instantiate(getActivity(),
											TrackBrowserFragment.class
													.getName(), args))
							.addToBackStack(null).commit();
				}
			}
		});

		mView_Title.setText(R.string.local_playlist);

		mView_GoToPlayer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.switchToPlayer();
			}
		});

		mView_MenuNavigation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.getSlidingMenu().showMenu();
			}
		});

		mView_CreatePlaylist.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Resources r = getResources();
				mEditTextDialogFragment = EditTextDialogFragment.newInstance(
						r.getString(R.string.create_playlist), null,
						r.getString(R.string.input_playlist_name),
						mCreateNewPlaylistListener);
				mEditTextDialogFragment.show(getFragmentManager(),
						"createNewPlaylist");
			}
		});

		mView_MoreFunctions.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mOverflowPopupMenu.show();
			}
		});

		mOverflowPopupMenu
				.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						Bundle args;
						switch (item.getItemId()) {
						case R.id.sort_by_playlist_name:
							Collections.sort(mAdapter.getData(),
									mPlaylistNameComparator);
							mAdapter.notifyDataSetChanged();
							break;
						case R.id.sort_by_playlist_music_count:
							Collections.sort(mAdapter.getData(),
									mPlaylistSongCountComparator);
							mAdapter.notifyDataSetChanged();
							break;
						case R.id.sort_by_playlist_created_time:
							args = new Bundle();
							args.putInt(SORT_ORDER, SORT_ORDER_CREATED_TIME);
							getLoaderManager().restartLoader(
									PLAYLIST_RETRIEVE_LOADER, args,
									PlaylistBrowserFragment.this);
							break;
						case R.id.sort_by_playlist_modified_time:
							args = new Bundle();
							args.putInt(SORT_ORDER, SORT_ORDER_MODIFIED_TIME);
							getLoaderManager().restartLoader(
									PLAYLIST_RETRIEVE_LOADER, args,
									PlaylistBrowserFragment.this);
							break;
						default:
							break;
						}
						return true;
					}
				});
	}

	@Override
	public Loader<List<PlaylistInfo>> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");
		String sortOrder = null;
		if (args != null) {
			switch (args.getInt(SORT_ORDER, -1)) {
			case SORT_ORDER_CREATED_TIME:
				sortOrder = Playlists.DATE_ADDED;
				break;
			case SORT_ORDER_MODIFIED_TIME:
				sortOrder = Playlists.DATE_MODIFIED;
				break;
			default:
				break;
			}
		}
		return new PlaylistInfoRetrieveLoader(getActivity(), null, null,
				sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<List<PlaylistInfo>> loader,
			List<PlaylistInfo> data) {
		Log.i(TAG, "onLoadFinished");

		// 载入完成，更新列表数据
		mAdapter.setData(data);

		// 在标题栏上显示艺术家数目
		if (data != null && data.size() != 0) {
			mView_Title.setText(getResources().getString(
					R.string.local_playlist)
					+ "(" + data.size() + ")");
		}
	}

	@Override
	public void onLoaderReset(Loader<List<PlaylistInfo>> loader) {
		Log.i(TAG, "onLoaderReset");
		mAdapter.setData(null);
	}

	private void startWatchingExternalStorage() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
		intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.setPriority(1000);
		intentFilter.addDataScheme("file");
		getActivity().registerReceiver(mExternalStorageReceiver, intentFilter);
	}

	private BroadcastReceiver mExternalStorageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)
					|| intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)
					|| intent.getAction().equals(
							Intent.ACTION_MEDIA_BAD_REMOVAL)) {
				// SD卡移除，设置列表为空
				mView_MoreFunctions.setClickable(false);
				mView_CreatePlaylist.setVisibility(View.GONE);
				mView_Title.setText("");
				mAdapter.setData(null);
				// 提示SD卡不可用
				Toast.makeText(getActivity(), R.string.sdcard_cannot_use,
						Toast.LENGTH_SHORT).show();
			} else if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
				// SD卡正常挂载,重新加载数据
				mView_MoreFunctions.setClickable(true);
				mView_CreatePlaylist.setVisibility(View.VISIBLE);
				getLoaderManager().restartLoader(PLAYLIST_RETRIEVE_LOADER,
						null, PlaylistBrowserFragment.this);
			}

		}
	};

	private OnMyDialogInputListener mCreateNewPlaylistListener = new OnMyDialogInputListener() {

		@Override
		public void onEditTextInputCompleted(String newListName) {
			int newListId = PlaylistDAO.createPlaylist(getActivity()
					.getContentResolver(), newListName);
			if (newListId == -1) {
				// 有重名播放列表，提示列表名已经存在
				Toast.makeText(
						getActivity(),
						getActivity().getString(
								R.string.playlist_has_already_existed),
						Toast.LENGTH_SHORT).show();
				mEditTextDialogFragment.setDialogStayShown();
			} else {
				// 新建后重新读取播放列表数据库
				getLoaderManager().restartLoader(PLAYLIST_RETRIEVE_LOADER,
						null, PlaylistBrowserFragment.this);
				mEditTextDialogFragment.setDialogDismiss();
			}
		}
	};

	private OnMyDialogInputListener mUpdatePlaylistListener = new OnMyDialogInputListener() {

		@Override
		public void onEditTextInputCompleted(String newListName) {
			// 尝试更新播放列表名称
			boolean isRename = PlaylistDAO.updatePlaylistName(getActivity()
					.getContentResolver(), newListName, mSelectedPlaylistId);
			if (isRename) {
				// 有重名播放列表，提示列表名已经存在
				Toast.makeText(
						getActivity(),
						getActivity().getString(
								R.string.playlist_has_already_existed),
						Toast.LENGTH_SHORT).show();
				mEditTextDialogFragment.setDialogStayShown();
			} else {
				// 新建后重新读取播放列表数据库
				getLoaderManager().restartLoader(PLAYLIST_RETRIEVE_LOADER,
						null, PlaylistBrowserFragment.this);
				mEditTextDialogFragment.setDialogDismiss();
			}
		}
	};
	private DialogInterface.OnClickListener mDeletePromptListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// 删除指定的播放列表
			PlaylistDAO.deletePlaylist(getActivity().getContentResolver(),
					mSelectedPlaylistId);

			// 重新读取数据库，更新列表显示
			getLoaderManager().restartLoader(PLAYLIST_RETRIEVE_LOADER, null,
					PlaylistBrowserFragment.this);

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

	private LoaderManager.LoaderCallbacks<List<TrackInfo>> mTracksLoaderCallbacks = new LoaderCallbacks<List<TrackInfo>>() {
		@Override
		public Loader<List<TrackInfo>> onCreateLoader(int id, Bundle args) {
			Log.i(TAG, "onCreateLoader");

			String sortOrder = Media.TITLE_KEY;

			// 查询语句：检索出.mp3为后缀名，时长大于1分钟，文件大小大于1MB的媒体文件
			StringBuffer where = new StringBuffer("(" + Media.DATA
					+ " like'%.mp3' or " + Media.DATA + " like'%.wma') and "
					+ Media.DURATION + " > " + 1000 * 60 * 1 + " and "
					+ Media.SIZE + " > " + 1024);

			where.append(" and " + Media._ID + " in (select "
					+ Playlists.Members.AUDIO_ID
					+ " from audio_playlists_map where "
					+ Playlists.Members.PLAYLIST_ID + "=" + mSelectedPlaylistId
					+ ")");

			return new MusicRetrieveLoader(getActivity(), where.toString(),
					null, sortOrder);
		}

		@Override
		public void onLoaderReset(Loader<List<TrackInfo>> loader) {
			Log.i(TAG, "onLoaderReset");

		}

		@Override
		public void onLoadFinished(Loader<List<TrackInfo>> loader,
				List<TrackInfo> data) {
			Log.i(TAG, "onLoadFinished");
			if (mMusicServiceBinder != null) {
				// 数据载入完毕，追加到当前播放列表后
				mMusicServiceBinder.appendToCurrentPlayList(data);
			}
		}
	};

	// 按歌曲数量倒序排序
	private Comparator<PlaylistInfo> mPlaylistSongCountComparator = new Comparator<PlaylistInfo>() {
		@Override
		public int compare(PlaylistInfo lhs, PlaylistInfo rhs) {
			if (lhs.getNumOfMembers() > rhs.getNumOfMembers()) {
				return -1;
			} else if (lhs.getNumOfMembers() < rhs.getNumOfMembers()) {
				return 1;
			} else {
				return 0;
			}
		}
	};

	// 按播放列表名称顺序排序
	private Comparator<PlaylistInfo> mPlaylistNameComparator = new Comparator<PlaylistInfo>() {
		char first_l, first_r;

		@Override
		public int compare(PlaylistInfo lhs, PlaylistInfo rhs) {
			first_l = lhs.getPlaylistName().charAt(0);
			first_r = rhs.getPlaylistName().charAt(0);
			if (StringHelper.checkType(first_l) == StringHelper.CharType.CHINESE) {
				first_l = StringHelper.getPinyinFirstLetter(first_l);
			}
			if (StringHelper.checkType(first_r) == StringHelper.CharType.CHINESE) {
				first_r = StringHelper.getPinyinFirstLetter(first_r);
			}
			if (first_l > first_r) {
				return 1;
			} else if (first_l < first_r) {
				return -1;
			} else {
				return 0;
			}
		}
	};
}
