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

import com.android.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.ImplicitSelectLinearLayout;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

/**
 * TreeViewAdapter with functionality to full/partially select child/parent objects in tree
 * @author oscar.salazar@cnhind.com
 */
public abstract class SelectionTreeViewAdapter<T> extends AbstractTreeViewAdapter<T> {
   /** Notify listener of object selected/unselected*/
   public interface OnTreeItemSelectedListener {
      void onItemSelected();
   }

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
   private OnTreeItemSelectedListener listener;

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

   /**
    * Get the selection map
    * @return Map
    */
   public Map<T, SelectionType> getSelectionMap() {
      return selectionMap;
   }

   /**
    * Get tree item selected listener
    */
   public OnTreeItemSelectedListener getOnTreeItemListener() {
      return listener;
   }

   /**
    * Set tree item selected listener
    */
   public void setOnTreeItemSelectedListener(OnTreeItemSelectedListener listener) {
      this.listener = listener;
   }

   /**
    * Should the tree item explicitly include its parent
    */
   public boolean includeParent(T id) {
      return false;
   }

   public void selectionImpl(Object id) {
      if (!selectionMap.containsKey(id) || SelectionType.IMPLICIT.equals(selectionMap.get(id))) {
         //Traverse down, select everything
         traverseTree((T) id, TRAVERSE_DOWN, new Visitor<T>() {
            @Override
            public boolean visit(T node) {
               selectionMap.put(node, SelectionType.FULL);
               return true;
            }
         });
         //Traverse up, implicitly select everything, check for full selections
         if (getManager().getParent((T) id) != null && childSelectionDoesImplicitSelectParentNodes(getRootNode((T) id))) {
            traverseTree(getManager().getParent((T) id), TRAVERSE_UP, new Visitor<T>() {
               @Override
               public boolean visit(T node) {
                  if (selectionMap.get(node) == null) {
                     selectionMap.put(node, SelectionType.IMPLICIT);
                  }
                  return true;
               }
            });

            if (includeParent((T) id)) {
               selectionMap.put(getManager().getParent((T) id), SelectionType.FULL);
            }
         }
      }
      else {
         //unselect self and everything below
         traverseTree((T) id, TRAVERSE_DOWN, new Visitor<T>() {

            @Override
            public boolean visit(T node) {
               selectionMap.remove(node);
               return true;
            }
         });
         if (includeParent((T) id)) {
            selectionMap.put(getManager().getParent((T) id), SelectionType.IMPLICIT);
         }
         //Traverse up, and unselect implicit parent if this is the only selected item
         if (getManager().getParent((T) id) != null) {
            traverseTree(getManager().getParent((T) id), TRAVERSE_UP, new Visitor<T>() {
               @Override
               public boolean visit(T node) {
                  boolean useImplicitSelection = childSelectionDoesImplicitSelectParentNodes(getRootNode((T) node));
                  //Determine state of child nodes
                  boolean hasSelectedChildren = false;
                  boolean hasImplicitSelectedChildren = false;
                  //Note: At least one child (or child of child..) is not selected
                  //      since this traversal is a result of an deselect event.
                  for (T child : getManager().getChildren(node)) {
                     if (selectionMap.containsKey(child)) {
                        if (SelectionType.FULL.equals(selectionMap.get(child))) {
                           hasSelectedChildren = true;
                        }
                        else {
                           hasImplicitSelectedChildren = true;
                        }
                        //break loop if possible (performance only)
                        if (hasSelectedChildren && hasImplicitSelectedChildren) {
                           break;
                        }
                     }
                  }
                  //modify selection state depending on the state of the childnodes
                  boolean modificationDone = false;
                  if (!hasSelectedChildren && !hasImplicitSelectedChildren) {
                     //no children is selected at all: unselect node
                     selectionMap.remove(node);
                     modificationDone = true;
                  }
                  else {
                     //at least one child is selected (full or implicit)
                     if (SelectionType.FULL.equals(selectionMap.get(node))) {
                        //if at least one of the children is not fully selected (type!=full) an update is required
                        modificationDone = true;
                        if (useImplicitSelection) {
                           //update from full to implicit
                           selectionMap.put(node, SelectionType.IMPLICIT);
                        }
                        else {
                           //remove selection in total
                           selectionMap.remove(node);
                        }
                     }
                  }
                  return modificationDone;
               }
            });

         }
      }

   }

