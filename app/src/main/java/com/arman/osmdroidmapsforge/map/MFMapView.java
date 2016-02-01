package com.arman.osmdroidmapsforge.map;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.arman.osmdroidmapsforge.Helper;
import com.arman.osmdroidmapsforge.R;
import com.arman.osmdroidmapsforge.map.overlays.MyLocationNewOverlay;
import com.arman.osmdroidmapsforge.map.overlays.RotationGestureOverlay;
import com.arman.osmdroidmapsforge.routing.RouteCalculator;
import com.arman.osmdroidmapsforge.routing.RoutingSession;

import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import java.util.ArrayList;
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
    boolean isRoutingStuffAdded = false;
    public static MFMapView mInstance;
    private static Context mContext;


    protected Polyline mRoadOverlay;
    Polyline mTrackingLineOverlay;

    protected FolderOverlay mRoadNodeMarkers;
    static final int OSRM=0, GRAPHHOPPER_FASTEST=1, GRAPHHOPPER_BICYCLE=2, GRAPHHOPPER_PEDESTRIAN=3, GOOGLE_FASTEST=4;
    RouteCalculator mRouteCalculator;
    int mWhichRouteProvider;
    public static Road mRoad; //made static to pass between activities
    boolean routingZoomToBoundingBox = true;

    Dialog longPressDlg;
    Marker startRouteMarker;
    ArrayList<Marker> intermediateMarkers;
    Marker endRouteMarker;

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
        mInstance = this;
        mContext = getContext();

//        UnComment these lines to add a scale bar to your map:

//        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(this.getContext());
//        scaleBarOverlay.drawLatitudeScale(true);
//        scaleBarOverlay.drawLongitudeScale(true);
//        mapView.getOverlays().add(scaleBarOverlay);


        mRotationGestureOverlay = new RotationGestureOverlay(this.getContext(), this);
        mRotationGestureOverlay.setEnabled(true);


        this.getOverlays().add(this.mRotationGestureOverlay);

        mRoadNodeMarkers = new FolderOverlay(mContext);
        mRoadNodeMarkers.setName("Route Steps");


        mWhichRouteProvider = GRAPHHOPPER_FASTEST;
        mRouteCalculator = new RouteCalculator(mContext);
        mRouteCalculator.loadGraphStorage();



        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this.getContext(), new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint arg0) {
                if (longPressMarker != null)
                    longPressMarker.closeInfoWindow();



                return false;
            }

            @Override
            public boolean longPressHelper(final GeoPoint position) {
                if (isRoutingStuffAdded) {
                    if (longPressDlg == null) {
                        longPressDlg = new Dialog(getContext());
                        longPressDlg.setTitle("Choose one");
                        longPressDlg.setContentView(R.layout.dlg_lng_press_choice);
                    }
                    if (intermediateMarkers == null) {
                        intermediateMarkers = new ArrayList<Marker>();
                    }

                    Button btn_set_start = (Button) longPressDlg.findViewById(R.id.btn_set_start);
                    Button btn_set_end = (Button) longPressDlg.findViewById(R.id.btn_set_end);
                    Button btn_add_poi = (Button) longPressDlg.findViewById(R.id.btn_add_poi);
                    Button btn_add_intermediate_point = (Button) longPressDlg.findViewById(R.id.btn_add_intermediate_point);

                    if (endRouteMarker != null)
                        btn_add_intermediate_point.setVisibility(View.VISIBLE);
                    else
                        btn_add_intermediate_point.setVisibility(View.GONE);

                    btn_add_intermediate_point.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            longPressDlg.dismiss();

                            final Marker myMarker = new Marker(mInstance);
                            myMarker.setPosition(position);
                            myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            myMarker.setIcon(getResources().getDrawable(R.drawable.marker_green));
//                            myMarker.setDraggable(true);
                            myMarker.setInfoWindow(null);
//                            myMarker.setOnMarkerDragListener(new MyMarker.OnMarkerDragListener() {
//                                @Override
//                                public void onMarkerDrag(MyMarker marker) {
//                                }
//                                @Override
//                                public void onMarkerDragEnd(MyMarker marker) {
//                                    checkForRouteDrawRequest();
//                                }
//                                @Override
//                                public void onMarkerDragStart(MyMarker marker) {
//                                }
//                            });

                            myMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                                @Override
                                public boolean onMarkerClick(Marker var1, MapView var2) {
                                    AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                                    alertDialog.setTitle("Delete marker");
                                    alertDialog.setMessage("Are you sure?");
                                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    mInstance.getOverlays().remove(myMarker);
                                                    intermediateMarkers.remove(myMarker);
                                                    mInstance.invalidate();
                                                    dialog.dismiss();

                                                    checkForRouteDrawRequest();
                                                }
                                            });
                                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    alertDialog.show();
                                    return false;
                                }
                            });

                            intermediateMarkers.add(myMarker);
                            mInstance.getOverlays().add(myMarker);
                            mInstance.invalidate();

                            checkForRouteDrawRequest();
                        }
                    });

                    btn_set_start.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (startRouteMarker != null)
                                mInstance.getOverlays().remove(startRouteMarker);

                            startRouteMarker = new Marker(mInstance);
                            startRouteMarker.setPosition(position);
                            startRouteMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            startRouteMarker.setIcon(getResources().getDrawable(R.drawable.marker_blue));
