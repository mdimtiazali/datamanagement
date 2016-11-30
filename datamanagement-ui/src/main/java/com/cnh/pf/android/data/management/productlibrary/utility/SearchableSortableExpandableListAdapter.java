/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.utility;

import com.cnh.android.pf.widget.adapters.SearchableExpandableListAdapter;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Extension of a {@link com.cnh.android.pf.widget.adapters.SearchableExpandableListAdapter}.
 * This classed is prepared for sorting of it's elements. You need to configure a comparator to use the functionality.
 *
 * @author waldschmidt
 */
public abstract class SearchableSortableExpandableListAdapter<T> extends SearchableExpandableListAdapter {

   private static final Logger log = LoggerFactory.getLogger(ProductLibraryFragment.class);
   private List<T> itemList;

   public SearchableSortableExpandableListAdapter() {
      itemList = new ArrayList<T>();
   }

   /**
    * Getter for the item list.
    * @return the list of items
    */
   public List<T> getItems() {
      return itemList;
   }

   /**
    * Setter for the item list.
    * @param items the new list of items
    */
   public void setItems(List<T> items) {
      this.itemList = items;
   }

   /**
    * Sorts the items inside and updates the adapter.
    * @param comparator the comparator used for sorting
    * @param asc true if the items should be sorted ascending, false otherwise
    */
   public void sort(Comparator<T> comparator, boolean asc) {
      if ((this.itemList != null) && (!this.itemList.isEmpty())) {
         Collections.sort(this.itemList, comparator);
         if (!asc) {
            Collections.reverse(this.itemList);
         }
         this.notifyDataSetChanged();
      }
   }

   @Override
   public int getGroupCount() {
      return this.itemList == null ? 0 : this.itemList.size();
   }

   @Override
   public int getChildrenCount(int group) {
      if (0 <= group && itemList != null && group < this.itemList.size()) {
         return 1;
      }
      else {
         log.warn(this.getClass().getSimpleName(), "getChildrenCount invalid group: " + group);
         return 0;
      }
   }

   @Override
   public T getGroup(int groupId) {
      return this.itemList.get(groupId);
   }

   @Override
   public T getChild(int group, int child) {
      return (T) this.itemList.get(group);
   }

   @Override
   public long getGroupId(int group) {
      return getGroup(group).hashCode();
   }

   @Override
   public long getChildId(int group, int child) {
      return getGroup(group).hashCode();
   }

   @Override
   public boolean isChildSelectable(int groupPosition, int childPosition) {
      return true;
   }

   @Override
   public boolean hasStableIds() {
      return true;
   }
}
