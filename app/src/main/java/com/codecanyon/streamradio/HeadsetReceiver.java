package com.codecanyon.streamradio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.codecanyon.radio.R;

/**
 * Created by User on 2014.07.10..
 */
public class HeadsetReceiver extends BroadcastReceiver {
    private boolean notRunWhenStart = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (notRunWhenStart)
            notRunWhenStart = false;
        else {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                ToastCreator toastCreator = new ToastCreator(context);
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        toastCreator.show(R.drawable.volume_muted_light, "Headset unplugged");
                        try {
                            Intent startIntent = new Intent(context, ForegroundService.class);
                            startIntent.setAction(Constants.ACTION.STOPFOREGROUND_PLAYER);
                            context.startService(startIntent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 1:
                        toastCreator.show(R.drawable.headphones, "Headset plugged");
                        break;
                    default:
                }
            }
        }
    }
}
