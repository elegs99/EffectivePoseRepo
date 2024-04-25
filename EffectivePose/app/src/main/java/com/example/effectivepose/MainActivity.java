package com.example.effectivepose;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private ProcessCameraProvider cameraProvider;
    private PreviewView previewWindow;
    private PoseDetector poseDetector;
    private PoseOverlayView poseOverlayView;
    private Button toggleOverlay;
    private TextView fpsText;
    private boolean overlayEnabled = true;
    private float avgFPS;
    private int fpsIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewWindow = findViewById(R.id.previewWindow);
        poseOverlayView = findViewById(R.id.poseOverlayWindow);
        fpsText = findViewById(R.id.textView);

        toggleOverlay = findViewById(R.id.overlayToggle);
        if (toggleOverlay != null) { // Check if the toggleOverlay button exists in the layout
            toggleOverlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    overlayEnabled = !overlayEnabled;
                    // Update the button text based on the state of overlayEnabled
                    toggleOverlay.setText(overlayEnabled ? "Disable Overlay" : "Enable Overlay");
                    if (!overlayEnabled) {
                        poseOverlayView.clearCanvas(); // Clear the canvas if the overlay is disabled
                    }
                }
            });
        }

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            poseOverlayView.setTranslation(0f, 200f);
        }else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            poseOverlayView.setTranslation(-175f, 50f);
        }

        /*
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            poseOverlayView.setTranslation(25f, 350f);
        }else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            poseOverlayView.setTranslation(-275f, 75f);
        }*/

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderListenableFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        cameraProvider = cameraProviderListenableFuture.get();

                        StartCamera(cameraProvider);

                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, ContextCompat.getMainExecutor(this));
        }
    }

    private void StartCamera(ProcessCameraProvider cameraProvider) {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        @SuppressLint("RestrictedApi") Preview preview = new Preview.Builder()
                .build();

        preview.setSurfaceProvider(previewWindow.getSurfaceProvider());
        //Log.i("Custom4Me", "starting camera");

        try {
            PoseDetectorOptions options = new PoseDetectorOptions.Builder().build();
            poseDetector = PoseDetection.getClient(options);

            ImageAnalysis imageAnalysis = buildImageAnalysis();

            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    private void AnalyzePose(Pose result) {
        List<PoseLandmark> allPoseLandmarks = result.getAllPoseLandmarks();
        if (overlayEnabled) {
            poseOverlayView.setLandmarks(allPoseLandmarks);
        } else {
            poseOverlayView.invalidate();
            poseOverlayView.clearCanvas();
        }
        /*for (PoseLandmark landmark : allPoseLandmarks) {
            String logMessage = String.format("Landmark ID: %s, Position: %s", landmark.getLandmarkType(), landmark.getPosition().toString());
            Log.i("Custom4Me", logMessage);
        }*/
    }
    private ImageAnalysis buildImageAnalysis() {
        @SuppressLint("RestrictedApi") ImageAnalysis imageAnalysisTemp = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setDefaultResolution(new Size(480,360))
                .build();
        imageAnalysisTemp.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @OptIn(markerClass = ExperimentalGetImage.class)
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                //Log.d("Custom4Me", "Starting image analysis.");
                final long analysisStartTime = System.currentTimeMillis();
                Image mediaImage = imageProxy.getImage();

                if (mediaImage != null) {
                    //Log.d("Custom4Me", String.format("Analyzing image with dimensions: %dx%d.", mediaImage.getWidth(), mediaImage.getHeight()));

                    InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                    poseDetector.process(image)
                            .addOnSuccessListener(new OnSuccessListener<Pose>() {
                                @Override
                                public void onSuccess(Pose pose) {
                                    long duration = System.currentTimeMillis() - analysisStartTime;
                                    fpsIndex++;
                                    avgFPS += (float) 1000 / duration;
                                    fpsText.setText(String.format("fps: %.2f", (avgFPS / fpsIndex)));
                                    Log.d("Custom4Me", "Pose detection completed in " + duration + " ms");
                                    //Log.d("Custom4Me", "Pose detection succeeded.");
                                    AnalyzePose(pose);
                                }
                            })
                            .addOnCompleteListener(new OnCompleteListener<Pose>() {
                                @Override
                                public void onComplete(@NonNull Task<Pose> task) {

                                    //Log.d("Custom4Me", "Pose detection completed in " + duration + " ms");
                                    imageProxy.close();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //Log.e("Custom4Me", "Pose detection failed.", e);
                                }
                            });
                } else {
                    //Log.w("Custom4Me", "No media image available for analysis.");
                    imageProxy.close();
                }
            }
        });
        //Log.i("Custom4Me", "finished analyzing!");
        return imageAnalysisTemp;
    }
}