/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.pf.widget.controls.PickListItemWithTag;
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.android.pf.widget.utilities.VarietyHelper;
import com.cnh.android.pf.widget.utilities.commands.SaveVarietyCommand;
import com.cnh.android.pf.widget.utilities.commands.VarietyCommandParams;
import com.cnh.android.pf.widget.utilities.listeners.GenericListener;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.widget.Widget;
import com.cnh.android.widget.control.InputField;
import com.cnh.android.widget.control.PickList;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.pf.android.data.management.productlibrary.adapter.VarietyAdapter;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.CropTypeComparator;
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.configuration.VarietyColor;
import com.cnh.pf.model.product.library.CropType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Dialog for adding or editing {@link com.cnh.pf.model.product.configuration.Variety}s
 *
 * @author waldschmidt
 */
public class AddOrEditVarietyDialog extends DialogView {
   public enum VarietyDialogActionType {
      ADD, EDIT
   }

   private static final Logger logger = LoggerFactory.getLogger(VarietyAdapter.class);
   private List<Variety> varieties;
   private Variety currentVariety;
   private Variety modifiedVariety;
   private VarietyDialogActionType actionType = VarietyDialogActionType.ADD;
   private InputField inputField;
   private PickList pickList;
   private IVIPServiceAIDL vipService;
   private List<PickListItem> cropTypePickListItems;
   private List<VarietyColor> colors;
   private GridView colorGrid;
   private DisabledOverlay disabledOverlay;
   private ColorGridAdapter colorGridAdapter;
   private boolean colorChangedByUser = false; // set to true if color change minimum once by the user

