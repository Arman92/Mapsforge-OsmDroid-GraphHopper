package com.arman.osmdroidmapsforge;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.arman.osmdroidmapsforge.map.MFMapView;
import com.arman.osmdroidmapsforge.map.MFTileModuleProvider;
import com.arman.osmdroidmapsforge.map.MFTileSource;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.math.BigInteger;


public class MainActivity extends ActionBarActivity implements IRegisterReceiver {
    MFMapView mapView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AndroidGraphicFactory.createInstance(this.getApplication());


        try {
            ResourceProxy resourceProxy = new DefaultResourceProxyImpl(this);

            // Create a custom tile source
            final ITileSource tileSource = new MFTileSource(8,20, 256,
                    Environment.getExternalStorageDirectory() + "/DolphinLocationApp"
                    + "/Mapsforge/map/iran.map",
                    Environment.getExternalStorageDirectory() + "/DolphinLocationApp"
                            + "/Mapsforge/renderthemes/detailed.xml"
                    , this);
//
            final IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(this);



            MFTileModuleProvider moduleProvider;
            moduleProvider = new MFTileModuleProvider(this, new File(""), (MFTileSource)tileSource);



            MapTileModuleProviderBase[] pTileProviderArray;
            pTileProviderArray = new MapTileModuleProviderBase[] { moduleProvider};


            final MapTileProviderArray tileProviderArray = new MapTileProviderArray(
                    tileSource, registerReceiver, pTileProviderArray);

            mapView = new MFMapView(this, tileProviderArray.getTileSource().getTileSizePixels(),
                    resourceProxy, tileProviderArray);


            mapView.setBuiltInZoomControls(false);
            mapView.setMultiTouchControls(true);


            final LinearLayout mapLinearLayout = (LinearLayout) this.findViewById(R.id.mapLinearLayout);

            mapLinearLayout.addView(mapView);


            mapView.setCurrentAsCenter();
            mapView.addMyLocationOverlay();

            mapView.setBuiltInZoomControls(true);


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
