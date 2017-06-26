package com.ider.musicplay.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import com.ider.musicplay.R;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Created by Eric on 2017/6/13.
 */

public class Utility {
    private static String TAG = "Utility";
    private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
    private static Bitmap mCachedBit = null;
    public static boolean isScan=true,isScaning=false;
    public static boolean queryLocalMusic(final Context context){
        Log.i(TAG,isScan+"");
        new Thread(){
            public void run() {
//                while (isScan) {
//                    try {
//                        sleep(3000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    if (!isScaning) {
                        isScaning = true;
                        scanMusic(context);
                    }
//                }
            }
        }.start();

        return true;
    }
    public static boolean scanMusic(Context context){
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,null, null,null);
        if (c!=null) {
//            isScan =false;
//            new Thread(){
//                public void run() {
//                        try {
//                            sleep(2000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        isScan = true;
//
//                }
//            }.start();
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
        }else {
            isScaning = false;
            return false;
        }
        c.close();
//        List<Music> musicList = DataSupport.findAll(Music.class);
//        for (int i = 0;i<musicList.size();i++){
//            Music music = musicList.get(i);
//            if(music.getBitmap()==null){
//                List<Music> musics = DataSupport.where("musicArtist = ?", music.getMusicArtist()).find(Music.class);
//                for (int j = 0; j<musics.size();j++){
//                    Music music1 = musics.get(i);
//                    Bitmap bitmap = createAlbumArt(music1.getMusicPath());
//                    if (bitmap!=null){
//                        music.setBitmap(bitmap);
//                        DataSupport.deleteAll("musicPath = ?", music.getMusicPath());
//                        music.save();
//                    }
//                }
//            }
//        }

        isScaning = false;
        return true;
    }
    public static Bitmap getArtwork(Context context, long song_id, long album_id, boolean allowdefault) {
        if (album_id < 0) {
            // This is something that is not in the database, so get the album art directly
            // from the file.
            if (song_id >= 0) {
                Bitmap bm = getArtworkFromFile(context, song_id, -1);
                if (bm != null) {
                    return bm;
                }
            }
            if (allowdefault) {
                return getDefaultArtwork(context);
            }
            return null;
        }
        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        if (uri != null) {
            InputStream in = null;
            try {
                in = res.openInputStream(uri);
                return BitmapFactory.decodeStream(in, null, sBitmapOptions);
            } catch (FileNotFoundException ex) {
                // The album art thumbnail does not actually exist. Maybe the user deleted it, or
                // maybe it never existed to begin with.
                Bitmap bm = getArtworkFromFile(context, song_id, album_id);
                if (bm != null) {
                    if (bm.getConfig() == null) {
                        bm = bm.copy(Bitmap.Config.RGB_565, false);
                        if (bm == null && allowdefault) {
                            return getDefaultArtwork(context);
                        }
                    }
                } else if (allowdefault) {
                    bm = getDefaultArtwork(context);
                }
                return bm;
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                }
            }
        }

        return null;
    }
    private static Bitmap getArtworkFromFile(Context context, long songid, long albumid) {
        Bitmap bm = null;
        byte [] art = null;
        String path = null;
        if (albumid < 0 && songid < 0) {
            throw new IllegalArgumentException("Must specify an album or a song id");
        }
        try {
            if (albumid < 0) {
                Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            } else {
                Uri uri = ContentUris.withAppendedId(sArtworkUri, albumid);
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            }
        } catch (FileNotFoundException ex) {

        }
        if (bm != null) {
            mCachedBit = bm;
        }
        return bm;
    }


    public static Bitmap createAlbumArt(Context context,final String filePath,boolean isNarrow) {
        Bitmap bitmap = null;
        //能够获取多媒体文件元数据的类
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath); //设置数据源
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 10;
            byte[] embedPic = retriever.getEmbeddedPicture(); //得到字节型数据
            if (embedPic!=null&&isNarrow) {
                bitmap = BitmapFactory.decodeByteArray(embedPic, 0, embedPic.length, options); //转换为图片
            }else if(embedPic!=null){
                bitmap = BitmapFactory.decodeByteArray(embedPic, 0, embedPic.length);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        if (bitmap==null){
            bitmap = getDefaultArtwork(context);
        }
        return bitmap;
    }

    private static Bitmap getDefaultArtwork(Context context) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeStream(context.getResources().openRawResource(R.drawable.defult), null, opts);
    }
    public static Bitmap getMusicBitmap(int album_id,Context context){
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[] { "album_art" };
        Cursor cur = context.getContentResolver().query(Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)), projection, null, null, null);
        String album_art = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
            cur.moveToNext();
            album_art = cur.getString(0);
        }
        cur.close();
        cur = null;
        Bitmap bm = BitmapFactory.decodeFile(album_art);
        return bm;
    }
    public static String formatTime(int time) {
        if (time / 1000 % 60 < 10) {
            return time / 1000 / 60 + ":0" + time / 1000 % 60;

        } else {
            return time / 1000 / 60 + ":" + time / 1000 % 60;
        }

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
}
