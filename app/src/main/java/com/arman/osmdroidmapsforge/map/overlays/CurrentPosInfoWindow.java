package com.arman.osmdroidmapsforge.map.overlays;



import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.OverlayWithIW;
import org.osmdroid.views.MapView;


import android.location.Location;
import android.text.Html;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.arman.osmdroidmapsforge.R;


public class CurrentPosInfoWindow extends InfoWindow{
	private Marker marker;
	private Location location;
	
	public CurrentPosInfoWindow(int layoutResId, final MapView mapView, Marker marker, Location location) {
		super(layoutResId, mapView);

		this.marker = marker;
		this.location = location;


		mView.setOnTouchListener(new View.OnTouchListener() {
			@Override public boolean onTouch(View v, MotionEvent e) {
				//default behavior: close it when clicking on the bubble:
				if (e.getAction() == MotionEvent.ACTION_UP)
					close();
				
				return true;
			}
		});
	}
	
	
	
	@Override public void onOpen(Object item) {
		OverlayWithIW overlay = (OverlayWithIW)item;
		String title = overlay.getTitle();
		if (title == null)
			title = "";
		((TextView)mView.findViewById(R.id.bubble_title )).setText(title);
		
		String snippet = overlay.getSnippet();
		if (snippet == null)
			snippet = "";
		Spanned snippetHtml = Html.fromHtml(snippet);
		((TextView)mView.findViewById(R.id.bubble_description )).setText(snippetHtml);
		
		TextView accuracyTxtView = (TextView)mView.findViewById(R.id.bubble_accuracy);
		
		accuracyTxtView.setText("Accuracy" + Math.round(location.getAccuracy()) + "m");
		
		

	}
	
	@Override public void onClose() {
		//by default, do nothing
	}
	
	
}
