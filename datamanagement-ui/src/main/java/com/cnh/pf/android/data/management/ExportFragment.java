/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;

import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.data.management.DataManagementSession;

import butterknife.Bind;
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

   @Override public void inflateViews(LayoutInflater inflater, View leftPanel) {
      inflater.inflate(R.layout.export_left_panel, (LinearLayout) leftPanel);
   }

   @Override
   public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
   }

   @Override
   public void onNewSession() {
      setSession(new DataManagementSession(Datasource.Source.INTERNAL, Datasource.Source.USB));
      getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.DISCOVERY);
   }

   @Override
   public void processOperations() {
   }

   @Override public void enableButtons(boolean enable) {
      super.enableButtons(enable);
      exportSelectedBtn.setEnabled(enable);
   }

   @OnClick(R.id.export_selected_btn) void exportSelected() {
      Set<ObjectGraph> selected = getTreeAdapter().getSelected();
      if (selected.size() > 0) {
         getSession().setObjectData(new ArrayList<ObjectGraph>(getTreeAdapter().getSelected()));
         getSession().setSessionOperation(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS);
      }
   }
}
