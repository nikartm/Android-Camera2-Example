package ru.nikartm.android_camera2;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import ru.nikartm.android_camera2.util.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Utils.checkCameraPermission(MainActivity.this)) {
            initCameraFragment();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initCameraFragment();
                } else if (grantResults.length > 0
                        && grantResults[0] == -1) {
                    // Do something if skipped dialog
                }
                break;
            default:
                break;
        }
    }

    private void initCameraFragment() {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new CameraFragment())
                .commit();
    }
}
