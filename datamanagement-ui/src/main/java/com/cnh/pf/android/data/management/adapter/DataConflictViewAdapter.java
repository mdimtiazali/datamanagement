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
import android.graphics.Color;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.model.TypedValue;
import com.google.common.base.Strings;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import roboguice.RoboGuice;
import roboguice.inject.InjectResource;

/**
 * Adapter used to feed data to Conflict Resolution View
 * @author oscar.salazar@cnhind.com
 */
public class DataConflictViewAdapter extends DataManagementBaseAdapter {
   private static final Logger logger = LoggerFactory.getLogger(DataConflictViewAdapter.class);

   @Inject
   LayoutInflater layoutInflater;
   @InjectResource(R.string.cancel)
   String cancelStr;
   @InjectResource(R.string.done)
   String doneStr;

   public DataConflictViewAdapter(Activity context, List<Operation> operations) {
      super(context, operations);
      RoboGuice.getInjector(context).injectMembersWithoutViews(this);
   }

   private static class ColumnViewHolder extends ViewHolder{
      final TextView conflictFileTv;
      final LinearLayout exitingFile;
      final LinearLayout columnsLayout;
      final LinearLayout newFile;

      public ColumnViewHolder(View root) {
         super(root);
         conflictFileTv = (TextView) root.findViewById(R.id.conflict_file_tv);
         exitingFile = (LinearLayout) root.findViewById(R.id.existing_file);
         columnsLayout = (LinearLayout) root.findViewById(R.id.description_columns);
         newFile = (LinearLayout) root.findViewById(R.id.new_file);
      }
   }

   private static class LayoutViewHolder {
      final TextView headerTv;
      final LinearLayout descriptionArea;
      final List<TextView> columns = new ArrayList<TextView>();

      public LayoutViewHolder(View root) {
         headerTv = (TextView) root.findViewById(R.id.header_tv);
         descriptionArea = (LinearLayout) root.findViewById(R.id.description_area);
      }
   }

