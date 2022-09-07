package org.archecker.share;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import org.archecker.R;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ShareActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = ShareActivity.class.getSimpleName();
    private ShareAdapter shareAdapter;
    private ArrayList<File> calibsList;
    private ImageButton deleteButton;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File selectedFile = calibsList.get(position);
        Uri contentUri = FileProvider.getUriForFile(this, "org.artoolkitx.arx.fileprovider", selectedFile);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("application/octet-stream");
        sharingIntent.putExtra(Intent.EXTRA_STREAM,contentUri);
        startActivity(Intent.createChooser(sharingIntent,getResources().getString(R.string.share_text)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        ListView calibrationsListView = (ListView) findViewById(R.id.shareList);
        File calibsFile = new File(this.getCacheDir().getAbsolutePath()+"/calibs");
        Log.d(TAG,"calibsFile: " + calibsFile.toString());
        calibsList = new ArrayList<>(Arrays.asList(calibsFile.listFiles()));
        shareAdapter = new ShareAdapter(this,
                calibsList);
        calibrationsListView.setAdapter(shareAdapter);
        calibrationsListView.setOnItemClickListener(this);
        deleteButton = (ImageButton) findViewById(R.id.button_shareDelete);
        deleteButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        ArrayList<File> filesToRemove = new ArrayList<>();
        if(v== deleteButton){
            for (File file: calibsList){
                file.delete();
                filesToRemove.add(file);
            }
            calibsList.removeAll(filesToRemove);
        }
        shareAdapter.notifyDataSetChanged();
    }
}
