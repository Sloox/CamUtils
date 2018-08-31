package wrightstuff.co.za.cameramanager.camera2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import wrightstuff.co.za.cameramanager.camera2.ui.AutoFitTextureView;

import static wrightstuff.co.za.cameramanager.camera2.ImageProcessor.coerceIn;

/**
 * Camera manager
 * might refactor into smaller parts
 */
public class CameraUtilsManager {
    private static final String TAG = CameraUtilsManager.class.getSimpleName();
    /*Image Processor*/
    ImageProcessor imgProc;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCharacteristics characteristics;
    /*Activity*/
    private WeakReference<Activity> activityWeakReference;
    /*Camera*/
    private String cameraId;
    private Size imageDimension;

    private ImageReader imageReader;
    private boolean mFlashSupported;
    /*BackgroundThread*/
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    /*View to draw to*/
    private AutoFitTextureView textureView;
    /*Camera Callbacks*/
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.d(TAG, "CameraDevice.StateCallback->onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "CameraDevice.StateCallback->onDisconnected");
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "CameraDevice.StateCallback->onError - " + error);
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    public CameraUtilsManager(Activity activity, AutoFitTextureView textView) {
        activityWeakReference = new WeakReference<>(activity);
        textureView = textView;
        textureView.setSurfaceTextureListener(textureListener);
    }

    public void onResume() {
        Log.d(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
        if (imgProc != null) {
            imgProc.onResume();
        }
    }

    @SuppressLint("MissingPermission")
    private boolean openCamera() {
        Log.d(TAG, "Opening Camera");

        CameraManager manager = (CameraManager) activityWeakReference.get().getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return false; // we failed to open camera
        try {
            cameraId = manager.getCameraIdList()[0];
            Log.d(TAG, "Cameras available:" + manager.getCameraIdList().length);
            characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return false; //failed to openCamera
            imageDimension = coerceIn(map.getOutputSizes(SurfaceTexture.class)[0]);
            manager.openCamera(cameraId, stateCallback, null);
            Log.d(TAG, "opencamera() -details:: CameraID:" + cameraId + "; imageDimensions:" + imageDimension.toString());
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to open Camera - " + e);
            return false;
        }
        return true;
    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight()); //set preview size
            Surface surface = new Surface(texture); // to display onto a view
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW); //new request as preview

            // activityWeakReference.get().runOnUiThread(() -> textureView.setAspectRatio(imageDimension.getWidth(), imageDimension.getHeight()));
            //image to process
            //imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getWidth(), ImageFormat.YUV_420_888, 3);
            imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getWidth(), ImageFormat.JPEG, 2);
            imgProc = new ImageProcessor(textureView, activityWeakReference.get(), characteristics);
            imageReader.setOnImageAvailableListener(imgProc.getmImageAvailable(), mBackgroundHandler);

            captureRequestBuilder.addTarget(imageReader.getSurface());
            // captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "captureSession - ConfigurationFailed ");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreview()->Failed to access Camera - " + e);
        }
    }


    //TODO maybe allocate to a renderscript allocation and handle from there
    /*private void createCameraPreviewRenderScript() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight()); //set preview size
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW); //new request as preview
            imgProc = new ImageProcessor(textureView, activityWeakReference.get(), characteristics);
            Allocation alloc = createRenderscriptAlloc(imageDimension.getWidth(), imageDimension.getHeight());
            alloc.setOnBufferAvailableListener(imgProc.getBufferReady());
            captureRequestBuilder.addTarget(alloc.getSurface());
            // captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(alloc.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "captureSession - ConfigurationFailed ");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreview()->Failed to access Camera - " + e);
        }
    }*/

    private Allocation createRenderscriptAlloc(int width, int height) {
        RenderScript rs = RenderScript.create(activityWeakReference.get());
        Type.Builder yuvTypeBuilder = new Type.Builder(rs, Element.YUV(rs));
        yuvTypeBuilder.setX(width);
        yuvTypeBuilder.setY(height);
        yuvTypeBuilder.setYuvFormat(ImageFormat.YUV_420_888);
        return Allocation.createTyped(rs, yuvTypeBuilder.create(), Allocation.USAGE_IO_INPUT);

    }



   /* //TODO REMOVE WHEN READY TO DO SO
    private void oldcreateCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight()); //set preview size
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW); //new request as preview

            //image to process
            imageReader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getWidth(), ImageFormat.JPEG, 1);
            imgProc = new ImageProcessor(imageDimension, textureView);
            imageReader.setOnImageAvailableListener(imgProc.getmImageAvailable(), mBackgroundHandler);

            //captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "captureSession - ConfigurationFailed ");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreview()->Failed to access Camera - " + e);
        }
    }*/

    /*Callbacks*/

    private void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "updatePreview->Failed to access Camera - " + e);
        }
    }

    public void onPause() {
        Log.d(TAG, "onPause");

        if (imgProc != null) {
            imgProc.onPause();
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
        if (null != cameraCaptureSessions) {
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
        }
        stopBackgroundThread();
        closeCamera();
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    /*Threads*/
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background Thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread failed to stopped - " + e);
            }
        }
    }
}
