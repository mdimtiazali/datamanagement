/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import static com.cnh.pf.android.data.management.utility.UtilityHelper.EXPORT_DEST_POPOVER_DEFAULT_HEIGHT;
import static com.cnh.pf.android.data.management.utility.UtilityHelper.EXPORT_FORMAT_POPOVER_DEFAULT_HEIGHT;
import static com.cnh.pf.android.data.management.utility.UtilityHelper.MAX_TREE_SELECTIONS_FOR_DEFAULT_TEXT_SIZE;
import static com.cnh.pf.android.data.management.utility.UtilityHelper.NEGATIVE_BINARY_ERROR;
import static com.cnh.pf.android.data.management.utility.UtilityHelper.POPOVER_DEFAULT_WIDTH;
import static com.cnh.pf.model.constants.stringsConstants.BRAND_CASE_IH;
import static com.cnh.pf.model.constants.stringsConstants.BRAND_NEW_HOLLAND;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.dialog.TextDialogView;
import com.cnh.android.pf.widget.controls.PopoverWindowInfoView;
import com.cnh.android.pf.widget.controls.ToastMessageCustom;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickList;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.android.widget.control.Popover;
import com.cnh.android.widget.control.ProgressBarView;
import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.helper.DataExchangeProcessOverlay;
import com.cnh.pf.android.data.management.helper.IVIPDataHelper;
import com.cnh.pf.android.data.management.helper.VIPDataHandler;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.android.data.management.utility.UtilityHelper;
import com.cnh.pf.model.vip.vehimp.VehicleCurrent;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import roboguice.inject.InjectView;

/**
 * Export Tab Fragment, handles export to external mediums {USB, External Display}.
 *
 * @author oscar.salazar@cnhind.com
 */
public class ExportFragment extends BaseDataFragment {
   private static final Logger logger = LoggerFactory.getLogger(ExportFragment.class);
   private static final String SAVED_MEDIUM = "medium";
   private static final String SAVED_FORMAT = "format";
   private static final String PFDATABASE_FORMAT = "PF Database";
   private static final String ISOXML_FORMAT = "ISOXML";

   @Inject
   protected FormatManager formatManager;
   @InjectView(R.id.export_medium_picklist)
   PickList exportMediumPicklist;
   @InjectView(R.id.export_format_picklist)
   PickList exportFormatPicklist;
   @InjectView(R.id.export_drop_zone)
   LinearLayout exportDropZone;
   @InjectView(R.id.export_drop_zone_image)
   ImageView exportDropZoneImage;
   @InjectView(R.id.export_drop_zone_text)
   TextView exportDropZoneText;
   @InjectView(R.id.dataexchange_success_zone)
   LinearLayout exportFinishedStatePanel;
   @InjectView(R.id.dataexchange_success_text)
   TextView exportFinishedText;
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
   @InjectView(R.id.start_text)
   TextView startText;
   @InjectView(R.id.dest_info_btn)
   ImageButton destInfoButton;
   @InjectView(R.id.format_info_btn)
   ImageButton formatInfoButton;

   private final List<Session.Action> blockingActions = new ArrayList<Session.Action>(Arrays.asList(Session.Action.IMPORT));

   private int transparentColor;
   private int whiteTextColor;
   private int defaultTextColor;

   private Drawable exportWhiteIcon;
   private Drawable exportDefaultIcon;

   private VIPDataHandler vipDataHandler;
   private VipDataHelperListener vipDataHelperListener;
   private Map<Datasource.LocationType, String> displayStringMap;
   /** Used to indicate if the path is internal or external */
   private boolean useInternalFileSystem = false;

   private ProgressValue progressValue = ProgressValue.initProgress();

   private String loading_string;
   private String x_of_y_format;

   private TextDialogView lastDialogView;
   private static final int CANCEL_DIALOG_WIDTH = 550;

   /**
    * Sets the VIP Data Handler
    * @param vipDataHandler
    */
   public void setVipDataHandler(VIPDataHandler vipDataHandler) {
      this.vipDataHandler = vipDataHandler;
   }

