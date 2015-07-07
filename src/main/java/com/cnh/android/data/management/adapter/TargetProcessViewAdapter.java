/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.android.data.management.adapter;

import java.util.ArrayList;
import java.util.List;

import com.cnh.jgroups.MultiSetObjectGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.cnh.android.data.management.R;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;

/**
 * View Adapter for ProcessDialog
 * Created by oscar.salazar@cnhind.com
 */
public class TargetProcessViewAdapter extends DataManagementBaseAdapter {
   private static final Logger logger = LoggerFactory.getLogger(TargetProcessViewAdapter.class);

   private ArrayList<ObjectGraph> targets;

   public TargetProcessViewAdapter(Activity context, List<Operation> operations) {
      super(context, operations);
   }

   private class TViewHolder extends ViewHolder {
      private PickListEditable targetList;
   }

   @Override
   public View getView(View convertView) {
      View newView = null;
      if (activeOperation < totalOperation) {

         if (convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            newView = inflater.inflate(R.layout.select_target, null);
            final TViewHolder viewHolder = new TViewHolder();
            viewHolder.typeView = (TextView) newView.findViewById(R.id.object_type);
            viewHolder.nameView = (TextView) newView.findViewById(R.id.object_name);
            viewHolder.targetList = (PickListEditable) newView.findViewById(R.id.target_picklist);
            viewHolder.targetList.setAdapter(new PickListAdapter(viewHolder.targetList, context));
            viewHolder.targetList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                  if (id != PickListEditable.NO_ID) {
                     operationList.get(activeOperation).setTarget(targets.get((int) id));
                     activeOperation++;
                     checkAndUpdateActive();
                  }
               }

               @Override
               public void onNothingSelected(AdapterView<?> parent) {

               }
            });
            newView.setTag(viewHolder);
         } else {
            newView = convertView;
         }
         logger.debug("Solve Target for object: " + activeOperation);
         TViewHolder viewHolder = (TViewHolder) newView.getTag();
         Operation operation = operationList.get(activeOperation);
         if (operation.getData() instanceof MultiSetObjectGraph) {
            //TODO add UI to select options
            //for now select first option
            List<String> options = ((MultiSetObjectGraph) operation.getData()).getOptions();
            String firstOption = ((MultiSetObjectGraph) operation.getData()).getOptions().get(0);
            ArrayList<String> selected = new ArrayList<String>();
            selected.add(firstOption);
            ((MultiSetObjectGraph) operation.getData()).setSelectedOptions(selected);
         }
         viewHolder.typeView.setText(operation.getData().getType());
         viewHolder.nameView.setText(operation.getData().getName());
         viewHolder.targetList.clearList();
         targets = (ArrayList<ObjectGraph>) operation.getPotentialTargets();
         for (int i=0; i<targets.size(); i++) {
            viewHolder.targetList.addItem(new PickListItem(i, targets.get(i).getName()));
         }
      }
      targetView = newView;
      return newView;
   }

   @Override
   protected void checkAndUpdateActive() {
      while (activeOperation < totalOperation && operationList.get(activeOperation).getTarget() != null) {
         activeOperation++;
      }
      if (activeOperation == totalOperation) {
         onTargetsSelectedListener.onCompletion(operationList);
      } else {
         onTargetSelectedListener.onTargetSelected(false, targetView);
      }
   }




}
