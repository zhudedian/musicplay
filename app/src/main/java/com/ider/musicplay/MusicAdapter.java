package com.ider.musicplay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ider.musicplay.util.Music;
import com.ider.musicplay.util.MusicPlay;

import java.util.List;

/**
 * Created by Eric on 2017/3/31.
 */

public class MusicAdapter extends ArrayAdapter<Music> {

    private int resourceId;
    private Context context;
    private List<Bitmap> bitmapList;
    public MusicAdapter(Context context, int textViewResourceId, List<Music> objects){
        super(context,textViewResourceId,objects);
        resourceId = textViewResourceId;
        this.context = context;


    }
    @Override
    public View getView(int posetion, View convertView, ViewGroup parent){
        Music music = getItem(posetion);
        View view;
        MusicAdapter.ViewHolder viewHolder;
        if (convertView==null){
            view = LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
            viewHolder =new MusicAdapter.ViewHolder();
            viewHolder.name = (TextView)view.findViewById(R.id.music_name);
            viewHolder.album = (TextView)view.findViewById(R.id.music_album);
            viewHolder.artist = (TextView)view.findViewById(R.id.music_artist);
            viewHolder.playing = (ImageView)view.findViewById(R.id.playing_image);
            view.setTag(viewHolder);
        }else {
            view = convertView;
            viewHolder = (MusicAdapter.ViewHolder) view.getTag();
        }
        viewHolder.name.setText(music.getMusicName());
        viewHolder.album.setText("专辑："+music.getMusicAlbum());
        viewHolder.artist.setText(music.getMusicArtist());
        if (music.equals(MusicPlay.music)){
            viewHolder.playing.setVisibility(View.VISIBLE);
            if (MusicPlay.mediaPlayer.isPlaying()){
                viewHolder.playing.setImageResource(R.drawable.playing);
                AnimationDrawable animationDrawable = (AnimationDrawable)viewHolder.playing.getDrawable();
                animationDrawable.start();
            }else {
                viewHolder.playing.setImageResource(R.drawable.playing);
                AnimationDrawable animationDrawable = (AnimationDrawable)viewHolder.playing.getDrawable();
                animationDrawable.stop();
            }

        }else {
            viewHolder.playing.setVisibility(View.GONE);
        }

        return view;
    }
    class ViewHolder{
        TextView name,album,artist;
        ImageView playing;
    }
}
