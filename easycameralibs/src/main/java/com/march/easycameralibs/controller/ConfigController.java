package com.march.easycameralibs.controller;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.march.easycameralibs.common.CameraConstant;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * com.march.easycameralibs.easycam
 * EasyCamera
 * Created by chendong on 16/7/25.
 * Copyright © 2016年 chendong. All rights reserved.
 * Desc : 帮助Camera完成部分参数配置
 */
public class ConfigController {

    private Context context;
    private CameraSizeComparator sizeComparator;
    private boolean isLog = true;

    public ConfigController(Context context) {
        this.context = context;
        this.sizeComparator = new CameraSizeComparator();
    }

    //   toast提示
    public void toast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    //  打印info消息
    public void printInfo(String msg) {
        if (isLog)
            Log.i(CameraConstant.TAG, msg);
    }

    //    打印error提示
    public void printError(String msg) {
        if (isLog)
            Log.e(CameraConstant.TAG, msg);
    }

    public void setLogEnable(boolean log) {
        isLog = log;
    }

    public int getCameraNumbers() {
        return Camera.getNumberOfCameras();
    }


    /**
     * 控制图像的正确显示方向
     */
    public void setDisplay(Camera mCameraInst, Camera.Parameters parameters, int mCurrentCameraId) {
        if (Build.VERSION.SDK_INT >= 23 && mCurrentCameraId == CameraConstant.CAMERA_FACING_FRONT) {
            setDisplayOrientation(mCameraInst, 270);
        } else if (Build.VERSION.SDK_INT >= 8) {
            setDisplayOrientation(mCameraInst, 90);
        } else {
            parameters.setRotation(90);
        }
    }


    /**
     * 实现的图像的正确显示
     */
    private void setDisplayOrientation(Camera mCameraInst, int degree) {
        //反射处理角度
        Method downPolymorphic;
        try {
            downPolymorphic = mCameraInst.getClass().getMethod("setDisplayOrientation",
                    int.class);
            if (downPolymorphic != null) {
                downPolymorphic.invoke(mCameraInst, degree);
            }
        } catch (Exception e) {
            printError("反射设置方向失败");
        }
    }


    //    设置对焦模式，分版本
    public void setFocusMode(Camera.Parameters parameters) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && parameters.getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        }
    }


    /**
     * size比较器
     */
    private static class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    //获取上次拍摄模式
    public int getLastTimeMode(int defaultMode) {
        return context.getSharedPreferences(CameraConstant.KEY_SP_STORE, Context.MODE_PRIVATE)
                .getInt(CameraConstant.KEY_SP_MODE, defaultMode);
    }

    //put拍摄模式
    public void putLastTimeMode(int mode) {
        context.getSharedPreferences(CameraConstant.KEY_SP_STORE, Context.MODE_PRIVATE).edit()
                .putInt(CameraConstant.KEY_SP_MODE, mode).apply();
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
     * 是否可以反转镜头
     *
     * @return bool
     */
    public boolean isCanSwitchCameraFacing() {
        return checkCameraFacing(CameraConstant.CAMERA_FACING_FRONT)
                && checkCameraFacing(CameraConstant.CAMERA_FACING_BACK);
    }

    /**
     * 获取当前设备的最大的预览size
     *
     * @param th         比率
     * @param cameraInst camera
     * @return size 尺寸
     */
    public Camera.Size getMaxPreviewSize(Camera cameraInst, float th) {
        List<Camera.Size> list = cameraInst.getParameters().getSupportedPreviewSizes();
        printSize("preview", list);
        Collections.sort(list, sizeComparator);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (equalRate(list.get(i), th)) {
                return list.get(i);
            }
        }
        return null;
    }

    //获取最适合的预览
    public Camera.Size getPropPreviewSize(Camera cameraInst, float th, int minWidth) {
        List<Camera.Size> list = cameraInst.getParameters().getSupportedPreviewSizes();
        printSize("preview", list);
        Collections.sort(list, sizeComparator);

        for (int i = 0; i < list.size(); i++) {
            Camera.Size s = list.get(i);
            if (equalRate(s, th)) {
                if (minWidth > 100000) {
                    if (s.width * s.height >= minWidth) {
                        return s;
                    }
                } else {
                    if (s.width >= minWidth) {
                        return s;
                    }
                }
            }
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            Camera.Size s = list.get(i);
            if (equalRate(s, th)) {
                return s;
            }
        }

        return list.get(0);
    }


    /**
     * * 获取合适照片大小
     *
     * @param th         宽高比,大于1(1,1.33,1.77)
     * @param minWidth   最小宽度
     * @param cameraInst camera
     * @return size
     */
    public Camera.Size getPropPictureSize(Camera cameraInst, float th, int minWidth) {
        List<Camera.Size> list = cameraInst.getParameters().getSupportedPictureSizes();
        printSize("picture", list);
        Collections.sort(list, sizeComparator);
        for (int i = 0; i < list.size(); i++) {
            Camera.Size s = list.get(i);
            if (equalRate(s, th)) {
                if (minWidth > 100000) {
                    if (s.width * s.height >= minWidth) {
                        return s;
                    }
                } else {
                    if (s.width >= minWidth) {
                        return s;
                    }
                }
            }
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            Camera.Size s = list.get(i);
            if (equalRate(s, th)) {
                return s;
            }
        }
        return list.get(0);
    }

    /**
     * 尺寸比率
     *
     * @param s    size
     * @param rate 比率
     * @return 是否匹配
     */
    private boolean equalRate(Camera.Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        return Math.abs(r - rate) <= 0.03;
    }


    private void printSize(String txt, List<Camera.Size> sizes) {
        printInfo("type is " + txt);
        for (Camera.Size s : sizes) {
            printInfo("print( " + s.width + " * " + s.height + " rate = " + s.width * 1.0f / s.height + "  piexls = " + s.width * s.height);
        }
    }


}
