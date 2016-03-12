package com.march.easycameralibs;

/**
 * babyphoto_app     com.babypat.common.camera
 * Created by 陈栋 on 16/3/7.
 * 功能:
 */
public class CamInfo {

    public int picSize;
    public  int cameraId;
    public  float angle;
    public int sampleSize;

    public CamInfo(float angle, int cameraId, int picSize, int sampleSize) {
        this.angle = angle;
        this.cameraId = cameraId;
        this.picSize = picSize;
        this.sampleSize = sampleSize;
    }
}
