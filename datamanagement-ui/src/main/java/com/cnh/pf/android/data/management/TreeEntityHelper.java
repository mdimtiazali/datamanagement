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
      put(DataTypes.GROWER, R.string.pfds);
      put(DataTypes.TASK, R.string.tasks);
      put(DataTypes.RX, R.string.prescriptions);
      put(DataTypes.BOUNDARY, R.string.boundaries);
      put(DataTypes.GUIDANCE_GROUP, R.string.guidance_groups);
      put(DataTypes.LANDMARK, R.string.obstacles);
      put(DataTypes.PRODUCT, R.string.products);
      put(DataTypes.PRODUCT_MIX, R.string.product_mixes);
      put(DataTypes.VEHICLE, R.string.vehicles);
      put(DataTypes.IMPLEMENT, R.string.imps);
      put(DataTypes.NOTE, R.string.notes);
   }};

   public static String getGroupName(Context context, String type) {
      if(groupables.containsKey(type)) {
         return context.getString(groupables.get(type));
      }
      String name = type.substring(type.lastIndexOf('.') + 1);
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "s";
   }
}
