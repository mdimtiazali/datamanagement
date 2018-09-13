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

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressBarView;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.adapter.DataConflictViewAdapter;
import com.cnh.pf.android.data.management.adapter.DataManagementBaseAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import roboguice.RoboGuice;
import roboguice.inject.InjectResource;

/**
 * Dialog showing overall import/export process and current step in full process
 * Created by oscar.salazar@cnhind.com
 */
public class ProcessDialog extends DialogView {
   private static final Logger logger = LoggerFactory.getLogger(ProcessDialog.class);

   @InjectResource(R.string.cancel)
   String cancelStr;

   private DataManagementBaseAdapter adapter;
   private Activity context;
   private ProgressBarView pbBar;
   private View body;
   private DataConflictViewAdapter.OnActionSelectedListener listener;

   // maps from object type to ViewHolder cache
   private final Map<String, DataManagementBaseAdapter.ViewHolder> viewCache = new HashMap<String, DataManagementBaseAdapter.ViewHolder>();

   public ProcessDialog(Activity context) {
      super(context);
      RoboGuice.getInjector(context).injectMembersWithoutViews(this);
      this.context = context;
      init();
   }

   public void init() {
      setThirdButtonText(cancelStr);
      showSecondButton(false);
      showFirstButton(false);
      setDismissOnButtonClick(false);

      LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
      View view = inflater.inflate(R.layout.progress_layout, null);
      pbBar = (ProgressBarView) view.findViewById(R.id.progress_bar);
      pbBar.setProgress(0);
      pbBar.setShowProgress(false);
      setBodyView(view);
   }

   @Override
   public DialogView setBodyView(View view) {
      if (body != null) {
         ((ViewGroup) body.getParent()).removeView(body);
      }
      body = view;
      return super.setBodyView(view);
   }

   /**
    * Remove ProgressBar and calls adapter for view
    */
   public void clearLoading() {
      pbBar.setVisibility(GONE);
      listener = adapter.getActionListener();

      setOnButtonClickListener(new OnButtonClickListener() {
         @Override
         public void onButtonClick(DialogViewInterface dialog, int which) {
            if (which == DialogViewInterface.BUTTON_FIRST) {
               listener.onButtonSelected(ProcessDialog.this, DataManagementBaseAdapter.Action.ACTION1);
            }
            else if (which == DialogViewInterface.BUTTON_SECOND) {
               listener.onButtonSelected(ProcessDialog.this, DataManagementBaseAdapter.Action.ACTION2);
            }
            else if (which == DialogViewInterface.BUTTON_THIRD) {
               listener.onButtonSelected(ProcessDialog.this, null);
               ProcessDialog.this.dismiss();
            }
         }
      });

      adapter.setOnTargetSelectedListener(new DataManagementBaseAdapter.OnTargetSelectedListener() {
         @Override
         public void onTargetSelected() {
            updateView();
         }
      });
      viewCache.clear();
      updateView();
   }

   //let adapter either create new or reuse existing view
   private void updateView() {
      final String nextType = adapter.getType(adapter.getPosition());
      DataManagementBaseAdapter.ViewHolder targetView = null;
      if (viewCache.containsKey(nextType)) {
         targetView = adapter.getView(viewCache.get(nextType));
      }
      else {
         targetView = adapter.getView(null);
         viewCache.put(nextType, targetView);
      }
      if (targetView == null) {
         hide();
      }
      else if (targetView.getRoot() != body) {
         setBodyView(targetView.getRoot());
      }
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

   /**
    * Returns the LayoutParams of the dialogs content view
    * @return
    */
   @Nullable
   public ViewGroup.LayoutParams getContentLayoutParameter() {
      if (super.mFlContent != null) {
         return super.mFlContent.getLayoutParams();
      }
      else {
         return null;
      }
   }

   /**
    * This method allows to modify the layout parameter of the content view of the dialog
    * @param contentLayoutParams LayoutParams to be applied to dialogs content view
    */
   public void setContentLayoutParameter(ViewGroup.LayoutParams contentLayoutParams) {
      if (super.mFlContent != null) {
         super.mFlContent.setLayoutParams(contentLayoutParams);
      }
   }
}
