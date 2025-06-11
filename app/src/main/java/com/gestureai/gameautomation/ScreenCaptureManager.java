
package com.gestureai.gameautomation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScreenCaptureManager {
    private static final int SCREEN_CAPTURE_REQUEST_CODE = 1000;
    private android.media.projection.MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private Context context;

    public ScreenCaptureManager(Context context) {
        this.context = context;
        this.projectionManager = (android.media.projection.MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public Intent createScreenCaptureIntent() {
        return projectionManager.createScreenCaptureIntent();
    }

    public void handleScreenCaptureResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        }
    }

    public MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    public boolean hasPermission() {
        return mediaProjection != null;
    }

    public void stopProjection() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
