package com.example.gallery_group07.adapters;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gallery_group07.R;
import com.example.gallery_group07.activities.AlbumActivity;
import com.example.gallery_group07.activities.AlbumsActivity;
import com.example.gallery_group07.data.AlbumItem;
import com.example.gallery_group07.data.GalleryGridItem;
import com.example.gallery_group07.data.MediaStoreImage;
import com.example.gallery_group07.interfaces.AlbumItemDiffUtilCallback;
import com.example.gallery_group07.interfaces.GallerGridItemDiffUtilCallback;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {
    List<AlbumItem> albumList;
    AlbumsActivity albumsActivity;
    public final static String LOG_TAG = "AlbumAdapter";

    public AlbumAdapter(AlbumsActivity albumsActivity, List<AlbumItem> albumList) {
        this.albumsActivity = albumsActivity;
        this.albumList = albumList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_list, parent, false);
        return new ViewHolder(view, R.id.album_cover_image, R.id.album_title_text);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlbumItem albumItem = albumList.get(position);

        Glide.with(holder.getChildImageView())
                .load(albumItem.coverImageUri)
                .centerCrop()
//                .placeholder(R.drawable.image_placeholder)
                .sizeMultiplier(0.5f)
                .into(holder.getChildImageView());
        holder.getChildTextView().setText(String.format("%s (%d)", albumItem.title, albumItem.count));

        holder.getRootView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), AlbumActivity.class);
                intent.putExtra("albumName", albumItem.title);
                view.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView childImageView;
        TextView childTextView;
        View parentView;

        public ViewHolder(@NonNull View rootView, int childImageViewId, int childTextViewId) {
            super(rootView);

            this.parentView = rootView;
            this.childImageView = rootView.findViewById(childImageViewId);
            this.childTextView = rootView.findViewById(childTextViewId);
        }

        public TextView getChildTextView() {
            return childTextView;
        }

        public ShapeableImageView getChildImageView() {
            return childImageView;
        }

        public View getRootView() {
            return parentView;
        }

    }

    public void update(List<AlbumItem> albums) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AlbumItemDiffUtilCallback(albumList, albums));
        albumList.clear();
        albumList.addAll(albums);
        diffResult.dispatchUpdatesTo(this);
    }
}
