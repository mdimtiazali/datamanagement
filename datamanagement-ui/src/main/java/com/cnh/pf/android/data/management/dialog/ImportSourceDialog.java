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
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.annotations.VisibleForTesting;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TreeEntityHelper;
import com.cnh.pf.android.data.management.adapter.BaseTreeViewAdapter;
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
import pl.polidea.treeview.TreeNodeInfo;
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
   private LinearLayout loadingContainer;
   private ScrollView contentContainer;
   private TreeStateManager<IconizedFile> usbManager;
   private TreeStateManager<IconizedFile> cloudManager;
   private PathTreeViewAdapter usbTreeAdapter;
   private PathTreeViewAdapter cloudTreeAdapter;
   private TreeBuilder<IconizedFile> usbTreeBuilder;
   private TreeBuilder<IconizedFile> cloudTreeBuilder;
   private TreeViewList sourcePathUsbTreeView;
   private TreeViewList sourcePathCloudTreeView;
   private View usbSpacer;
   private TextView introductionTextView;
   private View introductionSpacer;
   private AsyncTask<Void, Void, Void> asyncUsbLoadingTask = null;
   private AsyncTask<Void, Void, Void> asyncCloudLoadingTask = null;

   private HashMap<Integer, SessionExtra> extras;
   private SessionExtra currentExtra;
   private SessionExtra cloudExtra;
   private int getCurrentSessionExtra;
   private boolean isUSBType = false;
   private Set<String> file2Support = new HashSet<String>();

   private boolean cloudIsLoaded = true;
   private boolean usbIsLoaded = true;

   private void initSupportedFiles() {
      //This list is case sensitive. Only add UpperCase-names!
      //Have a look at method: isFileExtensionSupported and isFileNameSupported.
      file2Support.add("TASKDATA.XML");
      file2Support.add("DBF");
      file2Support.add("SHP");
      file2Support.add("SHX");
      //TODO: The following files are to be supported in the future, but not by now
      //file2Support.add("VIP.XML");
      //file2Support.add("PDF"); //see eagle-systems-7665
      //file2Support.add("MKV"); //see eagle-systems-9401
   }

   public ImportSourceDialog(Activity context, List<SessionExtra> extras) {
      super(context);
      initSupportedFiles();
      RoboGuice.getInjector(context).injectMembers(this);
      setDialogWidth(getResources().getInteger(R.integer.import_source_dialog_width));
      setContentPaddings(0, 0, 0, 0);
      //TODO: Height should be dynamic! The following line is only a fix for DialogView loosing buttons if
      //      height is too great. This should be verified to be necessary with each core update!
      setBodyHeight(getResources().getInteger(R.integer.import_source_dialog_height));
      layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      eventManager = RoboGuice.getInjector(context).getInstance(EventManager.class);
      updateView(extras);
   }

   /**
    * updates view based on import source states
    * @param extras list of Import sources
    */
   public void updateView(List<SessionExtra> extras) {
      this.mFlContent.removeAllViews();
      init(extras);
   }

   private void init(List<SessionExtra> extrasIn) {
      setTitle(getResources().getString(R.string.select_source));
      final View rootView;
      if (extrasIn == null || extrasIn.isEmpty()) {
         //leave quick note in the log if extrasIn is null
         if (extrasIn == null) {
            log.error("Could not properly initialize ImportSourceDialog since incoming SessionExtras are null!");
         }
         //show no device found content
         rootView = layoutInflater.inflate(R.layout.no_device_layout, null);
         setBodyView(rootView);
         setFirstButtonText(getResources().getString(R.string.ok));
         setFirstButtonEnabled(true);
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
         //show select import source content
         final View sourcePathUsbDisabled;
         final View sourcePathCloudDisabled;
         rootView = layoutInflater.inflate(R.layout.import_source_layout, null);
         sourcePathUsbTreeView = (TreeViewList) rootView.findViewById(R.id.source_path_usb_tree_view);
         sourcePathUsbDisabled = rootView.findViewById(R.id.source_path_usb_disabled);
         sourcePathCloudTreeView = (TreeViewList) rootView.findViewById(R.id.source_path_cloud_tree_view);
         sourcePathCloudDisabled = rootView.findViewById(R.id.source_path_cloud_disabled);
         usbSpacer = rootView.findViewById(R.id.select_source_usb_spacer);
         introductionTextView = (TextView) rootView.findViewById(R.id.select_source_introduction);
         introductionSpacer = rootView.findViewById(R.id.select_source_introduction_spacer);
         displayPicklist = (PickListEditable) rootView.findViewById(R.id.display_picklist);
         loadingContainer = (LinearLayout) rootView.findViewById(R.id.import_source_select_import_sources_loading);
         contentContainer = (ScrollView) rootView.findViewById(R.id.import_source_select_import_sources_content);
         ButterKnife.bind(this, rootView);
         extras = new HashMap<Integer, SessionExtra>();
         int importSourceId = 0;
         Set<String> hosts = new HashSet<String>();
         usbManager = new InMemoryTreeStateManager<IconizedFile>();
         usbTreeBuilder = new TreeBuilder<IconizedFile>(usbManager);
         cloudManager = new InMemoryTreeStateManager<IconizedFile>();
         cloudTreeBuilder = new TreeBuilder<IconizedFile>(cloudManager);
         boolean usbTreeDisabled = true;
         boolean cloudTreeDisabled = true;
         for (SessionExtra extra : extrasIn) {
            if (extra.isUsbExtra()) {
               usbTreeDisabled = false;
               extras.put(importSourceId, extra);
               currentExtra = extra;
               File usbFile = new File(getContext().getResources().getString(R.string.usb_string));
               IconizedFile usbRootFile = new IconizedFile(usbFile, TreeEntityHelper.getIcon("USB"));
               usbIsLoaded = false;
               usbImportSource(usbRootFile, importSourceId, currentExtra);
               getCurrentSessionExtra = currentExtra.getType();
            }
            else if (extra.isCloudExtra()) {
               cloudTreeDisabled = false;
               extras.put(importSourceId, extra);
               cloudExtra = extra;
               File cloudFile = new File(getContext().getResources().getString(R.string.cloud_string));
               IconizedFile cloudRootFile = new IconizedFile(cloudFile, TreeEntityHelper.getIcon("CLOUD"));
               cloudIsLoaded = false;
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

         if (usbTreeDisabled) {
            //show disabled tree for usb
            sourcePathUsbDisabled.setVisibility(VISIBLE);
            sourcePathUsbTreeView.setVisibility(GONE);
         }
         else {
            sourcePathUsbDisabled.setVisibility(GONE);
            sourcePathUsbTreeView.setVisibility(VISIBLE);
         }
         if (cloudTreeDisabled) {
            //show disabled tree for cloud
            sourcePathCloudDisabled.setVisibility(VISIBLE);
            sourcePathCloudTreeView.setVisibility(GONE);
         }
         else {
            sourcePathCloudDisabled.setVisibility(GONE);
            sourcePathCloudTreeView.setVisibility(VISIBLE);
         }
         setBodyView(rootView);
         setFirstButtonText(getResources().getString(R.string.select_string));
         setFirstButtonEnabled(false);
         setSecondButtonEnabled(true);
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

   private synchronized void onLoadingFinished() {
      if (usbIsLoaded && cloudIsLoaded) {
         loadingContainer.setVisibility(GONE);
         contentContainer.setVisibility(VISIBLE);
      }
   }

   @VisibleForTesting
   public void usbImportSource(final IconizedFile usbFile, final int importSourceId, final SessionExtra currentExtra) {
      asyncUsbLoadingTask = new AsyncTask<Void, Void, Void>() {
         @Override
         protected Void doInBackground(Void... voids) {
            Thread.currentThread().setName("usbImportSource loading usb data");
            usbTreeBuilder.sequentiallyAddNextNode(usbFile, 0);
            File usbRootFile = new File(currentExtra.getPath());
            populateTree(usbTreeBuilder, usbFile, new IconizedFile(usbRootFile));
            return null;
         }

         @Override
         protected void onPostExecute(Void aVoid) {
            onImportSourceSelected(importSourceId);
            isUSBType = true; //for Testing
            usbIsLoaded = true;
            onLoadingFinished();
            asyncUsbLoadingTask = null;
         }
      };
      asyncUsbLoadingTask.execute();
   }

   private void cloudImportSource(final IconizedFile cloudFile, final int importSourceId, final SessionExtra cloudExtra) {
      asyncCloudLoadingTask = new AsyncTask<Void, Void, Void>() {
         @Override
         protected Void doInBackground(Void... voids) {
            Thread.currentThread().setName("cloudImportSource loading cloud data");
            cloudTreeBuilder.sequentiallyAddNextNode(cloudFile, 0);
            //TODO need to implement populateTree
            return null;
         }

         @Override
         protected void onPostExecute(Void aVoid) {
            onImportSourceSelected(importSourceId);
            cloudIsLoaded = true;
            onLoadingFinished();
            asyncCloudLoadingTask = null;
         }
      };
      asyncCloudLoadingTask.execute();
   }

   @Override
   protected void onDetachedFromWindow() {
      super.onDetachedFromWindow();
      //decouple async loading tasks
      if (asyncCloudLoadingTask != null) {
         asyncCloudLoadingTask.cancel(true);
         asyncCloudLoadingTask = null;
      }
      if (asyncUsbLoadingTask != null) {
         asyncUsbLoadingTask.cancel(true);
         asyncUsbLoadingTask = null;
      }
   }

   private void populateTree(final TreeBuilder<IconizedFile> treeBuilder, final IconizedFile parent, final IconizedFile dir) {
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
                     if ((f.isDirectory() && isDirAccept(f)) || (isFileNameSupported(f.getName()) || isFileExtensionSupported(f.getName()))) {
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
            else if (isFileExtensionSupported(file.getName())) {
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
         sourcePathUsbTreeView.removeAllViewsInLayout();
         sourcePathUsbTreeView.setVisibility(VISIBLE);
         sourcePathCloudTreeView.removeAllViewsInLayout();
         sourcePathCloudTreeView.setVisibility(VISIBLE);
         usbSpacer.setVisibility(VISIBLE);
         introductionTextView.setVisibility(VISIBLE);
         introductionSpacer.setVisibility(VISIBLE);
         displayPicklist.setVisibility(GONE);
         usbTreeAdapter = new PathTreeViewAdapter((Activity) getContext(), usbManager, 1);
         usbTreeAdapter.setAutomaticSelection(false);
         sourcePathUsbTreeView.setAdapter(usbTreeAdapter);
         usbManager.collapseChildren(null); //Collapse all children
         cloudTreeAdapter = new PathTreeViewAdapter((Activity) getContext(), cloudManager, 1);
         sourcePathCloudTreeView.setAdapter(cloudTreeAdapter);
         cloudManager.collapseChildren(null); //Collapse all children
         usbTreeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
            @Override
            public void onPathSelected(IconizedFile iconizedFile, TreeNodeInfo selectedNode) {
               onItemClick(iconizedFile, selectedNode, usbTreeAdapter, usbManager, sourcePathUsbTreeView, cloudTreeAdapter);
            }
         });
         cloudTreeAdapter.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
            @Override
            public void onPathSelected(IconizedFile iconizedFile, TreeNodeInfo selectedNode) {
               onItemClick(iconizedFile, selectedNode, cloudTreeAdapter, cloudManager, sourcePathCloudTreeView, usbTreeAdapter);
            }
         });
      }
      else if (currentExtra.isDisplayExtra()) {
         sourcePathUsbTreeView.setVisibility(GONE);
         sourcePathCloudTreeView.setVisibility(GONE);
         usbSpacer.setVisibility(GONE);
         introductionTextView.setVisibility(GONE);
         introductionSpacer.setVisibility(GONE);
         displayPicklist.setAdapter(new PickListAdapter(displayPicklist, getContext()));
         String currentHost = currentExtra.getDescription();
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
               //not needed
            }
         });
         displayPicklist.setVisibility(VISIBLE);
      }
   }

   private void onItemClick(IconizedFile iconizedFile, TreeNodeInfo selectedNode, PathTreeViewAdapter ownPathTreeViewAdapter, TreeStateManager<IconizedFile> ownManager,
         TreeViewList ownTreeViewList, PathTreeViewAdapter otherPathTreeViewAdapter) {
      if (!selectedNode.isWithChildren()) {
         //determine, if any other item is already selected
         if (ownPathTreeViewAdapter.getSelectionMap().containsKey(iconizedFile)) {
            ownPathTreeViewAdapter.getSelectionMap().remove(iconizedFile);
         }
         else {
            if (ownPathTreeViewAdapter.getSelectionMap().size() > 0) {
               //at least one item in  the usb tree is already selected, remove selection
               ownPathTreeViewAdapter.getSelectionMap().clear();
            }
            if (otherPathTreeViewAdapter.getSelectionMap().size() > 0) {
               otherPathTreeViewAdapter.getSelectionMap().clear();
            }
            File file = iconizedFile.getFile();
            currentExtra.setPath(file.getPath());
            ownPathTreeViewAdapter.getSelectionMap().put(iconizedFile, BaseTreeViewAdapter.SelectionType.FULL);
         }
      }
      else {
         //selected item is non selectable: expand or collapse
         if (ownPathTreeViewAdapter.getSelectionMap().containsKey(iconizedFile)) {
            ownPathTreeViewAdapter.getSelectionMap().remove(iconizedFile);
         }
         if (selectedNode.isExpanded()) {
            ownManager.collapseChildren(iconizedFile);
         }
         else {
            ownManager.expandDirectChildren(iconizedFile);
         }
      }
      updateSelectButtonState();
      ownPathTreeViewAdapter.updateViewSelection(ownTreeViewList);
   }

   /**
    * enables/disables the select button state
    */
   private void updateSelectButtonState() {
      final boolean selectButtonState;
      //TODO: As soon as the cloud is implemented, its selection map should be checked here as well
      if (usbTreeAdapter.getSelectionMap().isEmpty()) {
         selectButtonState = false;
      }
      else {
         selectButtonState = true;
      }
      setFirstButtonEnabled(selectButtonState);
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

   public static class ImportSourceSelectedEvent {
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
    * @return getCurrentSessionExtra the current sessionExtra
    */
   public int getCurrentExtra() {
      return getCurrentSessionExtra;
   }

   /**
    * getter for checking currentExtra Type. Used for Testing
    * @return isUSBType
    */

   public boolean checkUSBType() {
      return isUSBType;
   }

   /**
    * This method returns weather a file with the given filename is supported or not
    * @param filename Filename to be checked if supported or not
    * @return True if given filename is supported, false otherwise
    */
   private boolean isFileNameSupported(String filename) {
      if (filename != null) {
         return (file2Support.contains(filename.toUpperCase()));
      }
      else {
         log.error("Could not determine if empty filename is supported!");
         return false;
      }
   }

   /**
    * This method returns weather an extension of the given filename is supported or not
    * @param filename Filename containing the extension to be checked weather it is supported or not
    * @return True if extension of given filename is supported, false otherwise
    */
   private boolean isFileExtensionSupported(String filename) {
      if (filename != null) {
         return (file2Support.contains(Files.getFileExtension(filename).toUpperCase()));
      }
      else {
         log.error("Could not determine if extension of empty filename is supported!");
         return false;
      }
   }
}
