package com.arman.osmdroidmapsforge.map.utils;
import android.view.MotionEvent;

/**
 * Created by Arman on 2/20/2015.
 */
public class RotationGestureDetector {


    private double lastDelta = 0;
    protected float mRotation;
    private RotationListener mListener;

    public interface RotationListener {
        public void onRotate(float deltaAngle);
    }


    public RotationGestureDetector(RotationListener listener) {
        mListener = listener;
    }

    private static float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    public void onTouch(MotionEvent e) {
        if (e.getPointerCount() != 2)
            return;




        if (e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            mRotation = rotation(e);
        }

        float rotation = rotation(e);
        float delta = rotation - mRotation;
        mRotation += delta;

        if (lastDelta == 0)
            lastDelta = delta;

        if (((lastDelta < 0) != (delta < 0))) {
            lastDelta = delta;
            return;
        }
        lastDelta = delta;
        mListener.onRotate(delta);
    }


}
