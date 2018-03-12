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
 * NameSortComparator
 * Compares products by name
 */
public class ProductMixNameSortComparator extends AbstractProductMixComparator {

   private final Context context;

   public ProductMixNameSortComparator(Context context){
      this.context = context;
   }

   /**
    * Compare two productmixes by name
    * @param f1 first productMix
    * @param f2 second productMix
    * @return integer result of name comparison
    */
   public int compare(ProductMix f1, ProductMix f2) {
      if (isNameEqual(f1, f2)) {
         if (isFormEqual(f1, f2, context)) {
            return compareDefaultRate(f1, f2);
         }
         return compareForm(f1, f2, context);
      }
      return compareName(f1, f2);
   }
}