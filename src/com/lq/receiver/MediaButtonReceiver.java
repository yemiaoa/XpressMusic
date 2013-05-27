package com.lq.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import com.lq.service.MusicService;

/**
 * 处理媒体按键的广播接收器
 * 
 * @author lq 2013-6-1 lq2625304@gmail.com
 * */
public class MediaButtonReceiver extends BroadcastReceiver {
	private static final String TAG = MediaButtonReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			KeyEvent key = (KeyEvent) intent
					.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (key.getAction() == KeyEvent.ACTION_DOWN) {
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Context.TELEPHONY_SERVICE);
				if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
					Log.i(TAG, "OnReceive, getKeyCode = " + key.getKeyCode());

					switch (key.getKeyCode()) {
					case KeyEvent.KEYCODE_HEADSETHOOK:
						context.startService(new Intent(
								MusicService.ACTION_PLAY));
						break;
					case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
						context.startService(new Intent(
								MusicService.ACTION_PREVIOUS));
						break;
					case KeyEvent.KEYCODE_MEDIA_NEXT:
						context.startService(new Intent(
								MusicService.ACTION_NEXT));
						break;
					}
				}
			}
		}
	}
}
