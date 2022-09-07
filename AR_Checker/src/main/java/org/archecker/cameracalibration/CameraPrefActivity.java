package org.archecker.cameracalibration;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

import org.archecker.R;

import java.util.List;

public class CameraPrefActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "CameraPrefActivity";
    private static final PixelSizeToAspectRatio aspectRatios[] = new PixelSizeToAspectRatio[]{
            new PixelSizeToAspectRatio(1, 1, ASPECT_RATIO._1_1, "1:1"),
            new PixelSizeToAspectRatio(11, 9, ASPECT_RATIO._11_9, "11:9"),
            new PixelSizeToAspectRatio(5, 4, ASPECT_RATIO._5_4, "5:4"),
            new PixelSizeToAspectRatio(4, 3, ASPECT_RATIO._4_3, "4:3"),
            new PixelSizeToAspectRatio(3, 2, ASPECT_RATIO._3_2, "3:2"),
            new PixelSizeToAspectRatio(14, 9, ASPECT_RATIO._14_9, "14:9"),
            new PixelSizeToAspectRatio(8, 5, ASPECT_RATIO._8_5, "8:5"),
            new PixelSizeToAspectRatio(5, 3, ASPECT_RATIO._5_3, "5:3"),
            new PixelSizeToAspectRatio(16, 9, ASPECT_RATIO._16_9, "16:9"),
            new PixelSizeToAspectRatio(9, 5, ASPECT_RATIO._9_5, "9:5"),
            new PixelSizeToAspectRatio(17, 9, ASPECT_RATIO._17_9, "17:9"),
            new PixelSizeToAspectRatio(683, 384, ASPECT_RATIO._16_9, "16:9"),
            new PixelSizeToAspectRatio(85, 48, ASPECT_RATIO._16_9, "16:9"),
            new PixelSizeToAspectRatio(256, 135, ASPECT_RATIO._17_9, "17:9"),
            new PixelSizeToAspectRatio(512, 307, ASPECT_RATIO._5_3, "5:3"),
            new PixelSizeToAspectRatio(30, 23, ASPECT_RATIO._4_3, "4:3"),
            new PixelSizeToAspectRatio(128, 69, ASPECT_RATIO._17_9, "17:9"),
            new PixelSizeToAspectRatio(30, 23, ASPECT_RATIO._11_9, "11:9"),
    };
    public static final String PREF_CAMERA_INDEX = "pref_cameraIndex";
    public static final String PREF_CAMERA_RESOLUTION = "pref_cameraResolution";
    public static final String PREF_PAPER_SIZE = "pref_paperSize";
    private ListPreference cameraIndexPreference;
    private ListPreference cameraResolutionPreference;
    private ListPreference paperSizePreference;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageManager pm = this.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            finish();
            return;
        }

        addPreferencesFromResource(R.xml.preferences);
        cameraIndexPreference = (ListPreference) findPreference(PREF_CAMERA_INDEX);
        cameraResolutionPreference = (ListPreference) findPreference(PREF_CAMERA_RESOLUTION);
        paperSizePreference = (ListPreference) findPreference(PREF_PAPER_SIZE);

        int cameraCount;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            cameraCount = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraCountFront = 0;
            int cameraCountRear = 0;
            CharSequence[] entries = new CharSequence[cameraCount];
            CharSequence[] entryValues = new CharSequence[cameraCount];
            for (int camIndex = 0; camIndex < cameraCount; camIndex++) {
                Camera.getCameraInfo(camIndex, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    cameraCountFront++;
                    entries[camIndex] = "Front camera";
                    if (cameraCountFront > 1)
                        entries[camIndex] = entries[camIndex] + " " + cameraCountFront;
                } else {
                    cameraCountRear++;
                    entries[camIndex] = "Rear camera";
                    if (cameraCountRear > 1)
                        entries[camIndex] = entries[camIndex] + " " + cameraCountRear;
                }
                entryValues[camIndex] = Integer.toString(camIndex);
            }
            cameraIndexPreference.setEnabled(true);
            cameraIndexPreference.setEntries(entries);
            cameraIndexPreference.setEntryValues(entryValues);
        } else {
            cameraCount = 1;
            cameraIndexPreference.setEnabled(false);
        }

        buildResolutionListForCameraIndex();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void buildResolutionListForCameraIndex() {
        int camIndex = Integer.parseInt(cameraIndexPreference.getValue());

        Camera cam;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                cam = Camera.open(camIndex);
            else
                cam = Camera.open();

            Camera.Parameters params = cam.getParameters();
            List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
            cam.release();
            String camResolution = cameraResolutionPreference.getValue();
            boolean foundCurrentResolution = false;
            CharSequence[] entries = new CharSequence[previewSizes.size()];
            CharSequence[] entryValues = new CharSequence[previewSizes.size()];
            for (int i = 0; i < previewSizes.size(); i++) {
                int w = previewSizes.get(i).width;
                int h = previewSizes.get(i).height;
                entries[i] = w + "x" + h + "   (" + findAspectRatioName(w, h)
                        + ")";
                entryValues[i] = w + "x" + h;
                if (entryValues[i].equals(camResolution))
                    foundCurrentResolution = true;
            }
            cameraResolutionPreference.setEntries(entries);
            cameraResolutionPreference.setEntryValues(entryValues);

            if (!foundCurrentResolution) {
                cameraResolutionPreference.setValue(entryValues[0].toString());
                cameraResolutionPreference.setSummary(cameraResolutionPreference.getEntry());
            }

        } catch (RuntimeException e) {
            Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();

        cameraIndexPreference.setSummary(cameraIndexPreference.getEntry());
        cameraResolutionPreference.setSummary(cameraResolutionPreference.getEntry());

        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        switch (key) {
            case PREF_CAMERA_INDEX:
                cameraIndexPreference.setSummary(cameraIndexPreference.getEntry());
                buildResolutionListForCameraIndex();
                break;
            case PREF_CAMERA_RESOLUTION:
                cameraResolutionPreference.setSummary(cameraResolutionPreference.getEntry());
                break;
            case PREF_PAPER_SIZE:
                paperSizePreference.setSummary(paperSizePreference.getEntry());
                break;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }


    private String findAspectRatioName(int w, int h) {

        int primes[] = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43,
                47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};
        int w_lcd = w, h_lcd = h;
        for (int i : primes) {
            while (w_lcd >= i && h_lcd >= i && w_lcd % i == 0 && h_lcd % i == 0) {
                w_lcd /= i;
                h_lcd /= i;
            }
        }

        for (PixelSizeToAspectRatio aspectRatio : aspectRatios) {
            if (w_lcd == aspectRatio.width && h_lcd == aspectRatio.height)
                return aspectRatio.name;
        }
        return (w + ":" + h);
    }

    public enum ASPECT_RATIO {
        _1_1,
        _11_9,
        _5_4,
        _4_3,
        _3_2,
        _14_9,
        _8_5,
        _5_3,
        _16_9,
        _9_5,
        _17_9,
        _UNIQUE
    }

    private static final class PixelSizeToAspectRatio {
        final int width;
        final int height;
        final ASPECT_RATIO aspectRatio;
        final String name;

        PixelSizeToAspectRatio(int w, int h, ASPECT_RATIO ar, String name) {
            this.width = w;
            this.height = h;
            this.aspectRatio = ar;
            this.name = name;
        }
    }
}