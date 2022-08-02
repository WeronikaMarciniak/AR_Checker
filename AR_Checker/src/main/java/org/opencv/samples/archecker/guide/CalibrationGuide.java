package org.opencv.samples.archecker.guide;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.opencv.samples.archecker.CameraCalibrationActivity;
import org.opencv.samples.archecker.CameraCalibrator;
import org.opencv.samples.archecker.R;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Calendar;

public class CalibrationGuide {

    private static final String TAG = CalibrationGuide.class.getSimpleName();
    private static final int TIME_DIFF = 5000;
    private final ProgressBar mProgress;
    private final ImageView mTakePicture;
    private final CameraCalibrationActivity mActivity;
    private Mat mVideoFrame;
    private int mCurrentStep = 0;
    private int mWidth;
    private int mHeight;
    private static int CIRCLE_RADIUS = 15;
    private static Scalar CIRCLE_COLOR = new Scalar(253, 203, 0,1);
    private static int VALID_DISTANCE_MAX = 60;
    private static int VALID_DISTANCE_MIN = 10;
    private static final int MAX_POINTS = 8;
    Point mGuidePointPos = new Point(0,0);
    private boolean mPatternWasFound;
    private Mat mCorners;
    private CameraCalibrator mCameraCalibrator;

    private final Scalar arrowColorYellow = new Scalar(253, 203, 0, 1);
    private final Scalar arrowColorGreen = new Scalar(42, 159, 214,1);
    private Scalar currentArrowColor = arrowColorYellow;
    private long mStartTime;
    private Point mArrowEnd = new Point(0,0);
    private Point mArrowStart = new Point();
    private CalibrationGuideListener mListener;
    private int mCalculatedDensity;

    public CalibrationGuide(int width, int height,CameraCalibrationActivity cameraCalibrationActivity){
        this.mWidth = width;
        this.mHeight = height;
        mProgress = (ProgressBar) cameraCalibrationActivity.findViewById(R.id.progressBar);
        mTakePicture = (ImageView) cameraCalibrationActivity.findViewById(R.id.image_takePicture);
        this.mActivity = cameraCalibrationActivity;
        calculateDisplayDensity();
    }

    private void calculateDisplayDensity() {
        float widthInInch;
        Resources r = mActivity.getResources();
        DisplayMetrics metrics = r.getDisplayMetrics();
        widthInInch = (float) metrics.widthPixels / (float) metrics.densityDpi;
        float newDensity = mWidth / widthInInch;

        mCalculatedDensity = Math.round(newDensity);
    }

    public void registerCalibrationGuideListener(CalibrationGuideListener listener){
        this.mListener = listener;
    }

    public void processFrame(Mat videoFrame, boolean patternWasFound, Mat corners, CameraCalibrator cameraCalibrator){
        mVideoFrame = videoFrame;
        mPatternWasFound = patternWasFound;
        mCorners = corners;
        mCameraCalibrator = cameraCalibrator;
        drawCircleInFrame();

        if(mCurrentStep == 0 && mCorners != null && !mCorners.empty() && patternWasFound){
            mArrowStart.x = mCorners.get(40,0)[0];
            mArrowStart.y = mCorners.get(40,0)[1];
        }
        if(mPatternWasFound){
            drawArrowInFrame();
        }

        holdDistanceToGuideCorner();
    }

    private void holdDistanceToGuideCorner() {
        long currentTime = Calendar.getInstance().getTimeInMillis();

        if (mPatternWasFound){
            double distance = calcDistance(mArrowStart, mArrowEnd);

            Log.d(TAG,"Distance Max in pix: "+ dpToPixel(VALID_DISTANCE_MAX));
            if (distance > dpToPixel(VALID_DISTANCE_MIN) && distance < dpToPixel(VALID_DISTANCE_MAX)){
                currentArrowColor = arrowColorGreen;
                if (currentTime > (mStartTime + TIME_DIFF)) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTakePicture.setVisibility(View.VISIBLE);
                            mProgress.setVisibility(View.INVISIBLE);
                        }
                    });

