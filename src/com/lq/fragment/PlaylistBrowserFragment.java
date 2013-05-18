package com.lq.fragment;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import com.lq.activity.MainContentActivity;
import com.lq.activity.R;
import com.lq.adapter.PlaylistAdapter;
import com.lq.dao.PlaylistDAO;
import com.lq.entity.PlaylistInfo;
import com.lq.entity.TrackInfo;
import com.lq.fragment.EditTextDialogFragment.OnMyDialogInputListener;
import com.lq.loader.MusicRetrieveLoader;
import com.lq.loader.PlaylistInfoRetrieveLoader;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.util.GlobalConstant;

public class PlaylistBrowserFragment extends Fragment implements
		LoaderCallbacks<List<PlaylistInfo>> {
	private final String TAG = this.getClass().getSimpleName();

	private final int PLAYLIST_RETRIEVE_LOADER = 0;
	private final int TRACK_RETRIEVE_LOADER = 1;
	private final int CONTEXT_MENU_RENAME = 1;
	private final int CONTEXT_MENU_DELETE = 2;
	private final int CONTEXT_MENU_PLAYLATER = 3;
	private final int CONTEXT_MENU_ADD_MEMBERS = 4;

	private ImageView mView_MenuNavigation = null;
	private ImageView mView_MoreFunctions = null;
	private ImageView mView_GoToPlayer = null;
	private TextView mView_Title = null;
	private View mView_CreatePlaylist = null;
	private ListView mView_ListView = null;

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
		mView_MoreFunctions.setVisibility(View.GONE);
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
		getLoaderManager().restartLoader(PLAYLIST_RETRIEVE_LOADER, null,
				PlaylistBrowserFragment.this);
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();
		// 本界面不可见时取消绑定服务
		getActivity().unbindService(mServiceConnection);
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
					args.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_PLAYLIST);
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
	}

	@Override
	public Loader<List<PlaylistInfo>> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");

		return new PlaylistInfoRetrieveLoader(getActivity(), null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<List<PlaylistInfo>> loader,
			List<PlaylistInfo> data) {
		Log.i(TAG, "onLoadFinished");

		// TODO SD卡拔出时，没有处理

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

	private OnMyDialogInputListener mCreateNewPlaylistListener = new OnMyDialogInputListener() {

		@Override
		public void onPlaylistCreateCompleted(String newListName) {
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
		public void onPlaylistCreateCompleted(String newListName) {
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
}
