package com.unnamed.tvlauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Lightweight custom-drawn TV launcher surface, navigable with a D-pad or touch. */
final class LauncherView extends View {
    static final int CONTROL_INPUTS = 0, CONTROL_WIFI = 1, CONTROL_SETTINGS = 2;
    interface Callbacks {
        void launchApp(AppTile tile);
        void openControl(int control);
    }
    private final Callbacks callbacks;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<AppTile> apps = new ArrayList<>();
    private Bitmap splash, wallpaper;
    private boolean showingSplash = true;
    private int selectedIndex = 0, focusedControl = -1;
    private float scale = 1f, tileW, tileH, gap, margin, dockY;
    LauncherView(Context context, Callbacks callbacks) { super(context); this.callbacks = callbacks; setFocusable(true); }
    void setSplashBitmap(Bitmap b) { splash = b; invalidate(); }
    void setWallpaper(Bitmap b) { wallpaper = b; invalidate(); }
    void setApps(List<AppTile> values) { apps.clear(); if (values != null) apps.addAll(values); selectedIndex = Math.min(selectedIndex, Math.max(0, apps.size()-1)); invalidate(); }
    void showSplash() { showingSplash = true; invalidate(); }
    void showHome() { showingSplash = false; invalidate(); }
    boolean isShowingSplash() { return showingSplash; }
    void resetNavigation() { selectedIndex = 0; focusedControl = -1; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        scale = Math.max(0.7f, Math.min(w / 1280f, h / 720f));
        if (showingSplash) {
            if (splash != null) { paint.setColor(Color.WHITE); canvas.drawBitmap(splash, null, new RectF(0,0,w,h), paint); }
            else { canvas.drawColor(Color.BLACK); text(canvas, "UNNAMED", w/2f, h/2f, 44*scale, Color.WHITE, Paint.Align.CENTER); }
            return;
        }
        if (wallpaper != null) canvas.drawBitmap(wallpaper, null, new RectF(0,0,w,h), paint);
        else canvas.drawColor(Color.rgb(19,23,31));
        paint.setColor(0x99070A10); canvas.drawRect(0,0,w,h,paint);
        text(canvas, "UNNAMED", 48*scale, 66*scale, 22*scale, Color.WHITE, Paint.Align.LEFT);
        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        text(canvas, time, w-48*scale, 66*scale, 22*scale, Color.WHITE, Paint.Align.RIGHT);
        text(canvas, "Your entertainment, together.", 48*scale, 126*scale, 16*scale, 0xFFCED2D8, Paint.Align.LEFT);
        float controlY = 170*scale;
        String[] controls = {"INPUTS", "WI-FI", "SETTINGS"};
        for (int i=0;i<controls.length;i++) {
            float x=(48+i*164)*scale;
            rect.set(x, controlY-24*scale, x+142*scale, controlY+22*scale);
            paint.setColor(focusedControl==i ? 0xFFFFFFFF : 0x663F4858); canvas.drawRoundRect(rect, 16*scale,16*scale,paint);
            text(canvas,controls[i],x+71*scale,controlY+6*scale,13*scale,focusedControl==i?Color.BLACK:Color.WHITE,Paint.Align.CENTER);
        }
        text(canvas, apps.isEmpty() ? "No launchable apps found" : safeLabel(apps.get(selectedIndex)),
                48*scale, 260*scale, 32*scale, Color.WHITE, Paint.Align.LEFT);
        margin=48*scale; gap=14*scale; tileW=(w-2*margin-5*gap)/6f; tileH=100*scale; dockY=300*scale;
        int firstRow=Math.max(0, selectedIndex/6-1);
        int lastRow=Math.min((apps.size()-1)/6, firstRow+2);
        for(int row=firstRow;row<=lastRow;row++) for(int col=0;col<6;col++) {
            int idx=row*6+col; if(idx>=apps.size()) break;
            float x=margin+col*(tileW+gap), y=dockY+(row-firstRow)*(tileH+gap);
            rect.set(x,y,x+tileW,y+tileH);
            paint.setColor(idx==selectedIndex?Color.WHITE:0xFFE7E8EA);
            canvas.drawRoundRect(rect,14*scale,14*scale,paint);
            AppTile app=apps.get(idx);
            if(app.artwork!=null) {
                int iw=Math.max(1,app.artwork.getIntrinsicWidth()), ih=Math.max(1,app.artwork.getIntrinsicHeight());
                float max=Math.min(tileH*.55f,tileW*.35f), ratio=Math.min(max/iw,max/ih);
                float aw=iw*ratio, ah=ih*ratio;
                app.artwork.setBounds((int)(x+(tileW-aw)/2),(int)(y+(tileH-ah)/2),(int)(x+(tileW+aw)/2),(int)(y+(tileH+ah)/2));
                app.artwork.draw(canvas);
            } else text(canvas,safeLabel(app),x+tileW/2,y+tileH/2,13*scale,Color.DKGRAY,Paint.Align.CENTER);
            if(idx==selectedIndex) { paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3*scale); paint.setColor(Color.WHITE); canvas.drawRoundRect(rect,14*scale,14*scale,paint); paint.setStyle(Paint.Style.FILL); }
        }
        text(canvas,"Use the directional pad to navigate",48*scale,h-28*scale,13*scale,0xFFCED2D8,Paint.Align.LEFT);
    }
    private String safeLabel(AppTile tile) { return tile==null?"":tile.label; }
    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align) {
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
        paint.setTextAlign(align); paint.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)); c.drawText(s,x,y,paint);
    }
    boolean handleDpadKey(int key) {
        if (showingSplash) return true;
        if (key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (focusedControl>=0) focusedControl=Math.max(0,Math.min(2,focusedControl+(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1)));
            else if(!apps.isEmpty()) selectedIndex=GridNavigation.moveHorizontal(selectedIndex,apps.size(),key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);
            invalidate(); return true;
        }
        if (key==KeyEvent.KEYCODE_DPAD_UP) { if(focusedControl<0) focusedControl=2; else focusedControl=-1; invalidate(); return true; }
        if (key==KeyEvent.KEYCODE_DPAD_DOWN) { if(focusedControl>=0) focusedControl=-1; else if(!apps.isEmpty()) selectedIndex=GridNavigation.moveVertical(selectedIndex,apps.size(),1,GridNavigation.columnOf(selectedIndex)); invalidate(); return true; }
        if (key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER || key==KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if(focusedControl>=0) callbacks.openControl(focusedControl); else if(!apps.isEmpty()) callbacks.launchApp(apps.get(selectedIndex));
            return true;
        }
        return false;
    }
    boolean handleBack() { if(focusedControl>=0){focusedControl=-1;invalidate();return true;} return false; }
    @Override public boolean onTouchEvent(android.view.MotionEvent e) {
        if(e.getAction()!=android.view.MotionEvent.ACTION_UP)return true;
        if(showingSplash)return true;
        float x=e.getX(),y=e.getY();
        if(y>145*scale&&y<200*scale){int c=(int)(x/(164*scale));focusedControl=Math.max(0,Math.min(2,c));callbacks.openControl(focusedControl);invalidate();return true;}
        if(y>=dockY){int col=(int)((x-margin)/(tileW+gap));int row=(int)((y-dockY)/(tileH+gap));int idx=row*6+col;if(idx>=0&&idx<apps.size()){if(idx==selectedIndex)callbacks.launchApp(apps.get(idx));selectedIndex=idx;focusedControl=-1;invalidate();}}
        return true;
    }
}