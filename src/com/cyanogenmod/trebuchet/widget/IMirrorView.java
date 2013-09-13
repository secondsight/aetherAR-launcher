package com.cyanogenmod.trebuchet.widget;

import android.graphics.Bitmap;
import android.view.MotionEvent;

public interface IMirrorView {

    public interface MirrorViewCallback {
        void onMirrorActiveChanged(boolean actived);
        boolean dispatchMirrorTouchEvent(MotionEvent event);
        void postInvalidate();
    }
    
    void setSourceView(MirrorViewCallback callback);
    Bitmap getIdleBitmap(int w, int h);
    void postDraw(Bitmap bmp);
    void clearDrawRequests();
    
    void onAppear();
    void onDisappear();
}
