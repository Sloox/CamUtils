package wrightstuff.co.za.cameramanager.utils;

import android.view.Surface;

import java.nio.ByteBuffer;

public class NativeUtils {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public static native void RGBADisplay(int srcWidth, int srcHeight, int Y_rowStride, ByteBuffer Y_Buffer, int UV_rowStride, ByteBuffer U_Buffer, ByteBuffer V_Buffer, Surface surface);
}
