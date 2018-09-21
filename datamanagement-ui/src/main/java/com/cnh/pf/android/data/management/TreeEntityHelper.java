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

import com.cnh.autoguidance.shared.SwathType;
import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.pf.android.data.management.helper.DMTreeJsonData;
import com.cnh.pf.model.product.library.ProductForm;
import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import pl.polidea.treeview.TreeBuilder;

/**
 * Helper class deals with entity grouping for all Groups {PFDS, VIP, ...}
 * @author oscar.salazar@cnhind.com
 */
public class TreeEntityHelper {

   private static final Logger logger = LoggerFactory.getLogger(TreeEntityHelper.class);
   private static final Map<String, Integer> TYPE_ICONS = new HashMap<String, Integer>();
   private static final Map<SwathType, Integer> SWATH_ICONS = new EnumMap<SwathType, Integer>(SwathType.class);
   private static final Map<ProductForm, Integer> PRODUCTFORM_ICONS = new EnumMap<ProductForm, Integer>(ProductForm.class);
   public static final String HIDDEN_ITEM = "HIDDEN_ITEM";
   public static final String FOLLOW_SOURCE = "FOLLOW_SOURCE";
   public static final String CHILDREN_COUNT = "CHILDREN_COUNT";

   private static final Map<String, GroupObjectGraph> GroupObjectGraphMap = new HashMap<String, GroupObjectGraph>();
   protected static final Map<String, GroupObjectGraph> NeedtoFindParentGroup = new HashMap<String, GroupObjectGraph>();

   /**
    * Map lists all entities which are groupable in ui, entities with types specified in this lists will be grouped in ui.
    */
   protected static final Map<String, Integer> group2name = new HashMap<String, Integer>();
   protected static final Map<String, String> obj2group = new HashMap<String, String>();
   protected static final Map<String, String> group2group = new HashMap<String, String>();
   protected static final Map<String, Integer> datatype2name = new HashMap<String, Integer>();

