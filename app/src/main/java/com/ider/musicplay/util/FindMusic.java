package com.ider.musicplay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;




/**
 * Created by Eric on 2017/6/26.
 */

public class FindMusic implements Comparator<Music>{
    private static Context context = MyApplication.getContext();
    private static List<Music> musicList = new ArrayList<>();
    private Collator collator = Collator.getInstance(Locale.CHINA);
    private static String TAG = "FindMusic";
    public static boolean isScaning=false;
    public static void findFromMedia(){
        new Thread(){
            public void run() {
                if (!isScaning) {
                    isScaning = true;
                    scanMusic();
                }
            }
        }.start();
    }

    public static void findFromDataSupport(List<Music> dataList){
        SharedPreferences preferences = context.getSharedPreferences("music_play", Context.MODE_PRIVATE);
        int time = preferences.getInt("find_time",60);
        musicList = DataSupport.findAll(Music.class);
        if (musicList.size()>0){
            dataList.clear();
            for (Music music :musicList){
                if (new File(music.getMusicPath()).exists()){
                    if (music.getMusicDuration()>time*1000) {
                        dataList.add(music);
                    }
                }else {
                    DataSupport.deleteAll(Music.class,"musicPath = ?",music.getMusicPath());
                }
            }
        }
        Collections.sort(dataList,new FindMusic());
    }

    public static boolean scanMusic(){
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,null, null,null);
        if (c!=null) {
            c.moveToFirst();
            do{
                Music music = new Music();
                //名字
                String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                long album_id = c.getLong(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                long id = c.getLong(c.getColumnIndex(MediaStore.Audio.Media._ID));
                music.setMusic_id(id);
                music.setMusicAlbum_id(album_id);
                music.setMusicName(name);
                //专辑名
                String album = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                music.setMusicAlbum(album);
                //歌手名
                String artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                music.setMusicArtist(artist.replaceAll(" ",""));

                //URI 歌曲文件存放路径
                String path = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                music.setMusicPath(path);
                Log.i(TAG,path);
                String md5 = getFileMD5(new File(path));
                music.setMd5(md5);
                //歌曲文件播放时间长度
                int duration = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                music.setMusicDuration(duration);
                //音乐文件大小
                int size = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                music.setMusicSize(size);
                DataSupport.deleteAll(Music.class,"musicPath = ?",path);
                music.save();
            }while (c.moveToNext());
            SharedPreferences preferences = context.getSharedPreferences("music_play", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("data_save", true);
            editor.apply();
        }else {
            isScaning = false;
            return false;
        }
        c.close();
        isScaning = false;
        return true;
    }
    private static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }
    @Override
    public int compare(Music music1 , Music music2){
        int value = collator.compare(music1.getMusicName(),music2.getMusicName());
        if (value>0){
            return 1;
        }else if (value<0){
            return -1;
        }else {
            int value2 = collator.compare(music1.getMusicPath(),music2.getMusicPath());
            return value2;
        }
    }
}
