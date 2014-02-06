package com.lq.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lq.xpressmusic.R;
import com.lq.entity.TrackInfo;
import com.lq.util.StringHelper;
import com.lq.util.TimeHelper;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class TrackDetailDialogFragment extends DialogFragment {
	private static final String TAG = TrackDetailDialogFragment.class
			.getSimpleName();
	private static final String SONG_NAME = "song_name";
	private static final String SONG_DURATION = "song_duration";
	private static final String ARTIST = "artist";
	private static final String FILE_SIZE = "file_size";
	private static final String FILE_PATH = "file_path";

	private ViewGroup mView_vg_rootView = null;
	private TextView mView_tv_SongName = null;
	private TextView mView_tv_SongDuration = null;
	private TextView mView_tv_Artist = null;
	private TextView mView_tv_FileSize = null;
	private TextView mView_tv_FilePath = null;

	public static TrackDetailDialogFragment newInstance(TrackInfo track) {
		TrackDetailDialogFragment f = new TrackDetailDialogFragment();
		Bundle args = new Bundle();
		args.putString(SONG_NAME, track.getTitle());
		args.putString(SONG_DURATION,
				TimeHelper.milliSecondsToFormatTimeString(track.getDuration()));
		args.putString(ARTIST, track.getArtist());
		args.putString(FILE_SIZE, StringHelper.bytesToMB(track.getSize()));
		args.putString(FILE_PATH, track.getData());
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		findViews();
		setContentFromArgment();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.i(TAG, "onCreateDialog");
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.song_infomation).setView(mView_vg_rootView)
				.setNegativeButton(R.string.close, null).create();
	}

	private void findViews() {
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		mView_vg_rootView = (ViewGroup) inflater.inflate(
				R.layout.track_detail_dialog, null, false);
		mView_tv_SongName = (TextView) mView_vg_rootView
				.findViewById(R.id.track_detail_name);
		mView_tv_SongDuration = (TextView) mView_vg_rootView
				.findViewById(R.id.track_detail_song_duration);
		mView_tv_Artist = (TextView) mView_vg_rootView
				.findViewById(R.id.track_detail_artist);
		mView_tv_FileSize = (TextView) mView_vg_rootView
				.findViewById(R.id.track_detail_file_size);
		mView_tv_FilePath = (TextView) mView_vg_rootView
				.findViewById(R.id.track_detail_file_path);
	}

	private void setContentFromArgment() {
		Bundle args = getArguments();
		if (args != null) {
			mView_tv_SongName.setText(args.getString(SONG_NAME));
			mView_tv_SongDuration.setText(args.getString(SONG_DURATION));
			mView_tv_Artist.setText(args.getString(ARTIST));
			mView_tv_FileSize.setText(args.getString(FILE_SIZE));
			mView_tv_FilePath.setText(args.getString(FILE_PATH));
		}
	}
}
