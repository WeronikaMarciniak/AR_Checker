package org.opencv.samples.archecker.share;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.opencv.samples.archecker.R;

import java.io.File;
import java.util.ArrayList;

class ShareCalibrationsArrayAdapter extends ArrayAdapter<File> {
    private final int mMenuResource;
    private final ArrayList<File> mCacheFiles;

    public ShareCalibrationsArrayAdapter(ShareActivity shareActivity, ArrayList<File> cacheFiles) {
        super(shareActivity,R.layout.share_list_item,cacheFiles);
        this.mMenuResource = R.layout.share_list_item;
        this.mCacheFiles = cacheFiles;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        if(convertView == null){
            viewHolder = new ViewHolder();
            convertView =  LayoutInflater.from(parent.getContext()).inflate(mMenuResource, parent, false);
            viewHolder.shareItemTextView = (TextView) convertView.findViewById(R.id.shareItem_Text);
            convertView.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String calibFileText = mCacheFiles.get(position).getName();
        viewHolder.shareItemTextView.setText(calibFileText);
        return convertView;
    }

    private static class ViewHolder{
        TextView shareItemTextView;
    }
}
