package com.ider.musicplay.service;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import com.ider.musicplay.MusicPlayActivity;
import com.ider.musicplay.util.Music;
import com.ider.musicplay.util.MusicPlay;

import java.io.Serializable;
import java.util.List;

import static com.ider.musicplay.util.MusicPlay.mediaPlayer;

public class MusicPlayService extends Service {
    private MusicPlay musicPlay;
    public static List<Music> dateList;
    public static AudioManager audioManager;
    public static MyOnAudioFocusChangeListener myAudFocListener;
    private Music music;
    private int position;
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
//        Log.i("MusicPlayService1",((Music)intent.getSerializableExtra("music")).getMusicPath());
        if (isTopActivity("MusicPlayActivity")){
            return super.onStartCommand(intent,flags,startId);
        }
        if (intent.getBooleanExtra("notify",false)){
            Intent startIntent = new Intent(this, MusicPlayActivity.class);
            startIntent.putExtra("dateList", (Serializable) musicPlay.dateList);
            startIntent.putExtra("position",musicPlay.position);
            startIntent.putExtra("musicplay",musicPlay);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
            return super.onStartCommand(intent,flags,startId);
        }
        if (music!=null&&!intent.getSerializableExtra("music").equals(music)){
            musicPlay.mediaPlayer.reset();
            dateList = (List<Music>) intent.getSerializableExtra("dateList");
            position = intent.getIntExtra("position", 0);
            music = dateList.get(position);
            musicPlay = new MusicPlay(this, dateList, position);
            musicPlay.initMediaPlayer();
            musicPlay.sendNotification(this);
        }else if (music==null){
            dateList = (List<Music>) intent.getSerializableExtra("dateList");
            position = intent.getIntExtra("position", 0);
            music = dateList.get(position);
            musicPlay = new MusicPlay(this, dateList, position);
            musicPlay.initMediaPlayer();
            musicPlay.sendNotification(this);
        }
        Log.i("MusicPlayService","position:"+position);
        Log.i("MusicPlayService",music.getMusicPath());
        Intent startIntent = new Intent(this, MusicPlayActivity.class);
        startIntent.putExtra("dateList", (Serializable) dateList);
        startIntent.putExtra("position",position);
        startIntent.putExtra("musicplay",musicPlay);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlay.NEXTSONG);
        filter.addAction(MusicPlay.NEXTSONG_NOTIFY);
        filter.addAction(MusicPlay.PAUSE_OR_ARROW_NOTIFY);
        filter.addAction(MusicPlay.OPEN_MUSIC_PLAY);
        filter.addAction(MusicPlay.CLOSEAPP);
        filter.addAction(MusicPlay.CANCEL_PLAY);
        registerReceiver(myReceiver, filter);
        return super.onStartCommand(intent,flags,startId);
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
        super.onDestroy();
        musicPlay.notificationManager.cancel(1);
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        unregisterReceiver(myReceiver);
    }
    public class MyOnAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i("MusicPlayService",focusChange+"");
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN://你已经得到了音频焦点。
                    if (!musicPlay.mediaPlayer.isPlaying()){
                        Intent intent = new Intent(MusicPlay.PAUSE_OR_ARROW_NOTIFY);
                        sendBroadcast(intent);
                    }
//                    Log.i("MusicPlayService","case AudioManager.AUDIOFOCUS_GAIN:");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS://你已经失去了音频焦点很长时间了。你必须停止所有的音频播放。因为你应该不希望长时间等待焦点返回，这将是你尽可能清除你的资源的一个好地方。例如，你应该释放MediaPlayer。
//                    Log.i("MusicPlayService","case AudioManager.AUDIOFOCUS_LOSS:");
                    if (musicPlay.mediaPlayer.isPlaying()){
                        Intent intent = new Intent(MusicPlay.PAUSE_OR_ARROW_NOTIFY);
                        sendBroadcast(intent);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://你暂时失去了音频焦点，但很快会重新得到焦点。你必须停止所有的音频播放，但是你可以保持你的资源，因为你可能很快会重新获得焦点。
//                    Log.i("MusicPlayService","case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:");
                    if (musicPlay.mediaPlayer.isPlaying()){
                        Intent intent = new Intent(MusicPlay.PAUSE_OR_ARROW_NOTIFY);
                        sendBroadcast(intent);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://你暂时失去了音频焦点，但你可以小声地继续播放音频（低音量）而不是完全扼杀音频。
//                    Log.i("MusicPlayService","case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:");
                    if (musicPlay.mediaPlayer.isPlaying()){
                        Intent intent = new Intent(MusicPlay.PAUSE_OR_ARROW_NOTIFY);
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
                musicPlay.pausePlay();
                musicPlay.reNewNotification();
            }else if (action.equals(MusicPlay.NEXTSONG_NOTIFY)){
                musicPlay.nextSong();
            }else if (action.equals(MusicPlay.OPEN_MUSIC_PLAY)){
                Intent startIntent = new Intent(MusicPlayService.this, MusicPlayActivity.class);
                startIntent.putExtra("dateList", (Serializable) dateList);
                startIntent.putExtra("position",position);
                startIntent.putExtra("musicplay",musicPlay);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
            }else if (action.equals(MusicPlay.CANCEL_PLAY)){
                audioManager.abandonAudioFocus(myAudFocListener);
                stopSelf();
            }else if (action.equals(MusicPlay.CLOSEAPP)){
                if (!musicPlay.mediaPlayer.isPlaying()) {
                    audioManager.abandonAudioFocus(myAudFocListener);
                    stopSelf();
                }
            }else {
                musicPlay.reNewNotification();
            }
        }
    };

}
