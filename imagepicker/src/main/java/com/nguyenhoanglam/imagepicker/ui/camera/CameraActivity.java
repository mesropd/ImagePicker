package com.nguyenhoanglam.imagepicker.ui.camera;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.Toast;

import com.nguyenhoanglam.imagepicker.R;
import com.nguyenhoanglam.imagepicker.helper.CameraHelper;
import com.nguyenhoanglam.imagepicker.helper.ImageHelper;
import com.nguyenhoanglam.imagepicker.model.Config;
import com.nguyenhoanglam.imagepicker.model.Image;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.Locale;

/**
 * Created by hoanglam on 8/21/17.
 */

public class CameraActivity extends AppCompatActivity {
    private Config config;
    private String savedImagePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        if(savedInstanceState != null) {
            savedImagePath = savedInstanceState.getString("savedImagePath");
        }
        else if(getIntent() != null) {
            savedImagePath = getIntent().getStringExtra("savedImagePath");
        }

        config = intent.getParcelableExtra(Config.EXTRA_CONFIG);
        if (config.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.imagepicker_activity_camera);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(savedImagePath != null && !savedImagePath.isEmpty()) {
            finish();
            return;
        }
        captureImage();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("savedImagePath", savedImagePath);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        savedImagePath = savedInstanceState.getString("savedImagePath");
    }

    public Intent getCameraIntent() {
        Intent intent;
        if(config.isLoadVideos()) {
            intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        }
        else {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        File imageFile = ImageHelper.createImageFile(config.getSavePath(), config.isLoadVideos());
        if (imageFile != null) {
            Context appContext = getApplicationContext();
            String providerName = String.format(Locale.ENGLISH, "%s%s", appContext.getPackageName(), ".fileprovider");
            Uri uri = FileProvider.getUriForFile(appContext, providerName, imageFile);
            savedImagePath = imageFile.getAbsolutePath();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            ImageHelper.grantAppPermission(this, intent, uri);
            return intent;
        }
        return null;
    }

    private void captureImage() {
        if (!CameraHelper.checkCameraAvailability(this)) {
            finish();
            return;
        }
        Intent cameraIntent = getCameraIntent();
        if (cameraIntent == null) {
            Toast.makeText(this, getString(R.string.imagepicker_error_create_image_file), Toast.LENGTH_LONG).show();
            return;
        }
        startActivityForResult(cameraIntent, Config.RC_CAPTURE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Config.RC_CAPTURE_IMAGE && resultCode == RESULT_OK) {
            finishCaptureImage(savedImagePath);
        }
        else {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    public void finishCaptureImage(String imagePath) {
        final Uri imageUri = Uri.parse(new File(imagePath).toString());
        if (imageUri != null) {
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{imageUri.getPath()}
                    , null
                    , (path, uri) -> {
                        if (path != null) {
                            path = imagePath;
                        }
                        Image image = ImageHelper.getImageFromPath(path, false);
                        EventBus.getDefault().postSticky(image);
                        ImageHelper.revokeAppPermission(this, imageUri);
                    });
        }
    }
}