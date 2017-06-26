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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.ider.musicplay.util.MusicPlay.mediaPlayer;


public class MainActivity extends BaseActivity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{


    private Context context;
    private List<Music> dataList = new ArrayList<>();
    private TextView notice, musicnum ,nowTime;
    private ImageView fresh;
    private ProgressBar progressBar;
    private MusicAdapter adapter;
    private ListView listView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isFreshing=false;
    private int lastPosition;

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
        fresh = (ImageView) findViewById(R.id.fresh);
        progressBar = (ProgressBar)findViewById(R.id.progress_bar) ;
        notice = (TextView) findViewById(R.id.hav_no_music);
        musicnum= (TextView) findViewById(R.id.music_num);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        fresh.setOnClickListener(this);
        notice.setOnClickListener(this);
        listView = (ListView) findViewById(R.id.music_list);
        adapter = new MusicAdapter(MainActivity.this,R.layout.music_list_item,dataList);
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
                        showDropDownPopupDialog();
                        break;
                }
                return true;
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position,long id){
                Music music = dataList.get(position);
                String path = music.getMusicPath();
                if (new File(path).exists()){
                    Intent startIntent = new Intent(MainActivity.this,MusicPlayService.class);
                    startIntent.putExtra("dataList", (Serializable) dataList);
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
        SharedPreferences preferences = getSharedPreferences("music_play", Context.MODE_PRIVATE);
        boolean isDataSave = preferences.getBoolean("data_save", false);
//        Log.i("musicplay",firstIn+"");
        if (isDataSave&&!FindMusic.isScaning) {
            FindMusic.findFromDataSupport(context,dataList);
        }
        if (dataList.size()>0){
            initView();
        }else {
            queryMusic();
        }
    }
    private void queryMusic(){
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        FindMusic.findFromMedia(MainActivity.this);
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
        if (dataList.size()>0){
            notice.setVisibility(View.GONE);

        }else {
            notice.setVisibility(View.VISIBLE);
        }
        musicnum.setText(dataList.size()+"首");
        Log.i("musicplay","dateList.size()="+dataList.size());
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
//                showDropDownPopupDialog();
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
    public void onBackPressed(){
        Intent intent = new Intent(MusicPlay.CLOSEAPP);
        sendBroadcast(intent);
        finish();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

    }
}
