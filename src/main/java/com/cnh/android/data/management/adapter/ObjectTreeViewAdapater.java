/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.android.data.management.adapter;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cnh.android.data.management.R;
import com.cnh.jgroups.Datasource;
import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.ImplicitSelectLinearLayout;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import com.cnh.jgroups.ObjectGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.polidea.treeview.TreeViewList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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
public class ObjectTreeViewAdapater extends AbstractTreeViewAdapter<ObjectGraph> {
   private static final Logger logger = LoggerFactory.getLogger(ObjectTreeViewAdapater.class);

   public enum SelectionType {
      FULL, IMPLICIT
   }

   private static final Map<String, Integer> TYPE_ICONS = new HashMap<String, Integer>();

   static {
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Customer", R.drawable.ic_datatree_grower);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Farm", R.drawable.ic_datatree_farm);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Field", R.drawable.ic_datatree_field);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Task", R.drawable.ic_datatree_tasks);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Prescription", R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.RxPlan", R.drawable.ic_datatree_prescription);
      TYPE_ICONS.put(Datasource.DataType.PFDS.name(), 0);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Prescription_Group", 0);
      TYPE_ICONS.put("com.cnh.pf.model.pfds.Task_Group", 0);
   }

   private final Map<ObjectGraph, SelectionType> selectionMap;
   private List<ObjectGraph> data;

   public ObjectTreeViewAdapater(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);
      selectionMap = new HashMap<ObjectGraph, SelectionType>();

   }

   /**
    * Makes view state match selection state
    * @param parent  the tree view
    */
   private void updateViewSelection(final AdapterView< ? > parent) {
      logger.debug("Updating View Selection");
      for(int i=0; i<parent.getChildCount(); i++) {
         View child = parent.getChildAt(i);
         ObjectGraph node = (ObjectGraph) child.getTag(); //tree associates ObjectGraph with each view
         if(node==null) continue;
         ImplicitSelectLinearLayout layout = (ImplicitSelectLinearLayout) child;
         if(selectionMap.containsKey(node)) {
            SelectionType type = selectionMap.get(node);
            child.setSelected(SelectionType.FULL.equals(type));
            layout.setImplicitlySelected(SelectionType.IMPLICIT.equals(type));
         } else {
            child.setSelected(false);
            layout.setImplicitlySelected(false);
         }
      }
   }

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
         if(selectionMap.containsKey(child)) {
            if(Arrays.binarySearch(types, selectionMap.get(child))>=0) {
               node.addChild(filterSelected(child, types));
            }
         }
      }
      return node;
   }

   @Override
   public void handleItemClick(final AdapterView< ? > parent, View view, final int position, Object id) {
      super.handleItemClick(parent, view, position, id);

      ((TreeViewList)parent).setOnScrollListener(new AbsListView.OnScrollListener() {
         @Override
         public void onScrollStateChanged(AbsListView view, int scrollState) {

         }

         @Override
         public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            updateViewSelection(view);
         }
      });
      parent.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
         @Override
         public void onChildViewAdded(View p, View child) {
            updateViewSelection(parent);
         }

         @Override
         public void onChildViewRemoved(View p, View child) {
            updateViewSelection(parent);
         }
      });

      ObjectGraph obj = (ObjectGraph)id;
      logger.debug("handleItemClick: " + obj);
      if(obj.getType().endsWith("_Group")) return; //TODO how to handle group selection, now it is ignored

      //update selectionMap
      SelectionType type = selectionMap.get(obj);
      if(!selectionMap.containsKey(obj) ||
            SelectionType.IMPLICIT.equals(type)) {
         //traverse down mark as FULL
         ObjectGraph.traverse(obj, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph node) {
               selectionMap.put(node, SelectionType.FULL);
               return true;
            }
         });
         //traverse up mark unselected as IMPLICIT
         if(obj.getParent()!=null) {
            ObjectGraph.traverse(obj.getParent(), ObjectGraph.TRAVERSE_UP, new ObjectGraph.Visitor<ObjectGraph>() {
               @Override
               public boolean visit(ObjectGraph node) {
                  if(selectionMap.get(node)==null) {
                     selectionMap.put(node, SelectionType.IMPLICIT);
                  }
                  return true;
               }
            });
         }
      } else {
         //traverse down mark as unselected
         ObjectGraph.traverse(obj, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph node) {
               selectionMap.remove(node);
               return true;
            }
         });
         //traverse up mark IMPLICIT as unselected. stop at first FULL parent
         if(obj.getParent()!=null) {
            ObjectGraph.traverse(obj.getParent(), ObjectGraph.TRAVERSE_UP, new ObjectGraph.Visitor<ObjectGraph>() {
               @Override
               public boolean visit(ObjectGraph node) {
                  SelectionType currentType = selectionMap.get(node);
                  if(SelectionType.IMPLICIT.equals(currentType)) {
                     selectionMap.remove(node);
                  }
                  return !SelectionType.FULL.equals(currentType); //stop traversing up if this is fully selected node
               }
            });
         }
      }
      updateViewSelection(parent);
   }

   @Override
   public View getNewChildView(TreeNodeInfo treeNodeInfo) {
      final TextView view = (TextView) getActivity()
            .getLayoutInflater().inflate(R.layout.tree_list_item_simple, null);
      return updateView(view, treeNodeInfo);
   }

   @Override
   public View updateView(View view, TreeNodeInfo treeNodeInfo) {
      final TextView nameView = (TextView) view;
      ObjectGraph graph = (ObjectGraph) treeNodeInfo.getId();
      nameView.setText(graph.getName());
      nameView.setCompoundDrawablesWithIntrinsicBounds(TYPE_ICONS.get(graph.getType()), 0, 0, 0);
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
         if(!selectionMap.containsKey(root)) continue;
         //filter out unselected nodes from selected parents
         ObjectGraph filtered = filterSelected(root, SelectionType.FULL, SelectionType.IMPLICIT); //starting at top level, any selected nodes
         //find root level nodes FULL selected with IMPLICIT/null parent
         ObjectGraph.traverse(filtered, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph node) {
               boolean isFullSelected = SelectionType.FULL.equals(selectionMap.get(node));
               boolean parentNotFull = node.getParent()==null || !SelectionType.FULL.equals(selectionMap.get(node.getParent()));
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
