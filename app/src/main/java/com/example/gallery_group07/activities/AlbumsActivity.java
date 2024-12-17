package com.example.gallery_group07.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallery_group07.R;
import com.example.gallery_group07.SharedViewModel;
import com.example.gallery_group07.adapters.AlbumAdapter;
import com.example.gallery_group07.data.AlbumItem;

import java.util.List;

public class AlbumsActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AlbumsActivity";
    private List<AlbumItem> albumList;
    private SharedViewModel viewModel;
    private AlbumAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Albums");

        viewModel = (new ViewModelProvider(this)).get(SharedViewModel.class);
        loadAlbums();
        adapter = new AlbumAdapter(this, albumList);
        RecyclerView recyclerView = findViewById(R.id.gallery);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }

    public void loadAlbums(){
        albumList = viewModel.loadAlbums(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlbums();
        adapter.update(albumList);
    }
}
