package com.lq.fragment;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.lq.activity.MutipleEditActivity;
import com.lq.activity.R;
import com.lq.adapter.TrackAdapter;
import com.lq.dao.PlaylistDAO;
import com.lq.entity.ArtistInfo;
import com.lq.entity.FolderInfo;
import com.lq.entity.PlaylistInfo;
import com.lq.entity.TrackInfo;
import com.lq.listener.OnPlaybackStateChangeListener;
import com.lq.loader.MusicRetrieveLoader;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.util.GlobalConstant;

/**
 * 读取并显示设备外存上的音乐文件
 * 
 * @author lq
 * */
public class TrackBrowserFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<List<TrackInfo>>, OnItemClickListener {
	// 调试用的标记
	private final String TAG = this.getClass().getSimpleName();

	private static final int MUSIC_RETRIEVE_LOADER = 0;
	private final int CONTEXT_MENU_ADD_TO_PLAYLIST = 1;
	private final int CONTEXT_MENU_CHECK_DETAIL = 2;
	private final int CONTEXT_MENU_DELETE = 3;

	private String mSortOrder = Media.DEFAULT_SORT_ORDER;

	private Bundle mCurrentPlayInfo = null;

	private boolean mHasNewData = false;

	private MainContentActivity mActivity = null;

	/** 显示本地音乐的列表 */
	private ListView mView_ListView = null;

	private ImageView mView_MenuNavigation = null;
	private ImageView mView_GoToPlayer = null;
	private ImageView mView_MoreFunctions = null;
	private TextView mView_Title = null;
	private View mView_PlayAll = null;
	private View mView_Search = null;
	private View mView_MutipleChoose = null;
	private PopupMenu mOverflowPopupMenu = null;

	/** 用来绑定数据至ListView的适配器 */
	private TrackAdapter mAdapter = null;

	private ArtistInfo mArtistInfo = null;
	private FolderInfo mFolderInfo = null;
	private PlaylistInfo mPlaylistInfo = null;

	private TrackInfo mToDeleteTrack = null;

	private MusicPlaybackLocalBinder mMusicServiceBinder = null;

