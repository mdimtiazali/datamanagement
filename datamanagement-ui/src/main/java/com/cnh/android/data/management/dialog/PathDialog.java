/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.android.data.management.dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.cnh.android.data.management.R;
import com.cnh.android.data.management.adapter.PathTreeViewAdapter;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;

/**
 * Dialog allows user to select a directory path
 * Created by oscar.salazar@cnhind.com
 */
public class PathDialog extends DialogView {

   private TreeViewList pathList;
   private TreeStateManager<File> manager;
   private PathTreeViewAdapter treeAdapter;
   private TreeBuilder<File> treeBuilder;

   private PathTreeViewAdapter.OnPathSelectedListener listener;

   public PathDialog(Activity context) {
      super(context);
      init();
   }

   private void init() {
      LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
      View view = inflater.inflate(R.layout.path_tree, null);
      pathList = (TreeViewList) view.findViewById(R.id.path_tree_view);
      manager = new InMemoryTreeStateManager<File>();
      treeBuilder = new TreeBuilder<File>(manager);
      pathList.removeAllViewsInLayout();

      // Hard-coding until we begin support for usb slot
      getSourcePath(null, Environment.getExternalStorageDirectory());

      treeAdapter = new PathTreeViewAdapter((Activity) getContext(), manager, 1);
      pathList.setAdapter(treeAdapter);
      manager.collapseChildren(null); //Collapse all children
      treeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
         @Override
         public void onPathSelected(File path) {
            Toast.makeText(getContext(), "Selected Path:" + path, Toast.LENGTH_LONG).show();
            ((TabActivity) getContext()).dismissPopup(PathDialog.this);

            if (listener != null) {
               listener.onPathSelected(path);
            }
         }
      });

      setBodyView(view);
      setTitle(getResources().getString(R.string.select_path));
      setFirstButtonText(getResources().getString(R.string.cancel));
      setOnButtonClickListener(new OnButtonClickListener() {
         @Override
         public void onButtonClick(DialogViewInterface dialog, int which) {
            if (which == DialogViewInterface.BUTTON_FIRST) {
               //Cancel
               ((TabActivity) getContext()).dismissPopup(PathDialog.this);
            }
         }
      });
      showSecondButton(false);
      showThirdButton(false);
   }

   private void getSourcePath(File parent, File dir) {
      if(parent==null) {
         treeBuilder.sequentiallyAddNextNode(dir, 0);
      } else {
         treeBuilder.addRelation(parent, dir);
      }
      if(!dir.getName().contains("cn1") &&
            !dir.getName().contains("TASKDATA")) {
         File[] files = dir.listFiles();
         if (files != null) {
            for (File file : files) {
               if (file.isDirectory()) {
                  getSourcePath(dir, file);
               }
            }
         }
      }
   }

   public void setOnPathSelectedListener(PathTreeViewAdapter.OnPathSelectedListener listener) {
      this.listener = listener;
   }

}
