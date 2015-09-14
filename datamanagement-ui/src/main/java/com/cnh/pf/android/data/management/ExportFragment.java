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
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.android.widget.control.ProgressBarView;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.aidl.MediumDevice;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Export Tab Fragment, handles export to external mediums {USB, External Display}.
 * @author oscar.salazar@cnhind.com
 */
public class ExportFragment extends BaseDataFragment {
   private static final Logger logger = LoggerFactory.getLogger(ExportFragment.class);

   @Bind(R.id.export_medium_picklist) PickListEditable exportMediumPicklist;
   @Bind(R.id.export_format_picklist) PickListEditable exportFormatPicklist;
   @Bind(R.id.export_drop_zone) LinearLayout exportDropZone;
   @Bind(R.id.export_selected_btn) Button exportSelectedBtn;
   @Bind(R.id.status_panel) LinearLayout leftStatusPanel;
   @Bind(R.id.progress_bar) ProgressBarView progressBar;
   @Bind(R.id.operation_name) TextView operationName;
   @Bind(R.id.percent_tv) TextView percentTv;

   @Override public void inflateViews(LayoutInflater inflater, View leftPanel) {
      inflater.inflate(R.layout.export_left_panel, (LinearLayout) leftPanel);
   }

   @Override
   public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      // TODO: inflate a fragment view
      View rootView = super.onCreateView(inflater, container, savedInstanceState);
      ButterKnife.bind(this, rootView);
      return rootView;
   }

   @Override public void onDestroyView() {
      super.onDestroyView();
      ButterKnife.unbind(this);
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      populateExportToPickList();
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
               ObjectPickListItem<MediumDevice> item = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.findItemById(id);
               getSession().setDevice(item.getObject());
               checkExportButton();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
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
      setSession(new DataManagementSession(Datasource.Source.INTERNAL, Datasource.Source.USB, null));
      getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.DISCOVERY);
   }

   @Override
   public void processOperations() { }

   @Override
   public boolean isCurrentOperation(DataManagementSession session) {
      return session.getSourceType().equals(Datasource.Source.INTERNAL);
   }

   @Override
   public void onTreeItemSelected() {
      checkExportButton();
   }

   @Override
   public void onProgressPublished(String operation, int progress, int max) {
      logger.debug("onProgressPublished: {}", progress);
      final Double percent = ((progress * 1.0) / max) * 100;
      progressBar.setProgress(percent.intValue());
      percentTv.setText(percent.intValue());
   }

   @Override public void enableButtons(boolean enable) {
      super.enableButtons(enable);
      checkExportButton();
   }

   @OnClick(R.id.export_selected_btn)
   void exportSelected() {
      Set<ObjectGraph> selected = getTreeAdapter().getSelected();
      getSession().setObjectData(new ArrayList<ObjectGraph>(getTreeAdapter().getSelected()));
      ObjectPickListItem<MediumDevice> device = (ObjectPickListItem<MediumDevice>) exportMediumPicklist.getSelectedItem();
      getSession().setDevice(device.getObject());
      getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.PERFORM_OPERATIONS);
      exportDropZone.setVisibility(View.GONE);
      leftStatusPanel.setVisibility(View.VISIBLE);

      operationName.setText(getResources().getString(R.string.exporting_data));
      progressBar.setProgress(0);
      percentTv.setText("0");
   }

   private void checkExportButton() {
      logger.debug("checkExportButton, exportMedium: {}, selectedItems: {}",
            exportMediumPicklist.getSelectedItem() != null ? ((ObjectPickListItem<MediumDevice>) exportMediumPicklist.getSelectedItem()).getObject().getType() : "null",
            getTreeAdapter() != null ? getTreeAdapter().getSelected().size() : "0");
      exportSelectedBtn.setEnabled(exportMediumPicklist.getSelectedItem() != null && (getTreeAdapter() != null && getTreeAdapter().getSelected().size() > 0));
   }

   @OnClick(R.id.pause_button)
   void onPauseButton() {
      logger.debug("OnPause");
      //TODO add pause method to base datasource
   }

   @OnClick(R.id.stop_button)
   void onStopButton() {
      //TODO add stop method to base datasource
   }

   public class ObjectPickListItem<T> extends PickListItem {
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
