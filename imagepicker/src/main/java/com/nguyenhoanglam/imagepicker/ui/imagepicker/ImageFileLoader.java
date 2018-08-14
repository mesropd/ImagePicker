package com.nguyenhoanglam.imagepicker.ui.imagepicker;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.nguyenhoanglam.imagepicker.listener.OnImageLoaderListener;
import com.nguyenhoanglam.imagepicker.model.Folder;
import com.nguyenhoanglam.imagepicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hoanglam on 8/17/17.
 */

public class ImageFileLoader {

    private final String[] projection = new String[]{MediaStore.Images.Media._ID
            , MediaStore.Images.Media.DISPLAY_NAME
            , MediaStore.Images.Media.DATA
            , MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

    private Context context;
    private ExecutorService executorService;

    public ImageFileLoader(Context context) {
        this.context = context;
    }

    private static File makeSafeFile(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return new File(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void loadDeviceImages(boolean isFolderMode, boolean isLoadVideos, OnImageLoaderListener listener) {
        getExecutorService().execute(new ImageLoadRunnable(isFolderMode, isLoadVideos, listener));
    }

    public void abortLoadImages() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }

    private class ImageLoadRunnable implements Runnable {
        private boolean isFolderMode;
        private boolean isLoadVideos;
        private OnImageLoaderListener listener;

        public ImageLoadRunnable(boolean isFolderMode, boolean isLoadVideos, OnImageLoaderListener listener) {
            this.isFolderMode = isFolderMode;
            this.isLoadVideos = isLoadVideos;
            this.listener = listener;
        }

        @Override
        public void run() {
            Cursor imagesCursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                    null, null, MediaStore.Images.Media.DATE_ADDED);
            Cursor videosCursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                    null, null, MediaStore.Images.Media.DATE_ADDED);

            if (imagesCursor == null || videosCursor == null) {
                listener.onFailed(new NullPointerException());
                return;
            }

            List<Image> images = new ArrayList<>(imagesCursor.getCount() + videosCursor.getCount());
            Map<String, Folder> folderMap = isFolderMode ? new LinkedHashMap<String, Folder>() : null;

            if (imagesCursor.moveToLast()) {
                do {
                    long id = imagesCursor.getLong(imagesCursor.getColumnIndex(projection[0]));
                    String name = imagesCursor.getString(imagesCursor.getColumnIndex(projection[1]));
                    String path = imagesCursor.getString(imagesCursor.getColumnIndex(projection[2]));
                    String bucket = imagesCursor.getString(imagesCursor.getColumnIndex(projection[3]));

                    File file = makeSafeFile(path);
                    if (file != null && file.exists()) {
                        Image image = new Image(id, name, path, false);
                        images.add(image);

                        if (folderMap != null) {
                            Folder folder = folderMap.get(bucket);
                            if (folder == null) {
                                folder = new Folder(bucket);
                                folderMap.put(bucket, folder);
                            }
                            folder.getImages().add(image);
                        }
                    }
                } while (imagesCursor.moveToPrevious());
            }
            imagesCursor.close();

            if (videosCursor.moveToLast() && isLoadVideos) {
                do {
                    long id = videosCursor.getLong(videosCursor.getColumnIndex(projection[0]));
                    String name = videosCursor.getString(videosCursor.getColumnIndex(projection[1]));
                    String path = videosCursor.getString(videosCursor.getColumnIndex(projection[2]));
                    String bucket = videosCursor.getString(videosCursor.getColumnIndex(projection[3]));

                    File file = makeSafeFile(path);
                    if (file != null && file.exists()) {
                        Image image = new Image(id, name, path, true);
                        images.add(image);

                        if (folderMap != null) {
                            Folder folder = folderMap.get(bucket);
                            if (folder == null) {
                                folder = new Folder(bucket);
                                folderMap.put(bucket, folder);
                            }
                            folder.getImages().add(image);
                        }
                    }
                } while (videosCursor.moveToPrevious());
            }
            videosCursor.close();

            /* Convert HashMap to ArrayList if not null */
            List<Folder> folders = null;
            if (folderMap != null) {
                folders = new ArrayList<>(folderMap.values());
            }

            listener.onImageLoaded(images, folders);
        }
    }
}
