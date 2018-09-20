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

import com.cnh.android.dialog.DialogView;
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
   protected int position = 0;

   protected OnTargetsSelectedListener onTargetsSelectedListener;

   public DataManagementBaseAdapter(Activity context, List<Operation> operations) {
      this.context = context;
      operationList = operations;
   }

   /**
    * Set listener for object target selection
    * @param onTargetSelectedListener
    */
   public void setOnTargetSelectedListener(OnTargetSelectedListener onTargetSelectedListener) {
      this.onTargetSelectedListener = onTargetSelectedListener;
   }

   public Operation getOperation(int position) {
      return operationList.get(position);
   }

   /**
    * Get current position in adapter.
    * @return
    */
   public int getPosition() {
      return position;
   }

   public int getCount() {
      return operationList.size();
   }

   public String getType(int position) {
      return getOperation(position).getData().getType();
   }

   /**
    * Gets the view for the current Operation, possibly reusing existing view if possible.
    * @param convertView   existing view to reuse
    * @return  the view to use
    */
   public ViewHolder getView(ViewHolder convertView) {
      if (position < operationList.size()) {
         if (convertView == null) {
            convertView = createView();
         }
         updateView(convertView);
      }
      return convertView;
   }

   /**
    * Create initial ViewHolder
    * @return  the ViewHolder
    */
   protected abstract ViewHolder createView();

   /**
    * Updates view with new data
    * @param convertView ViewHolder to reuse
    * @return updated view
    */
   protected abstract ViewHolder updateView(ViewHolder convertView);

   /**
    * Whether or not the current view should be shown
    * @return
    */
   protected abstract boolean shouldShowView();

   /**
    * Skip until next Operation
    */
   protected void checkAndUpdateActive() {
      while (position < operationList.size() && !shouldShowView()) {
         position++;
      }
      if (onTargetsSelectedListener != null) {
         if (position == operationList.size()) {
            onTargetsSelectedListener.onCompletion(operationList);
         }
         else {
            onTargetSelectedListener.onTargetSelected();
         }
      }
   }

   public interface OnTargetSelectedListener {
      /**
       * Invoked when user has selected a target for object
       */
      void onTargetSelected();
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

   /**
    * Generic actions for the dialog buttons
    */
   public enum Action {
      ACTION1, ACTION2, ACTION3, ACTION4;
   }

   /**
    * This interface defines the action to be performed as a button is pressed
    */
   public interface OnActionSelectedListener {
      /**
       * Method to be called if a button is pressed
       */
      void onButtonSelected(DialogView dialog, Action action);
   }

   /**
    * Base class for all View Holders
    */
   public static class ViewHolder {
      private final View root;

      public ViewHolder(View root) {
         this.root = root;
      }

      public View getRoot() {
         return root;
      }
   }
}
