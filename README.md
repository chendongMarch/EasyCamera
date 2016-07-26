#EasyCamera

##希望能很好的封装关于Camera的一系列操作，简化关于Camera的开发，类库正在不断完善当中
##图片的拍摄和存储异步进行，支持照片拍摄和快速连拍

###[库代码链接](https://github.com/chendongMarch/EasyCamera)
##compile 'com.march.cameralibs:easycameralibs:1.0.7'

###在xml文件中使用
```java
//参数说明
<declare-styleable name="CamContainerView">
        //是否在顶部显示遮挡,如果为true,切换到1:1时,会在顶部出现黑色遮挡物,如果为false,预览界面始终在屏幕顶端
        <attr name="isShowTop" format="boolean"/>
</declare-styleable>

//xml实例
<com.march.libs.mycamera.CamContainerView
        android:id="@+id/activity_mycamera_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        cam:isShowTop="true"/>
```


###创建Activity同步生命周期
```java
	@Override
    protected void onResume() {
        super.onResume();
        CameraNative.getInst().onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CameraNative.getInst().onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraNative.getInst().onDestroy();
    }
```

###获取CameraNative实例并设置监听
```java
//get the singleton of CameraNative
private CameraNative mCameraNative;
mCameraNative = CameraNative.getInst();

//设置保存图片的路径
mCameraNative.setSaveDir(new File(ImageUtils.getSystemDcimPath()));

//设置闪光灯监听，有三个回调可以自由实现,使用该回调方法可以自定义在开启闪光灯时的额外操作，比如弹窗提醒
mCameraNative.setOnFlashChangeListener(new CameraNative.OnFlashChangeListener() {
            @Override
            public boolean OnTurnFlashOn() {
            //your code
                   return true;
            }
});

//设置照片拍摄的监听的监听，将会实现三个方法用来检测图片的存储进度
mCameraNative.setOnSavePicListener(this);
@Override
public void InSaveProgress(int num, float percent) {
        L.info("正在存储 " + num + "  rate  " + percent);
        //由于拍摄和存储是异步进行的，在这里可以回调保存的进度
}
@Override
public void OnSaveOver() {
        L.info("保存完成");
        //保存完成，回调发生
}

//错误回调监听
mCameraNative.setOnErrorListener(new CameraNative.OnErrorListener() {
            @Override
            public void error(int errorCode,String errMsg) {
            //重大错误会使用错误回调交给开发者处理，比如相机没有获得权限无法打开，详细参照CameraConstant错误码

            }
        });
```

###闪光灯API
```java
//切换闪光灯

//如果相机支持自动状态将会切换到自动状态，否则直接切换到关闭状态，是可以自适应的切换模式
//切换闪关灯状态，不涉及UI的变化
public void switchLightWithAuto()
//flashBtn ImageView 切换按钮 可以为空，为空时不切换
//res int[] 资源数组，长度必须是3，可以为空，为空时不切换
public void switchLightWithAuto(ImageView flashBtn, int... res)
eg:
mCameraNative.switchLightWithAuto(flashBtn,
                R.mipmap.camera_flashOn,
                 R.mipmap.camera_flashAuto,
                  R.mipmap.camera_flashOff);

//开启和关闭状态切换
//切换闪关灯状态，不涉及UI的变化
public void switchLight()
//flashBtn ImageView 切换按钮 可以为空，为空时不切换
//res int[] 资源数组，长度必须是2，可以为空，为空时不切换
public void switchLight(ImageView flashBtn, int... res)
eg:
mCameraNative.switchLight(flashBtn,
					R.mipmap.camera_flashOn,
						R.mipmap.camera_flashOff);
```

###照片大小API
```java
//size参数:(CameraConstant.AutoSwitch,CameraConstant.Four2Three,CameraConstant.One2One)
//涉及UI的变化
public void switchPicSize(int size, ImageView iv, int... res)
//无关UI变化
public void switchPicSize(int size)
//获取当前照片大小设置
public int getCurrentSize()
```

###照片模式API
```java
//mode参数：(CameraConstant.AutoSwitch,CameraConstant.MODE_PIC,CameraConstant.MODE_GIF)
//切换照片模式，两种模式
//MODE_PIC 照片模式，像素在300w以上
//MODE_GIF 连拍模式，相片像素600*800

//切换拍照模式,同时重新初始化相机
public void switchTakeMode(int mode)
public int getTakeMode()
```

###切换镜头API
```java
//cameraId参数：(CameraConstant.AutoSwitch,CameraConstant.CAMERA_FACING_BACK,CameraConstant.CAMERA_FACING_FRONT)
//切换camera镜头方向
public boolean switchCameraFacing(int cameraId)
```

###拍照API
```java
//拍照回调的接口，它不但是数据回调的接口也是对数据处理进行的参数配置，实现部分方法
public static abstract class OnTakePicListener {
         // 在操作过程中会返回data,由开发者自己做更多处理
        //当你想将它转化为bitmap时调用mCameraNative.handlePicData()方法进行转换
        public void onTakePic(byte[] data, CameraInfo info) {

        }

        //是否保存到本地，默认保存
        public boolean isSave2Local() {
             return true;
        }

        //采样的标准，根据二进制数据大小决定采样率默认是1
        public int getInSampleSize(byte[] data) {
            return 1;
        }
    }
//普通拍照
public boolean doTakePic(String fileName, OnTakePicListener listener)

//快速连拍
//开始连拍时调用doStartTakeFastPic()
public void doStartTakeFastPic()
//拍摄一张，使用listener获取拍摄的数据
public void doTakeFastPic(String fileName, OnTakePicListener listener)
//拍摄完毕停止连拍
public void doStopFastPic()
```

###图片处理API
```java
//在外部处理byte数组,返回bitmap,比如回调public void onTakePic(byte[] data, CameraInfo info) 的数据
public Bitmap handlePicData(byte[] data, int sampleSize, CameraInfo info)
```

###其他
```java
//由于存储照片是异步的，拍摄完毕之后需要调用doTakePicOver()方法，并在OnSaveOver()回调中执行相关操作
doTakePicOver()
//停止照片自定旋转,横屏拍摄的照片将不会自动横向显示
public void shutDownAutoRotate()
//记住上次的模式。不需要每次都设置
public void setAllowRememberLastTimeMode(boolean allowRememberLastTimeMode)
//关闭log
public void setLogEnable(boolean log)
```

###常用操作
```java
public void clickBtn(View view) {
        switch (view.getId()) {
            case R.id.activity_mycamera_take:
                cameraNative.doTakePic(System.currentTimeMillis() + ".jpg", new CameraNative.OnTakePicListener() {
                    @Override
                    public void onTakePic(byte[] data, CameraInfo info) {
                        Bitmap bitmap = CameraNative.getInst().handlePicData(data, 1, info);
                    }

                    @Override
                    public boolean isSave2Local() {
                        return true;
                    }

                    @Override
                    public int getInSampleSize(byte[] data) {
                        return 2;
                    }
                });
                break;
            case R.id.activity_mycamera_switch:
                cameraNative.switchCameraFacing(CameraConstant.AutoSwitch);
                break;
            case R.id.activity_mycamera_flash:
                cameraNative.switchLightWithAuto();
                break;
            case R.id.activity_mycamera_over:
                cameraNative.takePicOver();
                break;
            case R.id.activity_mycamera_mode:
                cameraNative.switchTakeMode(CameraConstant.AutoSwitch);
                break;
            case R.id.activity_mycamera_size:
                cameraNative.switchPicSize(CameraConstant.AutoSwitch);
                break;
        }
```