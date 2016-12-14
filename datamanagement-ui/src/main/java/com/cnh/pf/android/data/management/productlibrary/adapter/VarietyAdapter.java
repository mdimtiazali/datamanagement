/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.dialog.TextDialogView;
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.android.pf.widget.utilities.commands.DeleteVarietyCommand;
import com.cnh.android.pf.widget.utilities.commands.VarietyCommandParams;
import com.cnh.android.pf.widget.utilities.listeners.GenericListener;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.pf.android.data.management.productlibrary.utility.UiHelper;
import com.cnh.pf.android.data.management.productlibrary.views.AddOrEditVarietyDialog;
import com.cnh.pf.model.product.configuration.Variety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Adapter for Variety-ListView.
 *
 * Remark: Some parts of this implementation are based on the implementation of
 * Androids ArrayAdapter.
 */
public final class VarietyAdapter extends BaseAdapter implements Filterable {

   private static final Logger logger = LoggerFactory.getLogger(VarietyAdapter.class);
   private List<Variety> filteredList; // in ArrayAdapter called mObjects
   // This list is only needed if the list is getting filtered. It will be filled before filtering is used the first time.
   private List<Variety> originalList; // in ArrayAdapter called mOriginalValues
   private Filter filter;
   // lock for filteredList and originalList
   private final Object listsLock = new Object();
   private final Context context;
   private final OnEditButtonClickListener onEditButtonClickListener;
   private final OnDeleteButtonClickListener onDeleteButtonClickListener;
   private final TabActivity activity;
   private IVIPServiceAIDL vipService;
   private AddOrEditVarietyDialog editVarietyDialog;

   /**
    * @param context the context of the adapter
    * @param varieties the list of varieties which should be shown.
    * @param tabActivity the tabActivity
    * @param vipService the vipService
    */
   public VarietyAdapter(final Context context, final List<Variety> varieties, final TabActivity tabActivity, final IVIPServiceAIDL vipService) {
      this.context = context;
      // don't allow modifications from outside.
      // as long as the filtered list is "unfiltered", it's the list we get via parameter
      this.filteredList = new ArrayList<Variety>(varieties);
      onEditButtonClickListener = new OnEditButtonClickListener();
      onDeleteButtonClickListener = new OnDeleteButtonClickListener();
      this.activity = tabActivity;
      this.vipService = vipService;
   }

   /**
    * Setter for the vipService.
    * @param vipService the vipService
    */
   public void setVIPService(final IVIPServiceAIDL vipService){
      this.vipService = vipService;
      editVarietyDialog.setVIPService(vipService);
   }

   /**
    * Setter for the variety list.
    *
    * @param varieties
    */
   public void setVarietyList(final List<Variety> varieties){
      synchronized (listsLock){
         filteredList = new ArrayList<Variety>(varieties);
         originalList = null;
      }
      notifyDataSetChanged();
   }

   /**
    * Removes a variety.
    * @param variety the variety to remove
    */
   public void removeVariety(Variety variety){
      synchronized (listsLock){
         if (filteredList != null) {
            filteredList.remove(variety);
         }
         if (originalList != null) {
            originalList.remove(variety);
         }
      }
      notifyDataSetChanged();
   }

   @Override
   public int getCount() {
      return filteredList.size();
   }

   @Override
   public Object getItem(int i) {
      return filteredList.get(i);
   }

   @Override
   public long getItemId(int i) {
      return i;
   }

   @Override
   public View getView(int position, View convertView, ViewGroup parent) {
      final Variety variety = (Variety) getItem(position);
      if (convertView == null) {
         convertView = LayoutInflater.from(context).inflate(R.layout.variety_list_item, parent, false);
      }
      final TextView nameTextView = (TextView) convertView.findViewById(R.id.variety_name_textview);
      nameTextView.setText(variety.getName());
      final TextView cropTypeTextView = (TextView) convertView.findViewById(R.id.variety_crop_type_name);
      cropTypeTextView.setText(getCropTypeText(variety));
      UiHelper.setAlternatingTableItemBackground(context, position, convertView);
      final ImageButton editButton = (ImageButton) convertView.findViewById(R.id.variety_edit_button);
      if (variety.isUsed()){
         editButton.setEnabled(false);
         editButton.setClickable(false);
      } else {
         editButton.setTag(variety);
         editButton.setEnabled(true);
         editButton.setClickable(true);
         editButton.setOnClickListener(onEditButtonClickListener);
      }
      final ImageButton deleteButton = (ImageButton) convertView.findViewById(R.id.variety_delete_button);
      if (variety.isUsed()){
         deleteButton.setEnabled(false);
         deleteButton.setClickable(false);
      } else {
         deleteButton.setTag(variety);
         deleteButton.setEnabled(true);
         deleteButton.setClickable(true);
         deleteButton.setOnClickListener(onDeleteButtonClickListener);
      }
      return convertView;
   }

   private String getCropTypeText(Variety variety) {
      String cropTypeText;
      try {
         cropTypeText = EnumValueToUiStringUtility.getUiStringForCropType(variety.getCropType(), context);
      }
      catch (IllegalArgumentException e){
         cropTypeText = "";
      }
      return cropTypeText;
   }

   /**
    * Sorts the content of this adapter using the specified comparator.
    *
    * @param comparator The comparator used to sort the objects contained
    *        in this adapter.
    */
   public void sort(Comparator<Variety> comparator, boolean asc) {
      synchronized (listsLock) {
         if (originalList != null) {
            Collections.sort(originalList, comparator);
            if (!asc) {
               Collections.reverse(originalList);
            }
         }
         // removed the "else" compared to the original ArrayAdapter implementation because the ArrayAdapter is bugged
         // https://code.google.com/p/android/issues/detail?id=9666
         // https://code.google.com/p/android/issues/detail?id=69179
         Collections.sort(filteredList, comparator);
         if (!asc) {
            Collections.reverse(filteredList);
         }
      }
      notifyDataSetChanged();
   }

