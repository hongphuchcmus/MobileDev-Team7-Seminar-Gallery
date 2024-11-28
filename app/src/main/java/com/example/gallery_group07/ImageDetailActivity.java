package com.example.gallery_group07;

import android.app.ActionBar;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.time.Instant;
import java.util.Date;

public class ImageDetailActivity extends AppCompatActivity {
    public static final String TAG = "ImageDetailView>>";

    private ImageView imageDetailView;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector spanGestureDetector;

    // Image state
    private float scaleFactor = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail);


        long imgId = getIntent().getLongExtra("imgId", 0);
        String imgDisplayName = getIntent().getStringExtra("imgDisplayName");
        long imgDateAddedMillis = getIntent().getLongExtra("imgDateAddedMillis", 0);
        Date imgDateAdded = new Date(imgDateAddedMillis);
        Uri imgUri = Uri.parse(getIntent().getStringExtra("imgUri"));

        MediaStoreImage image = new MediaStoreImage(imgId, imgDisplayName, imgDateAdded, imgUri);

        setTitle(image.displayName);

        // Set the image view
        imageDetailView = findViewById(R.id.img_detail_view);
        Glide.with(this)
                .load(image.contentUri)
                .apply(new RequestOptions()
                        .fitCenter()
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .override(Target.SIZE_ORIGINAL))
                .placeholder(R.drawable.image_placeholder)
                .into(imageDetailView);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(this));
        spanGestureDetector = new GestureDetector(this, new SpanListener(this));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        spanGestureDetector.onTouchEvent(event);
        return true;
    }

    private static class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        ImageDetailActivity activity;

        public ScaleListener(ImageDetailActivity imageDetailActivity){
            this.activity = imageDetailActivity;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            float scaleChange = detector.getScaleFactor();

            // Determine focal point (where the gesture is happening)
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            // Compute scale adjustment
            float prevScaleFactor = activity.scaleFactor;
            activity.scaleFactor *= scaleChange;
            activity.scaleFactor = Math.max(1.0f, Math.min(activity.scaleFactor, 10.0f));

            // Adjust translation based on the focal point
            // Don't know how it worked yet
            activity.offsetX += (1.0f - scaleChange) * (focusX - activity.imageDetailView.getWidth() * 0.5f - activity.offsetX);
            activity.offsetY += (1.0f - scaleChange) * (focusY - activity.imageDetailView.getHeight() * 0.5f - activity.offsetY);
            activity.clampOffsetToBounds();

            // Apply transformations
            activity.imageDetailView.setScaleX(activity.scaleFactor);
            activity.imageDetailView.setScaleY(activity.scaleFactor);
            activity.imageDetailView.setTranslationX(activity.offsetX);
            activity.imageDetailView.setTranslationY(activity.offsetY);

            return true;
        }
    }

    private static class SpanListener extends GestureDetector.SimpleOnGestureListener{
        ImageDetailActivity activity;

        public SpanListener(ImageDetailActivity imageDetailActivity){
            this.activity = imageDetailActivity;
        }

        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            activity.offsetX -= distanceX;
            activity.offsetY -= distanceY;

            activity.clampOffsetToBounds();
            activity.imageDetailView.setTranslationX(activity.offsetX);
            activity.imageDetailView.setTranslationY(activity.offsetY);
            return true;
        }
    }

    private void clampOffsetToBounds(){
        int viewWidth = imageDetailView.getWidth();
        int viewHeight = imageDetailView.getHeight();
        int scaledImageWidth = (int) (viewWidth * scaleFactor);
        int scaledImageHeight = (int) (viewHeight * scaleFactor);

        // Clamp to right bound
        if (viewWidth * 0.5f + offsetX + scaledImageWidth * 0.5f < viewWidth){
            offsetX = viewWidth - scaledImageWidth * 0.5f - viewWidth * 0.5f;
        }
        // Clamp to left bound
        if (viewWidth * 0.5f + offsetX - scaledImageWidth * 0.5f > 0){
            offsetX = scaledImageWidth * 0.5f - viewWidth * 0.5f;
        }
        // Clamp to upper bound
        if (viewHeight * 0.5f + offsetY - scaledImageHeight * 0.5f > 0){
            offsetY = scaledImageHeight * 0.5f - viewHeight * 0.5f;
        }
        // Clamp to lower bound
        if (viewHeight * 0.5f + offsetY + scaledImageHeight * 0.5f < viewHeight){
            offsetY = viewHeight - scaledImageHeight * 0.5f - viewHeight * 0.5f;
        }
    }
}