   public static final String SUB_TYPE = "_subtype";
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
      TYPE_ICONS.put(DataTypes.GROWER, R.drawable.ic_data_tree_grower);
      TYPE_ICONS.put(DataTypes.FARM, R.drawable.ic_data_tree_farm);
      TYPE_ICONS.put(DataTypes.FIELD, R.drawable.ic_data_tree_field);
      TYPE_ICONS.put(TASKS, R.drawable.ic_data_tree_task);
      TYPE_ICONS.put(DataTypes.RX, R.drawable.ic_data_tree_rx);
      TYPE_ICONS.put(RXS, R.drawable.ic_data_tree_rx);
      TYPE_ICONS.put(DataTypes.RX_PLAN, R.drawable.ic_data_tree_rx);
      TYPE_ICONS.put(DataTypes.PRODUCT, R.drawable.dt_icon_products);
      TYPE_ICONS.put(PRODUCTS, R.drawable.dt_icon_products);
      TYPE_ICONS.put(DataTypes.PRODUCT_MIX, R.drawable.dt_icon_product_mix);
      TYPE_ICONS.put(PRODUCT_MIXS, R.drawable.dt_icon_product_mix);
      TYPE_ICONS.put(DataTypes.BOUNDARY, R.drawable.dt_icon_boundary);
      TYPE_ICONS.put(BOUNDARIES, R.drawable.dt_icon_boundary);
      TYPE_ICONS.put(DataTypes.LANDMARK, R.drawable.dt_icon_landmark);
      TYPE_ICONS.put(LANDMARKS, R.drawable.dt_icon_landmark);
      TYPE_ICONS.put(GUIDANCE_GROUPS, R.drawable.ic_data_tree_swaths);
      TYPE_ICONS.put(DataTypes.GUIDANCE_GROUP, R.drawable.ic_data_tree_swaths);
      TYPE_ICONS.put(DataTypes.GUIDANCE_PATTERN, R.drawable.ic_data_tree_swaths);
      TYPE_ICONS.put(DataTypes.GUIDANCE_CONFIGURATION, R.drawable.ic_data_tree_guidance);
      TYPE_ICONS.put(GUIDANCE_CONFIGURATIONS, R.drawable.ic_data_tree_guidance);
      TYPE_ICONS.put(DataTypes.COVERAGE, R.drawable.ic_data_tree_coverage_area);
      TYPE_ICONS.put(DataTypes.NOTE, R.drawable.dt_icon_bglayers);
      TYPE_ICONS.put(NOTES, R.drawable.dt_icon_bglayers);
      TYPE_ICONS.put(DataTypes.FILE, R.drawable.ic_data_tree_harvesting);
      TYPE_ICONS.put(DataTypes.VEHICLE, R.drawable.ic_data_tree_tractor_case);
      TYPE_ICONS.put(VEHICLES, R.drawable.ic_data_tree_tractor_case);
      TYPE_ICONS.put(DataTypes.VEHICLE_IMPLEMENT, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.VEHICLE_IMPLEMENT_CONFIG, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.IMPLEMENT, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(IMPLEMENTS, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.IMPLEMENT_PRODUCT_CONFIG, R.drawable.ic_datatree_screenshots);
      TYPE_ICONS.put(VARIETIES, R.drawable.dt_icon_varieties);
      TYPE_ICONS.put(DataTypes.USB, R.drawable.ic_data_tree_usb_active);
      TYPE_ICONS.put(DataTypes.CLOUD, R.drawable.ic_data_tree_cloud_active);

      SWATH_ICONS.put(SwathType.STRAIGHT, R.drawable.dt_icon_swath_straight);
      SWATH_ICONS.put(SwathType.HEADING, R.drawable.dt_icon_swath_heading);
      SWATH_ICONS.put(SwathType.CURVE, R.drawable.dt_icon_swath_curve);
      SWATH_ICONS.put(SwathType.PIVOT, R.drawable.ic_data_tree_pivot);
      SWATH_ICONS.put(SwathType.SPIRAL_PIVOT, R.drawable.ic_data_tree_spiral_swath);

      PRODUCTFORM_ICONS.put(ProductForm.GRANULAR, R.drawable.ic_data_tree_granular);
      PRODUCTFORM_ICONS.put(ProductForm.BULK_SEED, R.drawable.ic_data_tree_bulk_seed);
      PRODUCTFORM_ICONS.put(ProductForm.LIQUID, R.drawable.ic_data_tree_liquid);
      PRODUCTFORM_ICONS.put(ProductForm.SEED, R.drawable.ic_data_tree_seed_control);

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

      datatype2name.put(DataTypes.GROWER, R.string.grower);
      datatype2name.put(DataTypes.TASK, R.string.task);
      datatype2name.put(DataTypes.RX, R.string.prescription);
      datatype2name.put(DataTypes.BOUNDARY, R.string.boundary);
      datatype2name.put(DataTypes.GUIDANCE_GROUP, R.string.guidance_group);
      datatype2name.put(DataTypes.LANDMARK, R.string.obstacle);
      datatype2name.put(DataTypes.PRODUCT, R.string.product);
      datatype2name.put(DataTypes.PRODUCT_MIX, R.string.product_mix);
      datatype2name.put(DataTypes.VARIETY, R.string.variety);
      datatype2name.put(DataTypes.VEHICLE, R.string.vehicle);
      datatype2name.put(DataTypes.IMPLEMENT, R.string.implement);
      datatype2name.put(DataTypes.NOTE, R.string.note);
      datatype2name.put(DataTypes.GUIDANCE_CONFIGURATION, R.string.guidance_configuration);

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
    * This method returns the resource id of the translation of the given dataType
    * @param dataType DataType (upper case) as a string
    * @return Resource id of the translation of the given dataType
    */
   public static Integer getDataTypeName(String dataType) {
      if (datatype2name.containsKey(dataType)) {
         return datatype2name.get(dataType);
      }
      else {
         logger.debug("Could not find a matching translation for {} in datatype2name mapping!", dataType);
         return 0;
      }
   }

   /**
    * Returns if a given type is representing a Guidance groups
    * @param type Type in question to be a groupType
    * @return True if type is a guidance groups, false otherwise
    */
   public static boolean isGuidanceGroups(String type) {
      return type.equals(GUIDANCE_GROUPS);
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
      return TYPE_ICONS.containsKey(type) ? TYPE_ICONS.get(type) : 0;
   }

   /**
    * Return the icon resource id representing ObjectGraph data type.
    *
    * @param objectGraph   the object graph
    * @return  the icon resource id
    */
   public static int getIcon(ObjectGraph objectGraph) {
      if (hasSubtype(objectGraph)) {
         return getSubtypeIcon(objectGraph);
      }

      return getIcon(objectGraph.getType());
   }

   /**
    * Return true if ObjectGraph has sub type defined in its map data.
    *
    * @param objectGraph   the object graph
    * @return  true if ObjectGraph has sub type defined in its map data.
    */
   public static boolean hasSubtype(ObjectGraph objectGraph) {
      return objectGraph.getData() != null && objectGraph.hasData(SUB_TYPE);
   }

   /**
    * If ObjectGraph has sub type, return icon resource id assigned to sub type.
    *
    * @param objectGraph   the object graph
    * @return  resource id for icon. 0 if no sub type icon found.
    */
   public static int getSubtypeIcon(ObjectGraph objectGraph) {
      if (hasSubtype(objectGraph)) {
         if (DataTypes.GUIDANCE_GROUP.equals(objectGraph.getType())) {
            int subType = objectGraph.getDataInt(SUB_TYPE);
            SwathType sType = SwathType.findByValue(subType);
            return SWATH_ICONS.containsKey(sType) ? SWATH_ICONS.get(sType) : 0;
         }
         else if (DataTypes.PRODUCT.equals(objectGraph.getType())) {
            int form = objectGraph.getDataInt(SUB_TYPE);
            ProductForm pForm = ProductForm.findByValue(form);
            return PRODUCTFORM_ICONS.containsKey(pForm) ? PRODUCTFORM_ICONS.get(pForm) : 0;
         }
      }
      return 0;
   }

   /**
    * @param type the ObjectGraph type
    * @return is there an icon for this type
    */
   public static boolean hasIcon(String type) {
      return TYPE_ICONS.containsKey(type);
   }

   /**
    * Create UI Tree using json file.
    * @param context
    * @param builder
    */
   public static void LoadDMTreeFromJson(Context context, TreeBuilder<ObjectGraph> builder) {
      InputStream inputStream = null;
      InputStreamReader inputStreamReader = null;
      try {
         GroupObjectGraphMap.clear();
         NeedtoFindParentGroup.clear();
         inputStream = context.getAssets().open(("dmtree.json"));
         inputStreamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
         DMTreeJsonData[] jsondata = new Gson().fromJson(inputStreamReader, DMTreeJsonData[].class);

         for (DMTreeJsonData entry : jsondata) {
            addToTree(entry, null, builder);
         }
      }
      catch (IOException ioex) {
         logger.error("Unable to read the json file " + ioex.getMessage());
      }
      catch (JsonSyntaxException syexp) {
         logger.error("Syntax error in json file " + syexp.getMessage());
      }
      catch (JsonIOException jioexp) {
         logger.error("JSON IO Exception " + jioexp.getMessage());
      }
      finally {
         closeQuietly(inputStream);
         closeQuietly(inputStreamReader);
      }
   }

   /**
    * Quietly Close Resource
    * @param closeable  Resource to close
    */
   private static void closeQuietly(Closeable closeable) {
      try {
         if (closeable != null) {
            closeable.close();
         }
      }
      catch (Exception ex) {
         logger.error("Exception during Resource.close()", ex);
      }
   }

   /**
    * Helper function to add objects in tree.
    * @param entry
    * @param parent
    * @param builder
    */
   public static void addToTree(DMTreeJsonData entry, GroupObjectGraph parent, TreeBuilder<ObjectGraph> builder) {
      if (entry != null) {
         GroupObjectGraph gGroup = new GroupObjectGraph(null, entry.getGroupDataType(), entry.getTitle(), null, parent);
         gGroup.setId(UUID.randomUUID().toString());
         gGroup.addData(HIDDEN_ITEM, entry.getHidden());
         gGroup.addData(FOLLOW_SOURCE, entry.getFollowSource());
         GroupObjectGraphMap.put(entry.getDataType(), gGroup);
         if ((entry.getFollowSource() == 1) && (entry.getHidden() == 0)) {
            builder.bAddRelation(gGroup.getParent(), gGroup);
            if (entry.getChildren() != null) {
               for (DMTreeJsonData childEntry : entry.getChildren()) {
                  addToTree(childEntry, gGroup, builder);
               }
            }
         }
      }
   }

   /**
    * Get group object when provided with data type.
    * @param dataType
    * @return  group object graph
    */
   public static GroupObjectGraph findGroupNode(String dataType) {
      GroupObjectGraph retNode = null;
      if (GroupObjectGraphMap.containsKey(dataType)) {
         retNode = GroupObjectGraphMap.get(dataType);
      }
      return retNode;
   }

   public static GroupObjectGraph findParentNeededGroup(String objId, String dataType) {
      GroupObjectGraph retNode = null;
      if (NeedtoFindParentGroup.containsKey(objId)) {
         retNode = NeedtoFindParentGroup.get(objId);
         if (retNode.getType().equals(dataType) == false) {
            retNode = null;
         }
      }
      return retNode;

   }

   public static void addToParentNeededGroup(String objId, GroupObjectGraph node) {
      NeedtoFindParentGroup.put(objId, node);
   }

   public static void UpdateChidrenCount(GroupObjectGraph gNode) {
      if ((gNode != null) && (!gNode.hasData(CHILDREN_COUNT))) {
         gNode.addData(CHILDREN_COUNT, 1);
      }
   }
}
