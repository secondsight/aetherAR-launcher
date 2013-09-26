package com.cyanogenmod.trebuchet;

//import com.cyanogenmod.trebuchet.preference.PreferencesProvider;
import com.aetherar.launcher.R;
import com.cyanogenmod.trebuchet.widget.IMirrorView;
import com.cyanogenmod.trebuchet.widget.MirrorGLConfig;
import com.dwtech.android.launcher3d.HCamera;
import com.dwtech.android.launcher3d.HMath;
import com.dwtech.android.launcher3d.HMatrix;
import com.dwtech.android.launcher3d.HVector;
import com.dwtech.android.launcher3d.SensorFusion;
import com.dwtech.android.launcher3d.SensorFusion2;
import com.dwtech.android.launcher3d.ShaderManager;
import com.dwtech.android.launcher3d.Util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.nio.FloatBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class AppsGLSurfaceView extends GLSurfaceView implements Renderer, IMirrorView {

    private static final String TAG = AppsGLSurfaceView.class.getSimpleName();
    
    private static final float Z_SCALE_MAX = 1f;
    private static final float Z_SCALE_MIN = 0.3f;
    
    private static final boolean DEBUG_FPS = true;
    
    public float vertices[] = {-1f,1f,0, 1f,1f,0, 1f,-1f,0, -1f,-1f,0};
    public float texCoords[] = {0, 0, 1, 0, 1, 1, 0, 1};
    
    private final ReentrantLock mRenderLock = new ReentrantLock();
    protected static Object mLock = new Object();
    
    private GL10 mGL;
    private int mSdrBg;
    private int mSdrBarrel;
    private int mSdrOculusRift;
    
    private int mBgPositionHandle;
    private int mBgTexHandle;
    
    // barrel
    private int mMVPMatrixHandle;
    private int mPositionHandle;
    private int mTexHandle;
    private int mBarrelLevelHandle;
    private float mBarrelLevel = 0.5f;
    private float mScaleU = 1;
    private float mScaleV = 1;
    private Bitmap mBarrelBitmap;
    private Bitmap mBarrelBitmap2; // floating point precision issue
    private int mBarrelTid;
    private int mBarrelTid2;
    
    // oculus rift
    private int mORPositionHandle;
    private int mORTexHandle;
    private int mORMVPMatrixHandle;
    private int mORHmdWarpHandle;
    private Bitmap mORBitmap;
    private int mORTid;
    
    private float mORDistortLevel = 0.5f;
    private int mCurShader;
    
    private float[] mProjectionMatrix = new float[16];
    private float[] mProjectionMatrixInv = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mViewMatrixInv = new float[16];
    private float[] mMVPMatrix = new float[16];
    private float[] mBgMatrix = new float[16];
    private float[] mBgMatrixFlipped = new float[16];
    private float[] mTempMatrix = new float[16];
    
    private float mZScale = 1f;
    private float mCamRotate = 3f;
    private float mCamPosition;
    
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mCoordBuffer;

    private int mRefBgTid;
    private Bitmap mRefBitmap;
    private int mIconTid;
    private boolean mUploadFull = true;
    
    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private MirrorViewCallback mCallback;
    
    protected AtomicReference<Float> mAzimuth = new AtomicReference<Float>();
    protected AtomicReference<Float> mInclination = new AtomicReference<Float>();
    private HCamera mCam;
    private HVector mCamLookAt = new HVector(0, 0, -1);
    private float mDefInclination = (float)(90.0 * Math.PI / 180);
    private float mInitInclination;
    private int mInitInclinationState = STATE_INVALID;
    private static final int STATE_INVALID = 1;
    private static final int STATE_IGNORE = 2;
    private static final int STATE_COLLECT = 3;
    private int mTick;
    private int mInitAzimuthState = STATE_INVALID;
    private float mInitAzimuth;
    private float mAzimuthRange = (float)(30.0 * Math.PI / 180);
    
    private int mScreenRotation;
    
    private boolean mInactive;
    
    public static final int PREFERENCE_SDR_BARREL = 0;
    public static final int PREFERENCE_SDR_OCULUS_RIFT = 1;
    private int mPreferenceShader;
    private boolean mUseCameraBg = true;
    
    private final ScaleGestureDetector mScaleDetector;
    private final MirrorGLConfig mConfig;
    
    private boolean mShouldOpenCamera;
    private boolean mTouchEventReady;
    
    private SensorFusion2 mSensorFusion;
    
    public AppsGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        Log.i(TAG, "device name = " + android.os.Build.DEVICE);
        Log.i(TAG, "model name = " + android.os.Build.MODEL);
        Log.i(TAG, "product name = " + android.os.Build.PRODUCT);
        
        mConfig = new MirrorGLConfig(context);
        
        // register gyroscope listener
        if (mConfig.isSensorEnabled()) {
        	mSensorFusion = new SensorFusion2(context);
        }
        
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
        
        if (!mUseCameraBg) {
            mRefBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bg2);
        }

        mZScale = mConfig.getZScale();
        mCamRotate = mConfig.getCameraRotation();
        mCamPosition = mConfig.getCameraPosition();
        mPreferenceShader = mConfig.getShaderType();
        setBarrelDistortLevel(mConfig.getBarrelDistortLevel());
        int ordis = mConfig.getORDistortLevel();
        setORDistortLevel(100);
        
        if (supportsEs2)
        {
            // Request an OpenGL ES 2.0 compatible context.
            setEGLContextClientVersion(2);
        }
        
