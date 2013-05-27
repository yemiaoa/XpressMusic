package com.lq.entity;

/**
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class MemberInfo {
	private int member_id;
	private long audio_id;
	private long playlist_id;
	private long play_order;

	public int getMemberId() {
		return member_id;
	}

	public void setMemberId(int member_id) {
		this.member_id = member_id;
	}

	public long getAudioId() {
		return audio_id;
	}

	public void setAudioId(long audio_id) {
		this.audio_id = audio_id;
	}

	public long getPlaylistId() {
		return playlist_id;
	}

	public void setPlaylistId(long playlist_id) {
		this.playlist_id = playlist_id;
	}

	public long getPlayOrder() {
		return play_order;
	}

	public void setPlayOrder(long play_order) {
		this.play_order = play_order;
	}
}
