package com.march.easycameralibs.easycam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.march.easycameralibs.common.CameraConstant;
import com.march.easycameralibs.common.CameraInfo;
import com.march.easycameralibs.controller.ConfigController;
import com.march.easycameralibs.controller.DataProcessController;
import com.march.easycameralibs.controller.LightController;
import com.march.easycameralibs.helper.LogHelper;
import com.march.easycameralibs.widgets.CamContainerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * babyphoto_app     com.babypat.common
 * Created by 陈栋 on 16/3/5.
 * 功能:  处理拍照的核心功能
 */
public class CameraNative {

    //当前初始化的拍摄状态
    private int mCurrentSize = CameraConstant.Four2Three;
    private int mCurrentCameraId = CameraConstant.CAMERA_FACING_BACK;
    private int mTakeMode = CameraConstant.Mode_PIC;
    private boolean isAllowRememberLastTimeMode = true;

    private Camera mCameraInst;
    private Camera.Parameters parameters;
    private Camera.Size previewSize, pictureSize;

    private Context context;
    private SurfaceView surfaceView;
    private CamContainerView mCamContainerView;
    private static CameraNative mCameraNative;
    private Handler handler;
    //感应器，当横向拍照时自动旋转
    private OrientationEventListener mScreenOrientationEventListener;
    private File saveDir;

    private int width, height;
    private float angle = 0;
    private static Integer saveNum = 0;
    private int takeNum = 0;
    private boolean isCanTakePic = true;
    private boolean checkSaveIsOver = false;
    private boolean isStartPublishSaveProgress = false;
    private byte[] buffer;
    //处理闪光灯
    private LightController mLightController;
    //处理拍摄到的数据
    private DataProcessController mDataProcessController;
    //配置参数
    private ConfigController mConfigController;

    /**
     * 私有化构造方法
     */
    private CameraNative(Context context, CamContainerView mCamContainerView) {
        this.context = context;
        this.surfaceView = mCamContainerView.getSurfaceView();
        this.mCamContainerView = mCamContainerView;
        LogHelper.newInst(context);
        handler = new Handler();
        width = context.getResources().getDisplayMetrics().widthPixels;
        height = context.getResources().getDisplayMetrics().heightPixels;
        //保证保存路径不为空
        saveDir = Environment.getExternalStorageDirectory();
        mLightController = new LightController(mCameraInst);
        mDataProcessController = new DataProcessController(saveDir);
        mConfigController = new ConfigController(context);
        //可以使用默认的拍摄模式
        if (isAllowRememberLastTimeMode) {
            this.mTakeMode = mConfigController.getLastTimeMode(CameraConstant.Mode_PIC);
        }
    }


    /**
     * 初始化单例
     *
     * @param context          context
     * @param mCamContainerView mCamContainerView
     */
    public static void newInst(Context context, CamContainerView mCamContainerView) {
        if (mCameraNative == null) {
            synchronized (CameraNative.class) {
                if (mCameraNative == null) {
                    mCameraNative = new CameraNative(context, mCamContainerView);
                }
            }
        }
    }

    /**
     * 获取单例
     *
     * @return mCameraNative
     */
    public static CameraNative getInst() {
        if (mCameraNative == null) {
            throw new IllegalStateException("u must invoke the method newInst() at the first!");
        }
        return mCameraNative;
    }


    // 发送err到监听
    private void publishError(int errorCode, String msg) {
        if (mOnErrorListener != null) {
            mOnErrorListener.error(errorCode, msg);
        }
    }

    /**
     * 公开数据处理方法
     *
     * @param data       数据
     * @return 位图
     */
    public Bitmap handlePicData(byte[] data, CameraInfo info) {
        return mDataProcessController.findBitmap(info, data);
    }

    public CameraInfo getImmediateCamInfo(int sampleSize) {
        return new CameraInfo(angle, mCurrentCameraId, mCurrentSize, sampleSize);
    }

    /**
     * 是否记住上一次的模式
     *
     * @param allowRememberLastTimeMode boolean
     */
    public void setAllowRememberLastTimeMode(boolean allowRememberLastTimeMode) {
        isAllowRememberLastTimeMode = allowRememberLastTimeMode;
    }


    public void setLogEnable(boolean log) {
        LogHelper.get().setLogEnable(log);
    }

