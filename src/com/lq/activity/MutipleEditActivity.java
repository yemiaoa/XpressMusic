package com.lq.activity;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.lq.adapter.TrackMutipleChooseAdapter;
import com.lq.entity.TrackInfo;
import com.lq.util.GlobalConstant;

public class MutipleEditActivity extends FragmentActivity {
	private final String TAG = MutipleEditActivity.class.getSimpleName();

	private ImageView mView_Close = null;
	private TextView mView_Title = null;
	private TextView mView_NumOfSelect = null;
	private CheckBox mView_SelectAll = null;
	private ListView mView_ListView = null;
	private View mView_PlayListLater = null;
	private View mView_AddToPlaylist = null;
	private View mView_Delete = null;
	private ArrayList<TrackInfo> mDataList = null;
	private TrackMutipleChooseAdapter mAdapter = null;
	private String mTitle = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mutiple_choose);

		Bundle args = getIntent().getExtras();
		mDataList = args.getParcelableArrayList(GlobalConstant.DATA_LIST);
		mTitle = args.getString(GlobalConstant.TITLE) + "(" + mDataList.size()
				+ ")";

		findViews();
		initViewsSetting();
	}

	private void findViews() {
		mView_Close = (ImageView) findViewById(R.id.close_mutiple_edit);
		mView_Title = (TextView) findViewById(R.id.title_mutiple_edit);
		mView_NumOfSelect = (TextView) findViewById(R.id.num_of_select);
		mView_PlayListLater = (View) findViewById(R.id.play_list_later);
		mView_AddToPlaylist = (View) findViewById(R.id.add_to_playlist);
		mView_Delete = (View) findViewById(R.id.delete_item);
		mView_SelectAll = (CheckBox) findViewById(R.id.select_all_cb);
		mView_ListView = (ListView) findViewById(R.id.listview_mutiple);
	}

	private void initViewsSetting() {
		mAdapter = new TrackMutipleChooseAdapter(this, mDataList);
		mView_ListView.setAdapter(mAdapter);
		mView_ListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mView_ListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mAdapter.toggleCheckedState(position);
				mView_NumOfSelect.setText(getResources().getString(
						R.string.has_selected)
						+ mAdapter.getCheckedItemPositions().length
						+ getResources().getString(R.string.a_piece_of_song));

			}
		});

		mView_Close.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				MutipleEditActivity.this.finish();
			}
		});

		mView_Title.setText(mTitle);
		mView_NumOfSelect.setText(getResources().getString(
				R.string.has_selected)
				+ 0 + getResources().getString(R.string.a_piece_of_song));

		mView_SelectAll
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						mAdapter.selectAllItem(isChecked);
						mView_NumOfSelect.setText(getResources().getString(
								R.string.has_selected)
								+ mAdapter.getCheckedItemPositions().length
								+ getResources().getString(
										R.string.a_piece_of_song));
					}
				});

	}

}
