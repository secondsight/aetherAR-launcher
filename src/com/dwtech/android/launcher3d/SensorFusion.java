package com.dwtech.android.launcher3d;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Arrays;

public class SensorFusion implements SensorEventListener {

    private final Context mContext;
    private final SensorManager mSensorManager;
    
    protected float[] mAcceleratorData = new float[3];
    protected float[] mSensorMagData = new float[3];
    
    protected float[] mRotationMatrix = new float[16];
    protected float[] mFinalRotationMatrix = new float[16];
    protected float[] mInclinationMatrix = new float[16];
    protected float[] mFinalInclinationMatrix = new float[16];
    protected float[] mDefOrientation = new float[3];
    protected float[] mDefOrientationFiltered = new float[3];
    protected float[] mOrientation = new float[3];
    protected float[] mQuaternion = new float[4];
    
    protected boolean mHasSensor;
    protected boolean mHasGravity;
    
    private int mScreenRotation;
    
    public SensorFusion(Context context) {
        mContext = context;
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    }
    
    public float getAzimuth() {
        return mDefOrientationFiltered[0];
    }
    
    public float getInclination() {
        return mDefOrientationFiltered[1];
    }    
    
    public void start() {
        if(mSensorManager != null){
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
//            if(mConfig.useOrientationAPI()) {
//                mSensorManager.registerListener(this,
//                        mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
//            } else {
                mSensorManager.registerListener(this,
                        mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
//            }
        }
    }
    
    public void stop() {
        if(mSensorManager != null){
            mSensorManager.unregisterListener(this);
        }
    }
    
    public void reset() {
        Arrays.fill(mDefOrientationFiltered, 0f);        
    }
    
    public void setScreenRotation(int rotation) {
        if (mScreenRotation != rotation) {
            mScreenRotation = rotation;
            reset();
        }        
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {        
        mHasSensor = true;
        
        float[] values = event.values;
        switch(event.sensor.getType())
        {
            case Sensor.TYPE_GRAVITY:
                mAcceleratorData = lowPass(values.clone(), mAcceleratorData, 0.25f);
                mHasGravity = true;
                break;
                
            case Sensor.TYPE_ACCELEROMETER:
                if (mHasGravity) {
                    break;
                }
                mAcceleratorData = lowPass(values.clone(), mAcceleratorData, 0.25f);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mSensorMagData = lowPass(values.clone(), mSensorMagData, 0.25f);
                break;
            case Sensor.TYPE_GYROSCOPE:
            {
                SensorManager.getRotationMatrix(mRotationMatrix, mInclinationMatrix, mAcceleratorData, mSensorMagData);
                if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    SensorManager.remapCoordinateSystem(mRotationMatrix,
                            SensorManager.AXIS_MINUS_Y,
                            SensorManager.AXIS_X, mFinalRotationMatrix);
                    SensorManager.remapCoordinateSystem(mInclinationMatrix,
                            SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mFinalInclinationMatrix);
                } else {
                    System.arraycopy(mRotationMatrix, 0, mFinalRotationMatrix, 0, mRotationMatrix.length);
                    System.arraycopy(mInclinationMatrix, 0, mFinalInclinationMatrix, 0, mInclinationMatrix.length);
                }
                SensorManager.getOrientation(mFinalRotationMatrix, mOrientation);
                SensorManager.getOrientation(mRotationMatrix, mDefOrientation);
                
                // inclination
//                setInclination(mOrientation[1]);
                
                // azimuth
//              mDefOrientation[0] += Math.PI; // 0 ~ 2PI
                
//                Log.d("Lance", "0 = " + values[0] + " 1 = " + values[1]);
                mDefOrientation[0] = Math.abs(values[0]) > 0.06f ? values[0]/5 : 0;
                mDefOrientation[1] = Math.abs(values[1]) > 0.06f ? values[1]/5 : 0;
                float resetSpeed = 0;//mConfig.getSensorResetAcceleration();
                // linearly map to 0.01~0.2
                float a = (0.2f - 0.05f) / (100 - 20);
                resetSpeed = a * resetSpeed + (0.05f - 20 * a);
                mDefOrientationFiltered = lowPass(mDefOrientation.clone(), mDefOrientationFiltered, resetSpeed);
                
            }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }    

    private float[] lowPass( float[] input, float[] output, float deltaWeigh) {
        if ( output == null ) return input;     
        for ( int i=0; i<input.length; i++ ) {
            float delta = input[i] - output[i];
            if (Math.abs(delta) > Math.PI) {
                output[i] = input[i];
            } else {
                output[i] = output[i] + deltaWeigh * (delta);
            }
        }
        return output;
    }
}
