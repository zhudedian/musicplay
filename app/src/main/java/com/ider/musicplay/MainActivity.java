package com.ider.musicplay;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ider.musicplay.service.MusicPlayService;
import com.ider.musicplay.util.BaseActivity;
import com.ider.musicplay.util.Music;
import com.ider.musicplay.util.MusicPlay;
import com.ider.musicplay.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;




public class MainActivity extends BaseActivity implements View.OnClickListener{

    private List<Music> musicList = new ArrayList<>();
    private List<Music> dateList = new ArrayList<>();
    private List<Bitmap> bitmapList = new ArrayList<>();
    private TextView notice, musicnum;
    private ImageView fresh;
    private ProgressBar progressBar;
    private MusicAdapter adapter;
    private ListView listView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isFreshing=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.activity_music);
        ActionBar actionBar= getSupportActionBar();
        if (actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_list_white_24dp);
        }
        fresh = (ImageView) findViewById(R.id.fresh);
        progressBar = (ProgressBar)findViewById(R.id.progress_bar) ;
        notice = (TextView) findViewById(R.id.hav_no_music);
        musicnum= (TextView) findViewById(R.id.music_num);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        fresh.setOnClickListener(this);
        notice.setOnClickListener(this);
        listView = (ListView) findViewById(R.id.music_list);
        adapter = new MusicAdapter(MainActivity.this,R.layout.music_list_item,dateList);
        listView.setAdapter(adapter);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }else {
            initList();
        }


        navigationView.setCheckedItem(R.id.nav_time);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(MenuItem item){
                switch (item.getItemId()){
                    case R.id.nav_time:

                        break;
                }
                return true;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position,long id){
                Music music = dateList.get(position);
                String path = music.getMusicPath();
                if (new File(path).exists()){
                    Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                    startIntent.putExtra("dateList", (Serializable) dateList);
                    startIntent.putExtra("music",music);
                    startIntent.putExtra("notify",false);
                    startIntent.putExtra("position",position);
                    startService(startIntent);
                }else {
                    Toast.makeText(MainActivity.this,"该音乐不存在！",Toast.LENGTH_SHORT).show();
                }


            }
        });



    }
    private void initList(){
        SharedPreferences preferences = getSharedPreferences("music_prefers", Context.MODE_PRIVATE);
        boolean firstIn = preferences.getBoolean("first_in", true);
//        Log.i("musicplay",firstIn+"");
        if (!firstIn&&!Utility.isScaning) {
            musicList = DataSupport.findAll(Music.class);
        }else {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("first_in", false);
            editor.apply();
        }
        if (musicList.size()>0){
            dateList.clear();
            for (Music music :musicList){
                if (new File(music.getMusicPath()).exists()){
//                            Bitmap bitmap = Utility.createAlbumArt(MainActivity.this,music.getMusicPath(),true);
//                            bitmapList.add(bitmap);
                    dateList.add(music);
                }else {
                    DataSupport.deleteAll(Music.class,"musicPath = ?",music.getMusicPath());
                }
            }
            initView();
        }else {
            queryMusic();
        }
    }
    private void queryMusic(){
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        Utility.queryLocalMusic(MainActivity.this);
        new Thread(){
            public void run() {
                boolean isNotEnd = true;
                while (isNotEnd) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!Utility.isScaning) {
                        isNotEnd =false;
                        mHandler.sendEmptyMessage(1);
                    }
                }
            }
        }.start();
    }
    private void initView(){
        if (dateList.size()>0){
            notice.setVisibility(View.GONE);

        }else {
            notice.setVisibility(View.VISIBLE);
        }
        musicnum.setText(dateList.size()+"首");
        Log.i("musicplay","dateList.size()="+dateList.size());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view){
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
                    isFreshing = false;
                    break;
                default:
                    break;
            }

        }
    };

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
                    Utility.queryLocalMusic(MainActivity.this);
                    initList();
                }else {
                    Toast.makeText(this,"You denied the permission",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Intent intent = new Intent(MusicPlay.CLOSEAPP);
        sendBroadcast(intent);
    }
}