   private OnActionSelectedListener actionListener = new OnActionSelectedListener() {
      @Override
      public void onButtonSelected(final DialogView dialog, Action action) {
         if (Action.ACTION1.equals(action)) {
            Operation op = operationList.get(position);
            final DialogView newNameDialog = new DialogView(context);
            newNameDialog.setTitle(context.getResources().getString(R.string.new_id_title));
            View view = layoutInflater.inflate(R.layout.rename_file, null);
            newNameDialog.setBodyView(view);
            final EditText textEntry = (EditText) view.findViewById(R.id.file_name);
            if (!Strings.isNullOrEmpty(op.getNewName())) {
               textEntry.setText(op.getNewName());
            }
            newNameDialog.setFirstButtonText(doneStr);
            newNameDialog.setSecondButtonText(cancelStr);
            newNameDialog.showThirdButton(false);
            newNameDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface nameDialog, int which) {
                  if (which == DialogViewInterface.BUTTON_FIRST) {
                     String newName = textEntry.getText().toString();
                     if (!Strings.isNullOrEmpty(newName)) {
                        operationList.get(position).setNewName(newName);
                        operationList.get(position).setAction(Operation.Action.COPY_AND_KEEP);
                        newNameDialog.dismiss();
                        position++;
                        checkAndUpdateActive();
                     }
                  }
                  else if (which == DialogViewInterface.BUTTON_SECOND) {
                     newNameDialog.dismiss();
                  }
                  enableButtons(dialog, true);
               }
            });
            enableButtons(dialog, false);
            ((TabActivity) newNameDialog.getContext()).showPopup(newNameDialog, true);
         }
         else if (Action.ACTION2.equals(action)) {
            operationList.get(position).setAction(Operation.Action.COPY_AND_REPLACE);
            position++;
            checkAndUpdateActive();
         }
      }
   };

   void enableButtons(DialogView dialog, boolean enabled) {
      dialog.setFirstButtonEnabled(enabled);
      dialog.setSecondButtonEnabled(enabled);
      dialog.setThirdButtonEnabled(enabled);
   }

   @Override
   protected ViewHolder updateView(ViewHolder convertView) {
      final Operation operation = operationList.get(position);
      final ColumnViewHolder viewHolder = (ColumnViewHolder) convertView;
      //a <type> named <name> already exists
      if ((operation.getData().getType().equals(DataTypes.PRODUCT_MIX)) || (operation.getData().getType().equals(DataTypes.PRODUCT)))
      {
         viewHolder.conflictFileTv.setText(context.getResources().getString(R.string.duplicate_file, DataTypes.PRODUCT + " / " + DataTypes.PRODUCT_MIX, operation.getData().getName()));
      }
      else
      {
         viewHolder.conflictFileTv.setText(context.getResources().getString(R.string.duplicate_file, getTypeString(operation.getData().getType()), operation.getData().getName()));
      }

      populateDescriptionLayout(viewHolder.columnsLayout, viewHolder.exitingFile, viewHolder.newFile, operation.getConflictDataTyped(), operation.getData().getData());
      return convertView;
   }

   @Override
   protected ViewHolder createView() {
      return new ColumnViewHolder(layoutInflater.inflate(R.layout.data_conflict, null));
   }

   private String getTypeString(String type) {
      return type.substring(type.lastIndexOf(".") + 1);
   }

   /** Generates description area for the existing entity and new entitiy, reuses layouts */
   private void populateDescriptionLayout(LinearLayout rowLayout, LinearLayout existingFileLayout, LinearLayout newFileLayout, Map<String, TypedValue> existingMap,
         Map<String, TypedValue> newMap) {
      /** Viewholder to re-use textviews within description area*/
      LayoutViewHolder existingFileVH = getViewHolder(existingFileLayout);
      LayoutViewHolder newFileVH = getViewHolder(newFileLayout);
      LayoutViewHolder rowVH = getViewHolder(rowLayout);

      Operation op = operationList.get(position);
      String type = getTypeString(op.getData().getType());
      if (existingFileVH.headerTv.getText() == null || existingFileVH.headerTv.getText().equals("")) {
         existingFileVH.headerTv.setText(context.getResources().getString(R.string.existing_file));
      }
      if (newFileVH.headerTv.getText() == null || newFileVH.headerTv.getText().equals("")) {
         newFileVH.headerTv.setText(context.getResources().getString(R.string.new_file));
      }

      Map<String, Pair<String, String>> rowMap = new HashMap<String, Pair<String, String>>();

      Set<String> propNames = new HashSet<String>();
      propNames.addAll(existingMap.keySet());
      propNames.addAll(newMap.keySet());
      for (String key : propNames) {
         if (key.startsWith("_")) continue; //skip 'private' properties
         TypedValue existingVal = existingMap.get(key);
         TypedValue newVal = newMap.get(key);
         rowMap.put(key,
               new Pair<String, String>(existingVal != null ? String.valueOf(existingVal.getFieldValue()) : "", newVal != null ? String.valueOf(newVal.getFieldValue()) : ""));
      }

      int rowNum = 0;
      for (Map.Entry<String, Pair<String, String>> entry : rowMap.entrySet()) {
         TextView existingFileRow;
         TextView newFileRow;
         TextView row;
         if (existingFileVH.columns.size() > rowNum) {
            existingFileRow = existingFileVH.columns.get(rowNum);
            newFileRow = newFileVH.columns.get(rowNum);
            row = rowVH.columns.get(rowNum);
         }
         else {
            existingFileRow = createRow();
            existingFileVH.columns.add(existingFileRow);
            existingFileVH.descriptionArea.addView(existingFileRow);
            newFileRow = createRow();
            newFileVH.columns.add(newFileRow);
            newFileVH.descriptionArea.addView(newFileRow);
            row = createRow();
            rowVH.columns.add(row);
            rowLayout.addView(row);
         }
         row.setText(entry.getKey());
         existingFileRow.setText(entry.getValue().first);
         newFileRow.setText(entry.getValue().second);

         /** Highlight matching rows red for conflict */
         if (existingFileRow.getText().equals(newFileRow.getText())) row.setTextColor(Color.RED);
         rowNum++;
      }

      /** Remove any previous unused textviews, using existingFileVH index but all three layouts have similar number of views */
      if (existingFileVH.columns.size() != 0 && rowNum < existingFileVH.columns.size() - 1) {
         int toRemove = existingFileVH.columns.size() - 1 - rowNum;
         while (toRemove != 0) {
            int location = existingFileVH.columns.size() - 1;
            /** Revome from list and layout */
            TextView removed = existingFileVH.columns.remove(location);
            existingFileVH.descriptionArea.removeView(removed);
            removed = newFileVH.columns.remove(location);
            newFileVH.descriptionArea.removeView(removed);
            removed = rowVH.columns.remove(location);
            rowLayout.removeView(removed);
            toRemove--;
         }
      }
   }

   /** Finds ViewHolder contained within layout, else it creates one */
   private static LayoutViewHolder getViewHolder(LinearLayout layout) {
      if (layout.getTag() == null) {
         LayoutViewHolder lHolder = new LayoutViewHolder(layout);
         layout.setTag(lHolder);
      }
      return (LayoutViewHolder) layout.getTag();
   }

   /** Create a row for each row of data */
   private TextView createRow() {
      TextView tv = new TextView(context);
      tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
      tv.setGravity(Gravity.CENTER_HORIZONTAL);
      tv.setTextAppearance(context, R.style.TextAppearance_Data_File_Column);
      int dpAsPixels = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics());
      tv.setPadding(0, dpAsPixels, 0, dpAsPixels);
      return tv;
   }

   @Override
   public OnActionSelectedListener getActionListener() {
      return actionListener;
   }

   @Override
   protected boolean shouldShowView() {
      return operationList.get(position).isConflict();
   }
}
