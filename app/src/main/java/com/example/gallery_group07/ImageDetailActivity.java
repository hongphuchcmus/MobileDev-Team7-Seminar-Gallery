package com.example.gallery_group07;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
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

    private ImageView imageDetailView;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector spanGestureDetector;
    private ViewPager2 viewPager;

    // Image state
    private int imgIndex = 0;
    private float scaleFactor = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;

    // Buttons
    private ImageButton deleteButton;
    private ImageButton favoriteButton;
    private ImageButton shareButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        long imgId = getIntent().getLongExtra("imgId", -1);
        imgIndex = ImageManager.getInstance().getImageIndexById(imgId);

        if (imgIndex < 0){
            Toast.makeText(this, "Error loading image with id " + imgId, Toast.LENGTH_LONG).show();
            return;
        }

        MediaStoreImage image = ImageManager.getInstance().getImage(imgIndex);

        setTitle(image.displayName);

        // Set up View Pager
        viewPager = findViewById(R.id.viewPager);
        List<MediaStoreImage> images = ImageManager.getInstance().getImageList();
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, images);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(imgIndex, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                imgIndex = position;
                updateContent();
            }
        });

        // Controls
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(this));
        spanGestureDetector = new GestureDetector(this, new SpanListener(this));

        viewPager.getChildAt(0).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // If there is no ImageView for current image, get one from the pager
                if (imageDetailView == null){
                    imageDetailView = getCurrentImageView();
                }
                // If the result is still null, it means the view holder weren't ready yet
                // In that case we do nothing.
                if (imageDetailView == null){
                    return true;
                };

                scaleGestureDetector.onTouchEvent(motionEvent);

                // TODO: Fix issue where swiping is triggered when zooming out to the original size (scaleFactor == 1.0f).
                // Prevent unintended swiping while zooming out and maintain the ability to pan.
                if (scaleFactor > 1.0f){
                    spanGestureDetector.onTouchEvent(motionEvent);
                    return true; // Skip swiping gesture
                }

                return view.onTouchEvent(motionEvent);
            }
        });


        // Setup Button Events
        deleteButton = findViewById(R.id.btn_delete);
        favoriteButton = findViewById(R.id.btn_favorite);
        shareButton = findViewById(R.id.btn_share);

//      deleteButton.setOnClickListener(v -> moveToTrash(image));
        favoriteButton.setOnClickListener(v -> markAsFavorite());
        shareButton.setOnClickListener(v -> shareImage());

        updateContent();
    }

    private void markAsFavorite() {
        MediaStoreImage currentImage = ImageManager.getInstance().getImage(imgIndex);

        SharedPreferences preferences = getSharedPreferences("image_favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        boolean isFav = preferences.getBoolean(String.valueOf(currentImage.id), false);
        editor.putBoolean(String.valueOf(currentImage.id), !isFav);
        editor.apply();

        isFav = !isFav;
        setFavoriteIcon(isFav);
    }

    private void shareImage() {
        // Chia sẻ ảnh
        MediaStoreImage currentImage = ImageManager.getInstance().getImage(imgIndex);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, currentImage.contentUri);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ ảnh qua"));
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        // If there is no ImageView for current image, get one from the pager
//        if (imageDetailView == null){
//            imageDetailView = getCurrentImageView();
//        }
//        // If there is still no result, it means the view holder weren't ready yet
//        // In that case we do nothing.
//        if (imageDetailView != null){
//            scaleGestureDetector.onTouchEvent(event);
//            spanGestureDetector.onTouchEvent(event);
//        }
//        return true;
//    }

    private static class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
        ImageDetailActivity activity;

        public ScaleListener(ImageDetailActivity imageDetailActivity){
            this.activity = imageDetailActivity;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            // Determine focal point (where the gesture is happening)
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            // Compute scale adjustment
            float preScaleFactor = activity.scaleFactor;
            activity.scaleFactor *= detector.getScaleFactor();;
            activity.scaleFactor = Math.max(1.0f, Math.min(activity.scaleFactor, 10.0f));
            float scaleChange = activity.scaleFactor / preScaleFactor;

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

    private void updateContent() {
        MediaStoreImage image = ImageManager.getInstance().getImage(imgIndex);
        if (image == null){
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            return;
        }

        setTitle(image.displayName);

        scaleFactor = 1.0f;
        offsetX = 0.0f;
        offsetY = 0.0f;

        // Set favourite icon
        setFavoriteIcon(isFavorite(image.contentUri));

        imageDetailView = getCurrentImageView();
        if (imageDetailView == null){
            Log.e(TAG, "Image view is null");
            return;
        }
        imageDetailView.setScaleX(scaleFactor);
        imageDetailView.setScaleY(scaleFactor);
        imageDetailView.setTranslationX(offsetX);
        imageDetailView.setTranslationY(offsetY);
    }

    private ImageView getCurrentImageView(){
        if (viewPager == null){
            return null;
        }
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        // Notice that recyclerView returns null for views that are off-screen
        ImagePagerAdapter.ImageViewHolder viewHolder = (ImagePagerAdapter.ImageViewHolder) recyclerView.findViewHolderForAdapterPosition(imgIndex);
        if (viewHolder != null){
            return viewHolder.imageView;
        }
        return null;
    }

    // Repeated from FavouritesActivity class
    private boolean isFavorite(Uri imageUri) {
        SharedPreferences preferences = getSharedPreferences("image_favorites", MODE_PRIVATE);
        return preferences.getBoolean(String.valueOf(imageUri.getLastPathSegment()), false);
    }

    private void setFavoriteIcon(boolean glow){
        favoriteButton.setImageResource(glow ? R.drawable.ic_favourite : R.drawable.ic_favourite_outline);
    }
}