	/** 与Service连接时交互的类 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "onServiceConnected");
			mMusicServiceBinder = (MusicPlaybackLocalBinder) service;
			mMusicServiceBinder
					.registerOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
			mCurrentPlayInfo = mMusicServiceBinder.getCurrentPlayInfo();
		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
		}
	};

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

	/** 在此加载一个ListView，可以使用自定义的ListView */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.list_track, container, false);
		mView_ListView = (ListView) rootView
				.findViewById(R.id.listview_local_music);
		mView_MenuNavigation = (ImageView) rootView
				.findViewById(R.id.menu_navigation);
		mView_Title = (TextView) rootView.findViewById(R.id.title_of_top);
		mView_PlayAll = (View) rootView.findViewById(R.id.btn_play_all);
		mView_Search = (View) rootView.findViewById(R.id.btn_search);
		mView_MutipleChoose = (View) rootView
				.findViewById(R.id.btn_mutiple_choose);
		mView_MoreFunctions = (ImageView) rootView
				.findViewById(R.id.more_functions);
		mView_GoToPlayer = (ImageView) rootView
				.findViewById(R.id.switch_to_player);
		mOverflowPopupMenu = new PopupMenu(getActivity(), mView_MoreFunctions);
		Bundle args = getArguments();
		if (args != null) {
			switch (args.getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_LOCAL_MUSIC:
				mOverflowPopupMenu.getMenuInflater().inflate(
						R.menu.popup_local_music_list,
						mOverflowPopupMenu.getMenu());
				break;
			default:
				mOverflowPopupMenu.getMenuInflater().inflate(
						R.menu.popup_track_list, mOverflowPopupMenu.getMenu());
				break;
			}
		}
		return rootView;
	}

	/** 延迟ListView的设置到Activity创建时，为ListView绑定数据适配器 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);

		initViewsSetting();

		handleArguments();

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// sd card 可用
			// 初始化一个装载器，根据第一个参数，要么连接一个已存在的装载器，要么以此ID创建一个新的装载器
			getLoaderManager().initLoader(MUSIC_RETRIEVE_LOADER, null, this);
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
		// 在Fragment可见时绑定服务 ，以使服务可以发送消息过来
		getActivity().bindService(
				new Intent(getActivity(), MusicService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();

		// Fragment不可见时，无需更新UI，取消服务绑定
		mActivity.unbindService(mServiceConnection);
	}

	@Override
	public void onDetach() {
		Log.i(TAG, "onDetach");
		super.onDetach();
		mActivity = null;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
	}

	/** 初始化各个视图组件的设置 */
	private void initViewsSetting() {
		// ListView的设置-------------------------------------------------------------
		// 创建一个空的适配器，用来显示加载的数据，适配器内容稍后由Loader填充
		mAdapter = new TrackAdapter(getActivity());
		// 为ListView绑定数据适配器
		mView_ListView.setAdapter(mAdapter);
		registerForContextMenu(mView_ListView);
		// 为ListView的条目绑定一个点击事件监听
		mView_ListView.setOnItemClickListener(this);

		// 标题的设置-------------------------------------------------------------
		mView_Title.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 标题作为回退导航
				getFragmentManager().popBackStackImmediate();
			}
		});
		// 默认不可点击
		mView_Title.setClickable(false);

		// 跳转至播放界面-----------------------------------------------
		mView_GoToPlayer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mActivity.switchToPlayer();
			}
		});

		// 顶部弹出菜单----------------------------------------------------
		mOverflowPopupMenu
				.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.sort_by_music_name:
							mSortOrder = Media.TITLE_KEY;
							getLoaderManager().restartLoader(
									MUSIC_RETRIEVE_LOADER, null,
									TrackBrowserFragment.this);
							break;
						case R.id.sort_by_last_modify_time:
							mSortOrder = Media.DATE_MODIFIED;
							getLoaderManager().restartLoader(
									MUSIC_RETRIEVE_LOADER, null,
									TrackBrowserFragment.this);
							break;
						case R.id.classify_by_artist:
							if (null != getParentFragment()
									&& getParentFragment() instanceof FrameLocalMusicFragment) {
								getFragmentManager()
										.beginTransaction()
										.replace(
												R.id.frame_for_nested_fragment,
												Fragment.instantiate(
														getActivity(),
														ArtistBrowserFragment.class
																.getName(),
														null))
										.addToBackStack(null).commit();
							}
							break;
						case R.id.classify_by_folder:
							if (null != getParentFragment()
									&& getParentFragment() instanceof FrameLocalMusicFragment) {
								getFragmentManager()
										.beginTransaction()
										.replace(
												R.id.frame_for_nested_fragment,
												Fragment.instantiate(
														getActivity(),
														FolderBrowserFragment.class
																.getName(),
														null))
										.addToBackStack(null).commit();
							}
							break;
						default:
							break;
						}
						return true;
					}
				});

		mView_MoreFunctions.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mOverflowPopupMenu.show();
			}
		});

		// 侧滑菜单弹出按钮------------------------------------------------
		mView_MenuNavigation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.getSlidingMenu().showMenu();
			}
		});

		// 多选按钮-----------------------------------------------------------
		mView_MutipleChoose.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO 进入多选

				Intent intent = new Intent(getActivity(),
						MutipleEditActivity.class);
				Bundle data = new Bundle();

				// 传递参数给多选界面
				switch (getArguments().getInt(GlobalConstant.PARENT)) {
				case GlobalConstant.START_FROM_LOCAL_MUSIC:
					data.putString(GlobalConstant.TITLE, getResources()
							.getString(R.string.local_music));
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_LOCAL_MUSIC);
					break;
				case GlobalConstant.START_FROM_ARTIST:
					data.putString(GlobalConstant.TITLE,
							mArtistInfo.getArtistName());
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_ARTIST);
					break;
				case GlobalConstant.START_FROM_FOLER:
					data.putString(GlobalConstant.TITLE,
							mFolderInfo.getFolderName());
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_FOLER);
					break;
				case GlobalConstant.START_FROM_PLAYLIST:
					data.putString(GlobalConstant.TITLE,
							mPlaylistInfo.getPlaylistName());
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_PLAYLIST);
					data.putInt(GlobalConstant.PLAYLIST_ID,
							mPlaylistInfo.getId());
					break;
				default:
					break;
				}
				data.putInt(GlobalConstant.FIRST_VISIBLE_POSITION,
						mView_ListView.getFirstVisiblePosition());
				data.putParcelableArrayList(GlobalConstant.DATA_LIST,
						mAdapter.getData());
				intent.putExtras(data);
				startActivity(intent);
			}
		});

		// 搜索按钮-------------------------------------------------------------
		mView_Search.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO 搜索歌曲

			}
		});

		// 全部播放按钮----------------------------------------------------------
		mView_PlayAll.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO 全部播放
				if (mHasNewData && mMusicServiceBinder != null) {
					mMusicServiceBinder.setCurrentPlayList(mAdapter.getData());
				}
				mHasNewData = false;
				Intent intent = new Intent(MusicService.ACTION_PLAY);
				mActivity.startService(intent);
				mActivity.switchToPlayer();
			}
		});
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mHasNewData && mMusicServiceBinder != null) {
			mMusicServiceBinder.setCurrentPlayList(mAdapter.getData());
		}
		mHasNewData = false;
		Intent intent = new Intent(MusicService.ACTION_PLAY);
		intent.putExtra(GlobalConstant.REQUEST_PLAY_ID,
				mAdapter.getItem(position).getId());
		intent.putExtra(GlobalConstant.CLICK_ITEM_IN_LIST, 1);
		mActivity.startService(intent);
		mActivity.switchToPlayer();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		final AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(mAdapter.getItem(mInfo.position).getTitle());
		menu.add(0, CONTEXT_MENU_ADD_TO_PLAYLIST, Menu.NONE,
				R.string.add_to_playlist);
		menu.add(0, CONTEXT_MENU_CHECK_DETAIL, Menu.NONE, R.string.check_detail);

		switch (getArguments().getInt(GlobalConstant.PARENT)) {
		case GlobalConstant.START_FROM_PLAYLIST:
			menu.add(0, CONTEXT_MENU_DELETE, Menu.NONE, R.string.remove);
			break;
		default:
			menu.add(0, CONTEXT_MENU_DELETE, Menu.NONE, R.string.delete);
			break;
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		DialogFragment df = null;
		final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Log.i(TAG, "menuInfo:" + menuInfo);
		switch (item.getItemId()) {
		case CONTEXT_MENU_ADD_TO_PLAYLIST:
			// 弹出选择播放列表的窗口
			df = SelectPlaylistDialogFragment.newInstance(new long[] { mAdapter
					.getItem(menuInfo.position).getId() });
			df.show(getFragmentManager(), null);
			break;
		case CONTEXT_MENU_CHECK_DETAIL:
			// 弹出歌曲详细信息的窗口
			df = TrackDetailDialogFragment.newInstance(mAdapter
					.getItem(menuInfo.position));
			df.show(getFragmentManager(), null);
			break;
		case CONTEXT_MENU_DELETE:
			// 弹出确认删除的提示窗口
			mToDeleteTrack = mAdapter.getItem(menuInfo.position);
			switch (getArguments().getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_PLAYLIST:
				df = PromptDialogFragment.newInstance(
						getResources().getString(
								R.string.confirm_remove_song_from_playlist),
						mDeletePromptListener);
				df.show(getFragmentManager(), null);
				break;
			default:
				df = PromptDialogFragment.newInstance(
						getResources().getString(
								R.string.confirm_delete_song_file),
						mDeletePromptListener);
				df.show(getFragmentManager(), null);
				break;
			}
			break;

		default:
			return false;
		}
		return true;
	}

	/** 在装载器需要被创建时执行此方法，这里只有一个装载器，所以我们不必关心装载器的ID */
	@Override
	public Loader<List<TrackInfo>> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");

		// 查询语句：检索出.mp3为后缀名，时长大于1分钟，文件大小大于1MB的媒体文件
		StringBuffer select = new StringBuffer("(" + Media.DATA
				+ " like'%.mp3' or " + Media.DATA + " like'%.wma') and "
				+ Media.DURATION + " > " + 1000 * 60 * 1 + " and " + Media.SIZE
				+ " > " + 1024);

		if (mArtistInfo != null) {
			select.append(" and " + Media.ARTIST + " = '"
					+ mArtistInfo.getArtistName() + "'");
		} else if (mFolderInfo != null) {
			select.append(" and " + Media.DATA + " like '"
					+ mFolderInfo.getFolderPath() + File.separator + "%'");
		} else if (mPlaylistInfo != null) {
			select.append(" and " + Media._ID + " in (select "
					+ Playlists.Members.AUDIO_ID
					+ " from audio_playlists_map where "
					+ Playlists.Members.PLAYLIST_ID + "="
					+ mPlaylistInfo.getId() + ")");
		}

		MusicRetrieveLoader loader = new MusicRetrieveLoader(getActivity(),
				select.toString(), null, mSortOrder);

		if (mFolderInfo != null) {
			loader.setFolderFilterPattern(mFolderInfo.getFolderPath());
		}

		// 创建并返回一个Loader
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<List<TrackInfo>> loader,
			List<TrackInfo> data) {
		Log.i(TAG, "onLoadFinished");
		mHasNewData = true;

		// TODO SD卡拔出时，没有处理
		mAdapter.setData(data);

		// 每次加载新的数据设置一下标题中的歌曲数目
		if (getArguments() != null) {
			switch (getArguments().getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_LOCAL_MUSIC:
				mView_Title.setText(getResources().getString(
						R.string.local_music)
						+ "(" + data.size() + ")");
				break;
			case GlobalConstant.START_FROM_ARTIST:
				mView_Title.setText(mArtistInfo.getArtistName() + "("
						+ data.size() + ")");
				break;
			case GlobalConstant.START_FROM_FOLER:
				mView_Title.setText(mFolderInfo.getFolderName() + "("
						+ data.size() + ")");
				break;
			case GlobalConstant.START_FROM_PLAYLIST:
				mView_Title.setText(mPlaylistInfo.getPlaylistName() + "("
						+ data.size() + ")");
				break;
			default:
				break;
			}
		}

		if (mCurrentPlayInfo != null) {
			initCurrentPlayInfo(mCurrentPlayInfo);
		}
	}

	/** 此方法在提供给onLoadFinished()最后的一个游标准备关闭时调用，我们要确保不再使用它 */
	@Override
	public void onLoaderReset(Loader<List<TrackInfo>> loader) {
		Log.i(TAG, "onLoaderReset");
		mAdapter.setData(null);
	}

	/** 初始化当前播放信息 */
	private void initCurrentPlayInfo(Bundle bundle) {
		TrackInfo playingSong = bundle
				.getParcelable(GlobalConstant.PLAYING_MUSIC_ITEM);

		if (playingSong != null) {
			mAdapter.setSpecifiedIndicator(MusicService.seekPosInListById(
					mAdapter.getData(), playingSong.getId()));
		} else {
			mAdapter.setSpecifiedIndicator(-1);
		}

	}

	/** 处理从启动处传递过来的参数 */
	private void handleArguments() {
		// 如果有谁传递数据过来了，就设置一下
		Bundle args = getArguments();
		if (args != null) {
			switch (args.getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_ARTIST:
				// 如果是从歌手列表里启动的
				mArtistInfo = args.getParcelable(ArtistInfo.class
						.getSimpleName());
				if (mArtistInfo != null) {
					// 更新标题
					if (!mArtistInfo.getArtistName().equals("<unknown>")) {
						mView_Title.setText(mArtistInfo.getArtistName() + "("
								+ mArtistInfo.getNumberOfTracks() + ")");
					} else {
						mView_Title.setText(getResources().getString(
								R.string.unknown_artist)
								+ "(" + mArtistInfo.getNumberOfTracks() + ")");
					}
					setTitleLeftDrawable();
				}
				break;
			case GlobalConstant.START_FROM_FOLER:
				// 如果是从文件夹列表里启动的
				mFolderInfo = args.getParcelable(FolderInfo.class
						.getSimpleName());
				if (mFolderInfo != null) {
					// 更新标题
					mView_Title.setText(mFolderInfo.getFolderName() + "("
							+ mFolderInfo.getNumOfTracks() + ")");
					setTitleLeftDrawable();
				}
				break;
			case GlobalConstant.START_FROM_PLAYLIST:
				// 如果是从播放列表里启动的
				mPlaylistInfo = args.getParcelable(PlaylistInfo.class
						.getSimpleName());
				if (mPlaylistInfo != null) {
					// 更新标题
					mView_Title.setText(mPlaylistInfo.getPlaylistName() + "("
							+ mPlaylistInfo.getNumOfMembers() + ")");
					setTitleLeftDrawable();
				}
				break;

			default:
				break;
			}
		}
	}

	private void setTitleLeftDrawable() {
		mView_Title.setClickable(true);
		Drawable title_drawable = getResources().getDrawable(
				R.drawable.btn_titile_back);
		title_drawable.setBounds(0, 0, title_drawable.getIntrinsicWidth(),
				title_drawable.getIntrinsicHeight());
		mView_Title.setCompoundDrawables(title_drawable, null, null, null);
		mView_Title.setBackgroundResource(R.drawable.button_backround_light);
	}

	private DialogInterface.OnClickListener mDeletePromptListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			boolean isDeleted = false;
			switch (getArguments().getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_PLAYLIST:
				// 从播放列表移除歌曲，不会删除文件
				isDeleted = PlaylistDAO.removeTrackFromPlaylist(getActivity()
						.getContentResolver(), mPlaylistInfo.getId(),
						new long[] { mToDeleteTrack.getId() });
				if (isDeleted) {
					// 提示删除成功
					Toast.makeText(getActivity(),
							getResources().getString(R.string.remove_success),
							Toast.LENGTH_SHORT).show();
				}
				break;

			default:
				// 删除的歌曲现在是否正在播放
				TrackInfo curTrack = mMusicServiceBinder.getCurrentPlayInfo()
						.getParcelable(GlobalConstant.PLAYING_MUSIC_ITEM);

				if (curTrack != null) {
					if (curTrack.getId() == mToDeleteTrack.getId()) {
						// 要删除的歌曲正在播放，则先播放下一首歌,如果没有下一首就停止播放
						mMusicServiceBinder
								.removeSongFromCurrenPlaylist(mToDeleteTrack
										.getId());
					}
				}

				// 删除指定的歌曲,在存储器上的文件和数据库里的记录都要删除
				PlaylistDAO.removeTrackFromDatabase(getActivity()
						.getContentResolver(), new long[] { mToDeleteTrack
						.getId() });
				isDeleted = PlaylistDAO.deleteFile(mToDeleteTrack.getData());
				if (isDeleted) {
					// 提示删除成功
					Toast.makeText(getActivity(),
							getResources().getString(R.string.delete_success),
							Toast.LENGTH_SHORT).show();
				}
				break;
			}

			if (!isDeleted) {
				// 删除失败，提示失败信息
				Toast.makeText(getActivity(),
						getResources().getString(R.string.delete_failed),
						Toast.LENGTH_SHORT).show();
			} else {
				// 重新读取数据库，更新列表显示
				getLoaderManager().restartLoader(MUSIC_RETRIEVE_LOADER, null,
						TrackBrowserFragment.this);
			}
		}
	};

	private OnPlaybackStateChangeListener mOnPlaybackStateChangeListener = new OnPlaybackStateChangeListener() {

		@Override
		public void onMusicPlayed() {

		}

		@Override
		public void onMusicPaused() {

		}

		@Override
		public void onMusicStopped() {

		}

		@Override
		public void onPlayNewSong(TrackInfo playingSong) {
			mAdapter.setSpecifiedIndicator(MusicService.seekPosInListById(
					mAdapter.getData(), playingSong.getId()));
		}

		@Override
		public void onPlayModeChanged(int playMode) {

		}

		@Override
		public void onPlayProgressUpdate(int currentMillis) {

		}

	};
}
