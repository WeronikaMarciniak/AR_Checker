package org.archecker.artoolkitgame;

import android.app.Application;

import org.artoolkit.ar.base.assets.AssetHelper;

public class ARGameApplication extends Application {

	private static Application sInstance;

    public static Application getInstance() {
    	return sInstance;
    }
    
    @Override
    public void onCreate() {
    	super.onCreate(); 
    	sInstance = this;
    	((ARGameApplication) sInstance).initializeInstance();
    }

    protected void initializeInstance() {
		AssetHelper assetHelper = new AssetHelper(getAssets());        
		assetHelper.cacheAssetFolder(getInstance(), "Data");
    }
}
