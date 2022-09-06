package org.archecker.imageprocessing;
import android.os.Bundle;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ChessboardProcessing {
    public static Mat straightenImage(Mat image) {
        Mat gray=new Mat();
        Imgproc.cvtColor(image,gray,Imgproc.COLOR_BGR2GRAY);
        Mat edges = new Mat(image.rows(), image.cols(), image.type());
        Imgproc.Canny(gray,edges,550,100);
        List<MatOfPoint>  contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(
                edges, contours, hierarchy, Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE
        );
        List<MatOfInt> hull = new ArrayList<MatOfInt>();
        for(int i=0; i < contours.size(); i++){
            hull.add(new MatOfInt());

        }
        for(int i=0; i < contours.size(); i++){
            Imgproc.convexHull(contours.get(i), hull.get(i));
        }
        Mat drawing = Mat.zeros(gray.rows(), gray.cols(), CvType.CV_8UC1);
        Scalar color = new Scalar(255,0,0);
        List<Point[]> hullpoints = new ArrayList<Point[]>();
        for(int i=0; i < hull.size(); i++){
            Point[] points = new Point[hull.get(i).rows()];
            for(int j=0; j < hull.get(i).rows(); j++){
                int index = (int)hull.get(i).get(j, 0)[0];
                points[j] = new Point(contours.get(i).get(index, 0)[0], contours.get(i).get(index, 0)[1]);
            }

            hullpoints.add(points);
        }
        List<MatOfPoint> hullmop = new ArrayList<MatOfPoint>();
        for(int i=0; i < hullpoints.size(); i++){
            MatOfPoint mop = new MatOfPoint();
            mop.fromArray(hullpoints.get(i));
            hullmop.add(mop);
        }
        for(int i=0; i < contours.size(); i++){
            Imgproc.drawContours(drawing, contours, i, color);
            Imgproc.drawContours(drawing, hullmop, i, color);
        }
        double[] pt_A = {140, 130};
        double[]pt_B = {96, 500};
        double[]pt_C = {460,575};
        double[]pt_D = {500,70};

        double width_AD = Math.sqrt((Math.pow((pt_A[0] - pt_D[0]),2)) + Math.pow((pt_A[1] - pt_D[1]),2));
        double width_BC = Math.sqrt((Math.pow((pt_B[0] - pt_C[0]),2)) + Math.pow((pt_B[1] - pt_C[1]),2));
        double maxWidth = Math.max((int) width_AD, (int) width_BC);

        double height_AB = Math.sqrt((Math.pow((pt_A[0] - pt_B[0]),2)) + Math.pow((pt_A[1] - pt_B[1]),2));
        double height_CD = Math.sqrt((Math.pow((pt_C[0] - pt_D[0]),2)) + Math.pow((pt_C[1] - pt_D[1]),2));
        double maxHeight = Math.max((int) height_AB, (int) height_CD);

        Mat input_pts = new Mat(CvType.CV_32F);
        input_pts.put(0,0,pt_A);
        input_pts.put(0,1,pt_B);
        input_pts.put(0,2,pt_C);
        input_pts.put(0,3,pt_D);
        Mat output_pts = new Mat(CvType.CV_32F);
        double[] zeros={0,0};
        double[] maxH = {0, maxHeight - 1};
        double[] maxW = {maxWidth - 1, maxHeight - 1};
        double[] max={maxWidth - 1, 0};
        output_pts.put(0,0,zeros);
        output_pts.put(0,1,maxH);
        output_pts.put(0,2,maxW);
        output_pts.put(0,3,max);

        Mat M = Imgproc.getPerspectiveTransform(input_pts,output_pts);
        Size dsize= new Size(maxWidth, maxHeight);
        Mat out=new Mat();
        Imgproc.warpPerspective(image,out,M,dsize,Imgproc.INTER_LINEAR);
        return out;
    }
    public static Mat cropCheckerFields(Mat image){
        Mat gray=new Mat();
        Imgproc.cvtColor(image,gray,Imgproc.COLOR_BGR2GRAY);
        Mat img_binary  = new Mat(image.rows(), image.cols(), image.type());
        Imgproc.Canny(gray,img_binary,50,110);
        Mat dil_kernel = Mat.ones(3,3, CvType.CV_8UC1);
        Imgproc.dilate(img_binary,img_binary,dil_kernel);
        int line_min_width = 20;
        Mat kernel_h = Mat.ones(1,line_min_width, CvType.CV_8UC1);
        Mat img_binary_h = new Mat();
        Imgproc.morphologyEx(img_binary,img_binary_h, Imgproc.MORPH_OPEN, kernel_h);
        Mat kernel_v = Mat.ones(line_min_width,1, CvType.CV_8UC1);
        Mat img_binary_v =new Mat();
        Imgproc.morphologyEx(img_binary,img_binary_v, Imgproc.MORPH_OPEN, kernel_v);
        kernel_v = Mat.ones(line_min_width,1, CvType.CV_8UC1);
        Imgproc.morphologyEx(img_binary, img_binary_v,Imgproc.MORPH_OPEN, kernel_v);
        Mat labels = new Mat(),stats = new Mat(),centroids=new Mat();
        Rect ret;
        Imgproc.connectedComponentsWithStats(img_binary,labels,stats,centroids, 8, CvType.CV_32S);
        Mat croppedField = new Mat();
        stats.convertTo(stats,CvType.CV_16SC3);
        int size = (int) (stats.total() * stats.channels());
        for(int x=0,y=0,w=0,h=0,area=0;x<size;x++ , y++,w++ , h++, area++){
            if (area > 1000) {
                Imgproc.rectangle(image, new Point(x, y), new Point(x + w, y + h), new Scalar(0, 255, 0), 2);
                ret = new Rect(y, x,y+w,x+h);
                croppedField = image.submat(ret);
            }
        }

        return croppedField ;
    }
}
