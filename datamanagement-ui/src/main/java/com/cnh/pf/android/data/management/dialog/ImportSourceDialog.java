/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.dialog;

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
import com.cnh.jgroups.Datasource.Source;
import com.cnh.pf.android.data.management.ExportFragment.ObjectPickListItem;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.adapter.PathTreeViewAdapter;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.datamng.DataUtils;
import com.google.common.io.Files;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;
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
   private static final Logger log = LoggerFactory.getLogger(ImportSourceDialog.class);

   @Inject
   private EventManager eventManager;
   @Inject
   LayoutInflater layoutInflater;
   @Bind(R.id.import_selection_group)
   SegmentedToggleButtonGroup importGroup;
   @Bind(R.id.source_path_tree_view)
   TreeViewList sourcePathTreeView;
   @Bind(R.id.display_picklist)
   PickListEditable displayPicklist;
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
         View view = layoutInflater.inflate(R.layout.no_device_layout, null);
         setBodyView(view);
         setFirstButtonText(getResources().getString(R.string.ok));
         showSecondButton(false);
         showThirdButton(false);
         setOnButtonClickListener(new OnButtonClickListener() {
            @Override
            public void onButtonClick(DialogViewInterface dialog, int which) {
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
               if (hosts.add(device.getName())) { //one button per host
                  importGroup.addButton(getContext().getResources().getString(R.string.display_named, device.getName()), buttonId);
               }
               devices.put(buttonId, device);
            }
            buttonId++;
         }
         importGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
               onButtonSelectd(checkedId);
            }
         });
         setBodyView(view);

         setFirstButtonText(getResources().getString(R.string.select_string));
         showFirstButton(false);
         setSecondButtonText(getResources().getString(R.string.cancel));
         setOnButtonClickListener(new OnButtonClickListener() {
            @Override
            public void onButtonClick(DialogViewInterface dialog, int which) {
               if (which == DialogViewInterface.BUTTON_FIRST) {
                  //Select
                  List<MediumDevice> hostDevices = new ArrayList<MediumDevice>();
                  if (currentDevice.getType().equals(Source.DISPLAY)) {
                     String currentHostname = DataUtils.getHostnameOrIp(currentDevice.getAddress());
                     for (MediumDevice md : devices.values()) {
                        if (currentHostname.equals(DataUtils.getHostnameOrIp(md.getAddress()))) {
                           hostDevices.add(md);
                        }
                     }
                  }
                  else {
                     hostDevices.add(currentDevice);
                  }
                  eventManager.fire(new ImportSourceSelectedEvent(hostDevices));
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
      File[] files = dir.listFiles(new FileFilter() {
         @Override
         public boolean accept(File file) {
            return file.isDirectory() || "shp".equalsIgnoreCase(Files.getFileExtension(file.getName()));
         }
      });
      if (files != null) {
         for (File file : files) {
            populateTree(dir, file);
         }
      }
   }

   private void onButtonSelectd(int buttonId) {
      currentDevice = devices.get(buttonId);

      showFirstButton(true);

      if (currentDevice.getType().equals(Datasource.Source.USB) && currentDevice.getPath() != null) {
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
         manager.expandDirectChildren(currentDevice.getPath()); //expand top node only

         treeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
            @Override
            public void onPathSelected(File path) {
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
         log.trace("Medium devices {}", devices);
         for (MediumDevice md : devices.values()) {
            if (currentHost.equals(md.getName())) {
               log.trace("Adding device {}", md);
               displayPicklist.addItem(new ObjectPickListItem<MediumDevice>(id++, md.getAddress().toString(), md));
            }
         }
         displayPicklist.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean b) {
               ObjectPickListItem<MediumDevice> item = (ObjectPickListItem<MediumDevice>) displayPicklist.findItemById(id);
               currentDevice = item.getObject();
               setFirstButtonEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
         });
         displayPicklist.setVisibility(VISIBLE);
      }
   }

   public class ImportSourceSelectedEvent {
      private List<MediumDevice> device;

      public ImportSourceSelectedEvent(List<MediumDevice> device) {
         this.device = device;
      }

      /**
       * Returns the Device {USB/DISPLAY} selected by user
       * @return
       */
      public List<MediumDevice> getDevices() {
         return device;
      }

      /**
       * Returns the Device {USB/DISPLAY} selected by user
       * @return
       */
      public MediumDevice getDevice() {
         return (device != null && device.size() > 0) ? device.get(0) : null;
      }
   }
}
