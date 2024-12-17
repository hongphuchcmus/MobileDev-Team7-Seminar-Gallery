package com.example.gallery_group07.data;

import android.net.Uri;

import java.util.Date;

public class MediaStoreImage{
    public long id;
    public String displayName;
    public Date dateAdded;
    public Uri contentUri;

    public MediaStoreImage(long id, String displayName, Date dateAdded, Uri contentUri){
        this.id = id;
        this.displayName = displayName;
        this.dateAdded = dateAdded;
        this.contentUri = contentUri;
    }

}

