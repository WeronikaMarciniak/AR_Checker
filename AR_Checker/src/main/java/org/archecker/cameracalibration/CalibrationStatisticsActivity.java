package org.archecker.cameracalibration;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import org.archecker.R;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class CalibrationStatisticsActivity extends Activity implements OnChartValueSelectedListener, View.OnClickListener {

    private BarChart calibrationChart;
    private ArrayList<Float> reprojectionArray;
    private int value = -1;
    private ImageButton RemoveFrameButton;
    private ImageButton startCalibration;
    private View calibrationDetail;
    private CameraCalibrator calibrator;
    private int resultCode = RESULT_CANCELED;
    private List<Float> removedItems = new ArrayList<>();
    private List<Mat> removedFrames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_details);
        calibrationDetail = this.findViewById(R.id.view_camera_calibration_detail);
        calibrationDetail.setOnClickListener(this);
        RemoveFrameButton = (ImageButton) this.findViewById(R.id.button_calibDelete);
        RemoveFrameButton.setOnClickListener(this);
        ImageButtonHandle.setImageButtonEnabled(this.getApplicationContext(),false, RemoveFrameButton,R.drawable.ic_delete_forever_white_24px);
        startCalibration = (ImageButton) this.findViewById(R.id.button_calibDeleteConfirm);
        startCalibration.setOnClickListener(this);
        ImageButtonHandle.setImageButtonEnabled(this.getApplicationContext(),false, startCalibration,R.drawable.ic_done_white_24px);
        calibrationChart = (BarChart) findViewById(R.id.chart);
        calibrationChart.setOnChartValueSelectedListener(this);
        calibrator = (CameraCalibrator) getIntent().getExtras().getSerializable(CameraCalibrationActivity.INTENT_EXTRA_CAMERA_CALIBRATOR);
        reprojectionArray = calibrator.getReprojectionErrorArrayList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        generateChart(reprojectionArray);
    }
    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("TAG","Selected value" + e.getX() + " : " + calibrationChart.getHighlighted()[0].getX());
        ImageButtonHandle.setImageButtonEnabled(this.getApplicationContext(),true, RemoveFrameButton,R.drawable.ic_delete_forever_white_24px);
    }

    @Override
    public void onNothingSelected() {
        ImageButtonHandle.setImageButtonEnabled(this.getApplicationContext(),false, RemoveFrameButton,R.drawable.ic_delete_forever_white_24px);
    }

    @Override
    public void onClick(View view) {
        if(view.equals(RemoveFrameButton)){
            if(Math.round(calibrationChart.getHighlighted()[0].getX()) >=0) {
                ImageButtonHandle.setImageButtonEnabled(this.getApplicationContext(),true, startCalibration,R.drawable.ic_done_white_24px);
                int selectedValueX = Math.round(calibrationChart.getHighlighted()[0].getX());
                calibrationChart.highlightValue(-1, -1, true);
                Log.i("TAG","Selected value" + selectedValueX);
                calibrationChart.getData().getDataSetByIndex(0).removeEntryByXPos(selectedValueX);
                calibrationChart.invalidate();
                removedFrames.add(calibrator.getFrame(selectedValueX));
                removedItems.add(reprojectionArray.get(selectedValueX));
            }
            else{
                Toast toast = Toast.makeText(this, R.string.error_no_selection, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        if(view.equals(startCalibration)){
            ImageButtonHandle.setImageButtonEnabled(this.getApplicationContext(),false, startCalibration,R.drawable.ic_done_white_24px);
            resultCode = RESULT_OK;
            applyModification();
            generateChart(reprojectionArray);
            Toast toast = Toast.makeText(this, R.string.error_values_removed, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    private void generateChart(List<Float> reprojectionArray) {
        List<BarEntry> entries = new ArrayList<>();
        float averageRepError = 0;

        for (int i = 0; i <= reprojectionArray.size() -1; i++) {
            entries.add(new BarEntry(i, (Float) reprojectionArray.get(i)));
            averageRepError +=(float) reprojectionArray.get(i);
        }
        averageRepError /= reprojectionArray.size();
        BarDataSet dataSet = new BarDataSet(entries, "Label"); // add entries to dataset
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barData.setValueTextSize(10f);
        calibrationChart.setFitBars(true);
        calibrationChart.setData(barData);
        calibrationChart.setDescription("");
        calibrationChart.getLegend().setEnabled(false);
        YAxis axisLeft = calibrationChart.getAxisLeft();
        axisLeft.setEnabled(false);
        axisLeft.setDrawGridLines(false);
        calibrationChart.getAxisRight().setEnabled(false);
        calibrationChart.getXAxis().setEnabled(false);

        LimitLine ll = new LimitLine(averageRepError, (float) (Math.round(averageRepError * 1000))/1000 + " " + getString(R.string.string_averageReproErr));
        ll.setLineColor(ContextCompat.getColor(this, R.color.highlight_artk_light));
        ll.setLineWidth(1f);
        ll.setTextColor(Color.BLACK);
        ll.setTextSize(8f);
        ll.enableDashedLine(40,20,0);
        axisLeft.removeAllLimitLines();
        axisLeft.addLimitLine(ll);

        calibrationChart.setScaleEnabled(false);
        calibrationChart.setDrawGridBackground(false);
        calibrationChart.invalidate(); // refresh
    }

    private void applyModification() {
        for (Float item: removedItems) {
            reprojectionArray.remove(item);
        }

        for (Mat frame: removedFrames){
            calibrator.removeFrame(frame);
        }

        removedItems = new ArrayList<>();
        removedFrames = new ArrayList<>();
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        if(resultCode == RESULT_OK) {
            resultIntent.putExtra(CameraCalibrationActivity.INTENT_EXTRA_CAMERA_CALIBRATOR, calibrator);
        }
        setResult(resultCode, resultIntent);
        super.finish();
    }


}
