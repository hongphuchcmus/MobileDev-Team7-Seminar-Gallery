package com.example.gallery_group07.activities;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallery_group07.R;


public class MainActivity extends ImageGridActivity {
    private static final int READ_EXTERNAL_STORAGE_REQUEST = 0x1045;

    private static final String TAG = "MainActivity>>";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Gallery");
        if (haveStoragePermission()) {
            loadAndShowImages();
        } else {
            requestPermission();
        }
    }

    @Override
    public void loadImages(){
        super.loadImages();
    }

    public void loadAndShowImages(){
        loadImages();
        // save the ids
        getViewModel().addAllToSavedImageIds(this);
        RecyclerView recyclerView = findViewById(R.id.gallery);
        showImages(recyclerView, 4);
    }

    private boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            boolean readPermission = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean writePermission = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            return readPermission && writePermission;
        }
    }

    private void requestPermission() {
        if (!haveStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[]{
                        READ_MEDIA_IMAGES,
                }, READ_EXTERNAL_STORAGE_REQUEST);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        READ_EXTERNAL_STORAGE,
                        WRITE_EXTERNAL_STORAGE
                }, READ_EXTERNAL_STORAGE_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == READ_EXTERNAL_STORAGE_REQUEST) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
                loadAndShowImages();
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE);

                if (showRationale) {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Please provide permission to read the external storage", Toast.LENGTH_LONG).show();
                    goToSettings();
                }
            }
        }
    }

    private void goToSettings() {
        String packageUri = String.format("package:%s", getPackageName());
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(packageUri));
        intent.addCategory(Intent.CATEGORY_DEFAULT);//Redundant
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}