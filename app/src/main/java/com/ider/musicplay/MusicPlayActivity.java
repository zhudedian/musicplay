package com.ider.musicplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ider.musicplay.lrc.LrcHandle;
import com.ider.musicplay.lrc.WordView;
import com.ider.musicplay.service.MusicPlayService;
import com.ider.musicplay.util.BaseActivity;
import com.ider.musicplay.util.MusicPlay;
import com.ider.musicplay.util.Utility;
import com.ider.musicplay.view.ColorSeekBar;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static com.ider.musicplay.util.MusicPlay.NEXTSONG;
import static com.ider.musicplay.util.MusicPlay.PAUSE_OR_ARROW;
import static com.ider.musicplay.util.MusicPlay.historyPosition;
import static com.ider.musicplay.util.MusicPlay.mediaPlayer;

public class MusicPlayActivity extends BaseActivity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{

    private String TAG = "MusicPlayActivity";
    private MusicPlayReceiver musicPlayReceiver;

    private WordView mWordView;
    private List<Integer> mTimeList;
    private SeekBar seekBar;
    private ColorSeekBar colorSeekBar;
    private boolean shortPress = false;
    private ScheduledExecutorService scheduledExecutor;
    private boolean isSetProgress=true;
    private boolean isLongTouch;
    private int newPosition=0,lastPosition;
    private Button play,pause,stop;
    private ImageView playMode,previous,arrow,next;
    private CircleImageView cover;
    private TextView musicName,playTime,musicDuration;
    private Animation animation;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        //Log.i("MusicPlayActivity",dateList+"dateList");
        setContentView(R.layout.activity_music_play);
        animation= AnimationUtils.loadAnimation(MusicPlayActivity.this,R.anim.image_rotate);
        LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
        animation.setInterpolator(lin);
        seekBar = (SeekBar)findViewById(R.id.playSeekBar);
        colorSeekBar = (ColorSeekBar) findViewById(R.id.color_seekbar);
        colorSeekBar.setColor(Color.BLUE,Color.WHITE,Color.BLACK,Color.RED);
        play = (Button) findViewById(R.id.play);
        pause = (Button) findViewById(R.id.pause);
        stop = (Button) findViewById(R.id.stop);
        playMode = (ImageView)findViewById(R.id.play_mode);
        previous = (ImageView) findViewById(R.id.previous);
        arrow = (ImageView) findViewById(R.id.arrow_or_pause);
        next = (ImageView) findViewById(R.id.next);
        cover = (CircleImageView) findViewById(R.id.cover);
        musicName = (TextView) findViewById(R.id.music_name);
        playTime = (TextView) findViewById(R.id.play_time);
        musicDuration = (TextView) findViewById(R.id.music_duration);
        mWordView = (WordView) findViewById(R.id.lrc_text);

        seekBar.setOnSeekBarChangeListener(this);
        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);
        playMode.setOnClickListener(this);
        previous.setOnClickListener(this);
        arrow.setOnClickListener(this);
        next.setOnClickListener(this);

        if (MusicPlay.PLAY_MODE.equals(MusicPlay.RANDOM_PLAY)){
            playMode.setImageResource(R.drawable.lockscreen_player_btn_random);

        }else if (MusicPlay.PLAY_MODE.equals(MusicPlay.ORDER_PLAY)){
            playMode.setImageResource(R.drawable.lockscreen_player_btn_repeat_normal);
        }else {
            playMode.setImageResource(R.drawable.lockscreen_player_btn_repeat_once);
        }
        registerReceiver();


        if (savedInstanceState != null){
            initView();
        }else {
            initMediaPlayer();
        }
//        registerReceiver();

    }


    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
    }

    private void registerReceiver()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NEXTSONG);
        filter.addAction(PAUSE_OR_ARROW);
        filter.addAction(MusicPlay.NEXTSONG_NOTIFY);
        filter.addAction(MusicPlay.PAUSE_OR_ARROW_NOTIFY);
        filter.addAction(MusicPlay.CANCEL_PLAY);
        registerReceiver(myReceiver, filter);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
