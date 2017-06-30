package com.ider.musicplay.util;

import android.graphics.Bitmap;

import org.litepal.crud.DataSupport;

import java.io.Serializable;

/**
 * Created by Eric on 2017/3/31.
 */

public class Music extends DataSupport implements Serializable {
    private String musicName;
    private long musicAlbum_id;
    private long music_id;
    private String musicAlbum;
    private String musicArtist;
    private String musicPath;
    private int musicDuration;
    private int musicSize;
    private String md5;
    public Music(){

    }
    public Music(String musicName, long musicAlbum_id,long music_id, String musicAlbum, String musicArtist, String musicPath, int musicDuration, int musicSize, String md5){
        this.musicName = musicName;
        this.musicAlbum_id = musicAlbum_id;
        this.music_id = music_id;
        this.musicAlbum = musicAlbum;
        this.musicArtist = musicArtist;
        this.musicPath = musicPath;
        this.musicDuration = musicDuration;
        this.musicSize = musicSize;
        this.md5 = md5;
    }

    public String getMusicName(){
        return musicName;
    }
    public void setMusicName(String musicName){
        this.musicName= musicName;
    }
    public long getMusicAlbum_id(){
        return musicAlbum_id;
    }
    public void setMusicAlbum_id(long musicAlbum_id){
        this.musicAlbum_id = musicAlbum_id;
    }
    public long getMusic_id(){
        return music_id;
    }
    public void setMusic_id(long music_id){
        this.music_id = music_id;
    }
    public String getMusicAlbum(){
        return musicAlbum;
    }
    public void setMusicAlbum(String musicAlbum){
        this.musicAlbum= musicAlbum;
    }
    public String getMusicArtist(){
        return musicArtist;
    }
    public void setMusicArtist(String musicArtist){
        this.musicArtist= musicArtist;
    }
    public String getMusicPath(){
        return musicPath;
    }
    public void setMusicPath(String musicPath){
        this.musicPath= musicPath;
    }
    public int getMusicDuration(){
        return musicDuration;
    }
    public void setMusicDuration(int musicDuration){
        this.musicDuration= musicDuration;
    }
    public int getMusicSize(){
        return musicSize;
    }
    public void setMusicSize(int musicSize){
        this.musicSize= musicSize;
    }
    public String getMd5(){
        return md5;
    }
    public void setMd5(String md5){
        this.md5 = md5;
    }

    @Override
    public boolean equals(Object object){
        if (object instanceof Music){
            Music music= (Music) object;
            if (music.musicPath.equals(this.musicPath)&&music.musicName.equals(this.musicName)){
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }
    @Override
    public int hashCode(){
        return 2;
    }
}
