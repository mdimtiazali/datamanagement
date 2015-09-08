/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.dialog;

import java.io.File;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.SegmentedToggleButton;
import com.cnh.jgroups.Datasource;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.adapter.PathTreeViewAdapter;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.inject.Inject;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import roboguice.RoboGuice;
import roboguice.event.EventManager;

/**
 * Dialog allows user to select an import source and directory path
 * @author oscar.salazar@cnhind.com
 */
public class ImportSourceDialog extends DialogView {

   @Inject private File usbFile;
   @Inject private EventManager eventManager;
   @Inject LayoutInflater layoutInflater;
   @Bind(R.id.usb_btn) SegmentedToggleButton usbBtn;
   @Bind(R.id.display_button) SegmentedToggleButton displayButton;
   @Bind(R.id.source_path_tree_view) TreeViewList sourcePathTreeView;
   private TreeStateManager<File> manager;
   private PathTreeViewAdapter treeAdapter;
   private TreeBuilder<File> treeBuilder;

   private PathTreeViewAdapter.OnPathSelectedListener listener = null;

   private Datasource.Source selectedSource;
   /** Default path is root */
   private File selectedPath = null;

   public ImportSourceDialog(Activity context) {
      super(context);
      RoboGuice.getInjector(context).injectMembers(this);
      init();
   }

   private void init() {
      View view = layoutInflater.inflate(R.layout.import_source_layout, null);
      ButterKnife.bind(this, view);
      setBodyView(view);

      setTitle(getResources().getString(R.string.select_source));
      setFirstButtonText(getResources().getString(R.string.select_string));
      showFirstButton(false);
      setSecondButtonText(getResources().getString(R.string.cancel));
      setOnButtonClickListener(new OnButtonClickListener() {
         @Override public void onButtonClick(DialogViewInterface dialog, int which) {
            if (which == DialogViewInterface.BUTTON_FIRST) {
               //Select
               eventManager.fire(new ImportSourceSelectedEvent(selectedSource, selectedPath));
            }
            ((TabActivity) getContext()).dismissPopup(ImportSourceDialog.this);
         }
      });
      showThirdButton(false);
   }

   private void populateTree(File parent, File dir) {
      if (parent == null) {
         treeBuilder.sequentiallyAddNextNode(dir, 0);
      }
      else {
         treeBuilder.addRelation(parent, dir);
      }
      //TODO make cnh1 and TASKDATA constants in libdatamng
      if (!dir.getName().contains("cn1") && !dir.getName().contains("TASKDATA")) {
         File[] files = dir.listFiles();
         if (files != null) {
            for (File file : files) {
               if (file.isDirectory()) {
                  populateTree(dir, file);
               }
            }
         }
      }
   }

   @OnClick(R.id.usb_btn)
   void usbSelected() {
      selectedSource = Datasource.Source.USB;
      showFirstButton(true);

      //Do this after usb, display selection
      manager = new InMemoryTreeStateManager<File>();
      treeBuilder = new TreeBuilder<File>(manager);
      sourcePathTreeView.removeAllViewsInLayout();
      sourcePathTreeView.setVisibility(VISIBLE);

      populateTree(null, usbFile);
      treeAdapter = new PathTreeViewAdapter((Activity) getContext(), manager, 1);
      sourcePathTreeView.setAdapter(treeAdapter);
      manager.collapseChildren(null); //Collapse all children

      treeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
         @Override public void onPathSelected(File path) {
            selectedPath = path;
            setFirstButtonEnabled(true);
         }
      });
   }

   @OnClick(R.id.display_button)
   void displaySelected() {
      selectedSource = Datasource.Source.DISPLAY;
      showFirstButton(true);

      sourcePathTreeView.setVisibility(GONE);
   }

   public class ImportSourceSelectedEvent {
      private Datasource.Source sourceType;
      private File path;

      public ImportSourceSelectedEvent(Datasource.Source sourceType, File path) {
         this.sourceType = sourceType;
         this.path = path;
      }

      public Datasource.Source getSourceType() {
         return sourceType;
      }

      public File getPath() {
         return path;
      }
   }
}
