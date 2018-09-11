/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.widget.control.CheckBox;
import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.R;
import com.google.inject.Inject;

import java.util.List;

import roboguice.RoboGuice;

/**
 * Adapter used to feed data to Conflict Resolution View
 * @author oscar.salazar@cnhind.com
 */
public class DataConflictViewAdapter extends DataManagementBaseAdapter {
   @Inject
   LayoutInflater layoutInflater;

   protected String textualFeedbackString;
   protected boolean reuseAction = false;

   protected int progressTotalConflicts; //keeps track of all conflicts in the list of operations
   protected int progressCurrentConflict; //keeps track of the already solved conflicts
   //position keeps track of the current position under consideration in the operations list

   public DataConflictViewAdapter(Activity context, List<Operation> operations) {
      super(context, operations);
      RoboGuice.getInjector(context).injectMembersWithoutViews(this);

      textualFeedbackString = context.getString(R.string.import_conflict_dialog_textual_process_feedback_text);

      progressCurrentConflict = 1;
      progressTotalConflicts = 0;
      for (int currentOperationPosition = 0; currentOperationPosition < operationList.size(); currentOperationPosition++) {
         if (operationList.get(currentOperationPosition).isConflict()) {
            progressTotalConflicts++;
         }
      }
   }

   private static class DialogViewHolder extends ViewHolder {
      final TextView conflictFileTv;
      final TextView newName;
      final TextView oldName;
      final CheckBox reuseActionCheckBox;
      final TextView textualFeedback;
      final LinearLayout textualFeedbackContainer;
      final LinearLayout reuseActionContainer;

      public DialogViewHolder(View root) {
         super(root);
         conflictFileTv = (TextView) root.findViewById(R.id.conflict_file_tv);
         newName = (TextView) root.findViewById(R.id.import_conflict_dialog_new_field_text);
         oldName = (TextView) root.findViewById(R.id.import_conflict_dialog_existing_field_text);

         reuseActionCheckBox = (CheckBox) root.findViewById(R.id.import_conflict_dialog_reuse_action_checkbox);
         reuseActionContainer = (LinearLayout) root.findViewById(R.id.import_conflict_dialog_reuse_action_container);
         textualFeedbackContainer = (LinearLayout) root.findViewById(R.id.import_conflict_dialog_textual_process_feedback_container);
         textualFeedback = (TextView) root.findViewById(R.id.import_conflict_dialog_textual_process_feedback_text_view);
      }
   }

   /**
    * Applies the given action to all operations between fromIndex to toIndex
    * @param fromIndex Index of operation to apply the action to - first in the list (must be >= 0)
    * @param toIndex Index of operation to apply the action to - last in the list (must be < size of list)
    * @param action Action to be applied to operations in [fromIndex .. toIndex]
    */
   private void setActionForOperationsBetween(int fromIndex, int toIndex, Operation.Action action) {
      if (fromIndex <= toIndex && operationList != null) {
         //verify proper set of limits
         if (toIndex >= operationList.size()) {
            toIndex = operationList.size() - 1;
         }
         if (fromIndex < 0) {
            fromIndex = 0;
         }
         for (int currentIndex = fromIndex; currentIndex <= toIndex; currentIndex++) {
            Operation op = operationList.get(currentIndex);
            if (op != null) {
               op.setAction(action);
            }
         }
      }
   }

   /**
    * Performs the given operationAction to the currently active operation (or set of operations, if reuseAction is set)
    * @param operationAction Action to be performed
    */
   private void performAction(Operation.Action operationAction) {
      if (getReuseAction()) {
         //use selected action for all remaining conflicts
         int lastIndex = operationList.size() - 1;
         setActionForOperationsBetween(position, lastIndex, operationAction);
         position = lastIndex + 1;
         progressCurrentConflict = progressTotalConflicts;
      }
      else {
         //use selected action for single conflict
         operationList.get(position).setAction(operationAction);
         position++;
         progressCurrentConflict++;
      }
      checkAndUpdateActive();
   }

   private OnActionSelectedListener actionListener = new OnActionSelectedListener() {
      @Override
      public void onButtonSelected(final DialogView dialog, Action action) {
         if (Action.ACTION1.equals(action)) {
            //Keep Both action
            performAction(Operation.Action.COPY_AND_KEEP);
         }
         else if (Action.ACTION2.equals(action)) {
            //Replace action
            performAction(Operation.Action.COPY_AND_REPLACE);
         }
      }
   };

   @Override
   protected ViewHolder updateView(ViewHolder convertView) {
      final Operation operation = operationList.get(position);
      final DialogViewHolder viewHolder = (DialogViewHolder) convertView;
      //a <type> named <name> already exists
      if ((operation.getData().getType().equals(DataTypes.PRODUCT_MIX)) || (operation.getData().getType().equals(DataTypes.PRODUCT))) {
         viewHolder.conflictFileTv
               .setText(context.getResources().getString(R.string.duplicate_file, DataTypes.PRODUCT + " / " + DataTypes.PRODUCT_MIX, operation.getData().getName()));
      }
      else {
         viewHolder.conflictFileTv.setText(context.getResources().getString(R.string.duplicate_file, getTypeString(operation.getData().getType()), operation.getData().getName()));
      }

      String oldName = operation.getData().getName();
      String newName = operation.getNewName();

      viewHolder.oldName.setText(oldName);
      viewHolder.newName.setText(newName);

      //update textual progress
      if (operationList.size() > 1) {
         viewHolder.textualFeedback.setText(String.format(textualFeedbackString, progressCurrentConflict, progressTotalConflicts));
      }

      return convertView;
   }

   @Override
   protected ViewHolder createView() {
      DialogViewHolder dialogView = new DialogViewHolder(layoutInflater.inflate(R.layout.data_conflict, null));

      if (operationList.size() > 1) {
         dialogView.textualFeedbackContainer.setVisibility(View.VISIBLE);
         dialogView.reuseActionContainer.setVisibility(View.VISIBLE);

         dialogView.reuseActionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               setReuseAction(isChecked);
            }
         });
      }
      else {
         dialogView.textualFeedbackContainer.setVisibility(View.GONE);
         dialogView.reuseActionContainer.setVisibility(View.GONE);
      }

      return dialogView;

   }

   private String getTypeString(String type) {
      return type.substring(type.lastIndexOf('.') + 1);
   }

   @Override
   public OnActionSelectedListener getActionListener() {
      return actionListener;
   }

   @Override
   protected boolean shouldShowView() {
      return operationList.get(position).isConflict();
   }

   /**
    * Sets the reuse-action flag
    * @param reuseAction True if same action should be reused for all conflicts, false otherwise
    */
   public void setReuseAction(boolean reuseAction) {
      this.reuseAction = reuseAction;
   }

   /**
    * Returns the reuse-action flag
    * @return True if same action should be reused for all conflicts, false otherwise
    */
   public boolean getReuseAction() {
      return this.reuseAction;
   }
}
