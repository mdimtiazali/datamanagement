/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.adapter;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import com.android.annotations.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.ImplicitSelectLinearLayout;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;

/**
 * BaseTreeViewAdapter to implement selection of child/parent objects in a tree
 * @author mpadmanabhan
 * @since 6/26/2018.
 */
public abstract class BaseTreeViewAdapter<T> extends AbstractTreeViewAdapter<T> {

   private static final int TRAVERSE_UP = 1;
   private static final int TRAVERSE_DOWN = 2;
   private Map<T, SelectionType> selectionMap;

   public enum SelectionType {
      FULL, IMPLICIT
   }

   /**
    * constructor for BaseTreeViewAdapter class
    * @param activity current activity
    * @param treeStateManager to manage the state of the tree
    * @param numberOfLevels number of levels in the tree
    */
   public BaseTreeViewAdapter(Activity activity, TreeStateManager<T> treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);
      selectionMap = new ConcurrentHashMap<T, SelectionType>();
   }

   /**
    * Interface to track visiting node
    * @param <T> visitor object
    */
   public interface Visitor<T> {
      /**
       * visit the node
       * @param node the node
       * @return whether to continue the traversal
       */
      boolean visit(T node);
   }

   /**
    * Called when new view is to be created.
    * @param treeNodeInfo node info
    * @return view that should be displayed as tree content
    */
   @Override
   @Nullable
   public View getNewChildView(@Nullable TreeNodeInfo<T> treeNodeInfo) {
      return null;
   }

   /**
    * Called when new view is going to be reused. Update the view
    * and fill it with the data required to display the new information.
    * @param view view that should be updated with the new values
    * @param treeNodeInfo node info used to populate the view
    * @return view to used as row indented content
    */
   @Override
   @Nullable
   public View updateView(@Nullable View view, @Nullable TreeNodeInfo<T> treeNodeInfo) {
      return null;
   }

   @Override
   public long getItemId(int i) {
      return 0;
   }

   /**
    * Implements selection functionality based the selected object id
    * @param id Object id of selected item
    */
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
         if (getManager() != null && getManager().getParent((T) id) != null && childSelectionDoesImplicitSelectParentNodes(getRootNode((T) id))) {
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
         if (includeParent((T) id) && getManager() != null) {
            selectionMap.put(getManager().getParent((T) id), SelectionType.IMPLICIT);
         }
         //Traverse up, and unselect implicit parent if this is the only selected item
         if (getManager() != null && getManager().getParent((T) id) != null) {
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
    * updates view based on the selection
    * @param parent parent view
    */
   public void updateViewSelection(final AdapterView<?> parent) {
      for (int i = 0; i < parent.getChildCount(); i++) {
         View child = parent.getChildAt(i);
         T node = (T) child.getTag(); //tree associates ObjectGraph with each view
         if (node == null) continue;
         ImplicitSelectLinearLayout layout = (ImplicitSelectLinearLayout) child;
         layout.setSupported(isSupportedEntity(node));
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

   /**
    * traverses tree based on the directions
    * @param id object id
    * @param directions direction of traversal (up/down)
    * @param visitor visitor object
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
    * getter for selectionMap
    * @return selectionMap
    */
   public Map<T, SelectionType> getSelectionMap() {
      return selectionMap;
   }

   /**
    * checks if all the objects are selected or not
    * @return true if all the tree items are selected else false.
    */
   public boolean areAllSelected() {
      SelectAllVisitor v = new SelectAllVisitor();
      if (getManager() != null) {
         for (T graph : getManager().getChildren(null)) {
            traverseTree(graph, TRAVERSE_DOWN, v);
         }
      }
      return v.getResult();
   }

   /**
    * parent node(s) are Implicitly selected if all the child node(s) are selected
    * @param rootNode the root node for the selected child node
    * @return false if child selection should implicitly select root node
    */
   protected boolean childSelectionDoesImplicitSelectParentNodes(@Nullable T rootNode) {
      return false;
   }

   /**
    * This function returns the root node of any given node. This is calculated recursively.
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

   /**
    * includes parent based on the selection type (Implicit/Full)
    * @param id object id of the selected child node
    * @return false if the child node should include parent node(if there are more than one child, parent should be included if all the child nodes are selected)
    */
   public boolean includeParent(T id) {
      return false;
   }

   /**
    * checks if the selected child node supports layout implicit selection
    * @param node current selected child node
    * @return false if the given entity is not supported, else true if it is supported
    */
   public boolean isSupportedEntity(T node) {
      return false;
   }

   /**
    * checks if a selection is made or not.
    * @return true if there is a selection else false
    */
   public boolean hasSelection() {
      for (Map.Entry<T, SelectionType> entry : getSelectionMap().entrySet()) {
         if (isSupportedEntity(entry.getKey())) return true;
      }
      return false;
   }

   /**
    * class that handles selectAll based on SelectionType
    */
   public class SelectAllVisitor implements Visitor<T> {
      boolean result = true;

      @Override
      public boolean visit(T node) {
         if (!SelectionType.FULL.equals(selectionMap.get(node))) {
            result = false;
         }
         return result;
      }

      /**
       * getter to get the current result
       * @return result, true if selection type is implicit else returns false
       */
      public boolean getResult() {
         return result;
      }
   }
}
