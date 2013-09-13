package com.cyanogenmod.trebuchet.widget;

import java.lang.reflect.Method;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DisplayList;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.cyanogenmod.trebuchet.widget.IMirrorView.MirrorViewCallback;

public class MirrorSourceFrameLayout extends FrameLayout implements MirrorViewCallback {

    private boolean mIsMirrorViewActived;
    private IMirrorView mMirrorView;
    
    public MirrorSourceFrameLayout(Context context) {
        super(context);
    }

    public MirrorSourceFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MirrorSourceFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMirrorView(IMirrorView mirrorView) {
        mMirrorView = mirrorView;
        mirrorView.setSourceView(this);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }
    
    @Override
    public void setLayerType(int layerType, Paint paint) {
        super.setLayerType(LAYER_TYPE_SOFTWARE, paint);
    }

    public IMirrorView getMirrorView() {
        return mMirrorView;
    }

    @Override
    public void onMirrorActiveChanged(boolean actived) {
        mIsMirrorViewActived = actived;
    }

    @Override
    public boolean dispatchMirrorTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mIsMirrorViewActived) return false;
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        if (mMirrorView == null) {
            super.dispatchDraw(canvas);
            return;
        }
        
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        
        Bitmap bufBmp = mMirrorView.getIdleBitmap(w, h);
        Canvas bufCanvas = new Canvas(bufBmp);
        
        synchronized (mMirrorView) {
            bufCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            bufCanvas.save();
            bufCanvas.translate(-getScrollX(), -getScrollY()); 
            bufCanvas.scale(0.5f, 0.5f);
            super.dispatchDraw(bufCanvas);  
            bufCanvas.restore();
        }

        canvas.save();
        canvas.translate(getScrollX(), getScrollY());   
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//        canvas.drawColor(Color.BLACK);
        canvas.restore();

        mMirrorView.postDraw(bufBmp);
    }    
    
}
