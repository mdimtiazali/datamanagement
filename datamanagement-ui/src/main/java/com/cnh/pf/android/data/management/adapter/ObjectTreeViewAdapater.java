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
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.jgroups.Datasource;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import com.cnh.jgroups.ObjectGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter feeds data to TreeView
 * Created by oscar.salazar@cnhind.com
 */
public abstract class ObjectTreeViewAdapater extends SelectionTreeViewAdapter<ObjectGraph> {
   private static final Logger logger = LoggerFactory.getLogger(ObjectTreeViewAdapater.class);

   private static final Map<String, Integer> TYPE_ICONS = new HashMap<String, Integer>();

   static {
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Customer", R.drawable.ic_datatree_grower);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Farm", R.drawable.ic_datatree_farm);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Field", R.drawable.ic_datatree_field);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Task", R.drawable.ic_datatree_tasks);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Prescription", R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.RxPlan", R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put(Datasource.DataType.PFDS.name(), 0);
   }

   private List<ObjectGraph> data;

   public ObjectTreeViewAdapater(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);

   }

   protected abstract boolean isGroupableEntity(ObjectGraph node);

   /**
    * Make a copy of this object traversing down (parent is not copied)
    * Filter only selected objects
    * @param obj  the object to filter
    * @param types   the acceptible {@link SelectionType}(s)
    * @return  copy of this object with children
    */
   public ObjectGraph filterSelected(ObjectGraph obj, SelectionType...types) {
      ObjectGraph node = new ObjectGraph(obj.getSource(), obj.getType(), obj.getName(), new HashMap<String, String>(obj.getData()), null);
      for(ObjectGraph child : obj.getChildren()) {
         if(getSelectionMap().containsKey(child)) {
            if(Arrays.binarySearch(types, getSelectionMap().get(child))>=0) {
               node.addChild(filterSelected(child, types));
            }
         }
      }
      return node;
   }

   @Override
   public View getNewChildView(TreeNodeInfo<ObjectGraph> treeNodeInfo) {
      final TextView view = (TextView) getActivity()
            .getLayoutInflater().inflate(R.layout.tree_list_item_simple, null);
      return updateView(view, treeNodeInfo);
   }

   @Override
   public View updateView(View view, TreeNodeInfo treeNodeInfo) {
      final TextView nameView = (TextView) view;
      ObjectGraph graph = (ObjectGraph) treeNodeInfo.getId();
      nameView.setText(graph.getName());
      nameView.setCompoundDrawablesWithIntrinsicBounds(((graph instanceof GroupObjectGraph || !isGroupableEntity(graph)) ? TYPE_ICONS.get(graph.getType()) : 0), 0, 0, 0);
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
      for(ObjectGraph root : data) {
         if(!getSelectionMap().containsKey(root)) continue;
         //filter out unselected nodes from selected parents
         ObjectGraph filtered = filterSelected(root, SelectionType.FULL, SelectionType.IMPLICIT); //starting at top level, any selected nodes
         //find root level nodes FULL selected with IMPLICIT/null parent
         ObjectGraph.traverse(filtered, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph node) {
               boolean isFullSelected = SelectionType.FULL.equals(getSelectionMap().get(node));
               boolean parentNotFull = node.getParent()==null || !SelectionType.FULL.equals(getSelectionMap().get(node.getParent()));
               if(isFullSelected && parentNotFull)
                  stage1.add(node);
               return true;
            }
         });
      }
      //refilter final output to seperate multiple root-nodes within a single tree (Grower(FULL)->Farm(IMPLICIT)->Field(FULL))
      //only accept FULL selections below the root element
      HashSet<ObjectGraph> selected = new HashSet<ObjectGraph>();
      for(ObjectGraph root : stage1) {
         ObjectGraph filtered = filterSelected(root, SelectionType.FULL); //starting at top level, only FULL selected nodes
         //this stripped the parents, so we need to put them back
         ObjectGraph parent = root.getParent().copyUp();
         parent.addChild(filtered);
         selected.add(filtered);
      }
      return selected;
   }
}
