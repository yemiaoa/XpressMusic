package com.lq.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Files.FileColumns;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.lq.xpressmusic.R;
import com.lq.entity.FolderInfo;
import com.lq.fragment.SettingFragment;
import com.lq.util.Constant;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class FolderInfoRetreiveLoader extends AsyncTaskLoader<List<FolderInfo>> {
	private final String TAG = this.getClass().getSimpleName();
	private ContentResolver mContentResolver = null;
	private List<FolderInfo> mArtistInfoList = null;

	private String num_of_songs = "num_of_songs";

	/** 要从MediaStore检索的列 */
	private final String[] mProjection = new String[] { FileColumns.DATA,
			"count(" + FileColumns.PARENT + ") as " + num_of_songs };
	/** where子句 */

	private StringBuilder mSelection = new StringBuilder(FileColumns.MEDIA_TYPE
			+ " = " + FileColumns.MEDIA_TYPE_AUDIO + " and " + "("
			+ FileColumns.DATA + " like'%.mp3' or " + Media.DATA
			+ " like'%.wma')");
	// private String mSelection = FileColumns.MEDIA_TYPE + " = "
	// + FileColumns.MEDIA_TYPE_AUDIO + " and " + "(" + FileColumns.DATA
	// + " like'%.mp3' or " + Media.DATA + " like'%.wma') and "
	// + Media.DURATION + " > " + 1000 * 60 * 1 + " and "
	// + FileColumns.SIZE + " > " + 1024 + " ) " + " group by ( "
	// + FileColumns.PARENT;
	private String[] mSelectionArgs = null;
	private String mSortOrder = null;

	public FolderInfoRetreiveLoader(Context context, String sortOrder) {
		super(context);
		mContentResolver = context.getContentResolver();
		mSortOrder = sortOrder;

		SharedPreferences mSystemPreferences = PreferenceManager
				.getDefaultSharedPreferences(getContext());
		// 检查系统设置，是否需要按文件大小过滤
		if (mSystemPreferences.getBoolean(SettingFragment.KEY_FILTER_BY_SIZE,
				true)) {
			// 查询语句：检索出.mp3为后缀名，时长大于1分钟，文件大小大于1MB的媒体文件
			mSelection.append(" and " + Media.SIZE + " > "
					+ Constant.FILTER_SIZE);
		}

		// 检查系统设置，是否需要按歌曲时长过滤
		if (mSystemPreferences.getBoolean(
				SettingFragment.KEY_FILTER_BY_DURATION, true)) {
			mSelection.append(" and " + Media.DURATION + " > "
					+ Constant.FILTER_DURATION);
		}

		mSelection.append(") group by ( " + FileColumns.PARENT);
	}

	@Override
	public List<FolderInfo> loadInBackground() {
		Log.i(TAG, "loadInBackground");
		String filepath, folderpath, foldername;
		int song_num = 0;

		Cursor cursor = mContentResolver.query(
				MediaStore.Files.getContentUri("external"), mProjection,
				mSelection.toString(), mSelectionArgs, mSortOrder);

		List<FolderInfo> itemsList = new ArrayList<FolderInfo>();

		// 将数据库查询结果保存到一个List集合中(存放在RAM)
		if (cursor != null) {
			int index_data = cursor
					.getColumnIndex(MediaStore.Files.FileColumns.DATA);
			int index_num_of_songs = cursor.getColumnIndex(num_of_songs);
			while (cursor.moveToNext()) {
				FolderInfo item = new FolderInfo();

				// 获取每个目录下的歌曲数量
				song_num = cursor.getInt(index_num_of_songs);
				item.setNumOfTracks(song_num);

				// 获取文件的路径，如/storage/sdcard0/MIUI/music/Baby.mp3
				filepath = cursor.getString(index_data);

				// 获取文件所属文件夹的路径，如/storage/sdcard0/MIUI/music
				folderpath = filepath.substring(0,
						filepath.lastIndexOf(File.separator));

				// 获取文件所属文件夹的名称，如music
				foldername = folderpath.substring(folderpath
						.lastIndexOf(File.separator) + 1);
				item.setFolderName(foldername);
				item.setFolderPath(folderpath);

				itemsList.add(item);
			}
			cursor.close();
		}
		// 如果没有扫描到媒体文件，itemsList的size为0，因为上面new过了
		return itemsList;
	}

	@Override
	public void deliverResult(List<FolderInfo> data) {
		Log.i(TAG, "deliverResult");
		if (isReset()) {
			// An async query came in while the loader is stopped. We
			// don't need the result.
			if (data != null) {
				onReleaseResources(data);
			}
		}
		List<FolderInfo> oldList = data;
		mArtistInfoList = data;

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

	protected void onReleaseResources(List<FolderInfo> data) {
		Log.i(TAG, "onReleaseResources");
		// For a simple List<> there is nothing to do. For something
		// like a Cursor, we would close it here.
	}

	@Override
	protected void onStartLoading() {
		Log.i(TAG, "onStartLoading");
		if (mArtistInfoList != null) {
			// If we currently have a result available, deliver it
			// immediately.
			deliverResult(mArtistInfoList);
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
	public void onCanceled(List<FolderInfo> data) {
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
		if (mArtistInfoList != null) {
			onReleaseResources(mArtistInfoList);
			mArtistInfoList = null;
		}
	}

	@Override
	protected void onForceLoad() {
		super.onForceLoad();
	}

}