   public AddOrEditVarietyDialog(Context context) {
      super(context);
      this.showThirdButton(false).setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT)
            .setBodyView(R.layout.variety_add_or_edit_dialog)
            .setDialogWidth(ProductLibraryFragment.DIALOG_WIDTH);
      Resources res = context.getResources();
      this.setContentPaddings(
            res.getDimensionPixelSize(R.dimen.variety_dialog_content_padding_left_and_right),
            res.getDimensionPixelSize(R.dimen.variety_dialog_content_padding_top),
            res.getDimensionPixelSize(R.dimen.variety_dialog_content_padding_left_and_right),
            res.getDimensionPixelSize(R.dimen.variety_dialog_content_padding_bottom)
      );
   }

   /**
    * Setter for this VarietyDialog
    * @param actionType the VarietyDialogActionType
    */
   public void setActionType(VarietyDialogActionType actionType) {
      this.actionType = actionType;
   }

   /**
    * Setter for the varietyList. Copies the list for later thread safety. If the list given needs synchronization
    * the call to this method needs synchronization too.
    * @param varieties the varieties list
    */
   public void setVarietyList(List<Variety> varieties) {
      this.varieties = new ArrayList<Variety>(varieties);
   }

   /**
    * Setter for the vipService.
    * @param vipService the vipService
    */
   public void setVIPService(final IVIPServiceAIDL vipService) {
      if (vipService != null) {
         if (colors == null || colors.isEmpty()) {
            new AsyncTask<Void, Void, List<VarietyColor>>() {

               @Override
               protected void onPreExecute() {
                  disabledOverlay.setMode(DisabledOverlay.MODE.LOADING);
                  super.onPreExecute();
               }

               @Override
               protected List<VarietyColor> doInBackground(Void... voids) {
                  List<VarietyColor> tempColors = null;
                  try {
                     tempColors = vipService.getVarietyColorList();
                     logger.debug("got colors from vipService {}", tempColors);
                  }
                  catch (RemoteException e) {
                     logger.error("error when trying to get varietyColorList", e);
                  }
                  return tempColors;
               }

               @Override
               protected void onPostExecute(List<VarietyColor> tempColors) {
                  colors = tempColors;
                  fillColorGrid();
               }
            }.execute();
         }
      }
      this.vipService = vipService;
   }

   /**
    * Sets the variety to edit (only needed for edit action).
    * @param variety the current variety
    */
   public void setCurrentVariety(Variety variety) {
      this.currentVariety = new Variety(variety);
      this.modifiedVariety = new Variety(variety);
   }

   @Override
   protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      init();
   }

   private void init() {
      initOverlay();
      initInputField();
      initPickList();
      initButtons();
      initColorPicker();
      fillColorGrid();
   }

   private void initOverlay() {
      disabledOverlay = (DisabledOverlay) findViewById(R.id.variety_dialog_disabled_overlay);
      disabledOverlay.setMode(DisabledOverlay.MODE.DISCONNECTED);
   }

   private void initButtons() {
      updateSaveButtonState();
      mButtonFirst.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            saveVariety(modifiedVariety, vipService);
            dismiss();
         }
      });
   }

   private void initPickList() {
      if (pickList == null) {
         pickList = (PickList) findViewById(R.id.variety_dialog_crop_type_pick_list);
      }
      PickListAdapter pickListAdapter = new PickListAdapter(pickList, getContext());
      Integer selectedId = null;
      if (cropTypePickListItems == null) {
         CropType[] cropTypes = CropType.values();
         Arrays.sort(cropTypes, new CropTypeComparator(getContext()));
         cropTypePickListItems = new ArrayList<PickListItem>(cropTypes.length);
         CropType cropType;
         for (int i = 0; i < cropTypes.length; ++i) {
            cropType = cropTypes[i];
            cropTypePickListItems
                  .add(new PickListItemWithTag<CropType>(i, EnumValueToUiStringUtility.getUiStringForCropType(cropType, getContext()), true, false, false, false, false, cropType));
            if (actionType.equals(VarietyDialogActionType.EDIT) && (null != modifiedVariety) && (null != modifiedVariety.getCropType()) ) {
               if (modifiedVariety.getCropType().equals(cropType)) {
                  selectedId = i;
               }
               if(currentVariety.isUsed()) {
                  pickList.setReadOnly(true);
               }
            }
         }
      }
      pickListAdapter.addAll(cropTypePickListItems);
      pickList.setAdapter(pickListAdapter);
      PickList.OnItemSelectedListener onItemSelectedListener = new CropTypeSelectedListener();
      pickList.setOnItemSelectedListener(onItemSelectedListener);
      if (actionType.equals(VarietyDialogActionType.EDIT) && modifiedVariety != null && selectedId != null) {
         pickList.setSelectionById(selectedId);
      }
   }

   private void initInputField() {
      if (inputField == null) {
         inputField = (InputField) findViewById(R.id.variety_dialog_name_input_field);
      }
      Variety variety = modifiedVariety;
      if (actionType.equals(VarietyDialogActionType.EDIT) && variety != null) {
         inputField.setText(variety.getName());
      }
      inputField.addTextChangedListener(new TextWatcher() {
         @Override
         public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

         }

         @Override
         public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

         }

         @Override
         public void afterTextChanged(Editable editable) {
            if (modifiedVariety == null) {
               modifiedVariety = new Variety();
            }
            modifiedVariety.setName(editable.toString().trim());
            AddOrEditVarietyDialog.this.updateSaveButtonState();
         }
      });
   }

   /**
    * Inits the color picker/color grid but is doing this only for things where not data from pcm is needed.
    */
   private void initColorPicker() {
      colorGrid = (GridView) findViewById(R.id.variety_color_picker_grid);
   }

   /**
    * Fills the color grid with colors and sets up the adapter for the color grid.
    *
    * Color grid initialization is split into init and fill because most of the the data needs to be loaded when init is called.
    */
   private void fillColorGrid() {
      logger.debug("fill color grid: {} with colors: {}", colorGrid, colors);
      if (colorGrid != null && colors != null && colorGrid.getAdapter() == null) {
         colorGridAdapter = new ColorGridAdapter();
         colorGrid.setAdapter(colorGridAdapter);
         colorGrid.setOnItemClickListener(new OnColorClickListener());
         if (modifiedVariety != null){
            switch (actionType){
               case EDIT:
                  VarietyColor currentColor = modifiedVariety.getVarietyColor();
                  colorGridAdapter.setSelectedPosition(colorGridAdapter.getPositionOfColor(currentColor));
                  updateSaveButtonState();
                  break;
               case ADD:
                  VarietyColor nextColor = com.cnh.pf.util.VarietyHelper.retrieveNextUnusedVarietyColor(new ArrayList<Variety>(varieties), new LinkedList<VarietyColor>(colors));
                  modifiedVariety.setVarietyColor(nextColor);
                  colorGridAdapter.setSelectedPosition(colorGridAdapter.getPositionOfColor(nextColor));
                  updateSaveButtonState();
                  break;
               default:
                  logger.error("unimplemented state of variety dialog: {}", actionType);
                  break;
            }
         }
         disabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
      }
   }

   /**
    * Deletes the variety
    * @param variety Variety to delete
    * @param vipService the vipService
    */
   private static void saveVariety(Variety variety, IVIPServiceAIDL vipService) {
      logger.debug("on save button clicked in variety dialog: {}", variety.getName());
      VarietyCommandParams params = new VarietyCommandParams(vipService, variety);
      new VIPAsyncTask<VarietyCommandParams, Variety>(params, new GenericListener<Variety>() {
         @Override
         public void handleEvent(Variety variety) {
         }
      }).execute(new SaveVarietyCommand());
   }

   /**
    * Checks if saving is allowed and enables/disables the save button.
    */
   private void updateSaveButtonState() {
      if (modifiedVariety == null) {
         modifiedVariety = new Variety();
         setFirstButtonEnabled(false);
         return;
      }
      if (modifiedVariety.getName() == null || modifiedVariety.getName().isEmpty()) {
         setFirstButtonEnabled(false);
         return;
      }
      if (vipService == null) {
         setFirstButtonEnabled(false);
         return;
      }
      String newName = modifiedVariety.getName();
      switch (actionType) {
      case EDIT:
         // check if something has changed
         // check name first, then cropType. Reason: to set the error indicator always to the right value.
         for (Variety variety : varieties) {
            if (variety.getId() != modifiedVariety.getId() && variety.getName().equals(newName)) {
               // name already used by other variety
               setFirstButtonEnabled(false);
               inputField.setErrorIndicator(Widget.ErrorIndicator.NEEDS_CHECKING);
               return;
            }
         }
         inputField.setErrorIndicator(Widget.ErrorIndicator.NONE);
         // Logic: CropType and Color have to have been set, to save,
         //   or  you have to have changed something.  (Also can not check for equality if null)
         if ((null == modifiedVariety.getCropType()) || (null == modifiedVariety.getVarietyColor())
                 || (currentVariety.getName().equals(newName)
                 && modifiedVariety.getCropType().equals(currentVariety.getCropType())
                 && (currentVariety.getVarietyColor().getId() == modifiedVariety.getVarietyColor().getId()))) {
            setFirstButtonEnabled(false);
            return;
         }
         break;
      case ADD:
         /// check name first, then cropType. Reason: to set the error indicator always to the right value.
         for (Variety variety : varieties) {
            if (variety.getName().equals(newName)) {
               // name already used by other variety
               setFirstButtonEnabled(false);
               inputField.setErrorIndicator(Widget.ErrorIndicator.NEEDS_CHECKING);
               return;
            }
         }
         inputField.setErrorIndicator(Widget.ErrorIndicator.NONE);
         if (modifiedVariety.getCropType() == null || modifiedVariety.getVarietyColor() == null) {
            setFirstButtonEnabled(false);
            return;
         }
         break;
      default:
         setFirstButtonEnabled(false);
         inputField.setErrorIndicator(Widget.ErrorIndicator.INVALID);
         logger.error("unimplemented state of variety dialog: {}", actionType);
         return;
      }
      setFirstButtonEnabled(true);
   }

   /**
    * Lister for the pick list which implements crop type selection.
    */
   private final class CropTypeSelectedListener implements PickList.OnItemSelectedListener {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean fromUser) {
         logger.debug("selected item view: {}, position: {}, id: {}, fromUser: {}", view, position, id, fromUser);
         if (position < parent.getCount() && position > -1 && modifiedVariety != null) {
            CropType cropType = (CropType) ((PickListItemWithTag) parent.getItemAtPosition(position)).getTag();
            if (!cropType.equals(modifiedVariety.getCropType())) {
               modifiedVariety.setCropType(cropType);
               if (!colorChangedByUser) {
                  VarietyColor varietyColor = modifiedVariety.getVarietyColor();
                  VarietyColor newVarietyColor = com.cnh.pf.util.VarietyHelper.retrieveNextUnusedVarietyColor(new ArrayList<Variety>(varieties), new LinkedList<VarietyColor>(colors), cropType);
                  // update only if necessary
                  if (!newVarietyColor.equals(varietyColor)) {
                     modifiedVariety.setVarietyColor(newVarietyColor);
                     colorGridAdapter.setSelectedPosition(colorGridAdapter.getPositionOfColor(newVarietyColor));
                  }
               }
               updateSaveButtonState();
            }
         }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
   }

   /**
    * Adapter implementation for the GridView used to implement color picker functionality.
    */
   private final class ColorGridAdapter extends BaseAdapter {

      private int selectedPosition = -1;

      private void setSelectedPosition(int position){
         this.selectedPosition = position;
         logger.debug("this.selectedPosition: {}", selectedPosition);
         notifyDataSetChanged();
      }

      /**
       * Retrieves the position of the varietyColor in the colors list.
       * @param varietyColor the varietyColor
       * @return the position of the varietyColor in the colors list
       */
      int getPositionOfColor(VarietyColor varietyColor) {
         return colors.indexOf(varietyColor);
      }

      @Override
      public int getCount() {
         return colors.size();
      }

      @Override
      public Object getItem(int position) {
         return colors.get(position);
      }

      @Override
      public long getItemId(int position) {
         return position;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         logger.debug("getView - position; {}, convertView: {}, parent: {}", position, convertView, parent);
         if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.variety_color_picker_image_view, null);
         }
         ImageView imageView = (ImageView) convertView;
         if (position == selectedPosition){
            imageView.setImageResource(R.drawable.varieties_shape_with_border);
         }
         else {
            imageView.setImageResource(R.drawable.varieties_shape);
         }
         VarietyColor varietyColor = colors.get(position);
         GradientDrawable gradientDrawable = (GradientDrawable) imageView.getDrawable();
         gradientDrawable.setColor(VarietyHelper.retrieveToIntConvertedColor(varietyColor));
         convertView.setTag(varietyColor);
         logger.debug("position: {}, color: {}", position, varietyColor);
         return convertView;
      }
   }

   /**
    * ClickListener for the color picker.
    */
   private final class OnColorClickListener implements AdapterView.OnItemClickListener {

      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
         Object tag = view.getTag();
         logger.debug("onColorClicked - view tag: {}", tag);
         colorChangedByUser = true;
         modifiedVariety.setVarietyColor((VarietyColor) tag);
         colorGridAdapter.setSelectedPosition(position);
         updateSaveButtonState();
      }
   }
}
