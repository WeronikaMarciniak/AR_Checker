package org.archecker.artoolkitgame;
import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import org.artoolkit.ar.base.rendering.ARRenderer;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.archecker.R;

public class StartGameActivity extends Activity implements CvCameraViewListener2{
    private static final String TAG ="src" ;
    private JavaCameraView cameraView;
    private ARObjectRenderer renderer = new ARObjectRenderer();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (JavaCameraView) findViewById(R.id.cameraview);
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();
        cameraView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                renderer.click();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
    protected ARRenderer supplyRenderer() {
        return new ARObjectRenderer();
    }
    protected JavaCameraView supplyCameraView() {
        return (JavaCameraView)this.findViewById(R.id.cameraview);
    }
}
