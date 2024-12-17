package com.example.gallery_group07.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.gallery_group07.R;
import com.example.gallery_group07.SharedViewModel;
import com.example.gallery_group07.activities.ImageGridActivity;
import com.example.gallery_group07.data.MediaStoreImage;
import com.example.gallery_group07.interfaces.OptionMenuItem;

import java.util.ArrayList;
import java.util.LinkedList;

public class AlbumOptionMenuFragment extends OptionMenuDialogFragment{
    public static final String LOG_TAG = "AlbumOptionMenuFragment";

    public AlbumOptionMenuFragment(AppCompatActivity rootActivity, String optionName, OptionMenuItem[] options){
        super(rootActivity, optionName, options);
    }

    @Override
    public void createOptions() {
        // Retrieve the list of album names from the ViewModel
        SharedViewModel viewModel = getViewModel();
        ArrayList<String> albums = new ArrayList<>(viewModel.getAllAlbums(getRootActivity()));
        optionItems = new ArrayList<>(albums.size() + 1);

        optionItems.add(new AlbumOptionNewAlbum(this));

        for (String albumName : albums){
            optionItems.add(new AlbumOptionAddTo(albumName, this));
        }
    }
    
    public static class AlbumOptionNewAlbum extends DialogFragment implements OptionMenuItem{
        Button okButton;
        EditText editText;
        AlbumOptionMenuFragment menuFragment;

        public AlbumOptionNewAlbum(AlbumOptionMenuFragment menuFragment){
            this.menuFragment = menuFragment;
        }

        public void onSubmit(){
            String albumName = String.valueOf(editText.getText());
            // TODO: Check for valid album name
            // TODO: Check if album already exists
            menuFragment.getViewModel().createNewAlbumIfNotExists(menuFragment.getRootActivity(), albumName);
            Toast.makeText(menuFragment.getRootActivity(), "Created album " + albumName, Toast.LENGTH_SHORT).show();
            menuFragment.getViewModel().addImageToAlbum(menuFragment.getRootActivity(), albumName, menuFragment.getCurrentImage().id);
            dismiss();
        }

        @Override
        public String getOptionName() {
            return "New Album";
        }

        @Override
        public void onThisOptionSelected() {
            show(menuFragment.getRootActivity().getSupportFragmentManager(), "");
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.new_album_fragment, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            okButton = view.findViewById(R.id.ok_button);
            editText = view.findViewById(R.id.album_name_edit_text);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSubmit();
                }
            });
        }

        @Override
        public void onStart() {
            super.onStart();
            // Set custom dimensions for the dialog
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                getDialog().getWindow().setGravity(Gravity.BOTTOM);
                getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

    }

    public static class AlbumOptionAddTo implements OptionMenuItem{
        public final String albumName;
        public final AlbumOptionMenuFragment menuFragment;

        public AlbumOptionAddTo(String albumName, AlbumOptionMenuFragment menuFragment){
            this.albumName = albumName;
            this.menuFragment = menuFragment;
        }

        @Override
        public String getOptionName() {
            return albumName;
        }
        @Override
        public void onThisOptionSelected() {
            SharedViewModel viewModel = menuFragment.getViewModel();
            viewModel.addImageToAlbum(menuFragment.getRootActivity(), albumName, menuFragment.getCurrentImage().id);
            Log.i(LOG_TAG, "Added image to album " + albumName);
            Toast.makeText(menuFragment.getRootActivity(), "Added image to album " + albumName, Toast.LENGTH_SHORT).show();
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

