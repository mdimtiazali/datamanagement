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

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.annotations.Nullable;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.dialog.TextDialogView;
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
import com.cnh.pf.android.data.management.helper.DataExchangeProcessOverlay;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.android.data.management.utility.UtilityHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
   @InjectView(R.id.dataexchange_success_zone)
   LinearLayout importFinishedStatePanel;

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

   //this variable denotes the current state of dynamic visual feedback (success or failure of process)
   private boolean visualFeedbackActive = false; //true: visual feedback is currently shown, false: currently no visual feedback shown

   private final List<Session.Action> blockingActions = new ArrayList<Session.Action>(Arrays.asList(Session.Action.EXPORT));
   private final List<Session.Action> executableActions = new ArrayList<Session.Action>(Arrays.asList(Session.Action.IMPORT));
   private static final int ROOTNODENAMES_WITH_IMPLICIT_PARENT_SELECTION_IN_IMPORT = R.array.rootnodenames_with_implicit_parent_selection_in_import;

   private static final int CANCEL_DIALOG_WIDTH = 550;

   private TextDialogView lastCancelDialogView; //used to keep track of the cancel dialog (should be closed if open if process is finished)

   private String loadingString;
   private String xOfYFormat;
   private int whiteTextColor;
   private int defaultTextColor;

   private Drawable importWhiteIcon;
   private Drawable importDefaultIcon;

   @Override
   protected List<Session.Action> getBlockingActions() {
      return blockingActions;
   }

   @Override
   protected List<Session.Action> getExecutableActions() {
      return executableActions;
   }

   @Override
   protected int getRootNodeNamesWithImplicitSelectionResourceId() {
      return ROOTNODENAMES_WITH_IMPLICIT_PARENT_SELECTION_IN_IMPORT;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      final Resources resources = getResources();
      loadingString = resources.getString(R.string.loading_string);
      xOfYFormat = resources.getString(R.string.x_of_y_format);

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
      enableDragAndDropForTreeView();
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
            //Show confirmation cancel-dialog
            DialogViewInterface.OnButtonClickListener buttonListener = new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialog, int buttonId) {
                  if (buttonId == TextDialogView.BUTTON_FIRST) {
                     cancel();
                  }
               }
            };
            showCancelDialog(buttonListener);
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
            default:
               return false;
            }
         }
      });
      processDialog = new ProcessDialog(getActivity());
      startText.setVisibility(View.GONE);
      operationName.setText(R.string.importing_string);
      updateImportButton();
      updateSelectAllState();
      importFinishedStatePanel.setVisibility(View.GONE);
   }

   @Override
   public void onMyselfSessionCancelled(Session session) {
      ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_cancel), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
            getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();

      //close cancel dialog if still open
      closeCancelDialog();
      showDragAndDropZone();

      if (SessionUtil.isDiscoveryTask(session)) {
         showDragAndDropZone();
         showStartMessage();
      }
      else if (SessionUtil.isPerformOperationsTask(session)) {
         hideDisabledOverlay();
         showTreeList();
      }
      updateImportButton();
      updateSelectAllState();
      processOverlay.setMode(DataExchangeProcessOverlay.MODE.HIDDEN);
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
   public void onMyselfSessionSuccess(Session session) {
      logger.debug("onMyselfSessionSuccess()-Type:{}", session.getType());

      if (SessionUtil.isDiscoveryTask(session)) {
         initAndPopulateTree(session.getObjectData());

         hideDisabledOverlay();
         showTreeList();
         updateSelectAllState();

         if (session.getExtra() != null && session.getExtra().isUsbExtra()) {
            String filename = UtilityHelper.filenameOnly(session.getExtra().getPath());
            if (filename == null) filename = "";
            setHeaderText(filename);
         }
      }
      else if (SessionUtil.isCalculateOperationsTask(session)) {
         logger.debug("Calculate Targets");
         if (session.getOperations() == null || session.getOperations().isEmpty()) {
            ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.no_operations_check_connectivity_string),
                  Gravity.TOP | Gravity.CENTER_HORIZONTAL, getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset))
                  .show();
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
                  logger.debug("onCompletion: {}", operations);
                  showConflictDialog();

                  logger.debug("Going to Calculate Conflicts");
                  calculateConflicts(operations);
               }
            });
         }
         else {
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
            final android.widget.LinearLayout.LayoutParams dialogContentLayoutParams = new android.widget.LinearLayout.LayoutParams(processDialog.getContentLayoutParameter());
            Resources resources = getResources();
            final int conflictDialogWidth = resources.getDimensionPixelSize(R.dimen.import_conflict_width);
            final int conflictDialogHeight = resources.getDimensionPixelSize(R.dimen.import_conflict_height);
            processDialog.setDimensionsNoPadding(conflictDialogWidth, conflictDialogHeight);
            processDialog.setAdapter(adapter);
            processDialog.setTitle(dataConflictStr);
            processDialog.showFirstButton(true);
            processDialog.setFirstButtonText(keepBothStr);
            processDialog.showSecondButton(true);
            processDialog.setSecondButtonText(replaceStr);
            processDialog.clearLoading();

            //show add custom cancel handling
            final DataManagementBaseAdapter.OnActionSelectedListener listener = adapter.getActionListener();
            processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialog, int which) {
                  if (which == DialogViewInterface.BUTTON_FIRST) {
                     listener.onButtonSelected(processDialog, DataManagementBaseAdapter.Action.ACTION1);
                  }
                  else if (which == DialogViewInterface.BUTTON_SECOND) {
                     listener.onButtonSelected(processDialog, DataManagementBaseAdapter.Action.ACTION2);
                  }
                  else if (which == DialogViewInterface.BUTTON_THIRD) {
                     listener.onButtonSelected(processDialog, null);
                     //Show confirmation cancel-dialog
                     DialogViewInterface.OnButtonClickListener onButtonClickListener = new DialogViewInterface.OnButtonClickListener() {
                        @Override
                        public void onButtonClick(DialogViewInterface dialog, int buttonId) {
                           if (buttonId == TextDialogView.BUTTON_FIRST) {
                              hideAndDismissProcessDialog();
                              ToastMessageCustom
                                    .makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_cancel), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                                          getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset))
                                    .show();
                              cancelProcess();
                           }
                        }
                     };
                     showCancelDialog(onButtonClickListener);
                  }
               }
            });

            adapter.setOnTargetsSelectedListener(new DataManagementBaseAdapter.OnTargetsSelectedListener() {
               @Override
               public void onCompletion(List<Operation> operations) {
                  logger.debug("onCompletion {}", operations);
                  hideAndDismissProcessDialog();
                  showProgressPanel();
                  processOverlay.setMode(DataExchangeProcessOverlay.MODE.IMPORT_PROCESS);
                  updateImportButton();
                  updateSelectAllState();
                  performOperations(extra, operations);
               }
            });
            processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
               @Override
               public void onDismiss(DialogViewInterface dialogViewInterface) {
                  //reset layout dimensions
                  processDialog.setDimensionsNoPadding(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                  //reset margin
                  processDialog.setContentLayoutParameter(dialogContentLayoutParams);
                  //reset padding
                  Resources resources = getActivity().getResources();
                  int paddingTop = resources.getDimensionPixelSize(R.dimen.dialog_view_content_top_padding);
                  int paddingBottom = resources.getDimensionPixelSize(R.dimen.dialog_view_content_bottom_padding);
                  int paddingLeft = resources.getDimensionPixelSize(R.dimen.dialog_view_content_left_padding);
                  int paddingRight = resources.getDimensionPixelSize(R.dimen.dialog_view_content_right_padding);
                  processDialog.setContentPaddings(paddingLeft, paddingTop, paddingRight, paddingBottom);
               }
            });
         }
         else {
            logger.debug("No conflicts found");
            hideAndDismissProcessDialog();
            showProgressPanel();
            processOverlay.setMode(DataExchangeProcessOverlay.MODE.IMPORT_PROCESS);
            updateImportButton();
            updateSelectAllState();
            performOperations(extra, session.getOperations());
         }
      }
      else if (SessionUtil.isPerformOperationsTask(session)) {
         logger.trace("Import process has been completed. Reset session.");

         clearTreeSelection();
         showStartMessage();
         showFinishedStatePanel();
         processOverlay.setMode(DataExchangeProcessOverlay.MODE.HIDDEN);
         ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_complete), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
               getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
         // Reset session data after completing PERFORM_OPERATIONS successfully.
         resetSession();
         updateSelectAllState();

         //close cancel dialog if still open
         closeCancelDialog();
      }
   }

   /**
    * This method is necessary since hide does not automatically dismiss the dialog
    */
   private void hideAndDismissProcessDialog() {
      processDialog.hide();
      processDialog.dismiss();
   }

   @Override
   public void onMyselfSessionError(Session session, ErrorCode errorCode) {
      logger.debug("onMyselfSessionError(): {}, {}", session.getType(), errorCode);
      //disable overlays
      processOverlay.setMode(DataExchangeProcessOverlay.MODE.HIDDEN);
      hideDisabledOverlay();
      closeCancelDialog();

      if (ErrorCode.USB_REMOVED.equals(errorCode)) {
         showErrorStatePanel();
         //USB was removed, no tree available anymore
         hideTreeList();
         showStartMessage();
      }
      else {
         if (SessionUtil.isDiscoveryTask(session)) {
            //if task was discovery, just reset UI to beginning state
            showDragAndDropZone();
            hideTreeList();
            showStartMessage();
         }
         else {
            //if import was active, show error indicator
            showErrorStatePanel();
            //tree is still available
            showTreeList();
            initAndPopulateTree(session.getObjectData());

            /*
            //This is to be kept, since it is the approach to fix a defect (pfhmi-dev-defects-12995)
            //Removing the line "initAndPopulateTree(session.getObjectData());" above and
            //uncommenting the following lines somehow fixes the defect (but with having an unacceptable delay of 5 seconds visible)
            treeAdapter.refresh();
            treeViewList.invalidate();
            treeViewList.setAdapter(treeAdapter); //required
            //I have no(!) idea, why this delay of +5 seconds enables the treeViewList to update the selection properly!
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
               @Override
               public void run() {
                  treeAdapter.updateViewSelection(treeViewList);
                  treeViewList.refreshDrawableState();
               }
            }, 5000);
            */

         }
      }
      updateSelectAllState();
      updateImportButton();
   }

   /**
    * Update enable status & test on the import button.
    */
   private void updateImportButton() {
      Session s = getSession();
      boolean isActiveOperation = ((SessionUtil.isCalculateConflictsTask(s) || SessionUtil.isCalculateOperationsTask(s)) && Session.State.COMPLETE.equals(s.getState()))
            || (SessionUtil.isPerformOperationsTask(s) && s.getResultCode() == null);
      boolean connected = getSessionManager().isServiceConnected();
      boolean hasSelection = getTreeAdapter() != null && getTreeAdapter().hasSelection();
      boolean defaultButtonText = true;
      if (getTreeAdapter() != null && getTreeAdapter().getSelectionMap() != null) {
         int selectedItemCount = countSelectedItem();
         if (selectedItemCount > 0) {
            defaultButtonText = false;
            Resources resources = getResources();
            importSelectedBtn.setText(resources.getString(R.string.import_selected) + " (" + selectedItemCount + ")");
            if (selectedItemCount > MAX_TREE_SELECTIONS_FOR_DEFAULT_TEXT_SIZE) {
               importSelectedBtn.setTextSize(resources.getDimension(R.dimen.button_default_text_size) - resources.getDimension(R.dimen.decrease_text_size));
               importSelectedBtn.setPadding(resources.getInteger(R.integer.button_minimum_padding_left), resources.getInteger(R.integer.button_minimum_padding_top),
                     resources.getInteger(R.integer.button_minimum_padding_right), resources.getInteger(R.integer.button_minimum_padding_bottom));
            }
            else {
               importSelectedBtn.setTextSize(resources.getDimension(R.dimen.button_default_text_size));
               importSelectedBtn.setPadding(resources.getInteger(R.integer.button_default_padding_left), resources.getInteger(R.integer.button_default_padding_top),
                     resources.getInteger(R.integer.button_default_padding_right), resources.getInteger(R.integer.button_default_padding_bottom));
            }
         }
      }
      if (defaultButtonText) {
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

      // TODO: this creates the temporary folder on import from datamanagement, so it can be deleted from datamanagement as well.
      // this workaround prevents some trouble on a later export when datamanagement will try deleting the temporary folder that has been created by isoservice
      // it would be nice if we would find a cleaner solution for this
      // unfortunately setting permissions to 777 doesn't make a difference - data management is still unable to delete this folder if it has been created by isoservice
      final String tempPath = UtilityHelper.CommonPaths.PATH_TMP.getPathString();
      File tmpFolder = new File(tempPath);
      if (!tmpFolder.exists() && !tmpFolder.mkdirs()) {
         logger.error("unable to create tmp folder");
      }

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
   public void onPCMConnected() {
      logger.trace("PCM is online.");
      showLoadingOverlay();
      onResumeSession();
   }

   @Override
   public void onResume() {
      super.onResume();
      visualFeedbackActive = false;
   }

   @Override
   public void onResumeSession() {
      logger.debug("onResumeSession()");
      //verify that no other session is blocking fragment
      if (!requestAndUpdateBlockedOverlay(getBlockingActions())) {
         final Session session = getSession();

         if ((SessionUtil.isPerformOperationsTask(session) && SessionUtil.isComplete(session)) || SessionUtil.isCancelled(session)) {
            // This could happen when the import completes while the user focus is on other tab.
            // In that case, reset session & tree selection before updating UI.
            resetSession();
            clearTreeSelection();
            updateImportButton();
            updateSelectAllState();
         }

         if (SessionUtil.isDiscoveryTask(session) && (SessionUtil.isInProgress(session) || SessionUtil.isComplete(session))) {
            logger.info("There is already active session. Continue on the previous active session.");
            if (session.getObjectData() != null && !session.getObjectData().isEmpty()) {
               initAndPopulateTree(session.getObjectData());

               showTreeList();
               hideDisabledOverlay();
               updateSelectAllState();

               if (session.getExtra() != null && session.getExtra().isUsbExtra()) {
                  String filename = UtilityHelper.filenameOnly(session.getExtra().getPath());
                  if (filename == null) filename = "";
                  setHeaderText(filename);
               }
            }
            else {
               logger.info("Still loading data from media.");
               hideTreeList();
               hideStartMessage();
               showLoadingOverlay();
            }
         }
         else if (SessionUtil.isPerformOperationsTask(session) && SessionUtil.isInProgress(session)) {
            logger.info("There is import session (PERFORM_OPERATIONS) going on. Display the import process overlay.");
            showProgressPanel();
            processOverlay.setMode(DataExchangeProcessOverlay.MODE.IMPORT_PROCESS);
         }
         else {
            hideTreeList();
            hideDisabledOverlay();
            showStartMessage();
         }
         updateImportButton();
         updateSelectAllState();
      }
   }

   /** Shows conflict resolution dialog */
   private void showConflictDialog() {
      hideAndDismissProcessDialog();
      processDialog.init();
      processDialog.setTitle(getResources().getString(R.string.checking_conflicts));
      processDialog.setProgress(0);
      processDialog.setOnCancelListener(new DialogViewInterface.OnCancelListener() {
         @Override
         public void onCancel(DialogViewInterface dialogViewInterface) {
            //Show confirmation cancel-dialog
            DialogViewInterface.OnButtonClickListener onButtonClickListener = new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialog, int buttonId) {
                  if (buttonId == TextDialogView.BUTTON_FIRST) {
                     ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_cancel), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                           getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
                     cancelProcess();
                  }
               }
            };
            showCancelDialog(onButtonClickListener);
         }
      });

      processDialog.show();
   }

   /**
    * Cancel ongoing IMPORT process.
    */
   private void cancelProcess() {
      logger.debug("Cancel current import process.");
      cancel();
      hideAndDismissProcessDialog();
      processOverlay.setMode(DataExchangeProcessOverlay.MODE.HIDDEN);
      getSession().setType(Session.Type.DISCOVERY);
      clearTreeSelection();
      updateImportButton();
      updateSelectAllState();

      //close cancel dialog if still open
      closeCancelDialog();
   }

   /** Inflates left panel progress view */
   private void showProgressPanel() {
      //only update UI if no visual success/failure feedback is currently active
      if (!visualFeedbackActive) {
         importDropZone.setVisibility(View.GONE);
         progressBar.setSecondText(true, loadingString, null, true);
         progressBar.setProgress(0);
         importFinishedStatePanel.setVisibility(View.GONE);
         leftStatus.setVisibility(View.VISIBLE);
      }
   }

   /** Removes left panel progress view and replaces with operation view */
   private void showDragAndDropZone() {
      //only update UI if no visual success/failure feedback is currently active
      if (!visualFeedbackActive) {
         leftStatus.setVisibility(View.GONE);
         importFinishedStatePanel.setVisibility(View.GONE);
         importDropZone.setVisibility(View.VISIBLE);
      }
   }

   /**
    * This method is called to represent the finished import state in the UI.
    */
   private void showFinishedStatePanel() {
      logger.debug("showFinishedStatePanel");
      visualFeedbackActive = true;
      leftStatus.setVisibility(View.GONE);
      importDropZone.setVisibility(View.GONE);
      importFinishedStatePanel.setVisibility(View.VISIBLE);
      //post cleanup to show drag and drop zone after time X
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
         @Override
         public void run() {
            visualFeedbackActive = false;
            showDragAndDropZone();
         }
      }, SHOWING_FEEDBACK_AFTER_PROGRESS_MS);
   }

   /**
    * This method is called to represent the erroneous import state in the UI.
    */
   private void showErrorStatePanel() {
      logger.debug("showErrorStatePanel");
      visualFeedbackActive = true;
      importDropZone.setVisibility(View.GONE);
      importFinishedStatePanel.setVisibility(View.GONE);
      leftStatus.setVisibility(View.VISIBLE);
      progressBar.setErrorProgress(progressBar.getProgress(), getResources().getString(R.string.pb_error));
      //post cleanup to show drag and drop zone after time X
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
         @Override
         public void run() {
            visualFeedbackActive = false;
            showDragAndDropZone();
         }
      }, SHOWING_FEEDBACK_AFTER_PROGRESS_MS);
   }

   @Override
   public void onTreeItemSelected() {
      super.onTreeItemSelected();
      updateImportButton();
      updateSelectAllState();
   }

   @Override
   public void onProgressUpdate(String operation, int progress, int max) {
      final Double percent = ((progress * 1.0) / max) * 100;
      progressBar.setProgress(percent.intValue());
      progressBar.setSecondText(true, loadingString, String.format(xOfYFormat, progress, max), true);
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      // TEMPORARY: Do not import the four data types
      if (node.getType().equals("IMPLEMENT") || node.getType().equals("VEHICLE_IMPLEMENT") || node.getType().equals("VEHICLE_IMPLEMENT_CONFIG")
            || node.getType().equals("IMPLEMENT_PRODUCT_CONFIG")) {
         return false;
      }
      //For import, all formats supported
      return true;
   }

   /**
    * Shows Cancel Dialog with a custom listener if cancel is pressed.
    *
    * @param onClickListener Listener to be hooked to he cancel button
    */
   private void showCancelDialog(@Nullable final DialogViewInterface.OnButtonClickListener onClickListener) {
      final Resources resources = getResources();
      final TextDialogView cancelDialogue = new TextDialogView(ImportFragment.this.getActivity());
      cancelDialogue.setFirstButtonText(resources.getString(R.string.yes));
      cancelDialogue.setSecondButtonText(resources.getString(R.string.no));
      cancelDialogue.showThirdButton(false);
      cancelDialogue.setTitle(resources.getString(R.string.cancel));
      cancelDialogue.setBodyText(resources.getString(R.string.import_cancel_confirmation));
      cancelDialogue.setIcon(resources.getDrawable(R.drawable.ic_alert_red));
      cancelDialogue.setOnButtonClickListener(onClickListener);
      cancelDialogue.setDialogWidth(CANCEL_DIALOG_WIDTH);
      ((TabActivity) getActivity()).showModalPopup(cancelDialogue);
      cancelDialogue.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogViewInterface dialogViewInterface) {
            //remove reference just closed dialogue
            lastCancelDialogView = null;
         }
      });
      lastCancelDialogView = cancelDialogue;
   }

   /**
    * Dismisses Cancel Dialog if shown.
    */
   private void closeCancelDialog() {
      if (lastCancelDialogView != null) {
         final Activity activity = getActivity();
         if (activity instanceof TabActivity) {
            final TabActivity tabActivity = (TabActivity) getActivity();
            tabActivity.dismissPopup(lastCancelDialogView);
         }
         else {
            logger.debug("Could not dismiss Cancel Dialog!");
         }
      }
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

         processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
            @Override
            public void onButtonClick(DialogViewInterface dialog, int which) {
               if (which == DialogViewInterface.BUTTON_THIRD) {
                  //cancel button has been clicked
                  //Show confirmation cancel-dialog
                  DialogViewInterface.OnButtonClickListener onButtonClickListener = new DialogViewInterface.OnButtonClickListener() {
                     @Override
                     public void onButtonClick(DialogViewInterface dialog, int buttonId) {
                        if (buttonId == TextDialogView.BUTTON_FIRST) {
                           hideAndDismissProcessDialog();
                           ToastMessageCustom
                                 .makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_cancel), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                                       getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset))
                                 .show();
                           cancelProcess();
                        }
                     }
                  };
                  showCancelDialog(onButtonClickListener);

               }
            }
         });

         processDialog.show();
         calculateOperations(selected);
      }
   }

   private List<SessionExtra> generateImportExtras() {
      logger.debug("generateImportExtras: external storage state = {}", Environment.getExternalStorageState());
      boolean internalFileSystem = false;
      List<SessionExtra> list = new ArrayList<SessionExtra>();

      try {
         String fileStorage = UtilityHelper.getSharedPreferenceString(getActivity().getApplicationContext(), UtilityHelper.STORAGE_LOCATION_TYPE);
         String fileStorageLocation = UtilityHelper.getSharedPreferenceString(getActivity().getApplicationContext(), UtilityHelper.STORAGE_LOCATION);

         if (fileStorage != null && UtilityHelper.STORAGE_LOCATION_INTERNAL.equals(fileStorage) && fileStorageLocation != null && !fileStorageLocation.isEmpty()) {
            File storageFolder = new File(fileStorageLocation);
            if (storageFolder.exists() && storageFolder.canRead() && storageFolder.canWrite()) {
               SessionExtra newExtra = new SessionExtra(SessionExtra.USB, "USB", 0);
               internalFileSystem = true;
               newExtra.setUseInternalFileSystem(internalFileSystem);
               newExtra.setBasePath(storageFolder.getPath());
               newExtra.setPath(storageFolder.getPath());
               list.add(newExtra);

               logger.debug("using internal storage = {}", storageFolder);
            }
         }
      }
      catch (Exception e) {
         logger.info("Unable to check if internal flash need to be used.", e);
      }

      if (!internalFileSystem && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
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

         hideAndDismissProcessDialog();
         showDragAndDropZone();
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
