package com.example.gallery_group07;

import androidx.recyclerview.widget.RecyclerView;
import com.example.gallery_group07.MediaStoreImage;

import java.util.LinkedList;
import java.util.List;

public class ImageManager {
    private static ImageManager instance = null;
    private static List<MediaStoreImage> imgList;
    private static List<MediaStoreImage> pendingDeleteImages;

    private ImageManager(){
        imgList = new LinkedList<MediaStoreImage>();
    }

    public static synchronized ImageManager getInstance(){
        if (instance == null){
            instance = new ImageManager();
        }
        return instance;
    }

    public List<MediaStoreImage> getImageList(){
        return imgList;
    }

    public void setImageList(List<MediaStoreImage> list){
        imgList = list;
    }

    public void addToImageList(MediaStoreImage image){
        imgList.add(image);
    }

    public MediaStoreImage getImageById(long imgId){
        for (int i = 0; i < imgList.size(); i++){
            if (imgList.get(i).id == imgId){
                return imgList.get(i);
            }
        }
        return null;
    }

    public int getImageIndexById(long imgId){
        for (int i = 0; i < imgList.size(); i++){
            if (imgList.get(i).id == imgId){
                return i;
            }
        }
        return -1;
    }

    public MediaStoreImage getImage(int index){
        if (index < 0 || index > imgList.size()-1){
            return null;
        }
        return imgList.get(index);
    }

    public int getImageListSize(){
        if (imgList == null){
            return 0;
        }
        return imgList.size();
    }

    public void addPendingDeleteImage(MediaStoreImage image){
        // There is no duplicate check yet
        pendingDeleteImages.add(image);
    }

    public List<MediaStoreImage> getPendingDeleteImages(){
        return pendingDeleteImages;
    }

}
