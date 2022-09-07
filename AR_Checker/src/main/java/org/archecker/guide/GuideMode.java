package org.archecker.guide;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.archecker.cameracalibration.CameraCalibrationActivity;
import org.archecker.cameracalibration.CameraCalibrator;
import org.archecker.R;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.util.Calendar;

public class GuideMode {

    private static final String TAG = GuideMode.class.getSimpleName();
    private static final int TIME_DIFF = 5000;
    private static int CIRCLE_RADIUS = 15;
    private static Scalar CIRCLE_COLOR = new Scalar(253, 203, 0,1);
    private static int VALID_DISTANCE_MAX = 60;
    private static int VALID_DISTANCE_MIN = 10;
    private static final int MAX_POINTS = 8;
    private final ProgressBar progress;
    private final ImageView takePicture;
    private final CameraCalibrationActivity mActivity;
    private Mat frame;
    private int step = 0;
    private int width;
    private int height;
    Point guidePoint = new Point(0,0);
    private boolean patternWasFound;
    private Mat corners;
    private CameraCalibrator calibrator;
    private final Scalar arrowColorYellow = new Scalar(253, 203, 0, 1);
    private final Scalar arrowColorGreen = new Scalar(42, 159, 214,1);
    private Scalar currentArrowColor = arrowColorYellow;
    private long start;
    private Point arrowStart = new Point();
    private Point arrowEnd = new Point(0,0);
    private GuideModeListener guideModeListener;
    private int calculatedDensity;

    public GuideMode(int width, int height, CameraCalibrationActivity cameraCalibrationActivity){
        this.width = width;
        this.height = height;
        progress = (ProgressBar) cameraCalibrationActivity.findViewById(R.id.progressBar);
        takePicture = (ImageView) cameraCalibrationActivity.findViewById(R.id.image_takePicture);
        this.mActivity = cameraCalibrationActivity;
        calculateDisplayDensity();
    }

    private void calculateDisplayDensity() {
        float widthInInch;
        Resources res = mActivity.getResources();
        DisplayMetrics metrics = res.getDisplayMetrics();
        widthInInch = (float) metrics.widthPixels / (float) metrics.densityDpi;
        float newDensity = width / widthInInch;
        calculatedDensity = Math.round(newDensity);
    }

    public void registerCalibrationGuideListener(CameraCalibrationActivity listener){
        this.guideModeListener = (GuideModeListener) listener;
    }

    public void processFrame(Mat videoFrame, boolean patternWasFound, Mat corners, CameraCalibrator cameraCalibrator){
        frame = videoFrame;
        this.patternWasFound = patternWasFound;
        this.corners = corners;
        calibrator = cameraCalibrator;
        drawCircleInFrame();
        if(step == 0 && this.corners != null && !this.corners.empty() && patternWasFound){
            arrowStart.x = this.corners.get(40,0)[0];
            arrowStart.y = this.corners.get(40,0)[1];
        }
        if(this.patternWasFound){
            drawArrowInFrame();
        }
        holdDistanceToGuideCorner();
    }

    private void holdDistanceToGuideCorner() {
        long time = Calendar.getInstance().getTimeInMillis();

        if (patternWasFound){
            double distance = calcDistance(arrowStart, arrowEnd);
            Log.d(TAG,"Distance Max in pix: "+ dpToPixel(VALID_DISTANCE_MAX));
            if (distance > dpToPixel(VALID_DISTANCE_MIN) && distance < dpToPixel(VALID_DISTANCE_MAX)){
                currentArrowColor = arrowColorGreen;
                if (time > (start + TIME_DIFF)) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            takePicture.setVisibility(View.VISIBLE);
                            progress.setVisibility(View.INVISIBLE);
                        }
                    });

                    if(!calibrator.checkLastFrame()) {
                        calibrator.addCorners();
                        mActivity.picAddedMessage(calibrator.getCornersBufferSize());

                        calibrator.calibrate();
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
                                takePicture.setVisibility(View.INVISIBLE);
                                progress.setVisibility(View.VISIBLE);
                                Toast toast = Toast.makeText(mActivity, R.string.text_frameRejected,Toast.LENGTH_LONG);
                                toast.show();
                            }
                        });
                    }
                }
                if(start > 0) {
                    int progress = Math.round(((float) safeLongToInt(time - start)) / TIME_DIFF * 100);
                    this.progress.setProgress(progress);
                }
            }
            else {
                currentArrowColor = arrowColorYellow;
                start = time;
                progress.setProgress(0);
            }
        }
        else{
            progress.setProgress(0);
            start = time;
            currentArrowColor = arrowColorYellow;
        }
    }

    private void next(){
        start = Calendar.getInstance().getTimeInMillis();;
        step++;
        nextGuidePoint();
        drawCircleInFrame();
        drawArrowInFrame();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                takePicture.setVisibility(View.INVISIBLE);
                progress.setVisibility(View.VISIBLE);
            }
        });

        if(MAX_POINTS == step){
            guideModeListener.calibrationGuideFinish();
            step = 0;
        }
    }

    private void drawArrowInFrame() {
        double startX = 0;
        double startY = 0;
        double[] p1;
        double[] p2;

        switch (step){
            case 1:
                p1 = corners.get(24,0);
                p2 = corners.get(16,0);
                startX = ((p2[0] - p1[0]) / 2) + p1[0];
                startY = p1[1];
                break;
            case 2:
                startX = corners.get(0,0)[0];
                startY = corners.get(0,0)[1];
                break;
            case 3:
                startX = corners.get(0,0)[0];
                startY = corners.get(0,0)[1] + ((corners.get(7,0)[1] - corners.get(0,0)[1]) / 2);
                break;
            case 4:
                startX = corners.get(0,0)[0];
                startY = corners.get(7,0)[1];
                break;
            case 5:
                startX = corners.get(23,0)[0];
                startY = corners.get(23,0)[1];
                break;
            case 6:
                startX = corners.get(43,0)[0];
                startY = corners.get(39,0)[1];
                break;
            case 7:
                startX = corners.get(40,0)[0];
                startY = corners.get(40,0)[1] + ((corners.get(39,0)[1] - corners.get(40,0)[1])/2);
                break;
            default:
                startX = corners.get(40,0)[0];
                startY = corners.get(40,0)[1];
                break;
        }
        arrowStart = new Point(startX,startY);
        arrowEnd.x = guidePoint.x;
        arrowEnd.y = guidePoint.y;

        Imgproc.arrowedLine(frame, arrowStart, arrowEnd, currentArrowColor,2,Imgproc.LINE_8,0,0.01);
    }

    private void nextGuidePoint() {
        currentArrowColor = arrowColorYellow;
        double x = 0;
        double y = 0;

        switch (step){
            case 1:
                x = width / 2;
                y = 0;
                break;
            case 2:
                x = width;
                y = 0;
                break;
            case 3:
                x = width;
                y = height /2;
                break;
            case 4:
                x = width;
                y = height;
                break;
            case 5:
                x = width / 2;
                y = height;
                break;
            case 6:
                x = 0;
                y = height;
                break;
            case 7:
                x = 0;
                y = height /2;
                break;
        }
        guidePoint.x = x;
        guidePoint.y = y;
    }

    private void drawCircleInFrame() {
        Imgproc.circle(frame, guidePoint,dpToPixel(CIRCLE_RADIUS),CIRCLE_COLOR,-1,Imgproc.LINE_AA,0);
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
        return (int)((dp * calculatedDensity /160) + 0.5);
    }
}
