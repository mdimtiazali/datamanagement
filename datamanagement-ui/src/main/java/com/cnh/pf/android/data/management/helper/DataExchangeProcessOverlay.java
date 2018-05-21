/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.cnh.pf.android.data.management.R;

/**
 * provides overlay views to denote data exchange progress by providing a transparent overlay
 * default mode is hidden
 */
public class DataExchangeProcessOverlay extends FrameLayout {

   /**
    * modes available for overlay
    */
   public enum MODE {
      EXPORT_PROCESS, IMPORT_PROCESS, HIDDEN
   };

   private TextView titleText = null;
   private TextView descriptionText = null;

   private MODE mode = MODE.HIDDEN;

   public DataExchangeProcessOverlay(Context context) {
      super(context, null, 0);
      this.init(context);
   }

   public DataExchangeProcessOverlay(Context context, AttributeSet attrs) {
      this(context, attrs, 0);
   }

   public DataExchangeProcessOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      init(context);
   }

   /**
    * initialize data exchange overlay
    * @param context Context the overlay is created in
    */
   private void init(Context context) {
      setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
      setClickable(true);
      if (context.getResources() != null) {
         Resources res = context.getResources();
         setBackgroundColor(res.getColor(R.color.data_exchange_overlay_background));
      }
      setVisibility(View.INVISIBLE);

      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      if (inflater != null) {
         inflater.inflate(R.layout.data_exchange_process_overlay, this);
      }
      titleText = (TextView) findViewById(R.id.data_exchange_overlay_title);
      descriptionText = (TextView) findViewById(R.id.data_exchange_overlay_description);
      this.mode = MODE.HIDDEN;
      hideOverlay();
   }

   /**
    * show disabled overlay with loading animation
    */
   private void showExportProcess() {
      this.setVisibility(VISIBLE);
      titleText.setText(R.string.export_in_progress);
      descriptionText.setText(R.string.dataexchange_leave_during_export_process);
   }

   /**
    * show disabled overlay with disconnected graphic
    */
   private void showImportProcess() {
      this.setVisibility(VISIBLE);
      titleText.setText(R.string.import_in_progress);
      descriptionText.setText(R.string.dataexchange_leave_during_import_process);
   }

   /**
    * hide disabled overlay in its entirety
    */
   private void hideOverlay() {
      this.setVisibility(GONE);
   }

   /**
    * set mode to one of EXPORT, IMPORT, or HIDDEN
    * @param mode DataExchangeProcessOverlay.MODE to be set
    */
   public void setMode(DataExchangeProcessOverlay.MODE mode) {
      if (this.mode != mode) {
         this.mode = mode;
         switch (mode) {
         case EXPORT_PROCESS:
            showExportProcess();
            break;
         case IMPORT_PROCESS:
            showImportProcess();
            break;
         case HIDDEN:
         default:
            hideOverlay();
            break;
         }
      }
   }

   /**
    * retrieve current mode
    * @return mode
    */
   public MODE getMode() {
      return this.mode;
   }

}
