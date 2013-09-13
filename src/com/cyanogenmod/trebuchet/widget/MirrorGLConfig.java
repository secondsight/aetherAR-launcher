package com.cyanogenmod.trebuchet.widget;

import com.aetherar.launcher.R;
import com.cyanogenmod.trebuchet.preference.PreferencesProvider;

import android.content.Context;
import android.content.res.Resources;

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
    
    public float getSensorResetAcceleration() {
    	float a = PreferencesProvider.Interface.ThreeD.getSensorResetAcceleration(
                mResources.getInteger(R.integer.effect_3d_sensor_reset_acceleration));
        return ((a) * 2 - 40) / 3000f;
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
    
    public boolean useOrientationAPI() {
//        String pn = android.os.Build.PRODUCT;
//        String model = android.os.Build.MODEL;
        return false;//pn.startsWith("j");
    }
    
}
