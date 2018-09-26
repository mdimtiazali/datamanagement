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
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TreeEntityHelper;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import roboguice.RoboGuice;

/**
 * Adapter used to feed data to Conflict Resolution View
 * @author oscar.salazar@cnhind.com
 */
public class DataConflictViewAdapter extends DataManagementBaseAdapter {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementBaseAdapter.class);

   @Inject
   LayoutInflater layoutInflater;

   protected String textualFeedbackString;
   protected boolean reuseAction = false;

   protected int progressTotalConflicts; //keeps track of all conflicts in the list of operations
   protected int progressCurrentConflict; //keeps track of the already solved conflicts
   //position keeps track of the current position under consideration in the operations list

   protected EnumMap<ConflictDataType, List<Integer>> conflictMapping;

   protected ConflictDataType lastConflictDataType = null;

   /**
    * This enum groups different conflict data types to GFF (Grower, Farm, Field) and OTHER (all remaining)
    */
   public enum ConflictDataType {
      GFF, OTHER;
   }

   protected OnConflictTypeChangedListener onConflictTypeChangedListener;

   public DataConflictViewAdapter(Activity context, List<Operation> operations) {
      super(context, operations);
      RoboGuice.getInjector(context).injectMembersWithoutViews(this);

      textualFeedbackString = context.getString(R.string.import_conflict_dialog_textual_process_feedback_text);

      //initialize mapping
      conflictMapping = new EnumMap<ConflictDataType, List<Integer>>(ConflictDataType.class);
      conflictMapping.put(ConflictDataType.GFF, new ArrayList<Integer>());
      conflictMapping.put(ConflictDataType.OTHER, new ArrayList<Integer>());

      progressCurrentConflict = 1;
      progressTotalConflicts = 0;
      for (int currentOperationPosition = 0; currentOperationPosition < operationList.size(); currentOperationPosition++) {
         Operation currentOperation = operationList.get(currentOperationPosition);
         if (currentOperation.isConflict()) {
            //fill mapping with conflicting operations
            ObjectGraph operationData = currentOperation.getData();
            if (operationData != null) {
               String operationType = operationData.getType();
               ConflictDataType insertAsDataType = getConflictDataTypeAsEnum(operationType);
               conflictMapping.get(insertAsDataType).add(currentOperationPosition);
               //count number of total conflicts
               progressTotalConflicts++;
            }
            else {
               logger.error("Could not get operation data from operation! {}", currentOperation);
            }
         }
      }
   }

   /**
    * This method returns the conflict data type as a enum corresponding the given operationType
    * @param operationType conflict data type as string
    * @return ConflictDataType that corresponds the given operationType (GFF or OTHER)
    */
   public static ConflictDataType getConflictDataTypeAsEnum(String operationType) {
      if (DataTypes.GROWER.equals(operationType) || DataTypes.FARM.equals(operationType) || DataTypes.FIELD.equals(operationType)) {
         return ConflictDataType.GFF;
      }
      else {
         return ConflictDataType.OTHER;
      }
   }

   private static class DialogViewHolder extends ViewHolder {
      final TextView conflictFileTv;
      final TextView newName;
      final TextView oldName;
      final TextView newNameTitle;
      final TextView oldNameTitle;
      final CheckBox reuseActionCheckBox;
      final TextView reuseActionCheckBoxLabel;
      final TextView textualFeedback;
      final LinearLayout textualFeedbackContainer;
      final LinearLayout reuseActionContainer;

      public DialogViewHolder(View root) {
         super(root);
         conflictFileTv = (TextView) root.findViewById(R.id.conflict_file_tv);
         newName = (TextView) root.findViewById(R.id.import_conflict_dialog_new_field_text);
         oldName = (TextView) root.findViewById(R.id.import_conflict_dialog_existing_field_text);
         newNameTitle = (TextView) root.findViewById(R.id.import_conflict_dialog_new_title);
         oldNameTitle = (TextView) root.findViewById(R.id.import_conflict_dialog_existing_title);

         reuseActionCheckBox = (CheckBox) root.findViewById(R.id.import_conflict_dialog_reuse_action_checkbox);
         reuseActionCheckBoxLabel = (TextView) root.findViewById(R.id.import_conflict_dialog_reuse_action_checkbox_label);
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
         fromIndex = Math.max(0, fromIndex);
         int maxIndex = conflictMapping.get(ConflictDataType.GFF).size() + conflictMapping.get(ConflictDataType.OTHER).size() - 1;
         toIndex = Math.min(maxIndex, toIndex);
         //apply action to the given range
         for (int i = fromIndex; i <= toIndex; i++) {
            int conflictId = getMappedConflictId(i);
            Operation op = operationList.get(conflictId);
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
         String currentTypeString = getType(getCurrentMappedConflictId());
         ConflictDataType conflictDataType = getConflictDataTypeAsEnum(currentTypeString);
         int lastIndex;
         if (ConflictDataType.GFF.equals(conflictDataType)) {
            //apply action to all remaining gff items
            lastIndex = conflictMapping.get(ConflictDataType.GFF).size();
         }
         else {
            //apply action to all remaining regular items
            lastIndex = (conflictMapping.get(ConflictDataType.GFF).size()) + conflictMapping.get(ConflictDataType.OTHER).size();
         }
         setActionForOperationsBetween(position, lastIndex, operationAction);
         position = lastIndex;
         progressCurrentConflict = lastIndex;
      }
      else {
         //use selected action for single conflict
         operationList.get(getCurrentMappedConflictId()).setAction(operationAction);
         position++;
         progressCurrentConflict++;
      }
      checkAndUpdateActive();
   }

   @Override
   protected void checkAndUpdateActive() {
      if (onTargetsSelectedListener != null) {
         if (!shouldShowView()) {
            onTargetsSelectedListener.onCompletion(operationList);
         }
         else {
            onTargetSelectedListener.onTargetSelected();
            checkAndNotifyConflictTypeChange();
         }
      }
   }

   private OnActionSelectedListener actionListener = new OnActionSelectedListener() {
      @Override
      public void onButtonSelected(final DialogView dialog, Action action) {
         if (Action.ACTION1.equals(action)) {
            //Merge action
            performAction(Operation.Action.MERGE);
         }
         else if (Action.ACTION2.equals(action)) {
            //Keep Both action
            performAction(Operation.Action.COPY_AND_KEEP);
         }
         else if (Action.ACTION3.equals(action)) {
            //Replace action
            performAction(Operation.Action.COPY_AND_REPLACE);
         }
         else if (Action.ACTION4.equals(action)) {
            //Skip action
            performAction(Operation.Action.SKIP);
         }
      }
   };

   /**
    * This method returns the current conflict id (position in operationList)
    * @return Current conflict id
    */
   private int getCurrentMappedConflictId() {
      return getMappedConflictId(position);
   }

   /**
    * This method returns the current conflict id (position in operationList)
    * @return Current conflict id
    */
   private int getMappedConflictId(int temporaryPosition) {
      final int operationId;
      if (temporaryPosition < conflictMapping.get(ConflictDataType.GFF).size()) {
         operationId = conflictMapping.get(ConflictDataType.GFF).get(temporaryPosition);
      }
      else {
         final int positionOffset = temporaryPosition - conflictMapping.get(ConflictDataType.GFF).size();
         if (positionOffset < conflictMapping.get(ConflictDataType.OTHER).size()) {
            operationId = conflictMapping.get(ConflictDataType.OTHER).get(positionOffset);
         }
         else {
            operationId = -1;
            logger.error("Invalid position requested; conflictMapping does only contain {} ({} requested) elements for OTHER conflicts!",
                  conflictMapping.get(ConflictDataType.OTHER).size(), positionOffset);
         }
      }
      return operationId;
   }

   private void checkAndNotifyConflictTypeChange() {
      String currentTypeString = getType(getCurrentMappedConflictId());
      ConflictDataType currentTypeEnum = getConflictDataTypeAsEnum(currentTypeString);
      if (onConflictTypeChangedListener != null && (lastConflictDataType == null || !lastConflictDataType.equals(currentTypeEnum))) {
         lastConflictDataType = currentTypeEnum;
         onConflictTypeChangedListener.onConflictTypeChanged(currentTypeEnum);
      }
   }

   @Override
   @SuppressWarnings("squid:CommentedOutCodeLine")
   protected ViewHolder updateView(ViewHolder convertView) {
      //if datatype changed, notify for change
      checkAndNotifyConflictTypeChange();

      final Operation operation = operationList.get(getCurrentMappedConflictId());
      final DialogViewHolder viewHolder = (DialogViewHolder) convertView;

      String dataFieldName = getTypeString(operation.getData().getType());

      viewHolder.conflictFileTv.setText(context.getResources().getString(R.string.duplicate_file, dataFieldName, operation.getData().getName()));

      String oldName = operation.getData().getName();
      //keep this line until the uiux spec contains the suggested name instead of 3 times the same already existing name
      //String suggestedNewName = operation.getNewName();

      //reset reuse checkbox
      viewHolder.reuseActionCheckBox.setChecked(false);
      setReuseAction(false);

      //set label text
      int reuseLabelId;
      if (ConflictDataType.GFF.equals(getConflictDataTypeAsEnum(getType(getCurrentMappedConflictId())))) {
         reuseLabelId = R.string.import_conflict_dialog_reuse_gff_action_text;
      }
      else {
         reuseLabelId = R.string.import_conflict_dialog_reuse_other_action_text;
      }
      viewHolder.reuseActionCheckBoxLabel.setText(reuseLabelId);

      viewHolder.oldName.setText(oldName);
      viewHolder.newName.setText(oldName); //should be suggestedNewName, but uiux spec currently contains the old name 3 times

      String newNameTitle = context.getResources().getString(R.string.import_conflict_dialog_new_text);
      String existingNameTitle = context.getResources().getString(R.string.import_conflict_dialog_existing_text);

      viewHolder.oldNameTitle.setText(String.format(existingNameTitle, dataFieldName));
      viewHolder.newNameTitle.setText(String.format(newNameTitle, dataFieldName));

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

   /**
    * Translates the given type from upper case to translation. If no translation is found, the type is reused.
    * @param type DataType as string
    * @return Translation of given type to country specific string
    */
   private String getTypeString(String type) {
      String typeUpperCase = type.substring(type.lastIndexOf('.') + 1);
      int resourceIDOfDataType = TreeEntityHelper.getDataTypeName(typeUpperCase);
      if (resourceIDOfDataType > 0) {
         return context.getResources().getString(resourceIDOfDataType);
      }
      else {
         return type;
      }
   }

   @Override
   public OnActionSelectedListener getActionListener() {
      return actionListener;
   }

   @Override
   protected boolean shouldShowView() {
      return position < progressTotalConflicts;
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

   /**
    * This method applies the given onConflictTypeChangedListener to this class
    * This listener will become active as the conflict type changes from GFF to OTHER or vice versa
    * @param onConflictTypeChangedListener Instance implementing the OnConflictTypeChangedListener interface
    */
   public void setOnConflictTypeChangedListener(OnConflictTypeChangedListener onConflictTypeChangedListener) {
      this.onConflictTypeChangedListener = onConflictTypeChangedListener;
      //if datatype changed, notify for change
      checkAndNotifyConflictTypeChange();
   }

   /**
    * This interface defines  the action to be performed as the conflict type is changing
    */
   public interface OnConflictTypeChangedListener {
      /**
       * Method to be called if the conflict type is changing
       */
      void onConflictTypeChanged(ConflictDataType currentConflictType);
   }
}
