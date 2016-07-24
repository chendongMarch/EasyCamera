package com.march.easycameralibs.easycam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import com.march.easycameralibs.helper.CamInfo;
import com.march.easycameralibs.helper.CamParaHelper;
import com.march.easycameralibs.widgets.CamContainerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * babyphoto_app     com.babypat.common
 * Created by 陈栋 on 16/3/5.
 * 功能:
 */
public class CameraNative {

    private final String tag = " CameraNative ";
    public static final int CAMERA_FACING_BACK = 0, CAMERA_FACING_FRONT = 1;
    public static final int Mode_GIF = 0, Mode_PIC = 1;
    public static final int One2One = 0, Four2Three = 1;
    public static final int NotConvert = -1;
    private int mCurrentSize = Four2Three;
    private int mCurrentCameraId = CAMERA_FACING_BACK;
    private int mTakeMode = Mode_PIC;

    private Camera cameraInst;
    private Camera.Parameters parameters;
    private Camera.Size previewSize, pictureSize;

    private Context context;
    private SurfaceView surfaceView;
    private CamContainerView camContainerView;
    private static CameraNative cameraNative;
    private Handler handler;


    private OrientationEventListener mScreenOrientationEventListener;
    private File saveDir;
    private ExecutorService saveThread;

    private float angle = 0;
    private static Integer saveNum = 0;
    private int takeNum = 0;
    private boolean isCanTakePic = true;
    private boolean checkSaveIsOver = false;
    private boolean isStartPublishSaveProgress = false;
    private byte[] buffer;


    private void toast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private void printInfo(String msg) {
        Log.i("chendong", msg);
    }


