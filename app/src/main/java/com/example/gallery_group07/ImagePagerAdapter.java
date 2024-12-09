package com.example.gallery_group07;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
    public final static String TAG = "ImagePagerAdapter>>";
    private Context context;
    private List<MediaStoreImage> images;

    public ImagePagerAdapter(Context context, List<MediaStoreImage> images) {
        this.context = context;
        this.images = images;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_pager, parent, false);

        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        MediaStoreImage image = images.get(position);
        Glide.with(context)
                .load(image.contentUri)
                .apply(new RequestOptions()
                        .fitCenter()
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .override(Target.SIZE_ORIGINAL))
                .placeholder(R.drawable.image_placeholder)
                .into(holder.imageView);
    }

    public List<MediaStoreImage> getImages() {
        return images;
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_detail_view);
        }
    }

    public void update(List<MediaStoreImage> newImages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ImageDiffUtilCallBack(images, newImages));
        images.clear();
        images.addAll(newImages);
        diffResult.dispatchUpdatesTo(this);
    }
}
