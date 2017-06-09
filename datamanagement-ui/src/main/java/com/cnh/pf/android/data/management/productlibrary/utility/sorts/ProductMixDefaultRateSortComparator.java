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
import com.cnh.pf.model.product.library.ProductMix;

/**
 * DefaultRateSortComparator
 * Compares products by form
 */
public class ProductMixDefaultRateSortComparator extends AbstractProductMixComparator {

   private final Context context;

   public ProductMixDefaultRateSortComparator(Context context){
      this.context = context;
   }

   /**
    * Compare two products by default rate
    * @param f1 first productmix
    * @param f2 second productmix
    * @return integer result of default rate comparison
    */
   public int compare(ProductMix f1, ProductMix f2) {
      if (isDefaultRateEqual(f1, f2)) {
         if (isNameEqual(f1, f2)) {
            return compareForm(f1, f2, context);
         }
         return compareName(f1, f2);
      }
      return compareDefaultRate(f1, f2);
   }
}