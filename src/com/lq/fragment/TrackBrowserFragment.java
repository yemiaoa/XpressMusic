package com.lq.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.lq.activity.MainContentActivity;
import com.lq.activity.MainContentActivity.OnBackKeyPressedListener;
import com.lq.activity.MutipleEditActivity;
import com.lq.activity.R;
import com.lq.adapter.TrackAdapter;
import com.lq.dao.PlaylistDAO;
import com.lq.entity.AlbumInfo;
import com.lq.entity.ArtistInfo;
import com.lq.entity.FolderInfo;
import com.lq.entity.PlaylistInfo;
import com.lq.entity.TrackInfo;
import com.lq.listener.OnPlaybackStateChangeListener;
import com.lq.loader.MusicRetrieveLoader;
import com.lq.service.MusicService;
import com.lq.service.MusicService.MusicPlaybackLocalBinder;
import com.lq.util.GlobalConstant;
import com.lq.util.StringHelper;

/**
 * 读取并显示设备外存上的音乐文件
 * 
 * @author lq
 * */
public class TrackBrowserFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<List<TrackInfo>>, OnItemClickListener,
		OnBackKeyPressedListener {
	// 调试用的标记
	private final String TAG = this.getClass().getSimpleName();

	private static final int MUSIC_RETRIEVE_LOADER = 0;
	private final int CONTEXT_MENU_ADD_TO_PLAYLIST = 1;
	private final int CONTEXT_MENU_CHECK_DETAIL = 2;
	private final int CONTEXT_MENU_DELETE = 3;

	/** 手势检测 */
	private GestureDetector mDetector = null;

	private String mSortOrder = Media.TITLE_KEY;

	private Bundle mCurrentPlayInfo = null;

	private boolean mHasNewData = false;

	private MainContentActivity mActivity = null;

	/** 显示本地音乐的列表 */
	private ListView mView_ListView = null;

	private ImageView mView_MenuNavigation = null;
	private ImageView mView_GoToPlayer = null;
	private ImageView mView_MoreFunctions = null;
	private TextView mView_Title = null;
	private View mView_PlayAll = null;
	private View mView_Search = null;
	private View mView_MutipleChoose = null;
	private View mView_SearchBar = null;
	private EditText mView_SearchInput = null;
	private View mView_SearchCancel = null;
	private View mView_TrackOperations = null;
	private ImageView mView_KeyboardSwitcher = null;

	/** 弹出的搜索软键盘是否是自定义的T9键盘 */
	private boolean mIsT9Keyboard = true;

	private PopupMenu mOverflowPopupMenu = null;

	private PopupWindow mT9KeyBoardWindow = null;

	/** 用来绑定数据至ListView的适配器 */
	private TrackAdapter mAdapter = null;
	private List<TrackInfo> mOriginalData = new ArrayList<TrackInfo>();
	private List<TrackInfo> mShowData = new ArrayList<TrackInfo>();

	private ArtistInfo mArtistInfo = null;
	private FolderInfo mFolderInfo = null;
	private PlaylistInfo mPlaylistInfo = null;
	private AlbumInfo mAlbumInfo = null;
	private TrackInfo mToDeleteTrack = null;
	private TrackInfo mPlayingTrack = null;

	private InputMethodManager mInputMethodManager = null;

	private MusicPlaybackLocalBinder mMusicServiceBinder = null;

	/** 与Service连接时交互的类 */
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(TAG, "onServiceConnected");
			mMusicServiceBinder = (MusicPlaybackLocalBinder) service;
			mMusicServiceBinder
					.registerOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
			mCurrentPlayInfo = mMusicServiceBinder.getCurrentPlayInfo();
		}

		// 与服务端连接异常丢失时才调用，调用unBindService不调用此方法哎
		public void onServiceDisconnected(ComponentName className) {
		}
	};

	@Override
	public void onAttach(Activity activity) {
		Log.i(TAG, "onAttach");
		super.onAttach(activity);
		if (activity instanceof MainContentActivity) {
			mActivity = (MainContentActivity) activity;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		mInputMethodManager = (InputMethodManager) getActivity()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	/** 在此加载一个ListView，可以使用自定义的ListView */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.list_track, container, false);
		mView_ListView = (ListView) rootView
				.findViewById(R.id.listview_local_music);
		mView_MenuNavigation = (ImageView) rootView
				.findViewById(R.id.menu_navigation);
		mView_Title = (TextView) rootView.findViewById(R.id.title_of_top);
		mView_PlayAll = (View) rootView.findViewById(R.id.btn_play_all);
		mView_Search = (View) rootView.findViewById(R.id.btn_search);
		mView_MutipleChoose = (View) rootView
				.findViewById(R.id.btn_mutiple_choose);
		mView_MoreFunctions = (ImageView) rootView
				.findViewById(R.id.more_functions);
		mView_GoToPlayer = (ImageView) rootView
				.findViewById(R.id.switch_to_player);
		mView_SearchBar = (View) rootView.findViewById(R.id.search_bar);
		mView_KeyboardSwitcher = (ImageView) rootView
				.findViewById(R.id.keyboard_switcher);
		mView_SearchInput = (EditText) rootView.findViewById(R.id.search_input);
		mView_SearchCancel = (View) rootView.findViewById(R.id.cancel_search);
		mView_TrackOperations = (View) rootView
				.findViewById(R.id.track_operations);
		mOverflowPopupMenu = new PopupMenu(getActivity(), mView_MoreFunctions);
		Bundle args = getArguments();
		if (args != null) {
			switch (args.getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_LOCAL_MUSIC:
				mOverflowPopupMenu.getMenuInflater().inflate(
						R.menu.popup_local_music_list,
						mOverflowPopupMenu.getMenu());
				break;
			default:
				mOverflowPopupMenu.getMenuInflater().inflate(
						R.menu.popup_track_list, mOverflowPopupMenu.getMenu());
				break;
			}
		}
		return rootView;
	}

	/** 延迟ListView的设置到Activity创建时，为ListView绑定数据适配器 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);

		initViewsSetting();

		handleArguments();

	}

	@Override
	public void onStart() {
		Log.i(TAG, "onStart");
		super.onStart();
		// 在Fragment可见时绑定服务 ，以使服务可以发送消息过来
		getActivity().bindService(
				new Intent(getActivity(), MusicService.class),
				mServiceConnection, Context.BIND_AUTO_CREATE);
		if (mActivity instanceof MainContentActivity) {
			mActivity.registerBackKeyPressedListener(this);
		}
	}

	@Override
	public void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// sd card 可用
			// 显示操作栏
			mView_TrackOperations.setVisibility(View.VISIBLE);
			mView_MoreFunctions.setClickable(true);
			mView_Title.setText("");
			// 初始化一个装载器，根据第一个参数，要么连接一个已存在的装载器，要么以此ID创建一个新的装载器
			getLoaderManager().initLoader(MUSIC_RETRIEVE_LOADER, null, this);
		} else {
			// 当前不可用
			// 隐藏操作栏
			mView_TrackOperations.setVisibility(View.GONE);
			mView_MoreFunctions.setClickable(false);
			// 提示SD卡不可用
			Toast.makeText(getActivity(), R.string.sdcard_cannot_use,
					Toast.LENGTH_SHORT).show();
		}

		startWatchingExternalStorage();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStop() {
		Log.i(TAG, "onStop");
		super.onStop();
		mActivity.unregisterBackKeyPressedListener(this);
		// Fragment不可见时，无需更新UI，取消服务绑定
		mActivity.unbindService(mServiceConnection);
		mMusicServiceBinder
				.unregisterOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
		mMusicServiceBinder = null;
		getActivity().unregisterReceiver(mExternalStorageReceiver);
	}

	@Override
	public void onDetach() {
		Log.i(TAG, "onDetach");
		super.onDetach();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		mPlayingTrack = null;
		mAdapter = null;
		mActivity = null;
		mArtistInfo = null;
		mPlaylistInfo = null;
		mFolderInfo = null;
		mAlbumInfo = null;
		mCurrentPlayInfo = null;
		mShowData.clear();
		mShowData = null;
		mOriginalData.clear();
		mOriginalData = null;
	}

	@Override
	public void onBackKeyPressed() {

		// 按下返回键时关闭T9键盘
		if (mT9KeyBoardWindow != null) {
			if (mT9KeyBoardWindow.isShowing()) {
				mT9KeyBoardWindow.dismiss();
			}
		}
	}

	/** 初始化各个视图组件的设置 */
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

		// ListView的设置-------------------------------------------------------------
		// 创建一个空的适配器，用来显示加载的数据，适配器内容稍后由Loader填充
		mAdapter = new TrackAdapter(getActivity());
		// 为ListView绑定数据适配器
		mView_ListView.setAdapter(mAdapter);
		registerForContextMenu(mView_ListView);
		// 为ListView的条目绑定一个点击事件监听
		mView_ListView.setOnItemClickListener(this);

		// 标题的设置-------------------------------------------------------------
		mView_Title.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 标题作为回退导航
				getFragmentManager().popBackStackImmediate();
			}
		});
		// 默认不可点击
		mView_Title.setClickable(false);

		// 跳转至播放界面-----------------------------------------------
		mView_GoToPlayer.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mActivity.switchToPlayer();
			}
		});

		// 顶部弹出菜单----------------------------------------------------
		mOverflowPopupMenu
				.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.sort_by_music_name:
							mSortOrder = Media.TITLE_KEY;
							getLoaderManager().restartLoader(
									MUSIC_RETRIEVE_LOADER, null,
									TrackBrowserFragment.this);
							break;
						case R.id.sort_by_artist_name:
							mSortOrder = Media.ARTIST_KEY;
							getLoaderManager().restartLoader(
									MUSIC_RETRIEVE_LOADER, null,
									TrackBrowserFragment.this);
							break;
						case R.id.classify_by_artist:
							if (null != getParentFragment()
									&& getParentFragment() instanceof FrameLocalMusicFragment) {
								getFragmentManager()
										.beginTransaction()
										.replace(
												R.id.frame_for_nested_fragment,
												Fragment.instantiate(
														getActivity(),
														ArtistBrowserFragment.class
																.getName(),
														null))
										.addToBackStack(null).commit();
							}
							break;
						case R.id.classify_by_album:
							if (null != getParentFragment()
									&& getParentFragment() instanceof FrameLocalMusicFragment) {
								getFragmentManager()
										.beginTransaction()
										.replace(
												R.id.frame_for_nested_fragment,
												Fragment.instantiate(
														getActivity(),
														AlbumBrowserFragment.class
																.getName(),
														null))
										.addToBackStack(null).commit();
							}
							break;
						case R.id.classify_by_folder:
							if (null != getParentFragment()
									&& getParentFragment() instanceof FrameLocalMusicFragment) {
								getFragmentManager()
										.beginTransaction()
										.replace(
												R.id.frame_for_nested_fragment,
												Fragment.instantiate(
														getActivity(),
														FolderBrowserFragment.class
																.getName(),
														null))
										.addToBackStack(null).commit();
							}
							break;
						default:
							break;
						}
						return true;
					}
				});

		mView_MoreFunctions.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mOverflowPopupMenu.show();
			}
		});

		// 侧滑菜单弹出按钮------------------------------------------------
		mView_MenuNavigation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsT9Keyboard) {
					mT9KeyBoardWindow.dismiss();
				}
				mActivity.getSlidingMenu().showMenu();
			}
		});

		// 多选按钮-----------------------------------------------------------
		mView_MutipleChoose.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(),
						MutipleEditActivity.class);
				Bundle data = new Bundle();

				// 传递参数给多选界面
				switch (getArguments().getInt(GlobalConstant.PARENT)) {
				case GlobalConstant.START_FROM_LOCAL_MUSIC:
					data.putString(GlobalConstant.TITLE, getResources()
							.getString(R.string.local_music));
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_LOCAL_MUSIC);
					break;
				case GlobalConstant.START_FROM_ARTIST:
					data.putString(GlobalConstant.TITLE,
							mArtistInfo.getArtistName());
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_ARTIST);
					break;
				case GlobalConstant.START_FROM_FOLER:
					data.putString(GlobalConstant.TITLE,
							mFolderInfo.getFolderName());
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_FOLER);
					break;
				case GlobalConstant.START_FROM_PLAYLIST:
					data.putString(GlobalConstant.TITLE,
							mPlaylistInfo.getPlaylistName());
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_PLAYLIST);
					data.putInt(GlobalConstant.PLAYLIST_ID,
							mPlaylistInfo.getId());
					break;
				case GlobalConstant.START_FROM_ALBUM:
					data.putString(GlobalConstant.TITLE,
							mAlbumInfo.getAlbumName());
					data.putInt(GlobalConstant.PARENT,
							GlobalConstant.START_FROM_ALBUM);
					break;
				default:
					break;
				}
				data.putInt(GlobalConstant.FIRST_VISIBLE_POSITION,
						mView_ListView.getFirstVisiblePosition());
				data.putParcelableArrayList(GlobalConstant.DATA_LIST,
						mAdapter.getData());
				intent.putExtras(data);
				startActivity(intent);
			}
		});

		// 全部播放按钮----------------------------------------------------------
		mView_PlayAll.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mHasNewData && mMusicServiceBinder != null) {
					mMusicServiceBinder.setCurrentPlayList(mAdapter.getData());
				}
				mHasNewData = false;
				Intent intent = new Intent(MusicService.ACTION_PLAY);
				mActivity.startService(intent);
				mActivity.switchToPlayer();
			}
		});

		// 搜索相关-------------------------------------------------------------
		OnClickListener searchbarClickListner = new OnClickListener() {

			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.btn_search:
					// 显示搜索条，隐藏歌曲操作条
					mView_TrackOperations.setVisibility(View.GONE);
					mView_SearchBar.setVisibility(View.VISIBLE);
					mView_KeyboardSwitcher
							.setImageResource(R.drawable.keyboard_switch);
					mIsT9Keyboard = true;
					// 弹出T9键盘
					mT9KeyBoardWindow.showAtLocation(getView(), Gravity.BOTTOM,
							0, 0);
					// 搜索输入框禁止输入法输入
					mView_SearchInput.setInputType(InputType.TYPE_NULL);
					mView_SearchInput.requestFocus();
					break;
				case R.id.cancel_search:
					mView_SearchBar.setVisibility(View.GONE);
					mView_TrackOperations.setVisibility(View.VISIBLE);
					mView_SearchInput.setText("");
					mView_SearchInput
							.setHint(R.string.please_input_jianpin_or_quanpin);
					if (mIsT9Keyboard) {// 如果T9键盘开启，则隐藏
						mT9KeyBoardWindow.dismiss();
					} else {// 如果T9键盘未开启
						// 隐藏输入法
						mInputMethodManager.hideSoftInputFromWindow(
								mView_SearchInput.getWindowToken(), 0);
					}

					break;
				case R.id.keyboard_switcher:
					mIsT9Keyboard = !mIsT9Keyboard;// 置反一下
					if (mIsT9Keyboard) {// 如果T9键盘开启了
						mView_KeyboardSwitcher
								.setImageResource(R.drawable.keyboard_switch);
						mView_SearchInput
								.setHint(R.string.please_input_jianpin_or_quanpin);
						// 搜索输入框禁止输入法输入
						mView_SearchInput.setInputType(InputType.TYPE_NULL);

						// 隐藏输入法
						mInputMethodManager.hideSoftInputFromWindow(
								mView_SearchInput.getWindowToken(), 0);
						// 弹出T9键盘
						mT9KeyBoardWindow.showAtLocation(getView(),
								Gravity.BOTTOM, 0, 0);
					} else {// 如果T9键盘关闭了
						mT9KeyBoardWindow.dismiss();
						mView_KeyboardSwitcher
								.setImageResource(R.drawable.keyboard_switch_9);
						mView_SearchInput
								.setHint(R.string.please_input_song_or_artist_name);
						// 显示输入法
						mInputMethodManager.showSoftInput(mView_SearchInput, 0);
						// 搜索输入框允许输入法输入
						mView_SearchInput
								.setInputType(InputType.TYPE_CLASS_TEXT);
					}
					break;
				case R.id.search_input:
					if (mIsT9Keyboard) {
						if (!mT9KeyBoardWindow.isShowing()) {
							mT9KeyBoardWindow.showAtLocation(getView(),
									Gravity.BOTTOM, 0, 0);
						}
					}
					break;
				default:
					break;
				}

			}
		};

		mView_Search.setOnClickListener(searchbarClickListner);
		mView_SearchCancel.setOnClickListener(searchbarClickListner);
		mView_KeyboardSwitcher.setOnClickListener(searchbarClickListner);
		mView_SearchInput.setOnClickListener(searchbarClickListner);

		// T9键盘的设置---------------------------------------------------------------
		// 加载布局
		ViewGroup t9Layout = (ViewGroup) LayoutInflater.from(getActivity())
				.inflate(R.layout.t9_keyboard, null, false);
		t9Layout.findViewById(R.id.t9_key_2).setOnClickListener(
				mT9KeyClickedListener);
		t9Layout.findViewById(R.id.t9_key_3).setOnClickListener(
				mT9KeyClickedListener);
		t9Layout.findViewById(R.id.t9_key_4).setOnClickListener(
				mT9KeyClickedListener);
		t9Layout.findViewById(R.id.t9_key_5).setOnClickListener(
				mT9KeyClickedListener);
		t9Layout.findViewById(R.id.t9_key_6).setOnClickListener(
				mT9KeyClickedListener);
		t9Layout.findViewById(R.id.t9_key_7).setOnClickListener(
				mT9KeyClickedListener);
		t9Layout.findViewById(R.id.t9_key_8).setOnClickListener(
				mT9KeyClickedListener);
		t9Layout.findViewById(R.id.t9_key_9).setOnClickListener(
				mT9KeyClickedListener);
		t9Layout.findViewById(R.id.t9_exit).setOnClickListener(
				mT9KeyClickedListener);
		View deleteKey = t9Layout.findViewById(R.id.t9_delete);
		deleteKey.setOnClickListener(mT9KeyClickedListener);
		deleteKey.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				// 长按删除，删除所有输入内容
				Editable et = mView_SearchInput.getText();
				if (!et.toString().equals("")) {
					et.clear();
					mView_SearchInput.setText(et);
					mView_SearchInput.setSelection(0);
					return true;
				}
				return false;
			}
		});

		// 设置T9键盘弹出窗口属性
		mT9KeyBoardWindow = new PopupWindow(getActivity());
		mT9KeyBoardWindow.setOutsideTouchable(false);
		mT9KeyBoardWindow.setWidth(LayoutParams.MATCH_PARENT);
		mT9KeyBoardWindow.setHeight(LayoutParams.WRAP_CONTENT);
		mT9KeyBoardWindow.setFocusable(true);
		mT9KeyBoardWindow.setContentView(t9Layout);
		mT9KeyBoardWindow.setAnimationStyle(R.style.t9_window_anim);

		// 搜索输入框文本变化监听
		mView_SearchInput.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// 输入框文字改变时过滤歌曲列表
				if (TextUtils.isEmpty(s)) {
					mAdapter.setData(mOriginalData);
				} else if (mIsT9Keyboard) {
					// T9键盘开启，进行简拼全拼搜索
					pinyinSearch(s.toString());
				} else {
					// 普通的模糊搜索
					pinyinSearch(StringHelper.getPingYin(s.toString()));
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (mPlayingTrack != null) {
					mAdapter.setSpecifiedIndicator(MusicService
							.seekPosInListById(mAdapter.getData(),
									mPlayingTrack.getId()));
				} else {
					mAdapter.setSpecifiedIndicator(-1);
				}
			}
		});
	}

	/** T9键盘按键处理 */
	private OnClickListener mT9KeyClickedListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.t9_key_2:
				appendImageSpan(R.drawable.keyboard_edit_2, 2);
				break;
			case R.id.t9_key_3:
				appendImageSpan(R.drawable.keyboard_edit_3, 3);
				break;
			case R.id.t9_key_4:
				appendImageSpan(R.drawable.keyboard_edit_4, 4);
				break;
			case R.id.t9_key_5:
				appendImageSpan(R.drawable.keyboard_edit_5, 5);
				break;
			case R.id.t9_key_6:
				appendImageSpan(R.drawable.keyboard_edit_6, 6);
				break;
			case R.id.t9_key_7:
				appendImageSpan(R.drawable.keyboard_edit_7, 7);
				break;
			case R.id.t9_key_8:
				appendImageSpan(R.drawable.keyboard_edit_8, 8);
				break;
			case R.id.t9_key_9:
				appendImageSpan(R.drawable.keyboard_edit_9, 9);
				break;
			case R.id.t9_exit:
				mT9KeyBoardWindow.dismiss();
				break;
			case R.id.t9_delete:
				backDeleteImageSpan();
				break;
			default:
				break;
			}
		}
	};

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mHasNewData && mMusicServiceBinder != null) {
			mMusicServiceBinder.setCurrentPlayList(mAdapter.getData());
		}
		mHasNewData = false;
		Intent intent = new Intent(MusicService.ACTION_PLAY);
		intent.putExtra(GlobalConstant.REQUEST_PLAY_ID,
				mAdapter.getItem(position).getId());
		intent.putExtra(GlobalConstant.CLICK_ITEM_IN_LIST, true);
		mActivity.startService(intent);
		mActivity.switchToPlayer();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		final AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo) menuInfo;
		menu.setHeaderTitle(mAdapter.getItem(mInfo.position).getTitle());
		menu.add(0, CONTEXT_MENU_ADD_TO_PLAYLIST, Menu.NONE,
				R.string.add_to_playlist);
		menu.add(0, CONTEXT_MENU_CHECK_DETAIL, Menu.NONE, R.string.check_detail);

		switch (getArguments().getInt(GlobalConstant.PARENT)) {
		case GlobalConstant.START_FROM_PLAYLIST:
			menu.add(0, CONTEXT_MENU_DELETE, Menu.NONE, R.string.remove);
			break;
		default:
			menu.add(0, CONTEXT_MENU_DELETE, Menu.NONE, R.string.delete);
			break;
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		DialogFragment df = null;
		final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Log.i(TAG, "menuInfo:" + menuInfo);
		switch (item.getItemId()) {
		case CONTEXT_MENU_ADD_TO_PLAYLIST:
			// 弹出选择播放列表的窗口
			df = SelectPlaylistDialogFragment.newInstance(new long[] { mAdapter
					.getItem(menuInfo.position).getId() });
			df.show(getFragmentManager(), null);
			break;
		case CONTEXT_MENU_CHECK_DETAIL:
			// 弹出歌曲详细信息的窗口
			df = TrackDetailDialogFragment.newInstance(mAdapter
					.getItem(menuInfo.position));
			df.show(getFragmentManager(), null);
			break;
		case CONTEXT_MENU_DELETE:
			// 弹出确认删除的提示窗口
			mToDeleteTrack = mAdapter.getItem(menuInfo.position);
			switch (getArguments().getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_PLAYLIST:
				df = PromptDialogFragment.newInstance(
						getResources().getString(
								R.string.confirm_remove_song_from_playlist),
						mDeletePromptListener);
				df.show(getFragmentManager(), null);
				break;
			default:
				df = PromptDialogFragment.newInstance(
						getResources().getString(
								R.string.confirm_delete_song_file),
						mDeletePromptListener);
				df.show(getFragmentManager(), null);
				break;
			}
			break;

		default:
			return false;
		}
		return true;
	}

	/** 在装载器需要被创建时执行此方法，这里只有一个装载器，所以我们不必关心装载器的ID */
	@Override
	public Loader<List<TrackInfo>> onCreateLoader(int id, Bundle args) {
		Log.i(TAG, "onCreateLoader");

		// 查询语句：检索出.mp3为后缀名，时长大于1分钟，文件大小大于1MB的媒体文件
		StringBuffer select = new StringBuffer("(" + Media.DATA
				+ " like'%.mp3' or " + Media.DATA + " like'%.wma') and "
				+ Media.DURATION + " > " + 1000 * 60 * 1 + " and " + Media.SIZE
				+ " > " + 1024);

		if (mArtistInfo != null) {
			select.append(" and " + Media.ARTIST + " = '"
					+ mArtistInfo.getArtistName() + "'");
		} else if (mFolderInfo != null) {
			select.append(" and " + Media.DATA + " like '"
					+ mFolderInfo.getFolderPath() + File.separator + "%'");
		} else if (mPlaylistInfo != null) {
			select.append(" and " + Media._ID + " in (select "
					+ Playlists.Members.AUDIO_ID
					+ " from audio_playlists_map where "
					+ Playlists.Members.PLAYLIST_ID + "="
					+ mPlaylistInfo.getId() + ")");
		} else if (mAlbumInfo != null) {
			select.append(" and " + Media.ALBUM_ID + " = "
					+ mAlbumInfo.getAlbumId());
		}

		MusicRetrieveLoader loader = new MusicRetrieveLoader(getActivity(),
				select.toString(), null, mSortOrder);

		if (mFolderInfo != null) {
			loader.setFolderFilterPattern(mFolderInfo.getFolderPath());
		}

		// 创建并返回一个Loader
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<List<TrackInfo>> loader,
			List<TrackInfo> data) {
		Log.i(TAG, "onLoadFinished");
		mHasNewData = true;

		mOriginalData.clear();
		mOriginalData.addAll(data);
		mShowData.clear();
		mShowData.addAll(data);

		if (mSortOrder.equals(Media.TITLE_KEY)) {
			Collections.sort(data, mTrackNameComparator);
		} else if (mSortOrder.equals(Media.ARTIST_KEY)) {
			Collections.sort(data, mArtistNameComparator);
		}

		mAdapter.setData(data);

		// 每次加载新的数据设置一下标题中的歌曲数目
		if (getArguments() != null) {
			switch (getArguments().getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_LOCAL_MUSIC:
				mView_Title.setText(getResources().getString(
						R.string.local_music)
						+ "(" + data.size() + ")");
				break;
			case GlobalConstant.START_FROM_ARTIST:
				mView_Title.setText(mArtistInfo.getArtistName() + "("
						+ data.size() + ")");
				break;
			case GlobalConstant.START_FROM_FOLER:
				mView_Title.setText(mFolderInfo.getFolderName() + "("
						+ data.size() + ")");
				break;
			case GlobalConstant.START_FROM_PLAYLIST:
				mView_Title.setText(mPlaylistInfo.getPlaylistName() + "("
						+ data.size() + ")");
				break;
			case GlobalConstant.START_FROM_ALBUM:
				mView_Title.setText(mAlbumInfo.getAlbumName() + "("
						+ data.size() + ")");
				break;
			default:
				break;
			}
		}

		if (mCurrentPlayInfo != null) {
			initCurrentPlayInfo(mCurrentPlayInfo);
		}
	}

	/** 此方法在提供给onLoadFinished()最后的一个游标准备关闭时调用，我们要确保不再使用它 */
	@Override
	public void onLoaderReset(Loader<List<TrackInfo>> loader) {
		Log.i(TAG, "onLoaderReset");
		mAdapter.setData(null);
	}

	/** 初始化当前播放信息 */
	private void initCurrentPlayInfo(Bundle bundle) {
		mPlayingTrack = bundle.getParcelable(GlobalConstant.PLAYING_MUSIC_ITEM);

		if (mPlayingTrack != null) {
			mAdapter.setSpecifiedIndicator(MusicService.seekPosInListById(
					mAdapter.getData(), mPlayingTrack.getId()));
		} else {
			mAdapter.setSpecifiedIndicator(-1);
		}

	}

	/** 处理从启动处传递过来的参数 */
	private void handleArguments() {
		// 如果有谁传递数据过来了，就设置一下
		Bundle args = getArguments();
		if (args != null) {
			switch (args.getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_ARTIST:
				// 如果是从歌手列表里启动的
				mArtistInfo = args.getParcelable(ArtistInfo.class
						.getSimpleName());
				if (mArtistInfo != null) {
					// 更新标题
					if (!mArtistInfo.getArtistName().equals("<unknown>")) {
						mView_Title.setText(mArtistInfo.getArtistName() + "("
								+ mArtistInfo.getNumberOfTracks() + ")");
					} else {
						mView_Title.setText(getResources().getString(
								R.string.unknown_artist)
								+ "(" + mArtistInfo.getNumberOfTracks() + ")");
					}
					setTitleLeftDrawable();
				}
				break;
			case GlobalConstant.START_FROM_FOLER:
				// 如果是从文件夹列表里启动的
				mFolderInfo = args.getParcelable(FolderInfo.class
						.getSimpleName());
				if (mFolderInfo != null) {
					// 更新标题
					mView_Title.setText(mFolderInfo.getFolderName() + "("
							+ mFolderInfo.getNumOfTracks() + ")");
					setTitleLeftDrawable();
				}
				break;
			case GlobalConstant.START_FROM_PLAYLIST:
				// 如果是从播放列表里启动的
				mPlaylistInfo = args.getParcelable(PlaylistInfo.class
						.getSimpleName());
				if (mPlaylistInfo != null) {
					// 更新标题
					mView_Title.setText(mPlaylistInfo.getPlaylistName() + "("
							+ mPlaylistInfo.getNumOfMembers() + ")");
					setTitleLeftDrawable();
				}
				break;
			case GlobalConstant.START_FROM_ALBUM:
				// 如果是从专辑列表里启动的
				mAlbumInfo = args
						.getParcelable(AlbumInfo.class.getSimpleName());
				if (mAlbumInfo != null) {
					// 更新标题
					mView_Title.setText(mAlbumInfo.getAlbumName() + "("
							+ mAlbumInfo.getNumberOfSongs() + ")");
					setTitleLeftDrawable();
				}
				break;
			default:
				break;
			}
		}
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

	// 处理搜索的相关函数------------------------------------------------------------------

	/** T9键盘各数字键对应的正则表达式 */
	private static String T9KEYS[] = { "", "", "[abc]", "[def]", "[ghi]",
			"[jkl]", "[mno]", "[pqrs]", "[tuv]", "[wxyz]" };

	/** 在搜索输入框末尾追加T9键的输入 */
	private void appendImageSpan(int drawableResId, int keynum) {
		Drawable drawable = getResources().getDrawable(drawableResId);
		String insteadString = String.valueOf(keynum);
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight());
		// 需要处理的文本，[smile]是需要被替代的文本
		SpannableString spannable = new SpannableString(insteadString);
		// 要让图片替代指定的文字就要用ImageSpan
		ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
		// 开始替换，注意第2和第3个参数表示从哪里开始替换到哪里替换结束（start和end）
		// 最后一个参数类似数学中的集合,[5,12)表示从5到12，包括5但不包括12
		spannable.setSpan(span, 0, insteadString.length(),
				Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		Editable et = mView_SearchInput.getText();
		int start = mView_SearchInput.getSelectionStart();
		et.insert(start, spannable);
		mView_SearchInput.setText(et);
		mView_SearchInput.setSelection(start + spannable.length());
	}

	/** 回退删除搜索框的T9键的输入 */
	private void backDeleteImageSpan() {
		Editable et = mView_SearchInput.getText();
		if (!et.toString().equals("")) {
			et = et.delete(et.length() - 1, et.length());
			mView_SearchInput.setText(et);
			mView_SearchInput.setSelection(et.length());
		}
	}

	/**
	 * T9键盘简拼、全拼搜索
	 * 
	 * @param str
	 *            输入的字符串，均为2~9的数字
	 */
	private void pinyinSearch(String input) {
		mShowData.clear();
		StringBuffer sb = new StringBuffer();

		// XXX 对特殊非字母、非数字、非汉字的字符支持不理想，有待改进

		// 获取每一个数字对应的字母列表并以'-'隔开
		for (int i = 0; i < input.length(); i++) {
			if (mIsT9Keyboard && input.charAt(i) <= '9'
					&& input.charAt(i) >= '0') {
				sb.append(T9KEYS[input.charAt(i) - '0']);
			} else {
				sb.append(input.charAt(i));
			}
			if (i != input.length() - 1) {
				sb.append("-");
			}
		}

		// 遍历原始数据集合，寻找匹配的条目
		for (TrackInfo item : mOriginalData) {
			if (contains(sb.toString(), item.getTitleKey(), input)) {
				mShowData.add(item);
			} else if (contains(sb.toString(), item.getArtistKey(), input)) {
				mShowData.add(item);
			}
		}
		mAdapter.setData(mShowData);
	}

	/**
	 * 检查所给的搜索索引值是否匹配给定正则表达式
	 * 
	 * @param regexp
	 *            正则表达式
	 * @param key
	 *            索引值
	 * @param input
	 *            搜索条件是否大于6个字符
	 * @return
	 */
	private boolean contains(String regexp, String key, String input) {
		if (TextUtils.isEmpty(key)) {
			return false;
		}
		// 搜索条件大于6个字符将不按拼音首字母查询
		if (input.length() < 6) {
			// 根据首字母进行模糊查询
			Pattern pattern = Pattern.compile(regexp.toUpperCase().replace("-",
					"[*+a-z]*"));
			Matcher matcher = pattern.matcher(key);

			if (matcher.find()) {
				return true;
			}
		}

		// 根据全拼查询
		Pattern pattern = Pattern.compile(regexp.replace("-", ""),
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(key);
		return matcher.find();
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
				mView_TrackOperations.setVisibility(View.GONE);
				mView_MoreFunctions.setClickable(false);
				mView_TrackOperations.setVisibility(View.GONE);
				mView_Title.setText("");
				mAdapter.setData(null);
				// 提示SD卡不可用
				Toast.makeText(getActivity(), R.string.sdcard_cannot_use,
						Toast.LENGTH_SHORT).show();
			} else if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
				// SD卡正常挂载,重新加载数据
				mView_TrackOperations.setVisibility(View.VISIBLE);
				mView_MoreFunctions.setClickable(true);
				TrackBrowserFragment.this.getLoaderManager().restartLoader(
						MUSIC_RETRIEVE_LOADER, null, TrackBrowserFragment.this);
			}

		}
	};

	// 删除提醒对话框处理------------------------------------------------------------------
	private DialogInterface.OnClickListener mDeletePromptListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			boolean isDeleted = false;
			switch (getArguments().getInt(GlobalConstant.PARENT)) {
			case GlobalConstant.START_FROM_PLAYLIST:
				// 从播放列表移除歌曲，不会删除文件
				isDeleted = PlaylistDAO.removeTrackFromPlaylist(getActivity()
						.getContentResolver(), mPlaylistInfo.getId(),
						new long[] { mToDeleteTrack.getId() });
				if (isDeleted) {
					// 提示删除成功
					Toast.makeText(getActivity(),
							getResources().getString(R.string.remove_success),
							Toast.LENGTH_SHORT).show();
				}
				break;

			default:
				// 删除的歌曲现在是否正在播放
				TrackInfo curTrack = mMusicServiceBinder.getCurrentPlayInfo()
						.getParcelable(GlobalConstant.PLAYING_MUSIC_ITEM);

				if (curTrack != null) {
					if (curTrack.getId() == mToDeleteTrack.getId()) {
						// 要删除的歌曲正在播放，则先播放下一首歌,如果没有下一首就停止播放
						mMusicServiceBinder
								.removeSongFromCurrenPlaylist(mToDeleteTrack
										.getId());
						// getActivity().startService(
						// new Intent(MusicService.ACTION_NEXT));
					}
				}

				// 删除指定的歌曲,在存储器上的文件和数据库里的记录都要删除
				PlaylistDAO.removeTrackFromDatabase(getActivity()
						.getContentResolver(), new long[] { mToDeleteTrack
						.getId() });
				isDeleted = PlaylistDAO.deleteFile(mToDeleteTrack.getData());
				if (isDeleted) {
					// 提示删除成功
					Toast.makeText(getActivity(),
							getResources().getString(R.string.delete_success),
							Toast.LENGTH_SHORT).show();
				}
				break;
			}

			if (!isDeleted) {
				// 删除失败，提示失败信息
				Toast.makeText(getActivity(),
						getResources().getString(R.string.delete_failed),
						Toast.LENGTH_SHORT).show();
			} else {
				// 重新读取数据库，更新列表显示
				getLoaderManager().restartLoader(MUSIC_RETRIEVE_LOADER, null,
						TrackBrowserFragment.this);
			}
		}
	};

	private OnPlaybackStateChangeListener mOnPlaybackStateChangeListener = new OnPlaybackStateChangeListener() {

		@Override
		public void onMusicPlayed() {

		}

		@Override
		public void onMusicPaused() {

		}

		@Override
		public void onMusicStopped() {

		}

		@Override
		public void onPlayNewSong(TrackInfo playingSong) {
			mPlayingTrack = playingSong;
			mAdapter.setSpecifiedIndicator(MusicService.seekPosInListById(
					mAdapter.getData(), playingSong.getId()));
		}

		@Override
		public void onPlayModeChanged(int playMode) {

		}

		@Override
		public void onPlayProgressUpdate(int currentMillis) {

		}

	};

	// 按歌曲名称顺序排序
	private Comparator<TrackInfo> mTrackNameComparator = new Comparator<TrackInfo>() {
		char first_l, first_r;

		@Override
		public int compare(TrackInfo lhs, TrackInfo rhs) {
			first_l = lhs.getTitleKey().charAt(0);
			first_r = rhs.getTitleKey().charAt(0);
			if (StringHelper.checkType(first_l) == StringHelper.CharType.CHINESE) {
				first_l = StringHelper.getPinyinFirstLetter(first_l);
			}
			if (StringHelper.checkType(first_r) == StringHelper.CharType.CHINESE) {
				first_r = StringHelper.getPinyinFirstLetter(first_r);
			}
			if (first_l > first_r) {
				return 1;
			} else if (first_l < first_r) {
				return -1;
			} else {
				return 0;
			}
		}
	};
	// 按歌曲名称顺序排序
	private Comparator<TrackInfo> mArtistNameComparator = new Comparator<TrackInfo>() {
		char first_l, first_r;

		@Override
		public int compare(TrackInfo lhs, TrackInfo rhs) {
			first_l = lhs.getArtistKey().charAt(0);
			first_r = rhs.getArtistKey().charAt(0);
			if (StringHelper.checkType(first_l) == StringHelper.CharType.CHINESE) {
				first_l = StringHelper.getPinyinFirstLetter(first_l);
			}
			if (StringHelper.checkType(first_r) == StringHelper.CharType.CHINESE) {
				first_r = StringHelper.getPinyinFirstLetter(first_r);
			}
			if (first_l > first_r) {
				return 1;
			} else if (first_l < first_r) {
				return -1;
			} else {
				return 0;
			}
		}
	};

}
