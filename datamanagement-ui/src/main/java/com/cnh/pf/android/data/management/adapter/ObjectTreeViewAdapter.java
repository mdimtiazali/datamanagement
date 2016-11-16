/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.adapter;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.pf.model.TypedValue;
import com.google.common.base.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;

/**
 * Adapter feeds data to TreeView
 * Created by oscar.salazar@cnhind.com
 */
public abstract class ObjectTreeViewAdapter extends SelectionTreeViewAdapter<ObjectGraph> {
   private static final Logger logger = LoggerFactory.getLogger(ObjectTreeViewAdapter.class);

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
      TYPE_ICONS.put(DataTypes.GUIDANCE_GROUP, R.drawable.ic_datatree_swath);
      TYPE_ICONS.put(DataTypes.GUIDANCE_PATTERN, R.drawable.ic_datatree_swath);
      TYPE_ICONS.put(DataTypes.GUIDANCE_CONFIGURATION, R.drawable.ic_datatree_swath);
      TYPE_ICONS.put(DataTypes.COVERAGE, R.drawable.ic_datatree_boundaries);
      TYPE_ICONS.put(DataTypes.NOTE, R.drawable.ic_datatree_background_layers);
      TYPE_ICONS.put(DataTypes.FILE, R.drawable.ic_datatree_background_layers);
      TYPE_ICONS.put(DataTypes.VEHICLE, R.drawable.ic_datatree_copy);
      TYPE_ICONS.put(DataTypes.VEHICLE_IMPLEMENT, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.VEHICLE_IMPLEMENT_CONFIG, R.drawable.ic_datatree_background_layers);
      TYPE_ICONS.put(DataTypes.IMPLEMENT, R.drawable.ic_datatree_implements);
      TYPE_ICONS.put(DataTypes.IMPLEMENT_PRODUCT_CONFIG, R.drawable.ic_datatree_screenshots);
   }

   private List<ObjectGraph> data;

   public ObjectTreeViewAdapter(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);

   }

   protected abstract boolean isGroupableEntity(ObjectGraph node);

   /**
    * Make a copy of this object traversing down (parent is not copied)
    * Filter only objects which match predicate
    * @param obj  the object to filter
    * @param predicate   the predicate
    * @return  copy of this object with children filtered, could be null
    */
   public static ObjectGraph filter(ObjectGraph obj, Predicate<ObjectGraph> predicate) {
      if (!predicate.apply(obj)) return null;
      ObjectGraph node = new ObjectGraph(obj.getSources(), obj.getType(), obj.getId(), obj.getName(), new HashMap<String, TypedValue>(obj.getData()), null);
      for (ObjectGraph child : obj.getChildren()) {
         ObjectGraph fc = filter(child, predicate);
         if (fc != null) {
            node.addChild(fc);
         }
      }
      return node;
   }

   /**
    * Make a copy of this object traversing down (parent is not copied)
    * Filter only selected objects
    * @param obj  the object to filter
    * @param types   the acceptible {@link SelectionType}(s)
    * @return  copy of this object with children
    */
   public ObjectGraph filterSelected(ObjectGraph obj, final SelectionType... types) {
      return filter(obj, new Predicate<ObjectGraph>() {
         @Override
         public boolean apply(@Nullable ObjectGraph input) {
            return getSelectionMap().containsKey(input) && isSupportedEntitiy(input) && (Arrays.binarySearch(types, getSelectionMap().get(input)) >= 0);
         }
      });
   }

   @Override
   public View getNewChildView(TreeNodeInfo<ObjectGraph> treeNodeInfo) {
      final TextView view = (TextView) getActivity().getLayoutInflater().inflate(R.layout.tree_list_item_simple, null);
      return updateView(view, treeNodeInfo);
   }

   @Override
   public View updateView(View view, TreeNodeInfo treeNodeInfo) {
      final TextView nameView = (TextView) view;
      ObjectGraph graph = (ObjectGraph) treeNodeInfo.getId();
      nameView.setText(graph.getName());
      nameView.setTextColor(getActivity().getResources().getColorStateList(R.drawable.tree_text_color));
      if (TYPE_ICONS.containsKey(graph.getType()) && (graph instanceof GroupObjectGraph || !isGroupableEntity(graph))) {
         nameView.setCompoundDrawablesWithIntrinsicBounds(TYPE_ICONS.get(graph.getType()), 0, 0, 0);
      }
      else {
         nameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
      }
      return view;
   }

   @Override
   public long getItemId(int position) {
      return position;
   }

   /**
    * Save all node data for tree
    * @param data Object Data
    */
   public void setData(List<ObjectGraph> data) {
      this.data = data;
   }

   /**
    * Grab all Tree View origin data
    * @return Tree Origin Data
    */
   public List<ObjectGraph> getData() {
      return data;
   }

   @Override
   public Drawable getBackgroundDrawable(TreeNodeInfo<ObjectGraph> treeNodeInfo) {
      return getActivity().getResources().getDrawable(R.drawable.complex_selector_background);
   }

   /**
    * Get Set of selected ObjectGraphs
    * @return Set<ObjectGraph>
    */
   public Set<ObjectGraph> getSelected() {
      final List<ObjectGraph> stage1 = new LinkedList<ObjectGraph>();
      for (ObjectGraph root : data) {
         //filter out unselected nodes from selected parents
         ObjectGraph filtered = filterSelected(root, SelectionType.FULL, SelectionType.IMPLICIT); //starting at top level, any selected nodes
         if (filtered == null) continue;
         //find root level nodes FULL selected with IMPLICIT/null parent
         ObjectGraph.traverse(filtered, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph node) {
               boolean isFullSelected = SelectionType.FULL.equals(getSelectionMap().get(node));
               boolean parentNotFull = node.getParent() == null || !SelectionType.FULL.equals(getSelectionMap().get(node.getParent()));
               if (isFullSelected && parentNotFull) stage1.add(node);
               return true;
            }
         });
      }
      //refilter final output to seperate multiple root-nodes within a single tree (Grower(FULL)->Farm(IMPLICIT)->Field(FULL))
      //only accept FULL selections below the root element
      HashSet<ObjectGraph> selected = new HashSet<ObjectGraph>();
      for (ObjectGraph root : stage1) {
         ObjectGraph filtered = filterSelected(root, SelectionType.FULL); //starting at top level, only FULL selected nodes
         //this stripped the parents, so we need to put them back
         if (root.getParent() != null) {
            ObjectGraph parent = root.getParent().copyUp();
            parent.addChild(filtered);
         }
         selected.add(filtered);
      }
      return selected;
   }
}
