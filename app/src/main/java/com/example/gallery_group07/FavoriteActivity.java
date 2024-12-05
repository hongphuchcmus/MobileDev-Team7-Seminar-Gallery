package com.example.gallery_group07;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.SharedPreferences;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;

import android.provider.Settings;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;


public class FavoriteActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 0x1045;
    private static final String TAG = "FavoriteImagesActivity>>";

    private List<MediaStoreImage> images;
    RecyclerView recyclerView;
    ContentObserver contentObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Favorite Gallery");

        if (!haveStoragePermission()){
            requestPermission();
        } else {
            showImages();
        }
    }

    private boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (!haveStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, new String[]{
                        READ_MEDIA_IMAGES
                }, REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        READ_EXTERNAL_STORAGE
                }, REQUEST_CODE);
            }
        }
    }

    private void showImages(){
        if (haveStoragePermission()){
            loadImages();

            GalleryAdapter galleryAdapter = new GalleryAdapter(this, images);
            recyclerView = findViewById(R.id.gallery);
            GridLayoutManager gridLayoutManager = getGridLayoutManager(galleryAdapter);
            recyclerView.setLayoutManager(gridLayoutManager);
            recyclerView.setAdapter(galleryAdapter);
        }
    }

    private @NonNull GridLayoutManager getGridLayoutManager(GalleryAdapter galleryAdapter) {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = galleryAdapter.getItemViewType(position);
                if (viewType == GalleryAdapter.HEADER){
                    return 4;
                } else {
                    return 1;
                }
            }
        });
        return gridLayoutManager;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
                showImages();
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
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void loadImages(){
        images = queryFavoriteImages();
        if (contentObserver == null){
            contentObserver = new ContentObserver(null) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    loadImages();
                }
            };
            getApplication().getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    contentObserver
            );
        }
    }

    private List<MediaStoreImage> queryFavoriteImages() {
        LinkedList<MediaStoreImage> imageList = new LinkedList<>();

        String[] projection = {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_ADDED
        };

        String selection = null;
        String[] selectionArgs = null;

        String sortOrder = String.format("%s DESC", MediaStore.Images.Media.DATE_ADDED);
        Cursor cursor = getApplication().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
        if (cursor != null) {
            try {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID);
                int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME);
                int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_ADDED);

                Log.i(TAG, String.format("Found %d image(s)", cursor.getCount()));

                while (cursor.moveToNext()){
                    long id = cursor.getLong(idColumn);
                    Date dateModified = new Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateAddedColumn)));
                    String displayName = cursor.getString(displayNameColumn);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // Check if the image is marked as favorite
                    if (isFavorite(contentUri)) {
                        MediaStoreImage image = new MediaStoreImage(
                                id,
                                displayName,
                                dateModified,
                                contentUri
                        );
                        imageList.add(image);
                    }
                }
            } catch (IllegalArgumentException e){
                Log.e(TAG, e.toString());
            }
            cursor.close();
        }
        return imageList;
    }

    private boolean isFavorite(Uri imageUri) {
        SharedPreferences preferences = getSharedPreferences("image_favorites", MODE_PRIVATE);
        return preferences.getBoolean(String.valueOf(imageUri.getLastPathSegment()), false);
    }

    private boolean isImageAccessible(Uri imageUri){
        try {
            ContentResolver contentResolver = this.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(imageUri);

            if (inputStream != null){
                inputStream.close();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
