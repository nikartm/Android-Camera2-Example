package ru.nikartm.android_camera2;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import ru.nikartm.android_camera2.util.Camera2Helper;


public class CameraFragment extends Fragment {

    private Camera2Helper camHelper;
    private TextureView textureView;
    private Button btnCapture;

    public CameraFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        textureView = (TextureView) view.findViewById(R.id.texture);
        btnCapture = (Button) view.findViewById(R.id.btn_capture);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        camHelper = new Camera2Helper(getActivity(), textureView);
        camHelper.openCamera();

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camHelper.captureStillPicture();
                getActivity().finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        camHelper.openCamera();
    }

    @Override
    public void onStop() {
        super.onStop();
        camHelper.closeCamera();
    }
}