                    if(!mCameraCalibrator.checkLastFrame()) {
                        mCameraCalibrator.addCorners();
                        mActivity.pictureAdded(mCameraCalibrator.getCornersBufferSize());

                        mCameraCalibrator.calibrate();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        next();
                    }
                    else{
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTakePicture.setVisibility(View.INVISIBLE);
                                mProgress.setVisibility(View.VISIBLE);
                                Toast toast = Toast.makeText(mActivity, R.string.text_frameRejected,Toast.LENGTH_LONG);
                                toast.show();
                            }
                        });
                    }
                }
                if(mStartTime > 0) {
                    int progress = Math.round(((float) safeLongToInt(currentTime - mStartTime)) / TIME_DIFF * 100);
                    //Increase progress bar
                    mProgress.setProgress(progress);
                }
            }
            else {
                currentArrowColor = arrowColorYellow;
                mStartTime = currentTime;
                mProgress.setProgress(0);
            }
        }
        else{
            mProgress.setProgress(0);
            mStartTime = currentTime;
            currentArrowColor = arrowColorYellow;
        }
    }

    private void next(){
        mStartTime = Calendar.getInstance().getTimeInMillis();;
        mCurrentStep ++;
        nextGuidePoint();
        drawCircleInFrame();
        drawArrowInFrame();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTakePicture.setVisibility(View.INVISIBLE);
                mProgress.setVisibility(View.VISIBLE);
            }
        });

        if(MAX_POINTS == mCurrentStep){
            mListener.calibrationGuideFinish();
            mCurrentStep = 0;
        }
    }

    private void drawArrowInFrame() {
        double startX = 0;
        double startY = 0;
        double[] p1;
        double[] p2;

        switch (mCurrentStep){
            case 1:
                p1 = mCorners.get(24,0);
                p2 = mCorners.get(16,0);
                startX = ((p2[0] - p1[0]) / 2) + p1[0];
                startY = p1[1];
                break;
            case 2:
                startX = mCorners.get(0,0)[0];
                startY = mCorners.get(0,0)[1];
                break;
            case 3:
                startX = mCorners.get(0,0)[0];
                startY = mCorners.get(0,0)[1] + ((mCorners.get(7,0)[1] - mCorners.get(0,0)[1]) / 2);
                break;
            case 4:
                startX = mCorners.get(0,0)[0];
                startY = mCorners.get(7,0)[1];
                break;
            case 5:
                startX = mCorners.get(23,0)[0];
                startY = mCorners.get(23,0)[1];
                break;
            case 6:
                startX = mCorners.get(43,0)[0];
                startY = mCorners.get(39,0)[1];
                break;
            case 7:
                startX = mCorners.get(40,0)[0];
                startY = mCorners.get(40,0)[1] + ((mCorners.get(39,0)[1] - mCorners.get(40,0)[1])/2);
                break;
            default:
                startX = mCorners.get(40,0)[0];
                startY = mCorners.get(40,0)[1];
                break;
        }
        mArrowStart = new Point(startX,startY);
        mArrowEnd.x = mGuidePointPos.x;
        mArrowEnd.y = mGuidePointPos.y;

        Imgproc.arrowedLine(mVideoFrame, mArrowStart, mArrowEnd, currentArrowColor,2,Imgproc.LINE_8,0,0.01);
    }

    private void nextGuidePoint() {
        currentArrowColor = arrowColorYellow;
        double x = 0;
        double y = 0;

        switch (mCurrentStep){
            case 1:
                x = mWidth / 2;
                y = 0;
                break;
            case 2:
                x = mWidth;
                y = 0;
                break;
            case 3:
                x = mWidth;
                y = mHeight /2;
                break;
            case 4:
                x = mWidth;
                y = mHeight;
                break;
            case 5:
                x = mWidth / 2;
                y = mHeight;
                break;
            case 6:
                x = 0;
                y = mHeight;
                break;
            case 7:
                x = 0;
                y = mHeight /2;
                break;
        }
        mGuidePointPos.x = x;
        mGuidePointPos.y = y;
    }

    private void drawCircleInFrame() {
        Imgproc.circle(mVideoFrame, mGuidePointPos,dpToPixel(CIRCLE_RADIUS),CIRCLE_COLOR,-1,Imgproc.LINE_AA,0);
    }

    private double calcDistance(Point start, Point end) {

        double a = end.x - start.x;
        double b = end.y - start.y;

        double length = Math.sqrt(Math.pow(a,2) + Math.pow(b,2));
        Log.d(TAG,"Distance to corner: "+ length);
        return length;
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    private int dpToPixel(int dp) {
        return (int)((dp * mCalculatedDensity/160) + 0.5);
    }
}
