package com.example.gallery_group07;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

// TODO: use Bill Pugh Singleton pattern
public class ImageManager {
    private static ImageManager instance = null;
    private static List<MediaStoreImage> imgList;
    private final List<ImageListChangeListener> listeners = new LinkedList<>();

    public MediaStoreImage getImageById(long imgId) {
        if (imgList == null) {
            return null;
        }
        for (MediaStoreImage image : imgList) {
            if (image.id == imgId) {
                return image;
            }
        }
        return null;
    }

    public interface ImageListChangeListener {
        void onImageRemoved(MediaStoreImage image);
    }

    private ImageManager(){
        imgList = new LinkedList<MediaStoreImage>();
    }

    public void addImageListChangeListener(ImageListChangeListener listener){
        listeners.add(listener);
    }

    public static synchronized ImageManager getInstance(){
        if (instance == null){
            instance = new ImageManager();
        }
        return instance;
    }

    public List<MediaStoreImage> getImageList(){
        return new LinkedList<>(imgList); // Prevent modifying directly on the manager's image list
    }

    public void setImageList(List<MediaStoreImage> list){
        imgList = new LinkedList<>(list); // Prevent modifying directly on the manager's image list
    }

    public int getImageIndexById(long imgId){
        if (imgList == null){
            return -1;
        }
        // Using iterator is faster for Linked List
        ListIterator<MediaStoreImage> iterator = imgList.listIterator();
        while (iterator.hasNext()) {
            int curIndex = iterator.nextIndex();
            MediaStoreImage curImage = iterator.next();
            if(curImage.id == imgId){
                return curIndex;
            }
        }
        return -1;
    }

    public MediaStoreImage getImage(int index){
        if (imgList == null){
            return null;
        }
        if (index < 0 || index > imgList.size()-1){
            return null;
        }
        return imgList.get(index);
    }

    public int getImageListSize(){
        if (imgList == null){
            return 0;
        }
        return imgList.size(); // size() is O(1) even for linked lists
    }

    public void removeImage(MediaStoreImage image){
        if (imgList == null){
            return;
        }
        imgList.remove(image);
        for (ImageListChangeListener listener : listeners){
            listener.onImageRemoved(image);
        }
    }

    // Helper function
    private static boolean isImageInCollection(Context context, MediaStoreImage image, String collectionName){
        SharedPreferences preferences = context.getSharedPreferences(collectionName, MODE_PRIVATE);
        return preferences.getBoolean(String.valueOf(image.contentUri.getLastPathSegment()), false);
    }

    public List<MediaStoreImage> getImagesInCollection(Context context, String collectionName){
        if (imgList == null){
            return null;
        }
        List<MediaStoreImage> images = new LinkedList<>();
        for (MediaStoreImage image : imgList){
            if (isImageInCollection(context, image, collectionName)){
                images.add(image);
            }
        }
        return images;
    }

    public void removeImageListChangeListener(ImageListChangeListener listener){
        listeners.remove(listener);
    }
}
