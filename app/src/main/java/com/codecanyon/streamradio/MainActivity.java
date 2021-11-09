package com.codecanyon.streamradio;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

import com.codecanyon.radio.R;

public class MainActivity extends FragmentActivity {
    private HeadsetReceiver headsetReceiver;
    private static DataManager dataManager;
    private static TableLayout UIRadioList;
    private static ArrayList<String> userRadios = new ArrayList<String>();
    private static ViewPager viewPager;
    private static ImageView bufferingIndicator, speaker;
    private static LoadingAnimation bufferingAnimation;
    private static AudioManager audioManager;
    private static TextView radioListLocation;
    private static TextView radioListName;
    private static TextView radioTitle;
    private static boolean first = true;
    private static NotificationPanel nPanel;
    private ImageView screenChaneButton, plus;
    private boolean runOnce = true;
    private LinearLayout volumeLayout, volumeButton;
    private int volumeStore;
    private AdView adView;
    private Typeface fontRegular;
    private WebView twitter;
    protected static boolean notificationWhile = true;
    private boolean active = true;
    private String last = "";

    public static String title = "";
    private static boolean exit = false;
    public static int page=1;
    public static int pos=0;
    private boolean reStartAnimation = false;

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
        notificationWhile = true;
        if(exit){
            MusicPlayer.setIsWorking(false);
            notificationWhile = true;
            trackDetection();
            last = "";
        }
    }

    @Override
    protected void onStop() {
        active = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //NotificationPanel.notificationCancel();
        exit = true;
        super.onDestroy();
    }

    public static void newNotification(String notText, boolean status) {
        try {
            nPanel.showNotification(notText, status);
        }catch (Exception e){
            e.getMessage();
        }
    }

    public static void radioListRefresh() {
        dataManager.createRadioListForRadioScreen(UIRadioList, userRadios, radioListName, radioListLocation);
    }

    public static void setViewPagerSwitch() {
        viewPager.setCurrentItem(0, true);
    }

    public static void startBufferingAnimation() {
        bufferingIndicator = MainScreen.getLoadingImage();
        bufferingAnimation = new LoadingAnimation(bufferingIndicator);
        bufferingAnimation.startAnimation();
    }

    public static void stopBufferingAnimation() {
        bufferingIndicator = MainScreen.getLoadingImage();
        bufferingAnimation.clearAnimation();
    }

    public static TextView getRadioListName() {
        return radioListName;
    }


    public static TextView getRadioListLocation() {
        return radioListLocation;
    }

    public static void nextPage() {
        viewPager.setCurrentItem(1, true);
    }

    public static void play(String location) {
        try {
            exit = false;
            if (first) {
                radioListLocation.setText(location);
                title = location;
                RadioList.nextOrPreviousRadioStation(1, radioListLocation, radioListName);
                if (!MusicPlayer.isStarted()) {
                    RadioList.nextOrPreviousRadioStation(0, radioListLocation, radioListName);
                }
                first = false;
            } else {
                if (MusicPlayer.isStarted()) {
                    MusicPlayer.stopMediaPlayer();
                } else if (LoadingAnimation.hasEnded()) {
                    try {
                        RadioList.nextOrPreviousRadioStation(0, radioListLocation, radioListName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        permissions();
        try{
            headsetReceiver = new HeadsetReceiver();
            registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            registerReciver();
        }catch (Exception e){
            e.getMessage();
        }
        dataManager = new DataManager(this);
        fontRegular = Typeface.createFromAsset(getAssets(), "fonts/font.otf");
        radioTitle = (TextView) findViewById(R.id.radioTitle);
        radioTitle.setTypeface(fontRegular);

        plus = (ImageView) findViewById(R.id.plus);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notificationWhile = false;
                try {
                    MusicPlayer.stopMediaPlayer();
                } catch (Exception e) {
                    e.getMessage();
                }
                NotificationPanel.notificationCancel();
                exit = true;

                Intent stopIntent = new Intent(getApplicationContext(), ForegroundService.class);
                stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                getApplicationContext().startService(stopIntent);

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        TabPagerAdapter tabPageAdapter = new TabPagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(tabPageAdapter);

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
                pos = i2;
            }

            @Override
            public void onPageSelected(int i) {
                if (viewPager.getCurrentItem() == 0) {
                    radioTitle.setText(getResources().getString(R.string.title_1));
                    page = 1;
                    radioTitle.setTextColor(Color.parseColor("#7B000000"));
                    screenChaneButton.setImageResource(R.drawable.switch_page);
                    adView.setVisibility(View.INVISIBLE);
                    if (reStartAnimation) {
                        reStartAnimation = false;
                        if (title.length() == 0) {
                            MainScreen.setRadioListName(getResources().getString(R.string.welcome_small));
                        } else {
                            MainScreen.setRadioListName(title);
                        }
                    }
                } else if (viewPager.getCurrentItem() == 1) {
                    page = 2;
                    radioTitle.setText(getResources().getString(R.string.title_2));
                    radioTitle.setTextColor(Color.parseColor("#FF4C82C0"));
                    screenChaneButton.setImageResource(R.drawable.back);
                    if (Boolean.parseBoolean(getResources().getString(R.string.admob_true_or_false)))
                        adView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        screenChaneButton = (ImageView) findViewById(R.id.nextScreen);
        screenChaneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewPager.getCurrentItem() == 0) viewPager.setCurrentItem(1, true);
                else viewPager.setCurrentItem(0, true);
            }
        });
        speaker = (ImageView) findViewById(R.id.speaker);
        speaker.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeStore, volumeStore);
                    defaultVolumeBarPosition(audioManager, volumeLayout, volumeButton);
                } else {
                    volumeStore = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    defaultVolumeBarPosition(audioManager, volumeLayout, volumeButton);
                }
                return false;
            }
        });
        volumeLayout = (LinearLayout) findViewById(R.id.linearLayout_t);
        volumeButton = (LinearLayout) findViewById(R.id.button_t);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        adView = (AdView) this.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setVisibility(View.INVISIBLE);
    }

            @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
            defaultVolumeBarPosition(audioManager, volumeLayout, volumeButton);
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
            defaultVolumeBarPosition(audioManager, volumeLayout, volumeButton);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
            defaultVolumeBarPosition(audioManager, volumeLayout, volumeButton);
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
            defaultVolumeBarPosition(audioManager, volumeLayout, volumeButton);
        else if (keyCode == KeyEvent.KEYCODE_BACK){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            }
        else if (keyCode == KeyEvent.KEYCODE_HOME){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if (runOnce) {
            //mainView = (FrameLayout) findViewById(R.id.mainView);
            //mainView.setBackgroundDrawable(ob);
            UIRadioList = (TableLayout) findViewById(R.id.radioListUi);
            radioListName = (TextView) findViewById(R.id.mainRadioName);
            radioListLocation = (TextView) findViewById(R.id.mainRadioLocation);
            if(title.length()==0){
                MainScreen.setRadioListName(getResources().getString(R.string.welcome_small));
            }else{
                MainScreen.setRadioListName(title);
            }
            radioListRefresh();
            volumeBarReaction(volumeLayout, volumeButton, audioManager);
            connectionDialog(isOnline());
            trackDetection();
            nPanel = new NotificationPanel(this);
            nPanel.showNotification(this.getResources().getString(R.string.radio_name), true);
            twitter = (WebView) findViewById(R.id.webView);
            twitter.setWebViewClient(new WebViewClient());
            twitter.getSettings().setJavaScriptEnabled(true);
            twitter.loadUrl(getResources().getString(R.string.web_url));
            twitter.refreshDrawableState();
            runOnce = false;

            if(!MusicPlayer.isStarted()){
                if(Boolean.parseBoolean(this.getResources().getString(R.string.autostart_true_or_false))){
                    play(this.getResources().getString(R.string.radio_location));
                }
            }
        }
        defaultVolumeBarPosition(audioManager, volumeLayout, volumeButton);
    }

    public void defaultVolumeBarPosition(AudioManager audioManager, LinearLayout volumeLayout, LinearLayout volumeButton) {
        float actual = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float endPoint = volumeLayout.getWidth() - volumeButton.getWidth();
        volumeButton.setX((endPoint / max * actual));
        if (volumeButton.getX() == 0)
            speaker.setImageResource(R.drawable.volume_muted);
        else speaker.setImageResource(R.drawable.volume_on);
    }

    public void volumeBarReaction(final LinearLayout volumeLayout, final LinearLayout volumeButton, final AudioManager audioManager) {

        volumeLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                float endPoint = volumeLayout.getWidth() - volumeButton.getWidth();
                volumeButton.setX(motionEvent.getX() - volumeButton.getWidth() / 2);

                if (volumeButton.getX() >= 0) {
                    float pos = volumeButton.getX() / (endPoint / max);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) pos, 0);
                }
                if (volumeButton.getX() >= endPoint) {
                    volumeButton.setX(endPoint);
                }
                if (volumeButton.getX() <= 0) {
                    volumeButton.setX(0);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    speaker.setImageResource(R.drawable.volume_muted);
                } else speaker.setImageResource(R.drawable.volume_on);
                return true;
            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else return false;
    }

    public void connectionDialog(boolean isOnline) {
        if (!isOnline) {
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.without_internet);
            Typeface fontRegular = Typeface.createFromAsset(this.getAssets(), "fonts/font.otf");
            TextView dialogTitle = (TextView) dialog.findViewById(R.id.dialogTitle);
            TextView dialogText = (TextView) dialog.findViewById(R.id.dialogQuestion);
            TextView dialogButton = (TextView) dialog.findViewById(R.id.retry);
            dialogTitle.setTypeface(fontRegular);
            dialogText.setTypeface(fontRegular);
            dialogButton.setTypeface(fontRegular);

            FrameLayout mainLayout = (FrameLayout) findViewById(R.id.mainLayout);
            dialog.getWindow().setLayout((int) (mainLayout.getWidth() * 0.8), (int) (mainLayout.getHeight() * 0.35));
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            Button retryBtn = (Button) dialog.getWindow().findViewById(R.id.retry);
            retryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!isOnline()) {
                        dialog.dismiss();
                        dialog.show();
                    } else {
                        dialog.dismiss();
                        twitter.loadUrl(getResources().getString(R.string.web_url));
                    }
                }
            });
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    if (!isOnline()) {
                        dialog.dismiss();
                        dialog.show();
                    } else
                        twitter.loadUrl(getResources().getString(R.string.web_url));
                }
            });
            dialog.show();
        }
    }

    public void trackDetection() {
           new Thread() {
            @Override
            public void run() {
                while (notificationWhile) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!TelephonyManagerReceiver.message){
                                if(MusicPlayer.isWorking()){
                                    if (!MusicPlayer.getTrackTitle().trim().toString().equals("")) {
                                        title = MusicPlayer.getTrackTitle().trim().toString();
                                        if(title.contains("~+") || title.contains("ad|")){
                                            title = "COMMERCIAL BREAK";
                                        }
                                        if (title.length() > 0 && title.charAt(title.length()-1)=='-') {
                                            title = title.substring(0, title.length()-1).trim();
                                        }

                                        String[] parts = title.split("\\(");
                                        if(parts.length==2){
                                            MainScreen.setRadioListName(parts[0].toString() + "\n(" + parts[1]);
                                            title = parts[0].toString()+"\n("+parts[1];
                                        }
                                        else
                                            MainScreen.setRadioListName(title);
                                        if(!last.equals(title)) {
                                            MainActivity.newNotification(getResources().getString(R.string.radio_name), active);
                                            last = title;
                                        }
                                    }else if (MusicPlayer.getTrackTitle().trim().toString().equals("") && MusicPlayer.isStarted() && !radioListLocation.getText().toString().equals(getResources().getString(R.string.radio_location))) {
                                        title = getResources().getString(R.string.radio_location);
                                        MainScreen.setRadioListName(title);
                                        MainActivity.newNotification(getResources().getString(R.string.radio_name), active);
                                    }
                                }
                                else {
                                    if (!MusicPlayer.isWorking() && !exit) {
                                        title = getResources().getString(R.string.radio_offline);
                                        MainScreen.setRadioListName(title);
                                        MainActivity.newNotification(getResources().getString(R.string.radio_name), active);
                                    }else{
                                        //title = getResources().getString(R.string.welcome_small);
                                        MainScreen.setRadioListName(title);
                                        MainActivity.newNotification(getResources().getString(R.string.radio_name), active);
                                    }
                                }
                            }else{
                                MainScreen.setRadioListName(getResources().getString(R.string.resume));
                                MainActivity.newNotification(getResources().getString(R.string.radio_name), active);
                            }
                        }
                    });
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(exit==true){
                    System.exit(1);
                }
            }
        }.start();
    }
    public  boolean permissions() {
        //cheeck id device is Android M
        if (Build.VERSION.SDK_INT >= 23) {
            if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
                return false;
            }
        }
        else {
            //permission is automatically granted on devices lower than android M upon installation
            return true;
        }
    }

    public void registerReciver(){
        try{
                headsetReceiver = new HeadsetReceiver();
                registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        }catch (Exception e){

        }
    }
}


