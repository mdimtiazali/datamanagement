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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.Toast;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressBarView;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.DataConflictViewAdapter;
import com.cnh.pf.android.data.management.adapter.DataManagementBaseAdapter;
import com.cnh.pf.android.data.management.adapter.TargetProcessViewAdapter;
import com.cnh.pf.android.data.management.dialog.ImportSourceDialog;
import com.cnh.pf.android.data.management.dialog.ProcessDialog;
import com.cnh.pf.data.management.DataManagementSession;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.event.EventThread;
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
   @Bind(R.id.pause_button) ImageButton pauseBtn;
   @Bind(R.id.stop_button) ImageButton stopBtn;
   @Bind(R.id.progress_bar) ProgressBarView progressBar;
   @Bind(R.id.operation_name) TextView operationName;
   @Bind(R.id.percent_tv) TextView percentTv;
   @Bind(R.id.left_status) LinearLayout leftStatus;
   ProcessDialog processDialog;

   @Override public void inflateViews(LayoutInflater inflater, View leftPanel) {
      inflater.inflate(R.layout.import_left_layout, (LinearLayout) leftPanel);
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View rootView = super.onCreateView(inflater, container, savedInstanceState);
      ButterKnife.bind(this, rootView);
      pauseBtn.setOnClickListener(new View.OnClickListener() {
         @Override public void onClick(View v) {
            getDataManagementService().cancel();
         }
      });
      stopBtn.setOnClickListener(new View.OnClickListener() {
         @Override public void onClick(View v) {
            getDataManagementService().cancel();
         }
      });
      importDropZone.setOnDragListener(new View.OnDragListener() {
         @Override public boolean onDrag(View v, DragEvent event) {
            switch(event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
               importDropZone.setBackgroundColor(getResources().getColor(R.color.drag_accept));
               return true;
            case DragEvent.ACTION_DRAG_ENDED:
               importDropZone.setBackgroundColor(getResources().getColor(android.R.color.transparent));
               return true;
            case DragEvent.ACTION_DRAG_ENTERED:
               importDropZone.setBackgroundColor(getResources().getColor(R.color.drag_enter));
               return true;
            case DragEvent.ACTION_DRAG_EXITED:
               importDropZone.setBackgroundColor(getResources().getColor(R.color.drag_accept));
               return true;
            case DragEvent.ACTION_DROP:
               logger.info("Dropped");
               importSelected();
               return true;
            }
            return false;
         }
      });
      return rootView;
   }

   @Override
   public void onDestroyView() {
      super.onDestroyView();
      ButterKnife.unbind(this);
   }

   @Override
   public void enableButtons(boolean enable) {
      super.enableButtons(enable);
      checkImportButton();
   }

   private void checkImportButton() {
      importSelectedBtn.setEnabled(getTreeAdapter() != null && getTreeAdapter().getSelected().size() > 0);
   }

   /**Called when user selects Import source, from Import Source Dialog*/
   private void onImportSourceSelected(@Observes(EventThread.UI) ImportSourceDialog.ImportSourceSelectedEvent event) {
      removeProgressPanel();
      startText.setVisibility(View.GONE);
      treeProgress.setVisibility(View.VISIBLE);
      treeViewList.setVisibility(View.GONE);

      if (event.getDevice().getType().equals(Datasource.Source.USB)) {
         pathTv.setText(event.getDevice().getPath() == null ? "" : event.getDevice().getPath().getPath());
      }
      else {
         pathTv.setText(getString(R.string.display_named, event.getDevice().getName()));
      }
      setSession(new DataManagementSession(new Datasource.Source[] { event.getDevice().getType() },
         event.getDevices(),
         new Datasource.Source[] { Datasource.Source.INTERNAL, Datasource.Source.DISPLAY }));
      getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.DISCOVERY);
      if(getTreeAdapter()!=null) getTreeAdapter().selectAll(treeViewList, false);  //clear out the selection
   }

   @Override
   public void onNewSession() {
      removeProgressPanel();
   }

   @Override public void processOperations() {
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
            processDialog.clearLoading();
            processDialog.setTitle(getResources().getString(R.string.select_target));
            adapter.setOnTargetsSelectedListener(new TargetProcessViewAdapter.OnTargetsSelectedListener() {
               @Override public void onCompletion(List<Operation> operations) {
                  logger.debug("onCompletion: {}", operations.toString());
                  processDialog.hide();
                  getSession().setData(operations);
                  logger.debug("Going to Calculate Conflicts");
                  showConflictDialog();
                  getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.CALCULATE_CONFLICTS);
               }
            });
            processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override public void onButtonClick(DialogViewInterface dialog, int which) {
                  if (which == DialogViewInterface.BUTTON_THIRD) {
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
            logger.debug("Found no targets with no parents, skipping to Calculate Conflicts");
            showConflictDialog();
            getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.CALCULATE_CONFLICTS);
         }
      }
      else if (getSession().getSessionOperation().equals(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS)) {
         logger.debug("Calculate Conflicts");
         //Check for conflicts
         boolean hasConflicts = false;
         for (Operation operation : getSession().getData()) {
            hasConflicts |= operation.isConflict();
         }
         if (hasConflicts) {
            DataConflictViewAdapter adapter = new DataConflictViewAdapter(getActivity(), getSession().getData());
            processDialog.setAdapter(adapter);
            processDialog.showFirstButton(true);
            processDialog.showSecondButton(true);
            processDialog.clearLoading();
            adapter.setOnTargetsSelectedListener(new DataManagementBaseAdapter.OnTargetsSelectedListener() {
               @Override public void onCompletion(List<Operation> operations) {
                  logger.debug("onCompletion {}", operations);
                  processDialog.hide();
                  showProgressPanel();
                  getSession().setData(operations);
                  getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.PERFORM_OPERATIONS);
               }
            });
            processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
               @Override public void onDismiss(DialogViewInterface dialog) {
                  processDialog.hide();
               }
            });
         }
         else {
            logger.debug("No conflicts found");
            processDialog.hide();
            showProgressPanel();
            getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.PERFORM_OPERATIONS);
         }
      }
   }

   /** Shows conflict resolution dialog */
   private void showConflictDialog() {
      processDialog = new ProcessDialog(getActivity());
      processDialog.setTitle(getResources().getString(R.string.checking_conflicts));
      processDialog.show();
   }

   /** Inflates left panel progress view */
   private void showProgressPanel() {
      leftStatus.setVisibility(View.VISIBLE);
      importDropZone.setVisibility(View.GONE);
      operationName.setText(getResources().getString(R.string.importing_data));
      progressBar.setTitle(getResources().getString(R.string.importing_string));
      progressBar.setProgress(0);
      percentTv.setText("0");
   }

   /** Removes left panel progress view and replaces with operation view */
   private void removeProgressPanel() {
      leftStatus.setVisibility(View.GONE);
      importDropZone.setVisibility(View.VISIBLE);
   }


   /** Check if session returned by service is an import operation*/
   @Override
   public boolean isCurrentOperation(DataManagementSession session) {
      return Arrays.binarySearch(session.getDestinationTypes(), Datasource.Source.INTERNAL) > -1;
   }

   @Override
   public void onTreeItemSelected() {
      checkImportButton();
   }

   @Override
   public void onProgressPublished(String operation, int progress, int max) {
      logger.debug("onProgressPublished: {}", progress);
      if(progress == max) {
         logger.info("Process completed.  {}/{} objects", progress, max);
         getTreeAdapter().selectAll(treeViewList, false);
         removeProgressPanel();
         Toast.makeText(getActivity(), "Import Completed", Toast.LENGTH_LONG).show();
      }
      final Double percent = ((progress * 1.0) / max) * 100;

      if (progressBar.getVisibility() == View.VISIBLE) {
         progressBar.setProgress(percent.intValue());
         percentTv.setText(Integer.toString(percent.intValue()));
      }
      else {
         processDialog.setProgress(percent.intValue());
      }
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      //For import, all formats supported
      return true;
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
         getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.CALCULATE_OPERATIONS);
      }
      else {
         //notify user he needs to select items first
      }
   }

   @OnClick(R.id.import_source_btn)
   void selectSource() {
      DialogView importSourceDialog = new ImportSourceDialog(getActivity(), getDataManagementService().getMediums());
      ((TabActivity) getActivity()).showPopup(importSourceDialog, true);
   }
}
