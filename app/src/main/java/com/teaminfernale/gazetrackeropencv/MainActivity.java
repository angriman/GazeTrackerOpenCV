package com.teaminfernale.gazetrackeropencv;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GazeTracker::MainActivity";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    replaceImage();
                } break;

                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.textView)).setText(getMessage());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    private void replaceImage() {
        Bitmap bitmap = BitmapFactory.decodeResource(getBaseContext().getResources(), R.drawable.leovetto);
        // first convert bitmap into OpenCV mat object
        Mat imageMat = new Mat (bitmap.getHeight(), bitmap.getWidth(),
                CvType.CV_8U, new Scalar(4));
        Bitmap myBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(myBitmap, imageMat);

// now convert to gray
        Mat grayMat = new Mat (bitmap.getHeight(), bitmap.getWidth(),
                CvType.CV_8U, new Scalar(1));
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_RGB2GRAY, 1);

// get the thresholded image
        Mat thresholdMat = new Mat ( bitmap.getHeight(), bitmap.getWidth(),
                CvType.CV_8U, new Scalar(1));
        Imgproc.threshold(grayMat, thresholdMat , 128, 255, Imgproc.THRESH_BINARY);

// convert back to bitmap for displaying
        Bitmap resultBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        thresholdMat.convertTo(thresholdMat, CvType.CV_8UC1);
        Utils.matToBitmap(thresholdMat, resultBitmap);

        ImageView imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setImageBitmap(resultBitmap);
    }

    static {
        System.loadLibrary("hello-android-jni");
    }

    public native String getMessage();
}
