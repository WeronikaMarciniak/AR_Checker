package org.opencv.samples.archecker.menu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.samples.archecker.CameraCalibrationActivity;
import org.opencv.samples.archecker.R;

import java.util.ArrayList;

public class MenuArrayAdapter extends ArrayAdapter<String>{

    private final int mMenuResource;

    public MenuArrayAdapter(CameraCalibrationActivity cameraCalibrationActivity, ArrayList<String> strings) {
        super(cameraCalibrationActivity, R.layout.menu_list_item,strings);
        mMenuResource = R.layout.menu_list_item;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = LayoutInflater.from(parent.getContext()).inflate(mMenuResource, parent, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.menuItem_Image);
        TextView textView = (TextView) view.findViewById(R.id.menuItem_Text);

        int artImage;

        switch (position) {
            case CameraCalibrationActivity.COMPARE_MENU_POS:
                artImage = R.drawable.ic_compare_black_24px;
                textView.setText(R.string.comparison);
                break;
            case CameraCalibrationActivity.SETTINGS_MENU_POS:
                artImage = R.drawable.ic_tune_black_24px;
                textView.setText(R.string.menuItem_settings);
                break;
            case CameraCalibrationActivity.UNDISTORETED_VIDEO_POS:
                artImage = R.drawable.ic_photo_filter_black_24px;
                textView.setText(R.string.undistorted);
                break;
            case CameraCalibrationActivity.NEW_CALIBRATION:
                artImage = R.drawable.ic_transform_black_24px;
                textView.setText(R.string.calibration);
                break;
            case CameraCalibrationActivity.SHARE_POS:
                artImage = R.drawable.ic_share_black_24px;
                textView.setText(R.string.share_calibration);
                break;
            case CameraCalibrationActivity.HELP_POS:
                artImage = R.drawable.ic_help_outline_black_24px;
                textView.setText(R.string.help);
                break;
            case CameraCalibrationActivity.CALIB_MESSAGE_POS:
                String message = this.getItem(position);
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_list_item_calib_result, parent, false);
                textView = (TextView) view.findViewById(R.id.textView_calibrationResult);
                textView.setText(message);
                artImage = -1;
                break;
//            case CameraCalibrationActivity.PRINT_POS:
//                artImage = R.drawable.ic_cloud_print;
//                textView.setText(R.string.prefCategory_title_printing);
//                break;
            case CameraCalibrationActivity.CALIB_STATS:
                textView.setText(R.string.calib_stats);
                artImage = R.drawable.ic_chart_black_24px;
                break;
            default:
                return null;
        }
        if(artImage >0)
            imageView.setImageResource(artImage);
        return view;
    }
}
