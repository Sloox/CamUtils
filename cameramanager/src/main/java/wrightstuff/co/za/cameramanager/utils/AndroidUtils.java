package wrightstuff.co.za.cameramanager.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import wrightstuff.co.za.cameramanager.R;

public class AndroidUtils {
    private static final int REQUEST_CAMERA = 0;

    //https://developer.android.com/training/system-ui/dim
    //dim the screen and reduce clutter
    public static void setSystemUiLowProfile(View view) {
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
    }


    /*Permissions
     * Dont need to be concerned to much with the call back as this should be called on the onResume and checked each time*/
    public static void checkRequestCameraPermissions(@NonNull Activity activity, @NonNull View view) {
        if (checkCameraPermissions(activity)) {
            return;
        }
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);

        if (shouldProvideRationale) {
            Snackbar.make(view, R.string.permissions_rationale_camera, Snackbar.LENGTH_INDEFINITE).setAction(R.string.action_grant, v -> {
                requestCameraPermission(activity);
            }).show();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    public static void checkRequestCameraPermissions(@NonNull Activity activity, @NonNull View view, @NonNull CameraCallback cameraCallback) {
        if (checkCameraPermissions(activity)) {
            cameraCallback.onCameraSuccessCallback();
            return;
        }
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);

        if (shouldProvideRationale) {
            Snackbar.make(view, R.string.permissions_rationale_camera, Snackbar.LENGTH_INDEFINITE).setAction(R.string.action_grant, v -> requestCameraPermission(activity)).show();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    public static boolean checkCameraPermissions(@NonNull Activity activity) {
        int permissionState = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private static void requestCameraPermission(@NonNull Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
    }

    public interface CameraCallback {
        void onCameraSuccessCallback();
    }

}