   static class ProgressValue {
      private int currentValue;
      private int maxValue;

      public ProgressValue(int currentValue, int maxValue) {
         this.currentValue = currentValue;
         this.maxValue = maxValue;
      }

      /**
       * Getter for current progress value
       *
       * @return  current progress value
       */
      public int getCurrentValue() {
         return this.currentValue;
      }

      /**
       * Setter for current progress value
       *
       * @param currentValue  progress value
       */
      public void setCurrentValue(int currentValue) {
         this.currentValue = currentValue;
      }

      /**
       * Getter for the progress max value
       *
       * @return  the max value
       */
      public int getMaxValue() {
         return this.maxValue;
      }

      /**
       * Setter for the progress max value
       * @param maxValue
       */
      public void setMaxValue(int maxValue) {
         this.maxValue = maxValue;
      }

      /**
       * Return ProgressValue instance with initialized value set to zeros.
       * @return  ProgressValue instance
       */
      public static ProgressValue initProgress() {
         return new ProgressValue(0, 0);
      }
   }

   @Override
   protected List<Session.Action> getBlockingActions() {
      return blockingActions;
   }

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
      transparentColor = resources.getColor(android.R.color.transparent);
      whiteTextColor = resources.getColor(R.color.drag_drop_white_text_color);
      defaultTextColor = resources.getColor(R.color.drag_drop_default_text_color);

