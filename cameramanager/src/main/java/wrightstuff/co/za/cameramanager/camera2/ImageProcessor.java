package wrightstuff.co.za.cameramanager.camera2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import wrightstuff.co.za.cameramanager.BuildConfig;
import wrightstuff.co.za.cameramanager.utils.WorkLogger;

import static wrightstuff.co.za.cameramanager.camera2.CameraConfig.RotateBitmap;

public class ImageProcessor {

    public static final int MAX_PREVIEW_HEIGHT = 1080;
    public static final int MAX_PREVIEW_WIDTH = 1920;
    private static final String TAG = ImageProcessor.class.getSimpleName();
    private byte[] mRgbBuffer;
    private WeakReference<Activity> activityWeakReference;

    private int mHeight, mWidth;

    private volatile boolean WORKLOCK = false; //added to bypass background thread attempting to continue working

    private ImageReader.OnImageAvailableListener mImageAvailable;
    private TextureView surface;
    private Matrix rotationMatrix;

    public ImageProcessor(Size imageDimen, TextureView surface, Activity activity) {
        this.surface = surface;
        activityWeakReference = new WeakReference<>(activity);

        if (imageDimen != null) {
            mHeight = imageDimen.getHeight();
            mWidth = imageDimen.getWidth();
        }
        WORKLOCK = true;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Size coerceIn(Size input) {
        if (input == null) return new Size(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT);

        int maxxedWidth, maxxedHeight;
        if (input.getWidth() < 0) {
            maxxedWidth = 0;
        } else if (input.getWidth() > MAX_PREVIEW_WIDTH) {
            maxxedWidth = MAX_PREVIEW_WIDTH;
        } else {
            maxxedWidth = input.getWidth();
        }
        if (input.getHeight() < 0) {
            maxxedHeight = 0;
        } else if (input.getHeight() > MAX_PREVIEW_WIDTH) {
            maxxedHeight = MAX_PREVIEW_HEIGHT;
        } else {
            maxxedHeight = input.getHeight();
        }


        return new Size(maxxedWidth, maxxedHeight);
    }

    private void getRGBFromPlanes(Image.Plane[] planes) {
        try {
            ByteBuffer yPlane = planes[0].getBuffer();
            ByteBuffer uPlane = planes[1].getBuffer();
            ByteBuffer vPlane = planes[2].getBuffer();

            int bufferIndex = 0;
            final int total = yPlane.capacity();
            final int uvCapacity = uPlane.capacity();
            final int width = planes[0].getRowStride();


            int yPos = 0;
            for (int i = 0; i < mHeight; i++) {
                int uvPos = (i >> 1) * width;

                for (int j = 0; j < width; j++) {
                    if (uvPos >= uvCapacity - 1)
                        break;
                    if (yPos >= total)
                        break;

                    final int y1 = yPlane.get(yPos++) & 0xff;

            /*
              The ordering of the u (Cb) and v (Cr) bytes inside the planes is a
              bit strange. The _first_ byte of the u-plane and the _second_ byte
              of the v-plane build the u/v pair and belong to the first two pixels
              (y-bytes), thus usual YUV 420 behavior. What the Android devs did
              here (IMHO): just copy the interleaved NV21 U/V data to two planes
              but keep the offset of the interleaving.
             */
                    final int u = (uPlane.get(uvPos) & 0xff) - 128;
                    final int v = (vPlane.get(uvPos + 1) & 0xff) - 128;
                    if ((j & 1) == 1) {
                        uvPos += 2;
                    }

                    // This is the integer variant to convert YCbCr to RGB, NTSC values.
                    // formulae found at
                    // https://software.intel.com/en-us/android/articles/trusted-tools-in-the-new-android-world-optimization-techniques-from-intel-sse-intrinsics-to
                    // and on StackOverflow etc.
                    final int y1192 = 1192 * y1;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);

                    r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                    g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                    b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);
                    mRgbBuffer[bufferIndex++] = (byte) ((((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff)) % 255);
                }
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Buffer state is most likely abandoned");
        }
        mRgbBuffer[0] = 1;
    }


    public ImageReader.OnImageAvailableListener getmImageAvailable() {
        if (mImageAvailable == null) {
            mImageAvailable = reader -> {
                WorkLogger a = new WorkLogger(TAG, "mImageAvailable", !BuildConfig.DEBUG);
                Image image;
                try {
                    image = reader.acquireLatestImage();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "New image available while next hasnt been closed");
                    image = null;
                }

                if (image == null || !WORKLOCK) {
                    if (image != null) {
                        image.close();
                    }
                    return;
                }
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                Bitmap myBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                if (myBitmap != null && surface.isAvailable()) {
                    Canvas canvas = surface.lockCanvas();

                    if (canvas != null) {
                        canvas.drawBitmap(myBitmap, 0,0, null);
                        surface.draw(canvas);
                        surface.unlockCanvasAndPost(canvas);
                    }
                }


                a.addSplit("getRGBFromPlanes()");
                a.dumpToLog();

                image.close();
            };
        }
        return mImageAvailable;
    }



    public void onPause() {
        Log.d(TAG, "onPause called");
        mImageAvailable = null;
        WORKLOCK = false;
    }

    public Matrix getRotationMatrix() {
        if (rotationMatrix==null){
            rotationMatrix = CameraConfig.configurePictureTransform(activityWeakReference.get(), surface.getWidth(), surface.getHeight(), new Size(mWidth, mHeight));
        }
        return rotationMatrix;
    }

    public void onResume() {
        WORKLOCK = true;
        //config change might change the preview
        rotationMatrix = null;
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}
