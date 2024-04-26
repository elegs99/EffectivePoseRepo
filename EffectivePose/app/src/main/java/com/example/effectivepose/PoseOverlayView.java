package com.example.effectivepose;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.List;

public class PoseOverlayView extends View {
    private List<PoseLandmark> landmarks;
    private final Paint paint1 = new Paint();
    private final Paint paint2 = new Paint();
    //Samsung A03s
    private float scaleX = 2.25f;
    private float scaleY = 2.25f;
    private float offsetX = -175f;
    private float offsetY = 50f;
    /*
    //Google Pixel 8 Pro
    private float scaleX = 3.25f;
    private float scaleY = 3.25f;
    private float offsetX = -250f;
    private float offsetY = 0;*/
    private boolean isFlipped = false;
    private boolean canvasCleared = false;
    private final int[][] connectedJoints = {{11, 13}, {13, 15}, {15, 17}, {15, 19}, {17, 19}, // Right Arm
                                                {12, 14}, {14, 16}, {16, 18}, {16, 20}, {18, 20}, // Left Arm
                                                {11, 12}, {12, 24}, {11, 23}, {23, 24}, // Stomach
                                                {24, 26}, {26, 28}, {23, 25}, {25, 27}, // Legs
                                                {28, 30}, {30, 32}, {27, 29}, {29, 31}}; // Feet

    // Constructor for programmatically creating the view
    public PoseOverlayView(Context context) {
        this(context, null);
    }

    // Constructor for inflating the view from XML
    public PoseOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        landmarks = new ArrayList<>();
        paint1.setColor(Color.RED);
        paint1.setStyle(Paint.Style.FILL);

        paint2.setColor(Color.GREEN);
        paint2.setStyle(Paint.Style.FILL);
        paint2.setStrokeWidth(3f);
    }

    public void setLandmarks(List<PoseLandmark> landmarks) {
        this.landmarks = landmarks;
        invalidate(); // Redraw the view
    }

    public void clearCanvas() {
        canvasCleared = true;
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        invalidate(); // Redraw the view
    }

    public void setTranslation(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        invalidate(); // Redraw the view
    }

    public void setFlipped(boolean isFlipped) {
        this.isFlipped = isFlipped;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final long analysisStartTime = System.currentTimeMillis();
        super.onDraw(canvas);
        if (landmarks == null || landmarks.isEmpty()) {
            return;
        }
        if (canvasCleared) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvasCleared = false;
            return;
        }
        drawLinesBetweenJoints(canvas);
        for (PoseLandmark landmark : landmarks) {
            if (landmark.getInFrameLikelihood() > .95f && landmark.getLandmarkType() != 18 && landmark.getLandmarkType() != 22 && landmark.getLandmarkType() != 17 && landmark.getLandmarkType() != 21) {
                float x = translateX(landmark.getPosition().x);
                float y = translateY(landmark.getPosition().y);
                canvas.drawCircle(x, y, 4, paint1);
            }
        }
        long duration = System.currentTimeMillis() - analysisStartTime;
        Log.d("Custom4Me", "Draw call completed in " + duration + " ms");
    }
    private void drawLinesBetweenJoints(Canvas canvas) {
        // Ensure landmarks list is not null and has enough elements
        if (landmarks == null || landmarks.size() < 33) {
            return;
        }

        for (int[] jointPair : connectedJoints) {
            // Extract indexes from the pair
            int index1 = jointPair[0];
            int index2 = jointPair[1];

            // Get the PoseLandmarks for the current pair
            PoseLandmark landmark1 = landmarks.get(index1);
            PoseLandmark landmark2 = landmarks.get(index2);

            // Check if both landmarks are in frame with a high likelihood
            if (landmark1.getInFrameLikelihood() > 0.95f && landmark2.getInFrameLikelihood() > 0.95f) {
                // Translate coordinates for drawing
                float x1 = translateX(landmark1.getPosition().x);
                float y1 = translateY(landmark1.getPosition().y);
                float x2 = translateX(landmark2.getPosition().x);
                float y2 = translateY(landmark2.getPosition().y);

                // Draw a line between the two landmarks
                canvas.drawLine(x1, y1, x2, y2, paint2);
            }
        }
    }
    private float translateX(float x) {
        if (isFlipped) {
            x = getWidth() - (x * scaleX - offsetX);
        } else {
            x = x * scaleX + offsetX;
        }
        return x;
    }

    private float translateY(float y) {
        return y * scaleY - offsetY;
    }
}