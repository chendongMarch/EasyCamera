package com.march.easycameralibs.helper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.march.easycameralibs.common.CameraConstant;

/**
 * com.march.easycameralibs.helper
 * EasyCamera
 * Created by chendong on 16/7/25.
 * Copyright © 2016年 chendong. All rights reserved.
 * Desc :
 */
public class LogHelper {
    private static LogHelper mInst;
    private Context context;
    private boolean isLog = true;

    private LogHelper(Context context) {
        this.context = context;
    }

    public static LogHelper newInst(Context context) {
        if (mInst == null) {
            synchronized (LogHelper.class) {
                mInst = new LogHelper(context);
            }
        }
        return mInst;
    }

    public static LogHelper get() {
        return mInst;
    }

    public void setLogEnable(boolean log) {
        isLog = log;
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

}
