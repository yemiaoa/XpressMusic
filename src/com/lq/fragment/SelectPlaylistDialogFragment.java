package com.lq.fragment;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.lq.activity.MutipleEditActivity;
import com.lq.xpressmusic.R;
import com.lq.adapter.PlaylistAdapter;
import com.lq.dao.PlaylistDAO;
import com.lq.entity.PlaylistInfo;
import com.lq.loader.PlaylistInfoRetrieveLoader;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class SelectPlaylistDialogFragment extends DialogFragment implements
		LoaderCallbacks<List<PlaylistInfo>> {
	private static final String TAG = SelectPlaylistDialogFragment.class
			.getSimpleName();
	private final int PLAYLIST_RETRIEVE_LOADER = 0;

	private ViewGroup mView_rootView = null;
	private ViewGroup mView_top = null;
	private View mView_createNewList = null;
	private ListView mView_playlist = null;

	private PlaylistAdapter mAdapter = null;

	private long mAudioId[] = null;

	public static SelectPlaylistDialogFragment newInstance(long[] audioId) {
		SelectPlaylistDialogFragment f = new SelectPlaylistDialogFragment();
		Bundle args = new Bundle();
		args.putLongArray(Media._ID, audioId);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mAudioId = getArguments().getLongArray(Media._ID);
		}
		initViewsSetting();

		// 加载播放列表数据
		getLoaderManager().initLoader(PLAYLIST_RETRIEVE_LOADER, null, this);

		startWatchingExternalStorage();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.i(TAG, "onCreateDialog");
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.add_to)
				.setView(mView_rootView)
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dismiss();
							}
						}).create();
		return dialog;
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		getActivity().unregisterReceiver(mExternalStorageReceiver);
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

	}

	@Override
	public void onLoaderReset(Loader<List<PlaylistInfo>> loader) {
		Log.i(TAG, "onLoaderReset");
		mAdapter.setData(null);
	}

	private void initViewsSetting() {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		mView_rootView = (ViewGroup) inflater.inflate(R.layout.list_playlist,
				null, false);
		mView_top = (ViewGroup) mView_rootView.findViewById(R.id.top);
		mView_top.setVisibility(View.GONE);
		mView_playlist = (ListView) mView_rootView
				.findViewById(R.id.listview_playlist);
		mView_createNewList = (View) mView_rootView
				.findViewById(R.id.add_playlist);

		mAdapter = new PlaylistAdapter(getActivity());
		mAdapter.setPopupMenuVisible(false);
		mView_playlist.setAdapter(mAdapter);
		mView_playlist.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				addTrackToPlaylist(position);
			}

		});

		mView_createNewList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 弹出播放列表名称新建窗口
				AddTrackToNewPlaylistDialogFragment f = AddTrackToNewPlaylistDialogFragment
						.newInstance(mAudioId);
				f.show(getFragmentManager(), null);
				dismiss();
			}
		});
	}

	private void addTrackToPlaylist(int position) {
		boolean isExisted = PlaylistDAO.addTrackToPlaylist(getActivity()
				.getContentResolver(), mAdapter.getItem(position).getId(),
				mAudioId);
		if (isExisted) {
			// 如果歌曲已经存在于指定的播放列表中，提示一下
			Toast.makeText(
					getActivity(),
					getResources().getString(R.string.song_has_already_existed),
					Toast.LENGTH_SHORT).show();
		} else {
			// 不存在，则说明添加成功，提示一下，再让窗口消失
			Toast.makeText(getActivity(),
					getResources().getString(R.string.add_success),
					Toast.LENGTH_SHORT).show();

			// 如果是从多选界面打开本界面的，则关闭多选界面
			LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
					new Intent(MutipleEditActivity.ACTION_FINISH));
			dismiss();
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
				// 提示SD卡不可用
				Toast.makeText(getActivity(), R.string.sdcard_cannot_use,
						Toast.LENGTH_SHORT).show();
				mAdapter.setData(null);
				dismiss();
			} else if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
				// SD卡正常挂载,重新加载数据
				getLoaderManager().restartLoader(PLAYLIST_RETRIEVE_LOADER,
						null, SelectPlaylistDialogFragment.this);
			}

		}
	};

}