   /**
    * This method defines, if the selection of one child node of the given root node
    * results in the selection of all parent nodes of the selected child node
    *
    * @param rootNode Root node of all affected child nodes
    * @return True: Selection of one random child node of root node will select all of its root nodes, False: no parent node will be selected
    */
   protected boolean childSelectionDoesImplicitSelectParentNodes(@Nullable T rootNode) {
      return false;
   }

   /**
    * This function returns the root node of any given node. This is calculated recursively.
    *
    * @param currentNode Node of which the root node should be found.
    * @return The root node of the currentNode. May be null if currentNode itself is null!
    */
   @Nullable
   protected T getRootNode(@Nullable T currentNode) {
      T parentNode = getManager().getParent(currentNode);
      if (parentNode == null) {
         //if no parent node is available, the current node must be the root node
         return currentNode;
      }
      else {
         //if there is a parent node, its parent node may be the root node
         return getRootNode(parentNode);
      }
   }

   @Override
   public void handleItemClick(final AdapterView<?> parent, View view, int position, Object id) {
      super.handleItemClick(parent, view, position, id);
      setListeners(parent);
      selectionImpl(id);
      updateViewSelection(parent);
      if (listener != null) {
         listener.onItemSelected();
      }
   }

   public abstract boolean isSupportedEntitiy(T node);

   public boolean isSupportedEdit(T node) {
      return true;
   }

   public boolean isSupportedCopy(T node) {
      return true;
   }

   /**
    * Makes view state match selection state
    * @param parent  the tree view
    */
   public void updateViewSelection(final AdapterView<?> parent) {
      for (int i = 0; i < parent.getChildCount(); i++) {
         View child = parent.getChildAt(i);
         T node = (T) child.getTag(); //tree associates ObjectGraph with each view
         if (node == null) continue;
         ImplicitSelectLinearLayout layout = (ImplicitSelectLinearLayout) child;
         layout.setSupported(isSupportedEntitiy(node));
         if (selectionMap.containsKey(node)) {
            SelectionType type = selectionMap.get(node);
            layout.setSelected(SelectionType.FULL.equals(type));
            layout.setImplicitlySelected(SelectionType.IMPLICIT.equals(type));
         }
         else {
            layout.setSelected(false);
            layout.setImplicitlySelected(false);
         }
      }
   }

   private void setListeners(final AdapterView<?> parent) {
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
      ((TreeViewList) parent).setOnScrollListener(new AbsListView.OnScrollListener() {
         @Override
         public void onScrollStateChanged(AbsListView view, int scrollState) {

         }

         @Override
         public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            updateViewSelection(view);
         }
      });
   }

   /**
    * Sets all objects in tree to full selection or clears depending on selectAll
    */
   public void selectAll(AdapterView<?> parent, boolean selectAll) {
      //We can use shortcut by clearing selectionMap, and full selection of root objects
      selectionMap.clear();
      setListeners(parent);
      if (selectAll) {
         for (T graph : getManager().getChildren(null)) {
            traverseTree(graph, TRAVERSE_DOWN, new Visitor<T>() {
               @Override
               public boolean visit(T node) {
                  selectionMap.put(node, SelectionType.FULL);
                  return true;
               }
            });
         }
      }
      updateViewSelection(parent);
      if (listener != null) {
         listener.onItemSelected();
      }
   }

   /**
    * Determines if all nodes in tree are fully selected.
    */
   class SelectAllVisitor implements Visitor<T> {
      boolean result = true;

      @Override
      public boolean visit(T node) {
         if (!SelectionType.FULL.equals(selectionMap.get(node))) {
            result = false;
            return false;
         }
         return true;
      }

      public boolean getResult() {
         return result;
      }
   }

   /**
    * Are all nodes fully selected?
    * @return
    */
   public boolean areAllSelected() {
      SelectAllVisitor v = new SelectAllVisitor();
      for (T graph : getManager().getChildren(null)) {
         traverseTree(graph, TRAVERSE_DOWN, v);
      }
      return v.getResult();
   }

   public boolean hasSelection() {
      for (Map.Entry<T, SelectionType> entry : getSelectionMap().entrySet()) {
         if (isSupportedEntitiy(entry.getKey())) return true;
      }
      return false;
   }
}
