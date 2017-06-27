package com.ider.musicplay.util;

import android.app.Application;
import android.content.Context;

import org.litepal.LitePalApplication;

/**
 * Created by Eric on 2017/6/27.
 */

public class MyApplication extends Application {
    private static Context context;

    @Override
    public void onCreate(){
        context = getApplicationContext();
        LitePalApplication.initialize(context);
    }
    public static Context getContext(){
        return context;
    }
}
