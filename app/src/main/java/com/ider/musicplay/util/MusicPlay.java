package com.ider.musicplay.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.ider.musicplay.MusicPlayActivity;
import com.ider.musicplay.R;
import com.ider.musicplay.service.MusicPlayService;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

import static com.ider.musicplay.service.MusicPlayService.dateList;
import static com.ider.musicplay.service.MusicPlayService.myAudFocListener;

/**
 * Created by Eric on 2017/6/20.
 */

public class MusicPlay implements Serializable {

    public static NotificationManager notificationManager;

    public static Notification notification;

    public static Intent updateIntent;
    public static PendingIntent pendingIntent;
    public static String updateFile;

    public static  MediaPlayer mediaPlayer;

    public static RemoteViews remoteViews;

    public static Context context;

    public static LocalBroadcastManager localBroadcastManager;

    public static SharedPreferences preferences;

    public static List<Music> dateList;

    public static int position;

    public static Music music;

    public static final String NEXTSONG = "next_song";

    public static final String PAUSE_OR_ARROW_NOTIFY = "pause_or_arrow_notify";

    public static final String NEXTSONG_NOTIFY = "next_song_notify";

    public static final String OPEN_MUSIC_PLAY = "open_music_play";

    public static final String CLOSEAPP = "close_app";

    public static final String CANCEL_PLAY = "cancel_play";

    public static final String PAUSE_OR_ARROW = "pause_or_arrow_play";

    public static String PLAY_MODE ;

    public static final String RANDOM_PLAY = "random_play";

    public MusicPlay(Context context,List<Music> musicList, int position){
        this.context = context;
        this.dateList = musicList;
        this.position = position;
        mediaPlayer = new MediaPlayer();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        preferences = context.getSharedPreferences("music_play", Context.MODE_PRIVATE);
        PLAY_MODE = preferences.getString("play_mode", "random_play");
        music = musicList.get(position);
    }

    public static void initMediaPlayer(){
        try{
//            Log.i("MusicPlay","position："+position);
            String path = music.getMusicPath();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            context.startService(new Intent(context,AudioFocusService.class));
            MusicPlayService.audioManager.requestAudioFocus(MusicPlayService.myAudFocListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    nextSong();

                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void nextSong(){
        mediaPlayer.reset();
        String path;
        if (PLAY_MODE.equals(RANDOM_PLAY)) {
            Random random = new Random();
            int max = dateList.size();
            music = dateList.get(position=random.nextInt(max));
            path = music.getMusicPath();
        }else {
            music = dateList.get(++position);
            if (dateList==null){
                music = dateList.get(position=0);
            }
            path =music.getMusicPath();
        }
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.start();
        Intent intent = new Intent(NEXTSONG);
        context.sendBroadcast(intent);
    }

    public static void pausePlay(){
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
//            context.stopService(new Intent(context,AudioFocusService.class));
        }else {
            mediaPlayer.start();
//            context.startService(new Intent(context,AudioFocusService.class));
        }
        Intent intent = new Intent(PAUSE_OR_ARROW);
        context.sendBroadcast(intent);
    }


    public static void sendNotification(Context context) {

        notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent button1I = new Intent(PAUSE_OR_ARROW_NOTIFY);
        PendingIntent button1PI = PendingIntent.getBroadcast(context, 0, button1I, 0);
        Intent button2I = new Intent(NEXTSONG_NOTIFY);
        PendingIntent button2PI = PendingIntent.getBroadcast(context, 0, button2I, 0);
        Intent button3I = new Intent(CANCEL_PLAY);
        PendingIntent button3PI = PendingIntent.getBroadcast(context, 0, button3I, 0);
        Intent intent = new Intent(context,MusicPlayService.class);
        intent.putExtra("notify",true);
        pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        /*
         * 通知布局如果使用自定义布局文件中的话要通过RemoteViews类来实现，
         * 其实无论是使用系统提供的布局还是自定义布局，都是通过RemoteViews类实现，如果使用系统提供的布局，
         * 系统会默认提供一个RemoteViews对象。如果使用自定义布局的话这个RemoteViews对象需要我们自己创建，
         * 并且加入我们需要的对应的控件事件处理，然后通过setContent(RemoteViews remoteViews)方法传参实现
         */
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.notifacation);
        /*
         * 对于自定义布局文件中的控件通过RemoteViews类的对象进行事件处理
         */
        remoteViews.setImageViewResource(R.id.pause_music,R.drawable.ic_pause_white_24dp);
        remoteViews.setImageViewBitmap(R.id.song_cover,Utility.createAlbumArt(context,music.getMusicPath(),true));
        remoteViews.setTextViewText(R.id.music_name,music.getMusicName());
        remoteViews.setTextViewText(R.id.music_artist,music.getMusicArtist());
        remoteViews.setOnClickPendingIntent(R.id.pause_music, button1PI);
        remoteViews.setOnClickPendingIntent(R.id.next_music, button2PI);
        remoteViews.setOnClickPendingIntent(R.id.cancel_action,button3PI);

        notification = new NotificationCompat.Builder(context)
                .setContentTitle("通知2") // 创建通知的标题
                .setContentText("这是第二个通知") // 创建通知的内容
                .setSmallIcon(R.mipmap.ic_launcher) // 创建通知的小图标
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher)) // 创建通知的大图标
        /*
         * 是使用自定义视图还是系统提供的视图，上面4的属性一定要设置，不然这个通知显示不出来
         */
//                .setDefaults(Notification.DEFAULT_ALL)  // 置通知提醒方式为系统默认的提醒方式
//                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setContent(remoteViews) // 通过设置RemoteViews对象来设置通知的布局，这里我们设置为自定义布局
                .build(); // 创建通知（每个通知必须要调用这个方法来创建）
        notificationManager.notify(1, notification); // 发送通知

    }

    public static void reNewNotification(){
        if (mediaPlayer.isPlaying()){
            remoteViews.setImageViewResource(R.id.pause_music,R.drawable.ic_pause_white_48dp);
        }else {
            remoteViews.setImageViewResource(R.id.pause_music,R.drawable.ic_play_arrow_white_48dp);
        }
        remoteViews.setImageViewBitmap(R.id.song_cover,Utility.createAlbumArt(context,music.getMusicPath(),true));
        remoteViews.setTextViewText(R.id.music_name,music.getMusicName());
        remoteViews.setTextViewText(R.id.music_artist,music.getMusicArtist());
        notificationManager.notify(1, notification);
    }





}
