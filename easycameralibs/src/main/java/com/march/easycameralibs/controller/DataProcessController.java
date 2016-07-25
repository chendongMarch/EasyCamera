package com.march.easycameralibs.controller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import com.march.easycameralibs.common.CameraConstant;
import com.march.easycameralibs.common.CameraInfo;
import com.march.easycameralibs.easycam.CameraNative;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * com.march.easycameralibs.easycam
 * EasyCamera
 * Created by chendong on 16/7/25.
 * Copyright © 2016年 chendong. All rights reserved.
 * Desc : 主要复杂数据处理
 */
public class DataProcessController {
    private ExecutorService mSaveThread;
    private File saveDir;

    public DataProcessController(File saveDir) {
        this.saveDir = saveDir;
        mSaveThread = Executors.newFixedThreadPool(2);
    }

    /**
     * 根据文件名获取
     *
     * @param fileName 文件名
     * @return 获取要保存的文件
     */
    private File getSaveFile(String fileName) {
        return new File(saveDir, fileName);
    }


    public void savePic(final Bitmap bitmap, final String filename, final CameraInfo info, final Runnable afterSave) {
        mSaveThread.execute(new Runnable() {
            @Override
            public void run() {
                if (bitmap != null)
                    if (!bitmap.isRecycled()) {
                        //保存到sd卡,可替换
                        save2Sd(getSaveFile(filename), bitmap, 100);
                    } else {
                        Log.e(CameraConstant.TAG, "bitmap is recycled");
                    }

                if (bitmap != null && !bitmap.isRecycled())
                    bitmap.recycle();
                if (afterSave != null)
                    afterSave.run();
            }
        });
    }

    /**
     * 快速拍摄数据处理，将预览数据转为byte[]转化为可处理的byte[]
     *
     * @param data       数据
     * @param cameraInst camera
     * @return 处理之后的位图
     */
    public byte[] convertPreviewData(Camera cameraInst, byte[] data) {
        if (data == null) {
            return null;
        }
        Camera.Size size = cameraInst.getParameters().getPreviewSize(); //获取预览大小
        final int w = size.width;  //宽度
        final int h = size.height;
        final YuvImage image = new YuvImage(data, ImageFormat.NV21, w, h, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
            Log.e(CameraConstant.TAG, "image compressToJpeg fail");
            return null;
        }
        return os.toByteArray();
    }


    /**
     * 获取处理后的位图，根据信息将byte[]转化为bitmap, 包括旋转，反转，裁剪
     *
     * @param data 数据
     * @return 位图
     */
    public Bitmap findBitmap(CameraInfo info, byte[] data) {
        if (data == null) {
            return null;
        }
        Bitmap tempBitmap;
        /**
         * 根据镜头以及手机旋转角度调整图片
         */
        if (info.cameraId == CameraConstant.CAMERA_FACING_BACK) {
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
            case CameraConstant.One2One:
                if (info.angle != 0)
                    tempBitmap = Bitmap.createBitmap(tempBitmap, (tempBitmap.getWidth() - tempBitmap.getHeight()) / 2, 0, tempBitmap.getHeight(), tempBitmap.getHeight());
                else
                    tempBitmap = Bitmap.createBitmap(tempBitmap, 0, (tempBitmap.getHeight() - tempBitmap.getWidth()) / 2, tempBitmap.getWidth(), tempBitmap.getWidth());
                break;
            case CameraConstant.Four2Three:
                break;
        }
        return tempBitmap;
    }


    /**
     * 根据角度，水平垂直反转的信息转化byte[]
     *
     * @param data              数据
     * @param degree            角度
     * @param isHorizontalScale 水平翻转
     * @param size              大小
     * @param isVerticalScale   垂直翻转
     * @return 位图
     */
    private Bitmap convertCameraImg(byte[] data, float degree, boolean isHorizontalScale, int size, boolean isVerticalScale) {
        if (data == null) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = size;
        Bitmap bit = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        Log.e(CameraConstant.TAG, "degree is " + degree);
        if (!isHorizontalScale && !isVerticalScale && (Math.abs(degree - 360) < 5 || Math.abs(degree) < 5)) {
            Log.e(CameraConstant.TAG, "此时图片不需要旋转或者镜像翻转处理," +
                    "\n当前sdk版本 = " + Build.VERSION.SDK_INT + "  " +
                    "\n当前手机旋转角度 angle = " + degree + "  " +
                    "\n当前镜头朝向 = " + (CameraNative.getInst().getCurrentCameraId() == 0 ? "后置" : "前置"));
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
     *
     * @param path    文件路径
     * @param bit     位图
     * @param quality 质量
     */
    private void save2Sd(File path, Bitmap bit, int quality) {

        try {
            bit.compress(Bitmap.CompressFormat.JPEG, quality, new FileOutputStream(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
