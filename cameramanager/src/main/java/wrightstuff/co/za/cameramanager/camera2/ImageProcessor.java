package wrightstuff.co.za.cameramanager.camera2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import wrightstuff.co.za.cameramanager.camera2.ui.AutoFitTextureView;
import wrightstuff.co.za.cameramanager.renderscript.RenderscriptHelper;
import wrightstuff.co.za.cameramanager.utils.WorkLogger;

public class ImageProcessor {

    public static final int MAX_PREVIEW_HEIGHT = 900/*1080*/;
    public static final int MAX_PREVIEW_WIDTH = 1440/*1920*/;
    private static final String TAG = ImageProcessor.class.getSimpleName();
    private byte[] mRgbBuffer;
    private WeakReference<Activity> activityWeakReference;
    private CameraCharacteristics cameraCharacteristics;

    private volatile boolean WORKLOCK; //added to bypass background thread attempting to continue working

    private ImageReader.OnImageAvailableListener mImageAvailable;
    private AutoFitTextureView mTextureView;
    private Bitmap screenBitmap;
    private Boolean rotatePreview;

    private RenderscriptHelper rsHelper;
    private int choice;
    private float saturation = 2, blur = 5;

    public ImageProcessor(AutoFitTextureView mTextureView, Activity activity, CameraCharacteristics cameraCharacteristics) {
        this.mTextureView = mTextureView;
        activityWeakReference = new WeakReference<>(activity);
        this.cameraCharacteristics = cameraCharacteristics;
        WORKLOCK = true;
        rsHelper = new RenderscriptHelper(activity);
        mTextureView.setOnClickListener(v -> {
            saturation = saturation++ + 10;
            saturation = (saturation % 250) + 1;
            blur = blur++;
            blur = (blur % 25) + 1;
        });
        mTextureView.setOnLongClickListener(v -> {
            choice = (++choice) % 5;
            return false;
        });
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

    public ImageReader.OnImageAvailableListener getmImageAvailable() {
        if (mImageAvailable == null) {
            mImageAvailable = reader -> {
                WorkLogger a = new WorkLogger(TAG, "mImageAvailable", true);
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
                screenBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                /*Enter screen manipulation here*/
                screenBitmap = manipulateBitmap(screenBitmap);

                if (screenBitmap != null && mTextureView.isAvailable()) {
                    Canvas canvas = mTextureView.lockCanvas();
                    if (canvas != null) {
                        handleCanvasRotation(canvas, screenBitmap);
                        mTextureView.unlockCanvasAndPost(canvas);
                    }
                }
                a.addSplit("->drawingend()");
                image.close();
                a.addSplit("-> mImageAvailable- end");
                a.dumpToLog();
            };
        }
        return mImageAvailable;
    }

    private Bitmap manipulateBitmap(Bitmap bitmap) {
        switch (choice) {
            case 1:
                return rsHelper.blurBitmap(bitmap, blur);
            case 2:
                return rsHelper.histogramEqualization(bitmap);
            case 3:
                return rsHelper.saturation(bitmap, saturation);
            case 4:
                return rsHelper.mono(bitmap);
            default:
                return bitmap;
        }
    }

    private void handleCanvasRotation(Canvas canvas, Bitmap bitmap) {
        //we have Image, Canvas and Tetureview
        if (rotatePreview == null) {
            rotatePreview = isMustRotate();
        }
        if (rotatePreview) {
            canvas.save();
            canvas.rotate(90, canvas.getWidth() / 2, canvas.getHeight() / 2);
            canvas.drawBitmap(bitmap, null, canvas.getClipBounds(), null);
            canvas.restore();
        } else {
            canvas.drawBitmap(bitmap, 0, 0, null);//orientation is correct
        }
    }

    private boolean isMustRotate() {
        int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        boolean mustRotate = false;
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                    mustRotate = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                    mustRotate = true;
                }
                break;
            default:
                Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }
        return mustRotate;
    }

    private void getRGBFromPlanes(Image.Plane[] planes, int mHeight) {
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


    public void onPause() {
        Log.d(TAG, "onPause called");
        mImageAvailable = null;
        WORKLOCK = false;
        rotatePreview = null;
    }


    public void onResume() {
        WORKLOCK = true;
    }

    public Activity getActivity() {
        return activityWeakReference.get();
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
