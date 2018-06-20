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

   /**
    * Map lists all entities which are groupable in ui, entities with types specified in this lists will be grouped in ui.
    */
   protected static final Map<String, Integer> group2name = new HashMap<String, Integer>();
   protected static final Map<String, String> obj2group = new HashMap<String, String>();
   protected static final Map<String, String> group2group = new HashMap<String, String>();

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
   public static final String DDOP = "DDOP";

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
      TYPE_ICONS.put(DataTypes.USB, R.drawable.ic_data_tree_usb_active);
      TYPE_ICONS.put(DataTypes.CLOUD,R.drawable.ic_data_tree_cloud_active);

      group2name.put(GROWERS, R.string.pfds);
      group2name.put(GROWERS, R.string.pfds);
      group2name.put(TASKS, R.string.tasks);
      group2name.put(RXS, R.string.prescriptions);
      group2name.put(BOUNDARIES, R.string.boundaries);
      group2name.put(GUIDANCE_GROUPS, R.string.guidance_groups);
      group2name.put(LANDMARKS, R.string.obstacles);
      group2name.put(PRODUCTS, R.string.products);
      group2name.put(PRODUCT_MIXS, R.string.product_mixes);
      group2name.put(VARIETIES, R.string.varieties);
      group2name.put(VEHICLES, R.string.vehicles);
      group2name.put(IMPLEMENTS, R.string.imps);
      group2name.put(NOTES, R.string.notes);
      group2name.put(GUIDANCE_CONFIGURATIONS, R.string.guidance_configurations);
      group2name.put(PRODUCT_MIX_VARIETY, R.string.product_mix_variety);

      obj2group.put(DataTypes.GROWER, GROWERS);
      obj2group.put(DataTypes.TASK, TASKS);
      obj2group.put(DataTypes.RX, RXS);
      obj2group.put(DataTypes.BOUNDARY, BOUNDARIES);
      obj2group.put(DataTypes.GUIDANCE_GROUP, GUIDANCE_GROUPS);
      obj2group.put(DataTypes.LANDMARK, LANDMARKS);
      obj2group.put(DataTypes.PRODUCT, PRODUCTS);
      obj2group.put(DataTypes.PRODUCT_MIX, PRODUCT_MIXS);
      obj2group.put(DataTypes.VARIETY, VARIETIES);
      obj2group.put(DataTypes.VEHICLE, VEHICLES);
      obj2group.put(DataTypes.IMPLEMENT, IMPLEMENTS);
      obj2group.put(DataTypes.NOTE, NOTES);
      obj2group.put(DataTypes.GUIDANCE_CONFIGURATION, GUIDANCE_CONFIGURATIONS);
      obj2group.put(DataTypes.DDOP, DDOP);

      group2group.put(PRODUCTS, PRODUCT_MIX_VARIETY);
      group2group.put(PRODUCT_MIXS, PRODUCT_MIX_VARIETY);
      group2group.put(VARIETIES, PRODUCT_MIX_VARIETY);
   }

   private TreeEntityHelper() {
      //prevent instantiation
   }

   /**
    * Returns if a given type is representing a groupType
    * @param type Type in question to be a groupType
    * @return True if type is a groupType, false otherwise
    */
   public static boolean isGroupType(String type) {
      return group2name.containsKey(type);
   }

   /**
    * get a its group type, return origin type if there is no config
    * @param type the ObjectGraph type
    * @return group's data type
    */
   public static String getGroupType(String type) {
      if (obj2group.containsKey(type)) {
         return obj2group.get(type);
      }
      return type;
   }

   /**
    * get a its group's group type, return origin type if there is no config
    * @param type the ObjectGraph type
    * @return group's group data type
    */
   public static String getGroupOfGroupType(String type) {
      if (group2group.containsKey(obj2group.get(type))) {
         return group2group.get(obj2group.get(type));
      }
      return type;
   }

   /**
    * check if the data type has a parent group
    * @param type the ObjectGraph type
    * @return true if it has a parent group, false for no
    */
   public static boolean isGroup(String type) {
      return obj2group.containsKey(type);
   }

   /**
    * check if the data type has a group's group
    * @param type the ObjectGraph type
    * @return true if it has a group's group, false for no
    */
   public static boolean isGroupOfGroup(String type) {
      if (isGroup(type)) {
         return group2group.containsKey(getGroupType(type));
      }
      return false;
   }

   /**
    * Returns the group name of a given type
    * @param context Context string of GroupName should be loaded from
    * @param type Type of whose GroupName should be loaded from
    * @return GroupName of the given type
    */
   public static String getGroupName(Context context, String type) {
      if (group2name.containsKey(type)) {
         return context.getString(group2name.get(type));
      }
      String name = type.substring(type.lastIndexOf('.') + 1);
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name) + "s";
   }

   /**
    * @param type the ObjectGraph type
    * @return the icon resource id
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
