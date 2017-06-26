package com.ider.musicplay.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ider.musicplay.MusicPlayActivity;
import com.ider.musicplay.service.MusicPlayService;

import static com.ider.musicplay.util.MusicPlay.PAUSE_OR_ARROW;
import static com.ider.musicplay.util.MusicPlay.mediaPlayer;

/**
 * Created by Eric on 2017/6/21.
 */

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlay.CANCEL_PLAY);
        registerReceiver(myReceiver, filter);
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicPlay.CANCEL_PLAY)){
                finish();
            }
        }
    };
}
