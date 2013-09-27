package com.cyanogenmod.trebuchet.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;

import com.aetherar.launcher.R;
import com.cyanogenmod.trebuchet.preference.PreferencesProvider;
import com.dwtech.android.launcher3d.Util;

public class MirrorGLConfig {

    private final Context mContext;
    private final Resources mResources;
    
    public MirrorGLConfig(Context context) {
        mContext = context;
        mResources = context.getResources();
    }
    
    public float getZScale() {
        return PreferencesProvider.Interface.Effect3D.getZScale(
                mResources.getInteger(R.integer.effect_3d_z_scale)) / 100f;
    }
    
    public void putZScale(float scale) {
        PreferencesProvider.Interface.Effect3D.setZScale((int)(scale * 100));
    }
    
    public float getCameraRotation() {
        return PreferencesProvider.Interface.Effect3D.getCameraRotation(
                mResources.getInteger(R.integer.effect_3d_cam_rotation)) / 10f;
    }
    
    public float getCameraPosition() {
        int value = PreferencesProvider.Interface.Effect3D.getCameraDistance(
                mResources.getInteger(R.integer.effect_3d_cam_position));
        return value / Util.getMaxScreenWidthInMM(mResources);
    }
    
    public int getCameraFOV() {
        return PreferencesProvider.Interface.Effect3D.getCameraFOV(
                mResources.getInteger(R.integer.effect_3d_cam_fov));
    }
    
    public boolean isSensorEnabled() {
    	return PreferencesProvider.Interface.Effect3D.getEnableSensor(true);
    }
    
    public float getSensorResetAcceleration() {
    	return PreferencesProvider.Interface.Effect3D.getSensorResetAcceleration(
                mResources.getInteger(R.integer.effect_3d_sensor_reset_acceleration));
    }
    
    public int getShaderType() {
        return PreferencesProvider.Interface.Effect3D.getShaderType(mResources.getInteger(R.integer.effect_3d_shader));
    }
    
    public int getBarrelDistortLevel() {
        return PreferencesProvider.Interface.Effect3D.getCameraRotation(mResources.getInteger(R.integer.effect_3d_barrel_level));
    }
    
    public int getORDistortLevel() {
        return PreferencesProvider.Interface.Effect3D.getCameraRotation(mResources.getInteger(R.integer.effect_3d_oculus_level));
    }
    
    public boolean isHighEnd() {
        return true;//mProductName.startsWith("mproject") || mProductName.startsWith("nakasi") || mProductName.startsWith("u0");
    }

    public void setUseCamera(boolean use) {
        PreferencesProvider.Interface.Effect3D.getUseCamera(true);
    }
    
    public boolean useCamera() {
        return PreferencesProvider.Interface.Effect3D.getUseCamera(true);
    }
    
    private float mPaneScale = 1f;
    public Point getPaneSize() {
    	DisplayMetrics metrics = mResources.getDisplayMetrics();
    	int w = metrics.widthPixels / 2;
    	int h = metrics.heightPixels - getStatusBarHeight();    	
    	int sw = w * PreferencesProvider.Interface.Effect3D.getPaneWidth(100) / 100;
    	int sh = h * PreferencesProvider.Interface.Effect3D.getPaneHeight(100) / 100;
    	
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
