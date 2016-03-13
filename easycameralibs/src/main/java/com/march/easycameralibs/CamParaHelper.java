package com.march.easycameralibs;

import android.hardware.Camera;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * babyphoto_app     com.babypat.common.camera
 * Created by 陈栋 on 16/3/7.
 * 功能:
 */
public class CamParaHelper {


    private static CameraSizeComparator sizeComparator = new CameraSizeComparator();

    /**
     * size比较
     */
    private static class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // TODO Auto-generated method stub
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    /**
     *  获取当前设备的最大的预览size
     *
     * @param th 比率
     * @return size
     * @param cameraInst camera
     * @return size
     */
    public static Camera.Size getMaxPreviewSize(Camera cameraInst,float th) {
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

    public static Camera.Size getPropPreviewSize(Camera cameraInst,float th, int minWidth) {
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
     * @param th       宽高比,大于1(1,1.33,1.77)
     * @param minWidth 最小宽度

     * @param cameraInst camera
     * @return size
     */
    public static Camera.Size getPropPictureSize(Camera cameraInst,float th, int minWidth) {
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
     * @param s size
     * @param rate 比率
     * @return
     */
    private static boolean equalRate(Camera.Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        if (Math.abs(r - rate) <= 0.03) {
            return true;
        } else {
            return false;
        }
    }



    private static void printSize(String txt, List<Camera.Size> sizes) {
        Log.i("chendong","type is " + txt);
        for (Camera.Size s : sizes) {
            Log.i("chendong","print( " + s.width + " * " + s.height + " rate = " + s.width * 1.0f / s.height + "  piexls = " + s.width * s.height);
        }
    }


}