//        setZOrderOnTop(true);
        setEGLConfigChooser(8, 8, 8, 0, 0, 0);
//        getHolder().setFormat(PixelFormat.TRANSLUCENT);
//        getHolder().setFormat(PixelFormat.RGBA_8888);
        mCam = new HCamera(new HVector(0, 0, 0), mCamLookAt, 0);
        
        Matrix.setIdentityM(mBgMatrix, 0);
        Matrix.setIdentityM(mBgMatrixFlipped, 0);
        mBgMatrixFlipped[0] = -1;
        mBgMatrixFlipped[5] = -1;
        
        setRenderer(this);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleController());
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        int o = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        if (mScreenRotation != o) {
            mScreenRotation = o;
            mSensorFusion.setScreenRotation(mScreenRotation);
            resetSenorInitialValues();
        }
        
        setAzimuth(mSensorFusion.getAzimuth());
        setInclination(mSensorFusion.getInclination());
        
        mRenderLock.lock();
        try {
        	if (mShouldOpenCamera) {
        		if (mCamera != null)
                    closeCamera();
                
                initCamera(gl);
                
                openCamera();
                
                mShouldOpenCamera = false;
        	}
            onDrawFrameLocked(gl);
        } finally {
            mRenderLock.unlock();
        }
    }
    
    private void onDrawFrameLocked(GL10 gl) {
        if (DEBUG_FPS) outputFps();
        
        mTick++;
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        draw(gl);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        
        Log.d(TAG, "onSurfaceChanged width = " + width + " height = " + height);
        
        gl.glViewport(0, 0, width, height);
        
        Util.glhPerspectivef(mProjectionMatrix, 90, width/2/(float)height, 0.1f, 1000);
        
        mInitAzimuthState = STATE_INVALID;
        mInitInclinationState = STATE_INVALID;
        mInitAzimuth = 0;
        mInitInclination = 0;
        mTick = 0;
        
        mScreenRotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        
        if (mUseCameraBg) {
            mShouldOpenCamera = true;
        }
    }

    @Override
    public void onAppear() {
        Log.d(TAG, "onAppear");
        if (mConfig.isSensorEnabled()) {
            mSensorFusion.start();
        }
        mUploadFull = true;
        mTick = 0;
        mAzimuth = new AtomicReference<Float>();
        mCam = new HCamera(new HVector(0, 0, 0), mCamLookAt, 0);
        mShouldOpenCamera = true;
    }

    @Override
    public void onDisappear() {
        Log.d(TAG, "onDisappear");
        closeCamera();
        clearDrawRequests();
        mSensorFusion.stop();
        mInactive = true;
        mTick = 0;
        mConfig.putZScale(mZScale);
        mInitAzimuthState = STATE_INVALID;
        mInitInclinationState = STATE_INVALID;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
        Log.i(TAG, "onSurfaceCreated " + gl);
        
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0f);
        mVertexBuffer = Util.getFloatBufferFromFloatArray(vertices);
        mCoordBuffer = Util.getFloatBufferFromFloatArray(texCoords);
        
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glClientActiveTexture(GL11.GL_TEXTURE0);
        gl.glClientActiveTexture(GL11.GL_TEXTURE1);
        gl.glClientActiveTexture(GL11.GL_TEXTURE2);
        gl.glActiveTexture(GL11.GL_TEXTURE0);
        gl.glActiveTexture(GL11.GL_TEXTURE1);
        gl.glActiveTexture(GL11.GL_TEXTURE2);
        gl.glEnable(GL11.GL_BLEND);
        gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        if (DEBUG_FPS || mConfig.isSensorEnabled()){
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        } else {
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
        
        mSdrBg = ShaderManager.createShader(ShaderManager.SDR_BG);
        mBgPositionHandle = GLES20.glGetAttribLocation(mSdrBg, "a_Position");
        mBgTexHandle = GLES20.glGetAttribLocation(mSdrBg, "a_Texcoord");
        
        mSdrBarrel = ShaderManager.createShader(ShaderManager.SDR_BARREL);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mSdrBarrel, "u_MVPMatrix");
        mPositionHandle = GLES20.glGetAttribLocation(mSdrBarrel, "a_Position");
        mTexHandle = GLES20.glGetAttribLocation(mSdrBarrel, "a_Texcoord");
        mBarrelLevelHandle = GLES20.glGetUniformLocation(mSdrBarrel, "uBarrelLevel");
        
        mSdrOculusRift = ShaderManager.createShader(ShaderManager.SDR_OCULUS_RIFT);
        mORMVPMatrixHandle = GLES20.glGetUniformLocation(mSdrOculusRift, "u_MVPMatrix");
        mORPositionHandle = GLES20.glGetAttribLocation(mSdrOculusRift, "a_Position");
        mORTexHandle = GLES20.glGetAttribLocation(mSdrOculusRift, "a_Texcoord");
        mORHmdWarpHandle = GLES20.glGetUniformLocation(mSdrOculusRift, "HmdWarpParam");
        
        mCurShader = mPreferenceShader == PREFERENCE_SDR_OCULUS_RIFT ? mSdrOculusRift : mSdrBarrel;
        GLES20.glUseProgram(mCurShader);
        int posHnd = getPosHandle();
        int texHnd = getTexHandle();
        GLES20.glVertexAttribPointer(posHnd, 3, GLES20.GL_FLOAT, false,
                0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(posHnd);
        GLES20.glVertexAttribPointer(texHnd, 2, GLES20.GL_FLOAT, false,
                0, mCoordBuffer);
        GLES20.glEnableVertexAttribArray(texHnd);
        
        if (mCurShader == mSdrOculusRift) {
            float ordis = mORDistortLevel;
            float x = 5 * ordis;
            float y = 1 * ordis;
            float z = 0.5f * ordis;
            float w = 0.1f * ordis;
            GLES20.glUniform4f(mORHmdWarpHandle, x, y, z, w);
        }
        
        mGL = gl;
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        
        super.surfaceDestroyed(holder);
        mRefBgTid = 0;
        mIconTid = 0;
        mBarrelTid = 0;
        if (mCameraTexture != null) {
            mCameraTexture.release();
            mCameraTexture.setOnFrameAvailableListener(null);
            mCameraTexture = null;
        }
        closeCamera();
    }

    private void draw(GL10 gl) {
    	updateCamera();
        drawBg(gl);
        drawIcons(gl);
    }
    
    private void drawBg(GL10 gl) {
        createBgTexture(gl);
        if (mRefBgTid == 0) {
            return;
        }
        
        if (!mUseCameraBg) {
            GLES20.glUseProgram(mSdrBg);
            GLES20.glActiveTexture(GL10.GL_TEXTURE0);
            GLES20.glBindTexture(GL10.GL_TEXTURE_2D, mRefBgTid);
            
            GLES20.glVertexAttribPointer(mBgPositionHandle, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
            GLES20.glEnableVertexAttribArray(mBgPositionHandle);
            GLES20.glVertexAttribPointer(mBgTexHandle, 2, GLES20.GL_FLOAT, false, 0, mCoordBuffer);
            GLES20.glEnableVertexAttribArray(mBgTexHandle);
            
            // left image
            GLES20.glViewport(0, 0, mSurfaceWidth / 2, mSurfaceHeight);
            GLES20.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
            
            // right image
            GLES20.glViewport(mSurfaceWidth / 2, 0, mSurfaceWidth / 2, mSurfaceHeight);
            GLES20.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
    
            GLES20.glBindTexture(GL10.GL_TEXTURE_2D, 0);
        }
    }

    private static final long SCALE_DELAY_TIME = 100;
    private Bitmap mScaledRequestedBmp = null;
    private Bitmap mPrevRequetedBmp    = null;
    private long   mPrevRequetTime     = 0;
    private void drawIcons(GL10 gl) {
        Bitmap bmp = getRequestedBitmap();

        Bitmap iconBmp = bmp;
        if (iconBmp != null) {
            int sw = iconBmp.getWidth() / 2;
            int sh = iconBmp.getHeight() / 2;
            if (mScaledRequestedBmp == null || mScaledRequestedBmp.isRecycled()
                    || mScaledRequestedBmp.getWidth() < sw
                    || mScaledRequestedBmp.getHeight() < sh) {
                if (mScaledRequestedBmp != null) {
                    mScaledRequestedBmp.recycle();
                }
                mScaledRequestedBmp = Bitmap.createScaledBitmap(iconBmp, sw, sh, false);
            } else {
                Canvas canvas = new Canvas(mScaledRequestedBmp);
                canvas.scale(0.5f, 0.5f, 0, 0);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawBitmap(iconBmp, 0, 0, null);
            }
            iconBmp = mScaledRequestedBmp;
            mPrevRequetTime = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - mPrevRequetTime > SCALE_DELAY_TIME) {
                iconBmp = mPrevRequetedBmp;
            }
        }
        createIconTexture(gl, iconBmp);

//        releaseBitmap(bmp);
        if (iconBmp != null) {
            releaseBitmap(mPrevRequetedBmp);
            mPrevRequetedBmp = bmp;            
        }
        
        if (mTick > 20) mInactive = false;
        
        // when switching workspace and all app, we don't draw GL view
        // otherwise it will flicker. note we are still updating texture
        if (mInactive) return;
        
        // bind camera texture as texture 1
        gl.glActiveTexture(GL10.GL_TEXTURE1);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mRefBgTid);
        if (mHasNewCameraFrame) {
            mHasNewCameraFrame = false;
            mCameraTexture.updateTexImage();
        }
        int texLoc = GLES20.glGetUniformLocation(mCurShader, "tex2");
        GLES20.glUniform1i(texLoc, 1);
        
        int hwEnabledLoc = GLES20.glGetUniformLocation(mCurShader, "uHWacc");
        GLES20.glUniform1i(hwEnabledLoc, mConfig.isHighEnd() ? 1 : 0);
        
        if (mCurShader == mSdrOculusRift) {
            if (!mConfig.isHighEnd()) {
                createORTexture(gl);
                
                gl.glActiveTexture(GL10.GL_TEXTURE2);
                gl.glBindTexture(GL10.GL_TEXTURE_2D, mORTid);
                texLoc = GLES20.glGetUniformLocation(mCurShader, "pretex");
                GLES20.glUniform1i(texLoc, 2);
            }
        } else if (mCurShader == mSdrBarrel){
            float l = 1 - mBarrelLevel * 0.15f;
            GLES20.glUniform1f(mBarrelLevelHandle, l);
            
            if (!mConfig.isHighEnd()) {
                createBarrelTexture(gl);
                
                gl.glActiveTexture(GL10.GL_TEXTURE2);
                gl.glBindTexture(GL10.GL_TEXTURE_2D, mBarrelTid);
                texLoc = GLES20.glGetUniformLocation(mCurShader, "pretex");
                GLES20.glUniform1i(texLoc, 2);
                
//                gl.glActiveTexture(GL10.GL_TEXTURE3);
//                gl.glBindTexture(GL10.GL_TEXTURE_2D, mBarrelTid2);
//                int texLoc2 = GLES20.glGetUniformLocation(mCurShader, "pretex2");
//                GLES20.glUniform1i(texLoc2, 3);
            }
        }
        
        gl.glActiveTexture(GL10.GL_TEXTURE0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mIconTid);
        texLoc = GLES20.glGetUniformLocation(mCurShader, "tex");
        GLES20.glUniform1i(texLoc, 0);
        
        // get Model View Projection matrix handle
        int mvpHnd = getMVPHandle();
        int bg = GLES20.glGetUniformLocation(mCurShader, "uIsBg");
        int locUVScale = GLES20.glGetUniformLocation(mCurShader, "uTCScale");
        int locScale = GLES20.glGetUniformLocation(mCurShader, "uScale"); // to scale camera
        // left
        gl.glViewport(0, 0, mSurfaceWidth / 2, mSurfaceHeight);
        // set it to identity for camera preview we will identity MVP matrix
        Matrix.setIdentityM(mTempMatrix, 0);
        if (mScreenRotation == Surface.ROTATION_270) {
            GLES20.glUniformMatrix4fv(mvpHnd, 1, false, mBgMatrix, 0);
        } else {
            GLES20.glUniformMatrix4fv(mvpHnd, 1, false, mBgMatrixFlipped, 0);
        }
        GLES20.glUniform2f(locUVScale, 1, 1);
        // set as background
        
        GLES20.glUniform1i(bg, 1);
        
        GLES20.glUniform1f(locScale, 0.08f);
        gl.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        
        // Now draw icons
        Matrix.translateM(mTempMatrix, 0, 0, 0, -1f);
        Matrix.rotateM(mTempMatrix, 0, -mCamRotate, 0, 1, 0);
        Matrix.scaleM(mTempMatrix, 0, mZScale, mZScale, mZScale);
        
        mCam.setPosition(-mCamPosition, 0, 0);
        mCam.update();
        float[] camMat = mCam.getViewMatrix();
        Matrix.multiplyMM(mViewMatrix, 0, camMat, 0, mTempMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpHnd, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1i(bg, 0);
        GLES20.glUniform2f(locUVScale, mScaleU, mScaleV);
        GLES20.glUniform1f(locScale, 0.1f);
        gl.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        
        // right image
        gl.glViewport(mSurfaceWidth / 2, 0, mSurfaceWidth / 2, mSurfaceHeight);
        Matrix.setIdentityM(mTempMatrix, 0);
        if (mScreenRotation == Surface.ROTATION_270) {
            GLES20.glUniformMatrix4fv(mvpHnd, 1, false, mBgMatrix, 0);
        } else {
            GLES20.glUniformMatrix4fv(mvpHnd, 1, false, mBgMatrixFlipped, 0);
        }
        GLES20.glUniform1i(bg, 1);
        GLES20.glUniform2f(locUVScale, 1, 1);
        GLES20.glUniform1f(locScale, 0.08f);
        gl.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        
        Matrix.translateM(mTempMatrix, 0, 0, 0, -1f);
        Matrix.rotateM(mTempMatrix, 0, mCamRotate, 0, 1, 0);
        Matrix.scaleM(mTempMatrix, 0, mZScale, mZScale, mZScale);
        
        mCam.setPosition(mCamPosition, 0, 0);
        mCam.update();
        camMat = mCam.getViewMatrix();
        Matrix.multiplyMM(mViewMatrix, 0, camMat, 0, mTempMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpHnd, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1i(bg, 0);
        GLES20.glUniform2f(locUVScale, mScaleU, mScaleV);
        GLES20.glUniform1f(locScale, 0.1f);
        gl.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        
        mTouchEventReady = true;
    }
    
    private SurfaceTexture mCameraTexture = null;
    private Camera mCamera = null;
    private boolean mHasNewCameraFrame = false;
    private void createBgTexture(GL10 gl) {
        if (!mUseCameraBg) {
            GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, mRefBitmap, 0);
            gl.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL11.GL_TEXTURE_MIN_FILTER,
                    GL11.GL_LINEAR);
            gl.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL11.GL_TEXTURE_MAG_FILTER,
                    GL11.GL_LINEAR);
        }
    }

    private int mUploadedImageWidth = 0;
    private void createIconTexture(GL10 gl, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        
        if (mIconTid == 0) {
            int[] tids = new int[1];
            gl.glGenTextures(1, tids, 0);
            mIconTid = tids[0];
        }
        
        int w = bitmap.getWidth();
        gl.glBindTexture(GL11.GL_TEXTURE_2D, mIconTid);
        if (mUploadFull || mUploadedImageWidth != w) {
            GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, bitmap, 0);
            mUploadFull = false;
        } else {
            int format = GLUtils.getInternalFormat(bitmap);
            int type = GLUtils.getType(bitmap);
            GLUtils.texSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, bitmap, format, type);
        }
        mUploadedImageWidth = w;
        gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                mConfig.isHighEnd() ? GL10.GL_LINEAR : GL10.GL_NEAREST);
        gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
                mConfig.isHighEnd() ? GL10.GL_LINEAR : GL10.GL_NEAREST);
    }
    
    // ========================   Bitmap Pool Start  ==================================
    
    private static final int MAX_REQUEST = 3;
    private Deque<Bitmap> mIdleQueue = new LinkedList<Bitmap>();
    private Deque<Bitmap> mReqQueue = new LinkedList<Bitmap>();
    public Bitmap getIdleBitmap(int w, int h) {
        Bitmap bmp = null;
        synchronized (mIdleQueue) {
            bmp = mIdleQueue.pollLast();
        }
        if (bmp == null || bmp.isRecycled() || bmp.getWidth() < w || bmp.getHeight() < h) {
            if (bmp != null) {
                bmp.recycle();
            }
            int glw = Util.nextPowerOf2(w);
            int glh = Util.nextPowerOf2(h);
            
            Log.d(TAG, "Creating bitmap w = " + w + " glw = " + glw + " h = " + glh);
            mScaleU = w / (float)glw;
            mScaleV = h / (float)glh;
            bmp = Bitmap.createBitmap(glw, glh, Config.ARGB_8888);
            
            mRenderLock.lock();
            try {
                if (mCurShader == mSdrBarrel && !mConfig.isHighEnd()) {
                    if (mBarrelBitmap == null || mBarrelBitmap.getWidth() != glw || mBarrelBitmap.getHeight() != glh) {
                        if (mBarrelBitmap != null) mBarrelBitmap.recycle();
                        mBarrelBitmap = createBarrelBitmap(glw, glh);
                    }
                } else if (mCurShader == mSdrOculusRift && !mConfig.isHighEnd()) {
                    if (mORBitmap == null || mORBitmap.getWidth() != glw || mORBitmap.getHeight() != glh) {
                        if (mORBitmap != null) mORBitmap.recycle();
                        mORBitmap = createORBitmap(glw, glh);
                    }
                }
            } finally {
                mRenderLock.unlock();
            }
        }
        return bmp;
    }
    
    private void releaseBitmap(Bitmap bmp) {
        if (bmp == null) return;
        
        synchronized(mIdleQueue) {
            if(mIdleQueue.size() > MAX_REQUEST) {
                Log.d(TAG, "too many bmp in pool: " + mIdleQueue.size());
                bmp.recycle();
                return;
            }
            mIdleQueue.offer(bmp);
        }
    }

    public void postDraw(Bitmap bmp) {
        synchronized(mReqQueue) {
            if (mReqQueue.size() >= MAX_REQUEST) {
                //remove the 1st request add release it.
                releaseBitmap(mReqQueue.pollFirst());
            }
            mReqQueue.offer(bmp);            
        }
        requestRender();
    }
    
    private Bitmap getRequestedBitmap() {
        synchronized(mReqQueue) {
            return mReqQueue.poll();
        }
    }

    @Override
    public void clearDrawRequests() {
        //clear all requests & release the bitmaps
        Bitmap bmp = getRequestedBitmap();
        while(bmp != null) {
            releaseBitmap(bmp);
            bmp = getRequestedBitmap();
        }
    }

    // ========================   Bitmap Pool End  ==================================


    @Override
    public void setSourceView(MirrorViewCallback callback) {   
        if (callback != mCallback) {
            mCallback = callback;
            callback.postInvalidate();            
        }
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
    	mScaleDetector.onTouchEvent(event);
    	
    	if (!mTouchEventReady) return false;
    	
        if (mCallback != null && mSurfaceHeight > 0 && mSurfaceWidth > 0) {
            float mirrorWidth  = mSurfaceWidth / 2;
            float mirrorHeight = mSurfaceHeight;

            float x = event.getX();
            float y = event.getY();
            
            if (x > mirrorWidth) {
                x -= mirrorWidth;
            }
            
//            boolean print = event.getAction() == MotionEvent.ACTION_DOWN;
//            if (print) Log.e("Lance", "x = " + x + " y = " + y);
            
            float nx = 2 * x / mirrorWidth - 1;
            float ny = 1 - 2 * y / mirrorHeight;
//            if (print) Log.d("Lance", "nx = " + nx + " ny = " + ny);
            
            Matrix.invertM(mProjectionMatrixInv, 0, mProjectionMatrix, 0);
            HVector eye = HMatrix.multiply(mProjectionMatrixInv, nx, ny, -1);
//            if (print) Log.i("Lance", "eyex = " + eye.x + " eyey = " + eye.y + " eyez = " + eye.z);
            
            Matrix.invertM(mViewMatrixInv, 0, mViewMatrix, 0);
            HVector world = HMatrix.multiply(mViewMatrixInv, eye.x / (-eye.z), eye.y / (-eye.z), -1);
//            if (print) Log.w("Lance", "worldx = " + world.x + " worldy = " + world.y + " worldz = " + world.z);
            
            float outx = (0.5f + world.x/2) * mirrorWidth;
            float outy = (0.5f - world.y/2) * mirrorHeight;
//            if (print) Log.e("Lance", "screenx = " + outx + " screeny = " + outy);
            event.setLocation(outx, outy);
            
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
    
    
    private void setInclination(float inclination) {
        if (mScreenRotation == Surface.ROTATION_270) {
            inclination = -inclination;
        }
        
        if (mInitInclinationState == STATE_INVALID) {
            if (mTick >= 30) {
                mInitInclinationState = STATE_IGNORE;
            }
        } else if (mInitInclinationState == STATE_IGNORE) { // Average next 10?
            mInitInclination = inclination;
            mInitInclinationState = STATE_COLLECT;
        } else if (mInitInclinationState == STATE_COLLECT){
            mInclination.set(inclination);
        }
        
//        Log.d("Lance", "inc = " + inclination + " init inc = " + mInitInclination);
    }
    
    private void setAzimuth(float azimuth) {
        if (mInitAzimuthState == STATE_INVALID) {
            if (mTick >= 30) {
                mInitAzimuthState = STATE_IGNORE;
            }
        } else if (mInitAzimuthState == STATE_IGNORE) { // Average next 10?
            mInitAzimuth = azimuth;
            mInitAzimuthState = STATE_COLLECT;
        } else if (mInitAzimuthState == STATE_COLLECT){
            mAzimuth.set(azimuth);
        }
        
//        if (Math.abs(mInitAzimuth - azimuth) > Math.PI / 4) {
//            Log.e("Lance", "a = " + azimuth + " init a = " + mInitAzimuth);
//        }
    }
    
    private float clampInclination(float input, float range) {
    	float res = input - mDefInclination;
    	if (res > range) {
    		res = range;
    	} else if (res < -range) {
    		res = -range;
    	}
    	return res;
    }
    
    private void updateCamera() {
    	if (mInclination.get() == null || mAzimuth.get() == null) {
    		return;
    	}
    	
    	mCamLookAt.x = 0;
        mCamLookAt.y = 0;
        mCamLookAt.z = -1;
        HVector lookat = mCamLookAt;
        
        float azimuth = mAzimuth.get().floatValue();
        float delAzimuth = (float)(azimuth - mInitAzimuth);
        float min = (float)HMath.clampBetweenZeroAnd2PI(mInitAzimuth - mAzimuthRange);
        float max = (float)HMath.clampBetweenZeroAnd2PI(mInitAzimuth + mAzimuthRange);
        float clampedAzimuth = delAzimuth;
        
//        if (max > min) { // Normal case
//        	float absAngleMin = (float)HMath.getSmallerAngle(azimuth, min);
//        	float absAngleMax = (float)HMath.getSmallerAngle(azimuth, max);
//        	float minRefAzimuth = (float)((azimuth > max && absAngleMin < absAngleMax) ? azimuth - HMath.PI2 : azimuth);
//        	float maxRefAzimuth = (float)((azimuth < min && absAngleMax < absAngleMin) ? azimuth + HMath.PI2 : azimuth);
//        	if (minRefAzimuth < min && absAngleMin < absAngleMax) {
//        		mInitAzimuth -= absAngleMin; 
////        		Log.d(TAG, "A " + mInitAzimuth + "; min/max = " + min + ", " + max);
//        	} else if (maxRefAzimuth > max && absAngleMax < absAngleMin) {
//        		mInitAzimuth += absAngleMax; 
////        		Log.d(TAG, "B " + mInitAzimuth + "; min/max = " + min + ", " + max);
//        	}
//        } else if (azimuth > max && azimuth < min){
//        	if (azimuth > max && azimuth - max < min - azimuth) {
//        		mInitAzimuth += azimuth - max; 
////        		Log.d(TAG, "C " + mInitAzimuth + "; min/max = " + min + ", " + max);
//        	} else if (azimuth < min && azimuth - max > min - azimuth) {
//        		mInitAzimuth -= min - azimuth; 
////        		Log.d(TAG, "D " + mInitAzimuth + "; min/max = " + min + ", " + max);
//        	} else {
////        		Log.d(TAG, "E " + mInitAzimuth + "; min/max = " + min + ", " + max);
//        	}
//        }
        
        // re-center azimuth
        double delta = HMath.getSmallerAngle(azimuth, mInitAzimuth);
        if (Math.abs(delta) < 0.01f) {
//        	mInitAzimuth = azimuth;
        } else {
        	double step = delta * mConfig.getSensorResetAcceleration();
        	double attemp = Math.abs(HMath.getSmallerAngle(azimuth, HMath.clampBetweenZeroAnd2PI(mInitAzimuth + step)));
//        	if (attemp < delta) {
//    			mInitAzimuth += step;
//        	} else {
//                mInitAzimuth -= step;
//        	}
        }
        mInitAzimuth = (float)HMath.clampBetweenZeroAnd2PI(mInitAzimuth);
        
        // re-center inclination
        float inclination = mInclination.get().floatValue();
        delta = HMath.getSmallerAngle(inclination, mInitInclination);
        if (Math.abs(delta) < 0.01f) {
            mInitInclination = inclination;
        } else {
            double step = delta * mConfig.getSensorResetAcceleration();
            double attemp = Math.abs(HMath.getSmallerAngle(inclination, HMath.clampBetweenZeroAnd2PI(mInitInclination + step)));
//            if (attemp < delta) {
//                mInitInclination += step;
//            } else {
//                mInitInclination -= step;
//            }
        }
        mInitInclination = (float)HMath.clampBetweenZeroAnd2PI(mInitInclination);
        
        if (mInitAzimuthState != STATE_INVALID) {
            lookat.rotate(HVector.BASICY, -clampedAzimuth);
        }
        if (mInitInclinationState != STATE_INVALID) {
            lookat.rotate(HVector.BASICX, inclination - mInitInclination);
        }
        
        mCam.setLookAt(lookat.x, lookat.y, lookat.z);
        mCam.setUp(0, 1, 0);
//        mCam.update();
    }
    
    private void resetSenorInitialValues() {
        mCam = new HCamera(new HVector(0, 0, 0), mCamLookAt, 0);
        mInitAzimuthState = STATE_INVALID;
        mInitInclinationState = STATE_INVALID;
        mTick = 0;
    }
    
    private int getPosHandle() {
        return mCurShader == mSdrOculusRift ? mORPositionHandle : mPositionHandle;
    }
    
    private int getTexHandle() {
        return mCurShader == mSdrOculusRift ? mORTexHandle : mTexHandle;
    }
    
    private int getMVPHandle() {
        return mCurShader == mSdrOculusRift ? mORMVPMatrixHandle : mMVPMatrixHandle;
    }
    
    public void setCurShader(int idx) {
        mPreferenceShader = idx;
        mCurShader = mPreferenceShader == PREFERENCE_SDR_OCULUS_RIFT ? mSdrOculusRift : mSdrBarrel;
    }
    
    public void setBarrelDistortLevel(float percentage) {
        mBarrelLevel = percentage / 100;
    }
    
    public void setORDistortLevel(float percentage) {
        mORDistortLevel = percentage / 100;
    }
    
    // pre-calculated barrel distortion
    private Bitmap createBarrelBitmap(int w, int h) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
//        if (mBarrelBitmap2 == null) {
//            mBarrelBitmap2 = Bitmap.createBitmap(w, h, Config.ARGB_8888);
//        }
        float l = 1 - mBarrelLevel * 0.15f;
        for (int i = 0;i < w; ++i) {
            for (int j = 0;j < h; ++j){
                double s = (i / (double)(w) - 0.5) * 2;
                double t = ((h-j) / (double)(h) - 0.5) * 2;
                
                double theta  = Math.atan2(t,s);
                double radius = Math.sqrt(s * s + t * t);
                radius = Math.pow(radius, l);
                s = (radius * Math.cos(theta) + 1) * 0.5;
                t = (radius * Math.sin(theta) + 1) * 0.5;
                
                // now s & t is within 0~1
                int scale = 1 << 16;
                int it = (int)(t * scale);
                int is = (int)(s * scale);
                
                int r = (is >> 8) & 0xff;
                int g = (it >> 8) & 0xff;
                int b = (is & 0xff) | ((it & 0xf0) >> 4);
                bitmap.setPixel(i, j, (0xff<<24) | (r<<16) | (g<<8) | b);
                
                
//                r = (it >> 16) & 0xff;
//                g = (it >> 8) & 0xff;
//                b = (it) & 0xff;
//                mBarrelBitmap2.setPixel(i, j, (0xff<<24) | (r<<16) | (g<<8) | b);
            }
        }
        
        return bitmap;
    }
    
    private Bitmap createORBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        for (int i = 0;i < width; ++i) {
            for (int j = 0;j < height; ++j){
                double invecs = (double)i / width * mScaleU;
                double invect = (double)j / height * mScaleV;
                double lcx = 0.5 * mScaleU;
                double lcy = 0.5 * mScaleV;
                double thetax = (invecs - lcx) * 2;
                double thetay = (invect - lcy) * 2;
                double rSq = thetax * thetax + thetay * thetay;
                
                float ordis = mORDistortLevel;
                float x = 5 * ordis;
                float y = 1 * ordis;
                float z = 0.5f * ordis;
                float w = 0.1f * ordis;
                double rx = thetax * (x + y * rSq + z * rSq * rSq + w * rSq * rSq * rSq);
                double ry = thetay * (x + y * rSq + z * rSq * rSq + w * rSq * rSq * rSq);
                double s = lcx + 0.1 * rx;
                double t = lcy + 0.1 * ry;
                
                if (s < 0 || s > 1 || t < 0 || t > 1) {
                    s = 0;
                    t = 0;
                    bitmap.setPixel(i, j, 0);
                } else {
                    // now s & t is within 0~1
                    int scale = 1 << 16;
                    int it = (int)(t * scale);
                    int is = (int)(s * scale);
                    
                    int r = (is >> 8) & 0xff;
                    int g = (it >> 8) & 0xff;
                    int b = (is & 0xff) | ((it & 0xf0) >> 4);
                    bitmap.setPixel(i, j, (0xff<<24) | (r<<16) | (g<<8) | b);
                }
            }
        }
        return bitmap;
    }
    
    private void createBarrelTexture(GL10 gl) {
        if (mBarrelTid == 0 && mBarrelBitmap != null) {
            int[] tids = new int[1];
            gl.glGenTextures(1, tids, 0);
            mBarrelTid = tids[0];
            
            gl.glBindTexture(GL11.GL_TEXTURE_2D, mBarrelTid);
            GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, mBarrelBitmap, 0);
            gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            gl.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        
        if (mBarrelTid2 == 0 && mBarrelBitmap2 != null) {
            int[] tids = new int[1];
            gl.glGenTextures(1, tids, 0);
            mBarrelTid2 = tids[0];
            
            gl.glBindTexture(GL11.GL_TEXTURE_2D, mBarrelTid2);
            GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, mBarrelBitmap2, 0);
            gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            gl.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
    }
    
    private void createORTexture(GL10 gl) {
        if (mORTid == 0 && mORBitmap != null) {
            int[] tids = new int[1];
            gl.glGenTextures(1, tids, 0);
            mORTid = tids[0];
            
            gl.glBindTexture(GL11.GL_TEXTURE_2D, mORTid);
            GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, mORBitmap, 0);
            gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            gl.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
    }
    
    private int mFPS;
    private int mFrameCount = 0;
    private long mFrameCountingStart = 0;
    private long mElapsedTime;
    private void outputFps() {
        // accurate & simple FPS calculation
        // http://blogs.msdn.com/b/shawnhar/archive/2007/06/08/displaying-the-framerate.aspx
        long now = System.nanoTime();
        if (mFrameCountingStart != 0) {
            mElapsedTime += now - mFrameCountingStart;
            if (mElapsedTime > 1000000000) {
            	mFPS = mFrameCount;
                Log.d(TAG, "fps: " + mFPS);
                mFrameCount = 0;
                mElapsedTime -= 1000000000;
            }
        }
        mFrameCountingStart = now;
        ++mFrameCount;
    }
    
    public int getFPS() {
    	return mFPS;
    }
    
    private class ScaleController implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
			float scaleFactor = detector.getScaleFactor();
			if ((mZScale <= Z_SCALE_MIN && scaleFactor < 1f)
					|| (mZScale >= Z_SCALE_MAX && scaleFactor > 1f)) {
				return false;
			}
            
            float scale = mZScale * scaleFactor;
            mZScale = Util.clamp(scale, Z_SCALE_MIN, Z_SCALE_MAX);
			return true;
		}

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
//        	if (mZScale <= Z_SCALE_MIN || mZScale >= Z_SCALE_MAX) {
//        		return false;
//        	} else {
        		return true;