   @Override
   public Filter getFilter() {
      if (filter == null) {
         filter = new VarietyFilter();
      }
      return filter;
   }


   private class OnEditButtonClickListener implements View.OnClickListener {

      @Override
      public void onClick(View view) {
         logger.debug("on edit button clicked for variety: {}", ((Variety) view.getTag()).getName());
         final Variety variety = (Variety) view.getTag();
         editVarietyDialog = new AddOrEditVarietyDialog(context);
         editVarietyDialog.setCurrentVariety(variety);
         synchronized (listsLock) {
            editVarietyDialog.setVarietyList(originalList != null ? originalList : filteredList);
         }
         editVarietyDialog.setVIPService(vipService);
         editVarietyDialog.setFirstButtonText(activity.getResources().getString(R.string.variety_dialog_save_button_text))
               .setSecondButtonText(activity.getResources().getString(R.string.variety_dialog_cancel_button_text))
               .showThirdButton(false).setTitle(activity.getResources().getString(R.string.variety_edit_dialog_title))
               .setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT).setBodyView(R.layout.variety_add_or_edit_dialog)
               .setDialogWidth(ProductLibraryFragment.DIALOG_WIDTH);

         editVarietyDialog.setActionType(AddOrEditVarietyDialog.VarietyDialogActionType.EDIT);
         final TabActivity useModal = activity;
         useModal.showModalPopup(editVarietyDialog);
      }
   }

   private class OnDeleteButtonClickListener implements View.OnClickListener {

      @Override
      public void onClick(View view) {
         logger.debug("on delete button clicked for variety: {}", ((Variety) view.getTag()).getName());
         final Variety variety = (Variety) view.getTag();
         final TextDialogView deleteDialog = new TextDialogView(VarietyAdapter.this.context);
         deleteDialog.setBodyText(VarietyAdapter.this.activity.getResources().getString(R.string.variety_delete_dialog_body_text_delete_allowed));
         deleteDialog.setTitle(VarietyAdapter.this.activity.getResources().getString(R.string.variety_delete_dialog_delete_title_text));
         deleteDialog.setFirstButtonEnabled(true);
         deleteDialog.setFirstButtonText(VarietyAdapter.this.activity.getResources().getString(R.string.delete_dialog_confirm_button_text));
         deleteDialog.setSecondButtonEnabled(true);
         deleteDialog.setSecondButtonText(VarietyAdapter.this.activity.getResources().getString(R.string.cancel));
         deleteDialog.showThirdButton(false);
         deleteDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
            @Override
            public void onButtonClick(DialogViewInterface dialogViewInterface, int buttonNumber) {
               if (buttonNumber == DialogViewInterface.BUTTON_FIRST) {
                  deleteVariety(variety, VarietyAdapter.this.vipService);
               }
               deleteDialog.dismiss();
            }

            /**
             * Deletes the variety
             * @param variety Variety to delete
             * @param vipService the vipService
             */
            private void deleteVariety(Variety variety, IVIPServiceAIDL vipService){
               logger.debug("on okay button clicked on variety delete dialog for variety: {}", variety.getName());
               VarietyCommandParams params = new VarietyCommandParams(vipService, variety);
               new VIPAsyncTask<VarietyCommandParams, Variety>(params, new GenericListener<Variety>() {
                  @Override
                  public void handleEvent(Variety variety) {
                     if (variety != null) {
                        VarietyAdapter.this.removeVariety(variety);
                     }
                  }
               }).execute(new DeleteVarietyCommand());
            }
         });
         TabActivity useModal = VarietyAdapter.this.activity;
         useModal.showModalPopup(deleteDialog);
      }
   }

   /**
    * A {@link Filter} using a charSequence to check if charSequence is part of varieties name or varieties
    * enum-values-ui-string.
    */
   private class VarietyFilter extends Filter {

      @Override
      protected FilterResults performFiltering(CharSequence charSequence) {
         final FilterResults results = new FilterResults();
         if (originalList == null){
            synchronized (listsLock) {
               originalList = new ArrayList<Variety>(filteredList);
            }
         }
         final ArrayList<Variety> copyOfOriginalList;
         synchronized (listsLock) {
            copyOfOriginalList = new ArrayList<Variety>(originalList);
         }
         if (charSequence == null || charSequence.length() == 0) {
            results.values = copyOfOriginalList;
            results.count = copyOfOriginalList.size();
         } else {
            final ArrayList<Variety> newVarietyList = new ArrayList<Variety>();
            for (Variety variety : copyOfOriginalList) {
               if (variety != null) {
                  final String searchString = charSequence.toString().toLowerCase();
                  if (variety.getName().toLowerCase().contains(searchString)) {
                     newVarietyList.add(variety);
                  } else {
                     if (getCropTypeText(variety).toLowerCase().contains(charSequence)) {
                        newVarietyList.add(variety);
                     }
                  }
               }
            }
            results.values = newVarietyList;
            results.count = newVarietyList.size();
         }
         return results;
      }

      @Override
      protected void publishResults(CharSequence constraint, FilterResults results) {
         //noinspection unchecked
         filteredList = (List<Variety>) results.values;
         if (results.count > 0) {
            notifyDataSetChanged();
         } else {
            notifyDataSetInvalidated();
         }
      }
   }
}