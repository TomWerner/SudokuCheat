package com.wernerapps.sudokucheater;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import net.pikanji.camerapreviewsample.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraPreviewActivity extends Activity {
    private CameraPreviewView mPreview;
    private RelativeLayout mLayout;
    static final int REQUEST_TAKE_PHOTO = 1;
    private String mCurrentPhotoPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide status-bar
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Hide title-bar, must be before setContentView
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.camera_layout);
    }



    @Override
    protected void onResume() {
        super.onResume();
        // Set the second argument by your choice.
        // Usually, 0 for back-facing camera, 1 for front-facing camera.
        // If the OS is pre-gingerbreak, this does not have any effect.
        mPreview = new CameraPreviewView(this, 0, CameraPreviewView.LayoutMode.FitToParent);
        LayoutParams previewLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        // Un-comment below lines to specify the size.
        //previewLayoutParams.height = 500;
        //previewLayoutParams.width = 500;

        // Un-comment below line to specify the position.
        //mPreview.setCenterPosition(270, 130);

        mLayout = (RelativeLayout) findViewById(R.id.cameraLayout);
        mLayout.addView(mPreview, 0, previewLayoutParams);
        mLayout.addView(new SquareView(this), 1, previewLayoutParams);


        Button takePicture = (Button) findViewById(R.id.takePicture);
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.mCamera.takePicture(null, null, jpegCallback);
            }
        });
    }

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            FileOutputStream outStream = null;

            try {
                String filename = getImageFilename();
                outStream = new FileOutputStream(filename);
                outStream.write(data);
                outStream.close();
                Log.d("CPA", "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                camera.startPreview();
                Toast.makeText(getApplicationContext(), "Image snapshot Done", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, );)
            }
            Log.d("CPA", "onPictureTaken - jpeg");
        }
    };

    private String getImageFilename() {
        return getExternalFilesDir(null).getAbsolutePath() + File.separator + "sudoku.jpg";
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
        mLayout.removeView(mPreview); // This is necessary.
        mPreview = null;
    }
}
