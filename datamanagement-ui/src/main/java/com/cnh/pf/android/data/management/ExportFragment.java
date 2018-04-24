/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.dialog.TextDialogView;
import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickList;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.android.widget.control.ProgressBarView;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.helper.IVIPDataHelper;
import com.cnh.pf.android.data.management.helper.VIPDataHandler;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.utility.UtilityHelper;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.DataManagementSession.SessionOperation;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.model.vip.vehimp.VehicleCurrent;
import com.cnh.pf.datamng.Process;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import roboguice.inject.InjectView;

import static com.cnh.pf.android.data.management.utility.UtilityHelper.NEGATIVE_BINARY_ERROR;

/**
 * Export Tab Fragment, handles export to external mediums {USB, External Display}.
 *
 * @author oscar.salazar@cnhind.com
 */
public class ExportFragment extends BaseDataFragment {
   private static final Logger logger = LoggerFactory.getLogger(ExportFragment.class);
   private static final String SAVED_MEDIUM = "medium";
   private static final String SAVED_FORMAT = "format";

   @Inject
   protected FormatManager formatManager;
   @InjectView(R.id.export_medium_picklist)
   PickList exportMediumPicklist;
   @InjectView(R.id.export_format_picklist)
   PickList exportFormatPicklist;
   @InjectView(R.id.export_drop_zone)
   LinearLayout exportDropZone;
   @InjectView(R.id.export_finished_state_panel)
   LinearLayout exportFinishedStatePanel;
   @InjectView(R.id.export_selected_btn)
   Button exportSelectedBtn;
   @InjectView(R.id.stop_button)
   ImageButton stopButton;
   @InjectView(R.id.left_status_panel)
   LinearLayout leftStatusPanel;
   @InjectView(R.id.progress_bar)
   ProgressBarView progressBar;
   @InjectView(R.id.operation_name)
   TextView operationName;

   private int dragAcceptColor;
   private int dragRejectColor;
   private int dragEnterColor;
   private int transparentColor;
   private VIPDataHandler vipDataHandler;
   private VipDataHelperListener vipDataHelperListener;
   private Map<Datasource.LocationType, String> displayStringMap;

   /**
    * Sets the VIP Data Handler
    * @param vipDataHandler
    */
   public void setVipDataHandler(VIPDataHandler vipDataHandler) {
      this.vipDataHandler = vipDataHandler;
   }

   private int progressCurrentValue = 0;
   private int progressMaxValue = 0;

   private String loading_string;
   private String x_of_y_format;

   private TextDialogView lastDialogView;
   private static final int CANCEL_DIALOG_WIDTH = 550;
   private static final int SHOWING_FEEDBACK_AFTER_PROGRESS_MS = 2000;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      try {
         formatManager.parseXml();
      }
      catch (Exception e) {
         logger.error("Error parsing xml file", e);
      }

