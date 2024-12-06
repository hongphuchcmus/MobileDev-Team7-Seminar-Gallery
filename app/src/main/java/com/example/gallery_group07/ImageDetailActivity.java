package com.example.gallery_group07;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.Date;
import java.util.List;

public class ImageDetailActivity extends AppCompatActivity {
    public static final String TAG = "ImageDetailView>>";

    private ImageView imageDetailView;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector spanGestureDetector;

    // Image state
    private int imgIndex = 0;
    private float scaleFactor = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail);

        long imgId = getIntent().getLongExtra("imgId", -1);
        imgIndex = ImageManager.getInstance().getImageIndexById(imgId);
//        String imgDisplayName = getIntent().getStringExtra("imgDisplayName");
//        long imgDateAddedMillis = getIntent().getLongExtra("imgDateAddedMillis", 0);
//        Date imgDateAdded = new Date(imgDateAddedMillis);
//        Uri imgUri = Uri.parse(getIntent().getStringExtra("imgUri"));

        long imgId = getIntent().getLongExtra("imgId", 0);
        imgIndex = ImageManager.getInstance().getImageIndexById(imgId); //new MediaStoreImage(imgId, imgDisplayName, imgDateAdded, imgUri);
      
        if (imgIndex < 0){
            Toast.makeText(this, "Error loading image with id " + imgId, Toast.LENGTH_LONG).show();
            return;
        }

        MediaStoreImage image = ImageManager.getInstance().getImage(imgIndex);

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

        ImageButton deleteButton = findViewById(R.id.btn_delete);
        ImageButton favoriteButton = findViewById(R.id.btn_favorite);
        ImageButton shareButton = findViewById(R.id.btn_share);

//        deleteButton.setOnClickListener(v -> moveToTrash(image));
        favoriteButton.setOnClickListener(v -> markAsFavorite(image));
        shareButton.setOnClickListener(v -> shareImage(image));


        SharedPreferences preferences = getSharedPreferences("image_favorites", MODE_PRIVATE);
        boolean isFavorite = preferences.getBoolean(String.valueOf(image.id), false);
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite);
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    private void markAsFavorite(MediaStoreImage currentImage) {
        SharedPreferences preferences = getSharedPreferences("image_favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        ImageButton favoriteButton = findViewById(R.id.btn_favorite);

        boolean isFavorite = preferences.getBoolean(String.valueOf(currentImage.id), false);
        editor.putBoolean(String.valueOf(currentImage.id), !isFavorite);
        editor.apply();

        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
            Toast.makeText(this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite);
            Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
        }
    }


    private void shareImage(MediaStoreImage currentImage) {
        // Chia sẻ ảnh
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, currentImage.contentUri);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ ảnh qua"));
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

            if (Math.abs(distanceX) > 1.0f){
                // Next Image
                if (distanceX > 0.0f){
                    activity.changeToNextImage();
                    return true;
                }
                // Previous Image
                if (distanceX < 0.0f){
                    activity.changeToPreviousImage();
                    return true;
                }
            }

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

    private void changeToNextImage(){
        List<MediaStoreImage> images = ImageManager.getInstance().getImageList();
        if (imgIndex < images.size() - 1) {
            imgIndex++;
            updateContent();
        } else {
            Toast.makeText(this, "This is the last image", Toast.LENGTH_SHORT).show();
        }
    }

    private  void changeToPreviousImage(){
        if (imgIndex > 0) {
            imgIndex--;
            updateContent();
        } else {
            Toast.makeText(this, "This is the first image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateContent() {
        MediaStoreImage image = ImageManager.getInstance().getImage(imgIndex);

        if (image != null) {
            setTitle(image.displayName);
            Glide.with(this)
                    .load(image.contentUri)
                    .apply(new RequestOptions()
                            .fitCenter()
                            .format(DecodeFormat.PREFER_ARGB_8888)
                            .override(Target.SIZE_ORIGINAL))
                    .placeholder(R.drawable.image_placeholder)
                    .into(imageDetailView);

            scaleFactor = 1.0f;
            offsetX = 0.0f;
            offsetY = 0.0f;
            imageDetailView.setScaleX(scaleFactor);
            imageDetailView.setScaleY(scaleFactor);
            imageDetailView.setTranslationX(offsetX);
            imageDetailView.setTranslationY(offsetY);
        } else {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }
}
