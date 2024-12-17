package com.example.gallery_group07.interfaces;

import androidx.recyclerview.widget.DiffUtil;

import androidx.recyclerview.widget.DiffUtil;

import com.example.gallery_group07.data.AlbumItem;

import java.util.List;

public class AlbumItemDiffUtilCallback extends DiffUtil.Callback {

    private final List<AlbumItem> oldList;
    private final List<AlbumItem> newList;

    public AlbumItemDiffUtilCallback(List<AlbumItem> oldList, List<AlbumItem> newList) {
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
        // Compare the unique identifiers of the items (e.g., title or Uri) to check if they represent the same item
        return oldList.get(oldItemPosition).title.equals(newList.get(newItemPosition).title);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Compare the content of the items to see if their properties are the same
        AlbumItem oldItem = oldList.get(oldItemPosition);
        AlbumItem newItem = newList.get(newItemPosition);

        return oldItem.title.equals(newItem.title) &&
                oldItem.coverImageUri.equals(newItem.coverImageUri) &&
                oldItem.count == newItem.count;
    }
}

