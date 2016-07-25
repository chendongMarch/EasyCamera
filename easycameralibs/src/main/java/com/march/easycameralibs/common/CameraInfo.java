package com.march.easycameralibs.common;

/**
 * babyphoto_app     com.babypat.common.camera
 * Created by 陈栋 on 16/3/7.
 * 功能: 存储照片的一些即时性信息，
 */
public class CameraInfo {

    public int picSize;
    public  int cameraId;
    public  float angle;
    public int sampleSize;

    public CameraInfo(float angle, int cameraId, int picSize, int sampleSize) {
        this.angle = angle;
        this.cameraId = cameraId;
        this.picSize = picSize;
        this.sampleSize = sampleSize;
    }
}
