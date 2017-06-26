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
import android.media.MediaPlayer;
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

import com.ider.musicplay.service.MusicPlayService;
import com.ider.musicplay.util.BaseActivity;
import com.ider.musicplay.util.Music;
import com.ider.musicplay.util.MusicPlay;
import com.ider.musicplay.util.Utility;
import com.ider.musicplay.view.ColorSeekBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.ider.musicplay.util.MusicPlay.PAUSE_OR_ARROW;

public class MusicPlayActivity extends BaseActivity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{

    private String TAG = "MusicPlayActivity";
    private MusicPlay musicPlay;
    private MediaPlayer mediaPlayer;
    private MusicPlayReceiver musicPlayReceiver;
    private List<Music> musicList = new ArrayList<>();
    private List<Music> dataList = new ArrayList<>();
    private Music music;
    private int position;
    private SeekBar seekBar;
    private ColorSeekBar colorSeekBar;
    private boolean shortPress = false;
    private ScheduledExecutorService scheduledExecutor;
    private boolean isSetProgress=true;
    private boolean isLongTouch;
    private int newPosition=0,lastPosition;
    private Button play,pause,stop;
    private ImageView previous,arrow,next;
    private CircleImageView cover;
    private TextView musicName,playTime,musicDuration;
    private Animation animation;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        position = intent.getIntExtra("position",0);
        dataList = (List<Music>) intent.getSerializableExtra("dataList");
        music = (Music)intent.getSerializableExtra("music");
        musicPlay = (MusicPlay)intent.getSerializableExtra("musicplay");
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
        previous = (ImageView) findViewById(R.id.previous);
        arrow = (ImageView) findViewById(R.id.arrow_or_pause);
        next = (ImageView) findViewById(R.id.next);
        cover = (CircleImageView) findViewById(R.id.cover);
        musicName = (TextView) findViewById(R.id.music_name);
        playTime = (TextView) findViewById(R.id.play_time);
        musicDuration = (TextView) findViewById(R.id.music_duration);
        seekBar.setOnSeekBarChangeListener(this);
        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);
        previous.setOnClickListener(this);
        arrow.setOnClickListener(this);
        next.setOnClickListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(musicPlay.NEXTSONG);
        filter.addAction(PAUSE_OR_ARROW);
        filter.addAction(MusicPlay.NEXTSONG_NOTIFY);
        filter.addAction(MusicPlay.PAUSE_OR_ARROW_NOTIFY);
        filter.addAction(MusicPlay.CANCEL_PLAY);
        registerReceiver(myReceiver, filter);
        if (savedInstanceState != null){
            musicPlay = (MusicPlay) savedInstanceState.getSerializable("music_play");
            initView();
        }else {
            initMediaPlayer();
        }
//        registerReceiver();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putSerializable("music_play",musicPlay);
    }

    private void registerReceiver()
    {
        musicPlayReceiver = new MusicPlayReceiver();
        IntentFilter intentFilter = new IntentFilter(MusicPlayReceiver.ACTION_1);
        intentFilter.addAction(MusicPlayReceiver.ACTION_2);
        registerReceiver(musicPlayReceiver, intentFilter);
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
            arrow.setImageResource(R.drawable.ic_pause_white_48dp);
            isSetProgress=false;
//            musicPlay.initMediaPlayer();
            mediaPlayer = musicPlay.mediaPlayer;
            seekBar.setMax(mediaPlayer.getDuration());
            cover.setImageBitmap(Utility.createAlbumArt(this,music.getMusicPath(),false));
            musicDuration.setText(Utility.formatTime(music.getMusicDuration()));
            musicName.setText(music.getMusicName());
            Log.i("musicplay","musicname:"+music.getMusicName()+"musicid:"+music.getMusic_id()+"musicalbum_id:"+music.getMusicAlbum_id());

            Log.i("musicplay","position:"+position);
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

        cover.startAnimation(animation);
    }
    private void nextSong(){
        musicPlay.nextSong();
        arrow.setImageResource(R.drawable.ic_pause_white_48dp);
        isSetProgress=false;
        music = musicPlay.music;
        mediaPlayer = musicPlay.mediaPlayer;
        seekBar.setMax(mediaPlayer.getDuration());
        cover.setImageBitmap(Utility.createAlbumArt(this,music.getMusicPath(),false));
        musicDuration.setText(Utility.formatTime(music.getMusicDuration()));
        musicName.setText(music.getMusicName());
        Log.i("musicplay","musicname:"+music.getMusicName()+"musicid:"+music.getMusic_id()+"musicalbum_id:"+music.getMusicAlbum_id());

        Log.i("musicplay",position+"");
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

        cover.startAnimation(animation);
    }

    private void initView(){
        music = musicPlay.music;
        mediaPlayer = musicPlay.mediaPlayer;
        seekBar.setMax(mediaPlayer.getDuration());
        cover.setImageBitmap(Utility.createAlbumArt(this,music.getMusicPath(),false));
        musicDuration.setText(Utility.formatTime(music.getMusicDuration()));
        musicName.setText(music.getMusicName());
        if (mediaPlayer.isPlaying()){
            cover.startAnimation(animation);
            arrow.setImageResource(R.drawable.ic_pause_white_48dp);
        }else {
            cover.clearAnimation();
            arrow.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        }
    }

    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicPlay.CANCEL_PLAY)){
                musicPlay.notificationManager.cancel(1);
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
                default:
                    break;
            }

        }
    };


    @Override
    public void onBackPressed(){
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
            case R.id.previous:

//                stopService(new Intent(MainActivity.this,StartService.class));
                break;
            case R.id.arrow_or_pause:
                if (mediaPlayer.isPlaying()){
                    musicPlay.pausePlay();
                    musicPlay.notificationManager.cancel(1);
                }else {
                    musicPlay.pausePlay();
                    musicPlay.reNewNotification();
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
        Log.i("key",keyCode+""+event);
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
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