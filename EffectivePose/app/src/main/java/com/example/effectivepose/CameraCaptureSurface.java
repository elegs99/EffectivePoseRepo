package com.example.effectivepose;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraCaptureSurface {
    private Context context;
    private SurfaceView surfaceView;
    private String cameraId;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private Handler mCameraHandler;
    private boolean isIrCamera = false;
    private Image imageFrame = null;
    private boolean capture = false;
    private SurfaceHolder holder;

    public CameraCaptureSurface(Context context, SurfaceView surfaceView) {
        this.context = context;
        this.surfaceView = surfaceView;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.cameraThread = new HandlerThread("CameraThread");
        this.cameraThread.start();
        this.mCameraHandler = new Handler(cameraThread.getLooper());
    }

    public void open(String cameraId1) {
        cameraId = cameraId1;
        isIrCamera = cameraId.equals("1");

        SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                holder = surfaceHolder;
                if (isIrCamera) {
                    imageReader = ImageReader.newInstance(608, 1120, ImageFormat.YUV_420_888, 2);
                } else {
                    imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 2);
                }
                imageReader.setOnImageAvailableListener(reader -> {
                    Image image = reader.acquireLatestImage();
                    if (image != null && capture) {
                        capture = false;
                    }
                    if (image != null) {
                        image.close();
                    }
                }, mCameraHandler);
                openCamera();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                // Implement as needed
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                // Implement as needed
            }
        };
        surfaceView.getHolder().addCallback(mSurfaceHolderCallback);
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    Surface surface = holder.getSurface();
                    try {
                        mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        mPreviewBuilder.addTarget(surface);
                        cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession session) {
                                        try {
                                            session.setRepeatingRequest(mPreviewBuilder.build(), null, mCameraHandler);
                                        } catch (CameraAccessException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                    }
                                }, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    // Handle errors
                }
            }, mCameraHandler);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle
        }
    }

    public Image getImageFrame() {
        // Returns the captured image frame
        // Need to define the ImageFrame class
        return imageFrame;
    }

    public void startCapture() {
        // Method to start the capture process
        capture = true;
    }

    public void close() {
        // Method to release camera resources
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        if (mCameraHandler != null) {
            mCameraHandler.removeCallbacksAndMessages(null);
            mCameraHandler.getLooper().quitSafely();
        }
        if (cameraThread != null) {
            cameraThread.quitSafely();
        }
    }
}