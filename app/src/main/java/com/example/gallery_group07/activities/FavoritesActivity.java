package com.example.gallery_group07.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallery_group07.R;

public class FavoritesActivity extends ImageGridActivity {
    private static final String LOG_TAG = "FavoriteActivity";
    public static final String ALBUM_NAME = "image_favorites";

    @Override
    public String getCollection() {
        return ALBUM_NAME;
    }

    @Override
    public void loadImages() {
        getViewModel().loadImages(this, ALBUM_NAME);
        setImageList(getViewModel().getImageList());
    }

    @Override
    public void reloadImages() {
        super.reloadImages();
    }

    public void loadAndShowImages(){
        loadImages();
        RecyclerView recyclerView = findViewById(R.id.gallery);
        showImages(recyclerView, 4);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Favorites");
        loadAndShowImages();
    }
}
