package com.example.gallery_group07.interfaces;

import android.util.Log;

import androidx.fragment.app.DialogFragment;

import com.example.gallery_group07.fragments.OptionMenuDialogFragment;

public interface OptionMenuItem {
    String getOptionName();
    void onThisOptionSelected();

}
