package org.opencv.samples.archecker;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class PawnsDetector implements IPawnsDetector {
    Mat src;
    Mat template;
    CvCameraViewFrame inputFrame;
    @Override
    public void initialize(){
        if (src.empty())
            return;
        if(template == null){
            Mat templ = Imgcodecs.imread("blackpawn.png", Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
            template = new Mat(templ.size(), CvType.CV_32F);
            Imgproc.cvtColor(templ, template, Imgproc.COLOR_BGR2RGBA);
        }
    }
   @Override
    public Mat detectPawns() {

        src = inputFrame.rgba();
        initialize();
        int match_method = Imgproc.TM_SQDIFF;

        int result_cols = src.cols() - template.cols() + 1;
        int result_rows = src.rows() - template.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32F);

        Imgproc.matchTemplate(src, template, result, match_method);
     //  Imgproc.HoughCircles(result, src, Imgproc.HOUGH_GRADIENT, Math.PI/180, 150);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        MinMaxLocResult mmr = Core.minMaxLoc(result);

        Point matchLoc;
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
        } else {
            matchLoc = mmr.maxLoc;
        }

        Rect roi = new Rect((int) matchLoc.x, (int) matchLoc.y, template.cols(), template.rows());
        Imgproc.rectangle(src, new Point(roi.x, roi.y), new Point(roi.width - 2, roi.height - 2), new Scalar(255, 0, 0, 255), 2);
        return src;
    }
}