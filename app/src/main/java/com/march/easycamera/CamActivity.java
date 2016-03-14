package com.march.easycamera;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.march.easycameralibs.easycam.CameraNative;
import com.march.easycameralibs.widgets.CamContainerView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CamActivity extends AppCompatActivity {


    @Bind(R.id.activity_mycamera_container)
    CamContainerView camContainerView;
    CameraNative cameraNative;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);
        ButterKnife.bind(this);
        cameraNative = CameraNative.getInst();
    }

    @OnClick({
            R.id.activity_mycamera_take,
            R.id.activity_mycamera_switch,
            R.id.activity_mycamera_flash,
            R.id.activity_mycamera_over,
            R.id.activity_mycamera_mode,
            R.id.activity_mycamera_size})
    public void clickBtn(View view) {
        switch (view.getId()) {
            case R.id.activity_mycamera_take:
                cameraNative.doTakePic(System.currentTimeMillis() + ".jpg", null);
                break;
            case R.id.activity_mycamera_switch:
                cameraNative.switchCamera();
                break;
            case R.id.activity_mycamera_flash:
                cameraNative.toogleLightWithAuto(null);
                break;
            case R.id.activity_mycamera_over:
                cameraNative.takePicOver();
                break;
            case R.id.activity_mycamera_mode:
                cameraNative.switchTakeMode(CameraNative.NotConvert);
                break;
            case R.id.activity_mycamera_size:
                cameraNative.switchPicSize(CameraNative.NotConvert);
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        CameraNative.newInst(CamActivity.this, camContainerView);
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
}
