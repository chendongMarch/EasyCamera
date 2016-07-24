#easycamera

#Camera Libs

###[库代码链接](https://github.com/chendongMarch/EasyCamera)
##compile 'com.march.cameralibs:easycameralibs:1.0.7'
##更新日志
###1.0.7 三星手机推出后台返回崩溃
###1.0.8 小米红米note2机型适配

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
        CameraNative.getInst().onDestory();
    }
```
###获取CameraNative实例并设置监听
```java
//get the singleton of CameraNative
private CameraNative mCameraNative;
mCameraNative = CameraNative.getInst();

//设置保存图片的路径
mCameraNative.setSaveDir(new File(ImageUtils.getSystemDcimPath()));
//设置闪光灯监听，有三个回调可以自由实现
mCameraNative.setOnFlashChangeListener(new CameraNative.OnFlashChangeListener() {
            @Override
            public boolean OnTurnFlashOn() {
            //your code
                   return true;
            }
});

//设置照片拍摄的监听的监听，将会实现三个方法
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

@Override
public void CanotTake() {
        ToastUtil.show("稍候操作");
        //camera拍摄过快时将会产生异常，会回调改方法
}

//错误回调监听
mCameraNative.setOnErrorListener(new CameraNative.OnErrorListener() {
            @Override
            public void error(String errMsg) {
            //错误信息，默认错误信息仍旧是会打印的

            }
        });
```

###闪光灯API
```java
//切换闪光灯
//如果相机支持将会切换到自动状态，否则直接切换到关闭状态，是可以自适应的切换模式，需要设置按钮和相关资源
//flashBtn ImageView 切换按钮 可以为空，为空时不切换
//res int[] 资源数组，长度必须是3，可以为空，为空时不切换
public void toogleLightWithAuto(ImageView flashBtn, int... res)
mCameraNative.toogleLightWithAuto(flashBtn,
                R.mipmap.camera_flashon,
                 R.mipmap.camera_flashauto,
                  R.mipmap.camera_flashoff);

//开启和关闭状态切换
//flashBtn ImageView 切换按钮 可以为空，为空时不切换
//res int[] 资源数组，长度必须是2，可以为空，为空时不切换
public void toogleLight(ImageView flashBtn, int... res)
mCameraNative.toogleLight(flashBtn,
					R.mipmap.camera_flashon,
						R.mipmap.camera_flashoff);
```

###照片大小API
```java
//自动切换图片资源同时切换图片大小
//size参数 为CameraNative.NotConvert时将会自己切换否则切换到制定状态(CameraNative.NotConvert,CameraNative.One2One,CameraNative.Four2Three)
public void switchPicSize(int size, ImageView iv, int... res)
//无关资源切换参数同上
调用CamContainerView的相关方法切换照片大小和界面显示
public void switchPicSize(int size)
public boolean isSizeOne2One()
public boolean isFour2Three()
```

###照片模式API
```java
//切换照片模式，两种模式
//MODE_PIC 照片模式，像素在300w以上
//MODE_GIF 连拍模式，相片像素600*800

//切换拍照模式,同时重新初始化相机
public void switchTakeMode(int mode)
public boolean isTakePic()
public boolean isTakeGif()
```

###拍照API
```java
//拍摄照片
//fileName 指定文件名存储,不能为空
//接口
public static abstract class OnTakePicListener {
//拍摄获得的二进制数组
        public void onTakePic(byte[] data) {

        }
//处理之后的bitmap
        public void onTakePic(Bitmap bit) {

        }
//是否处理成bitmap,默认不处理
        public boolean isConvert2Bitmap() {
            return false;
        }
//压缩的sampleSize,默认不压缩
        public int getInSampleSize(byte[] data) {
            return 1;
        }
}
public boolean doTakePic(String fileName, boolean isConvert2Bit, OnTakePicListener listener)

//快速连拍
//开始连拍时调用doStartTakeFastPic()
public void doStartTakeFastPic()
//拍摄一张，fileName == null时不存储，使用listener获取拍摄的数据
public void doTakeFastPic(String fileName, OnTakePicListener listener)
//拍摄完毕停止连拍
public void doStopFastPic()
```

###图片处理API
```java
//在外部处理byte数组,返回bitmap
public Bitmap handlePicData(boolean isFast, byte[] data, int sampleSize)
```

###其他
```java
//由于存储照片是异步的，拍摄完毕之后需要调用doTakePicOver()方法，并在OnSaveOver()回调中执行相关操作
doTakePicOver()
//停止照片自定旋转,横屏拍摄的照片将不会自动横向显示
public void shutDownAutoRotate()
```

###常用操作
```java
public void clickBtn(View view) {
        switch (view.getId()) {
            case R.id.activity_mycamera_take:
            //拍照
                cameraNative.doTakePic(str(System.currentTimeMillis()) + ".jpg", null);
                break;
            case R.id.activity_mycamera_switch:
            //切换摄像头
                cameraNative.switchCamera();
                break;
            case R.id.activity_mycamera_flash:
            //切换闪光灯
                cameraNative.toogleLightWithAuto(null);
                break;
            case R.id.activity_mycamera_over:
            //拍照结束
                cameraNative.takePicOver();
                break;
            case R.id.activity_mycamera_mode:
            //切换拍摄模式
                cameraNative.switchTakeMode(CameraNative.NotConvert);
                break;
            case  R.id.activity_mycamera_size:
            //切换照片大小
                cameraNative.switchPicSize(CameraNative.NotConvert);
                break;
        }
    }
```