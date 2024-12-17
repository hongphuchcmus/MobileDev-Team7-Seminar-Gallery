package com.example.gallery_group07.fragments;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.gallery_group07.activities.FavoritesActivity;
import com.example.gallery_group07.constants.OptionMenuConstants;
import com.example.gallery_group07.data.MediaStoreImage;
import com.example.gallery_group07.R;
import com.example.gallery_group07.adapters.ImageScollerAdapter;
import com.example.gallery_group07.activities.ImageGridActivity;

public class ImageScrollerFragment extends Fragment {
    // Debug
    public static final String LOG_TAG = "ImageScrollerFragment";

    private ImageGridActivity gridActivity;
    private ViewPager2 viewPager;

    // Image manipulation settings
    private static final float FLING_MULTIPLIER = 1.0f;
    private static final float FLING_FRICTION = 10.0f;
    private static final float SCALE_SNAPPING_THRESHOLD = 0.05f;

    // Image state
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector spanGestureDetector;
    private boolean isScalingGestureActive = false;
    private ImageView visibleImageView;
    private float scaleFactor = 1.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float velocityX = 0.0f;
    private float velocityY = 0.0f;
    private int initialPosition = 0;

    // UI buttons
    private ImageButton deleteButton;
    private ImageButton favoriteButton;
    private ImageButton shareButton;
    private ImageButton moreButton;

    // Animations
    private Handler animationHandler;
    private FlingAnimationRunnable flingAnimationRunnable;

    // Data
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;

    public ImageScrollerFragment(int initialPosition){
        this.initialPosition = initialPosition;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        gridActivity = (ImageGridActivity) context;
        Log.i(LOG_TAG, "Attached");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_scroller_fragment, container, false);
        Log.i(LOG_TAG, "Creating");

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        deleteButton = view.findViewById(R.id.btn_delete);
        favoriteButton = view.findViewById(R.id.btn_favorite);
        shareButton = view.findViewById(R.id.btn_share);
        moreButton = view.findViewById(R.id.btn_more);

        deleteButton.setOnClickListener(listener -> deleteCurrentImage());
        favoriteButton.setOnClickListener(listener -> toggleFavouriteStatus());
        shareButton.setOnClickListener(listener -> shareCurrentImage());
        moreButton.setOnClickListener(listener -> moreOptions());

