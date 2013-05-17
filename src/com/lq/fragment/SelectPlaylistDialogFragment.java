package com.lq.fragment;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.lq.activity.R;
import com.lq.adapter.PlaylistAdapter;
import com.lq.dao.PlaylistDAO;
import com.lq.entity.PlaylistInfo;
import com.lq.loader.PlaylistInfoRetrieveLoader;

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

	private long mAudioId = -1;

	public static SelectPlaylistDialogFragment newInstance(long audioId) {
		SelectPlaylistDialogFragment f = new SelectPlaylistDialogFragment();
		Bundle args = new Bundle();
		args.putLong(Media._ID, audioId);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mAudioId = getArguments().getLong(Media._ID);
		}
		initViewsSetting();

		// 加载播放列表数据
		getLoaderManager().initLoader(PLAYLIST_RETRIEVE_LOADER, null, this);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.i(TAG, "onCreateDialog");
		return new AlertDialog.Builder(getActivity())
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
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
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
				// TODO 点击一个条目，则收藏到对应的列表中
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
			dismiss();
		}
	}
}
