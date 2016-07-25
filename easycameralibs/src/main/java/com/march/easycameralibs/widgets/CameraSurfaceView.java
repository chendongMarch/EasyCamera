package com.march.easycameralibs.widgets;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.RelativeLayout;

import com.march.easycameralibs.common.CameraConstant;
import com.march.easycameralibs.easycam.CameraNative;


/**
 * babyphoto_app     com.babypat.widget
 * Created by 陈栋 on 16/3/5.
 * 功能:自定义预览SurfaceView
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, View.OnClickListener, View.OnTouchListener {

    private final String tag = " CameraSurfaceView ";
    private OnCameraSurfaceListener onCameraSurfaceListener;
    private boolean isPointFocus = false;
    private Handler handler;
    private float pointX = -1, pointY = -1;
    private View focusView;


    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSurfaceView();
    }

    public void setOnCameraSurfaceListener(OnCameraSurfaceListener onCameraSurfaceListener) {
        this.onCameraSurfaceListener = onCameraSurfaceListener;
    }

    /**
     * 设置定点对焦
     *
     * @param pointFocus 是否允许定点对焦
     * @param focusView  定点对焦时执行动画的View,为null时不会执行动画
     */
    public void setPointFocus(boolean pointFocus, View focusView) {
        this.isPointFocus = pointFocus;
        this.focusView = focusView;
    }

    private void initSurfaceView() {
        getHolder().addCallback(this);
        handler = new Handler();
        setOnClickListener(this);
        setOnTouchListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(tag, "---surfaceCreated---");
        //打开相机
        if (!CameraNative.getInst().isCameraInit()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    CameraNative.getInst().openCamera(CameraConstant.CAMERA_FACING_BACK);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    if (CameraNative.getInst().isCameraInit()) {
                        CameraNative.getInst().initSurfaceHolder();
                        CameraNative.getInst().initCamera();
                    }
                    if (onCameraSurfaceListener != null) {
                        onCameraSurfaceListener.afterCreate();
                    }
                }
            }.execute();
        } else {
            CameraNative.getInst().initSurfaceHolder();
            CameraNative.getInst().initCamera();
            if (onCameraSurfaceListener != null) {
                onCameraSurfaceListener.afterCreate();
            }

        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(tag, "---surfaceChanged---");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(tag, "---surfaceDestroyed---");
//        CameraNative.getInst().releaseCamera();
    }


    @Override
    public void onClick(View v) {
        //不允许定点对焦直接返回
        if (!isPointFocus || pointX == -1 || pointY == -1)
            return;
        CameraNative.getInst().pointFocus(pointX, pointY);
        if (focusView != null) {
            RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(focusView.getLayoutParams());
            layout.setMargins((int) pointX - 200, (int) pointY - 200, 0, 0);
            focusView.setLayoutParams(layout);
            focusView.setVisibility(View.VISIBLE);
            ScaleAnimation sa = new ScaleAnimation(1f, 0.5f, 1f, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            sa.setDuration(800);
            focusView.startAnimation(sa);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    focusView.setVisibility(View.GONE);
                }
            }, 800);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isPointFocus) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                pointX = event.getX();
                pointY = event.getY();
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(event.getX() - pointX) > 60 || Math.abs(event.getY() - pointY) > 60) {
                    pointY = -1;
                    pointX = -1;
                }
            }
        }
        return false;
    }


    /**
     * 回调接口
     */
    public interface OnCameraSurfaceListener {
        void afterCreate();
    }


}
