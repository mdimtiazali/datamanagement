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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import com.android.annotations.VisibleForTesting;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TreeEntityHelper;
import com.cnh.pf.android.data.management.adapter.PathTreeViewAdapter;
import com.cnh.pf.android.data.management.misc.IconizedFile;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

   private EventManager eventManager;
   private PickListEditable displayPicklist;
   private LayoutInflater layoutInflater;
   private TreeStateManager<IconizedFile> usbManager;
   private TreeStateManager<IconizedFile> cloudManager;
   private PathTreeViewAdapter usbTreeAdapter;
   private PathTreeViewAdapter cloudTreeAdapter;
   private TreeBuilder<IconizedFile> usbTreeBuilder;
   private TreeBuilder<IconizedFile> cloudTreeBuilder;
   private TreeViewList sourcePathTreeView;
   private HashMap<Integer, SessionExtra> extras;
   private SessionExtra currentExtra;
   private SessionExtra cloudExtra;
   public int getCurrentSessionExtra;
   public boolean isUSBType = false;
   private Set<String> file2Support = new HashSet<String>();

   private void initSupportedFiles() {
      file2Support.add("TASKDATA.XML");
      file2Support.add("vip.xml");
      file2Support.add("shp");
      file2Support.add("SHP");
      file2Support.add("shx");
      file2Support.add("SHX");
      file2Support.add("dbf");
      file2Support.add("DBF");
   }

   public ImportSourceDialog(Activity context, List<SessionExtra> extras) {
      super(context);
      initSupportedFiles();
      RoboGuice.getInjector(context).injectMembers(this);
      setDialogWidth(getResources().getInteger(R.integer.import_source_dialog_width));
      //TODO: Height should be dynamic! The following line is only a fix for DialogView loosing buttons if
      //      height is too great. This should be verified to be necessary with each core update!
      setBodyHeight(getResources().getInteger(R.integer.import_source_dialog_height));
      layoutInflater= (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      eventManager = RoboGuice.getInjector(context).getInstance(EventManager.class);
      init(extras);
   }

   private void init(List<SessionExtra> extrasIn) {
      setTitle(getResources().getString(R.string.select_source));
      if (extrasIn != null && extrasIn.isEmpty()) {
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
         sourcePathTreeView = (TreeViewList) view.findViewById(R.id.source_path_tree_view);
         displayPicklist = (PickListEditable) view.findViewById(R.id.display_picklist);
         ButterKnife.bind(this, view);
         extras = new HashMap<Integer, SessionExtra>();
         int importSourceId = 0;
         Set<String> hosts = new HashSet<String>();
         usbManager = new InMemoryTreeStateManager<IconizedFile>();
         usbTreeBuilder = new TreeBuilder<IconizedFile>(usbManager);
         cloudManager = new InMemoryTreeStateManager<IconizedFile>();
         cloudTreeBuilder = new TreeBuilder<IconizedFile>(cloudManager);
         for (SessionExtra extra : extrasIn) {
            if (extra.isUsbExtra()) {
               extras.put(importSourceId, extra);
               currentExtra = extra;
               File usbFile = new File(getContext().getResources().getString(R.string.usb_string));
               IconizedFile usbRootFile = new IconizedFile(usbFile, TreeEntityHelper.getIcon("USB"));
               usbImportSource(usbRootFile, importSourceId, currentExtra);
               getCurrentSessionExtra = currentExtra.getType();
            }
            else if (extra.isCloudExtra()) {
               extras.put(importSourceId, extra);
               cloudExtra = extra;
               File cloudFile = new File(getContext().getResources().getString(R.string.cloud_string));
               IconizedFile cloudRootFile = new IconizedFile(cloudFile, TreeEntityHelper.getIcon("CLOUD"));
               cloudImportSource(cloudRootFile, importSourceId, cloudExtra);//TODO needs Implementation
            }
            else if (extra.isDisplayExtra()) {
               if (hosts.add(extra.getDescription())) { //one button per host
                  //TODO needs Implementation
               }
               extras.put(importSourceId, extra);
            }
            importSourceId++;
         }
         setBodyView(view);
         setFirstButtonText(getResources().getString(R.string.select_string));
         showFirstButton(true);
         setFirstButtonEnabled(false);
         setSecondButtonText(getResources().getString(R.string.cancel));
         setOnButtonClickListener(new OnButtonClickListener() {
            @Override
            public void onButtonClick(DialogViewInterface dialog, int which) {
               if (which == DialogViewInterface.BUTTON_FIRST) {
                  eventManager.fire(new ImportSourceSelectedEvent(currentExtra));
               }
               ((TabActivity) getContext()).dismissPopup(ImportSourceDialog.this);
            }
         });
         showThirdButton(false);
      }
   }

   @VisibleForTesting
   public void usbImportSource(IconizedFile usbFile, int importSourceId, SessionExtra currentExtra) {
      usbTreeBuilder.sequentiallyAddNextNode(usbFile, 0);
      File usbRootFile = new File(currentExtra.getPath());
      populateTree(usbTreeBuilder, usbFile, new IconizedFile(usbRootFile));
      onImportSourceSelected(importSourceId);
      isUSBType = true; //for Testing
   }

   private void cloudImportSource(IconizedFile cloudFile, int importSourceId, SessionExtra cloudExtra) {
      cloudTreeBuilder.sequentiallyAddNextNode(cloudFile, 0);
      //TODO need to implement populateTree
      onImportSourceSelected(importSourceId);
   }

   private void populateTree(TreeBuilder<IconizedFile> treeBuilder, IconizedFile parent, IconizedFile dir) {
      if (parent == null) {
         treeBuilder.sequentiallyAddNextNode(dir, 0);
      }
      else {
         treeBuilder.addRelation(parent, dir);
      }

      File[] files = dir.getFile().listFiles(new FileFilter() {
         private boolean isDirAccept(File file) {
            if (file.isDirectory()) {
               File[] fs = file.listFiles();
               if (fs != null) {
                  for (File f : fs) {
                     if ((f.isDirectory() && isDirAccept(f)) || (file2Support.contains(Files.getFileExtension(f.getName())) || file2Support.contains(f.getName()))) {
                        return true;
                     }
                  }
               }
            }
            return false;
         }

         @Override
         public boolean accept(File file) {
            boolean ret = false;
            if (file.isDirectory()) {
               ret = isDirAccept(file);
            }
            else if (file2Support.contains(Files.getFileExtension(file.getName()))) {
               ret = true;
            }
            return ret;
         }
      });
      if (files != null) {
         for (File file : files) {
            IconizedFile tmpIconizedFile = new IconizedFile(file);
            populateTree(treeBuilder, dir, tmpIconizedFile);
         }
      }
   }

   private void onImportSourceSelected(int importSourceId) {
      currentExtra = extras.get(importSourceId);
      showFirstButton(true);
      if (currentExtra.isUsbExtra() && currentExtra.getPath() != null) {
         //Do this after usb, display selection
         sourcePathTreeView.removeAllViewsInLayout();
         sourcePathTreeView.setVisibility(VISIBLE);
         displayPicklist.setVisibility(GONE);
         usbTreeAdapter = new PathTreeViewAdapter((Activity) getContext(), usbManager, 1);
         sourcePathTreeView.setAdapter(usbTreeAdapter);
         usbManager.collapseChildren(null); //Collapse all children
         usbTreeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
            @Override
            public void onPathSelected(IconizedFile iconizedFile) {
               File file = iconizedFile.getFile();
               currentExtra.setPath(file.getPath());
               setFirstButtonEnabled(true);
            }
         });
      }
      else if (currentExtra.isDisplayExtra()) {
         sourcePathTreeView.setVisibility(GONE);
         displayPicklist.setAdapter(new PickListAdapter(displayPicklist, getContext()));
         String currentHost = currentExtra.getDescription();
         int id = 0;
         log.trace("current host {}", currentHost);
         log.trace("Medium devices {}", extras);
         for (SessionExtra ex : extras.values()) {
            if (currentHost.equals(ex.getDescription())) {
               log.trace("Adding device {}", ex);
               //displayPicklist.addItem(new ObjectPickListItem<MediumDevice>(id++, ex.getAddress().toString(), md));
            }
         }
         displayPicklist.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean b) {
               ObjectPickListItem<SessionExtra> item = (ObjectPickListItem<SessionExtra>) displayPicklist.findItemById(id);
               currentExtra = item.getObject();
               setFirstButtonEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
         });
         displayPicklist.setVisibility(VISIBLE);
      }
   }

   public static class ObjectPickListItem<T> extends PickListItem {
      private T object;

      public ObjectPickListItem(long id, String value, T object) {
         super(id, value);
         this.object = object;
      }

      public T getObject() {
         return object;
      }
   }

   public class ImportSourceSelectedEvent {
      private SessionExtra extra;

      public ImportSourceSelectedEvent(SessionExtra extra) {
         this.extra = extra;
      }

      /**
       * Returns the Device {USB/DISPLAY} selected by user
       * @return
       */
      public SessionExtra getExtra() {
         return extra;
      }
   }

   /**
    * getter for currentExtra. Used for Testing
    *
    * @return getCurrentSessionExtra the current sessionExtra
    */
   public int getCurrentExtra() {
      return getCurrentSessionExtra;
   }

   /**
    * getter for checking currentExtra Type. Used for Testing
    *
    * @return isUSBType
    */

   public boolean checkUSBType() {
      return isUSBType;
   }
}
