package com.ider.musicplay;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ider.musicplay.popu.PopuUtils;
import com.ider.musicplay.popu.PopupDialog;
import com.ider.musicplay.popu.Popus;
import com.ider.musicplay.service.MusicPlayService;
import com.ider.musicplay.util.BaseActivity;
import com.ider.musicplay.util.FindMusic;
import com.ider.musicplay.util.LastPlayInfo;
import com.ider.musicplay.util.Music;
import com.ider.musicplay.util.MusicPlay;
import com.ider.musicplay.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends BaseActivity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{


    private String TAG = "MainActivity";
    private Context context;
    private TextView notice, musicnum ,nowTime,musicName,musicArtist;
    private ImageView fresh,pauseMusic,nextMusic;
    private CircleImageView songCover;
    private ProgressBar progressBar;
    private MusicAdapter adapter;
    private ListView listView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isFreshing=false;
    private int lastPosition;
    private LinearLayout lastMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.activity_music);
        ActionBar actionBar= getSupportActionBar();
        if (actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_list_white_24dp);
        }
        lastMusic = (LinearLayout)findViewById(R.id.last_music);
        songCover = (CircleImageView)findViewById(R.id.song_cover);
        musicName = (TextView) findViewById(R.id.music_name);
        musicArtist = (TextView) findViewById(R.id.music_artist);
        pauseMusic = (ImageView)findViewById(R.id.pause_music);
        nextMusic = (ImageView)findViewById(R.id.next_music);
        fresh = (ImageView) findViewById(R.id.fresh);
        progressBar = (ProgressBar)findViewById(R.id.progress_bar) ;
        notice = (TextView) findViewById(R.id.hav_no_music);
        musicnum= (TextView) findViewById(R.id.music_num);
        navigationView = (NavigationView) findViewById(R.id.nav_view);

        fresh.setOnClickListener(this);
        notice.setOnClickListener(this);
        lastMusic.setOnClickListener(this);
        pauseMusic.setOnClickListener(this);
        nextMusic.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.music_list);
        adapter = new MusicAdapter(MainActivity.this,R.layout.music_list_item, MusicPlay.dataList);
        listView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }else {
            initList();
        }

        registerReceiver();

        navigationView.setCheckedItem(R.id.nav_time);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(MenuItem item){
                switch (item.getItemId()){
                    case R.id.nav_time:
                        showDropDownPopupDialog();
                        break;
                }
                return true;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position,long id){
                if (!MusicPlay.dataList.get(position).equals(MusicPlay.music)){
                    MusicPlay.music = MusicPlay.dataList.get(position);
                    String path = MusicPlay.music.getMusicPath();
                    if (new File(path).exists()){
                        MusicPlay.position = position;
                        MusicPlay.historyList.add(MusicPlay.music);
                        MusicPlay.historyPosition = MusicPlay.historyList.size()-1;
                        MusicPlay.initMediaPlayer();
                        Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                        startIntent.putExtra("commend","open_play");
                        startService(startIntent);
                        MusicPlay.mediaPlayer.start();
                    }else {
                        Toast.makeText(MainActivity.this,"该音乐不存在！",Toast.LENGTH_SHORT).show();
                    }
                }else if (MusicPlay.dataList.get(position).equals(MusicPlay.music)&&!MusicPlay.mediaPlayer.isPlaying()){
                    String path = MusicPlay.music.getMusicPath();
                    if (new File(path).exists()){
                        MusicPlay.position = position;
                        MusicPlay.initMediaPlayer();
                        MusicPlay.mediaPlayer.seekTo(MusicPlay.lastPlayInfo.getPlayPosition());
                        Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                        startIntent.putExtra("commend","open_play");
                        startService(startIntent);
                        MusicPlay.mediaPlayer.start();
                    }else {
                        Toast.makeText(MainActivity.this,"该音乐不存在！",Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                    startIntent.putExtra("commend","open_play");
                    startService(startIntent);
                }



            }
        });