//                            startRouteMarker.setDraggable(true);
                            startRouteMarker.setInfoWindow(null);
//                            startRouteMarker.setOnMarkerDragListener(new MyMarker.OnMarkerDragListener() {
//                                @Override
//                                public void onMarkerDrag(MyMarker marker) {
//                                }
//
//                                @Override
//                                public void onMarkerDragEnd(MyMarker marker) {
//                                    checkForRouteDrawRequest();
//                                }
//
//                                @Override
//                                public void onMarkerDragStart(MyMarker marker) {
//                                }
//                            });

                            mInstance.getOverlays().add(startRouteMarker);
                            mInstance.invalidate();

                            checkForRouteDrawRequest();

                            longPressDlg.dismiss();
                        }
                    });

                    btn_set_end.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (endRouteMarker != null)
                                mInstance.getOverlays().remove(endRouteMarker);

                            endRouteMarker = new Marker(mInstance);
                            endRouteMarker.setPosition(position);
                            endRouteMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            endRouteMarker.setIcon(getResources().getDrawable(R.drawable.marker_red));
//                            endRouteMarker.setDraggable(true);
                            endRouteMarker.setInfoWindow(null);
//                            endRouteMarker.setOnMarkerDragListener(new MyMarker.OnMarkerDragListener() {
//                                @Override
//                                public void onMarkerDrag(MyMarker marker) {
//
//                                }
//
//                                @Override
//                                public void onMarkerDragEnd(MyMarker marker) {
//                                    checkForRouteDrawRequest();
//                                }
//
//                                @Override
//                                public void onMarkerDragStart(MyMarker marker) {
//
//                                }
//                            });

                            mInstance.getOverlays().add(endRouteMarker);
                            mInstance.invalidate();

                            checkForRouteDrawRequest();

                            longPressDlg.dismiss();

                        }
                    });

                    btn_add_poi.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });

                    longPressDlg.show();
                }

                else {
                    if (endRouteMarker != null) {
                        endRouteMarker.closeInfoWindow();
                        mInstance.getOverlays().remove(endRouteMarker);
                    }


                    endRouteMarker = new Marker(mInstance);
                    endRouteMarker.setPosition(position);
                    endRouteMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                    endRouteMarker.setIcon(getResources().getDrawable(R.drawable.marker_red));

                    mInstance.getOverlays().add(endRouteMarker);

                    getController().animateTo(position);

                    mInstance.invalidate();


                    checkForRouteDrawRequest();
                }

                return false;
            }
        });

        this.getOverlays().add(0, mapEventsOverlay);

        setLocationListener();

        this.invalidate();
    }

    private void checkForRouteDrawRequest()
    {
        ArrayList<GeoPoint> geoPoints = new ArrayList<GeoPoint>();

        if (startRouteMarker != null)
        {
            geoPoints.add(startRouteMarker.getPosition());
        }

        if (startRouteMarker != null && endRouteMarker != null) {
            if (intermediateMarkers != null)
                for (Marker myMarker : intermediateMarkers) {
                    geoPoints.add(myMarker.getPosition());
                }
            geoPoints.add(endRouteMarker.getPosition());

            if (isRoutingStuffAdded)
                removeRoutingStuffFromUi();
            calculateAndShowRoute(geoPoints, true);
        }
    }

    public void calculateAndShowRoute(ArrayList<GeoPoint> points, boolean showStartEndMarkers)
    {
        UpdateRoadTask updateRoadTask = new UpdateRoadTask(showStartEndMarkers);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            updateRoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, points);
        else
            updateRoadTask.execute(points);
    }

    private UpdateRoadTask updateRoadTask;
    private class UpdateRoadTask extends AsyncTask<ArrayList<GeoPoint>, Void, Road> {
        private ArrayList<GeoPoint> mPoints;
        boolean mShowStartEndMarkers;
        public UpdateRoadTask(boolean showStartEndMarkers)
        {
            mShowStartEndMarkers = showStartEndMarkers;
        }

        protected void onPreExecute() {
            Toast.makeText(mContext, "Calculating best route...", Toast.LENGTH_LONG).show();
        }

        protected Road doInBackground(ArrayList<GeoPoint>... points) {
            mPoints = points[0];
            return Helper.getRoutingRoad(mContext, RoutingSession.getRouteCalculator(mContext),
                    mWhichRouteProvider, points[0]);
        }

        protected void onPostExecute(Road result) {
            if (mShowStartEndMarkers)
                setRoutingStart_EndMarkers(mPoints.get(0), mPoints.get(mPoints.size() - 1));
           updateUIWithRoad(result);

            updateRoadTask = null;
//            getPOIAsync(poiTagText.getText().toString());
        }
    }

    public void updateUIWithRoad(Road road){
        isRoutingStuffAdded = true;
        mRoad = road;

        mRoadNodeMarkers.getItems().clear();

        List<Overlay> mapOverlays = this.getOverlays();
        if (mRoadOverlay != null){
            mapOverlays.remove(mRoadOverlay);
            mRoadOverlay = null;
        }
        if (road == null)
            return;
        if (road.mStatus == Road.STATUS_TECHNICAL_ISSUE) {
            Toast.makeText(this.getContext(), "Technical error in calculating route", Toast.LENGTH_SHORT).show();
        }
        else if (road.mStatus > Road.STATUS_TECHNICAL_ISSUE) //functional issues
            Toast.makeText(this.getContext(), "No routes found", Toast.LENGTH_SHORT).show();
        mRoadOverlay = buildRoadOverlay(road, Color.parseColor("#004eff"), 7.0f, this.getContext());
        String routeDesc = road.getLengthDurationText(-1);
        mRoadOverlay.setTitle("path" + " - " + routeDesc);
        mapOverlays.add(0, mRoadOverlay);
        //we dinsert the road overlay at the "bottom", just above the MapEventsOverlay,
        //to avoid covering the other overlays.

        if (routingZoomToBoundingBox)
            this.zoomToBoundingBox(road.mBoundingBox);
        putRoadNodes(road);
        this.invalidate();

    }

    private void putRoadNodes(Road road){
        mInstance.getOverlays().remove(mRoadNodeMarkers);
        mRoadNodeMarkers.getItems().clear();
        Drawable icon = getResources().getDrawable(R.drawable.marker_node);
        int n = road.mNodes.size();
        MarkerInfoWindow infoWindow = new MarkerInfoWindow(R.layout.bouspack_bubble, this);
        TypedArray iconIds = getResources().obtainTypedArray(R.array.direction_icons);
        for (int i=0; i<n; i++){
            RoadNode node = road.mNodes.get(i);
            String instructions = (node.mInstructions==null ? "" : node.mInstructions);
            Marker nodeMarker = new Marker(this);
            nodeMarker.setTitle("Step"+ " " + (i+1));
            nodeMarker.setSnippet(instructions);
            nodeMarker.setSubDescription(Road.getLengthDurationText(node.mLength, node.mDuration));
            nodeMarker.setPosition(node.mLocation);
            nodeMarker.setIcon(icon);
            nodeMarker.setInfoWindow(infoWindow); //use a shared infowindow.
            int iconId = iconIds.getResourceId(node.mManeuverType, R.drawable.ic_empty);
            if (iconId != R.drawable.ic_empty){
                Drawable image = getResources().getDrawable(iconId);
                nodeMarker.setImage(image);
            }
            mRoadNodeMarkers.add(nodeMarker);

        }
        mInstance.getOverlays().add(1, mRoadNodeMarkers);
        iconIds.recycle();


        if (mRoadNodeMarkers != null && mRoadOverlay != null)
        {
            if (mRoad.mNodes.size() > 0) {
                RoadNode node = mRoad.mNodes.get(0);


                String distance;
                if (node.mLength < 1)
                    distance = String.format("%dm", (int) (node.mLength * 1000));
                else
                    distance = String.format("%.1fkm", node.mLength);


            }
        }
    }

    public static Polyline buildRoadOverlay(Road road, int color, float width, Context context){
        Polyline roadOverlay = new Polyline(context);
        roadOverlay.setColor(color);
        roadOverlay.setWidth(width);

        if (road != null) {
            ArrayList<GeoPoint> polyline = road.mRouteHigh;
            roadOverlay.setPoints(polyline);
        }
        return roadOverlay;
    }

    public void setRoutingStart_EndMarkers(GeoPoint startPoint, GeoPoint endPoint)
    {
        if (endRouteMarker != null) {
            endRouteMarker.closeInfoWindow();
            mInstance.getOverlays().remove(endRouteMarker);
        }
        endRouteMarker = new Marker(mInstance);
        endRouteMarker.setPosition(endPoint);
        endRouteMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        endRouteMarker.setIcon(getResources().getDrawable(R.drawable.marker_red));

        if (startRouteMarker != null) {
            startRouteMarker.closeInfoWindow();
            mInstance.getOverlays().remove(startRouteMarker);
        }
        startRouteMarker = new Marker(mInstance);
        startRouteMarker.setPosition(startPoint);
        startRouteMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startRouteMarker.setIcon(getResources().getDrawable(R.drawable.marker_blue));

        mInstance.getOverlays().add(startRouteMarker);
        mInstance.getOverlays().add(endRouteMarker);
    }

    public void removeRoutingStuffFromUi()
    {
        isRoutingStuffAdded = false;
        mInstance.getOverlays().remove(mRoadOverlay);
        mInstance.getOverlays().remove(mRoadNodeMarkers);
        mInstance.getOverlays().remove(endRouteMarker);
        mInstance.getOverlays().remove(startRouteMarker);
        if (intermediateMarkers != null)
            mInstance.getOverlays().removeAll(intermediateMarkers);
        startRouteMarker = null;
        endRouteMarker = null;

        mInstance.invalidate();
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
