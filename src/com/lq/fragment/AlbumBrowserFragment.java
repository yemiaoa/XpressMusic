package com.lq.fragment;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.lq.activity.MainContentActivity;
import com.lq.xpressmusic.R;
import com.lq.adapter.AlbumAdapter;
import com.lq.entity.AlbumInfo;
import com.lq.loader.AlbumInfoRetrieveLoader;
import com.lq.util.Constant;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class AlbumBrowserFragment extends Fragment implements
		LoaderCallbacks<List<AlbumInfo>> {
	private static final String TAG = AlbumBrowserFragment.class
			.getSimpleName();
	private final int ALBUM_RETRIEVE_LOADER = 0;

	private GestureDetector mDetector = null;

	private ImageView mView_MenuNavigation = null;
	private ImageView mView_MoreFunctions = null;
	private ImageView mView_GoToPlayer = null;
	private TextView mView_Title = null;
	private ListView mView_ListView = null;
	private PopupMenu mOverflowPopupMenu = null;

	private AlbumAdapter mAdapter = null;
	private MainContentActivity mActivity = null;
	private String mSortOrder = MediaStore.Audio.Albums.NUMBER_OF_SONGS
			+ " desc";

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof MainContentActivity) {
			mActivity = (MainContentActivity) activity;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");

		View rootView = inflater.inflate(R.layout.list_album, container, false);
		mView_ListView = (ListView) rootView.findViewById(R.id.listview_album);
		mView_MenuNavigation = (ImageView) rootView
				.findViewById(R.id.menu_navigation);
		mView_Title = (TextView) rootView.findViewById(R.id.title_of_top);
		mView_MoreFunctions = (ImageView) rootView
				.findViewById(R.id.more_functions);
		mView_GoToPlayer = (ImageView) rootView
				.findViewById(R.id.switch_to_player);
		mOverflowPopupMenu = new PopupMenu(getActivity(), mView_MoreFunctions);
		mOverflowPopupMenu.getMenuInflater().inflate(R.menu.popup_album_list,
				mOverflowPopupMenu.getMenu());

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		initViewsSetting();
		getLoaderManager().initLoader(ALBUM_RETRIEVE_LOADER, null, this);

	}

	@Override
	public void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
		startWatchingExternalStorage();
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();
		getActivity().unregisterReceiver(mExternalStorageReceiver);
	}

	@Override
	public void onDetach() {
		Log.i(TAG, "onDetach");
		super.onDetach();
		mActivity = null;
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

		mAdapter = new AlbumAdapter(getActivity());
		mView_ListView.setAdapter(mAdapter);
		mView_ListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (getParentFragment() instanceof FrameLocalMusicFragment
						|| getParentFragment() instanceof FrameAlbumFragment) {
					Bundle data = new Bundle();
					data.putParcelable(AlbumInfo.class.getSimpleName(),
							mAdapter.getData().get(position));
					data.putInt(Constant.PARENT, Constant.START_FROM_ALBUM);
					getFragmentManager()
							.beginTransaction()
							.replace(
									R.id.frame_for_nested_fragment,
									Fragment.instantiate(getActivity(),
											TrackBrowserFragment.class
													.getName(), data))
							.addToBackStack(null).commit();
				}
			}
		});
		mView_GoToPlayer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.switchToPlayer();
			}
		});
		mView_Title.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getFragmentManager().popBackStackImmediate();
			}
		});

		mOverflowPopupMenu
				.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.sort_by_music_count:
							mSortOrder = MediaStore.Audio.Albums.NUMBER_OF_SONGS
									+ " desc";
							getLoaderManager().restartLoader(
									ALBUM_RETRIEVE_LOADER, null,
									AlbumBrowserFragment.this);
							break;
						case R.id.sort_by_album:
							mSortOrder = Media.ALBUM_KEY;
							getLoaderManager().restartLoader(
									ALBUM_RETRIEVE_LOADER, null,
									AlbumBrowserFragment.this);
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

		if (getParentFragment() instanceof FrameLocalMusicFragment) {
			setTitleLeftDrawable();
		}
	}

	@Override
	public Loader<List<AlbumInfo>> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");

		StringBuilder where = new StringBuilder(Albums._ID
				+ " in (select distinct " + Media.ALBUM_ID
				+ " from audio_meta where (1=1 ");
		// String where = Albums._ID + " in (select " + Media.ALBUM_ID
		// + " from audio_meta where ((" + FileColumns.DATA
		// + " like'%.mp3' or " + Media.DATA + " like'%.wma') and "
		// + Media.DURATION + " > " + 1000 * 60 * 1 + " and "
		// + FileColumns.SIZE + " > " + 1024 + " )) ";

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		boolean filterBySize = sp.getBoolean(
				SettingFragment.KEY_FILTER_BY_SIZE, true);
		boolean filterByDuration = sp.getBoolean(

		SettingFragment.KEY_FILTER_BY_DURATION, true);
		if (filterBySize) {
			where.append(" and " + Media.SIZE + " > " + Constant.FILTER_SIZE);
		}
		if (filterByDuration) {
			where.append(" and " + Media.DURATION + " > "
					+ Constant.FILTER_DURATION);
		}

		where.append("))");

		// 创建并返回一个Loader
		return new AlbumInfoRetrieveLoader(getActivity(), where.toString(),
				null, mSortOrder);
	}

	@Override
	public void onLoadFinished(Loader<List<AlbumInfo>> loader,
			List<AlbumInfo> data) {
		Log.i(TAG, "onLoadFinished");

		// 载入完成，更新列表数据
		mAdapter.setData(data);

		// 在标题栏上显示艺术家数目
		if (data != null && data.size() != 0) {
			mView_Title.setText(getResources().getString(
					R.string.classify_by_album)
					+ "(" + data.size() + ")");
		}

	}

	@Override
	public void onLoaderReset(Loader<List<AlbumInfo>> loader) {
		Log.i(TAG, "onLoaderReset");
		mAdapter.setData(null);
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
				mView_Title.setText("");
				mAdapter.setData(null);
				// 提示SD卡不可用
				Toast.makeText(getActivity(), R.string.sdcard_cannot_use,
						Toast.LENGTH_SHORT).show();
			} else if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
				// TODO SD卡正常挂载,重新加载数据
				mView_MoreFunctions.setClickable(true);
				getLoaderManager().restartLoader(ALBUM_RETRIEVE_LOADER, null,
						AlbumBrowserFragment.this);
			}

		}
	};
}
