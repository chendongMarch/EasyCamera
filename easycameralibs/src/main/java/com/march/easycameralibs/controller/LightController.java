package com.march.easycameralibs.controller;

import android.hardware.Camera;
import android.widget.ImageView;

import com.march.easycameralibs.common.CameraConstant;
import com.march.easycameralibs.easycam.CameraNative;

import java.util.List;

/**
 * com.march.easycameralibs.easycam
 * EasyCamera
 * Created by chendong on 16/7/25.
 * Copyright © 2016年 chendong. All rights reserved.
 * Desc :完成关于闪光灯的相关操作
 */
public class LightController {

    private Camera cameraInst;


    public LightController(Camera cameraInst) {
        this.cameraInst = cameraInst;
    }


    /**
     * 检测是否可以切换闪光灯
     *
     * @param flashBtn 按钮
     * @param res 资源
     * @param resCount 检测资源数
     * @param mCurrentCameraId 当前镜头
     * @return 是否可以切换
     */
    private boolean checkCanSwitchLight(ImageView flashBtn, int[] res, int resCount, int mCurrentCameraId) {
        if (mCurrentCameraId == CameraConstant.CAMERA_FACING_FRONT) {
            ConfigController.get().printError("facing front camera not support change flash mode");
            return false;
        }
        if (cameraInst == null || cameraInst.getParameters() == null
                || cameraInst.getParameters().getSupportedFlashModes() == null) {
            ConfigController.get().printError("camera not init over");
            return false;
        }

        if (res != null && res.length != resCount && flashBtn != null) {
            ConfigController.get().printError("This method is not allow auto light,u must provide 3 image resource!");
            return false;
        }
        return true;
    }

    /**
     * 切换闪关灯到关闭状态
     * @param flashBtn 按钮
     * @param res 资源
     * @param mOnFlashChangeListener 监听
     */
    private void switchLight2OffState(ImageView flashBtn, int[] res,
                                      CameraNative.OnFlashChangeListener mOnFlashChangeListener) {
        Camera.Parameters parameters = cameraInst.getParameters();
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


    /**
     * 切换闪关灯到自动状态
     * @param flashBtn 按钮
     * @param res 资源
     * @param mOnFlashChangeListener 监听
     */
    private void switchLight2AutoState(ImageView flashBtn, int[] res,
                                       CameraNative.OnFlashChangeListener mOnFlashChangeListener) {
        Camera.Parameters parameters = cameraInst.getParameters();
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
    }

    /**
     * 切换闪光灯到自动状态
     * @param flashBtn 按钮
     * @param res 资源
     * @param mOnFlashChangeListener 监听
     */
    private void switchLight2OnState(ImageView flashBtn, int[] res
            , CameraNative.OnFlashChangeListener mOnFlashChangeListener) {
        Camera.Parameters parameters = cameraInst.getParameters();
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
    }

    /**
     * 切换闪光灯,包括自动状态,如果当前手机不支持自动闪光会直接切换到关闭
     *
     * @param flashBtn 切换闪光图标的View
     * @param res      on auto off
     */
    public void switchLightWithAuto(ImageView flashBtn, int mCameraId,
                                    CameraNative.OnFlashChangeListener lis, int... res) {
        if (!checkCanSwitchLight(flashBtn, res, 3, mCameraId)) return;
        String mode = cameraInst.getParameters().getFlashMode();
        List<String> focusMode = cameraInst.getParameters().getSupportedFlashModes();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)
                && focusMode.contains(Camera.Parameters.FLASH_MODE_ON)) {//关闭状态切换到开启状态
            switchLight2OnState(flashBtn, res, lis);
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(mode)) {//开启状态切换到自动状态,如果不支持自动状态切换到关闭状态
            if (focusMode.contains(Camera.Parameters.FLASH_MODE_AUTO)) {//有自动状态,切换到自动状态
                switchLight2AutoState(flashBtn, res, lis);
            } else if (focusMode.contains(Camera.Parameters.FLASH_MODE_OFF)) {//没有自动状态,切换到关闭状态
                switchLight2OffState(flashBtn, res, lis);
            }
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(mode)//自动状态切换到关闭状态
                && focusMode.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            switchLight2OffState(flashBtn, res, lis);
        }
    }

    /**
     * 切换闪光,不会切换到自动
     *
     * @param flashBtn 切换图标的View
     * @param res      on off
     */
    public void switchLight(ImageView flashBtn, int cameraId,
                            CameraNative.OnFlashChangeListener lis, int... res) {
        if (!checkCanSwitchLight(flashBtn, res, 2, cameraId)) return;
        String mode = cameraInst.getParameters().getFlashMode();
        List<String> focusMode = cameraInst.getParameters().getSupportedFlashModes();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)
                && focusMode.contains(Camera.Parameters.FLASH_MODE_ON)) {//关闭状态切换到开启状态
            switchLight2OnState(flashBtn, res, lis);
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(mode)
                && focusMode.contains(Camera.Parameters.FLASH_MODE_OFF)) {//开启状态切换到关闭状态
            switchLight2OffState(flashBtn, res, lis);
        }
    }

}
