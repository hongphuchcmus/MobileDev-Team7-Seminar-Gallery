package com.example.gallery_group07.data;

import android.net.Uri;

public class AlbumItem {
    public final String title;
    public final Uri coverImageUri;
    public final int count;

    public AlbumItem(String title, Uri coverImageUri, int count) {
        this.title = title;
        this.coverImageUri = coverImageUri;
        this.count = count;
    }
}
