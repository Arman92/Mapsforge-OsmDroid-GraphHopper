package com.arman.osmdroidmapsforge.map.overlays;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.Marker;


import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.arman.osmdroidmapsforge.R;
import com.arman.osmdroidmapsforge.map.MFMapView;

public class NewPoiInfoWindow extends InfoWindow{
    private Marker marker;
    private boolean isNewPoiSaved = false;

    public NewPoiInfoWindow(int layoutResId, final MFMapView mapView, Marker marker) {
        super(layoutResId, mapView);

        this.marker = marker;


        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                //default behavior: close it when clicking on the bubble:
                //if (e.getAction() == MotionEvent.ACTION_UP)
                //	close();
                mView.findViewById(R.id.title_textedit).clearFocus();
                EditText desc_textedit = (EditText)mView.findViewById(R.id.desc_textedit);
                InputMethodManager imm = (InputMethodManager)mapView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(desc_textedit.getWindowToken(), 0);
                return true;
            }
        });
    }



    @Override public void onOpen(Object item) {
//		OverlayWithIW overlay = (OverlayWithIW)item;
//		String title = overlay.getTitle();
//		if (title == null)
//			title = "";
//		((TextView)mView.findViewById(mTitleId /*R.id.title*/)).setText(title);
//
//		String snippet = overlay.getSnippet();
//		if (snippet == null)
//			snippet = "";
//		Spanned snippetHtml = Html.fromHtml(snippet);
//		((TextView)mView.findViewById(mDescriptionId /*R.id.description*/)).setText(snippetHtml);
//
        //handle sub-description, hidding or showing the text view:
//		TextView subDescText = (TextView)mView.findViewById(mSubDescriptionId);
//		String subDesc = overlay.getSubDescription();
//		if (subDesc != null && !("".equals(subDesc))){
//			subDescText.setText(Html.fromHtml(subDesc));
//			subDescText.setVisibility(View.VISIBLE);
//		} else {
//			subDescText.setVisibility(View.GONE);
//		}

    }

    @Override public void onClose() {
        if (!isNewPoiSaved)
            mMapView.getOverlays().remove(marker);

    }


}
