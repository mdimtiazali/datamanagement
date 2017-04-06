/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.cnh.pf.data.management.DataManagementSession.SessionOperation;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.datamng.Process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import roboguice.event.EventThread;
import roboguice.event.Observes;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;

/**
 * Import Tab Fragment, Handles import from external mediums {USB, External Display}.
 * @author oscar.salazar@cnhind.com
 */
public class ImportFragment extends BaseDataFragment {
   private static final Logger logger = LoggerFactory.getLogger(ImportFragment.class);

   @InjectView(R.id.import_source_btn)
   Button importSourceBtn;
   @InjectView(R.id.import_drop_zone)
   LinearLayout importDropZone;
   @InjectView(R.id.import_selected_btn)
   Button importSelectedBtn;
   @InjectView(R.id.stop_button)
   ImageButton stopBtn;
   @InjectView(R.id.progress_bar)
   ProgressBarView progressBar;
   @InjectView(R.id.operation_name)
   TextView operationName;
   @InjectView(R.id.percent_tv)
   TextView percentTv;
   @InjectView(R.id.left_status)
   LinearLayout leftStatus;
   ProcessDialog processDialog;
   //store original data in case cancel is pressed.  so we can restore it.
   private List<ObjectGraph> originalData;

   @InjectResource(R.string.keep_both)
   String keepBothStr;
   @InjectResource(R.string.copy_and_replace)
   String replaceStr;
   @InjectResource(R.string.data_conflict)
   String dataConflictStr;
   @InjectResource(R.string.select_target)
   String selectTargetStr;
   @InjectResource(R.string.next)
   String nextStr;


