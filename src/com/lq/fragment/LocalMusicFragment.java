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
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.lq.activity.MainContentActivity;
import com.lq.activity.R;
import com.lq.entity.MusicItem;
import com.lq.loader.MusicRetrieveLoader;
import com.lq.service.MusicService;

/**
 * 读取并显示设备外存上的音乐文件
 * 
 * @author lq
 * */
public class LocalMusicFragment extends SherlockFragment implements
		OnQueryTextListener, LoaderManager.LoaderCallbacks<List<MusicItem>>,
		OnItemClickListener {
	// 调试用的标记
	private static final String TAG = LocalMusicFragment.class.getSimpleName();

	private MainContentActivity mActivity = null;

	/** 显示本地音乐的列表 */
	private ListView mListView = null;

	/** 数据加载完成前显示的进度条 */
	private ProgressBar mProgressBar = null;

	/** 用来绑定数据至ListView的适配器 */
	private MusicListAdapter mAdapter = null;

	/** SearchView输入栏的过滤 */
	private String mCurFilter = null;

	private static final int MUSIC_RETRIEVE_LOADER = 0;

	/** 服务端的信使，通过它发送消息来与MusicService交互 */
	private Messenger mServiceMessenger = null;

	/** 客户端的信使，公布给服务端，服务端通过它发送消息给IncomingHandler处理 */
	private final Messenger mClientMessenger = new Messenger(
			new ClientIncomingHandler(LocalMusicFragment.this));

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
			// 保持一个对服务端信使的引用，以便向服务端发送消息
			mServiceMessenger = new Messenger(service);
			try {
				// 一旦客户端与服务端连接上，让服务端保持一个客户端信使的引用，以便服务端向客户端发送消息
				Message msg = Message.obtain(null,
						MusicService.MESSAGE_REGISTER_CLIENT_MESSENGER);
				msg.replyTo = mClientMessenger;
				mServiceMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// 客户端与服务端取消连接时，告知服务端停止向本客户端发送消息
			try {
				Message msg = Message.obtain(null,
						MusicService.MESSAGE_UNREGISTER_CLIENT_MESSENGER);
				mServiceMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			mServiceMessenger = null;
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
		mListView = (ListView) rootView.findViewById(R.id.listview_local_music);
		mProgressBar = (ProgressBar) rootView
				.findViewById(R.id.progressbar_loading_local_music);
		return rootView;
	}

	/** 延迟ListView的设置到Activity创建时，为ListView绑定数据适配器 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		// 如果没有数据，就作出提示
		// mListView.setEmptyView(getView().findViewById(
		// R.id.textview_local_music_empty));

		// 允许本Fragment在ActionBar上添加选项菜单项
		setHasOptionsMenu(true);

		// 创建一个空的适配器，用来显示加载的数据，适配器内容稍后由Loader填充
		mAdapter = new MusicListAdapter();

		// 为ListView绑定数据适配器
		mListView.setAdapter(mAdapter);

		// 为ListView的条目绑定一个点击事件监听
		mListView.setOnItemClickListener(this);

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
				mProgressBar.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_out));
				mListView.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_in));
			} else {
				mProgressBar.clearAnimation();
				mListView.clearAnimation();
			}
			mProgressBar.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
		} else {
			if (animate) {
				mProgressBar.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_in));
				mListView.startAnimation(AnimationUtils.loadAnimation(
						getActivity(), android.R.anim.fade_out));
			} else {
				mProgressBar.clearAnimation();
				mListView.clearAnimation();
			}
			mProgressBar.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent intent = new Intent(MusicService.ACTION_PLAY);
		intent.putExtra(Media.TITLE, mAdapter.getItem(position).getTitle());
		intent.putExtra(Media._ID, mAdapter.getItem(position).getId());
		mActivity.startService(intent);
		mActivity.switchToMusicPlayer();
	}

	/** 创建ActionBar上的选项菜单，在这里我们添加一个SearchView提供搜索过滤 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		// 添加一个ActionBar选项菜单项，设置参数
		MenuItem item = menu.add("Search");
		item.setIcon(android.R.drawable.ic_menu_search);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		// 绑定SearchView到ActionBar，设置其监听器
		SearchView sv = new SearchView(getActivity());
		sv.setOnQueryTextListener(this);
		item.setActionView(sv);
	}

	/** 当ActionBar的搜索内容改变时调用此方法 */
	@Override
	public boolean onQueryTextChange(String newText) {
		// 用户在查询输入框输入不为空则将输入的文字赋值给mCurFilter
		// mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;

		// 重启0号装载器
		// getLoaderManager().restartLoader(0, null, this);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// 不关心这个方法的实现
		return true;
	}

	/** 在装载器需要被创建时执行此方法，这里只有一个装载器，所以我们不必关心装载器的ID */
	@Override
	public Loader<List<MusicItem>> onCreateLoader(int id, Bundle args) {

		// 查询语句：检索出.mp3为后缀名，时长大于两分钟，文件大小大于1MB的媒体文件
		String select = Media.DATA + " like'%.mp3' and " + Media.DURATION
				+ " > " + 1000 * 60 * 2 + " and " + Media.SIZE + " > " + 1024;

		// 如果用户在搜索栏输入了文字，追加到查询条件末尾，进行文件名模糊查询
		// if (mCurFilter != null) {
		// select += " and " + Media.TITLE + " like '%" + mCurFilter + "%'";
		// }

		// 创建并返回一个CursorLoader
		return new MusicRetrieveLoader(getActivity(), select, null,
				Media.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<List<MusicItem>> loader,
			List<MusicItem> data) {
		//TODO SD卡拔出时，没有处理
		mAdapter.setData(data);
		try {
			// 将新的载入数据传递给Service
			Message msg = Message.obtain(null,
					MusicService.MESSAGE_DELIVER_CURRENT_MUSIC_LIST);
			msg.obj = mAdapter.getData();
			mServiceMessenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
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
		mAdapter.setData(null);
	}

	private class MusicListAdapter extends BaseAdapter {
		private List<MusicItem> mData = null;

		public MusicListAdapter() {
			mData = new ArrayList<MusicItem>();
		}

		public void setData(List<MusicItem> data) {
			if (mData != null) {
				mData.clear();
			}
			if (data != null)
				mData.addAll(data);
		}

		public List<MusicItem> getData() {
			return mData;
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
				holder.title = (TextView) convertView
						.findViewById(R.id.textview_music_title);
				holder.artist = (TextView) convertView
						.findViewById(R.id.textview_music_singer);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.title.setText(getItem(position).getTitle());
			holder.artist.setText(getItem(position).getArtist());

			return convertView;
		}
	}

	public static class ViewHolder {
		TextView title;
		TextView artist;
	}

}
