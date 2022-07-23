package org.opencv.samples.archecker;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public interface IPawnsDetector {
    void initialize();

    Mat detectPawns();
}
