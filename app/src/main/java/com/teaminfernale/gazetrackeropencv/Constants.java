package com.teaminfernale.gazetrackeropencv;

/**
 * Created by Eugenio on 02/05/16.
 */
public class Constants {
    // Debugging
    public final boolean kPlotVectorField = false;

    // Size public finalants
    public final int kEyePercentTop = 25;
    public final int kEyePercentSide = 13;
    public final int kEyePercentHeight = 30;
    public final int kEyePercentWidth = 35;

    // Preprocessing
    public final boolean kSmoothFaceImage = false;
    public final double kSmoothFaceFactor = 0.005;

    // Algorithm Parameters
    public final int kFastEyeWidth = 50;
    public final int kWeightBlurSize = 5;
    public final boolean kEnableWeight = true;
    public final double kWeightDivisor = 1.0;
    public final double kGradientThreshold = 50.0;

    // Postprocessing
    public final boolean kEnablePostProcess = true;
    public final double kPostProcessThreshold = 0.97;

    // Eye Corner
    public final boolean kEnableEyeCorner = false;
}
