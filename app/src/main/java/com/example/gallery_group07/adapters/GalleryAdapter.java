package com.example.gallery_group07.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gallery_group07.data.GalleryGridItem;
import com.example.gallery_group07.data.MediaStoreImage;
import com.example.gallery_group07.R;
import com.example.gallery_group07.activities.ImageGridActivity;
import com.example.gallery_group07.interfaces.GallerGridItemDiffUtilCallback;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.util.Log;
import android.widget.TextView;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private final List<GalleryGridItem> content;
    private final ImageGridActivity gridActivity;
    private int groupingMode;
    public static final int GROUP_BY_DAY = 0;
    public static final int GROUP_BY_MONTH = 1;
    public static final int GROUP_BY_YEAR = 2;
    public static final int GROUP_NONE = 3;

    public static final String LOG_TAG = "GalleryAdapter";

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View childView;
        View parentView;

        public ViewHolder(@NonNull View rootView, int childViewId) {
            super(rootView);

            this.parentView = rootView;
            this.childView = rootView.findViewById(childViewId);
        }

        public View getChildView() {
            return childView;
        }

        public View getRootView() {
            return parentView;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return content.get(position).getType();
    }

    public GalleryAdapter(ImageGridActivity gridActivity, List<MediaStoreImage> images, String collectionName) {
        this.gridActivity = gridActivity;
        content = createHeaderAndImageItems(images);
    }

    /*
    Assuming that the images were ordered by dates, in descending order (from the newest to the oldest),
    we can group the images by adding the headers at the start of the list and for every position in which
    the date of the images changed
    */
    private List<GalleryGridItem> createHeaderAndImageItems(List<MediaStoreImage> images) {
        List<GalleryGridItem> newContent = new LinkedList<>();
        SimpleDateFormat dateFormat;
        if (!images.isEmpty()) {
            switch (groupingMode) {
                case GROUP_BY_DAY: {
                    dateFormat = new SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault());
                    break;
                }
                case GROUP_BY_MONTH: {
                    dateFormat = new SimpleDateFormat("MMM - yyyy", Locale.getDefault());
                    break;
                }
                case GROUP_BY_YEAR: {
                    dateFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                    break;
                }
                default: {
                    dateFormat = null;
                    break;
                }
            }

            if (dateFormat == null) {
                for (int i = 0; i < images.size(); i++) {
                    newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_IMAGE, images.get(i)));
                }
            } else {
                String lastGroup = dateFormat.format(images.get(0).dateAdded);
                newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_HEADER, lastGroup));
                newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_IMAGE, images.get(0)));

                for (int i = 1; i < images.size(); i++) {
                    String currentGroup = dateFormat.format(images.get(i).dateAdded);
                    if (!currentGroup.equals(lastGroup)) {
                        newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_HEADER, currentGroup));
                        lastGroup = currentGroup;
                    }
                    newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_IMAGE, images.get(i)));
                }
            }

        }
        return newContent;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == GalleryGridItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.image_header_list, parent, false);
            return new ViewHolder(view, R.id.img_header);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_list, parent, false);

            return new ViewHolder(view, R.id.img);
        }
    }

    // Replace the content of each view (invoked by Layout Manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == GalleryGridItem.TYPE_IMAGE) {
            MediaStoreImage mediaStoreImage = (MediaStoreImage) content.get(position).getData();
            holder.getRootView().setTag(mediaStoreImage);

            holder.getRootView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(LOG_TAG, String.format("Viewing %s", mediaStoreImage.displayName));
                    gridActivity.onImageSelected(gridActivity.getImageList().indexOf(mediaStoreImage));
                }
            });
            ImageView imageView = (ShapeableImageView) holder.getChildView();

            Glide.with(holder.getChildView())
                    .load(mediaStoreImage.contentUri)
                    .centerCrop()
//                    .placeholder(R.drawable.image_placeholder)
                    .sizeMultiplier(0.5f)
                    .into(imageView);
        } else {
            TextView textView = (TextView) holder.getChildView();
            textView.setText((String) content.get(position).getData());
        }
    }

    @Override
    public int getItemCount() {
        return content.size();
    }

    public void update(List<MediaStoreImage> images) {
        List<GalleryGridItem> newContent = createHeaderAndImageItems(images);
        Log.i(LOG_TAG, String.format("Old size: %d -> New size: %d", content.size(), newContent.size()));
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new GallerGridItemDiffUtilCallback(content, newContent));
        content.clear();
        content.addAll(newContent);
        diffResult.dispatchUpdatesTo(this);
    }

    public void changeGroupingMode(int newGroupingMode) {
        groupingMode = newGroupingMode;
        update(gridActivity.getImageList());
    }
}

