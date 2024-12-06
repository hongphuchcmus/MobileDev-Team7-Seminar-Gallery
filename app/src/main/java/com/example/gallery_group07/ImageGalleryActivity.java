package com.example.gallery_group07;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.List;

public class ImageGalleryActivity extends AppCompatActivity {
    public final static String TAG = "ImageGalleryActivity>>" ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Get the image list from your manager
        List<MediaStoreImage> images = ImageManager.getInstance().getImageList();

        // Set up the adapter
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, images);
        viewPager.setAdapter(adapter);

        // Set the current item if needed
        long imgId = getIntent().getLongExtra("imgId", 0);
        int imgIndex = ImageManager.getInstance().getImageIndexById(imgId);
        if (imgIndex < 0){
            return;
        }
        Log.i(TAG, String.format("Image index is %d", imgIndex));
        viewPager.setCurrentItem(imgIndex, false);
    }
}
