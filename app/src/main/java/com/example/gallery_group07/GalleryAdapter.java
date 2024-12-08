package com.example.gallery_group07;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
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
    public static final int IMAGE = 0;
    public static final int HEADER = 1;

    private List<Object> content;
    private Context context;

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
        if (content.get(position) instanceof String){
            return HEADER;
        } else {
            return IMAGE;
        }
    }

    public GalleryAdapter(Context context, List<MediaStoreImage> images) {
        this.context = context;
        content = new LinkedList<Object>();
        // Assuming that the images were ordered by dates, in descending order (from the newest to the oldest),
        // we can group the images by adding the headers at the start of the list and for every position in which
        // the date of the images changed
        if (!images.isEmpty()) {
            // E.g: 20 - Nov - 2024
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd - MMM - yyyy", Locale.getDefault());
            String lastDate = dateFormat.format(images.get(0).dateAdded);
            content.add(lastDate);
            content.add(images.get(0));

            for (int i = 1; i < images.size(); i++) {
                String currentImgDate = dateFormat.format(images.get(i).dateAdded);
                if (!currentImgDate.equals(lastDate)){
                    content.add(currentImgDate);
                    lastDate = currentImgDate;
                }
                content.add(images.get(i));
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == HEADER){
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
        if (getItemViewType(position) == IMAGE){
            MediaStoreImage mediaStoreImage = (MediaStoreImage) content.get(position);
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
            textView.setText((String) content.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return content.size();
    }
}

