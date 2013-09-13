package com.cyanogenmod.trebuchet.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MirrorView extends View implements IMirrorView {
		
	private Bitmap mSourceBitmap;
	private MirrorViewCallback mCallback;

	public MirrorView(Context arg0) {
		super(arg0);
	}
	public MirrorView(Context arg0, AttributeSet arg1) {
		super(arg0, arg1);
	}
	public MirrorView(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2);
	}

    public void setSourceView(MirrorViewCallback callback) {
        mCallback = callback;
    }
	
	@Override
    public Bitmap getIdleBitmap(int w, int h) {
        if (mSourceBitmap == null || mSourceBitmap.getWidth() < w || mSourceBitmap.getHeight() < h) {
            if (mSourceBitmap != null) {
                mSourceBitmap.recycle();
            }
            mSourceBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        }
        return mSourceBitmap;
    }	
	
    @Override
    public void clearDrawRequests() {        
    }
    
    @Override
    public void postDraw(Bitmap bmp) {
        invalidate();
    }

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mSourceBitmap != null && !mSourceBitmap.isRecycled()) {
			int w = getMeasuredWidth();
			int h = getMeasuredHeight();

			canvas.drawBitmap(mSourceBitmap, w - mSourceBitmap.getWidth(), h - mSourceBitmap.getHeight(), null);
		}
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (mCallback != null) {
			int action = event.getAction();
			if (action == MotionEvent.ACTION_DOWN) {
				mCallback.onMirrorActiveChanged(true);
			} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
				mCallback.onMirrorActiveChanged(false);
			}

			return mCallback.dispatchMirrorTouchEvent(event);
		}
		return false;
	}
	
    @Override
    public void onAppear() {        
    }
    @Override
    public void onDisappear() {        
    }
}
