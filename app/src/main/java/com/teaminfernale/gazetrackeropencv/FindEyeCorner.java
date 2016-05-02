package com.teaminfernale.gazetrackeropencv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Eugenio on 02/05/16.
 */
public class FindEyeCorner {

    private static final boolean kEyeLeft = true;
    private static final boolean kEyeLRight = false;

    private Mat leftCornerKernel;
    private Mat rightCornerKernel;
    private double[][] kEyeCornerKernel = {
            {-1, -1, -1, 1, 1, 1},
            {-1, -1, -1, -1, 1, 1},
            {-1, -1, -1, -1, 0, 3},
            {1, 1, 1, 1, 1, 1}
    };

    public void createCornerKernels() {
        rightCornerKernel = new Mat(4, 6, CvType.CV_32F, new Scalar(0));
        for (int i = 0; i<kEyeCornerKernel.length; i++) {
            for (int j = 0; j<kEyeCornerKernel[0].length; j++) {
                rightCornerKernel.put(i,j,kEyeCornerKernel[i][j]);
            }
        }

        leftCornerKernel = new Mat(4, 6, CvType.CV_32F);
        Core.flip(rightCornerKernel, leftCornerKernel, 1);
    }

    public void releaseCornerKernels() {
        leftCornerKernel = null;
        rightCornerKernel = null;
    }

    public Mat eyeCornerMap(Mat region, boolean left, boolean left2) {
        Mat cornerMap = new Mat();

        Size sizeRegion = region.size();
        Range colRange = new Range((int)sizeRegion.width/4, (int)sizeRegion.width*3/4);
        Range rowRange = new Range((int)sizeRegion.height/4, (int)sizeRegion.height*3/4);

        Mat miRegion = new Mat(region, rowRange, colRange);
        Imgproc.filter2D(miRegion, cornerMap, CvType.CV_32F, (left && !left2) || (!left && !left2) ? leftCornerKernel : rightCornerKernel);

        return cornerMap;
    }

    public MatOfPoint2f findEyeCorner(Mat region, boolean left, boolean left2) {
        Mat cornerMat = eyeCornerMap(region, left, left2);

        Point maxP = Core.minMaxLoc(cornerMat).maxLoc;
        MatOfPoint2f maxP2 = findSubPixelEyeCorner(cornerMat, maxP);

        return maxP2;
    }

    public MatOfPoint2f findSubPixelEyeCorner(Mat region, Point maxP) {

        Size sizeRegion = region.size();
        Mat cornerMap = new Mat((int)sizeRegion.height*10, (int)sizeRegion.width * 10, CvType.CV_32F);
        Imgproc.resize(region, cornerMap, cornerMap.size(), 0, 0, Imgproc.INTER_CUBIC);

        Point maxP2 = Core.minMaxLoc(cornerMap).maxLoc;
        return new MatOfPoint2f(new Point((int)(sizeRegion.width/2 + maxP2.x/10), (int)(sizeRegion.height/2 + maxP2.y/10)));
    }

}
