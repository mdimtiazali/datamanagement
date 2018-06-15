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
import com.cnh.android.widget.control.PickListItem;
import com.cnh.android.widget.control.SegmentedToggleButtonGroup;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.adapter.PathTreeViewAdapter;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.google.common.io.Files;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
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
   private HashMap<Integer, SessionExtra> extras;
   private PathTreeViewAdapter.OnPathSelectedListener listener = null;
   private SessionExtra currentExtra;
   private Set<String> file2Support = new HashSet<String>(){{
      add("TASKDATA.XML");
      add("vip.xml");
      add("shp");
      add("SHP");
      add("shx");
      add("SHX");
      add("dbf");
      add("DBF");
   }};

   public ImportSourceDialog(Activity context, List<SessionExtra> extras) {
      super(context);
      RoboGuice.getInjector(context).injectMembers(this);
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
         ButterKnife.bind(this, view);

         extras = new HashMap<Integer, SessionExtra>();
         int buttonId = 0;
         Set<String> hosts = new HashSet<String>();
         for (SessionExtra extra : extrasIn) {
            if (extra.isUsbExtra()) {
               importGroup.addButton(getContext().getResources().getString(R.string.usb_string), buttonId);
               extras.put(buttonId, extra);
            }
            else if (extra.isDisplayExtra()) {
               if (hosts.add(extra.getDescription())) { //one button per host
                  importGroup.addButton(getContext().getResources().getString(R.string.display_named, extra.getDescription()), buttonId);
               }
               extras.put(buttonId, extra);
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
                  eventManager.fire(new ImportSourceSelectedEvent(currentExtra));
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
         private boolean isDirAccept(File file){
            if(file.isDirectory()){
               File[] fs = file.listFiles();
               if(fs != null){
                  for(File f: fs){
                     if((f.isDirectory() && isDirAccept(f)) || (file2Support.contains(Files.getFileExtension(f.getName())) || file2Support.contains(f.getName()))){
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
            if(file.isDirectory()) {
               ret = isDirAccept(file);
            }
            else if (file2Support.contains(Files.getFileExtension(file.getName()))){
               ret = true;
            }
            return ret;
         }
      });
      if (files != null) {
         for (File file : files) {
            populateTree(dir, file);
         }
      }
   }

   private void onButtonSelectd(int buttonId) {
      currentExtra = extras.get(buttonId);

      showFirstButton(true);

      if (currentExtra.isUsbExtra() && currentExtra.getPath() != null) {
         //Do this after usb, display selection
         manager = new InMemoryTreeStateManager<File>();
         treeBuilder = new TreeBuilder<File>(manager);
         sourcePathTreeView.removeAllViewsInLayout();
         sourcePathTreeView.setVisibility(VISIBLE);
         displayPicklist.setVisibility(GONE);

         populateTree(null, new File(currentExtra.getPath()));
         treeAdapter = new PathTreeViewAdapter((Activity) getContext(), manager, 1);
         sourcePathTreeView.setAdapter(treeAdapter);
         manager.collapseChildren(null); //Collapse all children
         manager.expandDirectChildren(new File(currentExtra.getPath())); //expand top node only

         treeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
            @Override
            public void onPathSelected(File path) {
               currentExtra.setPath(path.getPath());
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
//               displayPicklist.addItem(new ObjectPickListItem<MediumDevice>(id++, ex.getAddress().toString(), md));
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
}
