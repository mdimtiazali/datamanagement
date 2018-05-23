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
import android.os.RemoteException;
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
import com.cnh.android.pf.widget.view.DisabledOverlay;
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
   ProcessDialog processDialog;
   //store original data in case cancel is pressed.  so we can restore it.
   private List<ObjectGraph> originalData;
   private List<MediumDevice> previousDevices;//keep the previous path of process

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
               importSelected();
               return true;
            }
            return false;
         }
      });
      processDialog = new ProcessDialog(getActivity());
      startText.setVisibility(View.GONE);
      operationName.setText(R.string.importing_string);
      checkImportButton();
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
   public void enableButtons(boolean enable) {
      super.enableButtons(enable);
      checkImportButton();
   }

   @Override
   protected void onOtherSessionUpdate(DataManagementSession session) {
      logger.debug("Other session has been updated: {}", session.getSessionOperation().name());
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
      logger.debug("onImportSourceSelected( {} )", event.getDevice());
      startText.setVisibility(View.GONE);
      removeProgressPanel();
      progressUI();

      if(getSession() == null){
         setSession(createSession());
      }
      setCancelled(false);
      configSession(getSession());
      getSession().setSources(event.getDevices());
      getSession().setSource(event.getDevice());
      getDataManagementService().processOperation(getSession(), SessionOperation.DISCOVERY);
      previousDevices = event.getDevices();
   }

   @Override
   public DataManagementSession createSession(){
      logger.debug("createSession()");
      removeProgressPanel();
      super.onNewSession();

      if (hasLocalSource) {
         disabled.setVisibility(View.GONE);
         startText.setVisibility(View.INVISIBLE);
      }
      else {
         startText.setVisibility(View.GONE);
         disabled.setVisibility(View.VISIBLE);
         disabled.setMode(DisabledOverlay.MODE.DISCONNECTED);
      }
      //this is a stub Medium device, when user select source, it will be updated
      List stubDevices = new ArrayList<MediumDevice>();
      stubDevices.add(new MediumDevice(Datasource.LocationType.USB_PHOENIX));
      return new DataManagementSession(null, new Datasource.LocationType[] { Datasource.LocationType.PCM, Datasource.LocationType.DISPLAY }, stubDevices, null, null, null);
   }

   /** we have to override here since two cases to consider, when import fragment was created with session from savedInstanceState,
    *  resumed with previous session will include the previous datasource address
    * when import fragment was resume from onPause(), it also include previous datasource address
    * with datasource address won't be processed for dicovery. it need path.
    **/
   @Override
   public void configSession(DataManagementSession session) {
      if(previousDevices != null){
         logger.debug("previous devices are {}",previousDevices);
         session.setDestinations(null);
         session.setSources(previousDevices);
      }
      else {
         sessionInit(session);
      }
   }

   @Override
   protected void onErrorOperation() {
      if(getSession().getSessionOperation().equals(SessionOperation.DISCOVERY)){
         previousDevices = null;//clean to null
         idleUI();
         pathText.setText("");
         startText.setVisibility(View.VISIBLE);
      }
      else {
         logger.debug("Other operations when error");
         sessionInit(getSession());
         removeProgressPanel();
         postTreeUI();
      }
   }

   private DataManagementSession sessionInit(DataManagementSession session) {
      logger.debug("sessionInit() enter");
      if (session != null) {
         session.setSourceTypes(null);
         session.setSources(new ArrayList<MediumDevice>() {
            {
               add(new MediumDevice(Datasource.LocationType.USB_PHOENIX));
            }
         });
         session.setDestinationTypes(new Datasource.LocationType[] { Datasource.LocationType.PCM, Datasource.LocationType.DISPLAY });
         session.setDestinations(null);
      }
      return session;
   }
   //this is a stub Medium device, when user select source, it will be updated

   @Override
   public void processOperations() {
      // need to take care of error case in here
      logger.debug("processOperations()-SessionOperation:{}, Result:{}", getSession().getSessionOperation().name(), getSession().getResult().name());
      if (getSession().getResult().equals(Process.Result.NO_DATASOURCE) && getSession().getSessionOperation().equals(SessionOperation.DISCOVERY)) {
         removeProgressPanel();
         startText.setVisibility(View.VISIBLE);
      }
      else if (getSession().getResult().equals(Process.Result.ERROR) || getSession().getResult().equals(Process.Result.NO_DATASOURCE)) {
         if (getSession().getSessionOperation().equals(SessionOperation.DISCOVERY)) {
            idleUI();
         }
         else {
            removeProgressPanel();
            postTreeUI();
         }
      }
      else if (getSession().getResult().equals(Process.Result.CANCEL)) {
         getSession().setSessionOperation(SessionOperation.DISCOVERY);
         checkImportButton();
      }
      else {
         if (getSession().getSessionOperation().equals(SessionOperation.DISCOVERY)) {
            if (previousDevices != null) {

               if (previousDevices.get(0).getType().equals(Datasource.LocationType.USB_PHOENIX)) {
                  pathText.setText(previousDevices.get(0).getPath() == null ? "" : previousDevices.get(0).getPath().getPath());
               }
               else {
                  pathText.setText(getString(R.string.display_named, previousDevices.get(0).getName()));
               }
            }

         }
         else if (getSession().getSessionOperation().equals(SessionOperation.CALCULATE_OPERATIONS) && !isCancelled()) {
            logger.debug("Calculate Targets");
            if (getSession().getData() == null || getSession().getData().isEmpty()) {
               ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.no_operations_check_connectivity_string),
                       Gravity.TOP| Gravity.CENTER_HORIZONTAL, getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
               cancel();
               return;
            }
            boolean hasMultipleTargets = false;
            for (Operation operation : getSession().getData()) {
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
               // TEMPORARY: Do now show the second button (Replace button)
               processDialog.showSecondButton(false);
//               processDialog.setSecondButtonText(replaceStr);
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
            if (getSession().getResult() != null) {
               getTreeAdapter().selectAll(treeViewList, false);
               removeProgressPanel();
               if (getSession().getResult().equals(Process.Result.SUCCESS)) {
                  ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_complete),
                          Gravity.TOP| Gravity.CENTER_HORIZONTAL, getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
               }
               else if (getSession().getResult().equals(Process.Result.CANCEL)) {
                  ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.import_cancel),
                          Gravity.TOP| Gravity.CENTER_HORIZONTAL, getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
               }
            }
            else {
               showProgressPanel();
            }
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
      processDialog.hide();
      processDialog.init();
      processDialog.setTitle(getResources().getString(R.string.checking_conflicts));
      processDialog.setProgress(0);
      processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogViewInterface dialog) {
            cancel();
         }
      });
      processDialog.show();
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

   /** Check if session returned by service is an import operation*/
   @Override
   public boolean isCurrentOperation(DataManagementSession session) {
      logger.trace("isCurrentOperation( {} == {})", session.getUuid(), getSession().getUuid());
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
      boolean usbFound = false;

      for (MediumDevice medium : mediums) {
         Datasource.LocationType itemType = medium.getType();
         if (itemType.equals(Datasource.LocationType.USB_PHOENIX)
                 || itemType.equals(Datasource.LocationType.USB_HAWK)
                 || itemType.equals(Datasource.LocationType.USB_FRED)
                 || itemType.equals(Datasource.LocationType.USB_DESKTOP_SW)) {
            usbFound = true;
            break;
         }
      }

      // No USB plugged in. Reset the screen to initial state and reset session state as well.
      if (!usbFound && !isCancelled() && Environment.getExternalStorageState().equals(MEDIA_BAD_REMOVAL)) {
         logger.debug("Reset the import screen and session state.");
         if (getTreeAdapter() != null) getTreeAdapter().selectAll(treeViewList, false);
         processDialog.hide();
         setCancelled(true);
         checkImportButton();
         sessionInit(getSession());
         removeProgressPanel();
         getSession().setObjectData(null);
         pathText.setText("");
         startText.setVisibility(View.VISIBLE);
         updateSelectAllState();
      }
   }
}
