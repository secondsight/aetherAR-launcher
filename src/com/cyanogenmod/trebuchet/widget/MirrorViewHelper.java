package com.cyanogenmod.trebuchet.widget;

import android.content.Context;
import android.graphics.Point;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.cyanogenmod.trebuchet.AppsGLSurfaceView;

public class MirrorViewHelper {
    
    private static MirrorViewHelper sInstance = new MirrorViewHelper();
    
    private boolean mIsPaused = true;
    private IMirrorView mMirrorView = null;
    
    public static MirrorViewHelper getInstance() {
        return sInstance;
    }
    
    public void resume() {   
        mIsPaused = false;
        if (mMirrorView != null) {
            mMirrorView.onAppear();
        }      
    }
    
    public void pause() {    
        mIsPaused = true;
        if (mMirrorView != null) {
            mMirrorView.onDisappear();
        }
    }
    
    public void destroy() {     
        mMirrorView = null;
    }
    
    private IMirrorView addMirrorView(ViewGroup mirrorLayout) {
        if (mMirrorView == null) {
            mMirrorView = new AppsGLSurfaceView(mirrorLayout.getContext(), null);
        }
        AppsGLSurfaceView mirrorView = (AppsGLSurfaceView)mMirrorView;      
        ViewGroup parent = (ViewGroup)mirrorView.getParent();
        if (parent != mirrorLayout) {
            if (parent != null) {
                if (!mIsPaused) {
                    mirrorView.onDisappear();
                }
                parent.removeView(mirrorView);
            }
            mirrorLayout.addView(mirrorView);
            if (!mIsPaused) {
                mirrorView.onAppear();
            }
        }        
        return mirrorView;
    }
    
    public IMirrorView getMirrorView() {
    	return mMirrorView;
    }
    
    public IMirrorView mirror(View v) {
        ViewGroup parent = (ViewGroup)v.getParent();
        
        ViewGroup mirrorLayout = null;
        MirrorSourceFrameLayout mirrorSource = null;
        if (parent instanceof MirrorSourceFrameLayout) {
            mirrorSource = ((MirrorSourceFrameLayout)parent);
            mirrorLayout = (ViewGroup)mirrorSource.getParent().getParent();
            IMirrorView mirrorView = addMirrorView(mirrorLayout);
            mirrorSource.setMirrorView(mirrorView);  
        } else {
            int index = -1;
            if (parent != null) {
                index = getViewIndexInParent(v);
                if (index < 0) {
                    return null;
                }
                parent.removeViewAt(index);
            }

            mirrorLayout = createMirrorLayout(v);
            if (parent != null) {
                parent.addView(mirrorLayout, index);
            }
        }
        return mMirrorView;
    }

    private ViewGroup createMirrorLayout(View sourceView) {        
        Context context = sourceView.getContext();
        FrameLayout layout = new FrameLayout(context);        

        MirrorSourceFrameLayout mirrorSource = new MirrorSourceFrameLayout(context);
//        AppsGLSurfaceView mirrorView = new AppsGLSurfaceView(context, null);
        
        MirrorGLConfig config = new MirrorGLConfig(context);
        Point size = config.getPaneSize();        
        LinearLayout srcLayout = new LinearLayout(context);
        srcLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srcParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT); 
//        LinearLayout.LayoutParams srcParams = new LinearLayout.LayoutParams(size.x, size.y); 
        srcParams.weight = 1f;
        srcLayout.addView(mirrorSource, srcParams);
        srcLayout.addView(new View(context), srcParams); //view place holder.
        
        layout.addView(srcLayout);
//        layout.addView(mirrorView);
        IMirrorView mirrorView = addMirrorView(layout);
        mirrorSource.setMirrorView(mirrorView);
        

        layout.setLayoutParams(sourceView.getLayoutParams());
        

        float scale = config.getPaneScale();
        MirrorSourceFrameLayout.LayoutParams origParams = new MirrorSourceFrameLayout.LayoutParams(size.x, size.y); 
        origParams.gravity = Gravity.CENTER;
        sourceView.setScaleX(scale);
        sourceView.setScaleY(scale);
        mirrorSource.addView(sourceView, origParams);
        return layout;
    }
    
    private int getViewIndexInParent(View v) {
        ViewGroup parent = (ViewGroup)v.getParent();
        int index = -1;
        if (parent != null) {
            for (int i = 0, count = parent.getChildCount(); i < count; i++) {
                if (parent.getChildAt(i) == v) {
                    index = i;
                    break;
                }
            }
        }    
        return index;
    }
}
