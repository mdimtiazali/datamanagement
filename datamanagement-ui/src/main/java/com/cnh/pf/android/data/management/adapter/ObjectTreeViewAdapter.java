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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TreeEntityHelper;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.google.common.base.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import pl.polidea.treeview.InMemoryTreeNode;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

/**
 * Adapter feeds data to TreeView
 * Created by oscar.salazar@cnhind.com
 */
public abstract class ObjectTreeViewAdapter extends SelectionTreeViewAdapter<ObjectGraph> {

   private List<ObjectGraph> data;

   public ObjectTreeViewAdapter(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);

   }

   protected abstract boolean isGroupableEntity(ObjectGraph node);

   /**
    * Make a copy of this object traversing down (parent is not copied)
    * Filter only objects which match predicate
    *
    * @param obj       the object to filter
    * @param predicate the predicate
    * @return copy of this object with children filtered, could be null
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

   private boolean filterPredicate(ObjectGraph obj, SelectionType... types) {
      return getSelectionMap().containsKey(obj) && isSupportedEntitiy(obj) && (Arrays.binarySearch(types, getSelectionMap().get(obj)) >= 0);
   }

   // Check input node itself and children nodes recursively for selection type
   private boolean filterPredicateRecursive(ObjectGraph obj, SelectionType... types) {
      if (filterPredicate(obj, types)) return true;
      else {
         if (obj.getChildren() == null) return false;

         for (ObjectGraph child : obj.getChildren()) {
            if (filterPredicateRecursive(child, types)) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Make a copy of this object traversing down (parent is not copied)
    * Filter only selected objects
    *
    * @param obj   the object to filter
    * @param types the acceptible {@link SelectionType}(s)
    * @return copy of this object with children
    */
   public ObjectGraph filterSelected(ObjectGraph obj, final SelectionType... types) {
      return filter(obj, new Predicate<ObjectGraph>() {
         @Override
         public boolean apply(@Nullable ObjectGraph input) {
            return filterPredicateRecursive(input, types);
         }
      });
   }

   @Override
   public boolean includeParent(ObjectGraph id) {
      if (id.getType().equals(DataTypes.FILE)) {
         return true;
      }
      return false;
   }

   @Override
   public View getNewChildView(TreeNodeInfo<ObjectGraph> treeNodeInfo) {
      final TextView view = (TextView) getActivity().getLayoutInflater().inflate(R.layout.tree_list_item_simple, null);
      return updateView(view, treeNodeInfo);
   }

   @Override
   public View updateView(View view, TreeNodeInfo treeNodeInfo) {
      if (view instanceof TextView) {
         final TextView nameView = (TextView) view;
         ObjectGraph graph = (ObjectGraph) treeNodeInfo.getId();
         nameView.setText(graph.getName());
         nameView.setTextColor(getActivity().getResources().getColorStateList(R.color.tree_text_color));
         if (TreeEntityHelper.hasIcon(graph.getType()) && (graph instanceof GroupObjectGraph || !isGroupableEntity(graph))) {
            nameView.setCompoundDrawablesWithIntrinsicBounds(TreeEntityHelper.getIcon(graph.getType()), 0, 0, 0);
         }
         else {
            nameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
         }
      }
      return view;
   }

   @Override
   public long getItemId(int position) {
      return position;
   }

   /**
    * Save all node data for tree
    *
    * @param data Object Data
    */
   public void setData(List<ObjectGraph> data) {
      this.data = data;
   }

   /**
    * Grab all Tree View origin data
    *
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
    *
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
   /**
    * Get Set of selected root ObjectGraphs, it won't check the children node
    *
    * @return Set<ObjectGraph>
    */
   public Set<ObjectGraph> getSelectedRootNodes(){
      Set<ObjectGraph> set = new HashSet<ObjectGraph>();
      Map<ObjectGraph, SelectionType> allSelectedObj = getSelectionMap();
      if(!allSelectedObj.isEmpty()){
         Iterator<Map.Entry<ObjectGraph,SelectionType>> it = allSelectedObj.entrySet().iterator();
         while(it.hasNext()){
            ObjectGraph objectGraph = it.next().getKey();
            if(objectGraph.getParent() == null || !allSelectedObj.containsKey(objectGraph.getParent())){
               set.add(objectGraph);
            }
         }
      }
      return set;
   }
   /**
    * adding an objectGraph to the tree
    *
    * @param objectGraph Object Data
    */
   public void addObjectGraph(final ObjectGraph objectGraph){
      if(objectGraph != null){
         final ObjectGraph parent = objectGraph.getParent();
         List<ObjectGraph> objectGraphs = getData();
         if(objectGraphs != null) {
            if (parent == null) {
               objectGraphs.add(objectGraph);
            } else {
               for (ObjectGraph obj : objectGraphs) {
                  ObjectGraph.traverse(obj, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
                        @Override
                        public boolean visit(ObjectGraph node) {
                           if (node.equals(parent)) {
                              node.addChild(objectGraph);
                              return false;
                           }
                           return true;
                        }
                     });
                  }
            }
         }
      }
   }
   /**
    * adding objectGraphs to the tree
    *
    * @param objectGraphs Object Data
    */
   public void addObjectGraphs(final List<ObjectGraph> objectGraphs){
      if(objectGraphs != null && !objectGraphs.isEmpty()){
         for(ObjectGraph objectGraph: objectGraphs){
            addObjectGraph(objectGraph);
         }
      }
   }
   /**
    * remove ObjectGraph list from data
    *
    */
   public void removeObjectGraphs(final List<ObjectGraph> objects){
      if(objects != null && !objects.isEmpty()){
         for(ObjectGraph o: objects){
            removeObjectGraph(o);
         }
      }
      resetSelectedMap();
   }
   /**
    * remove ObjectGraph from data
    *
    */
   public void removeObjectGraph(final ObjectGraph objectGraph){
      final List<ObjectGraph> objs = new ArrayList<ObjectGraph>();
      List<ObjectGraph> objectGraphs = getData();
      if(objectGraph != null) {
         if (objectGraphs != null) {
            for (ObjectGraph obj : objectGraphs) {
               ObjectGraph.traverse(obj, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
                  @Override
                  public boolean visit(ObjectGraph node) {
                     if (node.equals(objectGraph)) {
                        objs.add(node);
                        return false;
                     }
                     return true;
                  }
               });
               if(objs.size() > 0){
                  break;
               }
            }
         }
      }
      if(!objs.isEmpty()){
         ObjectGraph parent = objs.get(0).getParent();
         if(parent != null){
            parent.getChildren().remove(objectGraph);
         }
         else if(objectGraphs.contains(objectGraph)){
            objectGraphs.remove(objectGraph);
         }
      }
   }
   private void resetSelectedMap(){
      if(!getSelectionMap().isEmpty()){
         getSelectionMap().clear();
      }
   }
   /**
    * Update a specific item view
    *
    * @param treeViewList listview to update its item view
    * @param position position item view to update
    */
   public void updateItemView(TreeViewList treeViewList, int position) {
      if (treeViewList != null) {
         int firstVisiblePosition = treeViewList.getFirstVisiblePosition();
         int lastVisiblePosition = treeViewList.getLastVisiblePosition();

         if (position >= firstVisiblePosition && position <= lastVisiblePosition) {
            View view = treeViewList.getChildAt(position - firstVisiblePosition);
            final ImageView toggle = (ImageView) view.findViewById(R.id.treeview_list_item_toggle);
            final LinearLayout frameLayout = (LinearLayout) view.findViewById(R.id.treeview_list_item_frame_layout);
            final ImageButton editBtn = (ImageButton) view.findViewById(R.id.mng_edit_button);
            final ImageButton copyBtn = (ImageButton) view.findViewById(R.id.mng_copy_button);
            final TextView textView = (TextView) view.findViewById(R.id.tree_list_item_text);
            if (textView != null) textView.setText(getTreeId(position).getName());
            if (toggle != null) toggle.setTag(getTreeId(position));
            if (frameLayout != null) frameLayout.setTag(getTreeId(position));
            if (editBtn != null) editBtn.setTag(getTreeId(position));
            if (copyBtn != null) copyBtn.setTag(getTreeId(position));
         }
      }
   }

   /**
    * Remove and return the specific node
    *
    * @param id
    * @return InMemoryTreeNode<T>
    */
   public InMemoryTreeNode<ObjectGraph> rmNretNode(ObjectGraph id) {
      return (InMemoryTreeNode<ObjectGraph>) getManager().rmNretNode(id);
   }

   /**
    * add the specific node
    *
    * @param node
    */
   public void addNote(InMemoryTreeNode<ObjectGraph> node) {
      getManager().addNote(node);
   }

   /**
    * Remove and return the specific node
    *
    * @param id
    * @param newName
    */
   public void updateNodeName(ObjectGraph id, String newName) {
      final List<ObjectGraph> list = new LinkedList<ObjectGraph>();
      //remove from selection pool first
      if (getSelectionMap().containsKey(id)) {
         traverseTree(id, ObjectGraph.TRAVERSE_DOWN, new Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph node) {
               if (getSelectionMap().containsKey(node)) {
                  list.add(node);
                  getSelectionMap().remove(node);
                  return true;
               }
               return false;
            }
         });
      }

      InMemoryTreeNode<ObjectGraph> inmemTreeNode = rmNretNode(id);
      inmemTreeNode.getId().setName(newName);
      //after update, put it back to selection map
      for (ObjectGraph obj : list) {
         getSelectionMap().put(obj, SelectionType.FULL);
      }
      addNote(inmemTreeNode);
   }

}
