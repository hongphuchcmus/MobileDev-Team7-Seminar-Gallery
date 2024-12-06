package com.example.gallery_group07;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
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

import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;

import android.provider.Settings;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 0x1045;
    private static final String TAG = "MainActivity>>";

    RecyclerView recyclerView;
    //Observer in case of the images inside the storage change
    ContentObserver contentObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Gallery");

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


            GalleryAdapter galleryAdapter = new GalleryAdapter(this, ImageManager.getInstance().getImageList());

            recyclerView = findViewById(R.id.gallery);
            GridLayoutManager gridLayoutManager = getGridLayoutManager(galleryAdapter);
            recyclerView.setLayoutManager(gridLayoutManager);
            recyclerView.setAdapter(galleryAdapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.menu_favorites) {
            Intent favoriteIntent = new Intent(MainActivity.this, FavoritesActivity.class);
            startActivity(favoriteIntent);
            return true;
        } else if(item.getItemId() == R.id.menu_trash) {
//            Intent trashIntent = new Intent(MainActivity.this, TrashActivity.class);
//            startActivity(trashIntent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
                showImages();
            } else {
                // If we weren't granted the permission, check to see if we should show
                // rationale for the permission.
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE);
                // If we should show the rationale for requesting storage permission, then
                // we'll show [ActivityMainBinding.permissionRationaleView] which does this.

                // If `showRationale` is false, this means the user has not only denied
                // the permission, but they've clicked "Don't ask again". In this case
                // we send the user to the settings page for the app so they can grant
                // the permission (Yay!) or uninstall the app.

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

    private void loadImages(){
        List<MediaStoreImage> images = queryImages();
        // Load images into ImageManager
        ImageManager.getInstance().setImageList(images);

        Toast.makeText(this, String.format("Found %d image(s)", images.size()), Toast.LENGTH_LONG).show();

        // This didn't work yet
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

    private List<MediaStoreImage> queryImages() {
        LinkedList<MediaStoreImage> imageList = new LinkedList<MediaStoreImage>();
        //A key concept when working with Android [ContentProvider]s is something called
        //"projections". A projection is the list of columns to request from the provider,
        //and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
        //statement.
        //It's not _required_ to provide a projection. In this case, one could pass `null`
        //in place of `projection` in the call to [ContentResolver.query], but requesting
        //more data than is required has a performance impact.
        //For this sample, we only use a few columns of data, and so we'll request just a
        //subset of columns.
        String[] projection = {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_ADDED
        };

        //The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
        //to omit this by passing `null` in its place, and then all rows will be returned.
        //In this case we're using a selection based on the date the image was taken.
        //Note that we've included a `?` in our selection. This stands in for a variable
        //which will be provided by the next variable.
        String selection = null;//String.format("%s >= ?", MediaStore.Images.ImageColumns.DATE_ADDED);

        //The `selectionArgs` is a list of values that will be filled in for each `?`
        //in the `selection`.
        String[] selectionArgs = null;

        //Sort order to use. This can also be null, which will use the default sort
        //order. For [MediaStore.Images], the default sort order is ascending by date taken.
        String sortOrder = String.format("%s DESC", MediaStore.Images.Media.DATE_ADDED);
        Cursor cursor = getApplication().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
        if (cursor != null) {


            // In order to retrieve the data from the [Cursor] that's returned, we need to
            // find which index matches each column that we're interested in.
            //
            // There are two ways to do this. The first is to use the method
            // [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
            // is useful if the code is programmatically choosing which columns to request,
            // but would like to use a single method to parse them into objects.
            //
            // In our case, since we know exactly which columns we'd like, and we know
            // that they must be included (since they're all supported from API 1), we'll
            // use [Cursor.getColumnIndexOrThrow]. This method will throw an
            // [IllegalArgumentException] if the column named isn't found.
            //
            // In either case, while this method isn't slow, we'll want to cache the results
            // to avoid having to look them up for each row.

            try {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID);
                int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME);
                int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_ADDED);

                Log.i(TAG, String.format("Found %d image(s)", cursor.getCount()));
//                int maxImages = 500;
                while (cursor.moveToNext()){

                    // Here we'll use the column indexes that we found above.
                    long id = cursor.getLong(idColumn);
                    Date dateModified = new Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateAddedColumn)));
                    String displayName = cursor.getString(displayNameColumn);

                    // This is one of the trickiest parts:
                    //
                    // Since we're accessing images (using
                    // [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], we'll use that
                    // as the base URI and append the ID of the image to it.
                    //
                    // This is the exact same way to do it when working with [MediaStore.Video] and
                    // [MediaStore.Audio] as well. Whatever `Media.EXTERNAL_CONTENT_URI` you
                    // query to get the items is the base, and the ID is the document to
                    // request there.

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    // Some images may lie inside of other app folders, which we don't
                    // have access to. Attempting to access them would result in blank ImageViews
                    if (!isImageAccessible(contentUri)){
                        continue;
                    }

                    MediaStoreImage image = new MediaStoreImage(
                            id,
                            displayName,
                            dateModified,
                            contentUri
                    );

                     imageList.add(image);
                }
            } catch (IllegalArgumentException e){
                Log.e(TAG, e.toString());
            }

            cursor.close();
        }
        return imageList;
    }

    // Due to the implementation Scoped Storage from Android 10 (SDK 29) onwards, we don't have
    // access to images stored inside other apps' folders (Android/data directory). But somehow
    // the MediaStore will load their Uris anyway. To prevent this, we have to try opening
    // those files for reading and see if we would get any errors
    private boolean isImageAccessible(Uri imageUri){
        try {
            ContentResolver contentResolver = this.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(imageUri);

            if (inputStream != null){
                inputStream.close();
                return  true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}