      exportWhiteIcon = resources.getDrawable(R.drawable.export_ic_white);
      exportDefaultIcon = resources.getDrawable(R.drawable.export_icon);

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
            runExport();
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
                     cancel();
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
               return true;
            case DragEvent.ACTION_DRAG_ENDED:
               return true;
            case DragEvent.ACTION_DRAG_ENTERED:
               if (!exportSelectedBtn.isEnabled()) {
                  ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.error_drag_drop), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                        getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
               }
               else {
                  setExportDragDropArea(DragEvent.ACTION_DRAG_ENTERED);
               }
               return true;
            case DragEvent.ACTION_DRAG_EXITED:
               if (exportSelectedBtn.isEnabled()) {
                  setExportDragDropArea(DragEvent.ACTION_DRAG_EXITED);
               }
               return true;
            case DragEvent.ACTION_DROP:
               logger.info("Dropped");
               if (exportSelectedBtn.isEnabled()) {
                  setExportDragDropArea(DragEvent.ACTION_DROP);
                  runExport();
               }
               return true;
            }
            return false;
         }
      });
      destInfoButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            destInfoButtonClicked();
         }
      });
      formatInfoButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            formatInfoButtonClicked();
         }
      });

      exportFinishedStatePanel.setVisibility(View.GONE);
      exportFinishedText.setText(R.string.export_complete);
      startText.setVisibility(View.GONE);
      operationName.setText(R.string.exporting_string);
   }

   private void setExportDragDropArea(int event) {
      if (event == DragEvent.ACTION_DRAG_ENTERED) {
         exportDropZoneImage.setImageDrawable(exportWhiteIcon);
         exportDropZoneText.setTextColor(whiteTextColor);
         exportDropZone.setBackgroundResource(R.drawable.dashed_border_accept);
      }
      else {
         exportDropZoneImage.setImageDrawable(exportDefaultIcon);
         exportDropZoneText.setTextColor(defaultTextColor);
         exportDropZone.setBackgroundResource(event == DragEvent.ACTION_DRAG_EXITED ? R.drawable.dashed_border_selected : R.drawable.dashed_border_initial);
      }
   }

   private void destInfoButtonClicked() {
      Context popoverContext = getActivity().getApplicationContext();
      String destInfoString = null;
      String defaultMakeString = getResources().getString(R.string.unknown_vehicle);
      String vehicleBrand = vipDataHandler.getMakeOfVehicle(defaultMakeString);
      if (vehicleBrand.equals(BRAND_CASE_IH)) {
         destInfoString = getString(R.string.case_destination_info);
      }
      else if (vehicleBrand.equals(BRAND_NEW_HOLLAND)) {
         destInfoString = getString(R.string.new_holland_destination_info);
      }

      PopoverWindowInfoView infoPopupWindow = new PopoverWindowInfoView(popoverContext, POPOVER_DEFAULT_WIDTH, EXPORT_DEST_POPOVER_DEFAULT_HEIGHT, Popover.Style.LIGHT_INFO);
      infoPopupWindow.setDescription(destInfoString);
      infoPopupWindow.setTitle(getString(R.string.destination_title));
      infoPopupWindow.showAt(destInfoButton, Gravity.END, Popover.ArrowPosition.LEFT_TOP);
   }

   private void formatInfoButtonClicked() {
      Context popoverContext = getActivity().getApplicationContext();
      PopoverWindowInfoView infoPopupWindow = new PopoverWindowInfoView(popoverContext, POPOVER_DEFAULT_WIDTH, EXPORT_FORMAT_POPOVER_DEFAULT_HEIGHT, Popover.Style.LIGHT_INFO);
      infoPopupWindow.setDescription(getString(R.string.format_description));
      infoPopupWindow.setTitle(getString(R.string.format_description_title));
      infoPopupWindow.showAt(formatInfoButton, Gravity.END, Popover.ArrowPosition.LEFT_TOP);
   }

   @Override
   public void onResume() {
      // Needs to clear tree selection. Otherwise, the previous selection is latched
      // until new DISCOVERY operation is finished.
      clearTreeSelection();
      initPickList();
      populateExportToPickList();
      populateFormatPickList();

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

   @Override
   public void onChannelConnectionChange(boolean updateNeeded) {
      logger.debug("onChannelConnectionChange()");

      if (updateNeeded) {
         hideTreeList();
         showLoadingOverlay();

         discovery();
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
      if (getArguments() != null && getArguments().getString(SAVED_FORMAT) != null && exportFormatPicklist.getAdapter().getCount() > 0) {
         boolean found = false;
         String selectedFormat = getArguments().getString(SAVED_FORMAT);
         logger.debug("Saved format: {}", selectedFormat);

         int pos = exportFormatPicklist.findItemPositionByValue(selectedFormat, true);
         if (pos >= 0) {
            exportFormatPicklist.setSelectionByPosition(pos);
         }
         else {
            // Reset the selection if the previous selection cannot be found.
            resetFormatSelection();
            exportFormatPicklist.setDisplayText(R.string.select_string);
         }
      }
      else if (exportFormatPicklist.getAdapter().getCount() > 0) {
         exportFormatPicklist.setDisplayText(R.string.select_string); //Set to "Select" if no saved item selected
         if (exportMediumPicklist.getAdapter().getCount() == 0) {
            exportFormatPicklist.setReadOnly(true);
         }
      }
      else {
         resetFormatSelection();
         exportFormatPicklist.setReadOnly(true);
         exportFormatPicklist.setDisplayText(R.string.select_string);
      }
   }

   /**
    * Save the current selection for the format pick list and it will be used later when a user comes back
    * to this fragment (tab).
    *
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
    * Reset the picklist selections
    */
   private void resetPicklistSelections(boolean mediumReadOnly) {
      if (getArguments() != null) {
         Bundle arguments = getArguments();
         arguments.putParcelable(SAVED_MEDIUM, null);
         arguments.putParcelable(SAVED_FORMAT, null);
      }

      if (mediumReadOnly) {
         exportMediumPicklist.setDisplayText(R.string.connect_source_string);
      }
      else {
         exportMediumPicklist.setDisplayText(R.string.select_string);
      }
      exportMediumPicklist.setReadOnly(mediumReadOnly);

      exportFormatPicklist.setDisplayText(R.string.select_string);
      exportFormatPicklist.setReadOnly(true);
   }

   /**
    * Restore user selection for the medium pick list. This works when a user switches back to this fragment.
    * PickList doesn't maintain its state upon fragment (tab) switch.
    */
   private void restoreMediumSelection() {
      if (getArguments() != null && getArguments().getParcelable(SAVED_MEDIUM) != null) {
         boolean found = false;
         SessionExtra savedMedium = getArguments().getParcelable(SAVED_MEDIUM);

         for (int i = 0; i < exportMediumPicklist.getAdapter().getCount(); i++) {
            ObjectPickListItem<SessionExtra> item = (ObjectPickListItem<SessionExtra>) exportMediumPicklist.getAdapter().getItem(i);
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
            resetPicklistSelections(false);
         }
      }
      else if (exportMediumPicklist.getAdapter().getCount() > 0) {
         resetPicklistSelections(false);
      }
   }

   /**
    * Save the current selection for the medium pick list and it will be used later when a user comes back
    * to this fragment (tab).
    * @param selectedMedium current medium device selection
    */
   private void saveMediumSelection(SessionExtra selectedMedium) {
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

   private List<SessionExtra> createMediumVariants(Map<UtilityHelper.MediumVariant, Integer> resourceMap, String basePath) {
      List<SessionExtra> list = new ArrayList<SessionExtra>();

      for (Map.Entry<UtilityHelper.MediumVariant, Integer> entry : resourceMap.entrySet()) {
         UtilityHelper.MediumVariant mediumVariant = entry.getKey();

         if (SessionExtra.USB == mediumVariant.getExtraType()) {
            Integer resId = resourceMap.get(mediumVariant);
            SessionExtra newExtra = new SessionExtra(SessionExtra.USB, getResources().getString(resId), mediumVariant.getValue());
            newExtra.setBasePath(basePath);
            newExtra.setUseInternalFileSystem(useInternalFileSystem);
            list.add(newExtra);
         }
      }

      return list;
   }

   private List<SessionExtra> generateExportExtras() {
      //temporarily always add usb for testing
      logger.debug("generateExportExtras external storage state = {}", Environment.getExternalStorageState());
      useInternalFileSystem = false;

      List<SessionExtra> list = new ArrayList<SessionExtra>();
      String defaultMakeString = getResources().getString(R.string.unknown_vehicle);
      Map<UtilityHelper.MediumVariant, Integer> resourceMap = UtilityHelper.getMediumVariantMap(vipDataHandler.getMakeOfVehicle(defaultMakeString));

      try {
         String fileStorage = UtilityHelper.getSharedPreferenceString(getActivity().getApplicationContext(), UtilityHelper.STORAGE_LOCATION_TYPE);
         String fileStorageLocation = UtilityHelper.getSharedPreferenceString(getActivity().getApplicationContext(), UtilityHelper.STORAGE_LOCATION);

         if (fileStorage != null && UtilityHelper.STORAGE_LOCATION_INTERNAL.equals(fileStorage) && fileStorageLocation != null && !fileStorageLocation.isEmpty()) {
            File storageFolder = new File(fileStorageLocation);
            if (storageFolder.exists() && storageFolder.canRead() && storageFolder.canWrite()) {
               useInternalFileSystem = true;
               list = createMediumVariants(resourceMap, storageFolder.getPath());
               logger.debug("using internal storage = {}", storageFolder);
            }
         }
      }
      catch (Exception e) {
         logger.info("Unable to check if internal flash need to be used.", e);
      }

      if (!resourceMap.isEmpty() && useInternalFileSystem == false && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
         list = createMediumVariants(resourceMap, Environment.getExternalStorageDirectory().getPath());
      }
      return list;
   }

   /**
    * Initialize medium & format pick lists. (setting adapter & listener)
    */
   private void initPickList() {
      exportMediumPicklist.setAdapter(new PickListAdapter(exportMediumPicklist, getActivity().getApplicationContext()));
      exportMediumPicklist.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean b) {
            ObjectPickListItem<SessionExtra> item = (ObjectPickListItem<SessionExtra>) exportMediumPicklist.findItemById(id);

            if (item != null) {
               saveMediumSelection(item.getObject());
            }
            forceMediumDeviceFormat();
            updateExportButton();
         }

         @Override
         public void onNothingSelected(AdapterView<?> parent) {
            resetPicklistSelections(exportMediumPicklist.getAdapter().getCount() > 0);
            updateExportButton();
         }
      });

      exportFormatPicklist.setAdapter(new PickListAdapter(exportFormatPicklist, getActivity().getApplicationContext()));
      exportFormatPicklist.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean b) {
            String selectedFormat = (id != -1 ? exportFormatPicklist.findItemById(id).getValue() : null);
            saveFormatSelection(selectedFormat);
            logger.trace("setFormat {}", selectedFormat);
            if (getTreeAdapter() != null) {
               getTreeAdapter().updateViewSelection(treeViewList);
            }
            updateExportButton();
            treeViewList.invalidate();
         }

         @Override
         public void onNothingSelected(AdapterView<?> adapterView) {
            resetFormatSelection();
            exportFormatPicklist.setDisplayText(R.string.select_string);
            if (exportMediumPicklist.getAdapter().getCount() == 0) {
               exportFormatPicklist.setReadOnly(true);
            }
         }
      });
   }

   private void populateFormatPickList() {
      exportFormatPicklist.setAdapter(new PickListAdapter(exportFormatPicklist, getActivity().getApplicationContext()));
      int formatId = 0;
      for (String format : formatManager.getFormats()) {
         logger.debug("Format: {}", format);
         exportFormatPicklist.addItem(new PickListItem(formatId++, format));
      }

      restoreFormatSelection();
   }

   private void populateExportToPickList() {
      if (exportMediumPicklist.getAdapter().getCount() > 0) {
         exportMediumPicklist.clearList();
      }

      List<SessionExtra> extras = generateExportExtras();
      if (!extras.isEmpty()) {
         for (SessionExtra extra : extras) {
            exportMediumPicklist.addItem(new ObjectPickListItem<SessionExtra>(extra.getOrder(), extra.getDescription(), extra));
         }
         exportMediumPicklist.sortAdapterData(new IDPicklistComparator()); //Sort based on the devices ID value
         restoreMediumSelection();
      }
      else {
         if (exportMediumPicklist.getAdapter().getCount() > 0) { //There are no medium devices, but picklist is still populated
            exportMediumPicklist.getAdapter().clear();
         }
         resetPicklistSelections(true);
      }
   }

   /**
    * While Medium Device is set to CLOUD, force to CNH format and disable user selection of format.
    * While Medium Device is set to Fred/USB or DesktopSW/USB, force to ISOXML and disable user selection of format.
   */
   private void forceMediumDeviceFormat() {
      if (exportMediumPicklist.getSelectedItemPosition() > NEGATIVE_BINARY_ERROR) {
         Boolean isReadOnly = false;
         SessionExtra extra = ((ObjectPickListItem<SessionExtra>) exportMediumPicklist.getSelectedItem()).getObject();
         UtilityHelper.MediumVariant mediumVariant = UtilityHelper.MediumVariant.fromValue(extra.getOrder());
         if (null != mediumVariant) {
            if (UtilityHelper.MediumVariant.CLOUD_OUTBOX.equals(mediumVariant)) { //Cloud export is only done in PF Database format
               isReadOnly = true;
               // "PF Database" is format type in formats.xml file
               exportFormatPicklist.setSelectionByPosition(exportFormatPicklist.findItemPositionByValue(PFDATABASE_FORMAT, true));
               saveFormatSelection(PFDATABASE_FORMAT);
               logger.debug("Force format to PF Database");
            }
            else if (UtilityHelper.MediumVariant.USB_DESKTOP_SW.equals(mediumVariant) || UtilityHelper.MediumVariant.USB_FRED.equals(mediumVariant)) {
               isReadOnly = true;
               // "ISOXML" is format type in formats.xml file
               exportFormatPicklist.setSelectionByPosition(exportFormatPicklist.findItemPositionByValue(ISOXML_FORMAT, true));
               saveFormatSelection(ISOXML_FORMAT);
               logger.debug("Force format to ISOXML");
            }
            else if (UtilityHelper.MediumVariant.USB_PHOENIX.equals(mediumVariant) || UtilityHelper.MediumVariant.USB_HAWK.equals(mediumVariant)) {
               exportFormatPicklist.setSelectionByPosition(exportFormatPicklist.findItemPositionByValue(PFDATABASE_FORMAT, true));
               saveFormatSelection(PFDATABASE_FORMAT);
               logger.debug("Force format to PF Database as default");
            }
         }
         exportFormatPicklist.setReadOnly(isReadOnly);
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
   public void onPCMDisconnected() {
      logger.trace("PCM is not online.");
      hideTreeList();
      showDisconnectedOverlay();
      updateSelectAllState();
      showDragAndDropZone();
      setHeaderText("");
   }

   @Override
   public void onPCMConnected() {
      logger.trace("PCM is online.");
      showLoadingOverlay();
      onResumeSession();
   }

   @Override
   public void onResumeSession() {
      final Session session = getSession();
      logger.debug("onResumeSession(): {}", session.getType());
      //verify that no other session is blocking fragment
      if (!requestAndUpdateBlockedOverlay(getBlockingActions())) {
         if (SessionUtil.isInProgress(session) && SessionUtil.isPerformOperationsTask(session)) {
            logger.debug("onResumeSession(): Still processing EXPORT. Show progress panel.");
            processOverlay.setMode(DataExchangeProcessOverlay.MODE.EXPORT_PROCESS);
            setExportPicklistsReadOnly(true);
            showProgressPanel();
            updateProgressbar(progressValue);

            initAndPouplateTree(session.getObjectData());
            hideDisabledOverlay();
            showTreeList();
            updateSelectAllState();
         }
         else {
            hideTreeList();
            showLoadingOverlay();
            discovery();
         }
      }
   }

   @Override
   public void onSessionCancelled(Session session) {
      logger.debug("onSessionCancelled(): {}, {}", session.getType(), session.getAction());
      if (SessionUtil.isPerformOperationsTask(session) || SessionUtil.isDiscoveryTask(session)) {
         ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.export_cancel), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
               getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
         clearTreeSelection();
      }

      updateExportButton();
      processOverlay.setMode(DataExchangeProcessOverlay.MODE.HIDDEN);
      setExportPicklistsReadOnly(false);
      showDragAndDropZone();
      updateSelectAllState();
   }

   @Override
   public void onMyselfSessionSuccess(Session session) {
      if (session == null) {
         showDragAndDropZone();
         return;
      }

      logger.debug("onMyselfSessionSuccess(): {}", session.getType());
      if (SessionUtil.isDiscoveryTask(session)) {
         initAndPouplateTree(session.getObjectData());

         hideDisabledOverlay();
         showTreeList();
         updateSelectAllState();
      }
      else if (SessionUtil.isPerformOperationsTask(session)) {
         logger.trace("Resetting new session.  Operation completed.");
         getTreeAdapter().selectAll(treeViewList, false);
         if (session.getResultCode() != null) {
            processOverlay.setMode(DataExchangeProcessOverlay.MODE.HIDDEN);
            setExportPicklistsReadOnly(false);
            if (SessionUtil.isSuccessful(session)) {
               showFinishedStatePanel(true);
               progressValue = ProgressValue.initProgress();
               ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.export_complete), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                     getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();

               // Reset session data after completing PERFORM_OPERATIONS successfully.
               resetSession();
            }
            else {
               showDragAndDropZone();
            }
         }
         else {
            showProgressPanel();
         }
      }
      updateExportButton();
      updateSelectAllState();
   }

   @Override
   public void onMyselfSessionError(Session session, ErrorCode errorCode) {
      logger.debug("onMyselfSessionError(): {}", session.getType());
      if (SessionUtil.isPerformOperationsTask(session)) {
         ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.export_cancel), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
               getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
         showDragAndDropZone();
         clearTreeSelection();
         updateExportButton();
         processOverlay.setMode(DataExchangeProcessOverlay.MODE.HIDDEN);
         setExportPicklistsReadOnly(false);
         updateSelectAllState();
      }
   }

   @Override
   public void onTreeItemSelected() {
      super.onTreeItemSelected();
      updateExportButton();
   }

   private void updateProgressbar(ProgressValue progressVal) {
      final Double percent = ((progressVal.getCurrentValue() * 1.0) / progressVal.getMaxValue()) * 100;
      progressBar.setProgress(percent.intValue());
      progressBar.setSecondText(true, loading_string, String.format(x_of_y_format, progressVal.getCurrentValue(), progressVal.getMaxValue()), true);
   }

   @Override
   public void onProgressUpdate(String operation, int progress, int max) {
      logger.debug("onProgressUpdate: {}, {}", progress, max);
      if (progressValue == null) {
         progressValue = ProgressValue.initProgress();
      }
      progressValue.setCurrentValue(progress);
      progressValue.setMaxValue(max);
      updateProgressbar(progressValue);
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      if (exportFormatPicklist.getSelectedItemPosition() > NEGATIVE_BINARY_ERROR) {
         String format = exportFormatPicklist.getSelectedItemValue();
         return formatManager.formatSupportsType(format, node.getType());
      }
      return false;
   }

   /**
    * Set export picklists as enabled or disabled.
    * @param readOnly True if picklists are to be set to readonly, false otherwise
    */
   private void setExportPicklistsReadOnly(boolean readOnly) {
      if (exportMediumPicklist.getAdapter().getCount() > 0) {
         exportMediumPicklist.setReadOnly(readOnly);

         if (exportMediumPicklist.getSelectedItem() != null) {
            //Only change state of Format Picklist if it isn't one of the three data sources below
            SessionExtra extra = ((ObjectPickListItem<SessionExtra>) exportMediumPicklist.getSelectedItem()).getObject();
            UtilityHelper.MediumVariant mediumVariant = UtilityHelper.MediumVariant.fromValue(extra.getOrder());
            if (mediumVariant.equals(UtilityHelper.MediumVariant.CLOUD_OUTBOX) || mediumVariant.equals(UtilityHelper.MediumVariant.USB_DESKTOP_SW)
                  || mediumVariant.equals(UtilityHelper.MediumVariant.USB_FRED)) {
               exportFormatPicklist.setReadOnly(true);
            }
            else {
               exportFormatPicklist.setReadOnly(readOnly);
            }
         }
         else {
            exportFormatPicklist.setReadOnly(true);
         }
      }
      else { //Reset both picklists and set them to read only (likely bad USB removal)
         exportMediumPicklist.setDisplayText(R.string.connect_source_string);
         exportMediumPicklist.setReadOnly(true);
         exportFormatPicklist.setDisplayText(R.string.select_string);
         exportFormatPicklist.setReadOnly(true);
      }
   }

   @Override
   public void onMediumUpdate() {
      super.onMediumUpdate();

      logger.trace("onMediumUpdate()");
      populateExportToPickList();
      updateExportButton();
   }

   /**
    * Run EXPORT.
    */
   private void runExport() {
      logger.trace("runExport()");
      progressValue = ProgressValue.initProgress();
      List<ObjectGraph> selected = new ArrayList<ObjectGraph>(getTreeAdapter().getSelected());
      ArrayList<ObjectGraph> ddopsSelect = new ArrayList<ObjectGraph>(getTreeAdapter().getData());
      for (ObjectGraph object : ddopsSelect) {
         if (object.getType().equals(DataTypes.DDOP)) {
            selected.add(object);
         }
      }

      if (!selected.isEmpty()) {
         final String tempPath = UtilityHelper.CommonPaths.PATH_TMP.getPathString();
         File tmpFolder = new File(tempPath);

         if (!tmpFolder.exists()) {
            logger.info("creating temporary folder:{}", tmpFolder.getPath());

            // The DM service will notify error with the fault alert if the temp dir isn't created.
            tmpFolder.mkdirs();
            tmpFolder.setReadable(true, false);
            tmpFolder.setExecutable(true, false);
            tmpFolder.setWritable(true, false);
         }

         ObjectPickListItem<SessionExtra> item = (ObjectPickListItem<SessionExtra>) exportMediumPicklist.getSelectedItem();
         SessionExtra extra = new SessionExtra(item.getObject());
         String format = exportFormatPicklist.getSelectedItemValue();
         String path = new File(extra.getBasePath(), formatManager.getFormat(format).path).getPath();

         extra.setFormat(format);
         // Based on PickList selections, set appropriate location type and file path.
         //TODO: Implement path resolver to generate base path given USB variation and cloud
         extra.setPath(path);

         List<Operation> operations = new ArrayList<Operation>();
         for (ObjectGraph obj : selected) {
            operations.add(new Operation(obj, null));
         }
         showProgressPanel();
         processOverlay.setMode(DataExchangeProcessOverlay.MODE.EXPORT_PROCESS);
         setExportPicklistsReadOnly(true);

         performOperations(extra, operations);
         updateExportButton();
         updateSelectAllState();
      }
      else {
         ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.no_data_of_format_string), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
               getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
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
      progressBar.setErrorProgress(progressBar.getProgress(), getResources().getString(R.string.pb_error));
      progressBar.setSecondText(true, loading_string, null, true);
      progressBar.setProgress(0); //resets error if set
      //set visibility of sections
      exportFinishedStatePanel.setVisibility(View.GONE);
      exportDropZone.setVisibility(View.GONE);
      leftStatusPanel.setVisibility(View.VISIBLE);
   }

   private void updateExportButton() {
      Session s = getSession();
      boolean isActiveOperation = SessionUtil.isPerformOperationsTask(s) && s.getResultCode() == null;
      boolean useDefaultText = true;

      if (getTreeAdapter() != null && getTreeAdapter().getSelectionMap() != null) {
         int selectedItemCount = countSelectedItem();
         if (selectedItemCount > 0) {
            useDefaultText = false;
            Resources resources = getResources();
            exportSelectedBtn.setText(resources.getString(R.string.export_selected) + " (" + selectedItemCount + ")");
            exportSelectedBtn.setTextSize(selectedItemCount > MAX_TREE_SELECTIONS_FOR_DEFAULT_TEXT_SIZE
                  ? resources.getDimension(R.dimen.button_default_text_size) - resources.getDimension(R.dimen.decrease_text_size)
                  : resources.getDimension(R.dimen.button_default_text_size));
         }
      }

      if (useDefaultText) {
         exportSelectedBtn.setText(getResources().getString(R.string.export_selected));
         float defaultButtonSize = getResources().getDimension(R.dimen.button_default_text_size);
         if (exportSelectedBtn.getTextSize() < defaultButtonSize) exportSelectedBtn.setTextSize(defaultButtonSize);
      }

      boolean hasSelection = (getTreeAdapter() != null && s != null && exportMediumPicklist.getSelectedItemPosition() > NEGATIVE_BINARY_ERROR
            && exportFormatPicklist.getSelectedItemPosition() > NEGATIVE_BINARY_ERROR && getTreeAdapter().hasSelection());

      if (hasSelection && !isActiveOperation) {
         exportSelectedBtn.setEnabled(true);
         exportDropZone.setBackgroundResource(R.drawable.dashed_border_selected);
      }
      else {
         exportSelectedBtn.setEnabled(false);
         exportDropZone.setBackgroundResource(R.drawable.dashed_border_initial);
      }

   }

   @Override
   public Session.Action getAction() {
      return Session.Action.EXPORT;
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
