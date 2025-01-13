package com.example.gallery_group07.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gallery_group07.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

import androidx.appcompat.app.AlertDialog;
import android.app.ProgressDialog;

public class EditPhotoActivity extends AppCompatActivity {
    private PhotoEditor photoEditor;
    private PhotoEditorView photoEditorView;
    private ProgressDialog progressDialog;
    private Bitmap croppedBitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_edit_photo);

        photoEditorView = findViewById(R.id.photoEditorView);
        photoEditor = new PhotoEditor.Builder(this, photoEditorView).build();

        ImageButton btnSave = findViewById(R.id.btn_save);
        ImageButton btnCrop = findViewById(R.id.btn_crop);
        ImageButton btnRotate = findViewById(R.id.btn_rotate);

        Uri imageUri = getIntent().getParcelableExtra("imageUri");
        if (imageUri == null) {
            Toast.makeText(this, "No image to edit!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        photoEditorView.getSource().setImageURI(imageUri);

        btnSave.setOnClickListener(view -> {
            if (croppedBitmap != null) {
                saveCroppedImage(croppedBitmap);
            } else {
                saveEditedPhoto();
            }
        });

        btnCrop.setOnClickListener(view -> {
            if (imageUri != null) {
                showCropOptionsDialog(imageUri);
            } else {
                Toast.makeText(this, "No image to crop!", Toast.LENGTH_SHORT).show();
            }
        });

        btnRotate.setOnClickListener(view -> {
            photoEditorView.getSource().setRotation(photoEditorView.getSource().getRotation() - 90);
        });
    }

    private void showCropOptionsDialog(Uri imageUri) {
        String[] cropOptions = {"Square", "4:3", "16:9", "3:2", "5:4", "2:1"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Crop Ratio")
                .setItems(cropOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            cropImage(imageUri, 1, 1); // Square
                            break;
                        case 1:
                            cropImage(imageUri, 4, 3); // 4:3
                            break;
                        case 2:
                            cropImage(imageUri, 16, 9); // 16:9
                            break;
                        case 3:
                            cropImage(imageUri, 3, 2); // 3:2
                            break;
                        case 4:
                            cropImage(imageUri, 5, 4); // 5:4
                            break;
                        case 5:
                            cropImage(imageUri, 2, 1); // 2:1
                            break;
                        default:
                            break;
                    }
                })
                .show();
    }

    private void cropImage(Uri imageUri, int widthRatio, int heightRatio) {
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();

            int newWidth = originalWidth;
            int newHeight = originalHeight;

            if (widthRatio == 1 && heightRatio == 1) {
                int size = Math.min(originalWidth, originalHeight);
                newWidth = size;
                newHeight = size;
            } else {
                if (widthRatio > heightRatio) {
                    newWidth = Math.min((int) (originalHeight * widthRatio / (float) heightRatio), originalWidth);
                    newHeight = (int) (newWidth * heightRatio / (float) widthRatio);
                } else {
                    newHeight = Math.min((int) (originalWidth * heightRatio / (float) widthRatio), originalHeight);
                    newWidth = (int) (newHeight * widthRatio / (float) heightRatio);
                }
            }

            int startX = Math.max((originalWidth - newWidth) / 2, 0);
            int startY = Math.max((originalHeight - newHeight) / 2, 0);

            newWidth = Math.min(newWidth, originalWidth - startX);
            newHeight = Math.min(newHeight, originalHeight - startY);

            croppedBitmap = Bitmap.createBitmap(originalBitmap, startX, startY, newWidth, newHeight);

            photoEditorView.getSource().setImageBitmap(croppedBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to crop image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false); // Không cho phép hủy
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void saveCroppedImage(Bitmap croppedBitmap) {
        showLoading("Saving cropped image...");

        new Thread(() -> {
            File picturesDir = new File(getExternalFilesDir(null), "Pictures");
            if (!picturesDir.exists()) {
                picturesDir.mkdirs();
            }

            File outputFile = new File(picturesDir, "cropped_photo_" + System.currentTimeMillis() + ".jpg");

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                addImageToGallery(outputFile.getAbsolutePath());
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "Image cropped and saved successfully!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveEditedPhoto() {
        showLoading("Saving edited photo...");

        File picturesDir = new File(getExternalFilesDir(null), "Pictures");
        if (!picturesDir.exists()) {
            picturesDir.mkdirs();
        }

        File outputFile = new File(picturesDir, "edited_photo_" + System.currentTimeMillis() + ".jpg");
        String filePath = outputFile.getAbsolutePath();

        photoEditor.saveAsFile(filePath, new PhotoEditor.OnSaveListener() {
            @Override
            public void onSuccess(@NonNull String imagePath) {
                addImageToGallery(filePath);
                hideLoading();

                Toast.makeText(EditPhotoActivity.this, "Image saved successfully!", Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent.setData(Uri.fromFile(outputFile));
                setResult(RESULT_OK, resultIntent);
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                hideLoading();
                Toast.makeText(EditPhotoActivity.this, "Failed to save image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addImageToGallery(String filePath) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "cropped_photo_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    File file = new File(filePath);
                    Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File file = new File(filePath);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);
        }
    }
}