//        Log.i("MusicPlayActivity",progress+"");
        lastPosition = progress;
        seekBar.setProgress(progress);
//        mtvstate.setText("开始拖动");
//        mtvdata.setText("当前进度数值是："+progress);

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        Log.i("MusicPlayActivity","onStartTrackingTouch");
        isSetProgress = false;
//        mtvstate.setText("开始拖动");

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
//        Log.i("MusicPlayActivity","onStopTrackingTouch");
        mediaPlayer.seekTo(lastPosition);
        isSetProgress = true;
        new Thread(){
            public void run() {
                while (isSetProgress) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mHandler.sendEmptyMessage(0);
                }
            }
        }.start();
//        mtvstate.setText("停止拖动");
    }


    private void initMediaPlayer(){
        try{
            if (MusicPlay.mediaPlayer.isPlaying()){
                arrow.setImageResource(R.drawable.ic_pause_white_48dp);
            }else {
                arrow.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            }

            isSetProgress=false;
            seekBar.setMax(mediaPlayer.getDuration());
            cover.setImageBitmap(Utility.createAlbumArt(MusicPlay.music.getMusicPath(),-1));
            musicDuration.setText(Utility.formatTime(MusicPlay.music.getMusicDuration()));
            musicName.setText(MusicPlay.music.getMusicName());
            Log.i("musicplay","musicname:"+MusicPlay.music.getMusicName()+"musicid:"+MusicPlay.music.getMusic_id()+"musicalbum_id:"+MusicPlay.music.getMusicAlbum_id());

            Log.i("musicplay","position:"+MusicPlay.position);
            //后台线程发送消息进行更新进度条
            isSetProgress = true;
            new Thread(){
                public void run() {
                    while (isSetProgress) {
                        mHandler.sendEmptyMessage(0);
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }catch (Exception e){
            e.printStackTrace();
        }
        LrcHandle.setHandler(mHandler);
        LrcHandle.findLrc(MusicPlay.music);
        mWordView.setText("正在查找歌词……");
        mTimeList=null;
        cover.startAnimation(animation);
    }
    private void nextSong(){
        MusicPlay.nextSong();
        arrow.setImageResource(R.drawable.ic_pause_white_48dp);
        isSetProgress=false;
        seekBar.setMax(mediaPlayer.getDuration());
        cover.setImageBitmap(Utility.createAlbumArt(MusicPlay.music.getMusicPath(),-1));
        musicDuration.setText(Utility.formatTime(MusicPlay.music.getMusicDuration()));
        musicName.setText(MusicPlay.music.getMusicName());
        Log.i("musicplay","musicname:"+MusicPlay.music.getMusicName()+"musicid:"+MusicPlay.music.getMusic_id()+"musicalbum_id:"+MusicPlay.music.getMusicAlbum_id());

        Log.i("musicplay",MusicPlay.position+"");
        //后台线程发送消息进行更新进度条
        isSetProgress = true;
        new Thread(){
            public void run() {
                while (isSetProgress) {
                    mHandler.sendEmptyMessage(0);
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        LrcHandle.findLrc(MusicPlay.music);
        mWordView.setText("正在查找歌词……");
        mTimeList=null;
        cover.startAnimation(animation);
    }

    private void initView(){
        seekBar.setMax(mediaPlayer.getDuration());
        cover.setImageBitmap(Utility.createAlbumArt(MusicPlay.music.getMusicPath(),-1));
        musicDuration.setText(Utility.formatTime(MusicPlay.music.getMusicDuration()));
        musicName.setText(MusicPlay.music.getMusicName());
        if (mediaPlayer.isPlaying()){
            cover.startAnimation(animation);
            arrow.setImageResource(R.drawable.ic_pause_white_48dp);
        }else {
            cover.clearAnimation();
            arrow.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        }
        LrcHandle.findLrc(MusicPlay.music);
        mWordView.setText("正在查找歌词……");
        mTimeList=null;
    }

    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicPlay.CANCEL_PLAY)){
                MusicPlay.notificationManager.cancel(1);
                Intent stopIntent = new Intent(MusicPlayActivity.this,MusicPlayService.class);
                stopService(stopIntent);
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    isSetProgress = false;
                }
                finish();
            }else {
                initView();
            }
        }
    };

    private void setBitmap(int position){
        Cursor myCur = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.ALBUM_ID}, null,null, null);
        myCur.moveToPosition(position);
        long songid = myCur.getLong(3);
        Log.i("musicplay",songid+"");
        long albumid = myCur.getLong(7);
        Log.i("musicplay",albumid+"");
        Bitmap bm = Utility.getArtwork(this, songid, albumid,true);
        if(bm != null){

            cover.setImageBitmap(bm);
        }else{

        }
    }
    //处理进度条更新
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case 0:
                    try {
                        //更新进度
                        if (isSetProgress) {
                            int position = mediaPlayer.getCurrentPosition();
//                    int time = mediaPlayer.getDuration();
//                    int max = seekBar.getMax();
                            seekBar.setProgress(position);
                            playTime.setText(Utility.formatTime(position));
                            if (mTimeList!=null){
                                for (int i = 0 ;i<mTimeList.size();i++){
                                    if (position>mTimeList.get(i)&&(i<mTimeList.size()-1)&&position<mTimeList.get(i+1)||position>mTimeList.get(mTimeList.size()-1)){
                                        mWordView.reNew(i);
                                    }
                                }
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                case 1:
                    try {
                        //更新进度
                        if (isLongTouch) {
                            if (lastPosition < mediaPlayer.getDuration()) {
                                newPosition++;
                            }
                            int position = mediaPlayer.getCurrentPosition();
                            int time = mediaPlayer.getDuration();
//                    int max = seekBar.getMax();
                            lastPosition = position + (newPosition * time / 100);
                            seekBar.setProgress(lastPosition);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    try {
                        LrcHandle.readLRC(LrcHandle.lrcPath);
                        mWordView.setText(LrcHandle.getWords());
                        mTimeList = LrcHandle.getTime();

                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    break;
                case 3:
                    mWordView.setText("未找到歌词");
                    mTimeList = null;
                    break;
                default:
                    break;
            }

        }
    };


    @Override
    public void onBackPressed(){
        if (!MusicPlay.mediaPlayer.isPlaying()){
            Utility.saveMusicToInfo(MusicPlay.music,MusicPlay.lastPlayInfo);
            MusicPlay.lastPlayInfo.setPlayMode(MusicPlay.PLAY_MODE);
            MusicPlay.lastPlayInfo.setPlayPosition(MusicPlay.mediaPlayer.getCurrentPosition());
        }
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
        finish();
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.play:
//                if (!mediaPlayer.isPlaying()){
//                    mediaPlayer.start();
//                }

//                cover.setImageBitmap(Utility.getArtwork(this,music.getMusic_id(),test,false));
//                Log.i(TAG,"test="+test);
//                test++;
                break;
            case R.id.pause:
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                }
                break;
            case R.id.stop:
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.reset();
                    initMediaPlayer();
                }
                break;
            case R.id.next:
                nextSong();
                break;
            case R.id.play_mode:
                if (MusicPlay.PLAY_MODE.equals(MusicPlay.RANDOM_PLAY)){
                    MusicPlay.PLAY_MODE = MusicPlay.ORDER_PLAY;
                    playMode.setImageResource(R.drawable.lockscreen_player_btn_repeat_normal);
                }else if (MusicPlay.PLAY_MODE.equals(MusicPlay.ORDER_PLAY)){
                    MusicPlay.PLAY_MODE= MusicPlay.SINGLE_PLAY;
                    playMode.setImageResource(R.drawable.lockscreen_player_btn_repeat_once);
                }else {
                    MusicPlay.PLAY_MODE = MusicPlay.RANDOM_PLAY;
                    playMode.setImageResource(R.drawable.lockscreen_player_btn_random);
                }
                break;
            case R.id.previous:
                if (MusicPlay.historyPosition>0){
                    MusicPlay.music = MusicPlay.historyList.get(--historyPosition);
                }
                MusicPlay.initMediaPlayer();
                MusicPlay.mediaPlayer.start();
                Intent intent = new Intent(MusicPlay.NEXTSONG);
                sendBroadcast(intent);
//                stopService(new Intent(MainActivity.this,StartService.class));
                break;
            case R.id.arrow_or_pause:
                if (mediaPlayer.isPlaying()){
                    MusicPlay.pausePlay();
                    MusicPlay.notificationManager.cancel(1);
                }else {
                    MusicPlay.pausePlay();
                    MusicPlay.sendNotification();
                }
                break;
            default:
                break;
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        isSetProgress = false;
        unregisterReceiver(myReceiver);
    }
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            shortPress = false;
            isSetProgress=false;
            isLongTouch=true;
            newPosition=0;
            new Thread(){
                public void run() {
                    while (isSetProgress) {
                        mHandler.sendEmptyMessage(0);
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
//            Toast.makeText(this, "longPress", Toast.LENGTH_LONG).show();
            return true;
        }
        //Just return false because the super call does always the same (returning false)
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            Log.i("key",keyCode+""+event);
            if(event.getAction() == KeyEvent.ACTION_DOWN){
                event.startTracking();
                if(event.getRepeatCount() == 0){
                    shortPress = true;
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if(shortPress){
//                Toast.makeText(this, "shortPress", Toast.LENGTH_LONG).show();
            } else {
                isLongTouch=false;
                isSetProgress= true;
                mediaPlayer.seekTo(lastPosition);
                new Thread(){
                    public void run() {
                        while (isSetProgress) {
                            mHandler.sendEmptyMessage(0);
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
                //Don't handle longpress here, because the user will have to get his finger back up first
            }
            shortPress = false;
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    private void updateAddOrSubtract(int viewId) {
        final int vid = viewId;
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = vid;
                handler.sendMessage(msg);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);    //每间隔100ms发送Message
    }

    private void stopAddOrSubtract() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
            scheduledExecutor = null;
        }
    }
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int viewId = msg.what;
            switch (viewId){

            }
        }
    };

    class PressThread extends Thread{

        public void run(){

            while(isSetProgress){
                mHandler.sendEmptyMessage(0);
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }

    }
    public static Bitmap setArtwork(Context context, String url, ImageView ivPic) {
        Uri selectedAudio = Uri.parse(url);
        MediaMetadataRetriever myRetriever = new MediaMetadataRetriever();
        myRetriever.setDataSource(context, selectedAudio); // the URI of audio file
        byte[] artwork;
        artwork = myRetriever.getEmbeddedPicture();
        if (artwork != null) {
            Bitmap bMap = BitmapFactory.decodeByteArray(artwork, 0, artwork.length);
            ivPic.setImageBitmap(bMap);

            return bMap;
        }
        return null;
//        else {
//            ivPic.setImageResource(R.drawable.defult_music);
//            return BitmapFactory.decodeResource(context.getResources(), R.drawable.defult_music);
//        }
    }


//    public class MySeekBar implements SeekBar.OnSeekBarChangeListener {
//
//        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//            //更新进度
//            int position = mediaPlayer.getCurrentPosition();
//            int time = mediaPlayer.getDuration();
//            int max = seekBar.getMax();
//            seekBar.setProgress(position*max/time);
//        }
//
//        /*滚动时,应当暂停后台定时器*/
//        public void onStartTrackingTouch(SeekBar seekBar) {
//
//        }
//        /*滑动结束后，重新设置值*/
//        public void onStopTrackingTouch(SeekBar seekBar) {
//            mediaPlayer.seekTo(seekBar.getProgress());
//        }
//    }
}
