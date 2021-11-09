package com.codecanyon.streamradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.codecanyon.radio.R;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class ForegroundService extends Service {

    private SimpleExoPlayer player;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            play(this.getString(R.string.radio_url));
            showNotification(true);
        } else if(intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_PLAYER)){
            player.stop();
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            stopMediaPlayer();
            closeNotification();
            stopForeground(true);
            stopSelf();
        }
        }catch (Exception e){
            e.getMessage();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void play(final String url){
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelector trackSelector = new DefaultTrackSelector(new Handler(), new AdaptiveVideoTrackSelection.Factory(bandwidthMeter));
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl());
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "streamradio"), bandwidthMeter);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource audioSource = new ExtractorMediaSource(Uri.parse(url), dataSourceFactory, extractorsFactory, null, null);
        player.prepare(audioSource);
        player.setPlayWhenReady(true);
        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {
                if(isLoading){
                    MainActivity.startBufferingAnimation();
                }
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if(playbackState==3){
                    MainActivity.stopBufferingAnimation();
                    MusicPlayer.isStarted = true;
                }else{
                    MusicPlayer.isStarted = false;
                }
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object o) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                    MusicPlayer.setIsWorking(false);
                    try {
                        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo netInfo = cm.getActiveNetworkInfo();
                        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                            MainActivity.stopBufferingAnimation();
                            MusicPlayer.setIsWorking(false);
                        } else {
                            MainActivity.stopBufferingAnimation();
                            MusicPlayer.setIsWorking(false);
                        }
                    } catch (Exception e2) {
                        // TODO: handle exception
                    }
            }

            @Override
            public void onPositionDiscontinuity() {

            }
        });
    }


    public void showNotification(boolean autoOpen) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(this.getString(R.string.radio_name))
                .setContentText(MainScreen.getRadioListName().getText().toString())
                .setSmallIcon(R.drawable.ic_stat_transmission4)
                .setContentIntent(pendingIntent)
                .setWhen(0)
                .setOngoing(true);

        nBuilder.setContentIntent(pendingIntent);
        Notification noti = nBuilder.build();
        noti.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, noti); //notification id
    }

    public void closeNotification(){
        NotificationManager nManager = (NotificationManager) this.getSystemService(this.NOTIFICATION_SERVICE);
        nManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
    }

    public void stopMediaPlayer() {
        MusicPlayer.isStarted = false;
        try {
            player.stop();
        }catch (Exception e){
            e.getMessage();
        }
    }
}