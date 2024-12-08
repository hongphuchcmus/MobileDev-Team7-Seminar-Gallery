package com.example.gallery_group07;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

public class ImageDetailActivity extends AppCompatActivity {
    public static final String TAG = "ImageDetailView>>";

    private ImageView visibleImageView; // The currently displayed image view
    private ScaleGestureDetector scaleGestureDetector; // Handles pinch-to-zoom
    private GestureDetector spanGestureDetector; // Handles panning gestures
    private ViewPager2 viewPager; // Allows swiping between images

    // Image manipulation settings
    private final float FLING_MULTIPLIER = 1.0f;
    private final float FLING_FRICTION = 10.0f;
    private final float SCALE_SNAPPING_THRESHOLD = 0.05f;

    // Image state
    private boolean isScalingGestureActive = false; // Tracks zooming state
    private int imgIndex = 0; // Index of the currently displayed image
    private float targetScaleFactor = 1.0f;
    private float scaleFactor = 1.0f; // Zoom scale
    private float offsetX = 0.0f; // Horizontal translation
    private float offsetY = 0.0f; // Vertical translation
    private float velocityX = 0.0f; // Horizontal velocity on fling action
    private float velocityY = 0.0f; // Vertical velocity on fling action

    // UI buttons
    private ImageButton deleteButton;
    private ImageButton favoriteButton;
    private ImageButton shareButton;

    // Animations
    public final long FRAME_DELAY_MILLIS = 16; // 16ms ~ 60FPS
    public final float FRAME_DELAY_SECONDS = FRAME_DELAY_MILLIS * 0.001f; // 16ms ~ 60FPS
    private Handler animationHandler;
    private FlingAnimationRunnable flingAnimationRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        // Retrieve the image ID passed to the activity
        long imgId = getIntent().getLongExtra("imgId", -1);
        imgIndex = ImageManager.getInstance().getImageIndexById(imgId);

        if (imgIndex < 0) {
            Toast.makeText(this, "Error loading image with id " + imgId, Toast.LENGTH_LONG).show();
            return;
        }

        MediaStoreImage image = ImageManager.getInstance().getImage(imgIndex);
        setTitle(image.displayName);

