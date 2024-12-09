package com.example.gallery_group07;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class ImageDiffUtilCallBack extends DiffUtil.Callback {
    private final List<MediaStoreImage> oldImgList;
    private final List<MediaStoreImage> newImgList;

    public ImageDiffUtilCallBack(List<MediaStoreImage> oldImgList, List<MediaStoreImage> newImgList){
        this.oldImgList = oldImgList;
        this.newImgList = newImgList;
    }

    @Override
    public int getOldListSize() {
        return oldImgList.size();
    }

    @Override
    public int getNewListSize() {
        return newImgList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldImgList.get(oldItemPosition).id == newImgList.get(newItemPosition).id;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldImgList.get(oldItemPosition).equals(newImgList.get(newItemPosition));
    }
}
