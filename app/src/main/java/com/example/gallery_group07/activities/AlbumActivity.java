package com.example.gallery_group07.activities;

import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallery_group07.R;

public class AlbumActivity extends ImageGridActivity {
    private static final String LOG_TAG = "AlbumActivity";
    public String albumName;

    @Override
    public void loadImages() {
        getViewModel().loadImages(this, albumName);
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

        albumName = getIntent().getStringExtra("albumName");
        setTitle(albumName);

        loadAndShowImages();
    }

}