//        	}
		}

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {  
		}
    	
    }
    
    private void initCamera(GL10 gl) {
        if (mRefBgTid == 0 || mGL != gl) {
            int[] tids = new int[1];
            GLES20.glGenTextures(1, tids, 0);
            mRefBgTid = tids[0];
            
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mRefBgTid);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER,
                    mConfig.isHighEnd() ? GL10.GL_NEAREST : GL10.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER,
                    mConfig.isHighEnd() ? GL10.GL_NEAREST : GL10.GL_NEAREST);
            // Clamp to edge is only option.
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S,
                    GL10.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T,
                    GL10.GL_CLAMP_TO_EDGE);
        }
        
        mHasNewCameraFrame = false;
        if (mCameraTexture == null || mGL != gl) {
            mCameraTexture = new SurfaceTexture(mRefBgTid);
            mCameraTexture.setOnFrameAvailableListener(new OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surface) {
                    mHasNewCameraFrame = true;
                }
            });
        }
    }
    
    private void openCamera() {
        if (mCamera != null) return;
        
        synchronized (mLock) {
            try {
                
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                
                Camera.Parameters params = mCamera.getParameters();
                int previewWidth = params.getPreviewSize().width;
                int previewHeight = params.getPreviewSize().height;
                Log.i(TAG, "camera preview default w = " + previewWidth + " h = " + previewHeight);
                List<Size> list = mCamera.getParameters().getSupportedPictureSizes();
                int minWidth=Integer.MAX_VALUE;
                int minHeight=Integer.MAX_VALUE;
                for(int i = 0; i<list.size(); i++){
                    if(list.get(i).height<minHeight){ // align vertical
                        minWidth = list.get(i).width;
                        minHeight = list.get(i).height;
                    }
                }
                previewWidth = minWidth;
                previewHeight = minHeight;
                    
                Log.i(TAG, "set camera preview w = " + previewWidth + " h = " + previewHeight);
                params.setPreviewSize(previewWidth, previewHeight);
                
                mCameraTexture.setDefaultBufferSize(previewWidth, previewHeight);
                mCamera.setPreviewTexture(mCameraTexture);
                mCamera.startPreview();
                mCamera.autoFocus(new Camera.AutoFocusCallback(){

                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (!success) {
                        	try {
                        		camera.autoFocus(this);
                        	} catch(Exception e) {
                        		e.printStackTrace();
                        	}
                        }
                    }
                    
                });
                Log.i(TAG, "camera opened");
            } catch (Exception e) {
                Log.e(TAG, "failed to open camera. ", e);
                closeCamera();
            }
        }
    }
    
    private synchronized void closeCamera() {
        try {
            if (mCamera != null) {
                synchronized (mLock) {
                    mCamera.release();
                    mCamera = null;
                    Log.i(TAG, "Camera closed");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
