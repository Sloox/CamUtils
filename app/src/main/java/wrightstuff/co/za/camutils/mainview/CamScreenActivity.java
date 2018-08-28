package wrightstuff.co.za.camutils.mainview;

import android.app.Activity;
import android.os.Bundle;

import butterknife.BindView;
import butterknife.ButterKnife;
import wrightstuff.co.za.cameramanager.camera2.CameraUtilsManager;
import wrightstuff.co.za.cameramanager.camera2.ui.AutoFitTextureView;
import wrightstuff.co.za.cameramanager.utils.AndroidUtils;
import wrightstuff.co.za.camutils.R;

public class CamScreenActivity extends Activity {
    public static final String TAG = CamScreenActivity.class.getSimpleName();

    @BindView(R.id.cameraView)
    AutoFitTextureView cameraView;
    CameraUtilsManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_screen);
        ButterKnife.bind(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidUtils.setSystemUiLowProfile(cameraView); //darkens the screen & hide some of the clutter
        AndroidUtils.checkRequestCameraPermissions(CamScreenActivity.this, cameraView, () -> {
            if (manager != null) {
                manager.onResume();
            } else {
                manager = new CameraUtilsManager(CamScreenActivity.this, cameraView);
                manager.onResume();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (manager != null) {
            manager.onPause();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
