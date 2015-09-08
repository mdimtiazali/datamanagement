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

import android.widget.AdapterView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.cnh.pf.android.data.management.R;

import java.io.File;

/**
 * Adapter feeds dir to TreeView
 * @author oscar.salazar@cnhind.com
 */
public class PathTreeViewAdapter extends AbstractTreeViewAdapter<File> {
   private static final Logger logger = LoggerFactory.getLogger(PathTreeViewAdapter.class);

   private OnPathSelectedListener listener;

   public PathTreeViewAdapter(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);
   }

   @Override
   public void handleItemClick(final AdapterView< ? > parent, final View view, final int position, final Object id) {
      super.handleItemClick(parent, view, position, id);
      if (listener != null) {
         listener.onPathSelected((File) id);
      }
      view.setSelected(true);
   }

   @Override
   public View getNewChildView(TreeNodeInfo treeNodeInfo) {
      final TextView nameView = (TextView) getActivity().getLayoutInflater().inflate(R.layout.tree_list_item_simple, null);
      return updateView(nameView, treeNodeInfo);
   }

   @Override
   public View updateView(View view, TreeNodeInfo treeNodeInfo) {
      final TextView nameView = (TextView) view;
      File path = (File) treeNodeInfo.getId();
      nameView.setText(path.getName());
      return nameView;
   }

   @Override
   public long getItemId(int position) {
      return position;
   }

   public void setOnPathSelectedListener(OnPathSelectedListener listener) {
      this.listener = listener;
   }

   public interface OnPathSelectedListener {
      /**
       * Invokes when user has selected a path
       * @param path String Absolute Path
       */
      void onPathSelected(File path);
   }
}
