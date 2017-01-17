/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.adapter;

import android.widget.Filter;
import com.cnh.android.pf.widget.adapters.SearchableExpandableListAdapter;

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
 * Remark: In the cases this class is used synchro
 * nization for the item list is used. Because this lead to errors this
 * class has package visibility and is not public.
 *
 * @author waldschmidt
 */
abstract class SearchableSortableExpandableListAdapter<T> extends SearchableExpandableListAdapter {

   private static final Logger log = LoggerFactory.getLogger(SearchableSortableExpandableListAdapter.class);
   List<T> filteredList;  // in ArrayAdapter called mObjects
   List<T> originalList; // in ArrayAdapter called mOriginalValues
   boolean isFiltered = false; // only change this in block synchronized by listsLock

   // lock for filteredList and originalList
   protected final Object listsLock = new Object();

   public SearchableSortableExpandableListAdapter(List<T> items) {
      filteredList = new ArrayList<T>(items);
   }

   /**
    * To get a copy of the unfiltered list.
    * @return the unfiltered list
    */
   public List<T> getCopyOfUnfilteredItemList(){
      synchronized (listsLock){
         if (originalList != null){
            return new ArrayList<T>(originalList);
         } else {
            // if original list is null filtered list is unfiltered
            return new ArrayList<T>(filteredList);
         }
      }
   }

   /**
    * Setter for the item list.
    * @param items the new list of items
    */
   public void setItems(List<T> items) {
      synchronized (listsLock){
         filteredList = new ArrayList<T>(items);
         originalList = null;
         updateFiltering();
      }
      notifyDataSetChanged();
   }

   /**
    * Removes a T.
    * @param item the item to remove
    */
   public void removeItem(T item){
      synchronized (listsLock){
         if (filteredList != null) {
            filteredList.remove(item);
         }
         if (originalList != null) {
            originalList.remove(item);
         }
         updateFiltering();
      }
      notifyDataSetChanged();
   }

   /**
    * If the list was changed - it's possible that the filter has working at a temporary copy of the old list and he must
    * not change the lists based on old data. So we set is filtered to false to force the filter to throw away the old
    * data and re-trigger filtering.
   */
   private void updateFiltering() {
      isFiltered = false;
      Filter filter = getFilter();
      if (filter instanceof UpdateableFilter){
         ((UpdateableFilter)filter).updateFiltering();
      }
   }

   /**
    * Sorts the items inside and updates the adapter.
    * @param comparator the comparator used for sorting
    * @param asc true if the items should be sorted ascending, false otherwise
    */
   public void sort(Comparator<T> comparator, boolean asc) {
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
   public int getGroupCount() {
      synchronized (listsLock) {
         return this.filteredList == null ? 0 : this.filteredList.size();
      }
   }

   @Override
   public int getChildrenCount(int group) {
      synchronized (listsLock) {
         if (0 <= group && filteredList != null && group < this.filteredList.size()) {
            return 1;
         } else {
            log.warn("{} getChildrenCount invalid group: {}", this.getClass().getSimpleName(), group);
            return 0;
         }
      }
   }

   @Override
   public T getGroup(int groupId) {
      synchronized (listsLock) {
         return this.filteredList.get(groupId);
      }
   }

   @Override
   public T getChild(int group, int child) {
      synchronized (listsLock) {
         return this.filteredList.get(group);
      }
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

   /**
    * Setter for the filter.
    *
    * {@inheritDoc}
    *
    * The filter used in this class should be an {@link UpdateableFilter}
    * @param filter the filter.
    */
   @Override
   public void setFilter(Filter filter) {
      super.setFilter(filter);
   }
}
