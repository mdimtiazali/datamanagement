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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressBarView;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.adapter.SelectionTreeViewAdapter;
import com.cnh.pf.android.data.management.connection.DataServiceConnection;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.ConnectionEvent;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.DataSessionEvent;
import com.cnh.pf.android.data.management.dialog.ErrorDialog;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.data.management.DataManagementSession;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventListener;
import roboguice.event.EventManager;
import roboguice.fragment.provided.RoboFragment;

/**
 * Base Import/Export Fragment, handles inflating TreeView area and selection. Import/Export source
 * control is handled by extending class. ButterKnife binding handled by this class. Extending classes need
 * to annotate and inflate layouts.
 * @author oscar.salazar@cnhind.com
 */
public abstract class BaseDataFragment extends RoboFragment {
   private static final Logger logger = LoggerFactory.getLogger(BaseDataFragment.class);

   @Inject private DataServiceConnection dataServiceConnection;
   @Inject protected LayoutInflater layoutInflater;
   /** Service shared global EventManager */
   @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)
   @Inject EventManager globalEventManager;
   @Bind(R.id.path_tv) TextView pathTv;
   @Bind(R.id.select_all_btn) Button selectAllBtn;
   @Bind(R.id.tree_view_list) TreeViewList treeViewList;
   @Bind(R.id.tree_progress) protected ProgressBarView treeProgress;
   LinearLayout leftPanel;

   private TreeStateManager<ObjectGraph> manager;
   private TreeBuilder<ObjectGraph> treeBuilder;
   private ObjectTreeViewAdapter treeAdapter;

   private volatile DataManagementSession session = null;

   /**
    * Extending class must inflate layout to be populated on the left panel
    * @param inflater Base class inflater
    * @param leftPanel Layout Left Panel Holder
    */
   public abstract void inflateViews(LayoutInflater inflater, View leftPanel);
   public abstract void onNewSession();
   public abstract void processOperations();
   public abstract boolean isCurrentOperation(DataManagementSession session);
   public abstract void onTreeItemSelected();
   public abstract void onProgressPublished(String operation, int progress, int max);

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.import_layout, container, false);
      leftPanel = (LinearLayout) layout.findViewById(R.id.left_panel_wrapper);
      inflateViews(inflater, leftPanel);
      //TODO less listeners, one event cover multiple triggers
      globalEventManager.registerObserver(DataSessionEvent.class, updateListener);
      globalEventManager.registerObserver(ConnectionEvent.class, connectionListener);
      globalEventManager.registerObserver(DataServiceConnectionImpl.ErrorEvent.class, errorListener);
      globalEventManager.registerObserver(DataServiceConnectionImpl.ProgressEvent.class, progressListener);
      ButterKnife.bind(this, layout);
      return layout;
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      ButterKnife.bind(this, view);
      treeViewList.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
   }

   @Override
   public void onDestroyView() {
      super.onDestroyView();
      ButterKnife.unbind(this);
      globalEventManager.unregisterObserver(DataSessionEvent.class, updateListener);
      globalEventManager.unregisterObserver(ConnectionEvent.class, connectionListener);
      globalEventManager.unregisterObserver(DataServiceConnectionImpl.ErrorEvent.class, errorListener);
      globalEventManager.unregisterObserver(DataServiceConnectionImpl.ProgressEvent.class, progressListener);
   }

   @Override
   public void onResume() {
      super.onResume();
      if (dataServiceConnection.isConnected()) {
         onResumeSession(dataServiceConnection.getService().getSession());
      }
   }

   /**
    * Enables all buttons in the layout when conncetion with data cluster is successful
    * @param enable Enable when true, else disable
    */
   public void enableButtons(boolean enable) {
      selectAllBtn.setEnabled(enable);
   }

   /**Called when the service catches an exception during operation */
   EventListener errorListener = new EventListener<DataServiceConnectionImpl.ErrorEvent>() {
      @Override
      public void onEvent(final DataServiceConnectionImpl.ErrorEvent event) {
         logger.debug("onErrorEvent");
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               DialogView errorDialog = new ErrorDialog(getActivity(), event);
               errorDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
                  @Override
                  public void onButtonClick(DialogViewInterface dialog, int which) {
                     if (which == DialogViewInterface.BUTTON_FIRST)
                        onResumeSession(null);
                  }
               });
               ((TabActivity) getActivity()).showPopup(errorDialog, true);
            }
         });
      }
   };

   /** Called when a change in connection to backend happens */
   EventListener connectionListener = new EventListener<ConnectionEvent>() {
      @Override
      public void onEvent(ConnectionEvent event) {
         logger.debug("onConnected");
         if (event.isConnected()) {
            onResumeSession(dataServiceConnection.getService().getSession());
         }
         else {
            //Disable all buttons for now
            enableButtons(false);
         }
      }
   };

   /** Called when progress is published from backed */
   EventListener progressListener = new EventListener<DataServiceConnectionImpl.ProgressEvent>() {
      @Override
      public void onEvent(final DataServiceConnectionImpl.ProgressEvent event) {
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               onProgressPublished(event.getOperation(), event.getProgress(), event.getMax());
            }
         });
      }
   };

   private void onResumeSession(DataManagementSession session) {
      logger.debug("onResumeSession");
      if (session == null || !isCurrentOperation(session)) {
         logger.debug("Starting new session");
         getDataManagementService().resetSession();
         setSession(null);
         treeViewList.setVisibility(View.GONE);
         onNewSession();
      }
      else if (session.getObjectData() == null || session.getObjectData().size() < 1) {
         //TODO NO data found dialog
      }
      else {
         setSession(session);
         DataManagementSession.SessionOperation op = session.getSessionOperation();
         if (op.equals(DataManagementSession.SessionOperation.DISCOVERY)) {
            treeProgress.setVisibility(View.GONE);
            initiateTree();
         }
         else if (op.equals(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS)) {
            //Import to existing parent if entity has parent
            List<Operation> operations = processPartialImports(getSession().getData());
            getSession().setData(operations);
            processOperations();
         }
         else if (op.equals(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS)) {
         }
      }
   }

   private List<Operation> processPartialImports(List<Operation> operations) {
      Map<ObjectGraph, Operation> operationMap = new HashMap<ObjectGraph, Operation>();
      for (Operation operation : operations) {
         operationMap.put(operation.getData(), operation);
      }
      for (ObjectGraph graph : treeAdapter.getSelected()) {
         if (graph.getParent() != null) {
            operationMap.get(graph).setTarget(graph.getParent());
         }
      }
      return new ArrayList<Operation>(operationMap.values());
   }

   /** Called when session was updated on backend */
   EventListener updateListener = new EventListener<DataSessionEvent>() {
      @Override
      public void onEvent(final DataSessionEvent event) {
         logger.debug("onSessionUpdated, session: {}", (session != null ? session.toString() : "null"));
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               onResumeSession(event.getSession());
            }
         });
      }
   };

   private void initiateTree() {
      logger.debug("initateTree");
      //Discovery happened
      enableButtons(true);
      manager = new InMemoryTreeStateManager<ObjectGraph>();
      treeBuilder = new TreeBuilder<ObjectGraph>(manager);
      treeViewList.setVisibility(View.VISIBLE);
      treeViewList.removeAllViewsInLayout();
      for (ObjectGraph graph : session.getObjectData()) {
         addToTree(null, graph);
      }
      treeAdapter = new ObjectTreeViewAdapter(getActivity(), manager, 1) {
         @Override
         protected boolean isGroupableEntity(ObjectGraph node) {
            return TreeEntityHelper.groupables.contains(node.getType());
         }
      };
      treeAdapter.setData(session.getObjectData());
      treeViewList.setAdapter(treeAdapter);
      treeAdapter.setOnTreeItemSelectedListener(new SelectionTreeViewAdapter.OnTreeItemSelectedListener() {
         @Override
         public void onItemSelected() {
            logger.debug("onTreeItemSelected");
            onTreeItemSelected();
         }
      });
   }

   private void addToTree(ObjectGraph parent, ObjectGraph object) {
      //Check if entity can be grouped
      if (TreeEntityHelper.groupables.contains(object.getType())) {
         GroupObjectGraph gparent = null;
         for (ObjectGraph child : manager.getChildren(parent)) {
            if (child instanceof GroupObjectGraph && child.getType().equals(object.getType())) {
               gparent = (GroupObjectGraph) child;
            }
         }
         if (gparent == null) {
            if (TreeEntityHelper.topLevelEntites.containsKey(object.getType())) {
               gparent = new GroupObjectGraph(null, object.getType(), getString(TreeEntityHelper.topLevelEntites.get(object.getType())));
               treeBuilder.sequentiallyAddNextNode(gparent, 0);
            }
            else {
               gparent = new GroupObjectGraph(null, object.getType(), object.getType().substring(object.getType().lastIndexOf('.') + 1) + "s", null, parent);
               treeBuilder.addRelation(parent, gparent);
            }
         }
         treeBuilder.addRelation(gparent, object);
      }
      //Else just add to parent
      else {
         treeBuilder.addRelation(parent, object);
      }
      for (ObjectGraph child : object.getChildren()) {
         addToTree(object, child);
      }
   }

   @OnClick(R.id.select_all_btn)
   void selectAll() {
      treeAdapter.selectAll(treeViewList);
   }

   public DataManagementSession getSession() {
      return session;
   }

   public DataManagementService getDataManagementService() {
      return dataServiceConnection.getService();
   }

   public ObjectTreeViewAdapter getTreeAdapter() {
      return treeAdapter;
   }

   public void setSession(DataManagementSession session) {
      this.session = session;
   }
}
