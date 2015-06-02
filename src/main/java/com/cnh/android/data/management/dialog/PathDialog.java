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

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

import android.app.Activity;
import android.os.Environment;
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
   private TreeStateManager<String> manager;
   private PathTreeViewAdapter treeAdapter;
   private TreeBuilder<String> treeBuilder;

   private PathTreeViewAdapter.OnPathSelectedListener listener;

   public PathDialog(Activity context) {
      super(context);
      init();
   }

   private void init() {
      LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
      View view = inflater.inflate(R.layout.path_tree, null);
      pathList = (TreeViewList) view.findViewById(R.id.path_tree_view);
      manager = new InMemoryTreeStateManager<String>();
      treeBuilder = new TreeBuilder<String>(manager);
      pathList.removeAllViewsInLayout();

      List<String> paths;

      // Hard-coding until we begin support for usb slot
      File fileList = new File("/sdcard");

      paths = getSourcePath(fileList);
      for (String path : paths) {
         treeBuilder.sequentiallyAddNextNode(path, 0);
         //TODO Currenly creates list of directories, should be enhanced to provide for Directory Tree
         //         addChildrenToTree(graph, 0);
      }
      treeAdapter = new PathTreeViewAdapter((Activity) getContext(), manager, 1);
      pathList.setAdapter(treeAdapter);
      manager.collapseChildren(null); //Collapse all children
      treeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
         @Override
         public void onPathSelected(String path) {
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

   private List<String> getSourcePath(File dir) {
      File[] files = dir.listFiles();
      List<String> sourcePaths = new ArrayList<String>();
      for (File file : files) {
         if (file.getName().contains("cn1")) {
            sourcePaths.add(file.getAbsolutePath());
         }
         else if (file.isDirectory()) {
            List<String> cPath = getSourcePath(file);
            for (String path : cPath) {
               sourcePaths.add(path);
            }
         }
      }
      return sourcePaths;
   }

   public void setOnPathSelectedListener(PathTreeViewAdapter.OnPathSelectedListener listener) {
      this.listener = listener;
   }

}
