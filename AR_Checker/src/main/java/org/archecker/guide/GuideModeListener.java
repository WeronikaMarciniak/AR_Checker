package org.archecker.guide;

import android.widget.FrameLayout;

import org.artoolkit.ar.base.rendering.ARRenderer;
import org.opencv.android.CameraBridgeViewBase;

public interface GuideModeListener {

    ARRenderer supplyRenderer();

   FrameLayout supplyFrameLayout();

    void calibrationGuideFinish();
}
