package com.ider.musicplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MusicPlayReceiver extends BroadcastReceiver {

    public static final String ACTION_1 = "PressPauseOrPlayButton";
    public static final String ACTION_2 = "PressNextButton";
    public MusicPlayReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String toastStr = "你点击了";
        if(action.equals(ACTION_1))
        {
            Toast.makeText(context, toastStr + "播放/暂停按钮", Toast.LENGTH_SHORT).show();
        }
        else if(action.equals(ACTION_2))
        {
            Toast.makeText(context, toastStr + "下一曲按钮", Toast.LENGTH_SHORT).show();
        }
    }
}
