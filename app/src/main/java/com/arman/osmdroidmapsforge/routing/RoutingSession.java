package com.arman.osmdroidmapsforge.routing;

import android.content.Context;

import com.arman.osmdroidmapsforge.MyApplication;


/**
 * Created by Arman on 8/1/2015.
 */
public class RoutingSession {
    public static RouteCalculator getRouteCalculator(Context context)
    {
        return ((MyApplication)context.getApplicationContext()).getRouteCalculator();
    }
}
