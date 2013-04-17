package com.lq.loader;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.lq.entity.MusicItem;

public class MusicRetrieveLoader extends AsyncTaskLoader<List<MusicItem>> {
	private final String TAG = MusicRetrieveLoader.class.getSimpleName();

	/** 要从MediaStore检索的列 */
	private final String[] mProjection = new String[] { Media._ID, Media.TITLE,
			Media.ALBUM, Media.ARTIST, Media.DATA, Media.SIZE, Media.DURATION };

	// 数据库查询相关参数
	private String mSelection = null;
	private String[] mSelectionArgs = null;
	private String mSortOrder = null;

	private ContentResolver mContentResolver = null;

	private List<MusicItem> mMusicItemList = null;

	public MusicRetrieveLoader(Context context, String selection,
			String[] selectionArgs, String sortOrder) {
		super(context);
		this.mSelection = selection;
		this.mSelectionArgs = selectionArgs;
		this.mSortOrder = sortOrder;
		mContentResolver = context.getContentResolver();
	}

	@Override
	public List<MusicItem> loadInBackground() {
		Log.i(TAG, "loadInBackground");
		Cursor cursor = mContentResolver.query(Media.EXTERNAL_CONTENT_URI,
				mProjection, mSelection, mSelectionArgs, mSortOrder);
		int index_id = cursor.getColumnIndex(Media._ID);
		int index_title = cursor.getColumnIndex(Media.TITLE);
		int index_data = cursor.getColumnIndex(Media.DATA);
		int index_artist = cursor.getColumnIndex(Media.ARTIST);
		int index_album = cursor.getColumnIndex(Media.ALBUM);
		int index_duration = cursor.getColumnIndex(Media.DURATION);
		int index_size = cursor.getColumnIndex(Media.SIZE);

		List<MusicItem> itemsList = new ArrayList<MusicItem>();

		// 将数据库查询结果保存到一个List集合中(存放在RAM)
		if (cursor != null) {
			while (cursor.moveToNext()) {
				MusicItem item = new MusicItem();
				item.setId(cursor.getLong(index_id));
				item.setArtist(cursor.getString(index_artist));
				item.setAlbum(cursor.getString(index_album));
				item.setTitle(cursor.getString(index_title));
				item.setData(cursor.getString(index_data));
				item.setDuration(cursor.getLong(index_duration));
				item.setSize(cursor.getLong(index_size));
				itemsList.add(item);
			}
		}

		// 如果没有扫描到媒体文件，itemsList的size为0，因为上面new过了
		return itemsList;
	}

	@Override
	public void deliverResult(List<MusicItem> data) {
		Log.i(TAG, "deliverResult");
		if (isReset()) {
			// An async query came in while the loader is stopped. We
			// don't need the result.
			if (data != null) {
				onReleaseResources(data);
			}
		}
		List<MusicItem> oldList = data;
		mMusicItemList = data;

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

	protected void onReleaseResources(List<MusicItem> data) {
		Log.i(TAG, "onReleaseResources");
		// For a simple List<> there is nothing to do. For something
		// like a Cursor, we would close it here.
	}

	@Override
	protected void onStartLoading() {
		Log.i(TAG, "onStartLoading");
		if (mMusicItemList != null) {
			// If we currently have a result available, deliver it
			// immediately.
			deliverResult(mMusicItemList);
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
	public void onCanceled(List<MusicItem> data) {
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
		if (mMusicItemList != null) {
			onReleaseResources(mMusicItemList);
			mMusicItemList = null;
		}
	}

	@Override
	protected void onForceLoad() {
		// TODO Auto-generated method stub
		super.onForceLoad();
	}

}
