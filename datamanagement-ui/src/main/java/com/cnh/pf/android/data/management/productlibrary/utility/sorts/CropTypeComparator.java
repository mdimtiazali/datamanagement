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
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.pf.model.product.library.CropType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * Comparator for CropTypes. Compare is using the localized names.
 *
 * @author waldschmidt
 */
public class CropTypeComparator implements Comparator<CropType> {

   private static final Logger logger = LoggerFactory.getLogger(CropTypeComparator.class);

   private final Context context;

   public CropTypeComparator(Context context){
      this.context = context;
   }

   @Override
   public int compare(CropType ct1, CropType ct2) {
      String cropTypeText1 = "";
      String cropTypeText2 = "";
      try {
         cropTypeText1 = EnumValueToUiStringUtility.getUiStringForCropType(ct1, context);
      } catch (IllegalArgumentException e){
         logger.error(this.getClass().getName(), e);
      }
      try {
         cropTypeText2 = EnumValueToUiStringUtility.getUiStringForCropType(ct2, context);
      } catch (IllegalArgumentException e){
         logger.error(this.getClass().getName(), e);
      }
      return cropTypeText1.compareTo(cropTypeText2);
   }
}