    /**
     * 是否初始化完成
     *
     * @return 是否初始化完成
     */
    public boolean isCameraInit() {
        return mCameraInst != null;
    }


    /**
     * 重新初始化状态,可以进行新的拍摄
     */
    public void reInitStatus() {
        angle = 0;
        saveNum = 0;
        takeNum = 0;
        isCanTakePic = true;
        checkSaveIsOver = false;
        isStartPublishSaveProgress = false;
        buffer = null;
    }

    /**
     * 设置保存的路径,应该是个目录
     *
     * @param saveDir 保存图片的路径
     */
    public void setSaveDir(File saveDir) {
        if (!saveDir.isDirectory()) {
            throw new IllegalArgumentException("the file saveDir must be a dir");
        }
        this.saveDir = saveDir;
    }

/**************************************Camera初始化相关SATART****************************************************************************/
    /**
     * 初始化相机参数
     */
    public void initCamera() {
        mCameraInst.stopPreview();
        parameters = mCameraInst.getParameters();

        mConfigController.setFocusMode(parameters);
        mConfigController.setDisplay(mCameraInst, parameters, mCurrentCameraId);
        List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();
        int[] ints = supportedPreviewFpsRange.get(supportedPreviewFpsRange.size() - 1);
        parameters.setPreviewFpsRange(ints[0], ints[1]);
        try {
            mCameraInst.setParameters(parameters);
        } catch (Exception e) {
            LogHelper.get().printInfo("initCamera 设置出错" + e.getMessage());
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement s : stackTrace) {
                LogHelper.get().printInfo(s.toString());
            }
        }

        resetCameraSize();

        startPreview();
        mCameraInst.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    /**
     * 释放camera
     */
    public void releaseCamera() {
        if (mCameraInst != null) {
            mCameraInst.setPreviewCallback(null);
            mCameraInst.release();
            mCameraInst = null;
        }
    }

