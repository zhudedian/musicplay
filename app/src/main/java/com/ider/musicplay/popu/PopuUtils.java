package com.ider.musicplay.popu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by Eric on 2017/6/26.
 */

public class PopuUtils {
    private static PopupDialog popupDialog= null;
    @SuppressLint("NewApi")
    public static PopupDialog createPopupDialog(Context context, Popus dialog ) {
        dismissPopupDialog();
        View view =null;
        if(dialog.getCustomView()==null){
            LayoutInflater inflater = LayoutInflater.from(context);
            view= inflater.inflate(dialog.getContentView(),null);
        }else{
            view= dialog.getCustomView();
        }
        view.setOnTouchListener(dialog.getTouchListener());
        if(0!= dialog.getBgAlpha()){
            view.setAlpha(dialog.getBgAlpha());
        }
        popupDialog= new PopupDialog(view,dialog.getvWidth(),dialog.getvHeight());
        ColorDrawable dw = new ColorDrawable(Color.TRANSPARENT); //follow two lines is used for back key -00000
        popupDialog.setBackgroundDrawable(dw);
        popupDialog.setAnimationStyle(dialog.getAnimFadeInOut());
        popupDialog.setOutsideTouchable(dialog.isClickable());
        popupDialog.setFocusable(true); //not allow user click popupwindowbackground event or not permit
        popupDialog.setOnDismissListener(dialog.getListener());
        popupDialog.update();
        return popupDialog;
    }
    public static void dismissPopupDialog() {
        if(popupDialog!=null&& popupDialog.isShowing()){
            popupDialog.dismiss();
            popupDialog= null;
        }
    }
    public static boolean isPopupShowing() {
        if(popupDialog!=null&& popupDialog.isShowing()){
            return true;
        }else{
            return false;
        }
    }
}
