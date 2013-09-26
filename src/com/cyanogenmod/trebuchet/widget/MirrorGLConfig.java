package com.cyanogenmod.trebuchet.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;

import com.aetherar.launcher.R;
import com.cyanogenmod.trebuchet.preference.PreferencesProvider;

public class MirrorGLConfig {

    private final Context mContext;
    private final Resources mResources;
    
    public MirrorGLConfig(Context context) {
        mContext = context;
        mResources = context.getResources();
    }
    
    public float getZScale() {
        return PreferencesProvider.Interface.ThreeD.getZScale(
                mResources.getInteger(R.integer.effect_3d_z_scale)) / 100f;
    }
    
    public void putZScale(float scale) {
        PreferencesProvider.Interface.ThreeD.setZScale((int)(scale * 100));
    }
    
    public float getCameraRotation() {
        return PreferencesProvider.Interface.ThreeD.getZDistance(
                mResources.getInteger(R.integer.effect_3d_cam_distance)) / 10f;
    }
    
    public float getCameraPosition() {
        return PreferencesProvider.Interface.ThreeD.getCameraPosition(
                mResources.getInteger(R.integer.effect_3d_cam_position)) / 100f;
    }
    
    public boolean isSensorEnabled() {
    	return PreferencesProvider.Interface.ThreeD.getEnableSensor(true);
    }
    
    public float getSensorResetAcceleration() {
    	return PreferencesProvider.Interface.ThreeD.getSensorResetAcceleration(
                mResources.getInteger(R.integer.effect_3d_sensor_reset_acceleration));
    }
    
    public int getShaderType() {
        return PreferencesProvider.Interface.ThreeD.getShaderType(mResources.getInteger(R.integer.effect_3d_shader));
    }
    
    public int getBarrelDistortLevel() {
        return PreferencesProvider.Interface.ThreeD.getZDistance(mResources.getInteger(R.integer.effect_3d_barrel_level));
    }
    
    public int getORDistortLevel() {
        return PreferencesProvider.Interface.ThreeD.getZDistance(mResources.getInteger(R.integer.effect_3d_oculus_level));
    }
    
    public boolean isHighEnd() {
        return true;//mProductName.startsWith("mproject") || mProductName.startsWith("nakasi") || mProductName.startsWith("u0");
    }

    public void setUseCamera(boolean use) {
        PreferencesProvider.Interface.ThreeD.getUseCamera(true);
    }
    
    public boolean useCamera() {
        return PreferencesProvider.Interface.ThreeD.getUseCamera(true);
    }
    
    private float mPaneScale = 1f;
    public Point getPaneSize() {
    	DisplayMetrics metrics = mResources.getDisplayMetrics();
    	int w = metrics.widthPixels / 2;
    	int h = metrics.heightPixels - getStatusBarHeight();    	
    	int sw = w * PreferencesProvider.Interface.ThreeD.getPaneWidth(100) / 100;
    	int sh = h * PreferencesProvider.Interface.ThreeD.getPaneHeight(100) / 100;
    	
    	mPaneScale = sw / (float)w;
    	sw = w;
    	sh = (int)(sh / mPaneScale);    	
    	return new Point(sw, sh); 
    }
    
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
  }
    
    public float getPaneScale() {
        return mPaneScale;
    }
    
}
