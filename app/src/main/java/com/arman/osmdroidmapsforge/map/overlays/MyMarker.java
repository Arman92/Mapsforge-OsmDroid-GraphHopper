package com.arman.osmdroidmapsforge.map.overlays;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.MarkerInfoWindow;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;


/**
 * Created by Arman on 7/10/2015.
 */

public class MyMarker extends Marker {
    protected Drawable mIcon;
    protected GeoPoint mPosition;
    protected float mBearing;
    protected float mAnchorU;
    protected float mAnchorV;
    protected float mIWAnchorU;
    protected float mIWAnchorV;
    protected float mAlpha;
    protected boolean mDraggable;
    protected boolean mIsDragged;
    protected boolean mFlat;
    protected MyMarker.OnMarkerClickListener mOnMarkerClickListener;
    protected MyMarker.OnMarkerDragListener mOnMarkerDragListener;
    protected Drawable mImage;
    protected boolean mPanToView;
    protected Object mRelatedObject;
    protected Point mPositionPixels;
    protected static MarkerInfoWindow mDefaultInfoWindow = null;
    protected static Drawable mDefaultIcon = null;
    public static final float ANCHOR_CENTER = 0.5F;
    public static final float ANCHOR_LEFT = 0.0F;
    public static final float ANCHOR_TOP = 0.0F;
    public static final float ANCHOR_RIGHT = 1.0F;
    public static final float ANCHOR_BOTTOM = 1.0F;


    private long mDbPoiId = -1;

    public MyMarker(MapView mapView) {
        this(mapView, new DefaultResourceProxyImpl(mapView.getContext()));
    }
    public MyMarker(MapView mapView, ResourceProxy resourceProxy) {
        super(mapView);
        this.mBearing = 0.0F;
        this.mAlpha = 1.0F;
        this.mPosition = new GeoPoint(0.0D, 0.0D);
        this.mAnchorU = 0.5F;
        this.mAnchorV = 0.5F;
        this.mIWAnchorU = 0.5F;
        this.mIWAnchorV = 0.0F;
        this.mDraggable = false;
        this.mIsDragged = false;
        this.mPositionPixels = new Point();
        this.mPanToView = true;
        this.mFlat = false;
        this.mOnMarkerClickListener = null;
        this.mOnMarkerDragListener = null;
        if(mDefaultIcon == null) {
            mDefaultIcon = resourceProxy.getDrawable(ResourceProxy.bitmap.marker_default);
        }

        this.mIcon = mDefaultIcon;
//        if(mDefaultInfoWindow == null || mDefaultInfoWindow.mMapView != mInstance) {
//            Context context = mInstance.getContext();
//            String packageName = context.getPackageName();
//            int defaultLayoutResId = context.getResources().getIdentifier("bonuspack_bubble", "layout", packageName);
//            if(defaultLayoutResId == 0) {
//                Log.e("BONUSPACK", "Marker: layout/bonuspack_bubble not found in " + packageName);
//            } else {
//                mDefaultInfoWindow = new MarkerInfoWindow(defaultLayoutResId, mInstance);
//            }
//        }

        this.setInfoWindow(mDefaultInfoWindow);
    }

    public void setDbPoiId(long dbPoiId)
    {
        mDbPoiId = dbPoiId;
    }

    public long getDbPoiId()
    {
        return mDbPoiId;
    }

    public void setIcon(Drawable icon) {
        if(icon != null) {
            this.mIcon = icon;
        } else {
            this.mIcon = mDefaultIcon;
        }

    }

    public GeoPoint getPosition() {
        return this.mPosition;
    }

    public void setPosition(GeoPoint position) {
        this.mPosition = position.clone();
    }

    public float getRotation() {
        return this.mBearing;
    }

    public void setRotation(float rotation) {
        this.mBearing = rotation;
    }

    public void setAnchor(float anchorU, float anchorV) {
        this.mAnchorU = anchorU;
        this.mAnchorV = anchorV;
    }

    public void setInfoWindowAnchor(float anchorU, float anchorV) {
        this.mIWAnchorU = anchorU;
        this.mIWAnchorV = anchorV;
    }

    public void setAlpha(float alpha) {
        this.mAlpha = alpha;
    }

    public float getAlpha() {
        return this.mAlpha;
    }

    public void setDraggable(boolean draggable) {
        this.mDraggable = draggable;
    }

    public boolean isDraggable() {
        return this.mDraggable;
    }

    public void setFlat(boolean flat) {
        this.mFlat = flat;
    }

    public boolean isFlat() {
        return this.mFlat;
    }

    public void remove(MapView mapView) {
        mapView.getOverlays().remove(this);
    }

    public void setOnMarkerClickListener(MyMarker.OnMarkerClickListener listener) {
        this.mOnMarkerClickListener = listener;
    }

    public void setOnMarkerDragListener(MyMarker.OnMarkerDragListener listener) {
        this.mOnMarkerDragListener = listener;
    }

    public void setImage(Drawable image) {
        this.mImage = image;
    }

    public Drawable getImage() {
        return this.mImage;
    }

    public void setInfoWindow(MarkerInfoWindow infoWindow) {
        this.mInfoWindow = infoWindow;
    }

    public void setPanToView(boolean panToView) {
        this.mPanToView = panToView;
    }

    public void setRelatedObject(Object relatedObject) {
        this.mRelatedObject = relatedObject;
    }

