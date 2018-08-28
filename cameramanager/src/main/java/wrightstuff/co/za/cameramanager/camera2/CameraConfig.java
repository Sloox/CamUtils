package wrightstuff.co.za.cameramanager.camera2;

import android.util.SparseIntArray;
import android.view.Surface;

/**
 * Config Class holding standard enums, orientations and max sizes of the camera and its associated pictures
 */
public class CameraConfig {

    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }




}
