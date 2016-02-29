/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.OnClick;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.android.widget.control.ProgressBarView;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.DataManagementSession.SessionOperation;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.datamng.Process;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.inject.InjectView;

/**
 * Export Tab Fragment, handles export to external mediums {USB, External Display}.
 * @author oscar.salazar@cnhind.com
 */
public class ExportFragment extends BaseDataFragment {
   private static final Logger logger = LoggerFactory.getLogger(ExportFragment.class);

   @Inject protected FormatManager formatManager;
   @InjectView(R.id.export_medium_picklist) PickListEditable exportMediumPicklist;
   @InjectView(R.id.export_format_picklist) PickListEditable exportFormatPicklist;
   @InjectView(R.id.export_drop_zone) LinearLayout exportDropZone;
   @InjectView(R.id.export_selected_btn) Button exportSelectedBtn;
   @InjectView(R.id.stop_button) ImageButton stopButton;
   @InjectView(R.id.status_panel) LinearLayout leftStatusPanel;
   @InjectView(R.id.progress_bar) ProgressBarView progressBar;
   @InjectView(R.id.operation_name) TextView operationName;
   @InjectView(R.id.percent_tv) TextView percentTv;

   private int dragAcceptColor;
   private int dragRejectColor;
   private int dragEnterColor;
   private int transparentColor;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      try {
         formatManager.parseXml();
      }
      catch (Exception e) {
         logger.error("Error parsing xml file", e);
      }
      dragAcceptColor = getResources().getColor(R.color.drag_accept);
      dragRejectColor = getResources().getColor(R.color.drag_reject);
      dragEnterColor = getResources().getColor(R.color.drag_enter);
      transparentColor = getResources().getColor(android.R.color.transparent);
   }

   @Override public void inflateViews(LayoutInflater inflater, View leftPanel) {
      inflater.inflate(R.layout.export_left_panel, (LinearLayout) leftPanel);
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      exportSelectedBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            exportSelected();
         }
      });
      stopButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            getDataManagementService().cancel(session);
         }
      });
      exportDropZone.setOnDragListener(new View.OnDragListener() {
         @Override public boolean onDrag(View v, DragEvent event) {
            switch(event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
               if(exportSelectedBtn.isEnabled()) {
                  exportDropZone.setBackgroundColor(dragAcceptColor);
               }
               return true;
            case DragEvent.ACTION_DRAG_ENDED:
               exportDropZone.setBackgroundColor(transparentColor);
               return true;
            case DragEvent.ACTION_DRAG_ENTERED:
               exportDropZone.setBackgroundColor(
                  exportSelectedBtn.isEnabled() ? dragEnterColor : dragRejectColor);
               return true;
            case DragEvent.ACTION_DRAG_EXITED:
                  exportDropZone.setBackgroundColor(
                        exportSelectedBtn.isEnabled() ? dragAcceptColor : transparentColor);
               return true;
            case DragEvent.ACTION_DROP:
               logger.info("Dropped");
               if(exportSelectedBtn.isEnabled()) {
                  exportSelected();
               }
               return true;
            }
            return false;
         }
      });
      startText.setVisibility(View.GONE);
      populateExportToPickList();
      populateFormatPickList();
   }

   private void populateFormatPickList() {
      exportFormatPicklist.setAdapter(new PickListAdapter(exportFormatPicklist, getActivity().getApplicationContext()));
      int formatId = 0;
      for (String format : formatManager.getFormats()) {
         logger.debug("Format: {}", format);
         exportFormatPicklist.addItem(new PickListItem(formatId++, format));
      }

      exportFormatPicklist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            if(id == -1) {
               if(getSession()!=null) {
                  getSession().setFormat(null);
               }
            } else {
               PickListItem item = exportFormatPicklist.findItemById(id);
               getSession().setFormat(item.getValue());
            }
            if(getTreeAdapter()!=null) {
               getTreeAdapter().updateViewSelection(treeViewList);
            }
            checkExportButton();
            treeViewList.invalidate();
         }

         @Override
         public void onNothingSelected(AdapterView<?> adapterView) {
            getSession().setFormat(null);
         }
      });
   }

   private void populateExportToPickList() {
      exportMediumPicklist.setAdapter(new PickListAdapter(exportMediumPicklist, getActivity().getApplicationContext()));
      List<MediumDevice> devices = getDataManagementService().getMediums();
      if (devices != null && devices.size() > 0) {
         int deviceId = 0;
         for (MediumDevice device : devices) {
            exportMediumPicklist.addItem(new ObjectPickListItem<MediumDevice>(deviceId, device.getType().toString(), device));
            deviceId++;
         }
         exportMediumPicklist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               if(id == -1) {
                  if(getSession()!=null) {
                     getSession().setDevice(null);
                  }
               } else {
                  ObjectPickListItem<MediumDevice> item = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.findItemById(id);
                  getSession().setDevice(Arrays.asList(item.getObject()));
               }
               checkExportButton();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
               getSession().setDevice(null);
            }
         });
      }
      else {
         //TODO Show Connect Sources in PickList
      }
   }

   @Override
   public void onNewSession() {
      leftStatusPanel.setVisibility(View.GONE);
      exportDropZone.setVisibility(View.VISIBLE);
      treeProgress.setVisibility(View.VISIBLE);
      treeViewList.setVisibility(View.GONE);
      setSession(new DataManagementSession(new Datasource.Source[] { Datasource.Source.INTERNAL, Datasource.Source.DISPLAY }, null,
         new Datasource.Source[] { Datasource.Source.USB }));
      if (exportMediumPicklist.getSelectedItemValue() != null) {
         ObjectPickListItem<MediumDevice> item = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.getSelectedItem();
         getSession().setDevice(Arrays.asList(item.getObject()));
      }
      if(exportFormatPicklist.getSelectedItemValue() != null) {
         getSession().setFormat(exportFormatPicklist.getSelectedItemValue());
      }
      setSession(getDataManagementService().processOperation(getSession(), SessionOperation.DISCOVERY));
   }

   @Override
   public void setSession(DataManagementSession session) {
      super.setSession(session);
      if(session!=null) {
         if(session.getFormat()==null) {
            exportFormatPicklist.setSelectionById(-1);
         }
         else {
            int index = new ArrayList<String>(formatManager.getFormats()).indexOf(session.getFormat());
            exportFormatPicklist.setSelectionByPosition(index);
         }
         if(session.getDevice()==null) {
            exportMediumPicklist.setSelectionById(-1);
         }
         else {
            for(int i=0; i<exportMediumPicklist.getAdapter().getCount(); i++) {
               ObjectPickListItem<MediumDevice> item = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.getAdapter().getItem(i);
               if(item.getObject().equals(session.getDevice())) {
                  exportMediumPicklist.setSelectionById(item.getId());
                  break;
               }
            }
         }
      }
      checkExportButton();
   }

   @Override
   public void processOperations() {
      if(getSession().getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS)) {
         logger.trace("resetting new session.  Operation completed.");
         getTreeAdapter().selectAll(treeViewList, false);
         removeProgressPanel();
         if(getSession().getResult().equals(Process.Result.SUCCESS)) {
            Toast.makeText(getActivity(), "Export Completed", Toast.LENGTH_LONG).show();
         }
         onNewSession();
      }
      checkExportButton();
   }

   @Override
   public boolean isCurrentOperation(DataManagementSession session) {
      logger.trace("isCurrentOperation( {} )", Arrays.toString(session.getSourceTypes()));
      return Arrays.binarySearch(session.getSourceTypes(), Datasource.Source.INTERNAL) > -1;
   }

   @Override
   public void onTreeItemSelected() {
      super.onTreeItemSelected();
      checkExportButton();
   }

   @Override
   public void onProgressPublished(String operation, int progress, int max) {
      logger.debug("onProgressPublished: {}", progress);
      final Double percent = ((progress * 1.0) / max) * 100;
      progressBar.setProgress(percent.intValue());
      percentTv.setText(percent.intValue()+"");
      if(getSession()!=null &&
            getSession().getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS)
            && progress == max) {
         logger.info("Process completed.  {}/{} objects", progress, max);
      }
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      boolean supported = true;
      if (exportFormatPicklist.getSelectedItemValue() != null) {
         supported = formatManager.formatSupportsType(exportFormatPicklist.getSelectedItemValue(), node.getType());
      }
      return supported;
   }

   @Override public void enableButtons(boolean enable) {
      super.enableButtons(enable);
      checkExportButton();
   }

   @Override
   protected void onOtherSessionUpdate(DataManagementSession session) {
      logger.debug("Other session has been updated");
      if(session.getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS)) {
         logger.trace("Other operation completed. Enabling UI.");
         checkExportButton();
      }
   }

   void exportSelected() {
      getSession().setData(null);
      getSession().setObjectData(new ArrayList<ObjectGraph>(getTreeAdapter().getSelected()));
      ObjectPickListItem<MediumDevice> device = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.getSelectedItem();
      getSession().setDevice(Arrays.asList(device.getObject()));
      getSession().setFormat(exportFormatPicklist.getSelectedItemValue());
      setSession(getDataManagementService().processOperation(getSession(), SessionOperation.PERFORM_OPERATIONS));
      showProgressPanel();
   }

   /** Inflates left panel progress view */
   private void showProgressPanel() {
      leftStatusPanel.setVisibility(View.VISIBLE);
      exportDropZone.setVisibility(View.GONE);
      progressBar.setTitle(getResources().getString(R.string.exporting_string));
      operationName.setText(getResources().getString(R.string.exporting_data));
      progressBar.setProgress(0);
      percentTv.setText("0");
   }

   /** Removes left panel progress view and replaces with operation view */
   private void removeProgressPanel() {
      leftStatusPanel.setVisibility(View.GONE);
      exportDropZone.setVisibility(View.VISIBLE);
   }

   private void checkExportButton() {
      DataManagementSession s = getSession();
      boolean isActiveOperation = s!=null
            && s.getSessionOperation().equals(SessionOperation.PERFORM_OPERATIONS)
            && s.getResult()==null;
      isActiveOperation |= getDataManagementService().hasActiveSession();
      boolean hasSelection = getTreeAdapter() != null
            && s != null
            && s.getDevice() != null
            && s.getFormat() != null
            && getTreeAdapter().getSelected().size() > 0;

      exportSelectedBtn.setEnabled(hasSelection && !isActiveOperation);

      logger.debug("checkExportButton {} {}", s,
            hasSelection ? getTreeAdapter().getSelected().size() : "null");
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
}
