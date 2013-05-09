package com.lq.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
import android.provider.MediaStore.Audio.Media;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.lq.activity.MainContentActivity;
import com.lq.activity.R;
import com.lq.entity.MusicItem;
import com.lq.listener.OnPlaybackStateChangeListener;
import com.lq.loader.MusicRetrieveLoader;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;

/**
 * 读取并显示设备外存上的音乐文件
 * 
 * @author lq
 * */
public class LocalMusicFragment extends SherlockFragment implements
		LoaderManager.LoaderCallbacks<List<MusicItem>>, OnItemClickListener {
	// 调试用的标记
	private static final String TAG = LocalMusicFragment.class.getSimpleName();

	public static final String FLAG_CLICK_ITEM_IN_LIST = "flag_click_item_in_list";
	public static final String REQUEST_PLAY_ID = "request_play_id";

	private static final int MUSIC_RETRIEVE_LOADER = 0;

	private String mSortOrder = Media.DEFAULT_SORT_ORDER;

	private boolean mHasNewData = false;

	private MainContentActivity mActivity = null;

	/** 显示本地音乐的列表 */
	private ListView mView_ListView = null;

	/** 数据加载完成前显示的进度条 */
	private ProgressBar mView_ProgressBar = null;

	private ImageView mView_MenuNavigation = null;
	private ImageView mView_MoreFunctions = null;
	private TextView mView_Title = null;
	private ViewGroup mView_Top_Container = null;

	private PopupMenu mOverflowPopupMenu = null;

	/** 用来绑定数据至ListView的适配器 */
	private MusicListAdapter mAdapter = null;

	private ClientIncomingHandler mHandler = new ClientIncomingHandler(
			LocalMusicFragment.this);

	private MusicPlaybackLocalBinder mMusicServiceBinder = null;

	/** 处理来自服务端的消息 */
	private static class ClientIncomingHandler extends Handler {
		// 使用弱引用，避免Handler造成的内存泄露(Message持有Handler的引用，内部定义的Handler类持有外部类的引用)
		WeakReference<LocalMusicFragment> mFragmentWeakReference = null;
		LocalMusicFragment mFragment = null;

		public ClientIncomingHandler(LocalMusicFragment fragment) {
			mFragmentWeakReference = new WeakReference<LocalMusicFragment>(
					fragment);
			mFragment = mFragmentWeakReference.get();
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
			mMusicServiceBinder = (MusicPlaybackLocalBinder) service;
			mMusicServiceBinder
					.registerOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
		}
	};

	@Override
	public void onAttach(Activity activity) {
		Log.i(TAG, "onAttach");
		super.onAttach(activity);
		mActivity = (MainContentActivity) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);

		// Activity重建的时候（比如屏幕方向改变）保证这个Fragment实例不被销
		// onCreate()和onDestroyed()不会被调用
		setRetainInstance(true);
	}

	/** 在此加载一个ListView，可以使用自定义的ListView */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.layout_local_music_list,
				container, false);
		mView_ListView = (ListView) rootView
				.findViewById(R.id.listview_local_music);
		mView_ProgressBar = (ProgressBar) rootView
				.findViewById(R.id.progressbar_loading_local_music);
		mView_MenuNavigation = (ImageView) rootView
				.findViewById(R.id.menu_navigation);
		mView_Title = (TextView) rootView
				.findViewById(R.id.title_of_local_music);
		mView_MoreFunctions = (ImageView) rootView
				.findViewById(R.id.more_functions);
		mView_Top_Container = (ViewGroup) rootView
				.findViewById(R.id.top_of_local_music);

		mOverflowPopupMenu = new PopupMenu(getActivity(), mView_MoreFunctions);
		mOverflowPopupMenu.getMenuInflater().inflate(
				R.menu.popup_local_music_list, mOverflowPopupMenu.getMenu());

		return rootView;
	}

	/** 延迟ListView的设置到Activity创建时，为ListView绑定数据适配器 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);

		initViewsSetting();

		// 数据加载完成之前，显示一个进度条，隐藏列表，完成加载后显示列表隐藏进度条
		setListShown(false, true);
		// 初始化一个装载器，根据第一个参数，要么连接一个已存在的装载器，要么以此ID创建一个新的装载器
		getLoaderManager().initLoader(MUSIC_RETRIEVE_LOADER, null, this);

	}

	@Override
	public void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();

		// 在Fragment可见时绑定服务，传递信使给服务，以使服务可以发送消息过来
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
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/**
	 * 控制是否要显示列表。不显示列表时，可以显示一个进度条以表明正在加载数据
	 * 
	 * @see {@link android.app.ListFragment#setListShown()}
	 * @param shown
	 *            是否显示ListView
	 * @param animate
	 *            进度条和列表切换时是否带动画
	 * */
	private void setListShown(boolean shown, boolean animate) {
		if (shown) {
			if (animate) {
				mView_ProgressBar.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_out));
				mView_ListView.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_in));
			} else {
				mView_ProgressBar.clearAnimation();
				mView_ListView.clearAnimation();
			}
			mView_ProgressBar.setVisibility(View.GONE);
			mView_ListView.setVisibility(View.VISIBLE);
		} else {
			if (animate) {
				mView_ProgressBar.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_in));
				mView_ListView.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_out));
			} else {
				mView_ProgressBar.clearAnimation();
				mView_ListView.clearAnimation();
			}
			mView_ProgressBar.setVisibility(View.VISIBLE);
			mView_ListView.setVisibility(View.GONE);
		}
	}

	private void initViewsSetting() {
		// 创建一个空的适配器，用来显示加载的数据，适配器内容稍后由Loader填充
		mAdapter = new MusicListAdapter();
		// 为ListView绑定数据适配器
		mView_ListView.setAdapter(mAdapter);
		// 为ListView的条目绑定一个点击事件监听
		mView_ListView.setOnItemClickListener(this);
		mView_ListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		mView_ListView
				.setMultiChoiceModeListener(new MultiChoiceModeListener() {

					@Override
					public boolean onCreateActionMode(ActionMode mode, Menu menu) {
						mView_Top_Container.setVisibility(View.GONE);
						mActivity.forbidSlide();
						MenuInflater inflater = mActivity.getMenuInflater();
						inflater.inflate(R.menu.main_content, menu);
						mode.setTitle("Select Items");
						return true;
					}

					@Override
					public boolean onPrepareActionMode(ActionMode mode,
							Menu menu) {
						return true;
					}

					@Override
					public boolean onActionItemClicked(ActionMode mode,
							MenuItem item) {
						switch (item.getItemId()) {
						case R.id.go_to_play:
							Toast.makeText(mActivity, "Clicked Action Item",
									Toast.LENGTH_SHORT).show();
							mode.finish();
							break;
						}
						return true;
					}

					@Override
					public void onDestroyActionMode(ActionMode mode) {
						mView_Top_Container.setVisibility(View.VISIBLE);
						mActivity.allowSlide();
					}

					@Override
					public void onItemCheckedStateChanged(ActionMode mode,
							int position, long id, boolean checked) {
						// TODO Auto-generated method stub

					}

				});
		mOverflowPopupMenu
				.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.sort_by_artist:
							mSortOrder = Media.ARTIST;
							getLoaderManager().restartLoader(
									MUSIC_RETRIEVE_LOADER, null,
									LocalMusicFragment.this);
							break;
						case R.id.sort_by_music_name:
							mSortOrder = Media.DEFAULT_SORT_ORDER;
							getLoaderManager().restartLoader(
									MUSIC_RETRIEVE_LOADER, null,
									LocalMusicFragment.this);
							break;

						default:
							break;
						}
						return true;
					}
				});

		mView_MenuNavigation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.getSlidingMenu().showMenu();
			}
		});

		mView_MoreFunctions.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mOverflowPopupMenu.show();
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
		intent.putExtra(REQUEST_PLAY_ID, mAdapter.getItem(position).getId());
		intent.putExtra(FLAG_CLICK_ITEM_IN_LIST, 1);
		mActivity.startService(intent);
		mActivity.switchToMusicPlayer();
	}

	/** 在装载器需要被创建时执行此方法，这里只有一个装载器，所以我们不必关心装载器的ID */
	@Override
	public Loader<List<MusicItem>> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");

		// 查询语句：检索出.mp3为后缀名，时长大于两分钟，文件大小大于1MB的媒体文件
		String select = Media.DATA + " like'%.mp3' and " + Media.DURATION
				+ " > " + 1000 * 60 * 2 + " and " + Media.SIZE + " > " + 1024;

		// 创建并返回一个Loader
		return new MusicRetrieveLoader(getActivity(), select, null, mSortOrder);
	}

	@Override
	public void onLoadFinished(Loader<List<MusicItem>> loader,
			List<MusicItem> data) {
		Log.i(TAG, "onLoadFinished");
		mHasNewData = true;

		// TODO SD卡拔出时，没有处理
		mAdapter.setData(data);

		if (data != null && data.size() != 0) {
			mView_Title.setText(getResources().getString(R.string.local_music)
					+ "(" + data.size() + ")");
		}
		// 数据加载完成，显示列表
		if (isResumed()) {
			// Fragment页面处于前端（Resumed状态）则显示页面变化的动画
			setListShown(true, true);
		} else {
			// Fragment页面不处于前端则不显示页面变化的动画
			setListShown(true, false);
		}

	}

	/** 此方法在提供给onLoadFinished()最后的一个游标准备关闭时调用，我们要确保不再使用它 */
	@Override
	public void onLoaderReset(Loader<List<MusicItem>> loader) {
		Log.i(TAG, "onLoaderReset");
		mAdapter.setData(null);
	}

	private class MusicListAdapter extends BaseAdapter {
		/** 数据源 */
		private List<MusicItem> mData = null;

		/** 播放时为相应播放条目显示一个播放标记 */
		private int mActivateItemPos = -1;

		public MusicListAdapter() {
			mData = new ArrayList<MusicItem>();
		}

		public void setData(List<MusicItem> data) {
			if (mData != null && data != null) {
				mData.clear();
				mData.addAll(data);
			}
			mActivateItemPos = -1;
		}

		public List<MusicItem> getData() {
			return mData;
		}

		/** 让指定位置的条目显示一个正在播放标记（活动状态标记） */
		public void setSpecifiedIndicator(int position) {
			mActivateItemPos = position;
			notifyDataSetChanged();
		}

		@Override
		public boolean isEmpty() {
			return mData.size() == 0;
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public MusicItem getItem(int position) {
			return mData.get((int) getItemId(position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = LayoutInflater.from(getActivity()).inflate(
						R.layout.list_item_local_music, null);
				holder = new ViewHolder();
				holder.indicator = convertView
						.findViewById(R.id.play_indicator);
				holder.title = (TextView) convertView
						.findViewById(R.id.textview_music_title);
				holder.artist = (TextView) convertView
						.findViewById(R.id.textview_music_singer);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			if (mActivateItemPos == position) {
				holder.indicator.setVisibility(View.VISIBLE);
			} else {
				holder.indicator.setVisibility(View.INVISIBLE);
			}
			holder.title.setText(getItem(position).getTitle());
			holder.artist.setText(getItem(position).getArtist());

			return convertView;
		}
	}

	public static class ViewHolder {
		TextView title;
		TextView artist;
		View indicator;
	}

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
		public void onPlayNewSong(MusicItem playingSong) {
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
