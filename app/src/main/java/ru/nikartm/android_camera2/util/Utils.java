package ru.nikartm.android_camera2.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Display;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivan Vodyasov on 02.09.2017.
 */

public class Utils {

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 111;

    public static Map<String, Float> getDisplayDpi(Activity activity) {
        Map<String, Float> displayDpi = new HashMap<>();
        Point size = getDisplaySize(activity);
        float density = activity.getResources().getDisplayMetrics().density;

        float width = size.x / density;
        float height = size.y / density;
        displayDpi.put("width", width);
        displayDpi.put("height", height);
        return displayDpi;
    }

    public static Point getDisplaySize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    /**
     * Checking permissions
     * @param activity activity context
     * @return boolean permission status, true - if permission granted
     */
    public static boolean checkCameraPermission(Activity activity) {
        boolean isGranted = false;
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isGranted = true;
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        return isGranted;
    }

}
