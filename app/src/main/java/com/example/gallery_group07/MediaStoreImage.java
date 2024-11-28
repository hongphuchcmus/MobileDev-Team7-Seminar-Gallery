package com.example.gallery_group07;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

// TODO: Implement DiffUtil Callback so the changes to images can be reloaded faster
public class MediaStoreImage implements Serializable{
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
