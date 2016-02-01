package com.arman.osmdroidmapsforge.routing;

/**
 * Created by Arman on 7/10/2015.
 */
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;

import java.io.File;
import java.util.List;


public class RouteCalculator {

    private GraphHopper hopper;
    private volatile boolean shortestPathRunning;
    private volatile boolean prepareInProgress = false;
    private Context mContext;

    public RouteCalculator(Context context)
    {
        mContext = context;
    }

    public void loadGraphStorage()
    {
        prepareInProgress = true;
        try {
//        logUser("loading graph (" + Constants.VERSION + ") ... ");
            new GHAsyncTask<Void, Void, Path>() {
                protected Path saveDoInBackground(Void... v) throws Exception {
                    GraphHopper tmpHopp = new GraphHopper().forMobile();
                    tmpHopp.setCHEnable(true);
                    tmpHopp.load(new File(Environment.getExternalStorageDirectory() + "/DolphinLocationApp"
                            + "/Routing").getAbsolutePath());
                    log("found graph " + tmpHopp.getGraph().toString() + ", nodes:" + tmpHopp.getGraph().getNodes());
                    hopper = tmpHopp;
                    return null;
                }

                protected void onPostExecute(Path o) {
                    if (hasError()) {
                        Log.i("", "An error happend while creating graph:"
                                + getErrorMessage());
                    } else {
                        Log.i("", "Finished loading graph. Press long to define where to start and end the route.");
                    }

                    finishPrepare();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        catch (Exception e)
        {

        }
    }

    public void loadGraphStorageSync () {

        GraphHopper tmpHopp = new GraphHopper().forMobile();
        tmpHopp.setCHEnable(true);
        tmpHopp.load(new File(Environment.getExternalStorageDirectory() + "/DolphinLocationApp"
                + "/Routing").getAbsolutePath());
        log("found graph " + tmpHopp.getGraph().toString() + ", nodes:" + tmpHopp.getGraph().getNodes());
        hopper = tmpHopp;

    }

    private void finishPrepare()
    {
        prepareInProgress = false;
    }

    public boolean isPrepareInProgress()
    {
        return prepareInProgress;
    }


    public GHResponse calculatePath( final List<GHPoint> points)
    {
//        StopWatch sw = new StopWatch().start();

        GHRequest req = new GHRequest(points).
                setAlgorithm(AlgorithmOptions.DIJKSTRA_BI);
        req.getHints().
                put("instructions", "true");
        GHResponse resp = null;
        if (hopper != null)
            resp = hopper.route(req);

        return resp;
    }

    public AsyncTask<List<GHPoint>, Void, GHResponse> CalcRouteTask = new AsyncTask<List<GHPoint>, Void, GHResponse>()
    {
        float time;

        protected GHResponse doInBackground( List<GHPoint> ... points )
        {
            StopWatch sw = new StopWatch().start();

            GHRequest req = new GHRequest(points[0]).
                    setAlgorithm(AlgorithmOptions.DIJKSTRA_BI);
            req.getHints().
                    put("instructions", "false");
            GHResponse resp = hopper.route(req);
            time = sw.stop().getSeconds();
            return resp;
        }

        protected void onPostExecute( GHResponse resp )
        {
            if (!resp.hasErrors())
            {
                //                    log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                //                            + toLon + " found path with distance:" + resp.getDistance()
                //                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                //                            + time + " " + resp.getDebugInfo());
                //                    log("the route is " + (int) (resp.getDistance() / 100) / 10f
                //                            + "km long, time:" + resp.getMillis() / 60000f + "min, debug:" + time);

                //                    mInstance.getLayerManager().getLayers().add(createPolyline(resp));
                //mInstance.redraw();
            } else
            {
                log("Error:" + resp.getErrors());
            }
            shortestPathRunning = false;
        }
    };


//    private Polyline createPolyline( GHResponse response )
//    {
//        Paint paintStroke = AndroidGraphicFactory.INSTANCE.createPaint();
//        paintStroke.setStyle(Style.STROKE);
//        paintStroke.setColor(Color.BLUE);
//        paintStroke.setDashPathEffect(new float[]
//                {
//                        25, 15
//                });
//        paintStroke.setStrokeWidth(8);
//
//        // TODO: new mapsforge version wants an mapsforge-paint, not an android paint.
//        // This doesn't seem to support transparceny
//        //paintStroke.setAlpha(128);
//        Polyline line = new Polyline((org.mapsforge.core.graphics.Paint) paintStroke, AndroidGraphicFactory.INSTANCE);
//        List<LatLong> geoPoints = line.getLatLongs();
//        PointList tmp = response.getPoints();
//        for (int i = 0; i < response.getPoints().getSize(); i++)
//        {
//
//            geoPoints.add(new LatLong(tmp.getLatitude(i), tmp.getLongitude(i)));
//        }
//
//        return line;
//    }

    private void log(String log)
    {}

}
