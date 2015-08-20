/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.android.data.management.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import com.cnh.android.data.management.R;
import com.cnh.android.data.management.adapter.DataManagementBaseAdapter;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressBarView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog showing overall import/export process and current step in full process
 * Created by oscar.salazar@cnhind.com
 */
public class ProcessDialog extends DialogView {
   private static final Logger logger = LoggerFactory.getLogger(ProcessDialog.class);

   private DataManagementBaseAdapter adapter;
   private View activeView;
   private Activity context;
   private ProgressBarView pbBar;


   public ProcessDialog(Activity context) {
      super(context);
      this.context = context;
      init();
   }

   private void init() {
      setFirstButtonText(context.getResources().getString(R.string.cancel));
      showSecondButton(false);
      showThirdButton(false);

      LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
      View view = inflater.inflate(R.layout.progress_layout, null);
      pbBar = (ProgressBarView) view.findViewById(R.id.progress_bar);
      pbBar.setProgress(0);
      activeView = this.setBodyView(view);
   }

   /**
    * Remove ProgressBar and calls adapter for view
    */
   public void clearLoading() {
      pbBar.setVisibility(GONE);
      setBodyView(adapter.getView(null));

      adapter.setOnTargetSelectedListener(new DataManagementBaseAdapter.OnTargetSelectedListener() {
         @Override
         public void onTargetSelected(boolean done, View convertView) {
            if (!done) {
               View targetView = adapter.getView(convertView);
               if (targetView == null) {
                  ProcessDialog.this.dismiss();
               }
            } else {
               ProcessDialog.this.dismiss();
            }
         }
      });
   }

   /**
    * Set {@link DataManagementBaseAdapter}
    * @param adapter
    */
   public void setAdapter(DataManagementBaseAdapter adapter) {
      this.adapter = adapter;
   }

   /**
    * Show the dialog
    */
   public void show() {
      ((TabActivity) context).showPopup(this, true);
   }

   /**
    * Clear and hide the dialog
    */
   public void hide() {
      logger.debug("hide()");
      ((TabActivity) context).dismissPopup(this);
   }

   /**
    * Set progress of progressbar
    * @param progress  int 0<n<100
    */
   public void setProgress(int progress) {
      logger.debug("setProgress:" + progress);
      pbBar.setProgress(progress);
   }

   @Override
   public DialogView setTitle(String text) {
      pbBar.setTitle(text);
      return super.setTitle(text);
   }
}
