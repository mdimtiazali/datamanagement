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
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.utility.UiHelper;
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

   /**
    * @param context the context of the adapter
    * @param varieties the list of varieties which should be shown.
    */
   public VarietyAdapter(Context context, List<Variety> varieties) {
      this.context = context;
      // don't allow modifications from outside.
      // as long as the filtered list is "unfiltered", it's the list we get via parameter
      this.filteredList = new ArrayList<Variety>(varieties);
      onEditButtonClickListener = new OnEditButtonClickListener();
      onDeleteButtonClickListener = new OnDeleteButtonClickListener();
   }

   /**
    * Setter for the variety list.
    *
    * @param varieties
    */
   public void setVarietyList(List<Variety> varieties){
      synchronized (listsLock){
         filteredList = new ArrayList<Variety>(varieties);
         originalList = null;
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
         convertView = LayoutInflater.from(context).inflate(R.layout.varietylist_item, parent, false);
      }
      final TextView nameTextView = (TextView) convertView.findViewById(R.id.variety_name_textview);
      nameTextView.setText(variety.getName());
      final TextView cropTypeTextView = (TextView) convertView.findViewById(R.id.variety_crop_type_name);
      cropTypeTextView.setText(EnumValueToUiStringUtility.getUiStringForCropType(variety.getCropType(), context));
      UiHelper.setAlternatingTableItemBackground(context, position, convertView);
      final ImageButton editButton = (ImageButton) convertView.findViewById(R.id.variety_edit_button);
      editButton.setTag(variety);
      editButton.setOnClickListener(onEditButtonClickListener);
      final ImageButton deleteButton = (ImageButton) convertView.findViewById(R.id.variety_delete_button);
      deleteButton.setTag(variety);
      deleteButton.setOnClickListener(onDeleteButtonClickListener);
      return convertView;
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
         logger.debug("on edit button clicked for variety: " + ((Variety) view.getTag()).getName());
      }
   }

   private class OnDeleteButtonClickListener implements View.OnClickListener {

      @Override
      public void onClick(View view) {
         logger.debug("on delete button clicked for variety: " + ((Variety) view.getTag()).getName());
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
                  } else if (EnumValueToUiStringUtility.getUiStringForCropType(variety.getCropType(), context)
                        .toLowerCase().contains(charSequence)) {
                     newVarietyList.add(variety);
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