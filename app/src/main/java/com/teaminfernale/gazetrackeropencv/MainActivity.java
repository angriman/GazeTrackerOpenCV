package com.teaminfernale.gazetrackeropencv;

import android.content.Context;
import android.graphics.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "GazeTracker::MainActivity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;


    private int learn_frames = 0;
    private Mat templateR;
    private Mat templateL;
    int method = 0;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;

    // Matrix for zooming
    private Mat mZoomWindow;
    private Mat mZoomWindow2;

    private int mAbsoluteFaceSize = 0;
    private float mRelativeFaceSize = 0.2f;

    private Camera mCamera;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;
    private CameraBridgeViewBase mOpenCvCameraView;

    private int mDetectorType = JAVA_DETECTOR;

    private String[] mDetectorName;

    private SeekBar mMethodSeekbar;
    private TextView mValue;

    double xCenter = -1;
    double yCenter = -1;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        InputStream inputStream = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream outputStream = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }

                        inputStream.close();
                        outputStream.close();

                        // Load left eye classificator

                        InputStream iser = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirER = getDir("cascadeER", Context.MODE_PRIVATE);
                        File cascadeFileER = new File(cascadeDirER, "haarcascade_eye_right.xml");
                        FileOutputStream oser = new FileOutputStream(cascadeFileER);

                        byte[] bufferER = new byte[4096];
                        int bytesReadER;
                        while ((bytesReadER = iser.read(bufferER)) != -1) {
                            oser.write(bufferER, 0, bytesReadER);
                        }

                        iser.close();
                        oser.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        }

                        mJavaDetectorEye = new CascadeClassifier(cascadeFileER.getAbsolutePath());

                        if (mJavaDetectorEye.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetectorEye = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        }

                        cascadeDir.delete();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                   /* mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();*/

                } break;

                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mMethodSeekbar = (SeekBar) findViewById(R.id.methodSeekBar);
        mValue = (TextView) findViewById(R.id.method);

        mMethodSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                method = i;
                switch (method) {
                    case 0:
                        mValue.setText("TM_SQDIFF");
                        break;
                    case 1:
                        mValue.setText("TM_SQDIFF_NORMED");
                        break;
                    case 2:
                        mValue.setText("TM_CCOEFF");
                        break;
                    case 3:
                        mValue.setText("TM_CCOEFF_NORMED");
                        break;
                    case 4:
                        mValue.setText("TM_CCORR");
                        break;
                    case 5:
                        mValue.setText("TM_CCORR_NORMED");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mZoomWindow.release();
        mZoomWindow2.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        if (mZoomWindow == null || mZoomWindow2 == null) {
            createAuxiliaryMats();
        }

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        Rect[] facesArray = faces.toArray();

        for (int i = 0; i<facesArray.length; i++) {
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
            xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
            yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;

            Point center = new Point(xCenter, yCenter);

            Imgproc.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);

            Imgproc.putText(mRgba, "[" + center.x + "," + center.y + "]", new Point(center.x + 20, center.y + 20), Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));

            Rect r = facesArray[i];

            // Compute eye area
            Rect eyeArea = new Rect(r.x + r.width/8, (int) (r.y + (r.height/4.5)), r.width - 2*r.width/8, (int) (r.height/3.0));

            // Split the eye area
            Rect eyeAreaRight = new Rect(r.x + r.width/16,
                    (int) (r.y + (r.height/4.5)),
                    (r.width - 2*r.width/16)/2, (int) (r.height/3.0));
            Rect eyeAreaLeft = new Rect(r.x + r.width/16
                    + (r.width - 2*r.width/16)/2,
                    (int) (r.y + (r.height/4.5)),
                    (r.width - 2*r.width/16)/2, (int) (r.height/3.0));
            // draw the area - mGray is working grayscale mat, if you want to
            // see area in rgb preview, change mGray to mRgba

            Imgproc.rectangle(mRgba, eyeAreaLeft.tl(), eyeAreaLeft.br(), new Scalar(255, 0, 0, 255), 2);
            Imgproc.rectangle(mRgba, eyeAreaRight.tl(), eyeAreaRight.br(), new Scalar(255, 0, 0 ,255), 2);

            if (learn_frames < 5) {
                templateR = get_template(mJavaDetectorEye, eyeAreaRight, 24);
                templateL = get_template(mJavaDetectorEye, eyeAreaLeft, 24);
                learn_frames++;
            }
            else {
                // Learning finished, use the new templates for template
                // matching

                matchEye(eyeAreaRight, templateR, method);
                matchEye(eyeAreaLeft, templateL, method);
            }


            Imgproc.resize(mRgba.submat(eyeAreaLeft), mZoomWindow2, mZoomWindow2.size());
            Imgproc.resize(mRgba.submat(eyeAreaRight), mZoomWindow, mZoomWindow.size());


        }
        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu called");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected called; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }


    private void createAuxiliaryMats() {
        if (mGray.empty()) {
            return;
        }

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows/2 + rows/10, rows, cols/2  + cols/10, cols);
            mZoomWindow2 = mRgba.submat(0, rows/2 - rows/10, cols/2 + cols/10, cols);
        }
    }

    private Mat get_template(CascadeClassifier classificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eyeTemplate = new Rect();

        classificator.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length;) {
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;

            Rect eyeOnlyRectangle = new Rect((int) e.tl().x, (int) (e.tl().y + e.height * 0.4), (int) e.width, (int) (e.height * 0.6));            mROI = mGray.submat(eyeOnlyRectangle);
            Mat vyrez = mRgba.submat(eyeOnlyRectangle);

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
            iris.x = mmG.minLoc.x + eyeOnlyRectangle.x;
            iris.y = mmG.minLoc.y + eyeOnlyRectangle.y;

            eyeTemplate = new Rect((int) iris.x - size/2, (int) iris.y - size/2, size, size);
            Imgproc.rectangle(mRgba, eyeTemplate.tl(), eyeTemplate.br(), new Scalar(255, 0, 0, 255), 2);
            template = (mGray.submat(eyeTemplate)).clone();
            return template;
        }
        return template;
    }

    private void matchEye(Rect area, Mat mTemplate, int type) {
        Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;

        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        // Check for bad template size


        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            switch (type) {
                case TM_SQDIFF:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                    break;
                case TM_SQDIFF_NORMED:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF_NORMED);
                    break;
                case TM_CCOEFF:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                    break;
                case TM_CCOEFF_NORMED:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF_NORMED);
                    break;
                case TM_CCORR:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                    break;
                case TM_CCORR_NORMED:
                    Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR_NORMED);
                    break;
            }

            Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
            // there is difference in matching methods - best match is max/min value
            if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
                matchLoc = mmres.minLoc;
            } else {
                matchLoc = mmres.maxLoc;
            }

            Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
            Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x, matchLoc.y + mTemplate.rows() + area.y);

            Imgproc.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0, 255));
            Rect rec = new Rect(matchLoc_tx,matchLoc_ty);
        }
    }

    public void onRecreateClick(View v) {
        learn_frames = 0;
    }

    // JNI stuff

    static {
        System.loadLibrary("hello-android-jni");
    }

    public native String getMessage();

}
