/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.utility.sorts;

import com.cnh.pf.model.product.library.Product;

/**
 * FormSortComparator
 * Compares products by form
 */
public class FormSortComparator extends AbstractProductComparator {
   /**
    * Compare two products by form
    * @param f1 first product
    * @param f2 second product
    * @return integer result of form comparison
    */
   public int compare(Product f1, Product f2) {
      if (isFormEqual(f1, f2)) {
         if (isNameEqual(f1, f2)) {
            return compareDefaultRate(f1, f2);
         }
         return compareName(f1, f2);
      }
      return compareForm(f1, f2);
   }
}