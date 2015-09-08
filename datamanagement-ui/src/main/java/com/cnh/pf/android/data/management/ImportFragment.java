/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.TargetProcessViewAdapter;
import com.cnh.pf.android.data.management.dialog.ImportSourceDialog;
import com.cnh.pf.android.data.management.dialog.ProcessDialog;
import com.cnh.pf.data.management.DataManagementSession;

import butterknife.Bind;
import butterknife.OnClick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.event.Observes;

/**
 * Import Tab Fragment, Handles import from external mediums {USB, External Display}.
 * @author oscar.salazar@cnhind.com
 */
public class ImportFragment extends BaseDataFragment {
   private static final Logger logger = LoggerFactory.getLogger(ImportFragment.class);

   @Bind(R.id.import_source_btn) Button importSourceBtn;
   @Bind(R.id.import_drop_zone) LinearLayout importDropZone;
   @Bind(R.id.import_selected_btn) Button importSelectedBtn;
   ProcessDialog processDialog;

   @Override public void inflateViews(LayoutInflater inflater, View leftPanel) {
      inflater.inflate(R.layout.import_left_layout, (LinearLayout) leftPanel);
   }

   @Override
   public void enableButtons(boolean enable) {
      super.enableButtons(enable);
      importSelectedBtn.setEnabled(enable);
   }

   /**Called when user selects Import source, from Import Source Dialog*/
   private void onImportSourceSelected(@Observes ImportSourceDialog.ImportSourceSelectedEvent event) {
      setSession(new DataManagementSession(event.getSourceType(), Datasource.Source.INTERNAL));
      getDataServiceConnection().processOperation(getSession(), DataManagementSession.SessionOperation.DISCOVERY);
   }

   @Override
   public void onNewSession() { }

   @Override
   public void processOperations() {
      if (getSession().getSessionOperation().equals(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS)) {
         logger.debug("Calculate Targets");
         boolean hasMultipleTargets = false;
         for (Operation operation : getSession().getData()) {
            if (operation.getPotentialTargets() != null && operation.getPotentialTargets().size() > 1) {
               hasMultipleTargets = true;
               break;
            }
         }
         if (hasMultipleTargets) {
            TargetProcessViewAdapter adapter = new TargetProcessViewAdapter(getActivity(), getSession().getData());
            processDialog.setAdapter(adapter);
            processDialog.setTitle(getResources().getString(R.string.select_target));
            processDialog.clearLoading();
            adapter.setOnTargetsSelectedListener(new TargetProcessViewAdapter.OnTargetsSelectedListener() {
               @Override public void onCompletion(List<Operation> operations) {
                  logger.debug("onCompletion: {}", operations.toString());
                  processDialog.hide();
                  getSession().setData(operations);
               }
            });
            processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override public void onButtonClick(DialogViewInterface dialog, int which) {
                  if (which == DialogViewInterface.BUTTON_FIRST) {
                     // user pressed "Cancel" button
                     dialog.dismiss();
                  }
               }
            });
            processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
               @Override public void onDismiss(DialogViewInterface dialog) {
                  processDialog.hide();
               }
            });
         }
         else {
            //TODO jump to calculate Conflicts
//            getDataServiceConnection().processOperation();
         }
      }
   }

   @OnClick(R.id.import_selected_btn)
   void importSelected() {
      logger.debug("Import selected");
      Set<ObjectGraph> selected = getTreeAdapter().getSelected();
      if (selected.size() > 0) {
         processDialog = new ProcessDialog(getActivity());
         processDialog.show();
         processDialog.setTitle(getResources().getString(R.string.checking_targets));
         getSession().setObjectData(new ArrayList<ObjectGraph>(getTreeAdapter().getSelected()));
         getDataServiceConnection().processOperation(getSession(), DataManagementSession.SessionOperation.CALCULATE_OPERATIONS);
      }
      else {
         //notify user he needs to select items first
      }
   }

   @OnClick(R.id.import_source_btn)
   void selectSource() {
      DialogView importSourceDialog = new ImportSourceDialog(getActivity());
      ((TabActivity) getActivity()).showPopup(importSourceDialog, true);
   }
}
