package com.example.gallery_group07.activities;

import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gallery_group07.adapters.GalleryAdapter;
import com.example.gallery_group07.data.GalleryGridItem;
import com.example.gallery_group07.data.MediaStoreImage;
import com.example.gallery_group07.R;
import com.example.gallery_group07.SharedViewModel;
import com.example.gallery_group07.fragments.ImageScrollerFragment;
import com.example.gallery_group07.fragments.OptionMenuDialogFragment;
import com.example.gallery_group07.interfaces.OptionMenuItem;

import java.util.LinkedList;
import java.util.List;

public class ImageGridActivity extends AppCompatActivity {
    public static final String LOG_TAG = "ImageGridActivity";

    private List<MediaStoreImage> imageList;
    private SharedViewModel viewModel;
    private GalleryAdapter adapter;
    private int selectedImageIndex = -1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_favorites) {
            Intent intent = new Intent(this, FavoritesActivity.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.menu_group) {
            OptionMenuItem[] sortOptions = {
                    new GroupOption(GalleryAdapter.GROUP_BY_DAY),
                    new GroupOption(GalleryAdapter.GROUP_BY_MONTH),
                    new GroupOption(GalleryAdapter.GROUP_BY_YEAR),
                    new GroupOption(GalleryAdapter.GROUP_NONE)
            };
            OptionMenuDialogFragment sortOptionMenu = new OptionMenuDialogFragment(this, "Group", sortOptions);
            sortOptionMenu.show(getSupportFragmentManager(), "");
        }
        return super.onOptionsItemSelected(item);
    }

    public String getCollection() {
        return null;
    }

    public final List<MediaStoreImage> getImageList() {
        return imageList;
    }

    protected final void setImageList(List<MediaStoreImage> images) {
        imageList = images;
    }

    public SharedViewModel getViewModel() {
        if (viewModel == null) {
            viewModel = (new ViewModelProvider(this)).get(SharedViewModel.class);
        }
        return viewModel;
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean modified = viewModel.checkForUpdates(this);
        if (modified) {
            Log.i(LOG_TAG, "Image list modified, reloading...");
            reloadImages();
            Log.i(LOG_TAG, "Reloaded");
        }
    }

    public final void onImageSelected(int position) {
        selectedImageIndex = position;
        ImageScrollerFragment fragment = new ImageScrollerFragment(position);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null); // Optional, for back navigation
        transaction.commit();
    }

    ;

    public MediaStoreImage getSelectedImage(){
        if (imageList == null || imageList.isEmpty()) return null;
        return imageList.get(selectedImageIndex);
    }

    public void loadImages() {
        viewModel = (new ViewModelProvider(this)).get(SharedViewModel.class);
        viewModel.loadImages(this, null);
        imageList = viewModel.getImageList();
    }

    ;

    public void reloadImages() {
        imageList = viewModel.getImageList();
        if (imageList == null || imageList.isEmpty()) {
            imageList = new LinkedList<>();
        }
        if (adapter == null) return;
        adapter.update(imageList);
    }

    protected final void showImages(RecyclerView recyclerView, int columnCount) {
        adapter = new GalleryAdapter(this, imageList, null);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(createGridLayoutManager(adapter, columnCount));
    }

    ;

    protected final GridLayoutManager createGridLayoutManager(GalleryAdapter galleryAdapter, int columnCount) {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columnCount);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = galleryAdapter.getItemViewType(position);
                if (viewType == GalleryGridItem.TYPE_HEADER) {
                    return columnCount; // Let a header span a fully row
                } else {
                    return 1;
                }
            }
        });
        return gridLayoutManager;
    }

    public class GroupOption implements OptionMenuItem {
        int groupingMode = 0;

        public GroupOption(int groupingMode) {
            this.groupingMode = groupingMode;
        }

        @Override
        public String getOptionName() {
            switch (groupingMode) {
                case GalleryAdapter.GROUP_BY_DAY:
                    return "Group by Day";
                case GalleryAdapter.GROUP_BY_MONTH:
                    return "Group by Month";
                case GalleryAdapter.GROUP_BY_YEAR:
                    return "Group by Year";
                case GalleryAdapter.GROUP_NONE:
                    return "None";
                default:
                    return "";
            }
        }

        @Override
        public void onThisOptionSelected() {
            adapter.changeGroupingMode(groupingMode);
        }
    }
}