        viewPager = view.findViewById(R.id.img_scroller_viewpager);
        ImageScollerAdapter adapter = new ImageScollerAdapter(requireContext(), gridActivity.getImageList());
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                update(position);
            }
        });
        viewPager.setCurrentItem(initialPosition, false);

        ActivityResultCallback<ActivityResult> deleteResultCallback = result -> {
            if (result.getResultCode() == RESULT_OK) {
                Log.i(LOG_TAG, "Deletion request granted");
                onPostImageDeletion();
                return;
            }
            Log.e(LOG_TAG, "Failed to delete the image");
            Toast.makeText(requireContext(), "Failed to delete the image. Please try again.", Toast.LENGTH_LONG).show();
        };
        deleteRequestLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), deleteResultCallback);

        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleListener());
        spanGestureDetector = new GestureDetector(requireContext(), new SpanListener());

        animationHandler = new Handler(Looper.getMainLooper());
        flingAnimationRunnable = new FlingAnimationRunnable();
        RecyclerView viewPagerRecyclerView = (RecyclerView) viewPager.getChildAt(0);
        viewPagerRecyclerView.setOnTouchListener(new TouchListener());

        Log.i(LOG_TAG, "Created");

    }

    public void moreOptions(){
        MediaStoreImage currentImage = gridActivity.getImageList().get(viewPager.getCurrentItem());
        String[] options = {
                OptionMenuConstants.ALBUM_OPTION_MENU,
        };
        OptionMenuDialogFragment dialog = new OptionMenuDialogFragment(gridActivity, "More", options);
        dialog.show(gridActivity.getSupportFragmentManager(), "OptionMenuDialogFragment");
    }

    public void deleteCurrentImage() {
        Log.i(LOG_TAG, "Deleting current image");
        MediaStoreImage currentImage = gridActivity.getImageList().get(viewPager.getCurrentItem());
        if (currentImage == null) return;

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            gridActivity.getViewModel().performDeleteImageLegacy(gridActivity, currentImage);
            onPostImageDeletion();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            IntentSenderRequest request = gridActivity.getViewModel().performDeleteImage(gridActivity, currentImage);
            if (request != null) deleteRequestLauncher.launch(request);
            return;
        }
    }

    public void shareCurrentImage(){
        // Create an intent to share the image
        MediaStoreImage currentImage = gridActivity.getImageList().get(viewPager.getCurrentItem());
        if (currentImage == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, currentImage.contentUri);
        startActivity(Intent.createChooser(shareIntent, "Share image via"));
    }

    public void toggleFavouriteStatus(){
        MediaStoreImage currentImage = gridActivity.getImageList().get(viewPager.getCurrentItem());
        if (currentImage == null) return;
        boolean isFavorite = gridActivity.getViewModel().isImageInAlbum(gridActivity, FavoritesActivity.ALBUM_NAME, currentImage.id);
        if (isFavorite){
            gridActivity.getViewModel().removeImageFromAlbum(gridActivity, FavoritesActivity.ALBUM_NAME, currentImage.id);
        } else {
            gridActivity.getViewModel().addImageToAlbum(gridActivity, FavoritesActivity.ALBUM_NAME, currentImage.id);
        }
        setFavoriteStatus(!isFavorite);

        if (gridActivity instanceof FavoritesActivity){
            gridActivity.getViewModel().removeFromImageList(currentImage);
            gridActivity.reloadImages();
            ImageScollerAdapter adapter = (ImageScollerAdapter) viewPager.getAdapter();
            if (adapter == null) return;
            adapter.update(gridActivity.getImageList());
            update(viewPager.getCurrentItem());
        }
    }

    public void setFavoriteStatus(boolean isFavorite){
        if (isFavorite){
            favoriteButton.setImageResource(R.drawable.ic_favourite);
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favourite_outline);
        }
    }

    public void onPostImageDeletion(){
        MediaStoreImage currentImage = gridActivity.getImageList().get(viewPager.getCurrentItem());
        gridActivity.getViewModel().performDeleteImagePostRequest(requireContext(), currentImage);
        ImageScollerAdapter adapter = (ImageScollerAdapter) viewPager.getAdapter();
        if (adapter == null) return;
        gridActivity.reloadImages();
        adapter.update(gridActivity.getImageList());
        update(viewPager.getCurrentItem());
    }

    public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            // Handle pinch-to-zoom gestures manually to avoid undesired behavior
            if (!isScalingGestureActive) {
                viewPager.setCurrentItem(viewPager.getCurrentItem(), false);
            }
            isScalingGestureActive = true;

            // Update scale and offset
            float preScaleFactor = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 10.0f));
            float scaleChange = scaleFactor / preScaleFactor;

            offsetX += (1.0f - scaleChange) * (detector.getFocusX() - visibleImageView.getWidth() * 0.5f - offsetX);
            offsetY += (1.0f - scaleChange) * (detector.getFocusY() - visibleImageView.getHeight() * 0.5f - offsetY);

            // Apply transformations
            applyImageTransformations();
            return true;
        }
    }

    public class SpanListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            // Update offset based on panning
            offsetX -= distanceX;
            offsetY -= distanceY;

            // Clamp and apply transformations
            applyImageTransformations();
            return true;
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float vX, float vY) {
            velocityX = vX * FLING_MULTIPLIER;
            velocityY = vY * FLING_MULTIPLIER;
            animationHandler.post(flingAnimationRunnable);
            return true;
        }
    }

    private ImageView findVisibleImageView() {
        if (viewPager == null) {
            return null;
        }
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
        ImageScollerAdapter.ImageViewHolder viewHolder =
                (ImageScollerAdapter.ImageViewHolder) recyclerView.findViewHolderForAdapterPosition(viewPager.getCurrentItem());
        return viewHolder != null ? viewHolder.getImageView() : null;
    }
    
    private void applyImageTransformations() {
        // Restrict the image panning into its boundaries
        if (visibleImageView == null) return;
        int viewWidth = visibleImageView.getWidth();
        int viewHeight = visibleImageView.getHeight();
        int scaledImageWidth = (int) (viewWidth * scaleFactor);
        int scaledImageHeight = (int) (viewHeight * scaleFactor);

        offsetX = Math.min(Math.max(offsetX, viewWidth - scaledImageWidth * 0.5f - viewWidth * 0.5f), scaledImageWidth * 0.5f - viewWidth * 0.5f);
        offsetY = Math.min(Math.max(offsetY, viewHeight - scaledImageHeight * 0.5f - viewHeight * 0.5f), scaledImageHeight * 0.5f - viewHeight * 0.5f);

        visibleImageView.setScaleX(scaleFactor);
        visibleImageView.setScaleY(scaleFactor);
        visibleImageView.setTranslationX(offsetX);
        visibleImageView.setTranslationY(offsetY);
    }

    private void resetImageTransformations() {
        scaleFactor = 1.0f;
        offsetX = 0.0f;
        offsetY = 0.0f;
    }

    public  class FlingAnimationRunnable implements Runnable {
        public final static long FRAME_DELAY_MILLIS = 16;
        public final static float FRAME_DELAY_SECONDS = FRAME_DELAY_MILLIS * 0.001f;

        @Override
        public void run() {
            // If the velocity is small enough, stop the animation
            if (Math.abs(velocityX) < 0.5f && Math.abs(velocityY) < 0.5f) {
                velocityX = 0f;
                velocityY = 0f;
                return;
            }

            // Apply friction
            float friction = Math.max(0f, 1.0f - FLING_FRICTION * FRAME_DELAY_SECONDS);
            velocityX *= friction;
            velocityY *= friction;

            // Update offsets
            offsetX += velocityX * FRAME_DELAY_SECONDS;
            offsetY += velocityY * FRAME_DELAY_SECONDS;

            applyImageTransformations();

            // Schedule the next frame
            animationHandler.postDelayed(this, FRAME_DELAY_MILLIS);
        }
    }

    private class TouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) view.performClick();
            if (visibleImageView == null) visibleImageView = findVisibleImageView();
            if (visibleImageView == null) return view.onTouchEvent(event); // View wasn't ready, use default behaviour

            if (isScalingGestureActive && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                isScalingGestureActive = false;
                // Snap to original size if close enough
                if (scaleFactor - 1.0f <= SCALE_SNAPPING_THRESHOLD) {
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

            return view.onTouchEvent(event);
        }
    }

    public void update(int newPosition){
        if (newPosition > gridActivity.getImageList().size()-1){
            newPosition = gridActivity.getImageList().size()-1;
        }
        if (newPosition < 0){
            gridActivity.getSupportFragmentManager().popBackStack();
            return;
        }
        MediaStoreImage currentImage = gridActivity.getImageList().get(newPosition);
        boolean isFavorite = gridActivity.getViewModel().isImageInAlbum(gridActivity, FavoritesActivity.ALBUM_NAME, currentImage.id);
        setFavoriteStatus(isFavorite);

        isScalingGestureActive = false;
        visibleImageView = findVisibleImageView();
        resetImageTransformations();
        applyImageTransformations();
    }
}
