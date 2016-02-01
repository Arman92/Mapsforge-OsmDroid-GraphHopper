package com.arman.osmdroidmapsforge;

import android.app.Application;
import android.content.Context;

import com.arman.osmdroidmapsforge.routing.RouteCalculator;


public class MyApplication extends Application {

    private static RouteCalculator mRouteCalculator;

    @Override
    public void onCreate() {
        super.onCreate();

        mRouteCalculator = new RouteCalculator(getApplicationContext());
        mRouteCalculator.loadGraphStorage();
    }

    public static RouteCalculator getRouteCalculator()
    {
        return mRouteCalculator;
    }


}