    private int getWidth() {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    private int getHeight() {
        return context.getResources().getDisplayMetrics().heightPixels;
    }


    /**
     * 私有化构造方法
     */
    private CameraNative(Context context, CamContainerView camContainerView) {
        this.context = context;
        this.surfaceView = camContainerView.getSurfaceView();
        this.camContainerView = camContainerView;
        saveThread = Executors.newFixedThreadPool(2);
        handler = new Handler();
        try {
            saveDir = Environment.getExternalStorageDirectory();//FileUtils.getDcimDir("chendong");
        }catch (Exception e){
            e.printStackTrace();
        }

//        int mode = SPUtils.get().getCameraDefaultMode();
//        if (mode == -1) {
//            setTakeMode(CameraNative.Mode_PIC);
//        } else
//            setTakeMode(mode);
    }


    /**
     * 初始化单例
     *
     * @param context context
     * @param camContainerView camcontainerview
     */
    public static void newInst(Context context, CamContainerView camContainerView) {
        if (cameraNative == null) {
            synchronized (CameraNative.class) {
                if (cameraNative == null) {
                    cameraNative = new CameraNative(context, camContainerView);
                }
            }
        }
    }


    /**
     * 获取单例
     *
     * @return cameranative
     */
    public static CameraNative getInst() {
        if (cameraNative == null) {
            throw new IllegalStateException("u must invoke the method newInst() at the first!");
        }
        return cameraNative;
    }


    /**
     * 是否初始化完成
     *
     * @return 是否初始化完成
     */
    public boolean isCameraInit() {
        return cameraInst != null;
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
     * 自定义拍摄回调
     *
     * @param shutterCallback 声音回调
     * @param callback 拍照回调
     */
    public void doTakePic(Camera.ShutterCallback shutterCallback, Camera.PictureCallback callback) {
        cameraInst.takePicture(shutterCallback, null, callback);
    }


    /**
     * 制定修改图片的大小
     *
     * @param size 图片大小
     */
    public void switchPicSize(int size) {
        if (size < 0) {
            if (mCurrentSize == One2One)
                this.mCurrentSize = Four2Three;
            else
                this.mCurrentSize = One2One;
        } else {
            this.mCurrentSize = size;
        }
        camContainerView.changeDisplayUI();
    }

    /**
     * 修改图片大小的同时进行UI的切换
     *
     * @param size 图片大小
     * @param iv 切换图标的View
     * @param res 资源数组
     */
    public void switchPicSize(int size, ImageView iv, int... res) {
        switchPicSize(size);
        if (iv != null && res != null && res.length == 2) {
            if (mCurrentSize == One2One)
                iv.setImageResource(res[0]);
            else
                iv.setImageResource(res[1]);
        } else {
            printError("if u do not want to change image res,use switchPicSize(int size) please!");
        }
    }

    /**
     * 设置拍摄模式gif或者pic
     * @param mode 拍摄模式
     */
    public void switchTakeMode(int mode) {
        if (mode < 0) {
            if (mTakeMode == Mode_GIF)
                mTakeMode = Mode_PIC;
            else
                mTakeMode = Mode_GIF;
        } else {
            mTakeMode = mode;
        }
        resetCameraSize();
    }

    /**
     * 设置拍摄的模式
     *
     * @param mTakeMode
     */
    private void setTakeMode(int mTakeMode) {
        this.mTakeMode = mTakeMode;
    }

    /**
     * 当前是在拍摄图片
     *
     * @return 是在拍摄图片
     */
    public boolean isTakePic() {
        return mTakeMode == Mode_PIC;
    }

    /**
     * 当前是在拍摄gif
     *
     * @return 在快速拍摄
     */
    public boolean isTakeGif() {
        return mTakeMode == Mode_GIF;
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

    /**
     * 获取sdk版本
     *
     * @return 当前sdk
     */
    private int getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 重新设置camera的预览和照片大小
     */
    private void resetCameraSize() {
        cameraInst.stopPreview();
        parameters = cameraInst.getParameters();

        if (mTakeMode == Mode_GIF) {
            parameters.setPictureFormat(ImageFormat.NV21);
            previewSize = CamParaHelper.getPropPreviewSize(cameraInst, 1.3333f, 500);
            pictureSize = CamParaHelper.getPropPictureSize(cameraInst, 1.3333f, 500);
        }

        if (mTakeMode == Mode_PIC) {
            parameters.setPictureFormat(ImageFormat.JPEG);
            previewSize = CamParaHelper.getMaxPreviewSize(cameraInst, 1.33333f);//getPropPreviewSize( 1.3333f, 1500000);
            pictureSize = CamParaHelper.getPropPictureSize(cameraInst, 1.3333f, 3000000);
        }

        printError("计算获取到  preview = " + previewSize.width + "*" + previewSize.height + "   " + "  pic = " +
                pictureSize.width + "*" + pictureSize.height);

        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);

        try {
            cameraInst.setParameters(parameters);
            cameraInst.startPreview();
        } catch (Exception e) {
            printInfo("resetCameraSize 设置出错" + e.getMessage());
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement s : stackTrace) {
                printInfo(s.toString());
            }
            cameraInst.startPreview();
        }

        printInfo("设置完毕后  preview = " + cameraInst.getParameters().getPreviewSize().width + "*" + cameraInst.getParameters().getPreviewSize().height + "   " + "  pic = " +
                cameraInst.getParameters().getPictureSize().width + "*" + cameraInst.getParameters().getPictureSize().height);
    }

    /**
     * 控制图像的正确显示方向
     */
    private void setDispaly(Camera.Parameters parameters) {
        if (Build.VERSION.SDK_INT >= 23 && mCurrentCameraId == CAMERA_FACING_FRONT) {
            setDisplayOrientation(270);
        } else if (Build.VERSION.SDK_INT >= 8) {
            setDisplayOrientation(90);
        } else {
            parameters.setRotation(90);
        }
    }


    /**
     * 实现的图像的正确显示
     */
    private void setDisplayOrientation(int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = cameraInst.getClass().getMethod("setDisplayOrientation",
                    new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(cameraInst, new Object[]{i});
            }
        } catch (Exception e) {
            Log.e("Came_e", "图像出错");
        }
    }