    public Object getRelatedObject() {
        return this.mRelatedObject;
    }

    public void showInfoWindow() {
        if(this.mInfoWindow != null) {
            boolean markerWidth = false;
            boolean markerHeight = false;
            int markerWidth1 = this.mIcon.getIntrinsicWidth();
            int markerHeight1 = this.mIcon.getIntrinsicHeight();
            int offsetX = (int)(this.mIWAnchorU * (float)markerWidth1) - (int)(this.mAnchorU * (float)markerWidth1);
            int offsetY = (int)(this.mIWAnchorV * (float)markerHeight1) - (int)(this.mAnchorV * (float)markerHeight1);
            this.mInfoWindow.open(this, this.mPosition, offsetX, offsetY);
        }
    }

//    public boolean isInfoWindowShown() {
//        if(this.mInfoWindow instanceof MarkerInfoWindow) {
//            MarkerInfoWindow iw = (MarkerInfoWindow)this.mInfoWindow;
//            return iw != null && iw.isOpen() && iw.mMarkerRef == this;
//        } else {
//            return super.isInfoWindowOpen();
//        }
//    }

    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if(!shadow) {
            if(this.mIcon != null) {
                Projection pj = mapView.getProjection();
                pj.toPixels(this.mPosition, this.mPositionPixels);
                int width = this.mIcon.getIntrinsicWidth();
                int height = this.mIcon.getIntrinsicHeight();
                Rect rect = new Rect(0, 0, width, height);
                rect.offset(-((int)(this.mAnchorU * (float)width)), -((int)(this.mAnchorV * (float)height)));
                this.mIcon.setBounds(rect);
                this.mIcon.setAlpha((int)(this.mAlpha * 255.0F));
                float rotationOnScreen = this.mFlat?-this.mBearing:mapView.getMapOrientation() - this.mBearing;
                drawAt(canvas, this.mIcon, this.mPositionPixels.x, this.mPositionPixels.y, false, rotationOnScreen);
            }
        }
    }

    public boolean hitTest(MotionEvent event, MapView mapView) {
        Projection pj = mapView.getProjection();
        pj.toPixels(this.mPosition, this.mPositionPixels);
        Rect screenRect = pj.getIntrinsicScreenRect();
        int x = -this.mPositionPixels.x + screenRect.left + (int)event.getX();
        int y = -this.mPositionPixels.y + screenRect.top + (int)event.getY();
        boolean hit = this.mIcon.getBounds().contains(x, y);
        return hit;
    }

    public boolean onSingleTapConfirmed(MotionEvent event, MapView mapView) {
        boolean touched = this.hitTest(event, mapView);
        return touched?(this.mOnMarkerClickListener == null?this.onMarkerClickDefault(this, mapView):this.mOnMarkerClickListener.onMarkerClick(this, mapView)):touched;
    }

    public void moveToEventPosition(MotionEvent event, MapView mapView) {
        Projection pj = mapView.getProjection();
        this.mPosition = (GeoPoint)pj.fromPixels((int)event.getX(), (int)event.getY());
        mapView.invalidate();
    }

    public boolean onLongPress(MotionEvent event, MapView mapView) {
        boolean touched = this.hitTest(event, mapView);
        if(touched && this.mDraggable) {
            this.mIsDragged = true;
            this.closeInfoWindow();
            if(this.mOnMarkerDragListener != null) {
                this.mOnMarkerDragListener.onMarkerDragStart(this);
            }

            this.moveToEventPosition(event, mapView);
        }

        return touched;
    }

    public boolean onTouchEvent(MotionEvent event, MapView mapView) {
        boolean touched = this.hitTest(event, mapView);

        if(this.mDraggable) {
            if(this.mIsDragged && event.getAction() == MotionEvent.ACTION_UP) {
                this.mIsDragged = false;
                if(this.mOnMarkerDragListener != null) {
                    this.mOnMarkerDragListener.onMarkerDragEnd(this);
                }

                return true;
            }

            else if(event.getAction() == MotionEvent.ACTION_MOVE) {
                if (touched && this.mDraggable && !this.mIsDragged) {
                    this.mIsDragged = true;
                    this.closeInfoWindow();
                    if (this.mOnMarkerDragListener != null) {
                        this.mOnMarkerDragListener.onMarkerDragStart(this);
                    }

                    this.moveToEventPosition(event, mapView);
                    return true;
                }


//                this.moveToEventPosition(event, mInstance);
//                if(this.mOnMarkerDragListener != null) {
//                    this.mOnMarkerDragListener.onMarkerDrag(this);
//                }
                return true;

            } else {
                return false;
            }
        }
//        else if (touched && this.mDraggable && event.getAction() == MotionEvent.ACTION_DOWN)
//        {
//
//        }
        else {
            return false;
        }
    }

    protected boolean onMarkerClickDefault(MyMarker marker, MapView mapView) {
        marker.showInfoWindow();
        if(marker.mPanToView) {
            mapView.getController().animateTo(marker.getPosition());
        }

        return true;
    }

    public interface OnMarkerClickListener {
        boolean onMarkerClick(MyMarker var1, MapView var2);
    }

    public interface OnMarkerDragListener {
        void onMarkerDrag(MyMarker var1);

        void onMarkerDragEnd(MyMarker var1);

        void onMarkerDragStart(MyMarker var1);
    }
}
