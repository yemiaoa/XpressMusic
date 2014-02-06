package com.lq.fragment;

import java.lang.reflect.Field;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import com.lq.xpressmusic.R;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class EditTextDialogFragment extends DialogFragment {
	/** 自定义的对话框里的文本输入监听器 */
	public interface OnMyDialogInputListener {
		/**
		 * 用户点击确定按钮时调用此方法
		 * 
		 * @param newListName
		 *            輸入的文本框中的内容
		 */
		public abstract void onEditTextInputCompleted(String newListName);
	}

	private static final String TAG = EditTextDialogFragment.class
			.getSimpleName();
	private static final String TITLE = "title";
	private static final String CONTENT = "content";
	private static final String HINT = "hint";

	private EditText mView_et_PlaylistName = null;
	private OnMyDialogInputListener mListener = null;

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
	public static EditTextDialogFragment newInstance(String title,
			String content, String hint, OnMyDialogInputListener listner) {
		EditTextDialogFragment f = new EditTextDialogFragment();
		f.setOnPlaylistCreateListener(listner);
		Bundle args = new Bundle();
		args.putString(TITLE, title);
		args.putString(CONTENT, content);
		args.putString(HINT, hint);
		f.setArguments(args);
		return f;
	}

	public void setOnPlaylistCreateListener(OnMyDialogInputListener l) {
		mListener = l;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		mView_et_PlaylistName = (EditText) LayoutInflater.from(getActivity())
				.inflate(R.layout.edittext_dialog, null);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.i(TAG, "onCreateDialog");
		String title = "";
		String content = "";
		String hint = "";
		if (getArguments() != null) {
			title = getArguments().getString(TITLE);
			content = getArguments().getString(CONTENT);
			hint = getArguments().getString(HINT);
		}
		mView_et_PlaylistName.setText(content);
		mView_et_PlaylistName.setHint(hint);
		if (content != null) {
			mView_et_PlaylistName.setSelection(content.length());
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
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		mListener = null;
		mAlertDialog = null;
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

	public void setDialogDismiss() {
		setWindowShownWhenClickedButton(false);
		dismiss();
	}

	public void setDialogStayShown() {
		setWindowShownWhenClickedButton(true);
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
			} else if (mListener != null) {
				Log.i(TAG, "确定--->传递文本给监听器");
				// 返回EditText中输入的内容
				mListener.onEditTextInputCompleted(mView_et_PlaylistName
						.getText().toString());
			}
		}
	};
	private DialogInterface.OnClickListener mNegativeClickListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			setDialogDismiss();
		}
	};
}
