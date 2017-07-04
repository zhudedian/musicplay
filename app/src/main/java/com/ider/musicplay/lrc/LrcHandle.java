package com.ider.musicplay.lrc;

import android.os.Environment;
import android.os.Handler;

import com.ider.musicplay.util.Music;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.R.attr.path;

/**
 * Created by Eric on 2017/7/3.
 */

public class LrcHandle {
    private static List mWords = new ArrayList();
    private static List mTimeList = new ArrayList();
    private static Handler handler;
    public static String lrcPath;

    //处理歌词文件
    public static void readLRC(String path) {
        File file = new File(path);
        mWords.clear();
        mTimeList.clear();
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(
                    fileInputStream, "utf-8");
            BufferedReader bufferedReader = new BufferedReader(
                    inputStreamReader);
            String s = "";
            while ((s = bufferedReader.readLine()) != null) {
                addTimeToList(s);
                if ((s.indexOf("[ar:") != -1) || (s.indexOf("[ti:") != -1)
                        || (s.indexOf("[by:") != -1)|| (s.indexOf("[al:") != -1)) {
                    s = s.substring(s.indexOf(":") + 1, s.indexOf("]"));
//                    mWords.add(s);
                } else {
                    if (s.indexOf("]")==s.length()-1){
                        mWords.add(" ");
                    }else {
                        String ss = s.substring(s.indexOf("["), s.indexOf("]") + 1);
                        s = s.replace(ss, "");
                        mWords.add(s);
                    }

                }

            }

            bufferedReader.close();
            inputStreamReader.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mWords.add("没有歌词文件，赶紧去下载");
        } catch (IOException e) {
            e.printStackTrace();
            mWords.add("没有读取到歌词");
        }
    }
    public static List getWords() {
        return mWords;
    }

    public static List getTime() {
        return mTimeList;
    }

    // 分离出时间
    private static int timeHandler(String string) {
        string = string.replace(".", ":");
        String timeData[] = string.split(":");
// 分离出分、秒并转换为整型
        int minute = Integer.parseInt(timeData[0]);
        int second = Integer.parseInt(timeData[1]);
        int millisecond = Integer.parseInt(timeData[2]);

        // 计算上一行与下一行的时间转换为毫秒数
        int currentTime = (minute * 60 + second) * 1000 + millisecond * 10;

        return currentTime;
    }

    private static void addTimeToList(String string) {
        Matcher matcher = Pattern.compile(
                "\\[\\d{1,2}:\\d{1,2}([\\.:]\\d{1,2})?\\]").matcher(string);
        if (matcher.find()) {
            String str = matcher.group();
            mTimeList.add(timeHandler(str.substring(1,
                    str.length() - 1)));
        }

    }
    public static void findLrc(final Music music){
        new Thread(){
            public void run() {
                String lrcName = music.getMusicArtist()+"-"+music.getMusicName()+".lrc";
                String lrcName2 = music.getMusicName()+"-"+music.getMusicArtist()+".lrc";
                File lrc;
                if ((lrc=findLrc(lrcName))!=null){
                    lrcPath = lrc.getPath();
                    handler.sendEmptyMessage(2);
                }else if ((lrc=findLrc(lrcName2))!=null){
                    lrcPath = lrc.getPath();
                    handler.sendEmptyMessage(2);
                }else {
                    handler.sendEmptyMessage(3);
                }
            }
        }.start();

    }
    private static File findLrc(String name) {
        String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(sdcardPath+"/Music/Musiclrc");
        File[] files = file.listFiles();
        for(File f:files){//遍历展开后的文件夹的文件
            if(f.isDirectory()){//如果是文件夹，继续展开
                File[] filess = f.listFiles();
                if (filess!=null)
                    return findLrc(name);//用递归递归
            }else if(f.getName().equals(name)){
                return f;//符合格式的添加入列
            }
        }
        return null;
    }
    public static void setHandler(Handler mHandler){
        handler = mHandler;
    }
}
