package com.nguyenhoanglam.imagepicker.ui.imagepicker;

import android.os.Handler;
import android.os.Looper;

import com.nguyenhoanglam.imagepicker.listener.OnImageLoaderListener;
import com.nguyenhoanglam.imagepicker.model.Folder;
import com.nguyenhoanglam.imagepicker.model.Image;
import com.nguyenhoanglam.imagepicker.ui.common.BasePresenter;

import java.io.File;
import java.util.List;

/**
 * Created by hoanglam on 8/17/17.
 */

public class ImagePickerPresenter extends BasePresenter<ImagePickerView> {

    private ImageFileLoader imageLoader;
    private Handler handler = new Handler(Looper.getMainLooper());

    public ImagePickerPresenter(ImageFileLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    public void abortLoading() {
        imageLoader.abortLoadImages();
    }

    public void loadImages(boolean isFolderMode, boolean isLoadVideos) {
        if (!isViewAttached()) return;

        getView().showLoading(true);
        imageLoader.loadDeviceImages(isFolderMode, isLoadVideos, new OnImageLoaderListener() {
            @Override
            public void onImageLoaded(final List<Image> images, final List<Folder> folders) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isViewAttached()) {
                            getView().showFetchCompleted(images, folders);
                            final boolean isEmpty = folders != null ? folders.isEmpty() : images.isEmpty();
                            if (isEmpty) {
                                getView().showEmpty();
                            } else {
                                getView().showLoading(false);
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailed(final Throwable throwable) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isViewAttached()) {
                            getView().showError(throwable);
                        }
                    }
                });
            }
        });
    }

    public void onDoneSelectImages(List<Image> selectedImages) {
        if (selectedImages != null && !selectedImages.isEmpty()) {
            for (int i = 0; i < selectedImages.size(); i++) {
                Image image = selectedImages.get(i);
                File file = new File(image.getPath());
                if (!file.exists()) {
                    selectedImages.remove(i);
                    i--;
                }
            }
        }
        getView().finishPickImages(selectedImages);
    }
}
