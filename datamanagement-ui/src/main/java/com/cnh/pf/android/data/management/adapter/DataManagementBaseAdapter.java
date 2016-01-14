/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.adapter;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import com.cnh.jgroups.Operation;

import java.util.List;

/**
 * Adapter for ProcessDialog View
 * Created by oscar.salazar@cnhind.com
 */
public abstract class DataManagementBaseAdapter {

   protected List<Operation> operationList;
   protected final Activity context;
   protected OnTargetSelectedListener onTargetSelectedListener;
   protected int activeOperation = 0;
   protected int totalOperation = 0;
   protected View targetView;

   protected OnTargetsSelectedListener onTargetsSelectedListener;

   public DataManagementBaseAdapter(Activity context, List<Operation> operations) {
      this.context = context;
      operationList = operations;
      totalOperation = operations.size();
   }

   protected class ViewHolder {
      protected TextView typeView;
      protected TextView nameView;
   }

   /**
    * Set listener for object target selection
    * @param onTargetSelectedListener
    */
   public void setOnTargetSelectedListener(OnTargetSelectedListener onTargetSelectedListener) {
      this.onTargetSelectedListener = onTargetSelectedListener;
   }

   /**
    * Return entity name of the current entity being processed
    */
   public String getEntityString() {
      String type = operationList.get(activeOperation).getData().getType();
      return type.substring(type.lastIndexOf('.') + 1);
   }

   /**
    * Updates view with new data
    * @param convertView View to reuse
    * @return updated view
    */
   public abstract View getView(View convertView);
   protected abstract void checkAndUpdateActive();

   public interface OnTargetSelectedListener {
      /**
       * Invoked when user has selected a target for object
       */
      void onTargetSelected(boolean done, View convertView);
   };

   /**
    * Set {@link OnTargetSelectedListener}
    * @param onTargetsSelectedListener
    */
   public void setOnTargetsSelectedListener(OnTargetsSelectedListener onTargetsSelectedListener) {
      this.onTargetsSelectedListener = onTargetsSelectedListener;
   }

   public interface OnTargetsSelectedListener {
      /**
       * Invoked when operations have completed
       * @param operations List<Operations> operations
       */
      void onCompletion(List<Operation> operations);
   }

   public abstract OnActionSelectedListener getActionListener();

   public interface OnActionSelectedListener {
      void onButtonSelected(Operation.Action action);
   }
}
