package com.lq.util;

import java.io.File;

import android.os.Environment;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public final class Constant {
	public static final String SDCARD_ROOT_PATH = Environment
			.getExternalStorageDirectory() + "/";
	public static final String LYRIC_SAVE_FOLDER_PATH = Environment
			.getExternalStorageDirectory()
			+ File.separator
			+ "随心而乐"
			+ File.separator + "lyric" + File.separator;
	public static final String AUTHOR_EMAIL = "lq2625304@gmail.com";
	public static final String PLAY_MODE = "play_mode";
	public static final String PLAYING_STATE = "playing_state";
	public static final String CURRENT_PLAY_POSITION = "current_play_position";
	public static final String PLAYING_MUSIC_ITEM = "playing_music_item";
	public static final String CLICK_ITEM_IN_LIST = "click_item_in_list";
	public static final String REQUEST_PLAY_ID = "request_play_id";
	public static final String PARENT = "parent";
	public static final String DATA_LIST = "data_list";
	public static final String TITLE = "title";
	public static final String FIRST_VISIBLE_POSITION = "first_visible_position";
	public static final String PLAYLIST_ID = "playlist_id";
	public static final String PLAYING_SONG_POSITION_IN_LIST = "playing_song_position_in_list";
	public static final int FILTER_SIZE = 1 * 1024 * 1024;// 1MB
	public static final int FILTER_DURATION = 1 * 60 * 1000;// 1分钟
	public static final int START_FROM_LOCAL_MUSIC = 1;
	public static final int START_FROM_ARTIST = 2;
	public static final int START_FROM_FOLER = 3;
	public static final int START_FROM_PLAYLIST = 4;
	public static final int START_FROM_ALBUM = 5;
}
