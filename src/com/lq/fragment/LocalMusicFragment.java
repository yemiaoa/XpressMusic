package com.lq.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.lq.activity.R;

public class LocalMusicFragment extends SherlockFragment implements
		OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor> {

	/** 显示本地音乐的列表 */
	private ListView mListView = null;

	/** 用来绑定数据至ListView的适配器 */
	private SimpleCursorAdapter mAdapter;

	/** SearchView输入栏的过滤 */
	private String mCurFilter;

	/** 要从MediaStore检索的列 */
	private static String[] MUSIC_PROJECTION = new String[] { Media._ID,
			Media.TITLE, Media.ALBUM, Media.ARTIST, Media.ALBUM_ID, Media.DATA,
			Media.SIZE };

	/** 在此加载一个ListView，可以使用自定义的ListView */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.layout_local_music_list,
				container, false);
		mListView = (ListView) rootView.findViewById(R.id.listview_local_music);
		return rootView;
	}

	/** 延迟ListView的设置到Activity创建时，为ListView绑定数据适配器 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// 如果没有数据，就作出提示
		mListView.setEmptyView(getView().findViewById(
				R.id.textview_local_music_empty));

		// 允许本Fragment在ActionBar上添加选项菜单项
		setHasOptionsMenu(true);

		// 创建一个空的适配器，用来显示加载的数据，适配器内容稍后由Loader填充
		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.list_item_local_music, null, new String[] {
						Media.TITLE, Media.ARTIST },
				new int[] { R.id.textview_music_title,
						R.id.textview_music_singer }, 0);

		// 为ListView绑定数据适配器
		mListView.setAdapter(mAdapter);

		// Start out with a progress indicator.
		// setListShown(false);

		// 初始化一个装载器，根据第一个参数，要么连接一个已存在的装载器，要么以此ID创建一个新的装载器
		getLoaderManager().initLoader(0, null, this);
	}

	/** 创建ActionBar上的选项菜单，在这里我们添加一个SearchView提供搜索过滤 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		// 添加一个ActionBar选项菜单项，设置参数
		MenuItem item = menu.add("Search");
		item.setIcon(android.R.drawable.ic_menu_search);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		// 绑定SearchView到ActionBar，设置其监听器
		SearchView sv = new SearchView(getActivity());
		sv.setOnQueryTextListener(this);
		item.setActionView(sv);
	}

	/** 当ActionBar的搜索内容改变时调用此方法 */
	@Override
	public boolean onQueryTextChange(String newText) {
		// 用户在查询输入框输入不为空则将输入的文字赋值给mCurFilter
		mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;

		// 重启0号装载器
		getLoaderManager().restartLoader(0, null, this);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		// 不关心这个方法的实现
		return true;
	}

	/** 在装载器需要被创建时执行此方法，这里只有一个装载器，所以我们不必关心装载器的ID */
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		// 查询语句：检索出.mp3为后缀名，时长大于两分钟，文件大小大于1MB的媒体文件
		String select = Media.DATA + " like'%.mp3' and " + Media.DURATION
				+ " > " + 1000 * 60 * 2 + " and " + Media.SIZE + " > " + 1024;

		// 如果用户在搜索栏输入了文字，追加到查询条件末尾，进行文件名模糊查询
		if (mCurFilter != null) {
			select += " and " + Media.TITLE + " like '%" + mCurFilter + "%'";
		}

		// 创建并返回一个CursorLoader
		return new CursorLoader(getActivity(), Media.EXTERNAL_CONTENT_URI,
				MUSIC_PROJECTION, select, null, Media.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// 在此切换新的游标，framework层会在我们返回新游标时自动帮我们关闭旧的游标，
		// 所以无需手动关闭旧游标
		mAdapter.swapCursor(data);

		// The list should now be shown.
		// if (isResumed()) {
		// setListShown(true);
		// } else {
		// setListShownNoAnimation(true);
		// }
	}

	/** 此方法在提供给onLoadFinished()最后的一个游标准备关闭时调用，我们要确保不再使用它 */
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

}
