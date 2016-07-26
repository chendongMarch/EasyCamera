package com.march.easycameralibs.common;

import android.content.SharedPreferences;

/**
 * com.march.easycameralibs.easycam
 * EasyCamera
 * Created by chendong on 16/7/25.
 * Copyright © 2016年 chendong. All rights reserved.
 * Desc :
 */
public class CameraConstant {
    //镜头方向
    public static final int CAMERA_FACING_BACK = 0, CAMERA_FACING_FRONT = 1;
    //拍摄模式
    public static final int Mode_FAST = 0, Mode_PIC = 1;
    //照片大小
    public static final int One2One = 0, Four2Three = 1;
    //自动切换
    public static final int AutoSwitch = -1;
    //打印tag
    public static final String TAG = " CameraNative ";
    //获取拍摄mode的key
    public static final String KEY_SP_MODE = "KEY_SP_MODE";
    //获取SharedPreferences
    public static final String KEY_SP_STORE = "CAMERA_NATIVE";




    public static final int ERROR_OPEN_CAMERA_FAILED = 0;

}
