package org.archecker.cameracalibration;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.archecker.imageprocessing.ChessboardProcessing;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class CameraCalibrator implements Serializable{
    private static final String TAG = "CameraCalibrator";
    private final Size patternSize = new Size(7, 7);
    private final int cornersSize = (int)(patternSize.width * patternSize.height);
    private boolean patternWasFound = false;
    private MatOfPoint2f corners = new MatOfPoint2f();
    private List<Mat> cornersBuffer = new ArrayList<>();
    private boolean isCalibrated = false;
    private Mat cameraMatrix = new Mat();
    private Mat distortionCoefficients = new Mat();
    private int flags;
    private double rms;
    private double fieldSize = 0.0181;
    private Size imageSize;
    private ArrayList<Mat> objectPoints = new ArrayList<>();
    private Mat reprojectionErrors;

    public CameraCalibrator(int width, int height) {
        imageSize = new Size(width, height);
        flags = Calib3d.CALIB_FIX_PRINCIPAL_POINT +
                Calib3d.CALIB_ZERO_TANGENT_DIST +
                Calib3d.CALIB_FIX_ASPECT_RATIO +
                Calib3d.CALIB_FIX_K4 +
                Calib3d.CALIB_FIX_K5;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(cameraMatrix);
        cameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(distortionCoefficients);
        Log.i(TAG, "Instantiated new " + this.getClass());
        reprojectionErrors = new Mat();
    }


    public void processFrame(Mat grayFrame, Mat rgbaFrame) {
        findPattern(grayFrame);
        renderFrame(rgbaFrame);
    }

    public void calibrate() {
        ArrayList<Mat> rvecs = new ArrayList<>();
        ArrayList<Mat> tvecs = new ArrayList<>();

        ArrayList<Mat> objectPoints = new ArrayList<>();
        objectPoints.add(Mat.zeros(cornersSize, 1, CvType.CV_32FC3));
        calcBoardCornerPositions(objectPoints.get(0));
        for (int i = 1; i < cornersBuffer.size(); i++) {
            objectPoints.add(objectPoints.get(0));
        }

        Calib3d.calibrateCamera(objectPoints, cornersBuffer, imageSize,
                cameraMatrix, distortionCoefficients, rvecs, tvecs, flags);

        isCalibrated = Core.checkRange(cameraMatrix)
                && Core.checkRange(distortionCoefficients);

        rms = computeReprojectionErrors(objectPoints, rvecs, tvecs, reprojectionErrors);
        Log.i(TAG, String.format("Average re-projection error: %f", rms));
        Log.i(TAG, "Camera matrix: " + cameraMatrix.dump());
        Log.i(TAG, "Distortion coefficients: " + distortionCoefficients.dump());
        this.objectPoints = objectPoints;
    }

    public void clearCorners() {
        cornersBuffer.clear();
    }

    private void calcBoardCornerPositions(Mat corners) {
        final int cn = 3;
        float positions[] = new float[cornersSize * cn];

        for (int i = 0; i < patternSize.height; i++) {
            for (int j = 0; j < patternSize.width * cn; j += cn) {
                positions[(int) (i * patternSize.width * cn + j + 0)] =
                        (2 * (j / cn) + i % 2) * (float) fieldSize;
                positions[(int) (i * patternSize.width * cn + j + 1)] =
                        i * (float) fieldSize;
                positions[(int) (i * patternSize.width * cn + j + 2)] = 0;
            }
        }
        corners.create(cornersSize, 1, CvType.CV_32FC3);
        corners.put(0, 0, positions);
    }

    private double computeReprojectionErrors(List<Mat> objectPoints,
                                             List<Mat> rvecs, List<Mat> tvecs, Mat perViewErrors) {
        MatOfPoint2f cornersProjected = new MatOfPoint2f();
        double totalError = 0;
        double error;
        float viewErrors[] = new float[objectPoints.size()];

        MatOfDouble distortionCoefficients = new MatOfDouble(this.distortionCoefficients);
        int totalPoints = 0;
        for (int i = 0; i < objectPoints.size(); i++) {
            MatOfPoint3f points = new MatOfPoint3f(objectPoints.get(i));
            Calib3d.projectPoints(points, rvecs.get(i), tvecs.get(i),
                    cameraMatrix, distortionCoefficients, cornersProjected);
            error = Core.norm(cornersBuffer.get(i), cornersProjected, Core.NORM_L2);

            int n = objectPoints.get(i).rows();
            viewErrors[i] = (float) Math.sqrt(error * error / n);
            totalError  += error * error;
            totalPoints += n;
        }
        perViewErrors.create(objectPoints.size(), 1, CvType.CV_32FC1);
        perViewErrors.put(0, 0, viewErrors);

        return Math.sqrt(totalError / totalPoints);
    }

    private void findPattern(Mat grayFrame) {
        patternWasFound = Calib3d.findChessboardCorners(grayFrame, patternSize,
                corners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        if(patternWasFound) {
            Log.i(TAG, "Corners: " + corners.toArray());
            ChessboardProcessing.straightenImage(grayFrame);
            ChessboardProcessing.cropCheckerFields(grayFrame);
        }
    }

    public void addCorners() {
        if (patternWasFound) {
            cornersBuffer.add(corners.clone());
        }
    }

    private void drawPoints(Mat rgbaFrame) {
        Calib3d.drawChessboardCorners(rgbaFrame, patternSize, corners, patternWasFound);
    }

    private void renderFrame(Mat rgbaFrame) {
        drawPoints(rgbaFrame);
        Imgproc.putText(rgbaFrame, "Captured: " + cornersBuffer.size(), new Point(rgbaFrame.cols() / 3 * 2, rgbaFrame.rows() * 0.1),
                Core.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 255, 0));
    }

    public Mat getCameraMatrix() {
        return cameraMatrix;
    }

    public Mat getDistortionCoefficients() {
        return distortionCoefficients;
    }

    public int getCornersBufferSize() {
        return cornersBuffer.size();
    }

    public double getAvgReprojectionError() {
        return rms;
    }

    public boolean isCalibrated() {
        return isCalibrated;
    }

    public void setCalibrated() {
        isCalibrated = true;
    }

    public boolean checkLastFrame()
    {
        boolean isFrameBad = false;

        if(cornersBuffer.size() > 0) {

            Mat tmpCamMatrix = new Mat();
            double badAngleThresh = 40;

            if (cameraMatrix.total() > 0) {
                tmpCamMatrix = Mat.eye(3, 3, CvType.CV_64F);
                tmpCamMatrix.put(0, 0, 20000);
                tmpCamMatrix.put(1, 1, 20000);
                tmpCamMatrix.put(0, 2, imageSize.height / 2);
                tmpCamMatrix.put(1, 2, imageSize.width / 2);
            } else {
                cameraMatrix.copyTo(tmpCamMatrix);
            }

            Mat r, t, angles;
            r = new Mat();
            t = new Mat();
            angles = new MatOfPoint2f();

            MatOfPoint3f previousObjectPoints = new MatOfPoint3f(objectPoints.get(objectPoints.size() -1));

            MatOfPoint2f currentPoints = new MatOfPoint2f(corners);

            MatOfDouble distortionCoefficients = new MatOfDouble(this.distortionCoefficients);

            Calib3d.solvePnP(previousObjectPoints, currentPoints, tmpCamMatrix, distortionCoefficients, r, t);
            angles = CameraRotationHandler.rodriguesToEuler(r, CameraRotationHandler.CALIB_DEGREES);

            if (angles != null) {
                if (Math.abs(angles.get(0, 0)[0]) > badAngleThresh || Math.abs(angles.get(1, 0)[0]) > badAngleThresh) {
                    isFrameBad = true;
                }
            } else {
                Log.e(TAG, "Frame evaluation failed");
            }
        }
        return isFrameBad;
    }

    public final Mat getReprojectionErrors(){
        return reprojectionErrors;
    }

    public void removeFrame(Mat itemToRemove) {
        cornersBuffer.remove(itemToRemove);
    }

    public Mat getFrame(int index) {
        return cornersBuffer.get(index);
    }

    public boolean patternWasFound() {return patternWasFound;}

    public final MatOfPoint2f getCorners() {
        return corners;
    }

    public ArrayList<Float> getReprojectionErrorArrayList(){
        ArrayList<Float> reprojectionArray = new ArrayList<>();

        for(int i = 0; i <= this.getCornersBufferSize()-1;i++){
            reprojectionArray.add((float) reprojectionErrors.get(i,0)[0]);
        }
        return reprojectionArray;
    }
}