package wrightstuff.co.za.camutils.mainview;

import android.app.Activity;
import android.os.Bundle;

import butterknife.BindView;
import butterknife.ButterKnife;
import wrightstuff.co.za.cameramanager.camera2.CameraUtilsManager;
import wrightstuff.co.za.cameramanager.camera2.ui.AutoFitTextureView;
import wrightstuff.co.za.camutils.R;
import wrightstuff.co.za.camutils.utils.AndroidUtils;

public class CamScreenActivity extends Activity {
    public static final String TAG = CamScreenActivity.class.getSimpleName();


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @BindView(R.id.cameraView)
    AutoFitTextureView cameraView;
    CameraUtilsManager manager;

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_screen);
        ButterKnife.bind(this);
        setTitle(stringFromJNI());

        manager = new CameraUtilsManager(CamScreenActivity.this, cameraView);

    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidUtils.setSystemUiLowProfile(cameraView); //darkens the screen & hide some of the clutter
        AndroidUtils.checkRequestCameraPermissions(CamScreenActivity.this, cameraView);

        if (manager != null) {
            manager.onResume();
        }

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
