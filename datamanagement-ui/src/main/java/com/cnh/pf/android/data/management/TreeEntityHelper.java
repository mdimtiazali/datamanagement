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

   public static final String GROWERS = "GROWERS";
   public static final String TASKS = "TASKS";
   public static final String RXS = "RXS";
   public static final String BOUNDARIES = "BOUNDARIES";
   public static final String GUIDANCE_GROUPS = "GUIDANCE_GROUPS";
   public static final String LANDMARKS = "LANDMARKS";
   public static final String PRODUCTS = "PRODUCTS";
   public static final String PRODUCT_MIXS = "PRODUCT_MIXS";
   public static final String VARIETIES = "VARIETIES";
   public static final String VEHICLES = "VEHICLES";
   public static final String IMPLEMENTS = "IMPLEMENTS";
   public static final String NOTES = "NOTES";
   public static final String PRODUCT_MIX_VARIETY = "PRODUCT_MIX_VARIETY";
   public static final String GUIDANCE_CONFIGURATIONS = "GUIDANCE_CONFIGURATIONS";

   static {
      TYPE_ICONS.put(DataTypes.GROWER, R.drawable.ic_datatree_grower);
      TYPE_ICONS.put(GROWERS, R.drawable.ic_datatree_grower);
      TYPE_ICONS.put(DataTypes.FARM, R.drawable.ic_datatree_farm);
      TYPE_ICONS.put(DataTypes.FIELD, R.drawable.ic_datatree_field);
      TYPE_ICONS.put(DataTypes.TASK, R.drawable.ic_datatree_tasks);
      TYPE_ICONS.put(TASKS, R.drawable.ic_datatree_tasks);
      TYPE_ICONS.put(DataTypes.RX, R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put(RXS, R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put(DataTypes.RX_PLAN, R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put(DataTypes.PRODUCT, R.drawable.ic_datatree_products);
      TYPE_ICONS.put(PRODUCTS, R.drawable.ic_datatree_products);
      TYPE_ICONS.put(DataTypes.PRODUCT_MIX, R.drawable.ic_datatree_obstacles);
      TYPE_ICONS.put(PRODUCT_MIXS, R.drawable.ic_datatree_obstacles);
      TYPE_ICONS.put(PRODUCT_MIX_VARIETY, R.drawable.ic_datatree_products);
      TYPE_ICONS.put(DataTypes.BOUNDARY, R.drawable.ic_datatree_boundaries);
      TYPE_ICONS.put(BOUNDARIES, R.drawable.ic_datatree_boundaries);
      TYPE_ICONS.put(DataTypes.LANDMARK, R.drawable.ic_datatree_obstacles);
      TYPE_ICONS.put(LANDMARKS, R.drawable.ic_datatree_obstacles);
      TYPE_ICONS.put(DataTypes.GUIDANCE_GROUP, R.drawable.ic_data_tree_swath);
      TYPE_ICONS.put(GUIDANCE_GROUPS, R.drawable.ic_data_tree_swath);
      TYPE_ICONS.put(DataTypes.GUIDANCE_PATTERN, R.drawable.ic_datatree_swath);
      TYPE_ICONS.put(DataTypes.GUIDANCE_CONFIGURATION, R.drawable.ic_data_tree_guidance);
      TYPE_ICONS.put(GUIDANCE_CONFIGURATIONS, R.drawable.ic_data_tree_guidance);
      TYPE_ICONS.put(DataTypes.COVERAGE, R.drawable.ic_data_tree_coverage_area);
      TYPE_ICONS.put(DataTypes.NOTE, R.drawable.ic_datatree_background_layers);
      TYPE_ICONS.put(NOTES, R.drawable.ic_datatree_background_layers);
      TYPE_ICONS.put(DataTypes.FILE, R.drawable.ic_data_tree_harvesting);
      TYPE_ICONS.put(DataTypes.VEHICLE, R.drawable.ic_data_tree_tractor_case);
      TYPE_ICONS.put(VEHICLES, R.drawable.ic_data_tree_tractor_case);
      TYPE_ICONS.put(DataTypes.VEHICLE_IMPLEMENT, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.VEHICLE_IMPLEMENT_CONFIG, R.drawable.ic_datatree_background_layers);
      TYPE_ICONS.put(DataTypes.IMPLEMENT, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(IMPLEMENTS, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.IMPLEMENT_PRODUCT_CONFIG, R.drawable.ic_datatree_screenshots);
      TYPE_ICONS.put(VARIETIES, R.drawable.ic_data_management_varieties);
      TYPE_ICONS.put(DataTypes.VARIETY, R.drawable.ic_data_management_varieties);
   }
   /* Map lists all entities which are groupable in ui, entities with types specified in
   this list will be grouped in ui.
    */
   public static Map<String, Integer> group2name = new HashMap<String, Integer>() {
      {
         put(GROWERS, R.string.pfds);
         put(TASKS, R.string.tasks);
         put(RXS, R.string.prescriptions);
         put(BOUNDARIES, R.string.boundaries);
         put(GUIDANCE_GROUPS, R.string.guidance_groups);
         put(LANDMARKS, R.string.obstacles);
         put(PRODUCTS, R.string.products);
         put(PRODUCT_MIXS, R.string.product_mixes);
         put(VARIETIES, R.string.varieties);
         put(VEHICLES, R.string.vehicles);
         put(IMPLEMENTS, R.string.imps);
         put(NOTES, R.string.notes);
         put(GUIDANCE_CONFIGURATIONS, R.string.guidance_configurations);
         put(PRODUCT_MIX_VARIETY, R.string.product_mix_variety);
      }
   };
   public static boolean isGroupType(String type){
      return group2name.containsKey(type);
   }
   public static Map<String, String> obj2group = new HashMap<String, String>() {
      {
         put(DataTypes.GROWER, GROWERS);
         put(DataTypes.TASK, TASKS);
         put(DataTypes.RX, RXS);
         put(DataTypes.BOUNDARY, BOUNDARIES);
         put(DataTypes.GUIDANCE_GROUP, GUIDANCE_GROUPS);
         put(DataTypes.LANDMARK, LANDMARKS);
         put(DataTypes.PRODUCT, PRODUCTS);
         put(DataTypes.PRODUCT_MIX,PRODUCT_MIXS);
         put(DataTypes.VARIETY, VARIETIES);
         put(DataTypes.VEHICLE,VEHICLES);
         put(DataTypes.IMPLEMENT,IMPLEMENTS);
         put(DataTypes.NOTE,NOTES);
         put(DataTypes.GUIDANCE_CONFIGURATION,GUIDANCE_CONFIGURATIONS);
      }
   };
   public static Map<String, String> group2group = new HashMap<String, String>() {
      {
         put(PRODUCTS, PRODUCT_MIX_VARIETY);
         put(PRODUCT_MIXS, PRODUCT_MIX_VARIETY);
         put(VARIETIES, PRODUCT_MIX_VARIETY);
      }
   };

   public static String getGroupName(Context context, String type) {
      if (group2name.containsKey(type)){
         return context.getString(group2name.get(type));
      }
      String name = type.substring(type.lastIndexOf('.') + 1);
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "s";
   }

   public static String getGroupType(String type){
      if(obj2group.containsKey(type)){
         return obj2group.get(type);
      }
      return null;
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
