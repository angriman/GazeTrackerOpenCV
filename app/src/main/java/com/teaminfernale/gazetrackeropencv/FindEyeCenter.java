package com.teaminfernale.gazetrackeropencv;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Eugenio on 02/05/16.
 */
public class FindEyeCenter {

    public Point unscalePoint(Point p, Rect origSize) {
        double ratio = (((double)Constants.kFastEyeWidth)/origSize.width);
        int x = (int) Math.round(p.x / ratio);
        int y = (int) Math.round(p.y / ratio);
        return new Point(x, y);
    }

    public void scaleToFastSize(Mat src, Mat dst) {
        Imgproc.resize(src, dst, new Size(Constants.kFastEyeWidth, (((double)Constants.kFastEyeWidth)/src.cols())*src.rows()));
    }

    public Mat computeMatXGradient(Mat mat) {
        Mat out = new Mat(mat.rows(), mat.cols(), CvType.CV_64F);

        for (byte y = 0; y < mat.rows(); ++y) {
            Mat mr = mat.row(y);
            out.put(y,0, mr.get(0, 1)[0] - mr.get(0,0)[0]);
            for (byte x = 1; x < mat.cols() - 1; ++x) {
                out.put(y, x, (mr.get(0, x+1)[0] - mr.get(0, x-1)[0]) / 2.0);
            }
        }

        return out;
    }

    public void testPossibleCentersFormulas(int x, int y, Mat weight, double gx, double gy, Mat out) {
        for (int cy = 0; cy < out.rows(); ++cy) {
            //double TODO
        }
    }



    public void findEyeCenter(Mat face, Rect eye, String debugWindow){
        // TODO
        // ???  Mat eyeROIUnscaled = face;


    }

}
