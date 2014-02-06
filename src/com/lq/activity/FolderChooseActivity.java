package com.lq.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.lq.fragment.EditTextDialogFragment;
import com.lq.fragment.EditTextDialogFragment.OnMyDialogInputListener;
import com.lq.fragment.SettingFragment;
import com.lq.util.Constant;
import com.lq.xpressmusic.R;
import com.umeng.analytics.MobclickAgent;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class FolderChooseActivity extends FragmentActivity {
	private final String TAG = FolderChooseActivity.class.getSimpleName();
	private ImageView mView_Close = null;
	private TextView mView_Title = null;
	private ListView mView_ListView = null;
	private Button mView_Confirm = null;
	private Button mView_CreateFolder = null;
	private ImageButton mView_BackToPrev = null;
	private TextView mView_CurrentPath = null;

	private ArrayAdapter<String> mAdapter = null;
	private ArrayList<String> mCurFolderList = new ArrayList<String>();

	private EditTextDialogFragment mCreateNewFolderDialogFragment = null;

	/** 路径栈 */
	private LinkedList<String> mPathStack = new LinkedList<String>();

	/** 当前路径 */
	private String mCurPath = Constant.SDCARD_ROOT_PATH;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.folder_choose);

		findViews();
		initViewsSetting();
		updateFolderList();
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	OnMyDialogInputListener mCreateNewFolderListener = new OnMyDialogInputListener() {

		@Override
		public void onEditTextInputCompleted(String newListName) {
			// TODO 在当前目录下新建目录

			String newFolerPath = mCurPath + newListName + File.separator;
			Log.i(TAG, "onEditTextInputCompleted--->newFolerPath:"
					+ newFolerPath);

			File file = new File(newFolerPath);
			if (file.exists()) {
				Log.i(TAG, "onEditTextInputCompleted--->exists");
				// 有同名目录，还请重新输入
				mCreateNewFolderDialogFragment.setDialogStayShown();
				Toast.makeText(getApplicationContext(),
						R.string.there_is_a_duplicated_folder,
						Toast.LENGTH_SHORT).show();
			} else {
				Log.i(TAG, "onEditTextInputCompleted--->not exists");
				// 木有同名目录才新建
				file.mkdir();
				updateFolderList();
				mCreateNewFolderDialogFragment.setDialogDismiss();
				mAdapter.notifyDataSetChanged();
			}

		}
	};

	@Override
	public void onBackPressed() {
		if (mPathStack.size() > 0) {
			backToPrevFolder();
		} else {
			super.onBackPressed();
		}
	}

	/** 返回上级目录 */
	private void backToPrevFolder() {
		mCurPath = mPathStack.pop();
		mView_CurrentPath.setText(mCurPath);
		updateFolderList();
		if (mPathStack.size() == 0) {
			mView_BackToPrev.setVisibility(View.INVISIBLE);
		}
	}

	/** 获取布局中的各个View对象 */
	private void findViews() {
		mView_Close = (ImageView) findViewById(R.id.close_folder_choose);
		mView_Title = (TextView) findViewById(R.id.title_folder_choose);
		mView_ListView = (ListView) findViewById(R.id.listview_storage_folders);
		mView_Confirm = (Button) findViewById(R.id.confirm_to_save);
		mView_CreateFolder = (Button) findViewById(R.id.create_new_folder);
		mView_BackToPrev = (ImageButton) findViewById(R.id.back_to_previous);
		mView_CurrentPath = (TextView) findViewById(R.id.current_path);
	}

	/** 初始化各个View的设置 */
	private void initViewsSetting() {
		// ListView的设置--------------------------------------------------------
		mAdapter = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.list_item_folder_choose, R.id.folder_name,
				mCurFolderList);
		mView_ListView.setAdapter(mAdapter);
		mView_ListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO 打开目录，记录上一次的目录
				mPathStack.push(mCurPath.toString());
				mCurPath = mCurPath + mAdapter.getItem(position)
						+ File.separator;
				mView_CurrentPath.setText(mCurPath);
				mView_BackToPrev.setVisibility(View.VISIBLE);
				updateFolderList();
			}
		});

		// 关闭按钮的设置--------------------------------------------------------
		mView_Close.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 点击关闭按钮，关闭本Activity
				FolderChooseActivity.this.finish();
			}
		});

		// 标题设置--------------------------------------------------------
		mView_Title.setText(R.string.choose_lyric_save_path);

		// 确认保存到当前位置
		mView_Confirm.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext())
						.edit()
						.putString(SettingFragment.KEY_LYRIC_SAVE_PATH,
								mCurPath).commit();
				FolderChooseActivity.this.finish();
			}
		});

		// 新建目录
		mView_CreateFolder.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCreateNewFolderDialogFragment = EditTextDialogFragment
						.newInstance(
								getResources().getString(
										R.string.create_new_folder),
								getResources().getString(R.string.app_name),
								null, mCreateNewFolderListener);
				mCreateNewFolderDialogFragment.show(
						getSupportFragmentManager(), null);
			}
		});

		// 回到上一级目录
		mView_BackToPrev.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				backToPrevFolder();
			}
		});

		// 获取歌词保存路径
		// mCurPath = PreferenceManager.getDefaultSharedPreferences(
		// getApplicationContext()).getString(
		// SettingFragment.KEY_LYRIC_SAVE_PATH,
		// Constant.LYRIC_SAVE_FOLDER_PATH);
		mView_CurrentPath.setText(mCurPath);
		mView_BackToPrev.setVisibility(View.INVISIBLE);
	}

	private void updateFolderList() {
		File file = new File(mCurPath.toString());
		mCurFolderList.clear();
		if (file.listFiles() != null) {
			for (File child : file.listFiles()) {
				if (child.isDirectory()) {
					mCurFolderList.add(child.getName());
				}
			}
			mAdapter.sort(mFolderNameComparator);
		}
		mAdapter.notifyDataSetChanged();
	}

	Comparator<String> mFolderNameComparator = new Comparator<String>() {
		char first_l, first_r;

		@Override
		public int compare(String lhs, String rhs) {
			// 汉字转拼音这个操作非常耗时
			// first_l = StringHelper.getPingYin(lhs).toLowerCase().charAt(0);
			// first_r = StringHelper.getPingYin(rhs).toLowerCase().charAt(0);
			first_l = lhs.toLowerCase().charAt(0);
			first_r = rhs.toLowerCase().charAt(0);
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
