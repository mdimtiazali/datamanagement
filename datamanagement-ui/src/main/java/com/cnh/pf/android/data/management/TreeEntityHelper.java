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
import java.util.Map;

/**
 * Helper class deals with entity grouping for all Groups {PFDS, VIP, ...}
 * @author oscar.salazar@cnhind.com
 */
public class TreeEntityHelper {

   private static final Map<String, Integer> TYPE_ICONS = new HashMap<String, Integer>();

   static {
      TYPE_ICONS.put(DataTypes.GROWER, R.drawable.ic_datatree_grower);
      TYPE_ICONS.put(DataTypes.FARM, R.drawable.ic_datatree_farm);
      TYPE_ICONS.put(DataTypes.FIELD, R.drawable.ic_datatree_field);
      TYPE_ICONS.put(DataTypes.TASK, R.drawable.ic_datatree_tasks);
      TYPE_ICONS.put(DataTypes.RX, R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put(DataTypes.RX_PLAN, R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put(DataTypes.PRODUCT, R.drawable.ic_datatree_obstacles);
      TYPE_ICONS.put(DataTypes.PRODUCT_MIX, R.drawable.ic_datatree_obstacles);
      TYPE_ICONS.put(DataTypes.BOUNDARY, R.drawable.ic_datatree_boundaries);
      TYPE_ICONS.put(DataTypes.LANDMARK, R.drawable.ic_datatree_obstacles);
      TYPE_ICONS.put(DataTypes.GUIDANCE_GROUP, R.drawable.ic_data_tree_swath);
      TYPE_ICONS.put(DataTypes.GUIDANCE_PATTERN, R.drawable.ic_datatree_swath);
      TYPE_ICONS.put(DataTypes.GUIDANCE_CONFIGURATION, R.drawable.ic_data_tree_guidance);
      TYPE_ICONS.put(DataTypes.COVERAGE, R.drawable.ic_data_tree_coverage_area);
      TYPE_ICONS.put(DataTypes.NOTE, R.drawable.ic_datatree_background_layers);
      TYPE_ICONS.put(DataTypes.FILE, R.drawable.ic_data_tree_harvesting);
      TYPE_ICONS.put(DataTypes.VEHICLE, R.drawable.ic_data_tree_tractor_case);
      TYPE_ICONS.put(DataTypes.VEHICLE_IMPLEMENT, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.VEHICLE_IMPLEMENT_CONFIG, R.drawable.ic_datatree_background_layers);
      TYPE_ICONS.put(DataTypes.IMPLEMENT, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.IMPLEMENT_PRODUCT_CONFIG, R.drawable.ic_datatree_screenshots);
   }

   /* Map lists all entities which are groupable in ui, entities with types specified in
   this list will be grouped in ui.
    */
   public static Map<String, Integer> groupables = new HashMap<String, Integer>() {
      {
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
      }
   };

   public static String getGroupName(Context context, String type) {
      if (groupables.containsKey(type)) {
         return context.getString(groupables.get(type));
      }
      String name = type.substring(type.lastIndexOf('.') + 1);
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "s";
   }

   /**
    * @param type the ObjectGraph type
    * @return  the icon resource id
    */
   public static int getIcon(String type) {
      return TYPE_ICONS.get(type);
   }

   /**
    * @param type the ObjectGraph type
    * @return is there an icon for this type
    */
   public static boolean hasIcon(String type) {
      return TYPE_ICONS.containsKey(type);
   }
}
