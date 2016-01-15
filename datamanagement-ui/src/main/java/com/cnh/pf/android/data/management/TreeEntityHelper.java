/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import android.content.Context;
import com.cnh.jgroups.DataTypes;
import com.google.common.base.CaseFormat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class deals with entity grouping for all Groups {PFDS, VIP, ...}
 * @author oscar.salazar@cnhind.com
 */
public class TreeEntityHelper {

   /* Map lists all entities which are groupable in ui, entities with types specified in
   this list will be grouped in ui.
    */
   public static Map<String, Integer> groupables = new HashMap<String, Integer>() {{
      put("com.cnh.pf.model.pfds.Customer", R.string.pfds);
      put("com.cnh.pf.model.pfds.Task", R.string.tasks);
      put("com.cnh.pf.model.pfds.Prescription", R.string.prescriptions);
      put("com.cnh.autoguidance.boundary.BoundaryItem", R.string.boundaries);
      put("com.cnh.autoguidance.model.GuidanceGroup", R.string.guidance_groups);
      put("com.cnh.autoguidance.boundary.LandmarkItem", R.string.obstacles);
      put("com.cnh.pf.model.product.library.Product", R.string.products);
      put("com.cnh.pf.model.product.library.ProductMix", R.string.product_mixes);
      put(DataTypes.VEHICLE, R.string.vehicles);
      put(DataTypes.IMPLEMENT, R.string.imps);
   }};

   public static String getGroupName(Context context, String type) {
      if(groupables.containsKey(type)) {
         return context.getString(groupables.get(type));
      }
      String name = type.substring(type.lastIndexOf('.') + 1);
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "s";
   }
}
