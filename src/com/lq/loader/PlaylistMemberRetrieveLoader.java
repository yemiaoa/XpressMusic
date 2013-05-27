package com.lq.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.lq.entity.TrackInfo;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PlaylistMemberRetrieveLoader extends
		AsyncTaskLoader<List<TrackInfo>> {
	private final String TAG = PlaylistMemberRetrieveLoader.class
			.getSimpleName();

	/** 要从MediaStore检索的列 */
	private final String[] mMemberProjection = new String[] {
			Playlists.Members._ID, Playlists.Members.PLAYLIST_ID,
			Playlists.Members.AUDIO_ID, Playlists.Members.PLAY_ORDER };
	private final String[] mTrackProjection = new String[] { Media._ID,
			Media.TITLE, Media.ALBUM, Media.ARTIST, Media.DATA, Media.SIZE,
			Media.DURATION, Media.DISPLAY_NAME };
	// 数据库查询相关参数
	private String mSelection = null;
	private String[] mSelectionArgs = null;
	private String mSortOrder = null;

	private ContentResolver mContentResolver = null;

	private List<TrackInfo> mMusicItemList = null;

	private Pattern mFolerPattern = null;

	int index_id;
	int index_title;
	int index_data;
	int index_artist;
	int index_album;
	int index_duration;
	int index_size;
	int index_displayname;

	public PlaylistMemberRetrieveLoader(Context context, String selection,
			String[] selectionArgs, String sortOrder) {
		super(context);
		this.mSelection = selection;
		this.mSelectionArgs = selectionArgs;
		this.mSortOrder = sortOrder;
		mContentResolver = context.getContentResolver();
	}

	@Override
	public List<TrackInfo> loadInBackground() {
		Log.i(TAG, "loadInBackground");
		Cursor cursor = mContentResolver.query(Media.EXTERNAL_CONTENT_URI,
				mTrackProjection, mSelection, mSelectionArgs, mSortOrder);
		index_id = cursor.getColumnIndex(Media._ID);
		index_title = cursor.getColumnIndex(Media.TITLE);
		index_data = cursor.getColumnIndex(Media.DATA);
		index_artist = cursor.getColumnIndex(Media.ARTIST);
		index_album = cursor.getColumnIndex(Media.ALBUM);
		index_duration = cursor.getColumnIndex(Media.DURATION);
		index_size = cursor.getColumnIndex(Media.SIZE);
		index_displayname = cursor.getColumnIndex(Media.DISPLAY_NAME);

		List<TrackInfo> itemsList = new ArrayList<TrackInfo>();
		TrackInfo item = null;
		// 将数据库查询结果保存到一个List集合中(存放在RAM)
		if (cursor != null) {
			while (cursor.moveToNext()) {

				// 如果设置了文件夹过滤
				if (mFolerPattern != null) {
					// 过滤出指定的文件夹下的文件，忽略子目录
					Matcher matcher = mFolerPattern.matcher(cursor
							.getString(index_data));
					// 如果是以xxx.xxx结尾的路径，则就是当前目录下的文件了
					if (matcher.find() && matcher.group().matches(".*\\..*")) {
						item = createNewItem(cursor);
					} else {// 是文件夹就忽略了
						continue;
					}
				} else {// 正常的创建新的条目
					item = createNewItem(cursor);
				}
				itemsList.add(item);
			}
			cursor.close();
		}

		// 如果没有扫描到媒体文件，itemsList的size为0，因为上面new过了
		return itemsList;
	}

	@Override
	public void deliverResult(List<TrackInfo> data) {
		Log.i(TAG, "deliverResult");
		if (isReset()) {
			// An async query came in while the loader is stopped. We
			// don't need the result.
			if (data != null) {
				onReleaseResources(data);
			}
		}
		List<TrackInfo> oldList = data;
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

	protected void onReleaseResources(List<TrackInfo> data) {
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
	public void onCanceled(List<TrackInfo> data) {
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
		super.onForceLoad();
	}

	private TrackInfo createNewItem(Cursor cursor) {
		TrackInfo item = new TrackInfo();
		item.setTitle(cursor.getString(index_title));
		item.setArtist(cursor.getString(index_artist));
		item.setDisplayName(cursor.getString(index_displayname));
		item.setId(cursor.getLong(index_id));
		item.setAlbum(cursor.getString(index_album));
		item.setDuration(cursor.getLong(index_duration));
		item.setSize(cursor.getLong(index_size));
		item.setData(cursor.getString(index_data));
		return item;
	}

	/** 设置要过滤的文件夹 */
	public void setFolderFilterPattern(String folderpath) {
		// 过滤出当前目录下的文件，而忽略子目录的存在
		mFolerPattern = Pattern.compile(folderpath + File.separator + "[^"
				+ File.separator + "]+");
	}
}
