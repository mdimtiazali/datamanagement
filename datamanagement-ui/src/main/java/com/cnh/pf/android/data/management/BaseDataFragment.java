/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.adapter.SelectionTreeViewAdapter;
import com.cnh.pf.android.data.management.connection.DataServiceConnection;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.ConnectionEvent;
import com.cnh.pf.android.data.management.dialog.ErrorDialog;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.pf.android.data.management.helper.TreeDragShadowBuilder;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.DataManagementSession.SessionOperation;
import com.cnh.pf.data.management.aidl.IDataManagementListenerAIDL;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.NodeAlreadyInTreeException;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventListener;
import roboguice.event.EventManager;
import roboguice.fragment.provided.RoboFragment;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;

/**
 * Base Import/Export Fragment, handles inflating TreeView area and selection. Import/Export source
 * control is handled by extending class. ButterKnife binding handled by this class. Extending classes need
 * to annotate and inflate layouts.
 * @author oscar.salazar@cnhind.com
 */
public abstract class BaseDataFragment extends RoboFragment implements IDataManagementListenerAIDL {
   public static final String NAME = "MEDIATOR";
   private final Logger logger = LoggerFactory.getLogger(getClass());

   @Inject
   private DataServiceConnection dataServiceConnection;
   @Inject
   protected LayoutInflater layoutInflater;
   /** Service shared global EventManager */
   @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)
   @Inject
   EventManager globalEventManager;
   @InjectView(R.id.path_tv)
   TextView pathTv;
   @InjectView(R.id.select_all_btn)
   Button selectAllBtn;
   @InjectView(R.id.tree_view_list)
   TreeViewList treeViewList;
   @InjectView(R.id.tree_progress)
   protected ProgressBar treeProgress;
   @InjectView(R.id.start_text)
   protected TextView startText;
   @InjectResource(R.string.done)
   String doneStr;

   private TreeStateManager<ObjectGraph> manager;
   private TreeBuilder<ObjectGraph> treeBuilder;
   protected ObjectTreeViewAdapter treeAdapter;
   protected Handler handler = new Handler(Looper.getMainLooper());
   protected boolean cancelled;

   /** Current session */
   protected volatile DataManagementSession session = null;

   /**
    * Extending class must inflate layout to be populated on the left panel
    * @param inflater Base class inflater
    * @param leftPanel Layout Left Panel Holder
    */
   public abstract void inflateViews(LayoutInflater inflater, View leftPanel);

   /**
    * Callback when new session is started.
    */
   public void onNewSession() {
      setSession(null);
   }

   /**
    * {@link SessionOperation} has completed.
    *
    * Useful to get user input and pass to next step in the process.
    * Also to respond after process completion.
    */
   public abstract void processOperations();

   /**
    * Is this operation the one currently managed by this fragment.
    * @param session the session
    * @return  <code>true</code> if the session is the current one, <code>false</code> if this is background session.
    */
   public abstract boolean isCurrentOperation(DataManagementSession session);

   /**
    * Progress update from datasource.
    * @param operation  the current operation
    * @param progress   current progress
    * @param max        total progress
    */
   public abstract void onProgressPublished(String operation, int progress, int max);

   /**
    * Test whether a node is supported by the current format.
    * @param node the node
    * @return  <code>true</code> if supported, <code>false</code> otherwise.
    */
   public abstract boolean supportedByFormat(ObjectGraph node);

   /**
    * A background session has completed.
    * @param session the session
    */
   protected abstract void onOtherSessionUpdate(DataManagementSession session);

   protected void onViewChange(org.jgroups.View oldView, org.jgroups.View newView) {
   }

   /**
    * When user (de)selects a node in the tree
    */
   public void onTreeItemSelected() {
      updateSelectAllState();
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putParcelable("session", session);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      if (savedInstanceState != null) {
         session = savedInstanceState.getParcelable("session");
      }
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.import_layout, container, false);
      LinearLayout leftPanel = (LinearLayout) layout.findViewById(R.id.left_panel_wrapper);
      inflateViews(inflater, leftPanel);
      return layout;
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      selectAllBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            selectAll();
         }
      });
      treeViewList.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
      treeViewList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
         @Override
         public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (!treeAdapter.getSelectionMap().containsKey(view.getTag())) {
               treeAdapter.handleItemClick(parent, view, position, view.getTag()); //select it, and start the drag
            }
            view.startDrag(null, new TreeDragShadowBuilder(view, treeViewList, treeAdapter), treeAdapter.getSelected(), 0);
            return false;
         }
      });
   }

   @Override
   public void onPause() {
      super.onPause();
      if (dataServiceConnection.isConnected()) {
         getDataManagementService().unregister(NAME);
      }
      globalEventManager.unregisterObserver(ConnectionEvent.class, connectionListener);
      globalEventManager.unregisterObserver(DataServiceConnectionImpl.ErrorEvent.class, errorListener);
      globalEventManager.unregisterObserver(DataServiceConnectionImpl.ViewChangeEvent.class, viewChangeListener);
   }

   @Override
   public void onResume() {
      super.onResume();
      globalEventManager.registerObserver(ConnectionEvent.class, connectionListener);
      globalEventManager.registerObserver(DataServiceConnectionImpl.ErrorEvent.class, errorListener);
      globalEventManager.registerObserver(DataServiceConnectionImpl.ViewChangeEvent.class, viewChangeListener);
      if (dataServiceConnection.isConnected()) {
         getDataManagementService().register(NAME, BaseDataFragment.this);
         if (session != null && !session.getSessionOperation().equals(SessionOperation.DISCOVERY)) {
            onResumeSession(session);
         }
         else {
            onResumeSession(null);
         }
      }
      ((DataManagementActivity) getActivity()).hideSubheader();
   }

   @Override
   public void onProgressUpdated(String operation, int progress, int max) throws RemoteException {
      onProgressPublished(operation, progress, max);
   }

   @Override
   public void onDataSessionUpdated(DataManagementSession session) throws RemoteException {
      if (isCurrentOperation(session)) {
         onResumeSession(session);
      }
      else {
         onOtherSessionUpdate(session);
      }
   }

   @Override
   public IBinder asBinder() {
      return null;
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
         logger.debug("onErrorEvent {}", event);
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               DialogView errorDialog = new ErrorDialog(getActivity(), event);
               errorDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
                  @Override
                  public void onButtonClick(DialogViewInterface dialog, int which) {
                     if (which == DialogViewInterface.BUTTON_FIRST) {
                        if (isCurrentOperation(event.getSession())) {
                           onResumeSession(null);
                        }
                        else {
                           onOtherSessionUpdate(event.getSession());
                        }
                     }
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
            getDataManagementService().register(NAME, BaseDataFragment.this);
            onResumeSession(session);
         }
         else {
            //Disable all buttons for now
            enableButtons(false);
            getDataManagementService().unregister(NAME);
         }
      }
   };

   /** Called when group membership changes */
   EventListener viewChangeListener = new EventListener<DataServiceConnectionImpl.ViewChangeEvent>() {
      @Override
      public void onEvent(final DataServiceConnectionImpl.ViewChangeEvent event) {
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               onViewChange(event.getOldView(), event.getNewView());
            }
         });
      }
   };

   private void onResumeSession(DataManagementSession session) {
      logger.debug("onResumeSession {}", session);
      if (session == null) {
         logger.debug("Starting new session");
         cancelled = false;
         treeViewList.setVisibility(View.GONE);
         onNewSession();
      }
      else {
         setSession(session);
         DataManagementSession.SessionOperation op = session.getSessionOperation();
         if (op.equals(DataManagementSession.SessionOperation.DISCOVERY)) {
            treeProgress.setVisibility(View.GONE);
            initiateTree();
            updateSelectAllState();
         }
         else if (op.equals(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS)) {
            //Import to existing parent if entity has parent
            getSession().setData(processPartialImports(getSession().getData()));
         }
         processOperations();
      }
   }

   private List<Operation> processPartialImports(List<Operation> operations) {
      if (operations == null) {
         logger.warn("calculate operations returned No operations");
         return null;
      }
      for (Operation operation : operations) {
         operation.setTarget(operation.getData().getParent());
      }
      return operations;
   }

   private void initiateTree() {
      logger.debug("initateTree");
      //Discovery happened
      enableButtons(true);
      List<ObjectGraph> data = session != null && session.getObjectData() != null ? session.getObjectData() : new ArrayList<ObjectGraph>();
      manager = new InMemoryTreeStateManager<ObjectGraph>();
      treeBuilder = new TreeBuilder<ObjectGraph>(manager);
      for (ObjectGraph graph : data) {
         addToTree(null, graph);
      }
      treeAdapter = new ObjectTreeViewAdapter(getActivity(), manager, 1) {
         @Override
         protected boolean isGroupableEntity(ObjectGraph node) {
            return TreeEntityHelper.groupables.containsKey(node.getType()) || node.getParent() == null;
         }

         @Override
         public boolean isSupportedEntitiy(ObjectGraph node) {
            return supportedByFormat(node);
         }
      };
      treeAdapter.setData(data);
      treeViewList.removeAllViewsInLayout();
      treeViewList.setVisibility(View.VISIBLE);
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
      try {
         //Check if entity can be grouped
         if (TreeEntityHelper.groupables.containsKey(object.getType()) || object.getParent() == null) {
            GroupObjectGraph group = null;
            for (ObjectGraph child : manager.getChildren(parent)) { //find the group node
               if (child instanceof GroupObjectGraph && child.getType().equals(object.getType())) {
                  group = (GroupObjectGraph) child;
                  break;
               }
            }
            if (group == null) { //if group node doesn't exist we gotta make it
               String name = TreeEntityHelper.getGroupName(getActivity(), object.getType());
               group = new GroupObjectGraph(null, object.getType(), name, null, parent);
               treeBuilder.addRelation(parent, group);
            }
            treeBuilder.addRelation(group, object);
         }
         //Else just add to parent
         else {
            treeBuilder.addRelation(parent, object);
         }
         for (ObjectGraph child : object.getChildren()) {
            addToTree(object, child);
         }
      } catch(NodeAlreadyInTreeException e) {
         logger.warn("Caught NodeAlreadyInTree exception", e);
      }
   }

   void updateSelectAllState() {
      selectAllBtn.setEnabled(getSession() != null && getSession().getObjectData() != null && !getSession().getObjectData().isEmpty());
      boolean allSelected = getTreeAdapter().areAllSelected();
      selectAllBtn.setText(allSelected ? R.string.deselect_all : R.string.select_all);
      selectAllBtn.setActivated(allSelected);
   }

   void selectAll() {
      treeAdapter.selectAll(treeViewList, !getTreeAdapter().areAllSelected());
      updateSelectAllState();
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

   protected boolean isCancelled() {
      return cancelled;
   }

   protected void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }
}
