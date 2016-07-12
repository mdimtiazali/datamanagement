/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.adapter.DataConflictViewAdapter;
import com.cnh.pf.android.data.management.adapter.DataManagementBaseAdapter;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressBarView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;
import roboguice.inject.InjectResource;

import static org.jgroups.conf.ProtocolConfiguration.log;

/**
 * Dialog showing overall import/export process and current step in full process
 * Created by oscar.salazar@cnhind.com
 */
public class ProcessDialog extends DialogView {
   private static final Logger logger = LoggerFactory.getLogger(ProcessDialog.class);

   @InjectResource(R.string.keep_both) String keepBothStr;
   @InjectResource(R.string.copy_and_replace) String replaceStr;
   @InjectResource(R.string.cancel) String cancelStr;
   @InjectResource(R.string.data_conflict) String dataConflictStr;
   private DataManagementBaseAdapter adapter;
   private View activeView;
   private Activity context;
   private ProgressBarView pbBar;

   private DataConflictViewAdapter.OnActionSelectedListener listener;


   public ProcessDialog(Activity context) {
      super(context);
      RoboGuice.getInjector(context).injectMembersWithoutViews(this);
      this.context = context;
      init();
   }

   public void init() {
      setFirstButtonText(keepBothStr);
      setSecondButtonText(replaceStr);
      setThirdButtonText(cancelStr);
      showSecondButton(false);
      showFirstButton(false);
      setDismissOnButtonClick(false);

      LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
      View view = inflater.inflate(R.layout.progress_layout, null);
      pbBar = (ProgressBarView) view.findViewById(R.id.progress_bar);
      pbBar.setProgress(0);
      activeView = this.setBodyView(view);
   }

   private View body;

   @Override
   public DialogView setBodyView(View view) {
      if(body!=null) {
         ((ViewGroup)body.getParent()).removeView(body);
      }
      body = view;
      return super.setBodyView(view);
   }

   /**
    * Remove ProgressBar and calls adapter for view
    */
   public void clearLoading() {
      pbBar.setVisibility(GONE);
      setBodyView(adapter.getView(null));
      listener = adapter.getActionListener();
      setTitle(dataConflictStr);

      setOnButtonClickListener(new OnButtonClickListener() {
         @Override
         public void onButtonClick(DialogViewInterface dialog, int which) {
            if (which == DialogViewInterface.BUTTON_FIRST) {
               listener.onButtonSelected(Operation.Action.COPY_AND_KEEP);
            }
            else if (which == DialogViewInterface.BUTTON_SECOND) {
               listener.onButtonSelected(Operation.Action.COPY_AND_REPLACE);
            }
            else if (which == DialogViewInterface.BUTTON_THIRD) {
               ProcessDialog.this.dismiss();
            }
         }
      });

      adapter.setOnTargetSelectedListener(new DataManagementBaseAdapter.OnTargetSelectedListener() {
         @Override
         public void onTargetSelected(boolean done, View convertView) {
            log.trace("onTargetSelected({}, {})", done, convertView);
            if (!done) {
               View targetView = adapter.getView(convertView);
               if (targetView == null) {
                  ProcessDialog.this.hide();
               }
            } else {
               ProcessDialog.this.hide();
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
      invalidate();
   }

   @Override
   public DialogView setTitle(String text) {
      pbBar.setTitle(text);
      return super.setTitle(text);
   }
}
