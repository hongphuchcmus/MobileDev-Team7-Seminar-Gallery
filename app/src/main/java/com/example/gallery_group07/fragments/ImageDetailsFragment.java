package com.example.gallery_group07.fragments;


import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.gallery_group07.R;
import com.example.gallery_group07.data.MediaStoreImage;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Image details: Name, Date, Dimension, Size
public class ImageDetailsFragment extends DialogFragment {
    private static final String LOG_TAG = "ImageDetailsFragment";
    Button closeButton;
    TextView titleText, dateText, dimensionsText, sizeText;
    private MediaStoreImage currentImage;

    public ImageDetailsFragment(MediaStoreImage currentImage){
        this.currentImage = currentImage;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_details_fragment, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        closeButton = view.findViewById(R.id.image_details_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        titleText = view.findViewById(R.id.image_details_title_text);
        dateText = view.findViewById(R.id.image_details_date_text);
        dimensionsText = view.findViewById(R.id.image_details_dimensions_text);
        sizeText = view.findViewById(R.id.image_details_size_text);

        titleText.setText(currentImage.displayName);
        dateText.setText(formatDate(currentImage.dateAdded));
        dimensionsText.setText(getImageDimensions(getContext(), currentImage.contentUri));
        sizeText.setText(formatFileSize(getImageSize(getContext(), currentImage.contentUri)));
    }

    public static String formatDate(Date date){
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return formatter.format(date);
    }

    // Calculate image dimensions
    public static String getImageDimensions(Context context, Uri imageUri) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // Only decode dimensions, not the full image

            // Open stream, decode the dimensions, and close the stream
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            int width = options.outWidth;
            int height = options.outHeight;

            return width + " x " + height;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error getting image dimensions: " + e.getMessage());
            return "Unknown";
        }
    }

    // Calculate image size
    public static long getImageSize(Context context, Uri imageUri) {
        String[] projection = { MediaStore.MediaColumns.SIZE }; // Only fetch the SIZE column
        long size = -1;

        try (Cursor cursor = context.getContentResolver().query(
                imageUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                size = cursor.getLong(sizeIndex); // Size in bytes
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error getting image size: " + e.getMessage());
        }

        return size;
    }


    private static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " Bytes";
        } else if (sizeInBytes < (1024 * 1024)) {
            return (sizeInBytes / 1024) + " KB";
        } else {
            return String.format("%.2f MB", (sizeInBytes / (1024.0 * 1024.0)));
        }
    }
}