Log.i(TAG,"Environment.getExternalStorageDirectory().getAbsolutePath() = "+ Environment.getExternalStorageDirectory().getAbsolutePath());

    }
    private void initList(){
        SharedPreferences preferences = getSharedPreferences("music_play", Context.MODE_PRIVATE);
        boolean isDataSave = preferences.getBoolean("data_save", false);
//        Log.i("musicplay",firstIn+"");
        if (isDataSave&&!FindMusic.isScaning) {
            FindMusic.findFromDataSupport(MusicPlay.dataList);
        }
        if (MusicPlay.dataList.size()>0){
            initView();
        }else {
            queryMusic();
        }
    }
    private void queryMusic(){
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        lastMusic.setVisibility(View.GONE);
        FindMusic.findFromMedia();
        new Thread(){
            public void run() {
                boolean isNotEnd = true;
                while (isNotEnd) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!FindMusic.isScaning) {
                        isNotEnd =false;
                        mHandler.sendEmptyMessage(1);
                    }
                }
            }
        }.start();
    }
    private void initView(){
        if (MusicPlay.dataList.size()>0){
            SharedPreferences preferences = getSharedPreferences("music_play", Context.MODE_PRIVATE);
            boolean haveLastPlayInfo = preferences.getBoolean("have_last_play_info", false);
            if (haveLastPlayInfo){
                MusicPlay.lastPlayInfo  = DataSupport.findLast(LastPlayInfo.class);
            }
            if (MusicPlay.lastPlayInfo!=null){
                MusicPlay.music = new Music();
                Utility.getMusicFromInfo(MusicPlay.lastPlayInfo,MusicPlay.music);
                MusicPlay.PLAY_MODE = MusicPlay.lastPlayInfo.getPlayMode();
                MusicPlay.historyList.add(MusicPlay.music);
                lastMusic.setVisibility(View.VISIBLE);
                songCover.setImageBitmap(Utility.createAlbumArt(MusicPlay.music.getMusicPath(),6));
                musicName.setText(MusicPlay.music.getMusicName());
                musicArtist.setText(MusicPlay.music.getMusicArtist());
                if (!MusicPlay.mediaPlayer.isPlaying()){
                    pauseMusic.setImageResource(R.drawable.ic_play_arrow_white_48dp);
                }else {
                    pauseMusic.setImageResource(R.drawable.ic_pause_white_48dp);
                }
            }else {
                lastMusic.setVisibility(View.GONE);
            }
            notice.setVisibility(View.GONE);
            musicnum.setText(MusicPlay.dataList.size()+"首");
            Log.i("musicplay","dateList.size()="+MusicPlay.dataList.size());
            adapter.notifyDataSetChanged();
            for (int i =0;i<MusicPlay.dataList.size();i++){
                Music music = MusicPlay.dataList.get(i);
                if (music.equals(MusicPlay.music)){
                    MusicPlay.position = i;
                    Log.i(TAG,"position="+MusicPlay.position);
                }
            }

        }else {
            notice.setVisibility(View.VISIBLE);
        }

    }

    private void reNewView(){
        if (MusicPlay.music!=null){
            lastMusic.setVisibility(View.VISIBLE);
            songCover.setImageBitmap(Utility.createAlbumArt(MusicPlay.music.getMusicPath(),6));
            musicName.setText(MusicPlay.music.getMusicName());
            musicArtist.setText(MusicPlay.music.getMusicArtist());
            if (!MusicPlay.mediaPlayer.isPlaying()){
                pauseMusic.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            }else {
                pauseMusic.setImageResource(R.drawable.ic_pause_white_48dp);
            }
        }else {
            lastMusic.setVisibility(View.GONE);
        }


        adapter.notifyDataSetChanged();
    }
    private void registerReceiver()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlay.NEXTSONG);
        registerReceiver(myReceiver, filter);
    }
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicPlay.NEXTSONG)){
               reNewView();
            }
        }
    };

    @Override
    public void onClick(View view){
        adapter.notifyDataSetChanged();
        switch (view.getId()){
            case R.id.hav_no_music:
                initList();
                break;
            case R.id.fresh:
                if (!isFreshing){
                    isFreshing = true;
                    queryMusic();
                }
                break;
           case R.id.last_music:
               if (!MusicPlay.mediaPlayer.isPlaying()){
                   String path = MusicPlay.music.getMusicPath();
                   if (new File(path).exists()){
                       MusicPlay.initMediaPlayer();
                       MusicPlay.mediaPlayer.seekTo(MusicPlay.lastPlayInfo.getPlayPosition());
                       Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                       startIntent.putExtra("commend","open_play");
                       startService(startIntent);
                   }else {
                       Toast.makeText(MainActivity.this,"该音乐不存在！",Toast.LENGTH_SHORT).show();
                   }
               }else {
                   Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                   startIntent.putExtra("commend","open_play");
                   startService(startIntent);
               }
               break;
            case R.id.pause_music:
                if (!MusicPlay.mediaPlayer.isPlaying()){
                    String path = MusicPlay.music.getMusicPath();
                    if (new File(path).exists()){
                        MusicPlay.initMediaPlayer();
                        MusicPlay.mediaPlayer.seekTo(MusicPlay.lastPlayInfo.getPlayPosition());
                        pauseMusic.setImageResource(R.drawable.ic_pause_white_48dp);
                        Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                        startIntent.putExtra("commend","only_play");
                        startService(startIntent);
                        MusicPlay.pausePlay();
                    }else {
                        Toast.makeText(MainActivity.this,"该音乐不存在！",Toast.LENGTH_SHORT).show();
                    }
                }else {
                    MusicPlay.pausePlay();
                    MusicPlay.notificationManager.cancel(1);
                    if (MusicPlay.lastPlayInfo==null){
                        MusicPlay.lastPlayInfo= new LastPlayInfo();
                    }
                    Utility.saveMusicToInfo(MusicPlay.music,MusicPlay.lastPlayInfo);
                    MusicPlay.lastPlayInfo.setPlayMode(MusicPlay.PLAY_MODE);
                    MusicPlay.lastPlayInfo.setPlayPosition(MusicPlay.mediaPlayer.getCurrentPosition());
                    pauseMusic.setImageResource(R.drawable.ic_play_arrow_white_48dp);
                }
                break;
            case R.id.next_music:
                if (MusicPlay.dataList.size()>0){
                    MusicPlay.initMediaPlayer();
                    if (MusicPlayService.myAudFocListener==null){
                        Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                        startIntent.putExtra("commend","only_play");
                        startService(startIntent);
                    }
                    MusicPlay.nextSong();
                    reNewView();
                }else {
                    Toast.makeText(MainActivity.this,"未发现本地音乐！",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case 0:
//                    progressBar.setVisibility(View.GONE);
                    initView();
//                    listView.setVisibility(View.VISIBLE);
                    isFreshing = false;
                    break;
                case 1:
                    initList();
                    progressBar.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                    lastMusic.setVisibility(View.VISIBLE);
                    isFreshing = false;
                    break;
                default:
                    break;
            }

        }
    };



    private void showDropDownPopupDialog() {
        SharedPreferences preferences = getSharedPreferences("music_play", Context.MODE_PRIVATE);
        int findTime = preferences.getInt("find_time", 60);
        View view = View.inflate(context, R.layout.setting_time, null);
        nowTime = (TextView) view.findViewById(R.id.now_time);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.time_select);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(120);
        seekBar.setProgress(findTime);
        Popus popup = new Popus();

// 这里是获得屏幕宽度使弹窗水平居中

        int xPos = 60;

        popup .setxPos( xPos );

        popup .setyPos(0);

        popup .setvWidth(700);

        popup .setvHeight(300);

        popup .setClickable( true );

        popup .setAnimFadeInOut(R.style.PopupWindowAnimation );

        popup.setCustomView(view);
        popup .setContentView(R.layout.activity_main );

        PopupDialog popupDialog = PopuUtils.createPopupDialog (context, popup );

        popupDialog .showAsDropDown(fresh, popup.getxPos(), popup.getyPos());

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
//        Log.i("MusicPlayActivity",progress+"");
        lastPosition = progress;
        nowTime.setText(progress+"秒");
        seekBar.setProgress(progress);
//        mtvstate.setText("开始拖动");
//        mtvdata.setText("当前进度数值是："+progress);

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        Log.i("MusicPlayActivity","onStartTrackingTouch");

//        mtvstate.setText("开始拖动");

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        SharedPreferences preferences = context.getSharedPreferences("music_play", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("find_time", lastPosition);
        editor.apply();
        FindMusic.findFromDataSupport(MusicPlay.dataList);
        musicnum.setText(MusicPlay.dataList.size()+"首");
        adapter.notifyDataSetChanged();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.toolbar_fresh:
                if (!isFreshing){
                    isFreshing = true;
                    queryMusic();
                }
                break;
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
            default:
                break;
        }
        return true;
    }

    public  String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            return time / 1000 / 60 + ":0" + time / 1000 % 60;

        } else {
            return time / 1000 / 60 + ":" + time / 1000 % 60;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        switch (requestCode){
            case 1:
                if (grantResults.length>0&& grantResults[0] ==PackageManager.PERMISSION_GRANTED){
                    initList();
                }else {
                    Toast.makeText(this,"You denied the permission",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    public void onResume(){
        Log.i(TAG,"onResume()");
        reNewView();
        super.onResume();
    }

    @Override
    public void onBackPressed(){
        if (MusicPlay.mediaPlayer!=null&&MusicPlay.mediaPlayer.isPlaying()){
            moveTaskToBack(false);
        }else {
            Intent intent = new Intent(MusicPlay.CLOSEAPP);
            sendBroadcast(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(myReceiver);

    }
}
