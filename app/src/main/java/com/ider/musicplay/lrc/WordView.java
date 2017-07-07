package com.ider.musicplay.lrc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eric on 2017/7/3.
 */

public class WordView extends TextView {
    private List<String> mWordsList = new ArrayList();
    private Paint mLoseFocusPaint;
    private Paint mOnFocusePaint;
    private float mX = 0;
    private float mMiddleY = 0;
    private float mY = 0;
    private static final int DY = 100;
    private int mIndex;

    public WordView(Context context) throws IOException {
        super(context);
        init();

    }

    public WordView(Context context, AttributeSet attrs) throws IOException {
        super(context, attrs);
        init();
    }

    public WordView(Context context, AttributeSet attrs, int defStyle) throws IOException {
        super(context,attrs,defStyle);

        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        canvas.drawColor(Color.BLACK);
        Paint p = mLoseFocusPaint;
        p.setTextAlign(Paint.Align.CENTER);
        Paint p2 = mOnFocusePaint;
        p2.setTextAlign(Paint.Align.CENTER);
        if(mWordsList.size()>mIndex){
            canvas.drawText(mWordsList.get(mIndex), mX, mMiddleY, p2);
        }




        int alphaValue = 25;
        float tempY = mMiddleY;
        for (int i = mIndex - 1; i >= 0; i--) {
            tempY -= DY;
            if (tempY < 0||tempY<p.getTextSize()) {
                break;
            }
            p.setColor(Color.argb(255 - alphaValue, 245, 245, 245));
            if (mWordsList.size()>i){
                canvas.drawText(mWordsList.get(i), mX, tempY, p);
            }
            alphaValue += 25;
        }
        alphaValue = 25;
        tempY = mMiddleY;
        for (int i = mIndex + 1, len = mWordsList.size(); i < len; i++) {
            tempY += DY;

            if (tempY > mY||tempY+p.getTextSize()>mY) {
                break;
            }
            p.setColor(Color.argb(255 - alphaValue, 245, 245, 245));
            if (mWordsList.size()>i){
                canvas.drawText(mWordsList.get(i), mX, tempY, p);
            }

            alphaValue += 25;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);

        mX = w * 0.5f;
        mY = h;
        mMiddleY = h * 0.5f;
    }

    @SuppressLint("SdCardPath")
    private void init() throws IOException {
        setFocusable(true);


        mLoseFocusPaint = new Paint();
        mLoseFocusPaint.setAntiAlias(true);
        mLoseFocusPaint.setTextSize(40);
        mLoseFocusPaint.setColor(Color.WHITE);
        mLoseFocusPaint.setTypeface(Typeface.SERIF);

        mOnFocusePaint = new Paint();
        mOnFocusePaint.setAntiAlias(true);
        mOnFocusePaint.setColor(Color.YELLOW);
        mOnFocusePaint.setTextSize(55);
        mOnFocusePaint.setTypeface(Typeface.SANS_SERIF);


    }
    public void setText(List<String> wordList){
        this.mWordsList = wordList;

    }
    public void setText(String word){
        mWordsList.clear();
        mWordsList.add(word);
        mIndex =0;
        invalidate();
    }
    public void reNew(int mIndex){
        this.mIndex = mIndex;
        this.invalidate();
    }

}
