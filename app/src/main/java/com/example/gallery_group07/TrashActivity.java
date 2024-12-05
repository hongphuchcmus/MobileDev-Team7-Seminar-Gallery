package com.example.gallery_group07;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrashActivity extends AppCompatActivity {
    private static final String TAG = "TrashActivity";
    private RecyclerView recyclerView;
    private List<MediaStoreImage> trashedImages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trash);
        setTitle("Trash");

        recyclerView = findViewById(R.id.recycler_view);
    }
}
