package com.lq.dao;

import java.io.File;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;

/**
 * 对“播放列表”相关的数据库访问进行的封装类
 * 
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PlaylistDAO {
	public static final String TAG = PlaylistDAO.class.getSimpleName();

	/**
	 * 新建无重名的播放列表
	 * 
	 * @param resolver
	 *            Context的ContentResolver实例
	 * @param name
	 *            新建的播放列表的名称
	 * @return 如果有重名的播放列表，返回-1；无重名返回新建列表在数据库中的ID
	 */
	public static int createPlaylist(ContentResolver resolver, String name) {
		// 先检查有无同名的播放列表
		Cursor cursor_playlist = resolver.query(Playlists.EXTERNAL_CONTENT_URI,
				new String[] { Playlists.NAME }, null, null, null);
		if (cursor_playlist != null) {
			int index_name = cursor_playlist.getColumnIndex(Playlists.NAME);
			while (cursor_playlist.moveToNext()) {
				if (cursor_playlist.getString(index_name).equals(name)) {
					// 如果有同名，返回false
					cursor_playlist.close();
					Log.i(TAG,
							"create new list failed:playlist of the same name already existed");
					return -1;
				}
			}
		}

		// 没有同名的才新建
		ContentValues values = new ContentValues();
		values.put(Playlists.NAME, name);
		values.put(Playlists.DATE_ADDED, System.currentTimeMillis());
		values.put(Playlists.DATE_MODIFIED, System.currentTimeMillis());
		Uri newPlaylistUri = resolver.insert(Playlists.EXTERNAL_CONTENT_URI,
				values);
		Log.i(TAG, "new create playlist uri:" + newPlaylistUri);
		int newPlaylistId = Integer
				.valueOf(newPlaylistUri.getLastPathSegment());
		return newPlaylistId;
	}

	/**
	 * 重命名播放列表，如果重名不执行修改
	 * 
	 * @param resolver
	 *            Context的ContentResolver实例
	 * @param newName
	 *            新建的播放列表的名称
	 * @return 如果有重名的播放列表，返回true；无重名返回false
	 */
	public static boolean updatePlaylistName(ContentResolver resolver,
			String newName, int playlistId) {
		// 先检查有无同名的播放列表
		Cursor cursor_playlist = resolver.query(Playlists.EXTERNAL_CONTENT_URI,
				new String[] { Playlists.NAME }, null, null, null);
		if (cursor_playlist != null) {
			int index_name = cursor_playlist.getColumnIndex(Playlists.NAME);
			while (cursor_playlist.moveToNext()) {
				if (cursor_playlist.getString(index_name).equals(newName)) {
					// 如果有同名，返回false
					cursor_playlist.close();
					Log.i(TAG,
							"update_list_failed:playlist of the same name already existed");
					return true;
				}
			}
		}
		// 没有同名的才更新
		ContentValues values = new ContentValues();
		values.put(Playlists.NAME, newName);
		values.put(Playlists.DATE_MODIFIED, System.currentTimeMillis());
		int update_row = resolver.update(Playlists.EXTERNAL_CONTENT_URI,
				values, Playlists._ID + " = " + playlistId, null);
		Log.i(TAG, "update_row:" + update_row);
		return false;
	}

	/**
	 * 删除指定的播放列表
	 * 
	 * @param resolver
	 *            Context的ContentResolver实例
	 * @param playlistId
	 *            要删除的播放列表的id
	 */
	public static void deletePlaylist(ContentResolver resolver, int playlistId) {
		// 先删除Members表中的记录
		Uri uri = Playlists.Members.getContentUri("external", playlistId);
		int deleteRow = resolver.delete(uri, Playlists.Members.PLAYLIST_ID
				+ " = " + playlistId, null);
		Log.i(TAG, "deleted row count in Members:" + deleteRow);

		// 再删除Playlists表中的记录
		deleteRow = resolver.delete(Playlists.EXTERNAL_CONTENT_URI,
				Playlists._ID + " = " + playlistId, null);
		Log.i(TAG, "deleted row count in Playlists:" + deleteRow);
	}

	/**
	 * 为播放列表添加成员
	 * 
	 * @param resolver
	 *            Context的ContentResolver实例
	 * @param playlistId
	 *            播放列表的ID
	 * @param audioIds
	 *            添加的音频ID们
	 * @return true表示该歌曲已经存在指定列表中，false表示添加成功
	 */
	public static boolean addTrackToPlaylist(ContentResolver resolver,
			long playlistId, long[] audioIds) {
		boolean hasExistedItems = false;
		long[] existedIds = null;

		// 将audioIds变为(2,3,4,5)的形式，作数据库查询条件用
		StringBuffer audioIdsstring = new StringBuffer("(");
		for (int i = 0; i < audioIds.length; i++) {
			audioIdsstring.append(audioIds[i] + ",");
		}
		audioIdsstring.setCharAt(audioIdsstring.length() - 1, ')');

		// 先查询该播放列表中有无该歌曲，有则不做插入
		Cursor cursor = resolver.query(
				Playlists.Members.getContentUri("external", playlistId),
				new String[] { Playlists.Members.AUDIO_ID },
				Playlists.Members.AUDIO_ID + " in " + audioIdsstring, null,
				null);
		if (cursor != null) {

			if (cursor.getCount() == audioIds.length) {
				// 如果Members表中已经拥有所有要添加的歌曲，直接返回已经存在
				cursor.close();
				Log.i(TAG,
						"add to playlist member failed:members of the same name already existed");
				return true;
			}
			Log.d(TAG, "cursor数目：" + cursor.getCount());
			Log.d(TAG, "audioIds数目：" + audioIds.length);
			hasExistedItems = !(cursor.getCount() == 0);
			if (hasExistedItems) {
				existedIds = new long[cursor.getCount()];
				int index_id = cursor
						.getColumnIndex(Playlists.Members.AUDIO_ID);
				int i = 0;
				while (cursor.moveToNext()) {
					existedIds[i] = cursor.getLong(index_id);
					i++;
				}
			}
		}

		// 列表中无指定的歌曲，则向Members表中插入记录
		Uri uri = Playlists.Members.getContentUri("external", playlistId);
		ContentValues values = null;
		if (hasExistedItems) {
			for (int i = 0; i < audioIds.length; i++) {
				if (!isIdInTheIntArray(audioIds[i], existedIds)) {
					values = new ContentValues();
					values.put(Playlists.Members.PLAY_ORDER, audioIds[i]);
					values.put(Playlists.Members.AUDIO_ID, audioIds[i]);
					Uri newInsertUri = resolver.insert(uri, values);
					Log.i(TAG, "The new uri added to Members:" + newInsertUri);
				}
			}
		} else {
			for (int i = 0; i < audioIds.length; i++) {
				values = new ContentValues();
				values.put(Playlists.Members.PLAY_ORDER, audioIds[i]);
				values.put(Playlists.Members.AUDIO_ID, audioIds[i]);
				Uri newInsertUri = resolver.insert(uri, values);
				Log.i(TAG, "The new uri added to Members:" + newInsertUri);
			}
		}
		return false;
	}

	private static boolean isIdInTheIntArray(long id, long a[]) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == id) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 从播放列表移除指定歌曲
	 * 
	 * @param resolver
	 *            Context的ContentResolver实例
	 * @param playlistId
	 *            播放列表的ID
	 * @param audioIds
	 *            要移除的音频ID
	 * @return 删除成功返回true,否则返回false
	 */

	public static boolean removeTrackFromPlaylist(ContentResolver resolver,
			long playlistId, long[] audioIds) {
		if (audioIds == null) {
			return false;
		}
		boolean isRemoved = false;
		int deleteRowCount = 0;

		// 将整型数组变成(1,2,3,4,5)的格式，作为稍后的数据库删除的where子句
		StringBuffer toDeletIds = new StringBuffer("(");
		for (int i = 0; i < audioIds.length; i++) {
			toDeletIds.append(audioIds[i] + ",");
		}
		toDeletIds.setCharAt(toDeletIds.length() - 1, ')');
		Log.i(TAG, "toDeletIds in Members:" + toDeletIds);

		// 从Members表中移除记录
		Uri uri = Playlists.Members.getContentUri("external", playlistId);
		deleteRowCount = resolver.delete(uri, Playlists.Members.AUDIO_ID
				+ " in " + toDeletIds, null);
		if (deleteRowCount > 0) {
			isRemoved = true;
		}
		Log.i(TAG, "deleted row count in Members:" + deleteRowCount);
		return isRemoved;
	}

	/**
	 * 获取播放列表里的歌曲数目
	 * 
	 * @param resolver
	 *            Context的ContentResolver实例
	 * @param playlistId
	 *            播放列表的ID
	 */
	public static int getPlaylistMemberCount(ContentResolver resolver,
			int playlistId) {
		int result = 0;
		Uri uri = Playlists.Members.getContentUri("external", playlistId);
		Cursor cursor = resolver.query(uri, null, null, null, null);
		if (cursor != null) {
			result = cursor.getCount();
			cursor.close();
		}
		Log.i(TAG, "members count of the playlist(" + playlistId + "):"
				+ result);
		return result;
	}

	/**
	 * 删除指定路径的文件
	 * 
	 * @return 删除成功返回true, 删除失败false.
	 */
	public static boolean deleteFile(String path) {
		boolean isDeleted = false;
		File file = new File(path);
		if (file.exists()) {
			isDeleted = file.delete();
		}
		Log.i(TAG, "delete file " + isDeleted + ":<" + path + ">");
		return isDeleted;
	}

	/**
	 * 批量删除指定文件
	 * 
	 * @param 要删除的文件路径数组
	 * @return 路径数组不为空即返回true
	 */
	public static boolean deleteFiles(String[] paths) {
		if (paths == null) {
			return false;
		}
		for (int i = 0; i < paths.length; i++) {
			if (!deleteFile(paths[i])) {
				Log.i(TAG, "delete file failed:<" + paths[i] + ">");
			}
		}
		return true;
	}

	/**
	 * 从数据库中移除指定的音频
	 * 
	 * @return 移除成功返回true, 删除失败false.
	 */
	public static boolean removeTrackFromDatabase(ContentResolver resolver,
			long[] audioIds) {
		if (audioIds == null) {
			return false;
		}

		boolean isRemoved = false;

		// 将整型数组变成(1,2,3,4,5)的格式，作为稍后的数据库删除的where子句
		StringBuffer toRemoveIds = new StringBuffer("(");
		for (int i = 0; i < audioIds.length; i++) {
			toRemoveIds.append(audioIds[i] + ",");
		}
		toRemoveIds.setCharAt(toRemoveIds.length() - 1, ')');
		Log.i(TAG, "toRemoveIds from database:" + toRemoveIds);

		// 从数据库中移除音频记录
		int deleteRowCount = resolver.delete(Media.EXTERNAL_CONTENT_URI,
				Media._ID + " in " + toRemoveIds, null);
		if (deleteRowCount > 0) {
			isRemoved = true;
		}
		Log.i(TAG, "count of removed track from database :" + deleteRowCount);
		return isRemoved;
	}

}
