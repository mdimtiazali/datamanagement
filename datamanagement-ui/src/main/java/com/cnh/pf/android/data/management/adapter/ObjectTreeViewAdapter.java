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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TreeEntityHelper;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;

/**
 * Adapter feeds data to TreeView
 * Created by oscar.salazar@cnhind.com
 */
public abstract class ObjectTreeViewAdapter extends SelectionTreeViewAdapter<ObjectGraph> {
   private static final Logger logger = LoggerFactory.getLogger(ObjectTreeViewAdapter.class);

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

      ObjectGraph node = obj.copy();
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
      nameView.setTextColor(getActivity().getResources().getColorStateList(R.color.tree_text_color));
      if(TreeEntityHelper.hasIcon(graph.getType()) && (graph instanceof GroupObjectGraph || !isGroupableEntity(graph)) ) {
         nameView.setCompoundDrawablesWithIntrinsicBounds(TreeEntityHelper.getIcon(graph.getType()), 0, 0, 0);
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
