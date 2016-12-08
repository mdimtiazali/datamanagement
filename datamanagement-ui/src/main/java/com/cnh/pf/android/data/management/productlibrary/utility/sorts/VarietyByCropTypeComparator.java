/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.utility.sorts;

import android.content.Context;
import com.cnh.pf.model.product.configuration.Variety;

/**
 * AbstractVarietyComparator compares varieties by various fields. Leading field is the crop type name (localized).
 *
 * @author waldschmidt
 */
public class VarietyByCropTypeComparator extends AbstractVarietyComparator {

   private final Context context;

   public VarietyByCropTypeComparator(Context context){
      this.context = context;
   }

   @Override
   public int compare(Variety v1, Variety v2) {
      if (isCropTypeEqual(v1, v2)) {
         return compareName(v1, v2);
      }
      return compareCropType(v1, v2, context);
   }
}
