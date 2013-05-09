package com.lq.listener;

import com.lq.entity.MusicItem;
import com.lq.service.MusicService.State;

/**
 * 自定义的一个服务连接监听器。 <br>
 * 在显示组件与服务组件连接时，用以将业务数据通知并传递给显示层进行初始化；<br>
 * 在显示组件与服务组件断开连接时，通知显示层。
 */
public interface OnServiceConnectionListener {
	/**
	 * 在调用MusicService里的
	 * {@linkplain com.lq.service.MusicService#registerOnServiceConnectionListener
	 * registerOnServiceConnectionListener()} 方法后调用此方法
	 * 
	 * @param currentPlayerState
	 *            当前音乐播放状态
	 * @param playingSong
	 *            当前播放的歌曲
	 * @param currenPlayPosition
	 *            当前已经播放到的位置
	 * @param playMode
	 *            当前播放模式（顺序播放、列表循环、单曲循环、随机播放）
	 * */
	public abstract void onServiceConnected(State currentPlayerState,
			MusicItem playingSong, int currenPlayPosition, int playMode);

	/**
	 * 在调用MusicService里的
	 * {@linkplain com.lq.service.MusicService#unregisterOnServiceConnectionListener
	 * unregisterOnServiceConnectionListener()} 方法后调用此方法
	 */
	public abstract void onServiceDisconnected();
}
