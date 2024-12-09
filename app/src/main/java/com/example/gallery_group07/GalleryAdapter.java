package com.example.gallery_group07;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.helper.widget.Grid;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.util.Log;
import android.widget.TextView;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private final List<GalleryGridItem> content;
    private final Context context;

    public static final String TAG = "GalleryAdapter>>";

    public static class ViewHolder extends RecyclerView.ViewHolder{
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

    public GalleryAdapter(Context context, List<MediaStoreImage> images) {
        this.context = context;
        content = createHeaderAndImageItems(images);
    }

     /*
     Assuming that the images were ordered by dates, in descending order (from the newest to the oldest),
     we can group the images by adding the headers at the start of the list and for every position in which
     the date of the images changed
     */
    private List<GalleryGridItem> createHeaderAndImageItems(List<MediaStoreImage> images){
        List<GalleryGridItem> newContent = new LinkedList<>();
        if (!images.isEmpty()) {
            // E.g: 20 - Nov - 2024
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault());
            String lastDate = dateFormat.format(images.get(0).dateAdded);
            newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_HEADER, lastDate));
            newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_IMAGE, images.get(0)));

            for (int i = 1; i < images.size(); i++) {
                String imgDate = dateFormat.format(images.get(i).dateAdded);
                if (!imgDate.equals(lastDate)){
                    newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_HEADER, imgDate));
                    lastDate = imgDate;
                }
                newContent.add(new GalleryGridItem(GalleryGridItem.TYPE_IMAGE,  images.get(i)));
            }
        }
        return newContent;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == GalleryGridItem.TYPE_HEADER){
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
        if (getItemViewType(position) == GalleryGridItem.TYPE_IMAGE){
            MediaStoreImage mediaStoreImage = (MediaStoreImage) content.get(position).getData();
            holder.getRootView().setTag(mediaStoreImage);

            holder.getRootView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.i(TAG, String.format("Viewing %s", mediaStoreImage.displayName));

                        Intent intent = new Intent(context, ImageDetailActivity.class);
                        Bundle bundle = new Bundle();

                        bundle.putLong("imgId", mediaStoreImage.id);

                        intent.putExtras(bundle);

                        context.startActivity(intent);
                    }
                });
            ImageView imageView = (ShapeableImageView) holder.getChildView();

            Glide.with(holder.getChildView())
                    .load(mediaStoreImage.contentUri)
                    .centerCrop()
                    .placeholder(R.drawable.image_placeholder)
                    .sizeMultiplier(0.5f)
                    .into(imageView);
        } else{
            TextView textView = (TextView) holder.getChildView();
            textView.setText((String) content.get(position).getData());
        }
    }

    @Override
    public int getItemCount() {
        return content.size();
    }

    public void update(List<MediaStoreImage> images){
        List<GalleryGridItem> newContent = createHeaderAndImageItems(images);
        Log.i(TAG, String.format("Old size: %d -> New size: %d", content.size(), newContent.size()));
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new GallerGridItemDiffUtilCallback(content, newContent));
        content.clear();
        content.addAll(newContent);
        diffResult.dispatchUpdatesTo(this);
    }

}

