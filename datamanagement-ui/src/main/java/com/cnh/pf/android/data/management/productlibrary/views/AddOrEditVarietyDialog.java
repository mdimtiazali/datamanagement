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
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.pf.widget.controls.PickListItemWithTag;
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.android.pf.widget.utilities.commands.SaveVarietyCommand;
import com.cnh.android.pf.widget.utilities.commands.VarietyCommandParams;
import com.cnh.android.pf.widget.utilities.listeners.GenericListener;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.widget.Widget;
import com.cnh.android.widget.control.InputField;
import com.cnh.android.widget.control.PickList;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.adapter.VarietyAdapter;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.CropTypeComparator;
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.configuration.VarietyColor;
import com.cnh.pf.model.product.library.CropType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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

   public AddOrEditVarietyDialog(Context context) {
      super(context);
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
   public void setVarietyList(List<Variety> varieties){
      this.varieties = new ArrayList<Variety>(varieties);
   }

   /**
    * Setter for the vipService.
    * @param vipService the vipService
    */
   public void setVIPService(final IVIPServiceAIDL vipService){
      if (vipService != null){
         if (colors == null || colors.isEmpty()) {
            new AsyncTask<Void, Void, Void>() {

               @Override
               protected Void doInBackground(Void... voids) {
                  try {
                     colors = vipService.getVarietyColorList();
                     logger.debug("got colors from vipService {}", colors);
                  } catch (RemoteException e) {
                     logger.error("error when trying to get varietyColorList", e);
                  }
                  return null;
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
   public void setCurrentVariety(Variety variety){
      this.currentVariety = new Variety(variety);
      this.modifiedVariety = new Variety(variety);
   }

   @Override
   protected void onAttachedToWindow() {
      init();
      super.onAttachedToWindow();
   }

   private void init(){
      initInputField();
      initPickList();
      initButtons();
   }

   private void initButtons(){
      updateSaveButtonState();
      mButtonFirst.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            // TODO: setting the color is done only to show that communication works - the dummy value needs to be replaced later
            // during https://polarion.cnhind.com/polarion/#/project/pfhmi-development2/workitem?id=pfhmi-development2-4639
            // added try/catch because this dummy value handling can cause a crash - I remove it later during the named ticket.
            try {
               if (modifiedVariety.getVarietyColor() == null) {
                  modifiedVariety.setVarietyColor(colors.get(10));
                  saveVariety(modifiedVariety, vipService);
               }
            } catch (Exception e){
               logger.error("unfinished implementation with dummy values - here is something left to do", e);
            }
            dismiss();
         }
      });
   }

   private void initPickList(){
      if (pickList == null){
         pickList = (PickList) findViewById(R.id.variety_dialog_crop_type_pick_list);
      }
      PickListAdapter pickListAdapter = new PickListAdapter(pickList, getContext());
      Integer selectedId = null;
      if (cropTypePickListItems == null) {
         CropType[] cropTypes = CropType.values();
         Arrays.sort(cropTypes, new CropTypeComparator(getContext()));
         cropTypePickListItems = new ArrayList<PickListItem>(cropTypes.length);
         CropType cropType;
         for(int i = 0; i < cropTypes.length; ++i){
            cropType = cropTypes[i];
            cropTypePickListItems.add(new PickListItemWithTag<CropType>(
                  i,
                  EnumValueToUiStringUtility.getUiStringForCropType(cropType, getContext()),
                  true, false, false, false, false, cropType)
            );
            if (actionType.equals(VarietyDialogActionType.EDIT) && modifiedVariety != null){
               if (modifiedVariety.getCropType().equals(cropType)) {
                  selectedId = i;
               }
            }
         }
      }
      pickListAdapter.addAll(cropTypePickListItems);
      pickList.setAdapter(pickListAdapter);
      PickList.OnItemSelectedListener onItemSelectedListener = new OnItemSelectedListener();
      pickList.setOnItemSelectedListener(onItemSelectedListener);
      if (actionType.equals(VarietyDialogActionType.EDIT) && modifiedVariety != null && selectedId != null){
         pickList.setSelectionById(selectedId);
      }
   }

   private void initInputField(){
      if (inputField == null){
         inputField = (InputField) findViewById(R.id.variety_dialog_name_input_field);
      }
      Variety variety = modifiedVariety;
      if (actionType.equals(VarietyDialogActionType.EDIT) && variety != null){
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
            if (modifiedVariety == null){
               modifiedVariety = new Variety();
            }
            modifiedVariety.setName(editable.toString().trim());
            AddOrEditVarietyDialog.this.updateSaveButtonState();
         }
      });
   }

   /**
    * Deletes the variety
    * @param variety Variety to delete
    * @param vipService the vipService
    */
   private static void saveVariety(Variety variety, IVIPServiceAIDL vipService){
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
   private void updateSaveButtonState(){
      if (modifiedVariety == null){
         modifiedVariety = new Variety();
         setFirstButtonEnabled(false);
         return;
      }
      if (modifiedVariety.getName() == null || modifiedVariety.getName().isEmpty()) {
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
         if (currentVariety.getName().equals(newName) && modifiedVariety.getCropType().equals(currentVariety.getCropType())) {
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
         if (modifiedVariety.getCropType() == null) {
            setFirstButtonEnabled(false);
            return;
         }
         break;
      default:
         setFirstButtonEnabled(false);
         inputField.setErrorIndicator(Widget.ErrorIndicator.INVALID);
         return;
      }
      setFirstButtonEnabled(true);
   }

   private class OnItemSelectedListener implements PickList.OnItemSelectedListener {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean fromUser) {
         logger.debug("selected item view: {}, position: {}, id: {}, fromUser: {}", view, position, id, fromUser);
         if (position < parent.getCount() && position > -1) {
            if (modifiedVariety != null){
               modifiedVariety.setCropType((CropType) ((PickListItemWithTag)parent.getItemAtPosition(position)).getTag());
               updateSaveButtonState();
            }
         }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
   }
}
