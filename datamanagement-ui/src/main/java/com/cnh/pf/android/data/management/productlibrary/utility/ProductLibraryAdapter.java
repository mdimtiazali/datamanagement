/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.utility;

import android.util.Log;

import com.cnh.android.pf.widget.adapters.SearchableExpandableListAdapter;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductUnits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ProductLibraryAdapter is an abstract parent class for any ExpandableListAdapters needed in
 * the product_library ui Child classes must implement getGroupView and getChildView to
 * provide specific UI implementation. Eventually, both the ProductAdapter and the future
 * ProductMixAdapter can freely inherit from ProductLibraryAdapter
 * @param <T> type param (either Product or ProductMix)
 */
public abstract class ProductLibraryAdapter<T> extends SearchableExpandableListAdapter {

   private static final String TAG = ProductLibraryAdapter.class.getSimpleName();
   private List<T> itemList;

   public ProductLibraryAdapter() {
      itemList = new ArrayList<T>();
   }

   public List<T> getItems() {
      return itemList;
   }

   public void setItems(List<T> items) {
      this.itemList = items;
   }

   public void setItems(List<T> items, Comparator<T> comparator, boolean asc) {
      this.itemList = items;
      sort(comparator, asc);
   }

   public void sort(Comparator<T> comparator, boolean asc) {
      if ((this.itemList != null) && (!this.itemList.isEmpty())) {
         Collections.sort(this.itemList, comparator);
         if (!asc) {
            Collections.reverse(this.itemList);
         }
         this.notifyDataSetChanged();
      }
   }

   public int getGroupCount() {
      return this.itemList == null ? 0 : this.itemList.size();
   }

   public int getChildrenCount(int group) {
      if (0 <= group && itemList != null && group < this.itemList.size()) {
         return 1;
      }
      else {
         Log.w(TAG, "getChildrenCount invalid group: " + group);
         return 0;
      }
   }

   public T getGroup(int groupId) {
      return this.itemList.get(groupId);
   }

   public T getChild(int group, int child) {
      return (T) this.itemList.get(group);
   }

   public long getGroupId(int group) {
      return getGroup(group).hashCode();
   }

   public long getChildId(int group, int child) {
      return getGroup(group).hashCode();
   }

   public boolean isChildSelectable(int groupPosition, int childPosition) {
      return true;
   }

   public boolean hasStableIds() {
      return true;
   }

   /**
    * get Productunit from product with the correct measurementsystem
    * @param product use to get the Product units
    * @return get the Productunit for the application rate if product is not null and measurementsystem, too, otherwise return null
    */
   public ProductUnits getApplicationRateProductUnit(Product product, MeasurementSystem measurementSystem) {
      if (product != null && measurementSystem != null) {
         switch (measurementSystem) {
         case IMPERIAL:
            return product.getRateDisplayUnitsImperial();
         case METRIC:
            return product.getRateDisplayUnitsMetric();
         case USA:
            return product.getRateDisplayUnitsUSA();
         }
      }
      return null;
   }

   /**
    * get Measurementsystem of the Product
    * @param product use to set the measurementsystem
    * @return the measurementsystem of volume or mass
    */
   public MeasurementSystem getProductUnitMeasurementSystem(Product product, MeasurementSystem volumeMeasurementSystem, MeasurementSystem massMeasurementSystem) {
      MeasurementSystem measurementSystem = MeasurementSystem.METRIC;
      if (product != null && volumeMeasurementSystem != null && massMeasurementSystem != null) {
         if (product.getForm() == ProductForm.ANHYDROUS || product.getForm() == ProductForm.LIQUID) {
            measurementSystem = volumeMeasurementSystem;
         }
         else {
            measurementSystem = massMeasurementSystem;
         }
      }
      return measurementSystem;
   }
}