      final Resources resources = getResources();
      dragAcceptColor = resources.getColor(R.color.drag_accept);
      dragRejectColor = resources.getColor(R.color.drag_reject);
      dragEnterColor = resources.getColor(R.color.drag_enter);
      transparentColor = resources.getColor(android.R.color.transparent);
      loading_string = resources.getString(R.string.loading_string);
      x_of_y_format = resources.getString(R.string.x_of_y_format);
      vipDataHelperListener = new VipDataHelperListener();
   }

   @Override
   public void inflateViews(LayoutInflater inflater, View leftPanel) {
      inflater.inflate(R.layout.export_left_panel, (LinearLayout) leftPanel);
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      exportSelectedBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            exportSelected();
         }
      });
      stopButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {

            final Resources resources = getResources();
            final TextDialogView cancelDialogue = new TextDialogView(ExportFragment.this.getActivity());
            cancelDialogue.setFirstButtonText(resources.getString(R.string.yes));
            cancelDialogue.setSecondButtonText(resources.getString(R.string.no));
            cancelDialogue.showThirdButton(false);
            cancelDialogue.setTitle(resources.getString(R.string.cancel));
            cancelDialogue.setBodyText(resources.getString(R.string.export_cancel_confirmation));
            cancelDialogue.setIcon(resources.getDrawable(R.drawable.ic_alert_red));
            cancelDialogue.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialog, int buttonId) {
                  if (buttonId == TextDialogView.BUTTON_FIRST) {
                     getDataManagementService().cancel(getSession());
                  }

               }
            });
            cancelDialogue.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
               @Override
               public void onDismiss(DialogViewInterface dialogViewInterface) {
                  //remove reference just closed dialogue
                  lastDialogView = null;
               }
            });
            cancelDialogue.setDialogWidth(CANCEL_DIALOG_WIDTH);
            lastDialogView = cancelDialogue;
            final TabActivity tabActivity = (DataManagementActivity) getActivity();
            tabActivity.showModalPopup(cancelDialogue);
         }
      });
      exportDropZone.setOnDragListener(new View.OnDragListener() {
         @Override
         public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
               if (exportSelectedBtn.isEnabled()) {
                  exportDropZone.setBackgroundColor(dragAcceptColor);
               }
               return true;
            case DragEvent.ACTION_DRAG_ENDED:
               exportDropZone.setBackgroundColor(transparentColor);
               return true;
            case DragEvent.ACTION_DRAG_ENTERED:
               exportDropZone.setBackgroundColor(exportSelectedBtn.isEnabled() ? dragEnterColor : dragRejectColor);
               return true;
            case DragEvent.ACTION_DRAG_EXITED:
               exportDropZone.setBackgroundColor(exportSelectedBtn.isEnabled() ? dragAcceptColor : transparentColor);
               return true;
            case DragEvent.ACTION_DROP:
               logger.info("Dropped");
               if (exportSelectedBtn.isEnabled()) {
                  exportSelected();
               }
               return true;
            }
            return false;
         }
      });

      exportFinishedStatePanel.setVisibility(View.GONE);
      startText.setVisibility(View.GONE);
      operationName.setText(R.string.exporting_string);
   }

   @Override
   public void onResume() {
      // Needs to clear tree selection. Otherwise, the previous selection is latched
      // until new DISCOVERY operation is finished.
      clearTreeSelection();
      populateExportToPickList();
      populateFormatPickList();
      DataManagementSession session = getSession();
      if (session != null) {
         if (session.getResult() == null) {
            //may be although null if building tree is in progress - additionally compare for session operation!
            SessionOperation sessionOperation = session.getSessionOperation();
            if (sessionOperation != null && sessionOperation.equals(SessionOperation.PERFORM_OPERATIONS)) {
               //still in operation mode - disable picklists and show process
               setExportPicklistsReadOnly(true);
               showProgressPanel();
               updateProgressbar(progressCurrentValue, progressMaxValue);
            }
         }
      }
      super.onResume();

      if (vipDataHandler != null) {
         vipDataHandler.addOnVehicleChangedListener(vipDataHelperListener);
         logger.debug("addOnVehicleChangedListener");
      }
   }

   @Override
   public void onPause() {
      super.onPause();
      if (vipDataHandler != null) {
         vipDataHandler.removeOnVehicleChangedListener(vipDataHelperListener);
         logger.debug("removeOnVehicleChangedListener");
      }
   }

   private class VipDataHelperListener implements IVIPDataHelper.OnVehicleChangedListener {

      @Override
      public void onCurrentVehicleChanged(VehicleCurrent vehicleCurrent) {
         logger.debug("VipDataHelperListener onCurrentVehicleChanged {}", vehicleCurrent);
      }
   }

   /**
    * Restore user selection for the format pick list. This works when a user switches back to this fragment.
    * PickList doesn't maintain its state upon fragment (tab) switch.
    */
   private void restoreFormatSelection() {
      if (getArguments() != null && getArguments().getString(SAVED_FORMAT) != null) {
         boolean found = false;
         String selectedFormat = getArguments().getString(SAVED_FORMAT);

         for (int i = 0; i < exportFormatPicklist.getAdapter().getCount(); i++) {
            PickListItem item = exportFormatPicklist.getAdapter().getItem(i);
            if (item.getValue().equals(selectedFormat)) {
               exportFormatPicklist.setSelectionByPosition(i);
               found = true;
               break;
            }
         }

         if (!found) {
            // Reset the selection if the previous selection cannot be found.
            resetFormatSelection();
            exportFormatPicklist.setDisplayText(R.string.select_string);
         }
      }
      else if (exportFormatPicklist.getAdapter().getCount() > 0) {
         exportFormatPicklist.setDisplayText(R.string.select_string); //Set to "Select" if no saved item selected
      }
   }

   /**
    * Save the current selection for the format pick list and it will be used later when a user comes back
    * to this fragment (tab).
    * @param selectedFormat current format selection in string
    */
   private void saveFormatSelection(String selectedFormat) {
      if (getArguments() != null) {
         getArguments().putString(SAVED_FORMAT, selectedFormat);
      }
   }

   /**
    * Reset the format selection.
    */
   private void resetFormatSelection() {
      if (getArguments() != null) {
         getArguments().putString(SAVED_FORMAT, null);
      }
   }

   /**
    * Restore user selection for the medium pick list. This works when a user switches back to this fragment.
    * PickList doesn't maintain its state upon fragment (tab) switch.
    */
   private void restoreMediumSelection() {
      if (getArguments() != null && getArguments().getParcelable(SAVED_MEDIUM) != null) {
         boolean found = false;
         MediumDevice savedMedium = getArguments().getParcelable(SAVED_MEDIUM);

         for (int i = 0; i < exportMediumPicklist.getAdapter().getCount(); i++) {
            ObjectPickListItem<MediumDevice> item = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.getAdapter().getItem(i);
            // Since there is no equals() defined in MediumDevice, comparison against toString()  would indicate
            // the equality of the object correctly. MediumDevice.toString() takes all attributes into account.
            if (item.getObject().toString().equals(savedMedium.toString())) {
               exportMediumPicklist.setSelectionByPosition(i);
               found = true;
               break;
            }
         }

         if (!found) {
            // Reset the selection if the previous selection cannot be found.
            resetMediumSelection();
            exportMediumPicklist.setDisplayText(R.string.select_string); //Set to "Select" if no saved item selected
         }
      }
      else if (exportMediumPicklist.getAdapter().getCount() > 0) {
         exportMediumPicklist.setDisplayText(R.string.select_string); //Set to "Select" if no saved item selected
      }
   }

   /**
    * Save the current selection for the medium pick list and it will be used later when a user comes back
    * to this fragment (tab).
    * @param selectedMedium current medium device selection
    */
   private void saveMediumSelection(MediumDevice selectedMedium) {
      if (getArguments() != null) {
         getArguments().putParcelable(SAVED_MEDIUM, selectedMedium);
      }
   }

   /**
    * Reset the medium selection.
    */
   private void resetMediumSelection() {
      if (getArguments() != null) {
         getArguments().putParcelable(SAVED_MEDIUM, null);
      }
   }

   private void populateFormatPickList() {
      exportFormatPicklist.setAdapter(new PickListAdapter(exportFormatPicklist, getActivity().getApplicationContext()));
      int formatId = 0;
      for (String format : formatManager.getFormats()) {
         logger.debug("Format: {}", format);
         exportFormatPicklist.addItem(new PickListItem(formatId++, format));
      }

      restoreFormatSelection();
      exportFormatPicklist.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean b) {
            if (getSession() != null) {
               String selectedFormat = (id != -1 ? exportFormatPicklist.findItemById(id).getValue() : null);
               getSession().setFormat(selectedFormat);
               saveFormatSelection(selectedFormat);
               logger.trace("setFormat {}", getSession().getFormat());
            }
            if (getTreeAdapter() != null) {
               getTreeAdapter().updateViewSelection(treeViewList);
            }
            checkExportButton();
            treeViewList.invalidate();
         }

         @Override
         public void onNothingSelected(AdapterView<?> adapterView) {
            resetFormatSelection();
            getSession().setFormat(null);
            exportFormatPicklist.setDisplayText(R.string.select_string);
         }
      });
   }

   private void populateExportToPickList() {
      exportMediumPicklist.setAdapter(new PickListAdapter(exportMediumPicklist, getActivity().getApplicationContext()));
      exportMediumPicklist.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean b) {
            if (getSession() != null) {
               MediumDevice itemObject = ((ObjectPickListItem<MediumDevice>) exportMediumPicklist.findItemById(id)).getObject();
               getSession().setDestinations(null != itemObject ? Arrays.asList(itemObject) : null);
               getSession().setDestinationTypes(null != itemObject ? itemObject.getType() : null);
               if (null != itemObject) {
                  saveMediumSelection(itemObject);
                  getSession().setDestination(itemObject);
               }
               forceMediumDeviceFormat();
            }
            checkExportButton();
         }

         @Override
         public void onNothingSelected(AdapterView<?> parent) {
            resetMediumSelection();
            getSession().setDestinations(null);
            checkExportButton();
         }
      });
      addMediumExportToPickList();
   }

   private boolean isExportProcessActive() {
      DataManagementSession currentDataManagementSession = getSession();
      return (null != currentDataManagementSession && (getSession().getSessionOperation() == SessionOperation.PERFORM_OPERATIONS) && getSession().isProgress());
   }

   /**
   While Medium Device is set to CLOUD, force to CNH format and disable user selection of format.
   While Medium Device is set to Fred/USB or DesktopSW/USB, force to ISOXML and disable user selection of format.
   */
   private void forceMediumDeviceFormat() {
      Boolean isReadOnly = false;
      Datasource.LocationType itemType = ((ObjectPickListItem<MediumDevice>) exportMediumPicklist.getSelectedItem()).getObject().getType();
      if (null != itemType) {
         if (itemType.equals(Datasource.LocationType.CLOUD_OUTBOX)) { //Cloud export is only done in PF Database format
            isReadOnly = true;
            // "PF Database" is format type in formats.xml file
            exportFormatPicklist.setSelectionById(exportFormatPicklist.findItemPositionByValue("PF Database", true));
            logger.debug("Force format to PF Database");
         }
         else if (itemType.equals(Datasource.LocationType.USB_DESKTOP_SW) ||
                 itemType.equals(Datasource.LocationType.USB_FRED)) {
            isReadOnly = true;
            // "ISOXML" is format type in formats.xml file
            exportFormatPicklist.setSelectionById(exportFormatPicklist.findItemPositionByValue("ISOXML", true));
            logger.debug("Force format to ISOXML");
         }
         else if (itemType.equals(Datasource.LocationType.USB_PHOENIX) ||
                 itemType.equals(Datasource.LocationType.USB_HAWK)) {
            exportFormatPicklist.setSelectionById(exportFormatPicklist.findItemPositionByValue("PF Database", true));
            logger.debug("Force format to PF Database as default");
         }
      }
      exportFormatPicklist.setReadOnly(isReadOnly);
   }

   /**
    * Adds devices to the destination export list based on the vehicle make and devices available
    */
   private void addMediumExportToPickList() {
      DataManagementService service = getDataManagementService();
      if (service == null) return;
      if (exportMediumPicklist.findItemPositionById(0) != NEGATIVE_BINARY_ERROR) {
         exportMediumPicklist.clearList();
      }

      boolean resetTarget = true;
      List<MediumDevice> devices = service.getMediums();  //Determine how many devices should be in the new list

      if (!isEmpty(devices)) {
         String defaultMakeString = getResources().getString(R.string.unknown_vehicle);
         displayStringMap = UtilityHelper.getListOfDestinations(vipDataHandler.getMakeOfVehicle(defaultMakeString), devices, getResources()); //Creates map of strings based on vehicle brand and devices available
         if (!displayStringMap.isEmpty()) {
            // Delete items from the picklist that don't appear in device list
            exportMediumPicklist.setAllowItemsDeletion(true);  //To delete items, globalAllowItemDeletion needs to be set to true first.
            int count = exportMediumPicklist.getAdapter().getCount();
            long thisID = 0;
            for (int i = count; i >= 0; i--) {
               thisID = exportMediumPicklist.getAdapter().getItemId(i);
               if (!displayStringMap.containsKey(Datasource.LocationType.getLocationType((int)thisID))) {
                  exportMediumPicklist.deleteItem(thisID); //Delete item from picklist if it's not in new list of devices
               }
            }
            exportMediumPicklist.setAllowItemsDeletion(false); //Set delete back to false

            // Add items to list TODO: - Combine delete and add functionality
            for (Map.Entry<Datasource.LocationType, String> s : displayStringMap.entrySet()) {
               for (MediumDevice d : devices) {
                  if (d.getType() == s.getKey() && null == exportMediumPicklist.findItemById(s.getKey().getValue())) {
                     exportMediumPicklist.addItem(new ObjectPickListItem<MediumDevice>(s.getKey().getValue(), s.getValue(), devices.get(devices.indexOf(d)))); //Add item if not in the list
                     break;
                  }
                  if (getSession() != null && getSession().getDestination() != null && getSession().getDestination().getType() == d.getType()) {
                     resetTarget = false;
                  }
               }
            }
            exportMediumPicklist.sortAdapterData(new IDPicklistComparator()); //Sort based on the devices ID value
         }
         restoreMediumSelection();
      }
      else {
         resetMediumSelection();
         if (exportMediumPicklist.getAdapter().getCount() > 0) { //There are no medium devices, but picklist is still populated
            exportMediumPicklist.getAdapter().clear();
         }
         exportFormatPicklist.setReadOnly(false);
         exportMediumPicklist.setDisplayText(R.string.connect_source_string); //Set to "Connect Source" since no medium are present
      }

      if (getSession() != null && resetTarget) {
         getSession().setDestinations(null);
      }
   }

   /**
   * Comparator class used to order position of Encryption Key Picklist so that "None" value is always top position
   *
   * @author mreece
   */
   private class IDPicklistComparator implements Comparator<PickListItem> {
      @Override
      public int compare(PickListItem lhs, PickListItem rhs) {
         return (int) lhs.getId() - (int) rhs.getId();
      }
   }

   @Override
   public DataManagementSession createSession() {
      logger.trace("createSession()");
      super.onNewSession();
      DataManagementSession oldSession = getSession();

      if (hasLocalSource) {
         disabled.setVisibility(View.GONE);
         treeProgress.setVisibility(View.VISIBLE);
      }
      else {
         disabled.setVisibility(View.VISIBLE);
         disabled.setMode(DisabledOverlay.MODE.DISCONNECTED);
         return null;
      }
      DataManagementSession session = new DataManagementSession(new Datasource.LocationType[] { Datasource.LocationType.PCM, Datasource.LocationType.DISPLAY }, null, null, null, null, null);
      sessionInit(session);
      if (oldSession != null) {
         session.setFormat(oldSession.getFormat());
         session.setDestinations(oldSession.getDestinations());
      }
      return session;
   }

   @Override
   public void configSession(DataManagementSession session) {
      sessionInit(session);//only consider one case
   }

   private DataManagementSession sessionInit(DataManagementSession session) {
      if(session != null) {
         session.setSourceTypes(new Datasource.LocationType[]{Datasource.LocationType.PCM, Datasource.LocationType.DISPLAY});
         session.setSources(null);
         session.setDestinationTypes(null);
         session.setDestinations(null);
         session.setFormat(exportFormatPicklist.getSelectedItem() != null ? exportFormatPicklist.getSelectedItem().getValue() : formatManager.getFormats().iterator().next());
         if (exportMediumPicklist.getAdapter().getCount() > 0) {
            ObjectPickListItem<MediumDevice> item = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.getSelectedItem();
            session.setDestinations(item != null ? Arrays.asList(item.getObject()) : null);
         }
      }
      return session;
   }

   public static boolean isEmpty(Collection<?> collection) {
      return collection == null || collection.isEmpty();
   }

   public static boolean isEmpty(Object[] array) {
      return array == null || array.length == 0;
   }

   @Override
   public void setSession(DataManagementSession session) {
      super.setSession(session);
      if (session != null) {
         logger.debug("setSession format: {}", session.getFormat());
         if (session.getFormat() == null) {
            exportFormatPicklist.setDisplayText(R.string.select_string);
         }
         else {
            exportFormatPicklist.setSelectionByPosition(new ArrayList<String>(formatManager.getFormats()).indexOf(session.getFormat()));
         }

         if (!isEmpty(session.getDestinations())) {
            ObjectPickListItem<MediumDevice> item = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.getAdapter().getItem(exportMediumPicklist.getSelectedItemPosition());
            session.setDestination(item.getObject());
         }
         else {
            session.setDestinations(null);
            exportMediumPicklist.setDisplayText(R.string.connect_source_string);
         }
      }
      else {
         exportFormatPicklist.setDisplayText(R.string.select_string);
         exportMediumPicklist.setDisplayText(R.string.connect_source_string);
      }
      checkExportButton();
   }

   @Override
   protected void onErrorOperation() {
      if (getSession().getSessionOperation().equals(SessionOperation.DISCOVERY)) {
         idleUI();
      }
      else {
         logger.debug("Other operations when error");
         showFinishedStatePanel(false);
         setExportPicklistsReadOnly(false);
         postTreeUI();
      }
   }

   @Override
   public void processOperations() {
      DataManagementSession session = getSession();
      if (session == null) {
         showDragAndDropZone();
         return;
      }
      logger.debug("processOperation() session result: {}", session.getResult());
      if (session.getResult() != null && session.getResult().equals(Process.Result.ERROR)) {
         onErrorOperation();
      }
      else {
         if (session.getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS)) {
            logger.trace("resetting new session.  Operation completed.");
            getTreeAdapter().selectAll(treeViewList, false);
            if (session.getResult() != null) {
               setExportPicklistsReadOnly(false);
               if (session.getResult().equals(Process.Result.SUCCESS)) {
                  showFinishedStatePanel(true);
                  progressCurrentValue = 0;
                  progressMaxValue = 0;
                  Toast.makeText(getActivity(), getString(R.string.export_complete), Toast.LENGTH_LONG).show();
               }
               else {
                  showDragAndDropZone();
               }
               configSession(session);
            }
            else {
               showProgressPanel();
            }
         }
      }
      checkExportButton();
   }

   @Override
   public boolean isCurrentOperation(DataManagementSession session) {
      logger.trace("isCurrentOperation( {} == {})", session, getSession());
      return session.equals(getSession());
   }

   @Override
   public void onTreeItemSelected() {
      super.onTreeItemSelected();
      checkExportButton();
   }

   private void updateProgressbar(int progressValue, int maxValue) {
      progressCurrentValue = progressValue;
      progressMaxValue = maxValue;
      final Double percent = ((progressValue * 1.0) / maxValue) * 100;
      progressBar.setProgress(percent.intValue());
      progressBar.setSecondText(true, loading_string, String.format(x_of_y_format, progressValue, maxValue), true);
   }

   @Override
   public void onProgressPublished(String operation, int progress, int max) {
      logger.debug("onProgressPublished: {}", progress);
      updateProgressbar(progress, max);
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      return (getSession() != null && getSession().getFormat() != null) ? formatManager.formatSupportsType(getSession().getFormat(), node.getType()) : false;
   }

   @Override
   public void enableButtons(boolean enable) {
      super.enableButtons(enable);
      checkExportButton();
   }

   /**
    * Set export picklists as enabled or disabled.
    * @param readOnly True if picklists are to be set to readonly, false otherwise
    */
   private void setExportPicklistsReadOnly(boolean readOnly) {
      exportMediumPicklist.setReadOnly(readOnly);

      //Only change state of Format Picklist if it isn't one of the three data sources below
      Datasource.LocationType itemType = ((ObjectPickListItem<MediumDevice>) exportMediumPicklist.getSelectedItem()).getObject().getType();
      if (itemType.equals(Datasource.LocationType.CLOUD_OUTBOX) ||
              itemType.equals(Datasource.LocationType.USB_DESKTOP_SW) ||
              itemType.equals(Datasource.LocationType.USB_FRED)) {
         exportFormatPicklist.setReadOnly(true);
      }
      else {
         exportFormatPicklist.setReadOnly(readOnly);
      }
   }

   @Override
   protected void onOtherSessionUpdate(DataManagementSession session) {
      logger.debug("Other session has been updated");
      if (session.getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS)) {
         logger.trace("Other operation completed. Enabling UI.");
         checkExportButton();
      }
   }

   private void exportSelected() {
      progressCurrentValue = 0;
      progressMaxValue = 0;
      getSession().setData(null);
      getSession().setObjectData(new ArrayList<ObjectGraph>(getTreeAdapter().getSelected()));
      getSession().setDestination(((ObjectPickListItem<MediumDevice>) exportMediumPicklist.getSelectedItem()).getObject());
      if (!getSession().getObjectData().isEmpty()) {
         setSession(getDataManagementService().processOperation(getSession(), SessionOperation.PERFORM_OPERATIONS));
         showProgressPanel();
         setExportPicklistsReadOnly(true);
      }
      else {
         Toast.makeText(getActivity(), "No data of selected format selected", Toast.LENGTH_LONG).show();
      }
   }

   private void showDragAndDropZone() {
      logger.debug("showDragAndDropZone");
      exportFinishedStatePanel.setVisibility(View.GONE);
      exportDropZone.setVisibility(View.VISIBLE);
      leftStatusPanel.setVisibility(View.GONE);
   }

   /**
    * This method is called to represent the finished export state in the UI.
    * @param successfullyFinished True if export finished successfully, false in case of an occurred error.
    */
   private void showFinishedStatePanel(boolean successfullyFinished) {
      logger.debug("showFinishedStatePanel:{}", successfullyFinished);
      //show content depending on success or failure
      if (successfullyFinished) {
         //successfully finished
         exportDropZone.setVisibility(View.GONE);
         leftStatusPanel.setVisibility(View.GONE);
         exportFinishedStatePanel.setVisibility(View.VISIBLE);
      }
      else {
         //error appeared
         progressBar.setErrorProgress(progressBar.getProgress(), getResources().getString(R.string.pb_error));
         stopButton.setVisibility(View.GONE);
      }
      //post cleanup to show drag and drop zone after time X
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
         @Override
         public void run() {
            showDragAndDropZone();
         }
      }, SHOWING_FEEDBACK_AFTER_PROGRESS_MS);
      //if cancel popup was active - close popup (cannot be canceled anyway)
      if (lastDialogView != null) {
         final TabActivity tabActivity = (DataManagementActivity) getActivity();
         tabActivity.dismissPopup(lastDialogView);
      }
   }

   /** Inflates left panel progress view */
   private void showProgressPanel() {
      logger.debug("showProgressPanel");
      //reset button and process
      stopButton.setVisibility(View.VISIBLE);
      // TODO: update to the new CORE39 API
      progressBar.setErrorProgress(0, getResources().getString(R.string.pb_error));
      progressBar.setSecondText(true, loading_string, null, true);
      progressBar.setProgress(0); //resets error if set
      //set visibility of sections
      exportFinishedStatePanel.setVisibility(View.GONE);
      exportDropZone.setVisibility(View.GONE);
      leftStatusPanel.setVisibility(View.VISIBLE);
   }

   private void checkExportButton() {
      DataManagementSession s = getSession();
      boolean isActiveOperation = s != null && s.getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS) && s.getResult() == null;
      boolean defaultButtonText = true;
      if (getTreeAdapter() != null && getTreeAdapter().getSelectionMap() != null) {
         int selectedItemCount = getTreeAdapter().getSelectionMap().size();
         if (selectedItemCount > 0) {
            defaultButtonText = false;
            exportSelectedBtn.setText(getResources().getString(R.string.export_selected) + " (" + getTreeAdapter().getSelectionMap().size() + ")");
         }
      }
      if (defaultButtonText == true) {
         exportSelectedBtn .setText(getResources().getString(R.string.export_selected));
      }
      boolean hasSelection = getTreeAdapter() != null
              && s != null
              && s.getDestinations() != null
              && s.getDestination() != null
              && s.getFormat() != null
              && getTreeAdapter().hasSelection();

      exportSelectedBtn.setEnabled(hasSelection && !isActiveOperation);
   }

   @Override
   public void onMediumsUpdated(List<MediumDevice> mediums) throws RemoteException {
      logger.info("onMediumsUpdated {}", mediums);
      //switch from callback thread to UI thread
      getActivity().runOnUiThread(new Runnable() {
         @Override
         public void run() {
            //refresh the export to list
            addMediumExportToPickList();
            checkExportButton();
         }
      });
   }

   public static class ObjectPickListItem<T> extends PickListItem {
      private T object;

      public ObjectPickListItem(long id, String value, T object) {
         super(id, value);
         this.object = object;
      }

      public T getObject() {
         return object;
      }
   }
}