        // Initialize ViewPager to display images
        viewPager = findViewById(R.id.viewPager);
        List<MediaStoreImage> images = ImageManager.getInstance().getImageList();
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, images);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(imgIndex, false);

        // Update content when the displayed page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                imgIndex = position;
                updateDisplayedImage();
            }
        });

        // Gesture detectors for zooming and panning
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(this));
        spanGestureDetector = new GestureDetector(this, new SpanListener(this));

        // Intercept touch events to manage gestures
        viewPager.getChildAt(0).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP){
                    view.performClick();
                }
                return handleTouchEvent(view, event);
            }
        });

        // Initialize button actions
        deleteButton = findViewById(R.id.btn_delete);
        favoriteButton = findViewById(R.id.btn_favorite);
        shareButton = findViewById(R.id.btn_share);

        favoriteButton.setOnClickListener(v -> toggleFavoriteStatus());
        shareButton.setOnClickListener(v -> shareImage());

        updateDisplayedImage();

        // This Handler will run on UI thread (main thread), therefore having
        // permission to alter UI
        animationHandler = new Handler(Looper.getMainLooper());
        flingAnimationRunnable = new FlingAnimationRunnable(this);
    }

    private void toggleFavoriteStatus() {
        MediaStoreImage currentImage = ImageManager.getInstance().getImage(imgIndex);

        // Toggle favorite status and save it in preferences
        SharedPreferences preferences = getSharedPreferences("image_favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        boolean isFav = preferences.getBoolean(String.valueOf(currentImage.id), false);
        editor.putBoolean(String.valueOf(currentImage.id), !isFav);
        editor.apply();

        // Update UI to reflect the new favorite state
        updateFavoriteIcon(!isFav); // Remember we are toggling here
    }

    private void shareImage() {
        MediaStoreImage currentImage = ImageManager.getInstance().getImage(imgIndex);

        // Create an intent to share the image
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, currentImage.contentUri);
        startActivity(Intent.createChooser(shareIntent, "Share image via"));
    }

    private void updateDisplayedImage() {
        MediaStoreImage image = ImageManager.getInstance().getImage(imgIndex);
        if (image == null) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            return;
        }

        setTitle(image.displayName);

        // Reset transformations for the new image
        scaleFactor = 1.0f;
        offsetX = 0.0f;
        offsetY = 0.0f;
        velocityX = 0.0f;
        velocityY = 0.0f;

        updateFavoriteIcon(isFavorite(image.contentUri));
        visibleImageView = getVisibleImageView();
        resetImageTransformations();
    }

    private boolean isFavorite(Uri imageUri) {
        SharedPreferences preferences = getSharedPreferences("image_favorites", MODE_PRIVATE);
        return preferences.getBoolean(String.valueOf(imageUri.getLastPathSegment()), false);
    }

    private ImageView getVisibleImageView() {
        if (viewPager == null) {
            return null;
        }
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        ImagePagerAdapter.ImageViewHolder viewHolder = (ImagePagerAdapter.ImageViewHolder) recyclerView.findViewHolderForAdapterPosition(imgIndex);
        return viewHolder != null ? viewHolder.imageView : null;
    }

    // IMAGE MANIPULATION
    // --------------------------
    // Image zooming and panning
    // --------------------------

    private static class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        private final ImageDetailActivity activity;

        public ScaleListener(ImageDetailActivity imageDetailActivity) {
            this.activity = imageDetailActivity;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            // Handle pinch-to-zoom gestures manually to avoid undesired behavior
            activity.isScalingGestureActive = true;

            // Update scale and offset
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            float preScaleFactor = activity.scaleFactor;
            activity.scaleFactor *= detector.getScaleFactor();
            activity.scaleFactor = Math.max(1.0f, Math.min(activity.scaleFactor, 10.0f));
            float scaleChange = activity.scaleFactor / preScaleFactor;

            activity.offsetX += (1.0f - scaleChange) * (focusX - activity.visibleImageView.getWidth() * 0.5f - activity.offsetX);
            activity.offsetY += (1.0f - scaleChange) * (focusY - activity.visibleImageView.getHeight() * 0.5f - activity.offsetY);

            // Apply transformations
            activity.applyImageTransformations();
            return true;
        }
    }

    private static class SpanListener extends GestureDetector.SimpleOnGestureListener {
        private final ImageDetailActivity activity;

        public SpanListener(ImageDetailActivity imageDetailActivity) {
            this.activity = imageDetailActivity;
        }

        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            // Update offset based on panning
            activity.offsetX -= distanceX;
            activity.offsetY -= distanceY;

            // Clamp and apply transformations
            activity.applyImageTransformations();
            return true;
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            activity.velocityX = velocityX * activity.FLING_MULTIPLIER;
            activity.velocityY = velocityY * activity.FLING_MULTIPLIER;
            activity.startFlingAnimation();
            return true;
        }
    }

    private void startFlingAnimation(){
        animationHandler.post(flingAnimationRunnable);
    }

    private void updateFavoriteIcon(boolean glow) {
        favoriteButton.setImageResource(glow ? R.drawable.ic_favourite : R.drawable.ic_favourite_outline);
    }

    private void applyImageTransformations(){
        // Restrict the image panning into its boundaries
        clampOffsetToBounds();
        visibleImageView.setScaleX(scaleFactor);
        visibleImageView.setScaleY(scaleFactor);
        visibleImageView.setTranslationX(offsetX);
        visibleImageView.setTranslationY(offsetY);
    }

    private void resetImageTransformations() {
        scaleFactor = 1.0f;
        offsetX = 0.0f;
        offsetY = 0.0f;

        if (visibleImageView != null) {
            visibleImageView.setScaleX(scaleFactor);
            visibleImageView.setScaleY(scaleFactor);
            visibleImageView.setTranslationX(offsetX);
            visibleImageView.setTranslationY(offsetY);
        }
    }

    private void clampOffsetToBounds() {
        // Ensure offsets don't exceed image boundaries
        int viewWidth = visibleImageView.getWidth();
        int viewHeight = visibleImageView.getHeight();
        int scaledImageWidth = (int) (viewWidth * scaleFactor);
        int scaledImageHeight = (int) (viewHeight * scaleFactor);

        offsetX = Math.min(Math.max(offsetX, viewWidth - scaledImageWidth * 0.5f - viewWidth * 0.5f), scaledImageWidth * 0.5f - viewWidth * 0.5f);
        offsetY = Math.min(Math.max(offsetY, viewHeight - scaledImageHeight * 0.5f - viewHeight * 0.5f), scaledImageHeight * 0.5f - viewHeight * 0.5f);
    }

    private boolean handleTouchEvent(View view, MotionEvent event) {
        if (visibleImageView == null) {
            visibleImageView = getVisibleImageView();
            if (visibleImageView == null) {
                return true; // View not ready
            }
        }

        if (isScalingGestureActive &&
                (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
            isScalingGestureActive = false;
            // Snap to original size if close enough
            if (scaleFactor - 1.0f <= SCALE_SNAPPING_THRESHOLD){
                scaleFactor = 1.0f;
                applyImageTransformations();
            }
            return true;
        }

        scaleGestureDetector.onTouchEvent(event);
        if (scaleFactor > 1.0f) { // Support pan gesture when zooming-in
            spanGestureDetector.onTouchEvent(event);
            return true;
        }
        if (isScalingGestureActive) {
            return true; // Clamped zoom-out
        }

        return view.onTouchEvent(event); // Default view pager handling
    }

    // ANIMATIONS
    // --------------
    // Smoothing animation for manipulation gestures
    // --------------

    private static class FlingAnimationRunnable implements Runnable {
        private final ImageDetailActivity activity;

        public FlingAnimationRunnable(ImageDetailActivity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            if (activity == null) {
                return; // Activity is gone; stop animation
            }

            // If the velocity is small enough, stop the animation
            if (Math.abs(activity.velocityX) < 0.5f && Math.abs(activity.velocityY) < 0.5f) {
                activity.velocityX = 0f;
                activity.velocityY = 0f;
                return;
            }

            // Apply friction
            float friction = Math.max(0f, 1.0f - activity.FLING_FRICTION * activity.FRAME_DELAY_SECONDS);
            activity.velocityX *= friction;
            activity.velocityY *= friction;

            // Update offsets
            activity.offsetX += activity.velocityX * activity.FRAME_DELAY_SECONDS;
            activity.offsetY += activity.velocityY * activity.FRAME_DELAY_SECONDS;

            activity.applyImageTransformations();

            // Schedule the next frame
            activity.animationHandler.postDelayed(this, activity.FRAME_DELAY_MILLIS);
        }
    }

    private static class ScaleAnimationRunnable implements Runnable {
        private final ImageDetailActivity activity;

        public ScaleAnimationRunnable(ImageDetailActivity activity){
            this.activity = activity;
        }

        @Override
        public void run() {
            if (activity == null) {
                return; // Activity is gone; stop animation
            }
        }
    }

}

