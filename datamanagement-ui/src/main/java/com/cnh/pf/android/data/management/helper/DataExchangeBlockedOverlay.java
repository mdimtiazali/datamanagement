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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.pf.android.data.management.R;

/**
 * provides overlay views to denote data exchange progress by blocking underlying layouts
 * default mode is hidden
 */
public class DataExchangeBlockedOverlay extends FrameLayout {

   /**
    * modes available for overlay
    */
   public enum MODE {
      DISCONNECTED, LOADING, BLOCKED_BY_EXPORT, BLOCKED_BY_IMPORT, HIDDEN
   };

   private TextView titleText = null;
   private TextView descriptionText = null;
   private ImageView modeImage = null;
   private LinearLayout blockedContainer = null;
   private View loadingContainer = null;
   private ImageView disconnectedContainer = null;

   private MODE mode = MODE.HIDDEN;

   public DataExchangeBlockedOverlay(Context context) {
      super(context, null, 0);
      this.init(context);
   }

   public DataExchangeBlockedOverlay(Context context, AttributeSet attrs) {
      this(context, attrs, 0);
   }

   public DataExchangeBlockedOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      init(context);
   }

   /**
    * initialize data exchange overlay
    * @param context Context the overlay is created in
    */
   private void init(Context context) {
      setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
      setClickable(true);

      setVisibility(View.INVISIBLE);

      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      if (inflater != null) {
         inflater.inflate(R.layout.data_exchange_blocked_overlay, this);
      }
      blockedContainer = (LinearLayout) findViewById(R.id.data_exchange_blocked);
      titleText = (TextView) findViewById(R.id.data_exchange_overlay_title);
      descriptionText = (TextView) findViewById(R.id.data_exchange_overlay_description);
      descriptionText.setText(R.string.dataexchange_accessible_after_process_finished);
      modeImage = (ImageView) findViewById(R.id.data_exchange_overlay_mode_image);
      loadingContainer = findViewById(R.id.data_exchange_loading);
      disconnectedContainer = (ImageView) findViewById(R.id.data_exchange_disconnected);
      this.mode = MODE.HIDDEN;
      hideOverlay();
   }

   /**
    * show blocked by export overlay
    */
   private void showBlockedByExport() {
      this.setVisibility(VISIBLE);
      blockedContainer.setVisibility(VISIBLE);
      loadingContainer.setVisibility(GONE);
      disconnectedContainer.setVisibility(GONE);
      titleText.setText(R.string.export_in_progress);
      modeImage.setImageResource(R.drawable.ic_tab_data_export_selected);
   }

   /**
    * show blocked by import overlay
    */
   private void showBlockedByImport() {
      this.setVisibility(VISIBLE);
      blockedContainer.setVisibility(VISIBLE);
      loadingContainer.setVisibility(GONE);
      disconnectedContainer.setVisibility(GONE);
      titleText.setText(R.string.import_in_progress);
      modeImage.setImageResource(R.drawable.ic_tab_data_import_selected);
   }

   /**
    * show loading overlay
    */
   private void showLoading() {
      this.setVisibility(VISIBLE);
      blockedContainer.setVisibility(GONE);
      loadingContainer.setVisibility(VISIBLE);
      disconnectedContainer.setVisibility(GONE);
   }

   /**
    * show disconnected overlay
    */
   private void showDisconnected() {
      this.setVisibility(VISIBLE);
      blockedContainer.setVisibility(GONE);
      loadingContainer.setVisibility(GONE);
      disconnectedContainer.setVisibility(VISIBLE);
   }

   /**
    * hide disabled overlay in its entirety
    */
   private void hideOverlay() {
      this.setVisibility(GONE);
      blockedContainer.setVisibility(GONE);
      loadingContainer.setVisibility(GONE);
      disconnectedContainer.setVisibility(GONE);
   }

   /**
    * set mode to one of EXPORT, IMPORT, or HIDDEN
    * @param mode DataExchangeOverlay.MODE to be set
    */
   public void setMode(DataExchangeBlockedOverlay.MODE mode) {
      if (this.mode != mode) {
         this.mode = mode;
         switch (mode) {
         case BLOCKED_BY_EXPORT:
            showBlockedByExport();
            break;
         case BLOCKED_BY_IMPORT:
            showBlockedByImport();
            break;
         case LOADING:
            showLoading();
            break;
         case DISCONNECTED:
            showDisconnected();
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
    * @return mode current MODE the overlay is in
    */
   public MODE getMode() {
      return this.mode;
   }

}