    private void setFocusMode(Camera.Parameters para) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && parameters.getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            para.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        } else {
            para.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        }
    }

    /**
     * 初始化相机参数
     */
    public void initCamera() {

        cameraInst.stopPreview();
        parameters = cameraInst.getParameters();

        setFocusMode(parameters);
        setDispaly(parameters);
        List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();
        int[] ints = supportedPreviewFpsRange.get(supportedPreviewFpsRange.size() - 1);
        parameters.setPreviewFpsRange(ints[0], ints[1]);
        try {
            cameraInst.setParameters(parameters);
        } catch (Exception e) {
            printInfo("initCamera 设置出错" + e.getMessage());
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement s : stackTrace) {
                printInfo(s.toString());
            }
        }

        resetCameraSize();

        cameraInst.startPreview();
        cameraInst.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    /**
     * 释放camera
     */
    public void releaseCamera() {
        if (cameraInst != null) {
            cameraInst.setPreviewCallback(null);
            cameraInst.release();
            cameraInst = null;
        }
    }

    /**
     * 定点对焦的代码
     * @param x 触摸点x
     * @param y 触摸点y
     */
    public void pointFocus(float x, float y) {
        cameraInst.cancelAutoFocus();
        parameters = cameraInst.getParameters();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (parameters.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> areas = new ArrayList<>();
                //xy变换了
                int rectY = (int) (-x * 2000 / getWidth() + 1000);
                int rectX = (int) (y * 2000 / getHeight() - 1000);

                int left = rectX < -900 ? -1000 : rectX - 100;
                int top = rectY < -900 ? -1000 : rectY - 100;
                int right = rectX > 900 ? 1000 : rectX + 100;
                int bottom = rectY > 900 ? 1000 : rectY + 100;
                Rect area1 = new Rect(left, top, right, bottom);
                areas.add(new Camera.Area(area1, 800));
                parameters.setMeteringAreas(areas);
            }
            setFocusMode(parameters);
        }
        cameraInst.setParameters(parameters);
    }

    /**
     * 切换制定摄像头
     * @param cameraId camera id
     * @return 切换成功
     */
    public boolean switchCamera(int cameraId) {
        if (!isCanSwitch()) {
            printError("This device not support switch camera!");
            return false;
        }
        if (mCurrentCameraId != cameraId) {
            mCurrentCameraId = (mCurrentCameraId + 1) % getCameraNumbers();
            releaseCamera();
            setUpCamera(mCurrentCameraId);
        }
        return true;
    }

    /**
     * 自由切换
     * @return 切换是否成功
     */
    public boolean switchCamera() {
        if (!isCanSwitch()) {
            printError("This device not support switch camera!");
            return false;
        }
        mCurrentCameraId = (mCurrentCameraId + 1) % getCameraNumbers();
        releaseCamera();
        setUpCamera(mCurrentCameraId);
        return true;
    }


    /**
     * 获取指定摄像头的camera对象
     *
     * @param id camera id
     */
    public void openCamera(final int id) {
        Camera c = null;
        try {
            c = Camera.open(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cameraInst = c;
    }

    /**
     * 二次重新修改相机参数
     *
     * @param mCurrentCameraId2 camera id
     */
    private void setUpCamera(int mCurrentCameraId2) {
        openCamera(mCurrentCameraId2);
        if (cameraInst != null) {
            try {
                cameraInst.setPreviewDisplay(surfaceView.getHolder());
                initCamera();
                cameraInst.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            toast("切换失败");
        }
    }


    /**
     * 检查该摄像头存不存在
     *
     * @param facing camera id
     * @return 是否是该摄像头
     */
    private boolean checkCameraFacing(final int facing) {
        if (getSdkVersion() < Build.VERSION_CODES.GINGERBREAD) {
            return false;
        }
        final int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, info);
            if (facing == info.facing) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否可以切换
     *
     * @return 可以切换
     */
    private boolean isCanSwitch() {
        return checkCameraFacing(CAMERA_FACING_FRONT) && checkCameraFacing(CAMERA_FACING_BACK);
    }


    /**
     * 获取相机的摄像头数量
     *
     * @return 摄像头数量
     */
    private int getCameraNumbers() {
        return Camera.getNumberOfCameras();
    }


    public void initSurfaceHolder() {
        try {
            cameraInst.setPreviewDisplay(surfaceView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
            printError("initSurfaceHolder 出错");
        }
    }

    public void startPreview() {
        try {
            cameraInst.startPreview();
        } catch (Exception e) {
            printError("startPreview 出错");
        }

    }

    public int getTakeNum() {
        return takeNum;
    }

    public boolean isSizeOne2One() {
        return mCurrentSize == One2One;
    }

    public boolean isFour2Three() {
        return mCurrentSize == Four2Three;
    }


    /**
     * 切换闪光灯,包括自动状态,如果当前手机不支持自动闪光会直接切换到关闭
     *
     * @param flashBtn 切换闪光图标的View
     * @param res      on auto off
     */
    public void toogleLightWithAuto(ImageView flashBtn, int... res) {
        if (mCurrentCameraId == CAMERA_FACING_FRONT) {
            printError("facing front camera not support change flash mode");
            return;
        }
        if (cameraInst == null || cameraInst.getParameters() == null
                || cameraInst.getParameters().getSupportedFlashModes() == null) {
            printError("camera not init over");
            return;
        }

        if (res != null && res.length != 3 && flashBtn != null) {
            printError("This method is not allow auto light,u must provide 3 image resource!");
            return;
        }

        Camera.Parameters parameters = cameraInst.getParameters();
        String mode = cameraInst.getParameters().getFlashMode();
        List<String> foucusMode = cameraInst.getParameters().getSupportedFlashModes();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)
                && foucusMode.contains(Camera.Parameters.FLASH_MODE_ON)) {//关闭状态切换到开启状态
            //如果有监听,查看监听的结果,没有监听直接切换
            if (mOnFlashChangeListener != null) {
                if (mOnFlashChangeListener.OnTurnFlashOn()) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    cameraInst.setParameters(parameters);
                    if (flashBtn != null && res != null)
                        flashBtn.setImageResource(res[0]);
                }
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                cameraInst.setParameters(parameters);
                if (flashBtn != null && res != null)
                    flashBtn.setImageResource(res[0]);
            }
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(mode)) {//开启状态切换到自动状态,如果不支持自动状态切换到关闭状态
            if (foucusMode.contains(Camera.Parameters.FLASH_MODE_AUTO)) {//有自动状态,切换到自动状态
                if (mOnFlashChangeListener != null) {
                    if (mOnFlashChangeListener.OnTurnFlashAuto()) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                        cameraInst.setParameters(parameters);
                        if (flashBtn != null && res != null)
                            flashBtn.setImageResource(res[1]);
                    }
                } else {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    cameraInst.setParameters(parameters);
                    if (flashBtn != null && res != null)
                        flashBtn.setImageResource(res[1]);
                }
            } else if (foucusMode.contains(Camera.Parameters.FLASH_MODE_OFF)) {//没有自动状态,切换到关闭状态
                if (mOnFlashChangeListener != null) {
                    if (mOnFlashChangeListener.OnTurnFlashOff()) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        cameraInst.setParameters(parameters);
                        if (flashBtn != null && res != null)
                            flashBtn.setImageResource(res[2]);
                    }
                } else {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    cameraInst.setParameters(parameters);
                    if (flashBtn != null && res != null)
                        flashBtn.setImageResource(res[2]);
                }

            }
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(mode)//自动状态切换到关闭状态
                && foucusMode.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            if (mOnFlashChangeListener != null) {
                if (mOnFlashChangeListener.OnTurnFlashOff()) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    cameraInst.setParameters(parameters);
                    if (flashBtn != null && res != null)
                        flashBtn.setImageResource(res[2]);
                }
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                cameraInst.setParameters(parameters);
                if (flashBtn != null && res != null)
                    flashBtn.setImageResource(res[2]);
            }
        }
    }


    /**
     * 切换闪光,不会切换到自动
     *
     * @param flashBtn 切换图标的View
     * @param res      on off
     */
    public void toogleLight(ImageView flashBtn, int... res) {
        if (res != null && res.length != 2 && flashBtn != null) {
            printError("This method is not allow auto light,u must provide 2 image resource!");
            return;
        }

        Camera.Parameters parameters = cameraInst.getParameters();
        String mode = cameraInst.getParameters().getFlashMode();
        List<String> foucusMode = cameraInst.getParameters().getSupportedFlashModes();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)
                && foucusMode.contains(Camera.Parameters.FLASH_MODE_ON)) {//关闭状态切换到开启状态
            if (mOnFlashChangeListener != null) {
                if (mOnFlashChangeListener.OnTurnFlashOn()) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    cameraInst.setParameters(parameters);
                    if (flashBtn != null && res != null)
                        flashBtn.setImageResource(res[0]);
                }
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                cameraInst.setParameters(parameters);
                if (flashBtn != null && res != null)
                    flashBtn.setImageResource(res[0]);
            }
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(mode)
                && foucusMode.contains(Camera.Parameters.FLASH_MODE_OFF)) {//开启状态切换到关闭状态
            if (mOnFlashChangeListener != null) {
                if (mOnFlashChangeListener.OnTurnFlashOff()) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    cameraInst.setParameters(parameters);
                    if (flashBtn != null && res != null)
                        flashBtn.setImageResource(res[2]);
                }
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                cameraInst.setParameters(parameters);
                if (flashBtn != null && res != null)
                    flashBtn.setImageResource(res[2]);
            }
        }
    }


    /**
     * 关闭旋转监听,横屏拍摄的图片将不会自动旋转
     */
    public void shutDownAutoRotate() {
        if (mScreenOrientationEventListener != null)
            mScreenOrientationEventListener.disable();
        angle = 0;
    }


    /**
     * 拍摄一张图片
     *
     * @param fileName 文件名
     * @param listener 监听
     * @return  是否拍摄成功吧
     */
    public boolean doTakePic(final String fileName, final OnTakePicListener listener) {
        if (isCanTakePic) {
            if (fileName == null) {
                printError("fileName can not be null");
                return false;
            }
            isCanTakePic = false;
            try {
                cameraInst.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        int sample = listener == null ? 1 : listener.getInSampleSize(data);
                        CamInfo immediateCamInfo = getImmediateCamInfo(sample);
                        if (listener != null) {
                            listener.onTakePic(data);
                            if (listener.isConvert2Bitmap()) {
                                listener.onTakePic(findBitmap(immediateCamInfo, data));
                            }
                        }
                        takeNum++;
                        isCanTakePic = true;
                        if (fileName != null)
                            savePic(data, fileName, immediateCamInfo);
                        cameraInst.startPreview();

                    }
                });
            } catch (Exception e) {
                //捕获拍摄的异常
                e.printStackTrace();
                isCanTakePic = true;
                cameraInst.startPreview();
            }

            return true;
        } else {
            toast("请稍候拍摄");
            return false;
        }
    }


    /**
     * 一次快速连拍,但是不同机型回调的时间差别大
     *
     * @param fileName 文件名
     * @param listener 监听
     */
    public void doTakeOneShotPic(final String fileName, final OnTakePicListener listener) {
        cameraInst.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (listener != null) {
                    listener.onTakePic(data);
                }
                if (fileName != null)
                    savePic(data, fileName, getImmediateCamInfo(1));
            }
        });
    }


    /**
     * 开启快速连拍
     */
    public void doStartTakeFastPic() {
        cameraInst.setPreviewCallback(new Camera.PreviewCallback() {
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
    public void doTakeFastPic(String fileName, OnTakePicListener listener) {
        if (listener != null) {
            listener.onTakePic(buffer);
            if (listener.isConvert2Bitmap()) {
                int inSampleSize = listener.getInSampleSize(buffer);
                listener.onTakePic(findPreviewBit(getImmediateCamInfo(inSampleSize), buffer));
            }
        }
        if (fileName != null) {
            CamInfo immediateCamInfo = getImmediateCamInfo(1);
            savePic(buffer, fileName, immediateCamInfo);
        }
    }

    /**
     * 停止快速连拍
     */
    public void doStopFastPic() {
        if (cameraInst != null)
            cameraInst.setPreviewCallback(null);
        buffer = null;
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
                        Thread.sleep(200);
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


    /**
     * 根据文件名获取
     *
     * @param fileName 文件名
     * @return  获取要保存的文件
     */
    private File getSaveFile(String fileName) {
        return new File(saveDir, fileName);
    }

    /**
     * 线程池,保存图片
     *
     * @param data 数据
     * @param filename 文件名
     */
    private void savePic(final byte[] data, final String filename, final CamInfo info) {
        saveThread.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap newBitmap;
                System.gc();
                newBitmap = findBitmap(info, data);

                if (newBitmap != null)
                    if (!newBitmap.isRecycled()) {
                        //保存到sd卡,可替换
                        save2Sd(getSaveFile(filename), newBitmap, 100);

                    } else {
                        printError("bitmap is recycled!");
                    }

                if (newBitmap != null && !newBitmap.isRecycled())
                    newBitmap.recycle();

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
     * 公开数据处理方法
     *
     * @param isFast 是不是快速拍照
     * @param data 数据
     * @param sampleSize 采样率
     * @return 位图
     */
    public Bitmap handlePicData(boolean isFast, byte[] data, int sampleSize) {
        if (isFast) {
            return findPreviewBit(getImmediateCamInfo(sampleSize), data);
        } else {
            return findBitmap(getImmediateCamInfo(sampleSize), data);
        }
    }


    /**
     * 快速拍摄数据处理
     *
     * @param info caminfo
     * @param data 数据
     * @return 处理之后的位图
     */
    private Bitmap findPreviewBit(CamInfo info, byte[] data) {
        if (data == null) {
            return null;
        }
        Camera.Size size = cameraInst.getParameters().getPreviewSize(); //获取预览大小
        final int w = size.width;  //宽度
        final int h = size.height;
        final YuvImage image = new YuvImage(data, ImageFormat.NV21, w, h, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
            printError("image.compressToJpeg fail");
            return null;
        }

        byte[] tmp = os.toByteArray();

        return findBitmap(info, tmp);
    }

    public CamInfo getImmediateCamInfo(int sampleSize) {
        return new CamInfo(angle, mCurrentCameraId, mCurrentSize, sampleSize);
    }


    /**
     * 获取处理后的位图
     *
     * @param data 数据
     * @return 位图
     */
    private Bitmap findBitmap(CamInfo info, byte[] data) {
        if (data == null) {
            return null;
        }
        Bitmap tempBitmap;
        /**
         * 根据镜头以及手机旋转角度调整图片
         */
        if (info.cameraId == CAMERA_FACING_BACK) {
            tempBitmap = convertCameraImg(data, 90 + info.angle, false, info.sampleSize, false);
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                tempBitmap = convertCameraImg(data, 90 + info.angle, true, info.sampleSize, false);

            } else
                tempBitmap = convertCameraImg(data, 270 + info.angle, true, info.sampleSize, false);
        }

        /**
         * 根据图片尺寸截取相关部分
         */
        switch (info.picSize) {
            case One2One:
                if (info.angle != 0)
                    tempBitmap = Bitmap.createBitmap(tempBitmap, (tempBitmap.getWidth() - tempBitmap.getHeight()) / 2, 0, tempBitmap.getHeight(), tempBitmap.getHeight());
                else
                    tempBitmap = Bitmap.createBitmap(tempBitmap, 0, (tempBitmap.getHeight() - tempBitmap.getWidth()) / 2, tempBitmap.getWidth(), tempBitmap.getWidth());
                break;
            case Four2Three:
                break;
        }
        return tempBitmap;
    }


    /**
     * 处理旋转角度和镜面翻转的信息
     *
     * @param data 数据
     * @param degree 角度
     * @param isHorizontalScale 水平翻转
     * @param size 大小
     * @param isVerticalScale 垂直翻转
     * @return 位图
     */
    private Bitmap convertCameraImg(byte[] data, float degree, boolean isHorizontalScale, int size, boolean isVerticalScale) {
        if (data == null) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = size;
        Bitmap bit = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        printInfo("degree is " + degree);
        if (!isHorizontalScale && !isVerticalScale && (Math.abs(degree - 360) < 5 || Math.abs(degree) < 5)) {
            printInfo("此时图片不需要旋转或者镜像翻转处理," +
                    "\n当前sdk版本 = " + Build.VERSION.SDK_INT + "  " +
                    "\n当前手机旋转角度 angle = " + angle + "  " +
                    "\n当前镜头朝向 = " + (mCurrentCameraId == 0 ? "后置" : "前置"));
            return bit;
        }


        int w = bit.getWidth();
        int h = bit.getHeight();
        Matrix matrix = new Matrix();
        if (isHorizontalScale)
            matrix.postScale(1, -1);   //镜像垂直翻转
        if (isVerticalScale)
            matrix.postScale(-1, 1);   //镜像水平翻转
        if (!(Math.abs(degree - 360) < 5 || Math.abs(degree) < 5))
            matrix.postRotate(degree);
        Bitmap convertBmp = Bitmap.createBitmap(bit, 0, 0, w, h, matrix, true);

        if (!bit.isRecycled())
            bit.recycle();
        return convertBmp;
    }


    /**
     * 保存图片到sd
     * @param path 文件路径
     * @param bit 位图
     * @param quality 质量
     */
    private void save2Sd(File path, Bitmap bit, int quality) {
//        ImageUtils.saveImageToSD(context, getSaveFile(filename).getAbsolutePath(), newBitmap, 100);
//        ImageUtils.saveBit2Sd(context, bit, path, quality);
    }

    public void onPause() {
        if (mScreenOrientationEventListener != null)
            mScreenOrientationEventListener.disable();
        releaseCamera();
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

    public void onDestory() {
        doStopFastPic();
        releaseCamera();
        shutDownAutoRotate();
        cameraNative = null;
//        SPUtils.get().putCameraDefaultMode(mTakeMode);
    }


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
        void error(String errMsg);
    }

    public interface OnSavePicListener {
        void InSaveProgress(int num, float percent);

        void OnSaveOver();

    }

    public static abstract class OnTakePicListener {
        public void onTakePic(byte[] data) {

        }

        public void onTakePic(Bitmap bit) {

        }

        public boolean isConvert2Bitmap() {
            return false;
        }

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

    private void printError(String msg) {
        if (mOnErrorListener != null) {
            mOnErrorListener.error(msg);
        }
        Log.e(tag, msg);
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

                if (cameraInst != null)
                    cameraInst.autoFocus(new Camera.AutoFocusCallback() {
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
