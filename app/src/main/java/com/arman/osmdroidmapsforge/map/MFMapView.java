package com.arman.osmdroidmapsforge.map;


import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.Toast;

import com.arman.osmdroidmapsforge.map.overlays.MyLocationNewOverlay;
import com.arman.osmdroidmapsforge.map.overlays.RotationGestureOverlay;

import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import java.util.List;

/**
 * Created by Arman on 5/14/2015.
 */
public class MFMapView extends org.osmdroid.views.MapView implements LocationListener {
    public static LocationManager locManager;
    private RotationGestureOverlay mRotationGestureOverlay;
    static GpsMyLocationProvider gpsLocationProvider;
    static MyLocationNewOverlay myLocationNewOverlay;
    private static boolean isCurrentPosLayerAdded = false;
    Marker longPressMarker;
    protected int rotationMode = 1; // 0 : Up to North, 1: equal to movement bearing
    boolean followMyLocation = true;


    public MFMapView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initializeMap();
    }
    public MFMapView(final Context context, final int tileSizePixels) {
        super(context, tileSizePixels);
        initializeMap();

    }

    public MFMapView(final Context context, final int tileSizePixels,
                     final ResourceProxy resourceProxy) {
        super(context, tileSizePixels, resourceProxy, null);
        initializeMap();

    }

    public MFMapView(final Context context, final int tileSizePixels,
                     final ResourceProxy resourceProxy, final MapTileProviderBase aTileProvider) {
        super(context, tileSizePixels, resourceProxy, aTileProvider, null);
        initializeMap();

    }

    public MFMapView(final Context context, final int tileSizePixels,
                     final ResourceProxy resourceProxy, final MapTileProviderBase aTileProvider,
                     final Handler tileRequestCompleteHandler) {
        super(context, tileSizePixels, resourceProxy, aTileProvider, tileRequestCompleteHandler,
                null);
        initializeMap();

    }

    protected void initializeMap()
    {

        this.setMultiTouchControls(true);

//        UnComment these lines to add a scale bar to your map:

//        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(this.getContext());
//        scaleBarOverlay.drawLatitudeScale(true);
//        scaleBarOverlay.drawLongitudeScale(true);
//        mapView.getOverlays().add(scaleBarOverlay);


        mRotationGestureOverlay = new RotationGestureOverlay(this.getContext(), this);
        mRotationGestureOverlay.setEnabled(true);


        this.getOverlays().add(this.mRotationGestureOverlay);




        setLocationListener();

        this.invalidate();
    }

    public void setLocationListener()
    {
        locManager = (LocationManager)this.getContext().getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 1f, this);
    }

    public void addMyLocationOverlay()
    {
        if (!isCurrentPosLayerAdded) {
            gpsLocationProvider = new GpsMyLocationProvider(this.getContext());

            gpsLocationProvider.setLocationUpdateMinTime(2);
            gpsLocationProvider.setLocationUpdateMinDistance(5);
            myLocationNewOverlay = new MyLocationNewOverlay(this.getContext(), gpsLocationProvider, this);
            myLocationNewOverlay.enableMyLocation(gpsLocationProvider);
//            You can set info window for current location, it opens by clicking on it's marker
//          myLocationNewOverlay.setInfoWindow(
//                new CurrentPosInfoWindow(R.layout.bubble_new_poi, this, myLocationNewOverlay));
            myLocationNewOverlay.enableCompass();


            this.getOverlays().add( myLocationNewOverlay);
            isCurrentPosLayerAdded = true;
            this.invalidate();
        }
    }

    public void setCurrentAsCenter()
    {
        if (locManager == null)
            locManager = (LocationManager)this.getContext().getSystemService(Context.LOCATION_SERVICE);


        Location location = getLastBestLocation(getContext());
        if (location != null)
        {
            getController().animateTo(new GeoPoint(location));
            this.getController().setZoom(15);
        }
        else
        {
            Location lastBestLocation = getLastBestLocation(getContext());
            if (lastBestLocation != null)
            {
                getController().animateTo(new GeoPoint(lastBestLocation));
                this.getController().setZoom(15);
            }
            else
            {
                Toast.makeText(this.getContext(), "Searching for your location", Toast.LENGTH_LONG).show();
                this.getController().setCenter(new GeoPoint(32.653906, 51.659088)); // Iran, Isfahan
                this.getController().setZoom(15);
            }
            try
            {
                locManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 5000, 20, this);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }

    public static Location getLastBestLocation(Context _context)
    {
        if (locManager == null)
            locManager = (LocationManager)_context.getSystemService(Context.LOCATION_SERVICE);

        Location bestResult = null;

        float bestAccuracy = Float.MAX_VALUE;

        // Iterate through all the providers on the system, keeping
        // note of the most accurate result within the acceptable time limit.
        // If no result is found within maxTime, return the newest Location.
        List<String> matchingProviders = locManager.getAllProviders();
        for (String provider: matchingProviders) {
            Location location = locManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                if ( accuracy < bestAccuracy) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                }
            }
        }

        return bestResult;
    }

    protected void setCurrentPositionMarker(Location location, boolean setCenter) {
        if (!isCurrentPosLayerAdded) {

            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 1f, this);

            this.addMyLocationOverlay();

        } else {
            if (followMyLocation)
                myLocationNewOverlay.enableFollowLocation();
            else
                myLocationNewOverlay.disableFollowLocation();

            myLocationNewOverlay.enableMyLocation();
        }

        if (setCenter && location != null) {
            this.getController().animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()));

            this.getController().setZoom(15);
        }
        this.invalidate();
    }


    @Override
    public void onLocationChanged(Location location) {
        if (followMyLocation)
        {
            setCurrentPositionMarker(location, true);
        }
        else
            setCurrentPositionMarker(location, false);

        if (rotationMode == 1 && location.hasBearing() &&  location.getSpeed() * 3.6 > 10)
        {
            this.setMapOrientation(360 - location.getBearing());
        }


    }
    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }
}
