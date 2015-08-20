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

import java.util.List;

import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.cnh.pf.android.data.management.R;
import com.cnh.jgroups.Operation;

/**
 * Adapter used to feed data to Conflict Resolution View
 * Created by oscar.salazar@cnhind.com
 */
public class ConflictResolutionViewAdapter extends DataManagementBaseAdapter {
   private static final Logger logger = LoggerFactory.getLogger(ConflictResolutionViewAdapter.class);

   public ConflictResolutionViewAdapter(Activity context, List<Operation> operations) {
      super(context, operations);
   }


   private class CViewHolder extends ViewHolder {
      private Button copyReplaceBtn;
      private Button dontCopyBtn;
      private Button keepBothBtn;
      private EditText fileNameView;
      private Button doneBtn;
      private LinearLayout newNameLayout;
   }

   @Override
   public View getView(View convertView) {
      View newView = null;
      if (activeOperation < totalOperation) {
         if (convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            newView = inflater.inflate(R.layout.conflict_resolution, null);
            final CViewHolder viewHolder = new CViewHolder();
            viewHolder.typeView = (TextView) newView.findViewById(R.id.object_type);
            viewHolder.nameView = (TextView) newView.findViewById(R.id.object_name);
            viewHolder.copyReplaceBtn = (Button) newView.findViewById(R.id.copy_and_replace_btn);
            viewHolder.dontCopyBtn = (Button) newView.findViewById(R.id.dont_copy_btn);
            viewHolder.keepBothBtn = (Button) newView.findViewById(R.id.keep_both_btn);
            viewHolder.newNameLayout = (LinearLayout) newView.findViewById(R.id.new_name_layout);
            viewHolder.fileNameView= (EditText) newView.findViewById(R.id.file_name);
            viewHolder.doneBtn= (Button) newView.findViewById(R.id.done_btn);
            Button.OnClickListener clickListener = new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                  if (v.equals(viewHolder.copyReplaceBtn)) {
                     operationList.get(activeOperation).setAction(Operation.Action.COPY_AND_REPLACE);
                     clearInputLayout(viewHolder);
                     activeOperation++;
                     checkAndUpdateActive();
                  }
                  else if (v.equals(viewHolder.dontCopyBtn)) {
                     operationList.remove(activeOperation);
                     clearInputLayout(viewHolder);
                     totalOperation--;
                     checkAndUpdateActive();
                  }
                  else if (v.equals(viewHolder.keepBothBtn)) {
                     viewHolder.newNameLayout.setVisibility(View.VISIBLE);
                     viewHolder.doneBtn.setEnabled(true);
                     viewHolder.doneBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                           String newName = viewHolder.fileNameView.getText().toString();
                           if (newName.equals("")) {
                              Toast.makeText(context.getApplicationContext(), "New name cannot be blank", Toast.LENGTH_LONG).show();
                           }
                           else {
                              operationList.get(activeOperation).setNewName(newName);
                              operationList.get(activeOperation).setAction(Operation.Action.COPY_AND_KEEP);
                              clearInputLayout(viewHolder);
                              activeOperation++;
                              checkAndUpdateActive();
                           }
                        }
                     });
                  }
               }
            };
            viewHolder.copyReplaceBtn.setOnClickListener(clickListener);
            viewHolder.dontCopyBtn.setOnClickListener(clickListener);
            viewHolder.keepBothBtn.setOnClickListener(clickListener);
            newView.setTag(viewHolder);
         } else {
            newView = convertView;
         }
         logger.debug("Solve Conflict for object: " + activeOperation);
         CViewHolder viewHolder = (CViewHolder) newView.getTag();
         Operation operation = operationList.get(activeOperation);
         viewHolder.typeView.setText(operation.getData().getType());
         viewHolder.nameView.setText(operation.getData().getName());
         targetView = newView;
      }
      targetView = newView;
      return newView;
   }

   private void clearInputLayout(CViewHolder viewHolder) {
      viewHolder.fileNameView.setText("");
      viewHolder.keepBothBtn.setActivated(false);
      viewHolder.newNameLayout.setVisibility(View.GONE);
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
