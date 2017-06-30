package com.ider.musicplay.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import com.ider.musicplay.MusicPlayActivity;
import com.ider.musicplay.util.LastPlayInfo;
import com.ider.musicplay.util.Music;
import com.ider.musicplay.util.MusicPlay;
import com.ider.musicplay.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.Serializable;
import java.util.List;

import static android.os.Build.VERSION_CODES.M;
import static com.ider.musicplay.util.MusicPlay.dataList;
import static com.ider.musicplay.util.MusicPlay.mediaPlayer;
import static com.ider.musicplay.util.MusicPlay.music;
import static com.ider.musicplay.util.MusicPlay.position;

public class MusicPlayService extends Service {

    private String TAG = "MusicPlayService";
    private LastPlayInfo lastPlayInfo;
    public static AudioManager audioManager;
    private Music music;
    public static MyOnAudioFocusChangeListener myAudFocListener;

    public MusicPlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");


    }
    @Override
    public void onCreate(){
        super.onCreate();
        audioManager = (AudioManager) getApplicationContext().getSystemService(
                Context.AUDIO_SERVICE);
        myAudFocListener = new MyOnAudioFocusChangeListener();
        audioManager.requestAudioFocus(myAudFocListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

    }
    @Override
    public int onStartCommand(Intent intent,int flags, int startId){


        if (intent!=null&&"only_play".equals(intent.getStringExtra("commend"))){
            MusicPlay.sendNotification();
            registerReceiver();
            return super.onStartCommand(intent,flags,startId);
        }
//        Log.i(TAG,music.getMusicName());
//        Log.i("MusicPlayService1",((Music)intent.getSerializableExtra("music")).getMusicPath());
        if (isTopActivity("MusicPlayActivity")){
            return super.onStartCommand(intent,flags,startId);
        }
        if (intent!=null&&intent.getBooleanExtra("notify",false)){
            startMusicPlayActivity();
            return super.onStartCommand(intent,flags,startId);
        }

        startMusicPlayActivity();
        MusicPlay.sendNotification();
        registerReceiver();
        return super.onStartCommand(intent,flags,startId);
    }
    private void registerReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlay.NEXTSONG);
        filter.addAction(MusicPlay.NEXTSONG_NOTIFY);
        filter.addAction(MusicPlay.PAUSE_OR_ARROW_NOTIFY);
        filter.addAction(MusicPlay.PAUSE_OR_ARROW_FOCUS);
        filter.addAction(MusicPlay.PAUSE_OR_ARROW);
        filter.addAction(MusicPlay.OPEN_MUSIC_PLAY);
        filter.addAction(MusicPlay.CLOSEAPP);
        filter.addAction(MusicPlay.CANCEL_PLAY);
        registerReceiver(myReceiver, filter);
    }

    private void startMusicPlayActivity(){
        Intent startIntent = new Intent(this, MusicPlayActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }
    private boolean isTopActivity(String activity)
    {
        ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        Log.i("MusicPlayService",cn.getClassName());
        return cn.getClassName().contains(activity);
    }
    @Override
    public void onDestroy(){
        LastPlayInfo lastPlayInfo = new LastPlayInfo();
        Utility.saveMusicToInfo(MusicPlay.music,lastPlayInfo);
        lastPlayInfo.setPlayMode(MusicPlay.PLAY_MODE);
        lastPlayInfo.setPlayPosition(MusicPlay.mediaPlayer.getCurrentPosition());
        DataSupport.deleteAll(LastPlayInfo.class);
        lastPlayInfo.save();
        SharedPreferences preferences = getSharedPreferences("music_play", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("have_last_play_info", true);
        editor.apply();
        MusicPlay.notificationManager.cancel(1);
        if (MusicPlay.mediaPlayer != null && MusicPlay.mediaPlayer.isPlaying()) {
            MusicPlay.mediaPlayer.stop();
            MusicPlay.mediaPlayer.release();
            MusicPlay.mediaPlayer = null;
        }
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }
    public class MyOnAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i("MusicPlayService",focusChange+"");
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN://你已经得到了音频焦点。
                    if (!MusicPlay.mediaPlayer.isPlaying()){
                        MusicPlay.mediaPlayer.start();
                        Intent intent = new Intent(MusicPlay.PAUSE_OR_ARROW_FOCUS);
                        sendBroadcast(intent);
                    }
//                    Log.i("MusicPlayService","case AudioManager.AUDIOFOCUS_GAIN:");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS://你已经失去了音频焦点很长时间了。你必须停止所有的音频播放。因为你应该不希望长时间等待焦点返回，这将是你尽可能清除你的资源的一个好地方。例如，你应该释放MediaPlayer。
//                    Log.i("MusicPlayService","case AudioManager.AUDIOFOCUS_LOSS:");
                    if (mediaPlayer.isPlaying()){
                        MusicPlay.mediaPlayer.pause();
                        Intent intent = new Intent(MusicPlay.PAUSE_OR_ARROW_FOCUS);
                        sendBroadcast(intent);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://你暂时失去了音频焦点，但很快会重新得到焦点。你必须停止所有的音频播放，但是你可以保持你的资源，因为你可能很快会重新获得焦点。
//                    Log.i("MusicPlayService","case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:");
                    if (mediaPlayer.isPlaying()){
                        MusicPlay.mediaPlayer.pause();
                        Intent intent = new Intent(MusicPlay.PAUSE_OR_ARROW_FOCUS);
                        sendBroadcast(intent);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://你暂时失去了音频焦点，但你可以小声地继续播放音频（低音量）而不是完全扼杀音频。
//                    Log.i("MusicPlayService","case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:");
                    if (mediaPlayer.isPlaying()){
                        MusicPlay.mediaPlayer.pause();
                        Intent intent = new Intent(MusicPlay.PAUSE_OR_ARROW_FOCUS);
                        sendBroadcast(intent);
                    }
                    break;
                default:
                    break;
            }
        }
    }
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicPlay.PAUSE_OR_ARROW_NOTIFY)){
                MusicPlay.pausePlay();
                MusicPlay.reNewNotification();
            }else if (action.equals(MusicPlay.PAUSE_OR_ARROW_FOCUS)){
                MusicPlay.sendNotification();
            }else if (action.equals(MusicPlay.NEXTSONG_NOTIFY)){
                MusicPlay.nextSong();
            }else if (action.equals(MusicPlay.PAUSE_OR_ARROW)){
                if (MusicPlay.mediaPlayer.isPlaying()){
                    MusicPlay.sendNotification();
                }
            }else if (action.equals(MusicPlay.OPEN_MUSIC_PLAY)){
                Intent startIntent = new Intent(MusicPlayService.this, MusicPlayActivity.class);
                startIntent.putExtra("dateList", (Serializable) dataList);
                startIntent.putExtra("position",position);

                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
            }else if (action.equals(MusicPlay.CANCEL_PLAY)){
                if (MusicPlay.mediaPlayer != null && MusicPlay.mediaPlayer.isPlaying()) {
                    MusicPlay.mediaPlayer.pause();
                }
                audioManager.abandonAudioFocus(myAudFocListener);
                stopSelf();
            }else if (action.equals(MusicPlay.CLOSEAPP)){
                Log.i(TAG,"reseive closeapp");
                if (MusicPlay.mediaPlayer!=null&&!MusicPlay.mediaPlayer.isPlaying()) {
//                    audioManager.abandonAudioFocus(myAudFocListener);
                    Intent sendIntent = new Intent(MusicPlay.CANCEL_PLAY);
                    sendBroadcast(sendIntent);
//                    stopSelf();
                }
            }else {
                MusicPlay.reNewNotification();
            }
        }
    };

}
