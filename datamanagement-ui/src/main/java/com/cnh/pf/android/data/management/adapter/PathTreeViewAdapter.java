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
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.misc.IconizedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

/**
 * Adapter feeds dir to TreeView
 * @author oscar.salazar@cnhind.com
 */
public class PathTreeViewAdapter extends BaseTreeViewAdapter<IconizedFile> {
   private static final Logger logger = LoggerFactory.getLogger(PathTreeViewAdapter.class);
   private OnPathSelectedListener listener;

   public PathTreeViewAdapter(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);
   }

   @Override
   public void handleItemClick(final AdapterView<?> parent, final View view, final int position, final Object id) {
      super.handleItemClick(parent, view, position, id);
      setListeners(parent);
      this.selectionImpl(id);
      this.updateViewSelection(parent);
      if (listener != null) {
         listener.onPathSelected((IconizedFile) id);
      }
   }

   @Override
   public View getNewChildView(TreeNodeInfo treeNodeInfo) {
      final TextView nameView = (TextView) getActivity().getLayoutInflater().inflate(R.layout.tree_list_item_simple, null);
      nameView.setTextColor(getActivity().getResources().getColorStateList(R.color.tree_text_color));
      return updateView(nameView, treeNodeInfo);
   }

   @Override
   public View updateView(View view, TreeNodeInfo treeNodeInfo) {
      final TextView nameView = (TextView) view;
      IconizedFile iconizedFile = (IconizedFile) treeNodeInfo.getId();
      File file = iconizedFile.getFile();
      nameView.setText(file.getName());
      if (iconizedFile.hasIcon()) {
         int iconResourceID = iconizedFile.getIconResourceId();
         nameView.setCompoundDrawablesWithIntrinsicBounds(iconResourceID, 0, 0, 0);
      }
      else {
         nameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
      }
      return nameView;
   }

   @Override
   public long getItemId(int position) {
      return position;
   }

   public void setOnPathSelectedListener(OnPathSelectedListener listener) {
      this.listener = listener;
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
    * Interface to handle listener when a path is selected
    */
   public interface OnPathSelectedListener {
      /**
       * Invokes when user has selected a path
       * @param path String Absolute Path
       */
      void onPathSelected(IconizedFile path);
   }
}
