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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioGroup;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.SegmentedToggleButtonGroup;
import com.cnh.jgroups.Datasource;
import com.cnh.pf.android.data.management.ExportFragment;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestImportFragment;
import com.cnh.pf.android.data.management.adapter.PathTreeViewAdapter;
import com.cnh.pf.data.management.aidl.MediumDevice;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import roboguice.RoboGuice;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;

/**
 * Dialog allows user to select an import source and directory path
 * @author oscar.salazar@cnhind.com
 */
public class ImportSourceDialog extends DialogView {
   private static final Logger log = LoggerFactory.getLogger(ImportSourceDialog.class);

   @Inject private EventManager eventManager;
   @Inject LayoutInflater layoutInflater;
   @Bind(R.id.import_selection_group) SegmentedToggleButtonGroup importGroup;
   @Bind(R.id.source_path_tree_view) TreeViewList sourcePathTreeView;
   @Bind(R.id.display_picklist) PickListEditable displayPicklist;
   private TreeStateManager<File> manager;
   private PathTreeViewAdapter treeAdapter;
   private TreeBuilder<File> treeBuilder;
   private HashMap<Integer, MediumDevice> devices;
   private PathTreeViewAdapter.OnPathSelectedListener listener = null;
   private MediumDevice currentDevice;

   public ImportSourceDialog(Activity context, List<MediumDevice> mediums) {
      super(context);
      RoboGuice.getInjector(context).injectMembers(this);
      init(mediums);
   }

   private void init(List<MediumDevice> mediums) {
      setTitle(getResources().getString(R.string.select_source));
      if (mediums == null) {
         //TODO inflate NO_IMPORT_SOURCE_DETECTED layout
         View view = layoutInflater.inflate(R.layout.no_device_layout, null);
         setBodyView(view);
         setFirstButtonText(getResources().getString(R.string.ok));
         showSecondButton(false);
         showThirdButton(false);
         setOnButtonClickListener(new OnButtonClickListener() {
            @Override public void onButtonClick(DialogViewInterface dialog, int which) {
               if (which == DialogViewInterface.BUTTON_FIRST) {
                  ((TabActivity) getContext()).dismissPopup(ImportSourceDialog.this);
               }
            }
         });
      }
      else {
         View view = layoutInflater.inflate(R.layout.import_source_layout, null);
         ButterKnife.bind(this, view);

         devices = new HashMap<Integer, MediumDevice>();
         int buttonId = 0;
         Set<String> hosts = new HashSet<String>();
         for (MediumDevice device : mediums) {
            if (device.getType().equals(Datasource.Source.USB)) {
               importGroup.addButton(getContext().getResources().getString(R.string.usb_string), buttonId);
               devices.put(buttonId, device);
            }
            else if (device.getType().equals(Datasource.Source.DISPLAY)) {
               if(hosts.add(device.getName())) { //one button per host
                  importGroup.addButton(getContext().getResources().getString(R.string.display_named, device.getName()), buttonId);
               }
               devices.put(buttonId, device);
            }
            buttonId++;
         }
         importGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
               onButtonSelectd(checkedId);
            }
         });
         setBodyView(view);

         setFirstButtonText(getResources().getString(R.string.select_string));
         showFirstButton(false);
         setSecondButtonText(getResources().getString(R.string.cancel));
         setOnButtonClickListener(new OnButtonClickListener() {
            @Override public void onButtonClick(DialogViewInterface dialog, int which) {
               if (which == DialogViewInterface.BUTTON_FIRST) {
                  //Select
                  eventManager.fire(new ImportSourceSelectedEvent(currentDevice));
               }
               ((TabActivity) getContext()).dismissPopup(ImportSourceDialog.this);
            }
         });
         showThirdButton(false);
      }
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

   private void onButtonSelectd(int buttonId) {
      currentDevice = devices.get(buttonId);

      showFirstButton(true);

      if (currentDevice.getType().equals(Datasource.Source.USB) && currentDevice.getPath()!=null) {
         //Do this after usb, display selection
         manager = new InMemoryTreeStateManager<File>();
         treeBuilder = new TreeBuilder<File>(manager);
         sourcePathTreeView.removeAllViewsInLayout();
         sourcePathTreeView.setVisibility(VISIBLE);
         displayPicklist.setVisibility(GONE);

         populateTree(null, currentDevice.getPath());
         treeAdapter = new PathTreeViewAdapter((Activity) getContext(), manager, 1);
         sourcePathTreeView.setAdapter(treeAdapter);
         manager.collapseChildren(null); //Collapse all children

         treeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
            @Override public void onPathSelected(File path) {
               currentDevice.setPath(path);
               setFirstButtonEnabled(true);
            }
         });
      }
      else if (currentDevice.getType().equals(Datasource.Source.DISPLAY)) {
         sourcePathTreeView.setVisibility(GONE);
         displayPicklist.setAdapter(new PickListAdapter(displayPicklist, getContext()));
         String currentHost = currentDevice.getName();
         int id = 0;
         log.trace("current host {}", currentHost);
         for(MediumDevice md : devices.values()) {
            log.trace("Looking at device {}", md.getName());
            if(currentHost.equals(md.getName())) {
               log.trace("Adding device");
               displayPicklist.addItem(new ExportFragment.ObjectPickListItem<MediumDevice>(id++, md.getAddress().toString(), md));
            }
         }
         displayPicklist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               ExportFragment.ObjectPickListItem<MediumDevice> item = (ExportFragment.ObjectPickListItem<MediumDevice>) displayPicklist.findItemById(id);
               currentDevice = item.getObject();
               setFirstButtonEnabled(true);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
         });
         displayPicklist.setVisibility(VISIBLE);
      }
   }

   public class ImportSourceSelectedEvent {
      private MediumDevice device;

      public ImportSourceSelectedEvent(MediumDevice device) {
         this.device = device;
      }

      /**
       * Returns the Device {USB/DISPLAY} selected by user
       * @return
       */
      public MediumDevice getDevice() {
         return device;
      }
   }
}
