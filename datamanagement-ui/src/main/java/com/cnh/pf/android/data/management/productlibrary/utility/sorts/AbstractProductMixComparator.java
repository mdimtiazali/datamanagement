/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.utility.sorts;

import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductMix;

import java.util.Comparator;

/**
 * AbstractProductComparator
 * Compares products by various fields
 * base class for implementations
 */
public abstract class AbstractProductMixComparator implements Comparator<ProductMix> {

   protected int compareName(ProductMix m1, ProductMix m2) {
      Product f1 = m1.getProductMixParameters();
      Product f2 = m2.getProductMixParameters();
      return f1.getName().compareTo(f2.getName());
   }

   protected int compareForm(ProductMix m1, ProductMix m2) {
      Product f1 = m1.getProductMixParameters();
      Product f2 = m2.getProductMixParameters();
      return f1.getForm().name().compareTo(f2.getForm().name());
   }

   protected int compareDefaultRate(ProductMix m1, ProductMix m2) {
      Product f1 = m1.getProductMixParameters();
      Product f2 = m2.getProductMixParameters();
      return Double.compare(f1.getDefaultRate(), f2.getDefaultRate());
   }

   protected boolean isNameEqual(ProductMix m1, ProductMix m2) {
      if (m1 != null && m2 != null) {
         Product f1 = m1.getProductMixParameters();
         Product f2 = m2.getProductMixParameters();
         return f1.getName().equals(f2.getName());
      }
      else {
         return false;
      }
   }

   protected boolean isFormEqual(ProductMix m1, ProductMix m2) {
      Product f1 = m1.getProductMixParameters();
      Product f2 = m2.getProductMixParameters();
      return f1.getForm().name().equals(f2.getForm().name());
   }

   protected boolean isDefaultRateEqual(ProductMix m1, ProductMix m2) {
      Product f1 = m1.getProductMixParameters();
      Product f2 = m2.getProductMixParameters();
      return f1.getDefaultRate() == f2.getDefaultRate();
   }

}