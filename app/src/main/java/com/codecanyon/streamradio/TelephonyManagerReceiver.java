package com.codecanyon.streamradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * Created by Mark on 2017. 01. 19..
 */

public class TelephonyManagerReceiver extends BroadcastReceiver {

    public static boolean message = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
                if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

                    if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                        if (MusicPlayer.isStarted()) {
                            message = true;
                        }
                    }

                    if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                        if (MusicPlayer.isStarted()) {
                            message = true;
                            MusicPlayer.stopMediaPlayer();
                        }
                    }

                    if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                        if (MusicPlayer.isStarted()) {
                            message = true;
                            MusicPlayer.stopMediaPlayer();
                        }
                    }
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
