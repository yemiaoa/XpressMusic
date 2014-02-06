package com.lq.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.lq.activity.MainContentActivity;
import com.lq.xpressmusic.R;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class PromptDialogFragment extends DialogFragment {
	private final String TAG = this.getClass().getSimpleName();
	private static final String TITLE = "title";
	MainContentActivity mMainActivity = null;
	DialogInterface.OnClickListener mListener = null;

	/**
	 * 生成PromptDialogFragment的实例
	 * 
	 * @param title
	 *            提示对话框的标题
	 * @param positiveButtonClickListener
	 *            确认按钮的监听器
	 */
	public static PromptDialogFragment newInstance(String title,
			DialogInterface.OnClickListener positiveButtonClickListener) {
		PromptDialogFragment f = new PromptDialogFragment();
		f.setOnPositiveButtonClickedListener(positiveButtonClickListener);
		Bundle args = new Bundle();
		args.putString(TITLE, title);
		f.setArguments(args);
		return f;
	}

	public void setOnPositiveButtonClickedListener(
			DialogInterface.OnClickListener positiveButtonClickListener) {
		mListener = positiveButtonClickListener;
	}

	@Override
	public void onAttach(Activity activity) {
		Log.i(TAG, "onAttach");
		super.onAttach(activity);
		if (activity instanceof MainContentActivity) {
			mMainActivity = (MainContentActivity) activity;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.i(TAG, "onCreateDialog");
		String title = "";
		if (getArguments() != null) {
			title = getArguments().getString(TITLE);
		}
		ContextThemeWrapper context = new ContextThemeWrapper(getActivity(),
				android.R.style.Theme_Dialog);
		return new AlertDialog.Builder(context)
				.setTitle(title)
				.setPositiveButton(R.string.confirm, mListener)
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dismiss();
							}
						}).create();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		super.onDestroy();
		mListener = null;
	}
}
