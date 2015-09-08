/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.ImplicitSelectLinearLayout;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TreeViewAdapter with functionality to full/partially select child/parent objects in tree
 * @author oscar.salazar@cnhind.com
 */
public abstract class SelectionTreeViewAdapter<T> extends AbstractTreeViewAdapter<T> {

   public interface Visitor<T> {
      /**
       * visit the node.
       * @param node the node
       * @return  whether to continue traversal [true, false]
       */
      boolean visit(T node);
   }

   public static final int TRAVERSE_UP = 1;
   public static final int TRAVERSE_DOWN = 2;

   public enum SelectionType {
      FULL, IMPLICIT
   }

   private Map<T, SelectionType> selectionMap;

   public SelectionTreeViewAdapter(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);
      selectionMap = new ConcurrentHashMap<T, SelectionType>();
   }

   /*
   Traverse UP/DOWN T id and perform invoke visit at each level
    */
   protected void traverseTree(T id, int directions, Visitor<T> visitor) {
      if (!visitor.visit(id)) return;

      if ((directions & TRAVERSE_UP) == TRAVERSE_UP && getManager().getParent(id) != null) {
         traverseTree(getManager().getParent(id), TRAVERSE_UP, visitor);
      }
      if ((directions & TRAVERSE_DOWN) == TRAVERSE_DOWN) {
         for (T child : getManager().getChildren(id)) {
            traverseTree(child, TRAVERSE_DOWN, visitor);
         }
      }
   }

   @Override
   public void handleItemClick(final AdapterView<?> parent, View view, int position, Object id) {
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

      if (!selectionMap.containsKey(id)) {
         //Traverse down, select everything
         traverseTree((T) id, TRAVERSE_DOWN, new Visitor<T>() {
            @Override
            public boolean visit(T node) {
//               itemSelectionChange(node, SelectionType.FULL);
               selectionMap.put(node, SelectionType.FULL);
               return true;
            }
         });
         //Traverse up, implicitly select everything, check for full selections
         if (getManager().getParent((T) id) != null) {
            traverseTree(getManager().getParent((T) id), TRAVERSE_UP, new Visitor<T>() {
               @Override
               public boolean visit(T node) {
                  if (selectionMap.get(node) == null) {
                     selectionMap.put(node, SelectionType.IMPLICIT);
                  }
                  boolean hasAllChildrenSelected = true;
                  for (T child : getManager().getChildren(node)) {
                     if (!selectionMap.containsKey(child) || selectionMap.get(child).equals(SelectionType.IMPLICIT) ) {
                        hasAllChildrenSelected = false;
                        break;
                     }
                  }
                  if (hasAllChildrenSelected) selectionMap.put(node, SelectionType.FULL);
                  return true;
               }
            });
         }
      }
      else {
         //unselect everything below
         traverseTree((T) id, TRAVERSE_DOWN, new Visitor<T>() {
            @Override
            public boolean visit(T node) {
               selectionMap.remove(node);
               return true;
            }
         });
         //Traverse up, and unselect parent if this is the only selected item
         traverseTree((T) id, TRAVERSE_UP, new Visitor<T>() {
            @Override
            public boolean visit(T node) {
               boolean hasOtherSelectedChildren = false;
               for (T child : getManager().getChildren(node)) {
                  if (selectionMap.containsKey(child)) {
                     hasOtherSelectedChildren = true;
                     break;
                  }
               }
               if (hasOtherSelectedChildren) {
                  selectionMap.put(node, SelectionType.IMPLICIT);
               }
               else {
                  selectionMap.remove(node);
               }
               return true;
            }
         });
      }
      updateViewSelection(parent);
   }

   public Map<T, SelectionType> getSelectionMap() {
      return selectionMap;
   }

   /**
    * Makes view state match selection state
    * @param parent  the tree view
    */
   protected void updateViewSelection(final AdapterView< ? > parent) {
      for(int i=0; i<parent.getChildCount(); i++) {
         View child = parent.getChildAt(i);
         T node = (T) child.getTag(); //tree associates ObjectGraph with each view
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
    * Sets all objects in tree to full selection
    */
   public void selectAll(AdapterView<?> parent) {
      //We can use shortcut by clearing selectionMap, and full selection of root objects
      selectionMap.clear();
      for (T graph : getManager().getChildren(null)) {
         traverseTree(graph, TRAVERSE_DOWN, new Visitor<T>() {
            @Override public boolean visit(T node) {
               selectionMap.put(node, SelectionType.FULL);
               return true;
            }
         });
      }
      updateViewSelection(parent);
   }
}