    /**
     * 对制定点定点对焦的代码
     *
     * @param x 触摸点x
     * @param y 触摸点y
     */
    public void pointFocus(float x, float y) {
        mCameraInst.cancelAutoFocus();
        parameters = mCameraInst.getParameters();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (parameters.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> areas = new ArrayList<>();
                //xy变换了
                int rectY = (int) (-x * 2000 / width + 1000);
                int rectX = (int) (y * 2000 / height - 1000);

                int left = rectX < -900 ? -1000 : rectX - 100;
                int top = rectY < -900 ? -1000 : rectY - 100;
                int right = rectX > 900 ? 1000 : rectX + 100;
                int bottom = rectY > 900 ? 1000 : rectY + 100;
                Rect area1 = new Rect(left, top, right, bottom);
                areas.add(new Camera.Area(area1, 800));
                parameters.setMeteringAreas(areas);
            }
            mConfigController.setFocusMode(parameters);
        }
        mCameraInst.setParameters(parameters);
    }


    /**
     * 获取指定摄像头的camera对象
     *
     * @param id camera id
     */
    public void openCamera(final int id) {
        try {
            mCameraInst = Camera.open(id);
        } catch (Exception e) {
            publishError(CameraConstant.ERROR_OPEN_CAMERA_FAILED, "open failed");
            e.printStackTrace();
        }
    }

    public void initSurfaceHolder() {
        try {
            mCameraInst.setPreviewDisplay(surfaceView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
            LogHelper.get().printError("initSurfaceHolder 出错");
        }
    }

    //开始预览
    public void startPreview() {
        try {
            mCameraInst.startPreview();
        } catch (Exception e) {
            LogHelper.get().printError("startPreview 出错");
        }
    }
/**************************************Camera初始化相关END,切换图片大小，拍摄模式，镜头转向START*********************************************************/

    /**
     * 制定修改图片的大小
     *
     * @param size 图片大小 可选 One2One,Four2Three,AutoSwitch;
     */
    public void switchPicSize(int size) {
        if (size == CameraConstant.AutoSwitch) {
            if (mCurrentSize == CameraConstant.One2One)
                this.mCurrentSize = CameraConstant.Four2Three;
            else
                this.mCurrentSize = CameraConstant.One2One;
        } else {
            this.mCurrentSize = size;
        }
        mCamContainerView.changeDisplayUI();
    }

    /**
     * 修改图片大小的同时进行UI的切换
     *
     * @param size 图片大小
     * @param iv   切换图标的View
     * @param res  资源数组
     */
    public void switchPicSize(int size, ImageView iv, int... res) {
        switchPicSize(size);
        if (iv != null && res != null && res.length == 2) {
            if (mCurrentSize == CameraConstant.One2One)
                iv.setImageResource(res[0]);
            else
                iv.setImageResource(res[1]);
        } else {
            LogHelper.get().printError("if u do not want to change image res,use switchPicSize(int size) please!");
        }
    }

    /**
     * 切换制定摄像头
     *
     * @param cameraId cameraId CAMERA_FACING_BACK,CAMERA_FACING_FRONT,AutoSwitch
     * @return 是否切换成功
     */
    public boolean switchCameraFacing(int cameraId) {
        if (!mConfigController.isCanSwitchCameraFacing()) {
            LogHelper.get().printError("This device not support switch camera!");
            return false;
        }
        //自动切换
        if (cameraId == CameraConstant.AutoSwitch) {
            mCurrentCameraId = (mCurrentCameraId + 1) % mConfigController.getCameraNumbers();
        } else if (mCurrentCameraId != cameraId) {
            mCurrentCameraId = (mCurrentCameraId + 1) % mConfigController.getCameraNumbers();
        }
        releaseCamera();
        setUpCamera(mCurrentCameraId);
        return true;
    }

    /**
     * 设置拍摄模式gif或者pic
     *
     * @param mode 拍摄模式
     */
    public void switchTakeMode(int mode) {
        if (mode == CameraConstant.AutoSwitch) {
            if (mTakeMode == CameraConstant.Mode_GIF)
                mTakeMode = CameraConstant.Mode_PIC;
            else
                mTakeMode = CameraConstant.Mode_GIF;
        } else {
            mTakeMode = mode;
        }
        resetCameraSize();
    }

    /**
     * 获取当前照片大小
     *
     * @return size one2one four2three
     */
    public int getCurrentSize() {
        return mCurrentSize;
    }

    /**
     * 获取当前镜头方向
     *
     * @return cameraId front back
     */
    public int getCurrentCameraId() {
        return mCurrentCameraId;
    }

    /**
     * 获取当前拍摄模式
     *
     * @return gif 和 pic
     */
    public int getTakeMode() {
        return mTakeMode;
    }
/**********************************************切换图片大小，拍摄模式，镜头转向END,闪光灯START************************************************************/
    /**
     * 切换闪光，不切换资源
     */
    public void switchLightWithAuto() {
        switchLightWithAuto(null);
    }

    /**
     * 切换闪光，不切换到自动
     */
    public void switchLight() {
        switchLight(null);
    }

    /**
     * 切换闪光灯,包括自动状态,如果当前手机不支持自动闪光会直接切换到关闭
     *
     * @param flashBtn 切换闪光图标的View
     * @param res      on auto off
     */
    public void switchLightWithAuto(ImageView flashBtn, int... res) {
        mLightController.switchLightWithAuto(flashBtn, mCurrentCameraId, mOnFlashChangeListener, res);
    }

    /**
     * 切换闪光,不会切换到自动
     *
     * @param flashBtn 切换图标的View
     * @param res      on off
     */
    public void switchLight(ImageView flashBtn, int... res) {
        mLightController.switchLightWithAuto(flashBtn, mCurrentCameraId, mOnFlashChangeListener, res);
    }

/**********************************************闪光灯END,拍摄照片START************************************************************/


    /**
     * 保存图片到文件
     *
     * @param bitmap           图片
     * @param immediateCamInfo 信息
     * @param fileName         文件名
     */
    private void savePic(Bitmap bitmap, byte[] data, CameraInfo immediateCamInfo, String fileName) {
        //保存
        mDataProcessController.savePic(bitmap, data, fileName, immediateCamInfo, new Runnable() {
            @Override
            public void run() {
                synchronized (saveNum) {
                    saveNum++;
                    if (mOnSavePicListener != null && isStartPublishSaveProgress) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mOnSavePicListener.InSaveProgress(saveNum, saveNum * 1.0f / takeNum);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 拍摄一张图片
     *
     * @param fileName 文件名
     * @param listener 监听
     * @return 是否拍摄成功吧
     */
    public boolean doTakePic(final String fileName, @NonNull final OnTakePicListener listener) {
        if (!isCanTakePic) {
            LogHelper.get().toast("请稍候拍摄");
            return false;
        }
        isCanTakePic = false;
        mCamContainerView.splash();
        try {
            mCameraInst.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    publishPicData(false, data, fileName, listener);
                    startPreview();
                    isCanTakePic = true;
                }
            });
        } catch (Exception e) {
            //捕获拍摄的异常
            e.printStackTrace();
            isCanTakePic = true;
            startPreview();
        }
        return true;

    }

    /**
     * 一次快速连拍,但是不同机型回调的时间差别大
     *
     * @param fileName 文件名
     * @param listener 监听
     */
    public void doTakeOneShotPic(final String fileName, @NonNull final OnTakePicListener listener) {
        mCamContainerView.splash();
        mCameraInst.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                publishPicData(true, data, fileName, listener);
            }
        });
    }


    /**
     * 开启快速连拍
     */
    public void doStartTakeFastPic() {
        mCameraInst.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                buffer = data;
            }
        });
    }

    /**
     * 获取一帧
     *
     * @param fileName 文件名
     * @param listener 监听
     */
    public void doTakeFastPic(String fileName, @NonNull OnTakePicListener listener) {
        mCamContainerView.splash();
        publishPicData(true, buffer, fileName, listener);
    }

    /**
     * 停止快速连拍
     */
    public void doStopFastPic() {
        if (mCameraInst != null)
            mCameraInst.setPreviewCallback(null);
        buffer = null;
    }

    /**
     * 根据回调的配置处理数据
     *
     * @param isFast   是不是快速拍照
     * @param postData 数据
     * @param fileName 文件
     * @param listener 监听
     */
    private void publishPicData(boolean isFast, byte[] postData,
                                String fileName, OnTakePicListener listener) {
        byte[] data;
        if (isFast)
            data = mDataProcessController.convertPreviewData(mCameraInst, postData);
        else
            data = postData;
        int inSampleSize = 1;
        boolean isSave2Local = false;
        CameraInfo immediateCamInfo = getImmediateCamInfo(inSampleSize);
        if (listener != null) {
            inSampleSize = listener.getInSampleSize(data);
            immediateCamInfo = getImmediateCamInfo(inSampleSize);
            listener.onTakePic(data, immediateCamInfo);
            isSave2Local = listener.isSave2Local();
        }
        //如果文件名不为空，存储
        if (fileName != null && isSave2Local) {
            takeNum++;
            savePic(null, postData, immediateCamInfo, fileName);
        }
    }

    /**
     * 拍照完毕启动检测是否存储完毕的程序
     */
    public void takePicOver() {
        //将会启动检测是否存储完毕的程序
        isStartPublishSaveProgress = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                checkSaveIsOver = true;
                while (checkSaveIsOver) {
                    if (mOnSavePicListener != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mOnSavePicListener.InSaveProgress(saveNum, saveNum * 1.0f / takeNum);
                            }
                        });
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (saveNum == takeNum) {
                        if (mOnSavePicListener != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mOnSavePicListener.OnSaveOver();
                                }
                            });
                        }
                        checkSaveIsOver = false;
                    }
                }
            }
        }).start();
    }
