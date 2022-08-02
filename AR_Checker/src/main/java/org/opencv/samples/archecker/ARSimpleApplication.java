package org.opencv.samples.archecker;

import android.app.Application;

import org.artoolkit.ar.base.assets.AssetHelper;

public class ARSimpleApplication extends Application {

	private static Application sInstance;

    public static Application getInstance() {
    	return sInstance;
    }
    
    @Override
    public void onCreate() {
    	super.onCreate(); 
    	sInstance = this;
    	((ARSimpleApplication) sInstance).initializeInstance();
    }

    protected void initializeInstance() {

		AssetHelper assetHelper = new AssetHelper(getAssets());        
		assetHelper.cacheAssetFolder(getInstance(), "Data");
    }
}
