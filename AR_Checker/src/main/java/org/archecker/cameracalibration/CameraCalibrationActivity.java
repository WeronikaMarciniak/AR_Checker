package org.archecker.cameracalibration;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import org.artoolkit.ar.base.rendering.ARRenderer;
import org.archecker.R;
import org.archecker.artoolkitgame.StartGameActivity;
import org.archecker.guide.GuideMode;
import org.archecker.guide.GuideModeListener;
import org.archecker.menu.MenuArrayAdapter;
import org.archecker.share.ShareActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.StaticHelper;
import org.opencv.core.Mat;

import java.io.File;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class CameraCalibrationActivity extends Activity implements CvCameraViewListener2,
        View.OnClickListener, Animation.AnimationListener, AdapterView.OnItemClickListener, GuideModeListener {
    public static final int COMPARE_MENU = 0;
    public static final int UNDISTORETED = 1;
    public static final int NEW_CALIBRATION = 2;
    public static final int SHARE_CALIBRATION = 3;
    public static final int SETTINGS = 5;
    public static final int CALIB_MESSAGE = 7;
    public static final int CALIB_STATS = 4;
    private static final int CALIBRATION_DETAIL_REQ_CODE = 1;
    public static final String INTENT_EXTRA_CAMERA_CALIBRATOR = "Calibrator";
    public static boolean GUIDE_MODE = false;
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase oCvCameraView;
    private CameraCalibrator calibrator;
    private OnCameraFrameRender renderer;
    private int matrixWidth;
    private int matrixHeight;
    private ImageButton startCalibrationButton;
    private ImageButton startGameButton;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private TextView guideText;
    private Animation fadeInAnimation;
    private Animation fadeOutAnimation;
    private MenuArrayAdapter menuArrayAdapter;
    private ViewGroup compareVideo;
    private SharedPreferences preferences;
    private ProgressBar progress;
    private ImageButton guideButton;
    private PopupWindow mPopupWindow;
    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("calibration_upload_native");
    }
    
    public CameraCalibrationActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public static native void nativeSaveParam(double[] cameraMatrix, double[] distortionCoefficientsArray,
                                              int sizeX, int sizeY, float average, float min, float max);
    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "call onCreate");
        super.onCreate(savedInstanceState);
        if (!StaticHelper.initOpenCV(false)) {
            Log.d(TAG, "OpenCV library not found.");
            finish();
        } else {
            Log.d(TAG, "OpenCV library found.");
            Log.i(TAG, "OpenCV loaded successfully");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.camera_calibration_surface_view);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        oCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_calibration_java_surface_view);
        oCvCameraView.setVisibility(SurfaceView.VISIBLE);
        oCvCameraView.setCvCameraViewListener(this);
        oCvCameraView.setOnClickListener(this);

        startCalibrationButton = (ImageButton) findViewById(R.id.button_startCalibration);
        startCalibrationButton.setOnClickListener(this);
        startGameButton = (ImageButton) findViewById(R.id.button_startGame);
        startGameButton.setOnClickListener(this);

        menuButton = (ImageButton) findViewById(R.id.button_menu);
        menuButton.setOnClickListener(this);
        guideButton = (ImageButton) findViewById(R.id.button_guideMode);
        guideButton.setOnClickListener(this);
        
        String[] menuEntries = getResources().getStringArray(R.array.menuItems);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ListView drawerList = (ListView) findViewById(R.id.menuList);
        menuArrayAdapter = new MenuArrayAdapter(this, new ArrayList<>(Arrays.asList(menuEntries)));
        drawerList.setAdapter(menuArrayAdapter);
        drawerList.setOnItemClickListener(this);
        guideText = (TextView) findViewById(R.id.text_guiding);
        
        fadeInAnimation = AnimationUtils.loadAnimation(this, R.layout.fade_in);
        fadeInAnimation.setAnimationListener(this);
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.layout.fade_out);
        fadeOutAnimation.setAnimationListener(this);
        compareVideo = (ViewGroup) findViewById(R.id.view_compare);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        
    }

    @Override
    public void onPause() {
        super.onPause();
        if (oCvCameraView != null)
            oCvCameraView.disableView();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onResume() {
        super.onResume();
        oCvCameraView.enableView();
        int cameraId = Integer.parseInt(preferences.getString(CameraPrefActivity.PREF_CAMERA_INDEX, this.getString(R.string.pref_defaultValue_cameraIndex)));
        oCvCameraView.setCameraIndex(cameraId);
        oCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        boolean frontFacing = false;
        CameraManager cameraManager = (CameraManager) oCvCameraView.getContext().getSystemService(Context.CAMERA_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId + "");
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontFacing = true;
                }
            } catch (Exception e) {

            }
        }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) frontFacing = true;
        }
        else{
            Log.e(TAG, "Not supported Android version.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                return null;
            }
        };

        if (oCvCameraView != null)
            oCvCameraView.disableView();
    }
    public void onCameraViewStarted(int width, int height) {
        oCvCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (matrixWidth != width || matrixHeight != height) {
            matrixWidth = width;
            matrixHeight = height;
            calibrator = new CameraCalibrator(matrixWidth, matrixHeight);
            guideButton.setBackgroundResource(R.drawable.hexagon);

            if (CalibrationResult.tryLoad(this, calibrator.getCameraMatrix(), calibrator.getDistortionCoefficients(), matrixWidth, matrixHeight)) {
                calibrator.setCalibrated();
                guideText.setText(R.string.guidingText_Preloaded);
            }
            renderer = new OnCameraFrameRender(new CalibrationFrameRender(calibrator));
        }
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return renderer.render(inputFrame);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick invoked on View: " + v);
        if (v.equals(startCalibrationButton)) {
            String picsTaken = getResources().getQuantityString(R.plurals.numberOfPicturesTaken, 0, 0);
            guideText.setText(picsTaken);
            startCalibrationButton.startAnimation(fadeOutAnimation);
            startCalibration(calibrator);
        }
        else if (v.equals(startGameButton)) {

            startGameButton.startAnimation(fadeOutAnimation);
            Intent intent1 = new Intent(this, StartGameActivity.class);
            startActivity(intent1);
        }else if (v.equals(menuButton)) {
            drawerLayout.openDrawer(GravityCompat.START);
        } else if (v.equals(guideButton)) {
            guideModeManagement();
        }
        else {
            if (renderer.instanceOfFrameRenderer(CalibrationFrameRender.class) && !GUIDE_MODE) {
                calibrator.addCorners();
                this.picAddedMessage(calibrator.getCornersBufferSize());
            }
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Context context = parent.getContext();
        compareVideo.setVisibility(View.INVISIBLE);
        guideText.setVisibility(View.VISIBLE);
        if (position == SETTINGS) {
            context.startActivity(new Intent(context, CameraPrefActivity.class));
        } else if (position == COMPARE_MENU && isCalibrationAvailable()) {
            guideButton.setVisibility(View.INVISIBLE);
            compareVideo.setVisibility(View.VISIBLE);
            renderer =
                    new OnCameraFrameRender(new ComparisonFrameRender(calibrator, matrixWidth, matrixHeight));
            guideText.setVisibility(View.INVISIBLE);
        } else if (position == UNDISTORETED && isCalibrationAvailable()) {
            guideButton.setVisibility(View.INVISIBLE);
            renderer =
                    new OnCameraFrameRender(new UndistortionFrameRender(calibrator));
            guideText.setText(R.string.undistorted);
        } else if (position == NEW_CALIBRATION) {
            guideButton.setVisibility(View.VISIBLE);
            startGameButton.setVisibility(View.INVISIBLE);
            calibrator = new CameraCalibrator(matrixWidth, matrixHeight);
            renderer =
                    new OnCameraFrameRender(new CalibrationFrameRender(calibrator));
            guideText.setText(R.string.guidingText_Start);
        } else if (position == SHARE_CALIBRATION) {
            File calibsFile = new File(this.getCacheDir().getAbsolutePath() + "/calibs");
            Log.d(TAG, "calibsFile: " + calibsFile.toString());
            File[] fileArray = calibsFile.listFiles();

            if (fileArray != null && (Array.getLength(fileArray) != 0)) {
                Intent share = new Intent(this, ShareActivity.class);
                startActivity(share);
            } else {
                Toast toast = Toast.makeText(this, R.string.error_no_calibration, Toast.LENGTH_LONG);
                toast.show();
            }
        } else if (position == CALIB_STATS && isCalibrationAvailable()) {
            Intent openStatistics = new Intent(this, CalibrationStatisticsActivity.class);
            openStatistics.putExtra(INTENT_EXTRA_CAMERA_CALIBRATOR, calibrator);
            startActivityForResult(openStatistics, CALIBRATION_DETAIL_REQ_CODE);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
    }
    @Override
    public void onAnimationStart(Animation animation) {}

    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation.equals(fadeInAnimation))
            startCalibrationButton.setVisibility(View.VISIBLE);

        else if (animation.equals(fadeOutAnimation))
            startCalibrationButton.setVisibility(View.INVISIBLE);

        if(calibrator.isCalibrated() && animation.equals(fadeInAnimation)){
            startGameButton.setVisibility(View.VISIBLE);
        }
        else if(!calibrator.isCalibrated() && animation.equals(fadeOutAnimation)){
            startGameButton.setVisibility(View.INVISIBLE);}
    }

    @Override
    public void onAnimationRepeat(Animation animation) {}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CALIBRATION_DETAIL_REQ_CODE){
            if(resultCode == RESULT_OK){
                CameraCalibrator cameraCalibrator = (CameraCalibrator) data.getSerializableExtra(CameraCalibrationActivity.INTENT_EXTRA_CAMERA_CALIBRATOR);
                if(cameraCalibrator.getCornersBufferSize() > 1) {
                    startCalibration(cameraCalibrator);
                }
            }
        }
    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private void startCalibration(final CameraCalibrator calibrator) {
        final Resources resources = getResources();

        renderer = new OnCameraFrameRender(new PreviewFrameRender());
        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog calibrationProgress;

            @Override
            protected void onPreExecute() {
                calibrationProgress = new ProgressDialog(CameraCalibrationActivity.this);
                calibrationProgress.setTitle(resources.getString(R.string.calibrating));
                calibrationProgress.setMessage(resources.getString(R.string.please_wait));
                calibrationProgress.setCancelable(false);
                calibrationProgress.setIndeterminate(true);
                calibrationProgress.show();
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                calibrator.calibrate();
                return null;
            }

            @SuppressLint("StaticFieldLeak")
            @Override
            protected void onPostExecute(Void calibResult) {
                calibrationProgress.dismiss();
                String resultMessage = (calibrator.isCalibrated()) ?
                        resources.getString(R.string.calibration_successful) + " " + calibrator.getAvgReprojectionError() :
                        resources.getString(R.string.calibration_unsuccessful);
                (Toast.makeText(CameraCalibrationActivity.this, resultMessage, Toast.LENGTH_SHORT)).show();
                
                if (menuArrayAdapter.getCount() > CALIB_MESSAGE) {
                    menuArrayAdapter.remove(menuArrayAdapter.getItem(CALIB_MESSAGE));
                }
                menuArrayAdapter.insert(resultMessage, CALIB_MESSAGE);
                menuArrayAdapter.notifyDataSetChanged();

                if (calibrator.isCalibrated()) {
                    CalibrationResult.save(CameraCalibrationActivity.this,
                            calibrator.getCameraMatrix(), calibrator.getDistortionCoefficients(), matrixWidth, matrixHeight);
                    saveCalibration();
                    startGameButton.setVisibility(View.VISIBLE);
                    guideText.setText(R.string.text_calibrationFinished);
                }
                CameraCalibrationActivity.this.calibrator = calibrator;
            }
        }.execute();
    }

    private void saveCalibration() {

        Mat cameraMatrix = calibrator.getCameraMatrix();
        Mat distortionCoefficients = calibrator.getDistortionCoefficients();

        double[] cameraMatrixArray = new double[CalibrationResult.CAMERA_MATRIX_ROWS * CalibrationResult.CAMERA_MATRIX_COLS];
        cameraMatrix.get(0, 0, cameraMatrixArray);

        double[] distortionCoefficientsArray = new double[CalibrationResult.DISTORTION_COEFFICIENTS_SIZE];
        distortionCoefficients.get(0, 0, distortionCoefficientsArray);

        ArrayList<Float> reprojectionErrorArrayList = calibrator.getReprojectionErrorArrayList();
        float averageRepError = 0;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (int i = 0; i <= reprojectionErrorArrayList.size() -1; i++) {
            averageRepError +=(float) reprojectionErrorArrayList.get(i);
            if(reprojectionErrorArrayList.get(i) < min){
                min = reprojectionErrorArrayList.get(i);
            }
            if(reprojectionErrorArrayList.get(i) > max){
                max = reprojectionErrorArrayList.get(i);
            }
        }
        averageRepError /= reprojectionErrorArrayList.size();

        CameraCalibrationActivity.nativeSaveParam(cameraMatrixArray, distortionCoefficientsArray, matrixWidth, matrixHeight,averageRepError, min, max);
    }

    private void guideModeManagement() {
        calibrator.clearCorners();
        if (!GUIDE_MODE) {
            GUIDE_MODE = true;
            progress.setVisibility(View.VISIBLE);
            guideText.setText(R.string.guidemode_intro_text);

            GuideMode guide = new GuideMode(matrixWidth, matrixHeight, this);
            guide.registerCalibrationGuideListener(this);
            renderer.setCalibrationGuide(guide);
            guideButton.setBackgroundResource(R.drawable.hexagon_gray);
        } else {
            GUIDE_MODE = false;
            progress.setVisibility(View.INVISIBLE);
            final String picsTaken = getResources().getQuantityString(R.plurals.numberOfPicturesTaken,
                    calibrator.getCornersBufferSize(), calibrator.getCornersBufferSize());
            guideText.setText(picsTaken);
            renderer.setCalibrationGuide(null);
            guideButton.setBackgroundResource(R.drawable.hexagon);
        }
    }

    private boolean isCalibrationAvailable() {

        if (calibrator.isCalibrated()) {
            return true;
        } else {
            (Toast.makeText(this, getResources().getString(R.string.more_samples), Toast.LENGTH_SHORT)).show();
        }
        return false;
    }

    public void picAddedMessage(final int cornerBufferListSize) {
        final String picsTaken = getResources().getQuantityString(R.plurals.numberOfPicturesTaken, cornerBufferListSize, cornerBufferListSize);

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                guideText.setText(picsTaken);
                if (cornerBufferListSize == 10 && !GUIDE_MODE) {
                    startCalibrationButton.startAnimation(fadeInAnimation);
                }
            }
        });
    }

    @Override
    public ARRenderer supplyRenderer() {
        return null;
    }

    @Override
    public FrameLayout supplyFrameLayout() {
        return null;
    }

    @Override
    public void calibrationGuideFinish() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startCalibration(calibrator);
            }
        });
    }

    private String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
