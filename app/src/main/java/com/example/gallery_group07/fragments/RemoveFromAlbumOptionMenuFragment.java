package com.example.gallery_group07.fragments;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gallery_group07.SharedViewModel;
import com.example.gallery_group07.activities.ImageGridActivity;
import com.example.gallery_group07.data.MediaStoreImage;
import com.example.gallery_group07.interfaces.OptionMenuItem;

import java.util.ArrayList;

public class RemoveFromAlbumOptionMenuFragment extends OptionMenuDialogFragment{
    public static final String LOG_TAG = "AlbumOptionMenuFragment";

    public RemoveFromAlbumOptionMenuFragment(AppCompatActivity rootActivity, String optionName, OptionMenuItem[] options){
        super(rootActivity, optionName, options);
    }

    @Override
    public void createOptions() {
        // Retrieve the list of album names from the ViewModel
        SharedViewModel viewModel = getViewModel();
        ArrayList<String> albums = new ArrayList<>(viewModel.getAlbumsContainingImage(getRootActivity(), getCurrentImage().id));
        optionItems = new ArrayList<>(albums.size() + 1);

        optionItems.add(new AlbumOptionRemoveAll(this));

        for (String albumName : albums){
            optionItems.add(new AlbumOptionRemove(albumName, this));
        }
    }

    public static class AlbumOptionRemove implements OptionMenuItem{
        public final String albumName;
        public final RemoveFromAlbumOptionMenuFragment menuFragment;

        public AlbumOptionRemove(String albumName, RemoveFromAlbumOptionMenuFragment menuFragment){
            this.albumName = albumName;
            this.menuFragment = menuFragment;
        }

        @Override
        public String getOptionName() {
            return albumName;
        }
        @Override
        public void onThisOptionSelected() {
            menuFragment.getViewModel().removeImageFromAlbum(menuFragment.getRootActivity(), albumName, menuFragment.getCurrentImage().id);
            Log.i(LOG_TAG, "Removed image from album " + albumName);
            Toast.makeText(menuFragment.getRootActivity(), "Removed image from album " + albumName, Toast.LENGTH_SHORT).show();
            // TODO: Remove image from scroller and grid
        }

    }

    public static class AlbumOptionRemoveAll implements OptionMenuItem{
        public final RemoveFromAlbumOptionMenuFragment menuFragment;

        public AlbumOptionRemoveAll(RemoveFromAlbumOptionMenuFragment menuFragment){
            this.menuFragment = menuFragment;
        }

        @Override
        public String getOptionName() {
            return "Remove from all albums";
        }

        @Override
        public void onThisOptionSelected() {
            ArrayList<String> albums = menuFragment.getViewModel().getAlbumsContainingImage(menuFragment.getRootActivity(), menuFragment.getCurrentImage().id);
            if (albums == null) return;
            if (albums.isEmpty()){
                Toast.makeText(menuFragment.getRootActivity(), "Image is not in any albums", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String albumName : albums){
                menuFragment.getViewModel().removeImageFromAlbum(menuFragment.getRootActivity(), albumName, menuFragment.getCurrentImage().id);
                Log.i(LOG_TAG, "Removed image from album " + albumName);
            }
            Toast.makeText(menuFragment.getRootActivity(), "Removed image from all albums", Toast.LENGTH_SHORT).show();
        }

    }

    public MediaStoreImage getCurrentImage() {
        ImageGridActivity gridActivity = (ImageGridActivity) getRootActivity();
        if (gridActivity != null){
            return gridActivity.getSelectedImage();
        }
        return null;
    }
}