/**********************************************拍照END,生命周期START************************************************************/


    /**
     * 关闭旋转监听,横屏拍摄的图片将不会自动旋转
     */
    public void shutDownAutoRotate() {
        if (mScreenOrientationEventListener != null)
            mScreenOrientationEventListener.disable();
        angle = 0;
    }

    public void onResume() {
        mScreenOrientationEventListener = new OrientationEventListener(context, 20000) {
            @Override
            public void onOrientationChanged(int i) {
                // i的范围是0～359
                // 屏幕左边在顶部的时候 i = 90;
                // 屏幕顶部在底部的时候 i = 180;
                // 屏幕右边在底部的时候 i = 270;
                // 正常情况默认i = 0;
                if (45 <= i && i < 135) {
                    angle = 90f;
                } else if (135 <= i && i < 225) {
                    angle = 0;//ExifInterface.ORIENTATION_ROTATE_270;
                } else if (225 <= i && i < 315) {
                    angle = 270f;
                } else {
                    angle = 0;//ExifInterface.ORIENTATION_ROTATE_90;
                }
            }
        };
        mScreenOrientationEventListener.enable();
    }

    public void onPause() {
        if (mScreenOrientationEventListener != null)
            mScreenOrientationEventListener.disable();
        releaseCamera();
    }

    public void onDestroy() {
        doStopFastPic();
        releaseCamera();
        shutDownAutoRotate();
        mCameraNative = null;
        if (isAllowRememberLastTimeMode)
            mConfigController.putLastTimeMode(this.mTakeMode);
    }

