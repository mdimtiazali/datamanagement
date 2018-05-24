/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import static android.os.Environment.MEDIA_BAD_REMOVAL;
import static com.cnh.pf.android.data.management.utility.UtilityHelper.MAX_TREE_SELECTIONS_FOR_DEFAULT_TEXT_SIZE;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.pf.widget.controls.ToastMessageCustom;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressBarView;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.DataConflictViewAdapter;
import com.cnh.pf.android.data.management.adapter.DataManagementBaseAdapter;
import com.cnh.pf.android.data.management.adapter.TargetProcessViewAdapter;
import com.cnh.pf.android.data.management.dialog.ImportSourceDialog;
import com.cnh.pf.android.data.management.dialog.ProcessDialog;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.cnh.pf.android.data.management.session.SessionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
   @InjectView(R.id.import_drop_zone_image)
   ImageView importDropZoneImage;
   @InjectView(R.id.import_drop_zone_text)
   TextView importDropZoneText;
   @InjectView(R.id.import_selected_btn)
   Button importSelectedBtn;
   @InjectView(R.id.stop_button)
   ImageButton stopBtn;
   @InjectView(R.id.progress_bar)
   ProgressBarView progressBar;
   @InjectView(R.id.left_status_panel)
   LinearLayout leftStatus;
   @InjectView(R.id.start_text)
   TextView startText;

   ProcessDialog processDialog;

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
   @InjectView(R.id.operation_name)
   TextView operationName;

   private String importing_data;
   private String loading_string;
   private String x_of_y_format;
   private int whiteTextColor;
   private int defaultTextColor;

   private Drawable importWhiteIcon;
   private Drawable importDefaultIcon;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      final Resources resources = getResources();
      importing_data = resources.getString(R.string.importing_data);
      loading_string = resources.getString(R.string.loading_string);
      x_of_y_format = resources.getString(R.string.x_of_y_format);

      whiteTextColor = resources.getColor(R.color.drag_drop_white_text_color);
      defaultTextColor = resources.getColor(R.color.drag_drop_default_text_color);

      importWhiteIcon = resources.getDrawable(R.drawable.ic_import_white);
      importDefaultIcon = resources.getDrawable(R.drawable.ic_import_grey);
   }

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
            runImport();
         }
      });
      importSourceBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            onSelectSource();
         }
      });
      stopBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            cancel();
         }
      });
      importDropZone.setOnDragListener(new View.OnDragListener() {
         @Override
         public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
               return true;
            case DragEvent.ACTION_DRAG_ENDED:
               return true;
            case DragEvent.ACTION_DRAG_ENTERED:
               setImportDragDropArea(DragEvent.ACTION_DRAG_ENTERED);
               return true;
            case DragEvent.ACTION_DRAG_EXITED:
               setImportDragDropArea(DragEvent.ACTION_DRAG_EXITED);
               return true;
            case DragEvent.ACTION_DROP:
               logger.info("Dropped");
               setImportDragDropArea(DragEvent.ACTION_DROP);
               runImport();
               return true;
            }
            return false;
         }
      });
      processDialog = new ProcessDialog(getActivity());
      startText.setVisibility(View.GONE);
      operationName.setText(R.string.importing_string);
      updateImportButton();
   }

   @Override
   public void onSessionCancelled(Session session) {
      super.onSessionCancelled(session);

      ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_cancel),
              Gravity.TOP| Gravity.CENTER_HORIZONTAL, getResources().getInteger(R.integer.toast_message_xoffset),
              getResources().getInteger(R.integer.toast_message_yoffset)).show();

      if (SessionUtil.isDiscoveryTask(session)) {
         removeProgressPanel();
         showStartMessage();
         updateImportButton();
      }
      else if (SessionUtil.isPerformOperationsTask(session)) {
         hideDisabledOverlay();
         showTreeList();
         updateSelectAllState();
      }
   }

   private void setImportDragDropArea(int event) {
      if (event == DragEvent.ACTION_DRAG_ENTERED) {
         importDropZoneImage.setImageDrawable(importWhiteIcon);
         importDropZoneText.setTextColor(whiteTextColor);
         importDropZone.setBackgroundResource(R.drawable.dashed_border_accept);
      }
      else {
         importDropZoneImage.setImageDrawable(importDefaultIcon);
         importDropZoneText.setTextColor(defaultTextColor);
         importDropZone.setBackgroundResource(event == DragEvent.ACTION_DRAG_EXITED ? R.drawable.dashed_border_selected : R.drawable.dashed_border_initial);
      }
   }

   @Override
   public void onOtherSessionSuccess(Session session) {
      logger.debug("onOtherSessionSuccess(): {}", session.getType());
   }

   @Override
   public void onMyselfSessionSuccess(Session session) {
      logger.debug("onMyselfSessionSuccess()-Type:{}", session.getType());

      if (SessionUtil.isDiscoveryTask(session)) {
         initAndPouplateTree(session.getObjectData());

         hideDisabledOverlay();
         showTreeList();
         updateSelectAllState();

         if (session.getExtra() != null && session.getExtra().isUsbExtra()) {
            setHeaderText(session.getExtra().getPath());
         }
      }
      else if (SessionUtil.isCalculateOperationsTask(session)) {
         logger.debug("Calculate Targets");
         if (session.getOperations() == null || session.getOperations().isEmpty()) {
            ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.no_operations_check_connectivity_string),
                    Gravity.TOP| Gravity.CENTER_HORIZONTAL, getResources().getInteger(R.integer.toast_message_xoffset),
                    getResources().getInteger(R.integer.toast_message_yoffset)).show();
            cancelProcess();
            return;
         }

         boolean hasMultipleTargets = false;
         for (Operation operation : session.getOperations()) {
            if (operation.isShowTargetSelection()) {
               hasMultipleTargets = true;
               break;
            }
            if (operation.getPotentialTargets() != null && operation.getPotentialTargets().size() > 1 && operation.getData().getParent() == null) {
               hasMultipleTargets = true;
               break;
            }
         }
         if (hasMultipleTargets) {
            TargetProcessViewAdapter adapter = new TargetProcessViewAdapter(getActivity(), session.getOperations());
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
                  showConflictDialog();

                  logger.debug("Going to Calculate Conflicts");
                  calculateConflicts(operations);
               }
            });
         } else {
            logger.debug("Found no targets with no parents, skipping to Calculate Conflicts");
            showConflictDialog();

            List<Operation> operations = session.getOperations();
            calculateConflicts(operations);
         }
      }
      else if (SessionUtil.isCalculateConflictsTask(session)) {
         logger.debug("Calculate Conflicts");
         final SessionExtra extra = new SessionExtra(session.getExtra());

         //Check for conflicts
         boolean hasConflicts = false;
         for (Operation operation : session.getOperations()) {
            hasConflicts |= operation.isConflict();
         }
         if (hasConflicts) {
            DataConflictViewAdapter adapter = new DataConflictViewAdapter(getActivity(), session.getOperations());
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

                  performOperations(extra, operations);
               }
            });
         } else {
            logger.debug("No conflicts found");

            processDialog.hide();
            showProgressPanel();
            performOperations(extra, session.getOperations());
         }
      }
      else if (SessionUtil.isPerformOperationsTask(session)) {
         logger.trace("Import process has been completed. Reset session.");

         clearTreeSelection();
         removeProgressPanel();
         ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_complete),
                 Gravity.TOP| Gravity.CENTER_HORIZONTAL, getResources().getInteger(R.integer.toast_message_xoffset),
                 getResources().getInteger(R.integer.toast_message_yoffset)).show();
         // Reset session data after completing PERFORM_OPERATIONS successfully.
         resetSession();
      }
   }

   @Override
   public void onOtherSessionError(Session session, ErrorCode errorCode) {
      logger.debug("onOtherSessionError(): {}, {}", session.getType(), errorCode);
   }

   @Override
   public void onMyselfSessionError(Session session, ErrorCode errorCode) {
      logger.debug("onMyselfSessionError(): {}, {}", session.getType(), errorCode);
      if (SessionUtil.isDiscoveryTask(session)) {
         removeProgressPanel();
         hideDisabledOverlay();
         showStartMessage();
      }
      else {
         if(SessionUtil.isDiscoveryTask(session)){
            hideTreeList();
            hideDisabledOverlay();
         }
         else {
            removeProgressPanel();
            hideDisabledOverlay();
            showTreeList();
         }
      }
   }

   /**
    * Update enable status & test on the import button.
    */
   private void updateImportButton() {
      Session s = getSession();
      boolean isActiveOperation = (SessionUtil.isCalculateConflictsTask(s) ||
              SessionUtil.isCalculateOperationsTask(s) ||
              SessionUtil.isPerformOperationsTask(s) && s.getResultCode() == null);
      boolean connected = getSessionManager().isServiceConnected();
      boolean hasSelection = getTreeAdapter() != null && getTreeAdapter().hasSelection();
      boolean defaultButtonText = true;
      if (getTreeAdapter() != null && getTreeAdapter().getSelectionMap() != null) {
         int selectedItemCount = getTreeAdapter().getSelectionMap().size();
         if (selectedItemCount > 0) {
            defaultButtonText = false;
            Resources resources = getResources();
            importSelectedBtn.setText(resources.getString(R.string.import_selected) + " (" + selectedItemCount + ")");
            importSelectedBtn.setTextSize(selectedItemCount > MAX_TREE_SELECTIONS_FOR_DEFAULT_TEXT_SIZE
                    ? resources.getDimension(R.dimen.button_default_text_size) - resources.getDimension(R.dimen.decrease_text_size)
                    : resources.getDimension(R.dimen.button_default_text_size));
         }
      }
      if (defaultButtonText == true) {
         importSelectedBtn.setText(getResources().getString(R.string.import_selected));
         float defaultButtonSize = getResources().getDimension(R.dimen.button_default_text_size);
         if (importSelectedBtn.getTextSize() < defaultButtonSize) importSelectedBtn.setTextSize(defaultButtonSize);
      }

      importSourceBtn.setEnabled(connected && !isActiveOperation);
      if (connected && hasSelection && !isActiveOperation && s != null) {
         importSelectedBtn.setEnabled(true);
         importDropZone.setBackgroundResource(R.drawable.dashed_border_selected);
      }
      else {
         importSelectedBtn.setEnabled(false);
         importDropZone.setBackgroundResource(R.drawable.dashed_border_initial);
      }
   }

   /**Called when user selects Import source, from Import Source Dialog*/
   private void onImportSourceSelected(@Observes(EventThread.UI) ImportSourceDialog.ImportSourceSelectedEvent event) {
      logger.debug("onImportSourceSelected( {} )", event.getExtra());
      SessionExtra extra = new SessionExtra(event.getExtra());

      hideTreeList();
      hideStartMessage();
      showLoadingOverlay();

      discovery(extra);
   }

   @Override
   public void onPCMDisconnected() {
      logger.trace("PCM is not online.");
      hideTreeList();
      showDisconnectedOverlay();
      updateSelectAllState();
   }

   @Override
   public void onResumeSession() {
      logger.debug("onResumeSession()");
      final Session session = getSession();

      if (SessionUtil.isDiscoveryTask(session)
              && session.getObjectData() != null
              && !session.getObjectData().isEmpty()) {
         logger.trace("There is already active session. Continue on the previous active session.");
         initAndPouplateTree(session.getObjectData());

         showTreeList();
         hideDisabledOverlay();
         updateSelectAllState();

         if (session.getExtra() != null && session.getExtra().isUsbExtra()) {
            setHeaderText(session.getExtra().getPath());
         }
      }
      else {
         hideTreeList();
         hideDisabledOverlay();
         showStartMessage();
      }
      updateImportButton();
   }

   /** Shows conflict resolution dialog */
   private void showConflictDialog() {
      processDialog.hide();
      processDialog.init();
      processDialog.setTitle(getResources().getString(R.string.checking_conflicts));
      processDialog.setProgress(0);
      processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogViewInterface dialog) {
            cancelProcess();
         }
      });
      processDialog.show();
   }

   /**
    * Cancel ongoing IMPORT process.
    */
   private void cancelProcess() {
      processDialog.hide();
      logger.debug("Cancel current import process.");
      getSession().setType(Session.Type.DISCOVERY);

      clearTreeSelection();
      updateImportButton();
   }

   /** Inflates left panel progress view */
   private void showProgressPanel() {
      importDropZone.setVisibility(View.GONE);
      progressBar.setSecondText(true, loading_string, null, true);
      progressBar.setProgress(0);
      leftStatus.setVisibility(View.VISIBLE);
   }

   /** Removes left panel progress view and replaces with operation view */
   private void removeProgressPanel() {
      leftStatus.setVisibility(View.GONE);
      importDropZone.setVisibility(View.VISIBLE);
   }

   @Override
   public void onTreeItemSelected() {
      super.onTreeItemSelected();
      updateImportButton();
   }

   @Override
   public void onProgressUpdate(String operation, int progress, int max) {
      final Double percent = ((progress * 1.0) / max) * 100;
      progressBar.setProgress(percent.intValue());
      progressBar.setSecondText(true, loading_string, String.format(x_of_y_format, progress, max), true);
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      // TEMPORARY: Do not import the four data types
      if (node.getType().equals("IMPLEMENT") || node.getType().equals("VEHICLE_IMPLEMENT") ||
              node.getType().equals("VEHICLE_IMPLEMENT_CONFIG") || node.getType().equals("IMPLEMENT_PRODUCT_CONFIG")) {
         return false;
      }
      //For import, all formats supported
      return true;
   }

   /**
    * Run IMPORT.
    */
   private void runImport() {
      logger.debug("Run Import!");
      List<ObjectGraph> selected = new ArrayList<ObjectGraph>(getTreeAdapter().getSelected());
      if (!selected.isEmpty()) {
         processDialog.init();
         processDialog.setTitle(getResources().getString(R.string.checking_targets));
         processDialog.setProgress(0);
         processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogViewInterface dialog) {
               cancelProcess();
            }
         });
         processDialog.show();

         calculateOperations(selected);
      }
   }

   private List<SessionExtra> generateImportExtras() {
      List<SessionExtra> list = new ArrayList<SessionExtra>();

      if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
         SessionExtra newExtra = new SessionExtra(SessionExtra.USB, "USB", 0);
         newExtra.setPath(Environment.getExternalStorageDirectory().getPath());
         list.add(newExtra);
      }
      return list;
   }

   private void onSelectSource() {
      ((TabActivity) getActivity()).showPopup(new ImportSourceDialog(getActivity(), generateImportExtras()), true);
   }

   /**
    * Display starting message on the screen.
    */
   protected void showStartMessage() {
      startText.setVisibility(View.VISIBLE);
   }

   /**
    * Hide starting message.
    */
   protected void hideStartMessage() {
      startText.setVisibility(View.GONE);
   }

   @Override
   public void onMediumUpdate() {
      super.onMediumUpdate();

      logger.trace("onMediumUpdate()");
      if (Environment.getExternalStorageState().equals(MEDIA_BAD_REMOVAL)) {
         logger.debug("onMediumUpdate() - Reset the import screen and session state.");

         processDialog.hide();
         removeProgressPanel();
         setHeaderText("");
         showStartMessage();
         clearTreeSelection();
         resetSession();
         updateSelectAllState();
         updateImportButton();
      }
   }

   @Override
   public Session.Action getAction() {
      return Session.Action.IMPORT;
   }
}
