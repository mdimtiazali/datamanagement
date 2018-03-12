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
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.library.CropType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * AbstractVarietyComparator compares varieties by various fields.
 *
 * @author waldschmidt
 */
public abstract class AbstractVarietyComparator implements Comparator<Variety>{
   
   private CropTypeComparator cropTypeComparator;

   /**
    * Compares the name of the varieties v1 and v2
    * @param v1 Variety 1
    * @param v2 Variety 2
    * @return the compareTo result returned by {@link String#compareTo(String)} for the names of the varieties.
    */
   protected int compareName(Variety v1, Variety v2){
      return v1.getName().compareTo(v2.getName());
   }

   /**
    * Compares the localized names of the cropTypes of the varieties
    * @param v1 Variety 1
    * @param v2 Variety 2
    * @param context a context to get resources
    * @return the compareTo result returned by {@link String#compareTo(String)} for the localized strings of the cropTypes
    */
   protected int compareCropType(Variety v1, Variety v2, Context context){
      if (cropTypeComparator == null){
         cropTypeComparator = new CropTypeComparator(context);
      }
      return cropTypeComparator.compare(v1.getCropType(), v2.getCropType());
   }

   /**
    * Check if the names of the varieties v1 and v2 are equal
    * @param v1 Variety 1
    * @param v2 Variety 2
    * @return true if the names are equal, false otherwise
    */
   protected boolean isNameEqual(Variety v1, Variety v2){
      return v1.getName().equals(v2.getName());
   }

   /**
    * Compares the cropTypes of the varieties.
    * @param v1 Variety 1
    * @param v2 Variety 2
    * @return true if the {@link CropType}s are equal, false otherwise
    */
   protected boolean isCropTypeEqual(Variety v1, Variety v2){
      return v1.getCropType().equals(v2.getCropType());
   }
}
