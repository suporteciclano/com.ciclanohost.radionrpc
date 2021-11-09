package com.codecanyon.streamradio;

import android.content.Context;
import android.content.Intent;

import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by User on 2014.07.03..
 */
public class MusicPlayer {
    public static boolean isStarted = false;
    private static String trackTitle = "";
    private static String radioName = "";
    private static Context context;
    private RadioListElement radioListElement;
    private Timer timer = new Timer();
    private boolean timerIndicator = false;

    public static boolean isWorking() {
        return isWorking;
    }

    public static void setIsWorking(boolean isWorking) {
        MusicPlayer.isWorking = isWorking;
    }

    private static boolean isWorking = true;

    public static String getRadioName() {
        return radioName;
    }

    public static String getTrackTitle() {
        return trackTitle;
    }

    public static boolean isStarted() {
        return isStarted;
    }

    public static void stopMediaPlayer() {
        isStarted = false;
        Intent stopIntent = new Intent(context, ForegroundService.class);
        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_PLAYER);
        context.startService(stopIntent);
    }

    public void play(RadioListElement rle) {
        startThread();
        TelephonyManagerReceiver.message = false;
        isWorking = true;
        isStarted = true;
        radioListElement = rle;
        context = radioListElement.getContext();
        MainActivity.setViewPagerSwitch();
        Intent startIntent = new Intent(context, ForegroundService.class);
        startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        context.startService(startIntent);
        radioListElement.getName();
        radioName = radioListElement.getName();
    }
    public void startThread() {
        if (!timerIndicator) {
            timerIndicator = true;
            timer.schedule(new TimerTask() {
                public void run() {
                    if (isStarted) {
                        URL url;
                        try {
                            url = new URL(radioListElement.getUrl());
                            IcyStreamMeta icy = new IcyStreamMeta(url);
                            if (icy.getArtist().length() > 0 && icy.getTitle().length() > 0) {
                                String title = icy.getArtist() + " - " + icy.getTitle();
                                trackTitle = new String(title.getBytes("ISO-8859-1"), "UTF-8");
                            } else {
                                String title = icy.getArtist() + "" + icy.getTitle();
                                trackTitle = new String(title.getBytes("ISO-8859-1"), "UTF-8");
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }, 0, 1000);
        }
    }
}
