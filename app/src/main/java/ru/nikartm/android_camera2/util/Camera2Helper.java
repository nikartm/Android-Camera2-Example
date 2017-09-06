package ru.nikartm.android_camera2.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivan Vodyasov on 03.09.2017.
 */

public class Camera2Helper extends SurfaceView implements SurfaceHolder.Callback  {

    private static final String TAG = Camera2Helper.class.getSimpleName();

    private Activity activity;
    private String mCameraID;

    private CameraDevice mCameraDevice = null;
    private CameraManager mCameraManager = null;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private TextureView textureView;
    private ImageReader jpegImageReader;
    private File mFile;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    public Camera2Helper(Activity activity, @NonNull TextureView textureView) {
        super(activity);
        this.activity = activity;
        this.textureView = textureView;
        this.mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        this.mFile = new File(activity.getExternalFilesDir(null), "husky.jpg");
    }

    public boolean isOpen() {
        return mCameraDevice != null;
    }

    public void openCamera() {
        startBackgroundThread();
        mCameraID = "0"; // 0 - back camera, 1 - front
        try {
            if(Utils.checkCameraPermission(activity)) {
                mCameraManager.openCamera(mCameraID, mCameraCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (jpegImageReader != null) {
            jpegImageReader.close();
            jpegImageReader = null;
        }
        stopBackgroundThread();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            createCameraPreviewSession();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            if (isOpen()) {
                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
            }
        }
    };

    private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
            jpegImageReader = ImageReader.newInstance(Utils.getDisplaySize(activity).x, Utils.getDisplaySize(activity).y, ImageFormat.JPEG, 1);
            jpegImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
            Log.i(TAG, "Open camera with id: " + mCameraDevice.getId());
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if(mCameraDevice != null) {
                Log.i(TAG, "Disconnect camera with id: " + mCameraDevice.getId());
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i(TAG, "Error! Camera id: " + camera.getId() + " error: " + error);
        }
    };

    private void createCameraPreviewSession() {
        List<Surface> surfaces = new ArrayList<>();
        Point display = Utils.getDisplaySize(activity);
        int width = display.x;
        int height = display.y;

        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(width, height);
        Surface previewSurface = new Surface(texture);
        surfaces.add(previewSurface);

        Surface surfaceJpeg = jpegImageReader.getSurface();
        surfaces.add(surfaceJpeg);

        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            // The camera is already closed
                            if (mCameraDevice == null) {
                                return;
                            }
                            Log.i(TAG, "Config success");
                            mCaptureSession = session;
                            try {
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Config camera session failed", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.i(TAG, "Config failed");
                        }
                    },
                    null
            );
        } catch (CameraAccessException e) {
            Log.e(TAG, "Create capture session failed", e);
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void captureStillPicture() {
        try {
            if (activity == null || mCameraDevice == null) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            captureRequestBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(jpegImageReader.getSurface());
//            captureRequestBuilder.addTarget(obj3D);

            // Use the same AE and AF modes as the preview.
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // Orientation
//            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            // Сейчас по дефолту фото делается в портретном виде
            // При необходимости можно сделать автоматическую ротацию
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.d(TAG, "Create photo : " + mFile.toString());
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(), CaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