   @Override
   public void inflateViews(LayoutInflater inflater, View leftPanel) {
      inflater.inflate(R.layout.import_left_layout, (LinearLayout) leftPanel);
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      importSelectedBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            importSelected();
         }
      });
      importSourceBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            selectSource();
         }
      });
      stopBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            getDataManagementService().cancel(session);
         }
      });
      importDropZone.setOnDragListener(new View.OnDragListener() {
         @Override
         public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
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
      processDialog = new ProcessDialog(getActivity());
      checkImportButton();
   }

   @Override
   public void enableButtons(boolean enable) {
      super.enableButtons(enable);
      checkImportButton();
   }

   @Override
   protected void onOtherSessionUpdate(DataManagementSession session) {
      logger.debug("Other session has been updated");
      if (session.getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS)) {
         logger.trace("Other operation completed. Enabling UI.");
         checkImportButton();
      }
   }

   private void checkImportButton() {
      DataManagementSession s = getSession();
      boolean isActiveOperation = s != null
            && (s.getSessionOperation().equals(SessionOperation.CALCULATE_CONFLICTS) || s.getSessionOperation().equals(SessionOperation.CALCULATE_OPERATIONS)
                  || (s.getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS) && s.getResult() == null));
      boolean connected = getDataManagementService() != null;
      isActiveOperation |= connected && getDataManagementService().hasActiveSession();
      boolean hasSelection = getTreeAdapter() != null && getTreeAdapter().hasSelection();
      importSourceBtn.setEnabled(connected && !isActiveOperation);
      importSelectedBtn.setEnabled(connected && hasSelection && !isActiveOperation && s != null);
   }

   /**Called when user selects Import source, from Import Source Dialog*/
   private void onImportSourceSelected(@Observes(EventThread.UI) ImportSourceDialog.ImportSourceSelectedEvent event) {
      logger.debug("onImportSourceSelected( {} )", event.getDevice());
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
      DataManagementSession session = new DataManagementSession(null, new Datasource.Source[] { Datasource.Source.INTERNAL, Datasource.Source.DISPLAY }, event.getDevices(), null);
      setSession(session);
      getDataManagementService().processOperation(getSession(), SessionOperation.DISCOVERY);
      if (getTreeAdapter() != null) getTreeAdapter().selectAll(treeViewList, false); //clear out the selection
   }

   @Override
   public void onNewSession() {
      removeProgressPanel();
      setSession(null);
   }

   @Override
   public void processOperations() {
      if (getSession().getSessionOperation().equals(SessionOperation.CALCULATE_OPERATIONS) && !isCancelled()) {
         logger.debug("Calculate Targets");
         if (getSession().getData() == null) {
            Toast.makeText(getActivity(), "No operations came back from server.  Check connectivity", Toast.LENGTH_SHORT).show();
            cancel();
            return;
         }
         boolean hasMultipleTargets = false;
         for (Operation operation : getSession().getData()) {
            if (operation.getPotentialTargets() != null && operation.getPotentialTargets().size() > 1 && operation.getData().getParent() == null) {
               hasMultipleTargets = true;
               break;
            }
         }
         if (hasMultipleTargets) {
            TargetProcessViewAdapter adapter = new TargetProcessViewAdapter(getActivity(), getSession().getData());
            processDialog.setAdapter(adapter);
            processDialog.setTitle(selectTargetStr);
            processDialog.setFirstButtonText(nextStr);
            processDialog.showFirstButton(true);
            processDialog.showSecondButton(false);
            processDialog.clearLoading();
            adapter.setOnTargetsSelectedListener(new TargetProcessViewAdapter.OnTargetsSelectedListener() {
               @Override
               public void onCompletion(List<Operation> operations) {
                  logger.debug("onCompletion: {}", operations.toString());
                  getSession().setData(operations);
                  logger.debug("Going to Calculate Conflicts");
                  showConflictDialog();
                  setSession(getDataManagementService().processOperation(getSession(), SessionOperation.CALCULATE_CONFLICTS));
               }
            });
         }
         else {
            logger.debug("Found no targets with no parents, skipping to Calculate Conflicts");
            showConflictDialog();
            setSession(getDataManagementService().processOperation(getSession(), SessionOperation.CALCULATE_CONFLICTS));
         }
      }
      else if (getSession().getSessionOperation().equals(SessionOperation.CALCULATE_CONFLICTS) && !isCancelled()) {
         logger.debug("Calculate Conflicts");
         //Check for conflicts
         boolean hasConflicts = false;
         for (Operation operation : getSession().getData()) {
            hasConflicts |= operation.isConflict();
         }
         if (hasConflicts) {
            DataConflictViewAdapter adapter = new DataConflictViewAdapter(getActivity(), getSession().getData());
            processDialog.setAdapter(adapter);
            processDialog.setTitle(dataConflictStr);
            processDialog.showFirstButton(true);
            processDialog.setFirstButtonText(keepBothStr);
            processDialog.showSecondButton(true);
            processDialog.setSecondButtonText(replaceStr);
            processDialog.clearLoading();
            adapter.setOnTargetsSelectedListener(new DataManagementBaseAdapter.OnTargetsSelectedListener() {
               @Override
               public void onCompletion(List<Operation> operations) {
                  logger.debug("onCompletion {}", operations);
                  processDialog.hide();
                  showProgressPanel();
                  getSession().setData(operations);
                  setSession(getDataManagementService().processOperation(getSession(), SessionOperation.PERFORM_OPERATIONS));
               }
            });
         }
         else {
            logger.debug("No conflicts found");
            processDialog.hide();
            showProgressPanel();
            setSession(getDataManagementService().processOperation(getSession(), SessionOperation.PERFORM_OPERATIONS));
         }
      }
      else if (getSession().getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS)) {
         logger.trace("resetting new session.  Operation completed.");
         getTreeAdapter().selectAll(treeViewList, false);
         removeProgressPanel();
         if (getSession().getResult().equals(Process.Result.SUCCESS)) {
            Toast.makeText(getActivity(), "Import Completed", Toast.LENGTH_LONG).show();
         }
         else if (getSession().getResult().equals(Process.Result.CANCEL)) {
            Toast.makeText(getActivity(), "Import Cancelled", Toast.LENGTH_LONG).show();
         }
      }

   }

   @Override
   public void setSession(DataManagementSession session) {
      super.setSession(session);
      checkImportButton();
   }

   /** Shows conflict resolution dialog */
   private void showConflictDialog() {
      processDialog.init();
      processDialog.setTitle(getResources().getString(R.string.checking_conflicts));
      processDialog.setProgress(0);
      processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogViewInterface dialog) {
            cancel();
         }
      });
   }

   void cancel() {
      processDialog.hide();
      logger.debug("Cancelling operation");
      setCancelled(true);
      getSession().setObjectData(originalData);
      getSession().setSessionOperation(SessionOperation.DISCOVERY);
      if (getTreeAdapter() != null) getTreeAdapter().selectAll(treeViewList, false); //clear out the selection
      checkImportButton();
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
      return session.equals(getSession());
   }

   @Override
   public void onTreeItemSelected() {
      super.onTreeItemSelected();
      checkImportButton();
   }

   @Override
   public void onProgressPublished(String operation, int progress, int max) {
      final Double percent = ((progress * 1.0) / max) * 100;
      if (processDialog.isShown()) {
         processDialog.setProgress(percent.intValue());
      }
      else {
         progressBar.setProgress(percent.intValue());
         percentTv.setText(Integer.toString(percent.intValue()));
      }
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      //For import, all formats supported
      return true;
   }

   void importSelected() {
      logger.debug("Import selected");
      Set<ObjectGraph> selected = getTreeAdapter().getSelected();
      if (selected.size() > 0 && getSession() != null) {
         processDialog.init();
         processDialog.setTitle(getResources().getString(R.string.checking_targets));
         processDialog.setProgress(0);
         processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogViewInterface dialog) {
               cancel();
            }
         });
         processDialog.show();
         setCancelled(false);
         originalData = getSession().getObjectData();
         getSession().setObjectData(new ArrayList<ObjectGraph>(selected));
         setSession(getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.CALCULATE_OPERATIONS));
      }
   }

   void selectSource() {
      ((TabActivity) getActivity()).showPopup(new ImportSourceDialog(getActivity(), getDataManagementService().getMediums()), true);
   }

   @Override
   public void onMediumsUpdated(List<MediumDevice> mediums) throws RemoteException {
      logger.info("onMediumsUpdated {}", mediums);
   }
}
