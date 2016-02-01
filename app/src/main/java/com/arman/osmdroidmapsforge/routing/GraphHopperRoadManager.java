package com.arman.osmdroidmapsforge.routing;

/**
 * Created by Arman on 7/10/2015.
 */

import android.content.Context;
import android.util.Log;

import com.graphhopper.GHResponse;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint;

import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;

/** get a route between a start and a destination point, going through a list of waypoints.
 * It uses GraphHopper, an open source routing service based on OpenSteetMap data. <br>
 *
 * It requests by default the GraphHopper demo site.
 * Use setService() to request another (for instance your own) GraphHopper-compliant service. <br>
 *
 * @see <a href="https://github.com/graphhopper/web-api/blob/master/docs-routing.md">GraphHopper</a>
 * @author M.Kergall
 */
public class GraphHopperRoadManager extends RoadManager {


    public static final int STATUS_NO_ROUTE = Road.STATUS_TECHNICAL_ISSUE+1;
    public RouteCalculator mRouteCalculator;
    private Context mContext;

    protected boolean mWithElevation;

    /** mapping from GraphHopper directions to MapQuest maneuver IDs: */
    static final HashMap<Integer, Integer> MANEUVERS;
    static {
        MANEUVERS = new HashMap<Integer, Integer>();
        MANEUVERS.put(0, 1); //Continue
        MANEUVERS.put(1, 6); //Slight right
        MANEUVERS.put(2, 7); //Right
        MANEUVERS.put(3, 8); //Sharp right
        MANEUVERS.put(-3, 5); //Sharp left
        MANEUVERS.put(-2, 4); //Left
        MANEUVERS.put(-1, 3); //Slight left
        MANEUVERS.put(4, 24); //Arrived
        MANEUVERS.put(5, 24); //Arrived at waypoint
    }


    public GraphHopperRoadManager(Context context){
        super();
        mWithElevation = false;
        this.mContext = context;
    }



    /** set if altitude of every route point should be requested or not. Default is false. */
    public void setElevation(boolean withElevation){
        mWithElevation = withElevation;
    }



    @Override public Road getRoad(ArrayList<GeoPoint> waypoints) {

        if (mRouteCalculator == null) {
            mRouteCalculator = new RouteCalculator(mContext);
            mRouteCalculator.loadGraphStorage();
        }
        ArrayList<GHPoint> points = new ArrayList<GHPoint>();
        for (int i =0; i < waypoints.size(); i++)
        {
            points.add(new GHPoint(waypoints.get(i).getLatitude(), waypoints.get(i).getLongitude()));
        }

        GHResponse response = mRouteCalculator.calculatePath(points);



        Road road = new Road();
        try {

            if (response.hasErrors()){
                road = new Road(waypoints);
                road.mStatus = STATUS_NO_ROUTE;
                return road;
            }

            ArrayList<GeoPoint> allPoints = new ArrayList<GeoPoint>();
            PointList tmpPointList = response.getPoints();
            for (int i = 0; i < tmpPointList.getSize(); i++)
            {
                allPoints.add(new GeoPoint(tmpPointList.getLatitude(i), tmpPointList.getLongitude(i)));
            }
            road.mRouteHigh = allPoints;

            int n = response.getInstructions().getSize();
            for (int i = 0; i < n; i++){
                Instruction instruction = response.getInstructions().get(i);

                RoadNode node = new RoadNode();
                //JSONArray jInterval = instruction
                // int positionIndex = jInterval.getInt(0);
                node.mLocation = new GeoPoint(instruction.getPoints().getLatitude(0),
                        instruction.getPoints().getLongitude(0));


                node.mLength = instruction.getDistance()/1000.0;
                node.mDuration = instruction.getTime()/1000.0; //Segment duration in seconds.
                int direction = instruction.getSign();
                node.mManeuverType = getManeuverCode(direction);
                String instructionStr = "";
                switch (direction) {
                    case 0:
                        instructionStr += "مستقیم بروید";
                        break;
                    case 1:
                        instructionStr += "کمی به راست بچیپید";
                        break;
                    case 2:
                        instructionStr += "به راست بپیچید";
                        break;
                    case 3:
                        instructionStr += "کامل به راست بپیچید";
                        break;
                    case -1:
                        instructionStr += "کمی به چپ بپیچید";
                        break;
                    case -2:
                        instructionStr += "به چپ بپیچید";
                        break;
                    case -3:
                        instructionStr += "کامل به چپ بچیپید";
                        break;
                    case 4:
                        instructionStr += "به مقصد رسیدید";
                        break;
                    case 5:
                        instructionStr += "به نقطه ی میانی رسیدید";
                        break;
                }


                if (instruction.getName().equals(""))
                {
                    node.mInstructions = instructionStr + " " + instruction.getName();
                }
                else
                    node.mInstructions = instructionStr + " به " + instruction.getName();

                road.mNodes.add(node);
            }
            road.mLength = response.getDistance()/1000.0;
            road.mDuration = response.getMillis()/1000.0;

            BBox bBox = response.calcRouteBBox(new BBox(32.00, 33.99, 51.00, 53.00));
            road.mBoundingBox = new BoundingBoxE6(bBox.maxLat, bBox.maxLon, bBox.minLat, bBox.minLon);
            road.mStatus = Road.STATUS_OK;

        }
        catch (Exception e) {
            road.mStatus = Road.STATUS_TECHNICAL_ISSUE;
            e.printStackTrace();
        }
        if (road.mStatus != Road.STATUS_OK){
            //Create default road:
            int status = road.mStatus;
            road = new Road(waypoints);
            road.mStatus = status;
        } else {
            road.buildLegs(waypoints);
        }
        Log.d(BonusPackHelper.LOG_TAG, "GraphHopper.getRoad - finished");
        return road;
    }

    protected int getManeuverCode(int direction){
        Integer code = MANEUVERS.get(direction);
        if (code != null)
            return code;
        else
            return 0;
    }

}
