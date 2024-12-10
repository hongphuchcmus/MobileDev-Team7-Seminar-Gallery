package com.example.gallery_group07;

import android.net.Uri;

import java.util.Date;

public class MediaStoreImage{
    long id;
    String displayName;
    Date dateAdded;
    Uri contentUri;

    public MediaStoreImage(long id, String displayName, Date dateAdded, Uri contentUri){
        this.id = id;
        this.displayName = displayName;
        this.dateAdded = dateAdded;
        this.contentUri = contentUri;
    }

}

