package com.lq.fragment;

import java.lang.reflect.Field;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import com.lq.activity.MutipleEditActivity;
import com.lq.xpressmusic.R;
import com.lq.dao.PlaylistDAO;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class AddTrackToNewPlaylistDialogFragment extends DialogFragment {

	private static final String TAG = AddTrackToNewPlaylistDialogFragment.class
			.getSimpleName();

	private static final String AUDIO_ID = "audio_id";

	private long mAudioIds[] = null;

	private EditText mView_et_PlaylistName = null;

	private AlertDialog mAlertDialog = null;

	/**
	 * 产生一个EditTextDialogFragment实例
	 * 
	 * @param title
	 *            对话框的标题
	 * @param content
	 *            文本输入框预置内容
	 * @param hint
	 *            文本输入框提示信息
	 * @param listner
	 *            OnMyDialogInputListener实例，以接受输入事件的变化
	 * */
	public static AddTrackToNewPlaylistDialogFragment newInstance(long[] audioId) {
		AddTrackToNewPlaylistDialogFragment f = new AddTrackToNewPlaylistDialogFragment();
		Bundle args = new Bundle();
		args.putLongArray(AUDIO_ID, audioId);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mView_et_PlaylistName = (EditText) LayoutInflater.from(getActivity())
				.inflate(R.layout.edittext_dialog, null);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.i(TAG, "onCreateDialog");
		Resources r = getActivity().getResources();
		String title = r.getString(R.string.create_playlist);
		String hint = r.getString(R.string.input_playlist_name);
		mView_et_PlaylistName.setHint(hint);

		if (getArguments() != null) {
			mAudioIds = getArguments().getLongArray(AUDIO_ID);
		}

		mAlertDialog = new AlertDialog.Builder(getActivity()).setTitle(title)
				.setView(mView_et_PlaylistName)
				.setPositiveButton(R.string.confirm, mPositiveClickListener)
				.setNegativeButton(R.string.cancel, mNegativeClickListener)
				.create();

		return mAlertDialog;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mAlertDialog = null;
	}

	private void addTrackToPlaylist(String newListName) {
		int newListId = PlaylistDAO.createPlaylist(getActivity()
				.getContentResolver(), newListName);
		if (newListId == -1) {
			// 有重名播放列表，提示列表名已经存在
			Toast.makeText(
					getActivity(),
					getActivity().getString(
							R.string.playlist_has_already_existed),
					Toast.LENGTH_SHORT).show();
			setWindowShownWhenClickedButton(true);
		} else {
			// 无重名，则添加歌曲到新建的播放列表中
			if (mAudioIds != null) {
				PlaylistDAO.addTrackToPlaylist(getActivity()
						.getContentResolver(), newListId, mAudioIds);
				// 提示添加成功
				Toast.makeText(getActivity(),
						getActivity().getString(R.string.add_success),
						Toast.LENGTH_SHORT).show();
				// 如果是从多选界面打开本界面的，则关闭多选界面
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
						new Intent(MutipleEditActivity.ACTION_FINISH));
			}
			setWindowShownWhenClickedButton(false);
			dismiss();
		}
	}

	/**
	 * 利用反射将父类mShowing变量设为false，表示对话框已关闭，父类不会再因为按了按钮而关闭对话框
	 * 
	 * @param isShown
	 *            true表示点击按钮时对话框不会关闭，false为点击按钮对话框会关闭
	 */
	private void setWindowShownWhenClickedButton(boolean isShown) {
		try {
			Field field = mAlertDialog.getClass().getSuperclass()
					.getDeclaredField("mShowing");
			field.setAccessible(true);
			// 将mShowing变量设为false，表示对话框已关闭
			field.set(mAlertDialog, !isShown);
		} catch (Exception e) {

		}
	}

	private DialogInterface.OnClickListener mPositiveClickListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (mView_et_PlaylistName.getText().toString().equals("")) {
				// 如果输入框为空，提示输入
				Toast.makeText(getActivity(),
						getActivity().getString(R.string.input_playlist_name),
						Toast.LENGTH_SHORT).show();
				setWindowShownWhenClickedButton(true);
			} else {
				addTrackToPlaylist(mView_et_PlaylistName.getText().toString());
			}

		}
	};
	private DialogInterface.OnClickListener mNegativeClickListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			setWindowShownWhenClickedButton(false);
			dismiss();
		}
	};
}
