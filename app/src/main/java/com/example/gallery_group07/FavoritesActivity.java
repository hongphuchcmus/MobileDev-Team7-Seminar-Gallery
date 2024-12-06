package com.example.gallery_group07;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.SharedPreferences;

import java.util.LinkedList;
import java.util.List;


public class FavoritesActivity extends AppCompatActivity {
    private static final String TAG = "FavoriteImagesActivity>>";

    private List<MediaStoreImage> images;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Favorites");

        loadImages();
        showImages();
    }

    private void loadImages(){
        images = new LinkedList<>();
        for (int i = 0; i <  ImageManager.getInstance().getImageListSize(); i++){
            MediaStoreImage img = ImageManager.getInstance().getImage(i);
            if (isFavorite(img.contentUri)){
                images.add(img);
            }
        }
    }

    private void showImages(){
        loadImages();

        GalleryAdapter galleryAdapter = new GalleryAdapter(this, images);
        recyclerView = findViewById(R.id.gallery);
        GridLayoutManager gridLayoutManager = getGridLayoutManager(galleryAdapter);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(galleryAdapter);
    }

    private @NonNull GridLayoutManager getGridLayoutManager(GalleryAdapter galleryAdapter) {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = galleryAdapter.getItemViewType(position);
                if (viewType == GalleryAdapter.HEADER){
                    return 4; // Let a header span a fully row
                } else {
                    return 1;
                }
            }
        });
        return gridLayoutManager;
    }

    private boolean isFavorite(Uri imageUri) {
        SharedPreferences preferences = getSharedPreferences("image_favorites", MODE_PRIVATE);
        return preferences.getBoolean(String.valueOf(imageUri.getLastPathSegment()), false);
    }
}
