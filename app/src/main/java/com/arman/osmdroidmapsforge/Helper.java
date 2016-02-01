package com.arman.osmdroidmapsforge;

import android.content.Context;
import android.os.AsyncTask;

import com.arman.osmdroidmapsforge.routing.GraphHopperRoadManager;
import com.arman.osmdroidmapsforge.routing.RouteCalculator;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Arman on 2016-02-01.
 */
public class Helper {
    public static final int OSRM=0, GRAPHHOPPER_FASTEST=1, GRAPHHOPPER_BICYCLE=2, GRAPHHOPPER_PEDESTRIAN=3, GOOGLE_FASTEST=4;
    public interface AsyncTaskEventListener<Result>{
        void onPreExecute();
        void onPostExecute(Result result);
    }



    public static BoundingBoxE6 calculateBBox(List<GeoPoint> points)
    {
        double minLat = Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double maxLng = Double.MIN_VALUE;

        for (GeoPoint geoPoint : points)
        {
            if (geoPoint.getLatitude() < minLat)
                minLat = geoPoint.getLatitude();
            if (geoPoint.getLongitude() < minLng)
                minLng = geoPoint.getLongitude();

            if (geoPoint.getLatitude() > maxLat)
                maxLat = geoPoint.getLatitude();
            if (geoPoint.getLongitude() > maxLng)
                maxLng = geoPoint.getLongitude();
        }
        return new BoundingBoxE6(maxLat, maxLng, minLat, minLng);
    }

    public static Road getRoutingRoad(Context context, RouteCalculator routeCalculator,
                                      int whichRouteProvider, ArrayList<GeoPoint> waypoints)
    {
        RoadManager roadManager = null;
        Locale locale = new Locale("fa", "IR");
        switch (whichRouteProvider) {
            case OSRM:
//                    roadManager = new OSRMRoadManager();
                break;
            case GRAPHHOPPER_FASTEST:
                roadManager = new GraphHopperRoadManager(context);
                ((GraphHopperRoadManager)roadManager).mRouteCalculator = routeCalculator;
                roadManager.addRequestOption("locale=" + locale.getLanguage());

                break;
            case GRAPHHOPPER_BICYCLE:
                roadManager = new GraphHopperRoadManager(context);
                ((GraphHopperRoadManager)roadManager).mRouteCalculator = routeCalculator;
                roadManager.addRequestOption("locale=" + locale.getLanguage());
                roadManager.addRequestOption("vehicle=bike");
                //((GraphHopperRoadManager)roadManager).setElevation(true);
                break;
            case GRAPHHOPPER_PEDESTRIAN:
                roadManager = new GraphHopperRoadManager(context);
                ((GraphHopperRoadManager)roadManager).mRouteCalculator = routeCalculator;
                roadManager.addRequestOption("locale=" + locale.getLanguage());
                roadManager.addRequestOption("vehicle=foot");
                //((GraphHopperRoadManager)roadManager).setElevation(true);
                break;
            case GOOGLE_FASTEST:
//                    roadManager = new GoogleRoadManager();
                break;
            default:
                return null;
        }
        return roadManager.getRoad(waypoints);
    }

    public static class GetRoadAsync extends AsyncTask<Object, Integer, Road>
    {
        AsyncTaskEventListener<Road> mAsyncTaskEventListener;
        public GetRoadAsync(AsyncTaskEventListener<Road> asyncTaskEventListener)
        {
            this.mAsyncTaskEventListener = asyncTaskEventListener;
        }
        public GetRoadAsync()
        {}
        @Override
        public Road doInBackground(Object... params)
        {
            return getRoutingRoad((Context)params[0], (RouteCalculator)params[1],
                    (Integer)params[2], (ArrayList<GeoPoint>)params[3]);
        }
        @Override
        public void onPostExecute(Road result)
        {
            if (mAsyncTaskEventListener != null)
                mAsyncTaskEventListener.onPostExecute(result);
        }
        @Override
        public void onPreExecute()
        {
            if (mAsyncTaskEventListener != null)
                mAsyncTaskEventListener.onPreExecute();
        }
    }

}
