package com.arman.osmdroidmapsforge.map.overlays;


import java.util.LinkedList;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.SensorEventListenerProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.api.IMyLocationOverlay;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.IOverlayMenuProvider;
import org.osmdroid.views.overlay.Overlay.Snappable;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.FloatMath;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.arman.osmdroidmapsforge.R;


public class MyLocationNewOverlay extends Marker implements IMyLocationConsumer, IMyLocationOverlay,
        IOverlayMenuProvider, Snappable, SensorEventListener {

    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================

    protected final Paint mPaint = new Paint();
    protected final Paint mCirclePaint = new Paint();

    protected final Bitmap mIconBitmap;
    protected Drawable mIcon;
    protected final Bitmap mDirectionArrowBitmap;
    protected static Drawable mDefaultIcon = null; //cache for default icon (resourceProxy.getDrawable being slow)

    protected final MapView mMapView;

    private final IMapController mMapController;
    public IMyLocationProvider mMyLocationProvider;

    private final LinkedList<Runnable> mRunOnFirstFix = new LinkedList<Runnable>();
    private final Point mMapCoordsProjected = new Point();
    private final Point mMapCoordsTranslated = new Point();
    private final Handler mHandler;
    private final Object mHandlerToken = new Object();

    private Location mLocation;
    private final GeoPoint mGeoPoint = new GeoPoint(0, 0); // for reuse
    private boolean mIsLocationEnabled = false;
    protected boolean mIsFollowing = false; // follow location updates
    protected boolean mDrawAccuracyEnabled = true;

    /** Coordinates the feet of the person are located scaled for display density. */
    protected final PointF mPersonHotspot;

    protected final float mDirectionArrowCenterX;
    protected final float mDirectionArrowCenterY;

    public static final int MENU_MY_LOCATION = getSafeMenuId();

    private boolean mOptionsMenuEnabled = true;

    // to avoid allocations during onDraw
    private final float[] mMatrixValues = new float[9];
    private final Matrix mMatrix = new Matrix();
    private final Rect mMyLocationRect = new Rect();
    private final Rect mMyLocationPreviousRect = new Rect();

    protected float mAnchorU, mAnchorV;
    protected float mIWAnchorU, mIWAnchorV;
    protected OnMyLocationNewOverlayClickListener mOnMyLocationNewOverlayClickListener;
    protected OnMyLocationNewOverlayDragListener mOnMyLocationNewOverlayDragListener;
    protected Drawable mImage;
    protected Point mPositionPixels;
    protected boolean mDraggable, mIsDragged;
    protected  GeoPoint mPosition;

    protected final Picture mCompassFrame = new Picture();
    protected final Picture mCompassRose = new Picture();
    private final Matrix mCompassMatrix = new Matrix();
    private final Display mDisplay;
    /**
     * The bearing, in degrees east of north, or NaN if none has been set.
     */
    private float mAzimuth = Float.NaN;

    private float mCompassCenterX = 35.0f;
    private float mCompassCenterY = 35.0f;
    private final float mCompassRadius = 20.0f;

    protected final float COMPASS_FRAME_CENTER_X;
    protected final float COMPASS_FRAME_CENTER_Y;
    protected final float COMPASS_ROSE_CENTER_X;
    protected final float COMPASS_ROSE_CENTER_Y;
    public SensorEventListenerProxy mSensorListener = null;
    private final SensorManager mSensorManager;

    /** Usual values in the (U,V) coordinates system of the icon image */
    public static final float ANCHOR_CENTER=0.5f, ANCHOR_LEFT=0.0f, ANCHOR_TOP=0.0f, ANCHOR_RIGHT=1.0f, ANCHOR_BOTTOM=1.0f;
    // ===========================================================
    // Constructors
    // ===========================================================

    public MyLocationNewOverlay(Context context, MapView mapView) {
        this(context, new GpsMyLocationProvider(context), mapView);
    }

    public MyLocationNewOverlay(Context context, IMyLocationProvider myLocationProvider,
                                MapView mapView) {
        this(myLocationProvider, mapView, new DefaultResourceProxyImpl(context));
    }

    public MyLocationNewOverlay(IMyLocationProvider myLocationProvider, MapView mapView,
                                ResourceProxy resourceProxy) {
        super(mapView);

        mMapView = mapView;
        mMapController = mapView.getController();
        mCirclePaint.setARGB(0, 100, 100, 255);
        mCirclePaint.setAntiAlias(true);
        mPosition = new GeoPoint(0 ,0);
        mPositionPixels = new Point();

        mAnchorU = ANCHOR_CENTER;
        mAnchorV = ANCHOR_CENTER;

        final WindowManager windowManager = (WindowManager) this.mMapView.getContext().getSystemService(Context.WINDOW_SERVICE);
        mDisplay = windowManager.getDefaultDisplay();
        mSensorManager = (SensorManager) this.mMapView.getContext().getSystemService(Context.SENSOR_SERVICE);

        mPaint.setFilterBitmap(true);

        if (mDefaultIcon == null)
            mDefaultIcon = this.mMapView.getContext().getResources().getDrawable(R.drawable.current_pos);
        mIcon = mDefaultIcon;
        mIconBitmap = BitmapFactory.decodeResource(this.mMapView.getContext().getResources(),
                R.drawable.current_pos);
        mDirectionArrowBitmap = BitmapFactory.decodeResource(this.mMapView.getContext().getResources(),
                R.drawable.direction_arrow);

        mDirectionArrowCenterX = mDirectionArrowBitmap.getWidth() / 2.0f - 0.5f;
        mDirectionArrowCenterY = mDirectionArrowBitmap.getHeight() / 2.0f - 0.5f;

        // Calculate position of person icon's feet, scaled to screen density
        mPersonHotspot = new PointF(18.0f * mScale, 18.0f * mScale);

        mHandler = new Handler(Looper.getMainLooper());
        setMyLocationProvider(myLocationProvider);

        createCompassFramePicture();
        createCompassRosePicture();

        COMPASS_FRAME_CENTER_X = mCompassFrame.getWidth() / 2 - 0.5f;
        COMPASS_FRAME_CENTER_Y = mCompassFrame.getHeight() / 2 - 0.5f;
        COMPASS_ROSE_CENTER_X = mCompassRose.getWidth() / 2 - 0.5f;
        COMPASS_ROSE_CENTER_Y = mCompassRose.getHeight() / 2 - 0.5f;
    }

    private void invalidateCompass() {
        Rect screenRect = mMapView.getProjection().getScreenRect();
        final int frameLeft = screenRect.left + (mMapView.getWidth() / 2)
                + (int) Math.ceil((mCompassCenterX - COMPASS_FRAME_CENTER_X) * mScale);
        final int frameTop = screenRect.top + (mMapView.getHeight() / 2)
                + (int) Math.ceil((mCompassCenterY - COMPASS_FRAME_CENTER_Y) * mScale);
        final int frameRight = screenRect.left + (mMapView.getWidth() / 2)
                + (int) Math.ceil((mCompassCenterX + COMPASS_FRAME_CENTER_X) * mScale);
        final int frameBottom = screenRect.top + (mMapView.getHeight() / 2)
                + (int) Math.ceil((mCompassCenterY + COMPASS_FRAME_CENTER_Y) * mScale);

        // Offset by 2 to cover stroke width
        mMapView.postInvalidate(frameLeft - 2, frameTop - 2, frameRight + 2, frameBottom + 2);
    }

    public void setCompassCenter(final float x, final float y) {
        mCompassCenterX = x;
        mCompassCenterY = y;
    }

    protected void drawCompass(final Canvas canvas, final float bearing, final Rect screenRect) {
        final float centerX = mCompassCenterX * mScale;
        final float centerY = mCompassCenterY * mScale + (canvas.getHeight() - mMapView.getHeight());

        mCompassMatrix.setTranslate(-COMPASS_FRAME_CENTER_X, -COMPASS_FRAME_CENTER_Y);
        mCompassMatrix.postTranslate(centerX, centerY);

        canvas.save();
        canvas.setMatrix(mCompassMatrix);
        canvas.drawPicture(mCompassFrame);

        mCompassMatrix.setRotate(-bearing, COMPASS_ROSE_CENTER_X, COMPASS_ROSE_CENTER_Y);
        mCompassMatrix.postTranslate(-COMPASS_ROSE_CENTER_X, -COMPASS_ROSE_CENTER_Y);
        mCompassMatrix.postTranslate(centerX, centerY);

        canvas.setMatrix(mCompassMatrix);
        canvas.drawPicture(mCompassRose);
        canvas.restore();
    }

    @Override
    public void onDetach(MapView mapView) {
        this.disableMyLocation();
        super.onDetach(mapView);
    }

    public void setInfoWindowAnchor(float anchorU, float anchorV){
        mIWAnchorU = anchorU;
        mIWAnchorV= anchorV;
    }

    public void setOnMyLocationNewOverlayClickListener(OnMyLocationNewOverlayClickListener listener){
        mOnMyLocationNewOverlayClickListener = listener;
    }

    public void setOnMyLocationNewOverlayDragListener(OnMyLocationNewOverlayDragListener listener){
        mOnMyLocationNewOverlayDragListener = listener;
    }

    /** set an image to be shown in the InfoWindow  - this is not the marker icon */
    public void setImage(Drawable image){
        mImage = image;
    }

    /** get the image to be shown in the InfoWindow - this is not the marker icon */
    public Drawable getImage(){
        return mImage;
    }

    /** Set the InfoWindow to be used.
     * Default is a MyLocationNewOverlayInfoWindow, with the layout named "bonuspack_bubble".
     * You can use this method either to use your own layout, or to use your own sub-class of InfoWindow.
     * Note that this InfoWindow will receive the MyLocationNewOverlay object as an input, so it MUST be able to handle MyLocationNewOverlay attributes.
     * If you don't want any InfoWindow to open, you can set it to null. */
    public void setInfoWindow(InfoWindow infoWindow){
        mInfoWindow = infoWindow;
    }

    public void showInfoWindow(){
        if (mInfoWindow == null)
            return;
        int markerWidth = 0, markerHeight = 0;
        markerWidth = mIcon.getIntrinsicWidth();
        markerHeight = mIcon.getIntrinsicHeight();

        int offsetX = (int)(mIWAnchorU*markerWidth) - (int)(mAnchorU*markerWidth);
        int offsetY = (int)(mIWAnchorV*markerHeight) - (int)(mAnchorV*markerHeight);

        mInfoWindow.open(this, mPosition, offsetX, offsetY);
    }

    public boolean isInfoWindowShown(){
        if (mInfoWindow instanceof CurrentPosInfoWindow){
            CurrentPosInfoWindow iw = (CurrentPosInfoWindow)mInfoWindow;
            return (iw != null) && iw.isOpen() ;
        } else
            return super.isInfoWindowOpen();
    }

    @Override public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView){
        boolean touched = hitTest(event, mapView);
        if (touched){
            if (mOnMyLocationNewOverlayClickListener == null){
                return onMyLocationNewOverlayClickDefault(this, mapView);
            } else {
                return mOnMyLocationNewOverlayClickListener.onMyLocationNewOverlayClick(this, mapView);
            }
        } else
            return touched;
    }

    public boolean hitTest(final MotionEvent event, final MapView mapView){
        final Projection pj = mapView.getProjection();
        pj.toPixelsFromProjected(mMapCoordsProjected, mPositionPixels);

        final Rect screenRect = pj.getIntrinsicScreenRect();
        int x = -mPositionPixels.x + screenRect.left + (int) event.getX();
        int y = -mPositionPixels.y + screenRect.top + (int) event.getY();
        boolean hit = mIcon.getBounds().contains(x, y);
        return hit;
//        return true;
    }

    @Override public boolean onLongPress(final MotionEvent event, final MapView mapView) {
        boolean touched = hitTest(event, mapView);
        if (touched){
            if (mDraggable){
                //starts dragging mode:
                mIsDragged = true;
                closeInfoWindow();
                if (mOnMyLocationNewOverlayDragListener != null)
                    mOnMyLocationNewOverlayDragListener.onMyLocationNewOverlayDragStart(this);
                moveToEventPosition(event, mapView);
            }
        }
        return touched;
    }


    public void moveToEventPosition(final MotionEvent event, final MapView mapView){
        final Projection pj = mapView.getProjection();
        mPosition = (GeoPoint) pj.fromPixels((int)event.getX(), (int)event.getY());
        mapView.invalidate();
    }


    @Override public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
        if (mDraggable && mIsDragged){
            if (event.getAction() == MotionEvent.ACTION_UP) {
                mIsDragged = false;
                if (mOnMyLocationNewOverlayDragListener != null)
                    mOnMyLocationNewOverlayDragListener.onMyLocationNewOverlayDragEnd(this);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE){
                moveToEventPosition(event, mapView);
                if (mOnMyLocationNewOverlayDragListener != null)
                    mOnMyLocationNewOverlayDragListener.onMyLocationNewOverlayDrag(this);
                return true;
            } else
                return false;
        } else
            return false;
    }

    /** default behaviour when no click listener is set */
    protected boolean onMyLocationNewOverlayClickDefault(MyLocationNewOverlay marker, MapView mapView) {
        marker.showInfoWindow();

        mapView.getController().animateTo(mPosition);
        return true;
    }

    public interface OnMyLocationNewOverlayClickListener{
        abstract boolean onMyLocationNewOverlayClick(MyLocationNewOverlay marker, MapView mapView);
    }

    public interface OnMyLocationNewOverlayDragListener{
        abstract void onMyLocationNewOverlayDrag(MyLocationNewOverlay marker);
        abstract void onMyLocationNewOverlayDragEnd(MyLocationNewOverlay marker);
        abstract void onMyLocationNewOverlayDragStart(MyLocationNewOverlay marker);
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    /**
     * If enabled, an accuracy circle will be drawn around your current position.
     *
     * @param drawAccuracyEnabled
     *            whether the accuracy circle will be enabled
     */
    public void setDrawAccuracyEnabled(final boolean drawAccuracyEnabled) {
        mDrawAccuracyEnabled = drawAccuracyEnabled;
    }

    /**
     * If enabled, an accuracy circle will be drawn around your current position.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isDrawAccuracyEnabled() {
        return mDrawAccuracyEnabled;
    }

    public IMyLocationProvider getMyLocationProvider() {
        return mMyLocationProvider;
    }

    protected void setMyLocationProvider(IMyLocationProvider myLocationProvider) {
        if (myLocationProvider == null)
            throw new RuntimeException(
                    "You must pass an IMyLocationProvider to setMyLocationProvider()");

        if (isMyLocationEnabled())
            stopLocationProvider();

        mMyLocationProvider = myLocationProvider;
    }

    public void setPersonHotspot(float x, float y) {
        mPersonHotspot.set(x, y);
    }

    protected void drawMyLocation(final Canvas canvas, final MapView mapView, final Location lastFix) {
//        LocationUpdateService.staticLog("ARMAN", "draw mylocation");
        mPosition.setLatitudeE6((int)(lastFix.getLatitude() * 1000000));
        mPosition.setLongitudeE6((int) (lastFix.getLongitude() * 1000000));

        super.setPosition(mPosition);

        final Projection pj = mapView.getProjection();
        pj.toPixelsFromProjected(mMapCoordsProjected, mMapCoordsTranslated);

        pj.toPixelsFromProjected(mMapCoordsProjected, mPositionPixels);

        int width = mIcon.getIntrinsicWidth();
        int height = mIcon.getIntrinsicHeight();
        Rect rect = new Rect(0, 0, width, height);
        rect.offset(-(int)(mAnchorU * width), -(int)(mAnchorV * height));
        mIcon.setBounds(rect);

        if (mDrawAccuracyEnabled) {
            final float radius = lastFix.getAccuracy()
                    / (float) TileSystem.GroundResolution(lastFix.getLatitude(),
                    mapView.getZoomLevel());

            mCirclePaint.setAlpha(50);
            mCirclePaint.setStyle(Style.FILL);
            canvas.drawCircle(mMapCoordsTranslated.x, mMapCoordsTranslated.y, radius, mCirclePaint);

            mCirclePaint.setAlpha(150);
            mCirclePaint.setStyle(Style.STROKE);
            canvas.drawCircle(mMapCoordsTranslated.x, mMapCoordsTranslated.y, radius, mCirclePaint);
        }

        canvas.getMatrix(mMatrix);
        mMatrix.getValues(mMatrixValues);

        if (DEBUGMODE) {
            final float tx = (-mMatrixValues[Matrix.MTRANS_X] + 20)
                    / mMatrixValues[Matrix.MSCALE_X];
            final float ty = (-mMatrixValues[Matrix.MTRANS_Y] + 90)
                    / mMatrixValues[Matrix.MSCALE_Y];
            canvas.drawText("Lat: " + lastFix.getLatitude(), tx, ty + 5, mPaint);
            canvas.drawText("Lon: " + lastFix.getLongitude(), tx, ty + 20, mPaint);
            canvas.drawText("Alt: " + lastFix.getAltitude(), tx, ty + 35, mPaint);
            canvas.drawText("Acc: " + lastFix.getAccuracy(), tx, ty + 50, mPaint);
        }

        // Calculate real scale including accounting for rotation
        float scaleX = (float) Math.sqrt(mMatrixValues[Matrix.MSCALE_X]
                * mMatrixValues[Matrix.MSCALE_X] + mMatrixValues[Matrix.MSKEW_Y]
                * mMatrixValues[Matrix.MSKEW_Y]);
        float scaleY = (float) Math.sqrt(mMatrixValues[Matrix.MSCALE_Y]
                * mMatrixValues[Matrix.MSCALE_Y] + mMatrixValues[Matrix.MSKEW_X]
                * mMatrixValues[Matrix.MSKEW_X]);
        if (lastFix.hasBearing()) {
            canvas.save();
            // Rotate the icon
            canvas.rotate(lastFix.getBearing(), mMapCoordsTranslated.x, mMapCoordsTranslated.y);
            // Counteract any scaling that may be happening so the icon stays the same size
            canvas.scale(1 / scaleX, 1 / scaleY, mMapCoordsTranslated.x, mMapCoordsTranslated.y);
            // Draw the bitmap
            canvas.drawBitmap(mDirectionArrowBitmap, mMapCoordsTranslated.x
                            - mDirectionArrowCenterX, mMapCoordsTranslated.y - mDirectionArrowCenterY,
                    mPaint);
            canvas.restore();
        } else {
            canvas.save();
            // Unrotate the icon if the maps are rotated so the little man stays upright
            canvas.rotate(-mMapView.getMapOrientation(), mMapCoordsTranslated.x,
                    mMapCoordsTranslated.y);
            // Counteract any scaling that may be happening so the icon stays the same size
            canvas.scale(1 / scaleX, 1 / scaleY, mMapCoordsTranslated.x, mMapCoordsTranslated.y);
            // Draw the bitmap
            canvas.drawBitmap(mIconBitmap, mMapCoordsTranslated.x - mPersonHotspot.x,
                    mMapCoordsTranslated.y - mPersonHotspot.y, mPaint);
            canvas.restore();
        }
    }

    protected Rect getMyLocationDrawingBounds(int zoomLevel, Location lastFix, Rect reuse) {
        if (reuse == null)
            reuse = new Rect();

        final Projection pj = mMapView.getProjection();
        pj.toPixelsFromProjected(mMapCoordsProjected, mMapCoordsTranslated);

        // Start with the bitmap bounds
        if (lastFix.hasBearing()) {
            // Get a square bounding box around the object, and expand by the length of the diagonal
            // so as to allow for extra space for rotating
            int widestEdge = (int) Math.ceil(Math.max(mDirectionArrowBitmap.getWidth(),
                    mDirectionArrowBitmap.getHeight()) * Math.sqrt(2));
            reuse.set(mMapCoordsTranslated.x, mMapCoordsTranslated.y, mMapCoordsTranslated.x
                    + widestEdge, mMapCoordsTranslated.y + widestEdge);
            reuse.offset(-widestEdge / 2, -widestEdge / 2);
        } else {
            reuse.set(mMapCoordsTranslated.x, mMapCoordsTranslated.y, mMapCoordsTranslated.x
                    + mIconBitmap.getWidth(), mMapCoordsTranslated.y + mIconBitmap.getHeight());
            reuse.offset((int) (-mPersonHotspot.x + 0.5f), (int) (-mPersonHotspot.y + 0.5f));
        }

        // Add in the accuracy circle if enabled
        if (mDrawAccuracyEnabled) {
            final int radius = (int) FloatMath.ceil(lastFix.getAccuracy()
                    / (float) TileSystem.GroundResolution(lastFix.getLatitude(), zoomLevel));
            reuse.union(mMapCoordsTranslated.x - radius, mMapCoordsTranslated.y - radius,
                    mMapCoordsTranslated.x + radius, mMapCoordsTranslated.y + radius);
            final int strokeWidth = (int) FloatMath.ceil(mCirclePaint.getStrokeWidth() == 0 ? 1
                    : mCirclePaint.getStrokeWidth());
            reuse.inset(-strokeWidth, -strokeWidth);
        }

        return reuse;
    }

    /** Sets the icon for the marker. Can be changed at any time.
     * @param icon if null, the default osmdroid marker is used.
     */
    public void setIcon(Drawable icon){
        if (icon != null)
            mIcon = icon;
        else
            mIcon = mDefaultIcon;
    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public void draw(final Canvas c, MapView mapView, boolean shadow) {
        if (shadow)
            return;

        if (mLocation != null && isMyLocationEnabled()) {
            drawMyLocation(c, mapView, mLocation);
        }

        if (isCompassEnabled() && !Float.isNaN(mAzimuth)) {
            drawCompass(c, mAzimuth + getDisplayOrientation(), mapView.getProjection()
                    .getScreenRect());
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor arg0, final int arg1) {
        // This is not interesting for us at the moment
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            if (event.values != null) {
                mAzimuth = event.values[0];
                this.invalidateCompass();
            }
        }
    }

    /**
     * Enable orientation sensor (compass) updates and show a compass on the map. You will want to
     * call enableCompass() probably from your Activity's Activity.onResume() method, to enable the
     * features of this overlay. Remember to call the corresponding disableCompass() in your
     * Activity's Activity.onPause() method to turn off updates when in the background.
     */

    public boolean enableCompass() {
        boolean result = true;
        if (mSensorListener == null) {
            mSensorListener = new SensorEventListenerProxy(mSensorManager);
            result = mSensorListener.startListening(this, Sensor.TYPE_ORIENTATION,
                    SensorManager.SENSOR_DELAY_UI);
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            this.invalidateCompass();
        }

        return result;
    }



    /**
     * Disable orientation updates
     */
    @Override
    public void disableCompass() {
        if (mSensorListener != null) {
            mSensorListener.stopListening();
        }

        // Reset values
        mSensorListener = null;
        mAzimuth = Float.NaN;

        // Update the screen to see changes take effect
        if (mMapView != null) {
            this.invalidateCompass();
        }
    }

    /**
     * If enabled, the map is receiving orientation updates and drawing your location on the map.
     *
     * @return true if enabled, false otherwise
     */
    @Override
    public boolean isCompassEnabled() {
        return mSensorListener != null;
    }

    @Override
    public float getOrientation() {
        return mAzimuth;
    }



    @Override
    public boolean onSnapToItem(final int x, final int y, final Point snapPoint,
                                final IMapView mapView) {
        if (this.mLocation != null) {
            Projection pj = mMapView.getProjection();
            pj.toPixelsFromProjected(mMapCoordsProjected, mMapCoordsTranslated);
            snapPoint.x = mMapCoordsTranslated.x;
            snapPoint.y = mMapCoordsTranslated.y;
            final double xDiff = x - mMapCoordsTranslated.x;
            final double yDiff = y - mMapCoordsTranslated.y;
            boolean snap = xDiff * xDiff + yDiff * yDiff < 64;
//            if (DEBUGMODE) {
//                logger.debug("snap=" + snap);
//            }
            return snap;
        } else {
            return false;
        }
    }

    private int getDisplayOrientation() {
        switch (mDisplay.getOrientation()) {
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
            default: return 0;
        }
    }




    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras) {
    }

//    @Override
//    public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
//        if (event.getAction() == MotionEvent.ACTION_MOVE) {
//            this.disableFollowLocation();
//        }
//
//        return super.onTouchEvent(event, mapView);
//    }

    // ===========================================================
    // Menu handling methods
    // ===========================================================

    @Override
    public void setOptionsMenuEnabled(final boolean pOptionsMenuEnabled) {
        this.mOptionsMenuEnabled = pOptionsMenuEnabled;
    }

    @Override
    public boolean isOptionsMenuEnabled() {
        return this.mOptionsMenuEnabled;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu pMenu, final int pMenuIdOffset,
                                       final MapView pMapView) {
        pMenu.add(0, MENU_MY_LOCATION + pMenuIdOffset, Menu.NONE,
                mResourceProxy.getString(ResourceProxy.string.my_location))
                .setIcon(mResourceProxy.getDrawable(ResourceProxy.bitmap.ic_menu_mylocation))
                .setCheckable(true);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu pMenu, final int pMenuIdOffset,
                                        final MapView pMapView) {
        pMenu.findItem(MENU_MY_LOCATION + pMenuIdOffset).setChecked(this.isMyLocationEnabled());
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem pItem, final int pMenuIdOffset,
                                         final MapView pMapView) {
        final int menuId = pItem.getItemId() - pMenuIdOffset;
        if (menuId == MENU_MY_LOCATION) {
            if (this.isMyLocationEnabled()) {
                this.disableFollowLocation();
                this.disableMyLocation();
            } else {
                this.enableFollowLocation();
                this.enableMyLocation();
            }
            return true;
        } else {
            return false;
        }
    }

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * Return a GeoPoint of the last known location, or null if not known.
     */
    public GeoPoint getMyLocation() {
        if (mLocation == null) {
            return null;
        } else {
            return new GeoPoint(mLocation);
        }
    }

    public Location getLastFix() {
        return mLocation;
    }

    /**
     * Enables "follow" functionality. The map will center on your current location and
     * automatically scroll as you move. Scrolling the map in the UI will disable.
     */
    public void enableFollowLocation() {
        mIsFollowing = true;

        // set initial location when enabled
        if (isMyLocationEnabled()) {
            Location location = mMyLocationProvider.getLastKnownLocation();
            if (location != null) {
                setLocation(location);
            }
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }
    }

    public void tryUpdateCurrentLocation()
    {
        Location location = mMyLocationProvider.getLastKnownLocation();
        if (location != null) {
            setLocation(location);
        }
        else
            Toast.makeText(mMapView.getContext(), "در حال دریافت مکان شما", Toast.LENGTH_LONG).show();

    }

    /**
     * Disables "follow" functionality.
     */
    public void disableFollowLocation() {
        mIsFollowing = false;
    }

    /**
     * If enabled, the map will center on your current location and automatically scroll as you
     * move. Scrolling the map in the UI will disable.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isFollowLocationEnabled() {
        return mIsFollowing;
    }

    @Override
    public void onLocationChanged(final Location location, IMyLocationProvider source) {

        if (location != null) {
            // These location updates can come in from different threads
            mHandler.postAtTime(new Runnable() {
                @Override
                public void run() {
                    setLocation(location);

                    for (final Runnable runnable : mRunOnFirstFix) {
                        new Thread(runnable).start();
                    }
                    mRunOnFirstFix.clear();
                }
            }, mHandlerToken, 0);
        }
    }

    protected void setLocation(Location location) {
        // If we had a previous location, let's get those bounds
        Location oldLocation = mLocation;
        if (oldLocation != null) {
            this.getMyLocationDrawingBounds(mMapView.getZoomLevel(), oldLocation,
                    mMyLocationPreviousRect);
        }

        mLocation = location;

        // Cache location point
        mMapView.getProjection().toProjectedPixels((int) (mLocation.getLatitude() * 1E6),
                (int) (mLocation.getLongitude() * 1E6), mMapCoordsProjected);

        if (mIsFollowing) {
            mGeoPoint.setLatitudeE6((int) (mLocation.getLatitude() * 1E6));
            mGeoPoint.setLongitudeE6((int) (mLocation.getLongitude() * 1E6));
            // mMapController.animateTo(mGeoPoint); changed by Arman to:
            mMapController.setCenter(mGeoPoint);
        } else {
            // Get new drawing bounds
            this.getMyLocationDrawingBounds(mMapView.getZoomLevel(), mLocation, mMyLocationRect);

            // If we had a previous location, merge in those bounds too
            if (oldLocation != null) {
                mMyLocationRect.union(mMyLocationPreviousRect);
            }

            final int left = mMyLocationRect.left;
            final int top = mMyLocationRect.top;
            final int right = mMyLocationRect.right;
            final int bottom = mMyLocationRect.bottom;

            // Invalidate the bounds
            mMapView.invalidateMapCoordinates(left, top, right, bottom);
        }
    }

    public boolean enableMyLocation(IMyLocationProvider myLocationProvider) {
        // Set the location provider. This will call stopLocationProvider().
        setMyLocationProvider(myLocationProvider);

        tryUpdateCurrentLocation();
        boolean success = mMyLocationProvider.startLocationProvider(this);
        mIsLocationEnabled = success;

        // set initial location when enabled
        if (success) {
            Location location = mMyLocationProvider.getLastKnownLocation();
            if (location != null) {
                setLocation(location);
            }
        }

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }

        return success;
    }

    /**
     * Enable receiving location updates from the provided IMyLocationProvider and show your
     * location on the maps. You will likely want to call enableMyLocation() from your Activity's
     * Activity.onResume() method, to enable the features of this overlay. Remember to call the
     * corresponding disableMyLocation() in your Activity's Activity.onPause() method to turn off
     * updates when in the background.
     */
    public boolean enableMyLocation() {

        return enableMyLocation(mMyLocationProvider);
    }

    /**
     * Disable location updates
     */
    public void disableMyLocation() {
        mIsLocationEnabled = false;

        stopLocationProvider();

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView.postInvalidate();
        }
    }

    protected void stopLocationProvider() {
        if (mMyLocationProvider != null) {
            mMyLocationProvider.stopLocationProvider();
        }
        mHandler.removeCallbacksAndMessages(mHandlerToken);
    }

    /**
     * If enabled, the map is receiving location updates and drawing your location on the map.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isMyLocationEnabled() {
        return mIsLocationEnabled;
    }

    /**
     * Queues a runnable to be executed as soon as we have a location fix. If we already have a fix,
     * we'll execute the runnable immediately and return true. If not, we'll hang on to the runnable
     * and return false; as soon as we get a location fix, we'll run it in in a new thread.
     */
    public boolean runOnFirstFix(final Runnable runnable) {
        if (mMyLocationProvider != null && mLocation != null) {
            new Thread(runnable).start();
            return true;
        } else {
            mRunOnFirstFix.addLast(runnable);
            return false;
        }
    }





    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private Point calculatePointOnCircle(final float centerX, final float centerY,
                                         final float radius, final float degrees) {
        // for trigonometry, 0 is pointing east, so subtract 90
        // compass degrees are the wrong way round
        final double dblRadians = Math.toRadians(-degrees + 90);

        final int intX = (int) (radius * Math.cos(dblRadians));
        final int intY = (int) (radius * Math.sin(dblRadians));

        return new Point((int) centerX + intX, (int) centerY - intY);
    }

    private void drawTriangle(final Canvas canvas, final float x, final float y,
                              final float radius, final float degrees, final Paint paint) {
        canvas.save();
        final Point point = this.calculatePointOnCircle(x, y, radius, degrees);
        canvas.rotate(degrees, point.x, point.y);
        final Path p = new Path();
        p.moveTo(point.x - 2 * mScale, point.y);
        p.lineTo(point.x + 2 * mScale, point.y);
        p.lineTo(point.x, point.y - 5 * mScale);
        p.close();
        canvas.drawPath(p, paint);
        canvas.restore();
    }

    private void createCompassFramePicture() {
        // The inside of the compass is white and transparent
        final Paint innerPaint = new Paint();
        innerPaint.setColor(Color.WHITE);
        innerPaint.setAntiAlias(true);
        innerPaint.setStyle(Style.FILL);
        innerPaint.setAlpha(200);

        // The outer part (circle and little triangles) is gray and transparent
        final Paint outerPaint = new Paint();
        outerPaint.setColor(Color.GRAY);
        outerPaint.setAntiAlias(true);
        outerPaint.setStyle(Style.STROKE);
        outerPaint.setStrokeWidth(2.0f);
        outerPaint.setAlpha(200);

        final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2);
        final int center = picBorderWidthAndHeight / 2;

        final Canvas canvas = mCompassFrame.beginRecording(picBorderWidthAndHeight,
                picBorderWidthAndHeight);

        // draw compass inner circle and border
        canvas.drawCircle(center, center, mCompassRadius * mScale, innerPaint);
        canvas.drawCircle(center, center, mCompassRadius * mScale, outerPaint);

        // Draw little triangles north, south, west and east (don't move)
        // to make those move use "-bearing + 0" etc. (Note: that would mean to draw the triangles
        // in the onDraw() method)
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 0, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 90, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 180, outerPaint);
        drawTriangle(canvas, center, center, mCompassRadius * mScale, 270, outerPaint);

        mCompassFrame.endRecording();
    }

    private void createCompassRosePicture() {
        // Paint design of north triangle (it's common to paint north in red color)
        final Paint northPaint = new Paint();
        northPaint.setColor(0xFFA00000);
        northPaint.setAntiAlias(true);
        northPaint.setStyle(Style.FILL);
        northPaint.setAlpha(220);

        // Paint design of south triangle (black)
        final Paint southPaint = new Paint();
        southPaint.setColor(Color.BLACK);
        southPaint.setAntiAlias(true);
        southPaint.setStyle(Style.FILL);
        southPaint.setAlpha(220);

        // Create a little white dot in the middle of the compass rose
        final Paint centerPaint = new Paint();
        centerPaint.setColor(Color.WHITE);
        centerPaint.setAntiAlias(true);
        centerPaint.setStyle(Style.FILL);
        centerPaint.setAlpha(220);

        // final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2 * mScale);
        final int picBorderWidthAndHeight = (int) ((mCompassRadius + 5) * 2);
        final int center = picBorderWidthAndHeight / 2;

        final Canvas canvas = mCompassRose.beginRecording(picBorderWidthAndHeight,
                picBorderWidthAndHeight);

        // Blue triangle pointing north
        final Path pathNorth = new Path();
        pathNorth.moveTo(center, center - (mCompassRadius - 3) * mScale);
        pathNorth.lineTo(center + 4 * mScale, center);
        pathNorth.lineTo(center - 4 * mScale, center);
        pathNorth.lineTo(center, center - (mCompassRadius - 3) * mScale);
        pathNorth.close();
        canvas.drawPath(pathNorth, northPaint);

        // Red triangle pointing south
        final Path pathSouth = new Path();
        pathSouth.moveTo(center, center + (mCompassRadius - 3) * mScale);
        pathSouth.lineTo(center + 4 * mScale, center);
        pathSouth.lineTo(center - 4 * mScale, center);
        pathSouth.lineTo(center, center + (mCompassRadius - 3) * mScale);
        pathSouth.close();
        canvas.drawPath(pathSouth, southPaint);

        // Draw a little white dot in the middle
        canvas.drawCircle(center, center, 2, centerPaint);

        mCompassRose.endRecording();
    }
}
