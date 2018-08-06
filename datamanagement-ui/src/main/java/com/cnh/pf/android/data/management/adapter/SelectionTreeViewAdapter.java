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


import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

/**
 * TreeViewAdapter with functionality to full/partially select child/parent objects in tree
 * @author oscar.salazar@cnhind.com
 */
public abstract class SelectionTreeViewAdapter<T> extends BaseTreeViewAdapter<T> {

   /**
    * Notify listener of object selected/unselected
    */
   public interface OnTreeItemSelectedListener {
      void onItemSelected();
   }

   public static final int TRAVERSE_UP = 1;
   public static final int TRAVERSE_DOWN = 2;
   private OnTreeItemSelectedListener listener;

   public SelectionTreeViewAdapter(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);
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

   @Override
   public void handleItemClick(final AdapterView<?> parent, View view, int position, Object id) {
      super.handleItemClick(parent, view, position, id);
      setListeners(parent);
      this.selectionImpl(id);
      this.updateViewSelection(parent);
      if (listener != null) {
         listener.onItemSelected();
      }
   }

   public abstract boolean isSupportedEntity(T node);

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
      getSelectionMap().clear();
      setListeners(parent);
      if (selectAll) {
         for (T graph : getManager().getChildren(null)) {
            traverseTree(graph, TRAVERSE_DOWN, new Visitor<T>() {
               @Override
               public boolean visit(T node) {
                  getSelectionMap().put(node, SelectionType.FULL);
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
}
