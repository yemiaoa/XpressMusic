package com.lq.entity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.lq.activity.MainContentActivity;
import com.lq.activity.R;

public class ExitDialogFragment extends DialogFragment {
	private final String TAG = this.getClass().getSimpleName();

	MainContentActivity mMainActivity = null;

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
		ContextThemeWrapper context = new ContextThemeWrapper(getActivity(),
				android.R.style.Theme_Holo_Dialog_NoActionBar);
		return new AlertDialog.Builder(context)
				.setIconAttribute(android.R.attr.alertDialogIcon)
				.setTitle(R.string.are_you_sure_to_exit)
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								if (mMainActivity != null) {
									mMainActivity.exit();
								}
							}
						})
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
	}
}
