package com.example.gallery_group07.activities;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallery_group07.R;
import com.example.gallery_group07.adapters.DownloadImageAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DownloadActivity extends AppCompatActivity {

    private ArrayList<String> urlList = new ArrayList<>();
    private DownloadImageAdapter imageAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);


        initRecyclerView();
        loadURLs();
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.downloadRecycleView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        imageAdapter = new DownloadImageAdapter(urlList, this);
        recyclerView.setAdapter(imageAdapter);
    }

    private void loadURLs() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot != null && snapshot.hasChildren()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        urlList.add(dataSnapshot.getValue().toString());
                    }
                    imageAdapter.setUpdatedData(urlList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        DatabaseReference databaseReference = database.getReference().child("user_images");
        databaseReference.addValueEventListener(listener);
    }
}
