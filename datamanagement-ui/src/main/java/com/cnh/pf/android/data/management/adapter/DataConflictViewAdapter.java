/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.graphics.Color;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.R;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;
import roboguice.inject.InjectResource;

import static android.R.attr.id;

/**
 * Adapter used to feed data to Conflict Resolution View
 * @author oscar.salazar@cnhind.com
 */
public class DataConflictViewAdapter extends DataManagementBaseAdapter {
   private static final Logger logger = LoggerFactory.getLogger(DataConflictViewAdapter.class);

   @Inject LayoutInflater layoutInflater;
   @InjectResource(R.string.cancel) String cancelStr;
   @InjectResource(R.string.done) String doneStr;

   public DataConflictViewAdapter(Activity context, List<Operation> operations) {
      super(context, operations);
      RoboGuice.getInjector(context).injectMembersWithoutViews(this);
   }

   private class ColumnViewHolder {
      TextView conflictFileTv;
      LinearLayout exitingFile;
      LinearLayout columnsLayout;
      LinearLayout newFile;
   }

   private class LayoutViewHolder {
      TextView headerTv;
      LinearLayout descriptionArea;
      List<TextView> columns;
   }

   private OnActionSelectedListener actionListener = new OnActionSelectedListener() {
      @Override
      public void onButtonSelected(Operation.Action action) {
         if (action.equals(Operation.Action.COPY_AND_KEEP)) {
            Operation op = operationList.get(activeOperation);
            final DialogView newNameDialog = new DialogView(context);
            newNameDialog.setTitle(context.getResources().getString(R.string.new_id_title));
            View view = layoutInflater.inflate(R.layout.rename_file, null);
            newNameDialog.setBodyView(view);
            final EditText textEntry = (EditText) view.findViewById(R.id.file_name);
            if(!Strings.isNullOrEmpty(op.getNewName())) {
               textEntry.setText(op.getNewName());
            }
            newNameDialog.setFirstButtonText(doneStr);
            newNameDialog.setSecondButtonText(cancelStr);
            newNameDialog.showThirdButton(false);
            newNameDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialog, int which) {
                  if (which == DialogViewInterface.BUTTON_FIRST) {
                     String newName = textEntry.getText().toString();
                     if (!newName.equals("")) {
                        operationList.get(activeOperation).setNewName(newName);
                        operationList.get(activeOperation).setAction(Operation.Action.COPY_AND_KEEP);
                        newNameDialog.dismiss();
                        activeOperation++;
                        checkAndUpdateActive();
                     }
                  }
                  else if (which == DialogViewInterface.BUTTON_SECOND) {
                     newNameDialog.dismiss();
                  }
               }
            });
            ((TabActivity) newNameDialog.getContext()).showPopup(newNameDialog, true);
         }
         else if (action.equals(Operation.Action.COPY_AND_REPLACE)) {
            logger.debug("replace");
            operationList.get(activeOperation).setAction(Operation.Action.COPY_AND_REPLACE);
            activeOperation++;
            checkAndUpdateActive();
         }
      }
   };

   @Override
   public View getView(View convertView) {
      View newView = convertView;
      if (activeOperation < totalOperation) {
         Operation operation = operationList.get(activeOperation);
         if (convertView == null) {
            newView = layoutInflater.inflate(R.layout.data_conflict, null);
            ColumnViewHolder viewHolder = new ColumnViewHolder();
            viewHolder.conflictFileTv = (TextView) newView.findViewById(R.id.conflict_file_tv);
            viewHolder.exitingFile = (LinearLayout) newView.findViewById(R.id.existing_file);
            viewHolder.columnsLayout = (LinearLayout) newView.findViewById(R.id.description_columns);
            viewHolder.newFile = (LinearLayout) newView.findViewById(R.id.new_file);
            newView.setTag(viewHolder);
         }
         logger.debug("Solve Conflict for object: " + activeOperation);
         ColumnViewHolder viewHolder = (ColumnViewHolder) newView.getTag();

         //a <type> named <name> already exists
         viewHolder.conflictFileTv.setText(
               context.getResources().getString(R.string.duplicate_file,
                     getTypeString(operation.getData().getType()),
                     operation.getData().getName()));
         populateDescriptionLayout(viewHolder.columnsLayout, viewHolder.exitingFile, viewHolder.newFile, operation.getConflictData(), operation.getData().getData());
      }
      targetView = newView;
      return newView;
   }

   private String getTypeString(String type) {
      return type.substring(type.lastIndexOf(".") + 1);
   }

   /** Generates description area for the existing entity and new entitiy, reuses layouts */
   private void populateDescriptionLayout(LinearLayout rowLayout, LinearLayout existingFileLayout, LinearLayout newFileLayout,  Map<String, String> existingMap, Map<String, String> newMap) {
      logger.debug("Conflict existing data: {}\nNew data {}", existingMap, newMap);
      /** Viewholder to re-use textviews within description area*/
      LayoutViewHolder existingFileVH = getViewHolder(existingFileLayout);
      LayoutViewHolder newFileVH = getViewHolder(newFileLayout);
      LayoutViewHolder rowVH = (LayoutViewHolder) rowLayout.getTag();
      if (rowVH == null) {
         rowVH = new LayoutViewHolder();
         rowVH.columns = new ArrayList<TextView>();
         rowLayout.setTag(rowVH);
      }
      Operation op = operationList.get(activeOperation);
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
      for(String key : propNames) {
         if(key.startsWith("_")) continue; //skip 'private' properties
         rowMap.put(key, new Pair<String, String>(existingMap.get(key), newMap.get(key)));
      }

      int rowNum = 0;
      for (Map.Entry<String, Pair<String, String>> entry : rowMap.entrySet()) {
         logger.debug("descriptionMap: {} : {}", entry.getKey(), entry.getValue().toString());
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
         if (existingFileRow.getText().equals(newFileRow.getText()))
            row.setTextColor(Color.RED);
         rowNum++;
      }

      /** Remove any previous unused textviews, using existingFileVH index but all three layouts have similar number of views */
      if (existingFileVH.columns.size() != 0 && rowNum < existingFileVH.columns.size()-1) {
         int toRemove = existingFileVH.columns.size()-1-rowNum;
         logger.debug("Items to remove: {}", toRemove);
         while (toRemove != 0) {
            logger.debug("Removing {} from {} items", toRemove, existingFileVH.columns.size());
            int location = existingFileVH.columns.size()-1;
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
   private LayoutViewHolder getViewHolder(LinearLayout layout) {
      if (layout.getTag() == null) {
         LayoutViewHolder lHolder = new LayoutViewHolder();
         lHolder.headerTv = (TextView) layout.findViewById(R.id.header_tv);
         lHolder.descriptionArea = (LinearLayout) layout.findViewById(R.id.description_area);
         lHolder.columns = new ArrayList<TextView>();
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
      int dpAsPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics());
      tv.setPadding(0,dpAsPixels,0, dpAsPixels);
      return tv;
   }

   @Override
   public OnActionSelectedListener getActionListener() {
      return actionListener;
   }

   @Override
   protected void checkAndUpdateActive() {
      logger.debug("active:" + activeOperation + ", total:" + totalOperation);
      while (activeOperation < totalOperation && !operationList.get(activeOperation).isConflict()) {
         activeOperation++;
      }
      if (activeOperation == totalOperation) {
         onTargetsSelectedListener.onCompletion(operationList);
      } else {
         onTargetSelectedListener.onTargetSelected(false, targetView);
      }
   }
}
