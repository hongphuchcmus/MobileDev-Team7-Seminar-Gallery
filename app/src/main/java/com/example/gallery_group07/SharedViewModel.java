package com.example.gallery_group07;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.IntentSenderRequest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gallery_group07.constants.SharedPreferencesConstants;
import com.example.gallery_group07.data.AlbumItem;
import com.example.gallery_group07.data.MediaStoreImage;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SharedViewModel extends ViewModel {
    private static final String LOG_TAG = "SharedViewModel";
    private final MutableLiveData<List<MediaStoreImage>> imageList = new MutableLiveData<>();

    public final void setImageList(List<MediaStoreImage> data) {
        imageList.setValue(data);
    }

    public void removeFromImageList(MediaStoreImage image) {
        if (!imageList.isInitialized()) return;
        LinkedList<MediaStoreImage> images = new LinkedList<>(imageList.getValue());
        images.remove(image);
        imageList.setValue(images);
    }

    public void removeFromImageListByIds(List<Long> ids) {
        if (!imageList.isInitialized()) return;
        LinkedList<MediaStoreImage> images = new LinkedList<>(imageList.getValue());
        images.removeIf(image -> ids.contains(image.id));
        imageList.setValue(images);
    }

    public final List<MediaStoreImage> getImageList() {
        if (!imageList.isInitialized()) return null;
        return new LinkedList<>(imageList.getValue());
    }

    public void loadImages(AppCompatActivity activity, String albumName) {
        imageList.setValue(queryImages(activity, albumName));
    }

    public IntentSenderRequest performDeleteImage(Activity activity, MediaStoreImage image) {
        if (image == null) {
            Log.e(LOG_TAG, "Image is null");
            return null;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.e(LOG_TAG, "performDeleteImage() can only be used for [Build.VERSION_CODES.R] and above (Android 11+)");
            return null;
        }
        Uri[] uris = {image.contentUri};
        PendingIntent pendingIntent = MediaStore.createDeleteRequest(activity.getContentResolver(), Arrays.asList(uris));
        return (new IntentSenderRequest.Builder(pendingIntent.getIntentSender())).build();
    }

    public void performDeleteImagePostRequest(Context context, MediaStoreImage image) {
        if (!imageList.isInitialized()) return;

        // Create a mutable copy of the current image list
        LinkedList<MediaStoreImage> images = new LinkedList<>(imageList.getValue());

        // Remove the image from the list
        if (images.remove(image)) {
            // Update the ViewModel with the modified image list
            imageList.setValue(images);
        } else {
            Log.w(LOG_TAG, "Image not found in current image list.");
        }
    }

    public void removeFromSavedImageIds(Context context, long imageId) {
        // Update the deleted images in SharedPreferences
        SharedPreferences savedImageIdsPrefs = context.getSharedPreferences(SharedPreferencesConstants.SAVED_IMAGE_IDS, Context.MODE_PRIVATE);
        Set<String> savedIds = new HashSet<>(savedImageIdsPrefs.getStringSet(SharedPreferencesConstants.SAVED_IMAGE_IDS_SET, new HashSet<>()));

        // Remove the deleted image ID from SharedPreferences
        savedIds.remove(String.valueOf(imageId));
        SharedPreferences.Editor editor = savedImageIdsPrefs.edit();
        editor.putStringSet(SharedPreferencesConstants.SAVED_IMAGE_IDS_SET, savedIds);
        editor.apply();

        Log.i(LOG_TAG, "Deleted image ID: " + imageId + " from saved image ids.");
    }


    public void performDeleteImageLegacy(Activity activity, MediaStoreImage image) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
            return;
        }

        String deleteFilePath = "";

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
        };
        String selection = MediaStore.Images.Media._ID + " = ?";
        String[] selectionArgs = {String.valueOf(image.id)};

        Cursor cursor = activity.getApplication().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null) {
            int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            if (dataColumnIndex >= 0 && cursor.moveToNext()) {
                deleteFilePath = cursor.getString(dataColumnIndex);
                Log.i(LOG_TAG, String.format("Trying to delete physical copy of image with path %s", deleteFilePath));
            }
            cursor.close();
        }

        // WRITE_EXTERNAL_STORAGE & Legacy Storage required
        File imgFile = new File(deleteFilePath);
        if (imgFile.exists()) {
            boolean success = imgFile.delete();
            if (success) {
                Log.i(LOG_TAG, String.format("Successfully deleted physical copy of image with path %s", deleteFilePath));
                removeFromSavedImageIds(activity, image.id);
            } else {
                Log.e(LOG_TAG, String.format("Failed to delete physical copy of image with path %s", deleteFilePath));
            }
        } else {
            Log.e(LOG_TAG, String.format("Image with path %s doesn't exist", deleteFilePath));
        }
    }

    private List<MediaStoreImage> queryImages(AppCompatActivity activity, String albumName) {
        LinkedList<MediaStoreImage> imageList = new LinkedList<MediaStoreImage>();

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
        };

        String selection = null;//String.format("%s >= ?", MediaStore.Images.ImageColumns.DATE_ADDED);
        String[] selectionArgs = null;

        String sortOrder = String.format("%s DESC", MediaStore.Images.Media.DATE_ADDED);
        Cursor cursor = activity.getApplication().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
        if (cursor != null) {
            try {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

                Log.i(LOG_TAG, String.format("Found %d image(s)", cursor.getCount()));
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);

                    if (albumName != null && !isImageInAlbum(activity, albumName, id)) {
                        continue;
                    }

                    Date dateModified = new Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateAddedColumn)));
                    String displayName = cursor.getString(displayNameColumn);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    if (!isImageAccessible(activity, contentUri)) {
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
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, e.toString());
            }

            cursor.close();
        }
        return imageList;
    }

    private boolean isImageAccessible(AppCompatActivity activity, Uri imageUri) {
        try {
            ContentResolver contentResolver = activity.getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(imageUri);

            if (inputStream != null) {
                inputStream.close();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

/*
    public boolean isImageInCollection(Activity activity, long imageId, String collectionName){
        SharedPreferences sharedPreferences = activity.getSharedPreferences(collectionName, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(String.valueOf(imageId), false);
    };
*/

    /*public void removeImageFromCollection(Activity activity, long imageId, String collectionName){
        SharedPreferences sharedPreferences = activity.getSharedPreferences(collectionName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(String.valueOf(imageId));
        editor.apply();
    }*/

    /*public void addImageToCollection(Activity activity, long imageId, String collectionName){
        SharedPreferences sharedPreferences = activity.getSharedPreferences(collectionName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(String.valueOf(imageId), true);
        editor.apply();
    }*/

    public void addImageToAlbum(Context context, String albumName, long imageId) {
        SharedPreferences albumPrefs = context.getSharedPreferences(albumName, Context.MODE_PRIVATE);
        Set<String> imageSet = new HashSet<>(albumPrefs.getStringSet("images", new HashSet<>()));

        // Add the new image path
        imageSet.add(String.valueOf(imageId));

        // Save the updated set back to SharedPreferences
        SharedPreferences.Editor editor = albumPrefs.edit();
        editor.putStringSet("images", imageSet);
        editor.apply();
    }

    public void removeImageFromAlbum(Context context, String albumName, long imageId) {
        SharedPreferences albumPrefs = context.getSharedPreferences(albumName, Context.MODE_PRIVATE);
        Set<String> imageSet = new HashSet<>(albumPrefs.getStringSet("images", new HashSet<>()));

        // Remove the image path
        if (imageSet.remove(String.valueOf(imageId))) {
            SharedPreferences.Editor editor = albumPrefs.edit();
            editor.putStringSet("images", imageSet);
            editor.apply();
        }
    }

    public Set<String> getAllAlbums(Context context) {
        SharedPreferences albumPrefs = context.getSharedPreferences("albums", Context.MODE_PRIVATE);
        return albumPrefs.getStringSet("albums", new HashSet<>());
    }

    public void deleteAlbum(Context context, String albumName) {
        // Step 1: Remove the album name from the global album list
        SharedPreferences albumPrefs = context.getSharedPreferences("albums", Context.MODE_PRIVATE);
        Set<String> albumSet = new HashSet<>(albumPrefs.getStringSet("albums", new HashSet<>()));

        if (albumSet.remove(albumName)) {
            SharedPreferences.Editor editor = albumPrefs.edit();
            editor.putStringSet("albums", albumSet);
            editor.apply();
        }

        // Step 2: Clear and delete the associated album file
        SharedPreferences albumFile = context.getSharedPreferences(albumName, Context.MODE_PRIVATE);
        SharedPreferences.Editor albumEditor = albumFile.edit();
        albumEditor.clear();
        albumEditor.apply();

        // Optionally delete the file entirely (SharedPreferences files are lightweight, so clearing is usually sufficient)
        context.getSharedPreferences(albumName, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public void createNewAlbumIfNotExists(Context context, String albumName) {
        // Step 1: Access the global album list
        SharedPreferences albumPrefs = context.getSharedPreferences("albums", Context.MODE_PRIVATE);
        Set<String> albumSet = new HashSet<>(albumPrefs.getStringSet("albums", new HashSet<>()));

        // Step 2: Check if the album already exists
        if (!albumSet.contains(albumName)) {
            // Add the album to the global list
            albumSet.add(albumName);
            SharedPreferences.Editor editor = albumPrefs.edit();
            editor.putStringSet("albums", albumSet);
            editor.apply();

            // Step 3: Initialize an empty SharedPreferences file for the new album
            SharedPreferences newAlbumPrefs = context.getSharedPreferences(albumName, Context.MODE_PRIVATE);
            newAlbumPrefs.edit().apply(); // No initial data to add; create the file
        }
    }

    public boolean isImageInAlbum(Context context, String albumName, long imageId) {
        // Access the SharedPreferences for the specified album
        SharedPreferences albumPrefs = context.getSharedPreferences(albumName, Context.MODE_PRIVATE);

        // Retrieve the set of images, defaulting to an empty set if none exist
        Set<String> imageSet = albumPrefs.getStringSet("images", new HashSet<>());

        // Check if the image exists in the set
        return imageSet.contains(String.valueOf(imageId));
    }

    public boolean existsAlbum(Context context, String albumName) {
        SharedPreferences albumPrefs = context.getSharedPreferences("albums", Context.MODE_PRIVATE);
        Set<String> albumSet = albumPrefs.getStringSet("albums", new HashSet<>());
        return albumSet.contains(albumName);
    }

    public Map<String, Integer> getAllAlbumsWithImageCounts(Context context) {
        Map<String, Integer> albumImageCounts = new HashMap<>();

        // Access the global album list
        SharedPreferences albumPrefs = context.getSharedPreferences("albums", Context.MODE_PRIVATE);
        Set<String> albumSet = albumPrefs.getStringSet("albums", new HashSet<>());

        // Iterate through each album and retrieve the image count
        for (String albumName : albumSet) {
            SharedPreferences albumDataPrefs = context.getSharedPreferences(albumName, Context.MODE_PRIVATE);
            Set<String> imageSet = albumDataPrefs.getStringSet("images", new HashSet<>());

            // Add album name and image count to the map
            albumImageCounts.put(albumName, imageSet.size());
        }


        return albumImageCounts;
    }

    // This should only run once and on start
    public void addAllToSavedImageIds(Context context) {
        if (!imageList.isInitialized()) return;
        long[] ids = new long[imageList.getValue().size()];
        ListIterator<MediaStoreImage> iterator = imageList.getValue().listIterator();
        while (iterator.hasNext()) {
            ids[iterator.nextIndex()] = iterator.next().id;
        }

        SharedPreferences savedImageIdsPrefs = context.getSharedPreferences(SharedPreferencesConstants.SAVED_IMAGE_IDS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = savedImageIdsPrefs.edit();
        editor.clear(); // Fresh start
        Set<String> savedIds = new HashSet<>();
        for (long id : ids) {
            savedIds.add(String.valueOf(id));
        }
        editor.putStringSet(SharedPreferencesConstants.SAVED_IMAGE_IDS_SET, savedIds);
        editor.apply();
    }

    // Check if savedImageIds was modified
    public boolean checkForUpdates(Context context) {
        if (!imageList.isInitialized()) return false;

        // Convert current image IDs to a Set<String>
        Set<String> currentIds = new HashSet<>();
        for (MediaStoreImage image : imageList.getValue()) {
            currentIds.add(String.valueOf(image.id));
        }

        // Retrieve saved image IDs from SharedPreferences
        SharedPreferences savedImageIdsPrefs = context.getSharedPreferences(SharedPreferencesConstants.SAVED_IMAGE_IDS, Context.MODE_PRIVATE);
        Set<String> savedIds = savedImageIdsPrefs.getStringSet(SharedPreferencesConstants.SAVED_IMAGE_IDS_SET, new HashSet<>());

        // Check for missing IDs in the current subset
        Set<String> missingIds = new HashSet<>(currentIds);
        missingIds.removeAll(savedIds); // These IDs exist in current but are not saved

        // Remove only the missing IDs
        boolean modified = false;
        if (!missingIds.isEmpty()) {
            List<Long> idsToRemove = new LinkedList<>();
            for (String id : missingIds) {
                idsToRemove.add(Long.parseLong(id));
            }
            removeFromImageListByIds(idsToRemove);
            Log.i(LOG_TAG, String.format("Removed %d missing image(s) from list", missingIds.size()));
            modified = true;
        }

        return modified;
    }

    public ArrayList<String> getAlbumsContainingImage(Context context, long imageId) {
        Set<String> availableAlbums = getAllAlbums(context);
        ArrayList<String> albums = new ArrayList<>(availableAlbums.size());
        for (String album : availableAlbums) {
            if (isImageInAlbum(context, album, imageId)) {
                albums.add(album);
            }
        }
        return albums;
    }

    public Uri getCoverImageUri(Context context, String albumName) {
        SharedPreferences albumPrefs = context.getSharedPreferences(albumName, Context.MODE_PRIVATE);
        Set<String> imageSet = albumPrefs.getStringSet("images", new HashSet<>());
        if (imageSet.isEmpty()) return null;
        long coverId = Long.parseLong(imageSet.iterator().next());
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, coverId);
    }

    ;

    public int getAlbumImageCount(Context context, String albumName) {
        SharedPreferences albumPrefs = context.getSharedPreferences(albumName, Context.MODE_PRIVATE);
        Set<String> imageSet = albumPrefs.getStringSet("images", new HashSet<>());
        return imageSet.size();
    }

    public List<AlbumItem> loadAlbums(Context context) {
        Set<String> availableAlbums = getAllAlbums(context);
        ArrayList<AlbumItem> albumList = new ArrayList<>(availableAlbums.size());
        Log.i(LOG_TAG, "Found " + availableAlbums.size() + " albums");
        for (String album : availableAlbums) {
            albumList.add(new AlbumItem(
                    album,
                    getCoverImageUri(context, album),
                    getAlbumImageCount(context, album)
            ));
        }
        Log.i(LOG_TAG, "Loaded " + albumList.size() + " albums");
        return  albumList;
    }

    ;
}
