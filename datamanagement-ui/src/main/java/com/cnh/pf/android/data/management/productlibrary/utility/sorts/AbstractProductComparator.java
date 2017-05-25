/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.utility.sorts;

import android.content.Context;

import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.pf.model.product.library.Product;

import java.util.Comparator;

/**
 * AbstractProductComparator
 * Compares products by various fields
 * base class for implementations
 */
public abstract class AbstractProductComparator implements Comparator<Product> {

   protected int compareName(Product f1, Product f2) {
      return f1.getName().compareTo(f2.getName());
   }

   protected int compareForm(Product f1, Product f2, Context context) {
      return EnumValueToUiStringUtility.getUiStringForProductForm(f1.getForm(), context).compareTo(EnumValueToUiStringUtility.getUiStringForProductForm(f2.getForm(), context));
   }

   protected int compareDefaultRate(Product f1, Product f2) {
      return Double.compare(f1.getDefaultRate(), f2.getDefaultRate());
   }

   protected boolean isNameEqual(Product f1, Product f2) {
      return f1.getName().equals(f2.getName());
   }

   protected boolean isFormEqual(Product f1, Product f2, Context context) {
      return EnumValueToUiStringUtility.getUiStringForProductForm(f1.getForm(), context).equals(EnumValueToUiStringUtility.getUiStringForProductForm(f2.getForm(), context));
   }

   protected boolean isDefaultRateEqual(Product f1, Product f2) {
      return f1.getDefaultRate() == f2.getDefaultRate();
   }

}