package com.lq.loader;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.lq.dao.PlaylistDAO;
import com.lq.entity.PlaylistInfo;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PlaylistInfoRetrieveLoader extends
		AsyncTaskLoader<List<PlaylistInfo>> {
	private final String TAG = PlaylistInfoRetrieveLoader.class.getSimpleName();

	/** 要从MediaStore检索的列 */
	private final String[] mProjection = new String[] { Playlists._ID,
			Playlists.NAME, Playlists.DATE_ADDED, Playlists.DATE_MODIFIED };

	// 数据库查询相关参数
	private String mSelection = null;
	private String[] mSelectionArgs = null;
	private String mSortOrder = null;

	private ContentResolver mContentResolver = null;

	private List<PlaylistInfo> mPlaylistInfoList = null;

	public PlaylistInfoRetrieveLoader(Context context, String selection,
			String[] selectionArgs, String sortOrder) {
		super(context);
		this.mSelection = selection;
		this.mSelectionArgs = selectionArgs;
		this.mSortOrder = sortOrder;
		mContentResolver = context.getContentResolver();
	}

	@Override
	public List<PlaylistInfo> loadInBackground() {
		Log.i(TAG, "loadInBackground");
		int playlistId;
		Cursor cursor_playlist = mContentResolver.query(
				Playlists.EXTERNAL_CONTENT_URI, mProjection, mSelection,
				mSelectionArgs, mSortOrder);

		List<PlaylistInfo> itemsList = new ArrayList<PlaylistInfo>();

		// 将数据库查询结果保存到一个List集合中(存放在RAM)
		if (cursor_playlist != null) {
			int index_id = cursor_playlist.getColumnIndex(Playlists._ID);
			int index_name = cursor_playlist.getColumnIndex(Playlists.NAME);
			int index_date_added = cursor_playlist
					.getColumnIndex(Playlists.DATE_ADDED);
			int index_date_modified = cursor_playlist
					.getColumnIndex(Playlists.DATE_MODIFIED);
			while (cursor_playlist.moveToNext()) {
				PlaylistInfo item = new PlaylistInfo();
				playlistId = Integer.valueOf(cursor_playlist
						.getString(index_id));
				item.setId(playlistId);
				item.setNumOfMembers(PlaylistDAO.getPlaylistMemberCount(
						mContentResolver, playlistId));
				item.setPlaylistName(cursor_playlist.getString(index_name));
				item.setDateAdded(cursor_playlist.getInt(index_date_added));
				item.setDateModified(cursor_playlist
						.getInt(index_date_modified));
				itemsList.add(item);
			}
			cursor_playlist.close();
		}
		// 如果没有扫描到媒体文件，itemsList的size为0，因为上面new过了
		return itemsList;
	}

	@Override
	public void deliverResult(List<PlaylistInfo> data) {
		Log.i(TAG, "deliverResult");
		if (isReset()) {
			// An async query came in while the loader is stopped. We
			// don't need the result.
			if (data != null) {
				onReleaseResources(data);
			}
		}
		List<PlaylistInfo> oldList = data;
		mPlaylistInfoList = data;

		if (isStarted()) {
			// If the Loader is currently started, we can immediately
			// deliver its results.
			super.deliverResult(data);
		}

		// At this point we can release the resources associated with
		// 'oldApps' if needed; now that the new result is delivered we
		// know that it is no longer in use.
		if (oldList != null) {
			onReleaseResources(oldList);
		}
	}

	protected void onReleaseResources(List<PlaylistInfo> data) {
		Log.i(TAG, "onReleaseResources");
		// For a simple List<> there is nothing to do. For something
		// like a Cursor, we would close it here.
	}

	@Override
	protected void onStartLoading() {
		Log.i(TAG, "onStartLoading");
		if (mPlaylistInfoList != null) {
			// If we currently have a result available, deliver it
			// immediately.
			deliverResult(mPlaylistInfoList);
		}
		// If the data has changed since the last time it was loaded
		// or is not currently available, start a load.
		forceLoad();
	}

	@Override
	protected void onStopLoading() {
		Log.i(TAG, "onStartLoading");
		super.onStopLoading();
		// Attempt to cancel the current load task if possible.
		cancelLoad();
	}

	@Override
	public void onCanceled(List<PlaylistInfo> data) {
		super.onCanceled(data);
		Log.i(TAG, "onCanceled");
		// At this point we can release the resources associated with 'data'
		// if needed.
		onReleaseResources(data);
	}

	@Override
	protected void onReset() {
		super.onReset();
		Log.i(TAG, "onReset");
		// Ensure the loader is stopped
		onStopLoading();

		// At this point we can release the resources associated with 'data'
		// if needed.
		if (mPlaylistInfoList != null) {
			onReleaseResources(mPlaylistInfoList);
			mPlaylistInfoList = null;
		}
	}

	@Override
	protected void onForceLoad() {
		// TODO Auto-generated method stub
		super.onForceLoad();
	}

}
