package com.example.gallery_group07;

public class GalleryGridItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_IMAGE = 1;

    private int type;
    private Object data;  // Can be Header or MediaStoreImage

    public GalleryGridItem(int type, Object data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}
