package com.example.effectivepose;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOverlay;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.List;

public class PoseOverlayView extends View {
    private List<PoseLandmark> landmarks;
    private Paint paint = new Paint();

    // Constructor used when creating the view programmatically
    public PoseOverlayView(Context context) {
        this(context, null);
    }

    // Constructor used when inflating the view from XML
    public PoseOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        landmarks = new ArrayList<>();

        paint.setColor(Color.RED); // Set the color of landmarks
        paint.setStyle(Paint.Style.FILL); // Set style
        paint.setStrokeWidth(10f); // Set the width of the circles (if drawing circles)
    }

    public void setLandmarks(List<PoseLandmark> landmarks) {
        this.landmarks = landmarks;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (landmarks == null || landmarks.isEmpty()) {
            return;
        }
        // Draw landmarks using pre-allocated objects
        for (PoseLandmark landmark : landmarks) {
            canvas.drawCircle(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y), 10, paint);
        }
    }

    private float translateX(float x) {
        // Implement logic to translate X as per your camera and view configuration
        // This might involve flipping the coordinate, scaling, or translating.
        return x;
    }

    private float translateY(float y) {
        // Implement logic to translate Y as per your camera and view configuration
        // Similar to translateX, adjust based on your needs.
        return y;
    }
}
