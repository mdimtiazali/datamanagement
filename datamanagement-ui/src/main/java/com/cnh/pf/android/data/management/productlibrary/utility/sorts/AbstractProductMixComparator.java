/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.utility.sorts;

import android.content.Context;
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductMix;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * AbstractProductComparator
 * Compares products by various fields
 * base class for implementations
 */
public abstract class AbstractProductMixComparator implements Comparator<ProductMix> {

   protected int compareName(ProductMix m1, ProductMix m2) {
      return getName(m1).compareTo(getName(m2));
   }

   protected int compareForm(ProductMix m1, ProductMix m2, Context context) {
      return getFormName(m1, context).compareTo(getFormName(m2, context));
   }

   protected int compareDefaultRate(ProductMix m1, ProductMix m2) {
      return Double.compare(getDefaultRateWithNaNReturn(m1), getDefaultRateWithNaNReturn(m2));
   }

   protected boolean isNameEqual(ProductMix m1, ProductMix m2) {
      return getName(m1).equals(getName(m2));
   }

   protected boolean isFormEqual(ProductMix m1, ProductMix m2, Context context) {
      return getFormName(m1, context).equals(getFormName(m2, context));
   }

   protected boolean isDefaultRateEqual(ProductMix m1, ProductMix m2) {
      return getDefaultRate(m1) == getDefaultRate(m2);
   }

   @Nonnull
   private static String getName(ProductMix productMix) {
      String name = "";
      if (productMix != null) {
         Product productMixParameters = productMix.getProductMixParameters();
         if (productMixParameters != null) {
            name = productMixParameters.getName();
         }
      }
      return name;
   }

   @Nonnull
   private static String getFormName(ProductMix productMix, Context context) {
      String name = "";
      if (productMix != null) {
         Product productMixParameters = productMix.getProductMixParameters();
         if (productMixParameters != null) {
            name = EnumValueToUiStringUtility.getUiStringForProductForm(productMixParameters.getForm(), context);
         }
      }
      return name;
   }

   @Nullable
   private static Double getDefaultRate(ProductMix productMix){
      Double defaultRate = null;
      if (productMix != null) {
         Product product = productMix.getProductMixParameters();
         if (product != null){
            defaultRate =  product.getDefaultRate();
         }
      }
      return defaultRate;
   }

   private static double getDefaultRateWithNaNReturn(ProductMix productMix) {
      Double defaultRate = getDefaultRate(productMix);
      if (defaultRate == null) {
         // we can use Double.NaN in this case because Double.compare handles it like we need that,
         // but it can't handle null.
         return Double.NaN;
      } else {
         return defaultRate;
      }
   }
}