/**********************************生命周期END，私有方法START *****************************************************************/
    /**
     * 重新设置camera的预览和照片大小
     */
    private void resetCameraSize() {
        mCameraInst.stopPreview();
        parameters = mCameraInst.getParameters();

        if (mTakeMode == CameraConstant.Mode_GIF) {
            parameters.setPictureFormat(ImageFormat.NV21);
            previewSize = mConfigController.getPropPreviewSize(mCameraInst, 1.3333f, 500);
            pictureSize = mConfigController.getPropPictureSize(mCameraInst, 1.3333f, 500);
        }

        if (mTakeMode == CameraConstant.Mode_PIC) {
            parameters.setPictureFormat(ImageFormat.JPEG);
            previewSize = mConfigController.getMaxPreviewSize(mCameraInst, 1.33333f);//getPropPreviewSize( 1.3333f, 1500000);
            pictureSize = mConfigController.getPropPictureSize(mCameraInst, 1.3333f, 3000000);
        }

        LogHelper.get().printError("计算获取到  preview = " + previewSize.width + "*" + previewSize.height + "   " + "  pic = " +
                pictureSize.width + "*" + pictureSize.height);

        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);

        try {
            mCameraInst.setParameters(parameters);
            startPreview();
        } catch (Exception e) {
            LogHelper.get().printInfo("resetCameraSize 设置出错" + e.getMessage());
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement s : stackTrace) {
                LogHelper.get().printInfo(s.toString());
            }
            startPreview();
        }

        LogHelper.get().printInfo("设置完毕后  preview = " + mCameraInst.getParameters().getPreviewSize().width + "*" + mCameraInst.getParameters().getPreviewSize().height + "   " + "  pic = " +
                mCameraInst.getParameters().getPictureSize().width + "*" + mCameraInst.getParameters().getPictureSize().height);
    }

    /**
     * 二次重新修改相机参数
     *
     * @param mCurrentCameraId2 camera id
     */
    private void setUpCamera(int mCurrentCameraId2) {
        openCamera(mCurrentCameraId2);

        if (mCameraInst != null) {
            try {
                mCameraInst.setPreviewDisplay(surfaceView.getHolder());
                initCamera();
                startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LogHelper.get().toast("镜头切换失败");
        }
    }

/************************************私有方法END,接口和回调START********************************************************/

    /**
     * s闪光灯开启监听，在开启之前做某些操作时实现
     */
    public static abstract class OnFlashChangeListener {

        public boolean OnTurnFlashOn() {
            return true;
        }

        public boolean OnTurnFlashAuto() {
            return true;
        }

        public boolean OnTurnFlashOff() {
            return true;
        }
    }

    public interface OnErrorListener {
        void error(int errorCode, String errMsg);
    }

    public interface OnSavePicListener {
        void InSaveProgress(int num, float percent);

        void OnSaveOver();

    }

    public static abstract class OnTakePicListener {
        //获取拍摄的数据，isOnlyGetOriginData为true时返回原始数据，该方法的速度是相对较快的，因为不进行存储裁剪存储等操作。
        public void onTakePic(byte[] data, CameraInfo info) {

        }

        //是否保存到本地
        public boolean isSave2Local() {
            return true;
        }

        //采样的标准，根据二进制数据大小决定采样率默认是1
        public int getInSampleSize(byte[] data) {
            return 1;
        }
    }

    private OnSavePicListener mOnSavePicListener;
    private OnErrorListener mOnErrorListener;
    private OnFlashChangeListener mOnFlashChangeListener;

    public void setOnFlashChangeListener(OnFlashChangeListener mOnFlashChangeListener) {
        this.mOnFlashChangeListener = mOnFlashChangeListener;
    }

    public void setOnErrorListener(OnErrorListener mOnErrorListener) {
        this.mOnErrorListener = mOnErrorListener;
    }

    public void setOnSavePicListener(OnSavePicListener mOnSavePicListener) {
        this.mOnSavePicListener = mOnSavePicListener;
    }


    private void autoFocus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mCameraInst != null)
                    mCameraInst.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success)
                                initCamera();
                        }
                    });
            }
        };
    }
}
