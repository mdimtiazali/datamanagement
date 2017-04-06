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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.jgroups.MultiSetObjectGraph;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.ExportFragment;
import com.cnh.pf.android.data.management.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View Adapter for ProcessDialog
 * Created by oscar.salazar@cnhind.com
 */
public class TargetProcessViewAdapter extends DataManagementBaseAdapter {
   private static final Logger logger = LoggerFactory.getLogger(TargetProcessViewAdapter.class);

   public TargetProcessViewAdapter(Activity context, List<Operation> operations) {
      super(context, operations);
   }

   private class TViewHolder extends ViewHolder {
      final TextView typeView;
      final TextView nameView;
      PickListEditable optionList;
      final Map<String, PickListEditable> targetLists = new HashMap<String, PickListEditable>();

      public TViewHolder(View root) {
         super(root);
         typeView = (TextView) root.findViewById(R.id.object_type);
         nameView = (TextView) root.findViewById(R.id.object_name);
      }
   }

   @Override
   protected ViewHolder createView() {
      final LayoutInflater inflater = context.getLayoutInflater();
      final Operation operation = operationList.get(position);
      final TViewHolder viewHolder = new TViewHolder(inflater.inflate(R.layout.select_target, null));
      ObjectGraph firstTarget = operation.getPotentialTargets().iterator().next();
      ObjectGraph current = firstTarget.getRoot();
      //make the target picklists
      while(current != null) {
         final PickListEditable pl = (PickListEditable) inflater.inflate(R.layout.select_target_picklist, (ViewGroup)viewHolder.getRoot(), false);
         pl.setAdapter(new PickListAdapter(pl, context));
         pl.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean b) {
               if (id != PickListEditable.NO_ID) {
                  ExportFragment.ObjectPickListItem<ObjectGraph> pickItem = (ExportFragment.ObjectPickListItem<ObjectGraph>) pl.getAdapter().getItem(position);
                  if(pickItem.getObject().getChildren().isEmpty()) {
                     operationList.get(TargetProcessViewAdapter.this.position).setTarget(pickItem.getObject());
                  }
                  else {
                     //populate next picklist
                     ObjectGraph firstChild =  pickItem.getObject().getChildren().get(0);
                     PickListEditable nextPicklist = viewHolder.targetLists.get(firstChild.getType());
                     nextPicklist.clearList();
                     int i = 0;
                     for (ObjectGraph child : pickItem.getObject().getChildren()) {
                        nextPicklist.addItem(new ExportFragment.ObjectPickListItem<ObjectGraph>(i++, child.getName(), child));
                     }
                  }
               }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
         });
         viewHolder.targetLists.put(current.getType(), pl);
         TextView typeText = new TextView(context);
         typeText.setText(current.getType());
         ((ViewGroup) viewHolder.getRoot()).addView(typeText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
         ((ViewGroup) viewHolder.getRoot()).addView(pl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
         current = current.getChildren().isEmpty() ? null : current.getChildren().get(0);
      }

      if (operation.getData() instanceof MultiSetObjectGraph) {
         viewHolder.optionList = (PickListEditable) inflater.inflate(R.layout.select_target_picklist, (ViewGroup)viewHolder.getRoot(), false);
         viewHolder.optionList.setAdapter(new PickListAdapter(viewHolder.optionList, context));
         viewHolder.optionList.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean b) {
               if (id != PickListEditable.NO_ID) {
                  ExportFragment.ObjectPickListItem<String> pickItem = (ExportFragment.ObjectPickListItem<String>) viewHolder.optionList.getAdapter().getItem(position);
                  ((MultiSetObjectGraph) operationList.get(TargetProcessViewAdapter.this.position).getData()).setSelectedOption(pickItem.getObject());
               }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
         });
         ((ViewGroup) viewHolder.getRoot()).addView(viewHolder.optionList, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      }
      return viewHolder;
   }

   @Override
   protected ViewHolder updateView(ViewHolder convertView) {
      final Operation operation = operationList.get(position);
      final TViewHolder viewHolder = (TViewHolder) convertView;
      viewHolder.typeView.setText(operation.getData().getType());
      viewHolder.nameView.setText(operation.getData().getName());
      List<ObjectGraph> targets = new ArrayList<ObjectGraph>();
      for (ObjectGraph target : operation.getPotentialTargets()) {
         ObjectGraph.merge(Arrays.asList(target.getRoot()), targets, null);
      }
      int i = 0;
      PickListEditable rootTargetPickList = viewHolder.targetLists.get(targets.get(0).getType());
      rootTargetPickList.clearList();
      for (ObjectGraph target : targets) {
         rootTargetPickList.addItem(new ExportFragment.ObjectPickListItem<ObjectGraph>(i++, target.getName(), target));
      }

      if (operation.getData() instanceof MultiSetObjectGraph) {
         List<String> options = ((MultiSetObjectGraph) operation.getData()).getOptions();
         viewHolder.optionList.clearList();
         i = 0;
         for (String option : options) {
            viewHolder.optionList.addItem(new ExportFragment.ObjectPickListItem<String>(i++, option, option));
         }
      }
      return convertView;
   }

   @Override
   protected boolean shouldShowView() {
      return  operationList.get(position).getPotentialTargets() != null && operationList.get(position).getData().getParent() == null;
   }

   private OnActionSelectedListener actionListener = new OnActionSelectedListener() {
      @Override
      public void onButtonSelected(final DialogView dialog, Action action) {
         if (Action.ACTION1.equals(action)) {
            position++;
            checkAndUpdateActive();
         }
      }
   };

   @Override
   public OnActionSelectedListener getActionListener() {
      return actionListener;
   }

}
