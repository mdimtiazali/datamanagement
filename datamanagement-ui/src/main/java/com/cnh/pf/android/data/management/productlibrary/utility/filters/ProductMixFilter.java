/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.utility.filters;

import android.content.Context;
import android.widget.Filter;

import com.cnh.android.pf.widget.utilities.MathUtility;
import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.android.pf.widget.utilities.UnitsSettings;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductMix;
import com.cnh.pf.model.product.library.ProductUnits;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters products, providing search functionality
 * @author joorjitham
 */
public class ProductMixFilter extends Filter {

   private static final String TAG = ProductMixFilter.class.getSimpleName();
   private List<ProductMix> fullProductList;
   private Context context;
   private ProductLibraryFragment.ProductMixAdapter productMixAdapter;

   public ProductMixFilter(ProductLibraryFragment.ProductMixAdapter adapter, Context cntext, List<ProductMix> fullList) {
      super();
      productMixAdapter = adapter;
      context = cntext;
      fullProductList = fullList;
   }

   @Override
   protected FilterResults performFiltering(CharSequence charSequence) {
      FilterResults results = new FilterResults();
      if (charSequence == null || charSequence.length() == 0) {
         results.values = fullProductList;
         results.count = fullProductList.size();
      }
      else {
         List<ProductMix> productMixList = productMixAdapter.getItems();
         // We perform filtering operation
         List<ProductMix> nProductList = new ArrayList<ProductMix>();

         for (ProductMix mix : productMixList) {
            Product p = mix.getProductMixParameters();
            final ProductUnits rateProductUnits = ProductHelperMethods.retrieveProductRateUnits(p, ProductHelperMethods.queryMeasurementSystem(context, UnitsSettings.VOLUME));
            double rateUnitFactor = 1.0;
            if (rateProductUnits != null && rateProductUnits.isSetMultiplyFactorFromBaseUnits()) {
               rateUnitFactor = rateProductUnits.getMultiplyFactorFromBaseUnits();
            }

            if (p.getName().toUpperCase().contains(charSequence.toString().toUpperCase())
                  || (p.getForm() != null && p.getForm().name().toUpperCase().contains(charSequence.toString().toUpperCase()))
                  || (rateProductUnits != null && rateProductUnits.getName().toUpperCase().contains(charSequence.toString().toUpperCase()))
                  || (String.valueOf(MathUtility.getConvertedFromBase(p.getDefaultRate(), rateUnitFactor))).contains(charSequence.toString())) {
               nProductList.add(mix);
            }
         }

         results.values = nProductList;
         results.count = nProductList.size();

      }
      return results;
   }

   @Override
   protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
      // Now we have to inform the adapter about the new list filtered
      if (filterResults.count == 0)
         productMixAdapter.notifyDataSetInvalidated();
      else {
         productMixAdapter.setItems((List<ProductMix>) filterResults.values);
         productMixAdapter.notifyDataSetChanged();
      }
   }
}
