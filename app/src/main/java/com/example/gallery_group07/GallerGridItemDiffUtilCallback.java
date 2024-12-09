package com.example.gallery_group07;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class GallerGridItemDiffUtilCallback extends DiffUtil.Callback {
    public final String TAG = "GallerGridItemDiffUtilCallback>>";
    private final List<GalleryGridItem> oldList;
    private final List<GalleryGridItem> newList;

    public GallerGridItemDiffUtilCallback(List<GalleryGridItem> oldList, List<GalleryGridItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        GalleryGridItem oldItem = oldList.get(oldItemPosition);
        GalleryGridItem newItem = newList.get(newItemPosition);

        // If the item type is HEADER, compare based on header content
        if (oldItem.getType() == GalleryGridItem.TYPE_HEADER && newItem.getType() == GalleryGridItem.TYPE_HEADER) {
            return oldItem.getData().equals(newItem.getData());
        }

        // If the item type is IMAGE, compare based on image properties (e.g., ID, path)
        if (oldItem.getType() == GalleryGridItem.TYPE_IMAGE && newItem.getType() == GalleryGridItem.TYPE_IMAGE) {
            MediaStoreImage oldImage = (MediaStoreImage) oldItem.getData();
            MediaStoreImage newImage = (MediaStoreImage) newItem.getData();
            return oldImage.id == newImage.id;
        }

        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        GalleryGridItem oldItem = oldList.get(oldItemPosition);
        GalleryGridItem newItem = newList.get(newItemPosition);

        // Compare contents for both types of items
        if (oldItem.getType() == GalleryGridItem.TYPE_HEADER && newItem.getType() == GalleryGridItem.TYPE_HEADER) {
            return oldItem.getData().equals(newItem.getData());
        }

        if (oldItem.getType() == GalleryGridItem.TYPE_IMAGE && newItem.getType() == GalleryGridItem.TYPE_IMAGE) {
            MediaStoreImage oldImage = (MediaStoreImage) oldItem.getData();
            MediaStoreImage newImage = (MediaStoreImage) newItem.getData();
            return oldImage.equals(newImage);
        }

        return false;
    }

    // For debugging

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Log.i(TAG, "Changes appeared");
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
