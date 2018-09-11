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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.annotations.Nullable;
import com.cnh.android.pf.widget.controls.ToastMessageCustom;
import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.adapter.SelectionTreeViewAdapter;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.pf.android.data.management.helper.DataExchangeBlockedOverlay;
import com.cnh.pf.android.data.management.helper.DataExchangeProcessOverlay;
import com.cnh.pf.android.data.management.helper.TreeDragMultipleSelectionShadowBuilder;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionContract;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.datamng.Process;

import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import pl.polidea.treeview.InMemoryTreeNode;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.NodeAlreadyInTreeException;
import pl.polidea.treeview.SortableChildrenTree;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import roboguice.fragment.provided.RoboFragment;
import roboguice.inject.InjectView;

/**
 * Base Import/Export Fragment, handles inflating TreeView area and selection. Import/Export source
 * control is handled by extending class. ButterKnife binding handled by this class. Extending classes need
 * to annotate and inflate layouts.
 * @author oscar.salazar@cnhind.com
 */
public abstract class BaseDataFragment extends RoboFragment implements SessionContract.SessionView {
   public static final String NAME = "MEDIATOR";
   private final Logger logger = LoggerFactory.getLogger(getClass());

   @InjectView(R.id.tree_view_list)
   TreeViewList treeViewList;
   @InjectView(R.id.select_all_btn)
   Button selectAllBtn;
   @InjectView(R.id.header_text)
   TextView headerTextView;

   protected DataExchangeBlockedOverlay disabledOverlay = null;
   protected DataExchangeProcessOverlay processOverlay = null;
   protected TreeStateManager<ObjectGraph> manager;
   protected TreeBuilder<ObjectGraph> treeBuilder;
   protected ObjectTreeViewAdapter treeAdapter;

   protected static boolean isConnectedToPcm = false;
   protected static final int SHOWING_FEEDBACK_AFTER_PROGRESS_MS = 2000;

   private SessionContract.SessionManager sessionManager;

   private List<String> dataTreeRootNodesWithAutomaticParentSelection = null;
   private static boolean dsPerfFlag = false;

   public static boolean isDsPerfFlag() {
      return dsPerfFlag;
   }

   public static void setDsPerfFlag(boolean dsPerfFlag) {
      BaseDataFragment.dsPerfFlag = dsPerfFlag;
   }

   @Override
   public void setSessionManager(SessionContract.SessionManager sessionManager) {
      this.sessionManager = sessionManager;
   }

   /**
    * Expose session manager instance for child class.
    */
   protected SessionContract.SessionManager getSessionManager() {
      return this.sessionManager;
   }

   /**
    * Comparator to sort list items in alphabetical order. Depending on requirement this could
    * be implemented differently but current requirement specifies alphabetical order.
    */
   private Comparator<InMemoryTreeNode<ObjectGraph>> comparator = new Comparator<InMemoryTreeNode<ObjectGraph>>() {
      @Override
      public int compare(InMemoryTreeNode<ObjectGraph> lhs, InMemoryTreeNode<ObjectGraph> rhs) {
         // Do the case-insensitive check first
         if((lhs != null)  && (rhs != null) && (lhs.getId() != null) && (rhs.getId() != null) &&
            (lhs.getId().getName() != null) && (rhs.getId().getName() != null) ){
            int comparison = lhs.getId().getName().compareToIgnoreCase(rhs.getId().getName());
            if (comparison != 0) {
               return comparison;
            }
            // If the case-insensitive check is same, do the case-sensitive check to make upper-case come first
            return lhs.getId().getName().compareTo(rhs.getId().getName());
         }
         // error case, return after
         return 1;
      }
   };

   /**
    * Sort a list of child nodes under each tree parent node.
    */
   protected void sortTreeList() {
      if (manager instanceof SortableChildrenTree) {
         ((SortableChildrenTree) manager).sortChildren(comparator);
      }
   }

   /**
    * Extending class must inflate layout to be populated on the left panel
    * @param inflater Base class inflater
    * @param leftPanel Layout Left Panel Holder
    */
   public abstract void inflateViews(LayoutInflater inflater, View leftPanel);

   /**
    * Callback when existing session is resumed.
    */
   public abstract void onResumeSession();

   /**
    * Reset current session.
    */
   public void resetSession() {
      Session session = getSession();

      session.setType(Session.Type.DISCOVERY);
      session.setResultCode(null);
      session.setObjectData(new ArrayList<ObjectGraph>());
      session.setOperations(new ArrayList<Operation>());
      session.setSources(new ArrayList<Address>());
      session.setDestinations(new ArrayList<Address>());
      session.setState(Session.State.WAIT);
   }

   /**
    * Callback when PCM is disconnected. PCM datasources are not joined in the channel.
    */
   public abstract void onPCMDisconnected();

   /**
    * Callback when PCM is connected.
    */
   public abstract void onPCMConnected();

   /**
    * Callback when a session is successfully finished.
    *
    * @param session the session
    */
   public abstract void onMyselfSessionSuccess(Session session);

   /**
    * Callback when as session is successfully finished, but a session doesn't belong to
    * the current action.
    *
    * @param session the session
    */
   public void onOtherSessionSuccess(Session session) {
      logger.debug("onOtherSessionSuccess(): {}", session.getType());
      if (!requestAndUpdateBlockedOverlay(getBlockingActions())) {
         onResumeSession();
      }
   }

   /**
    * Callback when a session is finished with an error
    *
    * @param session the session
    * @param errorCode  error code
    */
   public abstract void onMyselfSessionError(Session session, ErrorCode errorCode);

   /**
    * Callback when a session is finished with an error, but a session doesn't belong to
    * the current action.
    *
    * @param session the session
    * @param errorCode  error code
    */
   public void onOtherSessionError(Session session, ErrorCode errorCode) {
      logger.debug("onOtherSessionError(): {}, {}", session.getType(), errorCode);
      if (!requestAndUpdateBlockedOverlay(getBlockingActions())) {
         onResumeSession();
      }
   }

   /**
    * Progress update from datasource.
    *
    * @param operation  the current operation
    * @param progress   current progress
    * @param max        total progress
    */
   @Override
   public void onProgressUpdate(String operation, int progress, int max) {
      logger.trace("onProgressUpdate(): {}, {}", progress, max);
   }

   @Override
   public void onSessionCancelled(Session session) {
      logger.trace("onSessionCancelled(): {}, {}", session.getType(), session.getAction());
      if (getExecutableActions().contains(session.getAction())) {
         onMyselfSessionCancelled(session);
      }
      else {
         onOtherSessionCancelled(session);
      }
   }

   /**
    * The own session was cancelled (e.g. unplugging USB stick while process was running)
    *
    * @param session Session that was cancelled.
    */
   protected void onMyselfSessionCancelled(Session session) {
      logger.debug("No further action performed as session was cancelled.");
   }

   /**
    * Another session was cancelled (e.g. unplugging USB stick while process was running)
    *
    * @param session Session that was cancelled.
    */
   protected void onOtherSessionCancelled(Session session) {
      //default behaviour on other session cancelled
      if (getBlockingActions().contains(session.getAction()) && !requestAndUpdateBlockedOverlay(getBlockingActions())) {
         onResumeSession();
      }
      //notify using a toast, which session has been cancelled
      Session.Action action = session.getAction();
      int cancelResourceId = R.string.cancel;
      if (action != null) {
         switch (action) {
         case EXPORT:
            cancelResourceId = R.string.export_cancel;
            break;
         case IMPORT:
            cancelResourceId = R.string.import_cancel;
            break;
         default:
            logger.error("Could not identify cancelled action!");
         }
         ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(cancelResourceId), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
               getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
      }
   }

   /**
    * Test whether a node is supported by the current format.
    * @param node the node
    * @return  <code>true</code> if supported, <code>false</code> otherwise.
    */
   public abstract boolean supportedByFormat(ObjectGraph node);

   /**
    * Defines actions which (if currently active) block / prohibit execution of fragment
    *
    * @return List of Session.Actions prohibiting fragments UI
    */
   protected abstract List<Session.Action> getBlockingActions();

   /**
    * Defines actions which can be executed by the current fragment
    *
    * @return List of Session.Actions prohibiting fragments UI
    */
   protected abstract List<Session.Action> getExecutableActions();

   @Override
   public void onChannelConnectionChange(boolean updateNeeded) {
      logger.trace("onChannelConnectionChange(): {}", updateNeeded);
   }

   /**
    * When user (de)selects a node in the tree
    */
   public void onTreeItemSelected() {
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.import_layout, container, false);
      LinearLayout leftPanel = (LinearLayout) layout.findViewById(R.id.left_panel_wrapper);
      inflateViews(inflater, leftPanel);

      disabledOverlay = (DataExchangeBlockedOverlay) layout.findViewById(R.id.disabled_overlay);
      processOverlay = (DataExchangeProcessOverlay) layout.findViewById(R.id.process_overlay);
      showLoadingOverlay();
      return layout;
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      selectAllBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            makeAllTreeSelection();
         }
      });

      treeViewList.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
   }

   /**
    * This method enables drag and drop for treeViewList
    */
   protected void enableDragAndDropForTreeView() {
      treeViewList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
         @Override
         public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (!treeAdapter.getSelectionMap().containsKey(view.getTag())) {
               treeAdapter.handleItemClick(parent, view, position, view.getTag()); //update selection
               view.setPressed(false); //Item would be shown/marked as pressed while in OnItemLongClickListener
            }

            //use different shadow builder depending on number of selected items
            View.DragShadowBuilder shadowBuilder = null;
            if (treeAdapter.getSelectionMap().size() == 1) {
               shadowBuilder = new View.DragShadowBuilder(view);
            }
            else {
               shadowBuilder = new TreeDragMultipleSelectionShadowBuilder(countSelectedItem(), getResources());
            }
            view.startDrag(null, shadowBuilder, treeAdapter.getSelected(), 0);

            return false;
         }
      });
   }

   @Override
   public void onPause() {
      super.onPause();
      logger.debug("onPause()");

      sessionManager.resetView();
   }

   @Override
   public void onResume() {
      super.onResume();
      logger.debug("onResume()");

      sessionManager.setView(this);
      if (sessionManager.isServiceConnected()) {
         onResumeSession();
      }

      ((DataManagementActivity) getActivity()).hideSubheader();
   }

   /**
    * Return active session that belongs to the current action (fragment)
    *
    * @return  the session
    */
   @NotNull
   protected Session getSession() {
      return sessionManager.getCurrentSession(getAction());
   }

   /**
    * Helper function to invoke discovery on the session manager
    */
   protected void discovery() {
      discovery(null);
   }

   /**
    * Helper function to invoke discovery on the session manager
    *
    * @param extra      session extra (path, format, type)
    */
   protected void discovery(SessionExtra extra) {
      if (sessionManager != null) {
         sessionManager.discovery(extra);
      }
      else {
         logger.debug("discovery(): Session manager is null");
      }
   }

   /**
    * Helper function to invoke performOperations on the session manager
    *
    * @param extra   session extra (path, format, type)
    * @param operations    list of operation
    */
   protected void performOperations(SessionExtra extra, List<Operation> operations) {
      if (sessionManager != null) {
         sessionManager.performOperations(extra, operations);
      }
      else {
         logger.debug("performOperations(): Session manager is null");
      }
   }

   /**
    * Helper function to invoke calculateConflicts on the session manager
    *
    * @param operations    list of operation
    */
   protected void calculateConflicts(List<Operation> operations) {
      if (sessionManager != null) {
         sessionManager.calculateConflicts(operations);
      }
      else {
         logger.debug("calculateConflicts(): Session manager is null");
      }
   }

   /**
    * Helper function to invoke calculateOperations on the session manager
    *
    * @param objectGraphs  list of object graph data
    */
   protected void calculateOperations(List<ObjectGraph> objectGraphs) {
      if (sessionManager != null) {
         sessionManager.calculateOperations(objectGraphs);
      }
      else {
         logger.debug("calculateOperations(): Session manager is null");
      }
   }

   /**
    * Helper function to invoke update on the session manager
    *
    * @param operations    list of operation
    */
   protected void update(List<Operation> operations) {
      if (sessionManager != null) {
         sessionManager.update(operations);
      }
      else {
         logger.debug("update(): Session manager is null");
      }
   }

   /**
    * Helper function to invoke copy paste on the session manager
    *
    * @param operations    list of operation
    */
   protected void paste(List<Operation> operations) {
      if (sessionManager != null) {
         sessionManager.paste(operations);
      }
      else {
         logger.debug("paste(): Session manager is null");
      }
   }

   /**
    * Helper function to invoke delete on the session manager
    *
    * @param operations    list of operation
    */
   protected void delete(List<Operation> operations) {
      if (sessionManager != null) {
         sessionManager.delete(operations);
      }
      else {
         logger.debug("delete(): Session manager is null");
      }
   }

   /**
    * Helper function to invoke cancel on the session manager
    */
   protected void cancel() {
      if (sessionManager != null) {
         sessionManager.cancel();
      }
      else {
         logger.debug("cancel(): Session manager is null");
      }
   }

   @Override
   public void onMediumUpdate() {
      logger.trace("onMediumUpdate");
   }

   /**
    * Change text string for the header UI element
    *
    * @param text text string
    */
   protected void setHeaderText(String text) {
      if (headerTextView != null) {
         headerTextView.setText(text);
      }
   }

   /**
    * Change text string for the header UI element
    *
    * @param id   resourcd ID
    */
   protected void setHeaderText(int id) {
      setHeaderText(getResources().getString(id));
   }

   /**
    * Make the tree list UI element visible
    */
   protected void showTreeList() {
      if (treeViewList != null) {
         treeViewList.setVisibility(View.VISIBLE);
      }
   }

   /**
    * Hide the tree list UI element
    */
   protected void hideTreeList() {
      if (treeViewList != null) {
         treeViewList.setVisibility(View.GONE);
      }
   }

   /**
    * Make the disabled overlay UI element visible and switch its mode to LOADING
    */
   protected void showLoadingOverlay() {
      if (disabledOverlay != null) {
         disabledOverlay.setMode(DataExchangeBlockedOverlay.MODE.LOADING);
      }
   }

   /**
    * Make the disabled overlay UI element visible and switch its mode to DISCONNECTED
    */
   protected void showDisconnectedOverlay() {
      if (disabledOverlay != null) {
         disabledOverlay.setMode(DataExchangeBlockedOverlay.MODE.DISCONNECTED);
      }
   }

   /**
    * Hide the disabled overlay UI element
    */
   protected void hideDisabledOverlay() {
      if (disabledOverlay != null) {
         disabledOverlay.setMode(DataExchangeBlockedOverlay.MODE.HIDDEN);
      }
   }

   /**
    * Create tree-adapter if no tree adapter is set
    */
   protected void createTreeAdapter() {
      if (treeAdapter == null) {
         treeAdapter = new ObjectTreeViewAdapter(getActivity(), manager, 1) {

            @Override
            protected boolean isGroupableEntity(ObjectGraph node) {
               return TreeEntityHelper.obj2group.containsKey(node.getType()) || node.getParent() == null;
            }

            @Override
            public boolean isSupportedEntity(ObjectGraph node) {
               return supportedByFormat(node);
            }

            @Override
            protected boolean childSelectionDoesImplicitSelectParentNodes(ObjectGraph rootNode) {
               if (rootNode != null) {
                  List<String> rootNodeNamesWithAutomaticParentSelection = getRootNodeNamesWithAutomaticParentSelection();
                  if (rootNodeNamesWithAutomaticParentSelection != null) {
                     return rootNodeNamesWithAutomaticParentSelection.contains(rootNode.getName());
                  }
               }
               return false;
            }

         };
      }
   }

   /**
    * Returns the list of node names that should automatic select their parents
    *
    * @return List of node names that should automatic select their parents
    */
   @Nullable
   protected List<String> getRootNodeNamesWithAutomaticParentSelection() {
      if (dataTreeRootNodesWithAutomaticParentSelection == null) {
         try {
            dataTreeRootNodesWithAutomaticParentSelection = Arrays.asList(getResources().getStringArray(getRootNodeNamesWithImplicitSelectionResourceId()));
         }
         catch (IllegalStateException e) {
            logger.error("Could not load Resources, since ExportFragment is not attached to activity!");
         }
      }
      return dataTreeRootNodesWithAutomaticParentSelection;
   }

   /**
    * Returns the resource-id of the array of root node names that support implicit selection in the specific fragment
    *
    * @return An integer representing the list of root node names that support implicit selection
    */
   protected abstract int getRootNodeNamesWithImplicitSelectionResourceId();

   /**
    * Clear selection on the tree items.
    */
   protected void clearTreeSelection() {
      if (treeAdapter != null) {
         treeAdapter.selectAll(treeViewList, false);
      }
   }

   /**
    * Create and initialize tree manager, builder & adapter. Populate tree list with
    * ObjectGraph data.
    *
    * @param objectGraphs  ObjectGraph data to populate the tree list
    */
   protected void initAndPopulateTree(List<ObjectGraph> objectGraphs) {
      logger.debug("initAndPopulateTree()");

      if (manager == null) {
         manager = new InMemoryTreeStateManager<ObjectGraph>();
      }
      else {
         manager.clear();
      }

      if (treeBuilder == null) {
         treeBuilder = new TreeBuilder<ObjectGraph>(manager);
      }
      else {
         treeBuilder.clear();
      }
      if (isDsPerfFlag()) {
         TreeEntityHelper.LoadDMTreeFromJson(this.getActivity(), treeBuilder);
         jsonAddToTree(objectGraphs);
      }
      else {
         addToTree(objectGraphs);
      }
      sortTreeList();
      createTreeAdapter();

      treeAdapter.setData(objectGraphs);
      treeViewList.removeAllViewsInLayout();
      treeViewList.setVisibility(View.VISIBLE);//this is for UT
      treeViewList.setAdapter(treeAdapter);
      treeAdapter.selectAll(treeViewList, false);//to clean all the previous selection
      manager.collapseChildren(null);
      treeAdapter.setOnTreeItemSelectedListener(new SelectionTreeViewAdapter.OnTreeItemSelectedListener() {
         @Override
         public void onItemSelected() {
            logger.debug("onTreeItemSelected");
            onTreeItemSelected();
         }
      });
   }

   /**
    * Resumes the tree view session with all data contained and selections made
    * in the current (/old) session.
    */
   protected void restoreTreeViewSession() {
      Session session = getSession();
      if (session != null) {
         treeAdapter.setData(getSession().getObjectData());
         treeViewList.setAdapter(treeAdapter);
         treeViewList.post(new Runnable() {
            @Override
            public void run() {
               treeAdapter.updateViewSelection(treeViewList);
               treeAdapter.setListeners(treeViewList);
            }
         });
      }
      else {
         logger.error("Could not restore tree session! Recreating datatree (loss of data and selection)!");
         initAndPopulateTree(new ArrayList<ObjectGraph>());
      }
   }

   /**
    * adding object(s) to treeview
    * @param  objectGraphs objects to add
    */
   protected void addToTree(List<ObjectGraph> objectGraphs) {
      boolean bVisible = false;
      if (objectGraphs != null && !objectGraphs.isEmpty()) {
         for (ObjectGraph o : objectGraphs) {
            if (bAddToTree(o.getParent(), o) && !bVisible) {
               bVisible = true;
            }
         }
         if (bVisible) {
            dataChangedRefresh();
         }
      }
   }

   //notify the treeview data change
   private void dataChangedRefresh() {
      manager.refresh();
   }

   /**
    * adding object(s) to treeview using json.
    * @param  objectGraphs objects to add
    */
   private void jsonAddToTree(List<ObjectGraph> objectGraphs) {
      for (ObjectGraph objectGraph : objectGraphs) {
         GroupObjectGraph gNode = TreeEntityHelper.findGroupNode(objectGraph.getType());
         if (gNode != null) {
            addOneObjectGraph(gNode, objectGraph);
         }
      }
   }

   /**
    * add object to treeview using json.
    * @param  objectGraph objects to add
    */
   private void addOneObjectGraph(ObjectGraph parent, ObjectGraph objectGraph) {
      if (objectGraph != null) {
         treeBuilder.bAddRelation(parent, objectGraph);
         for (ObjectGraph children : objectGraph.getChildren()) {
            addOneObjectGraph(objectGraph, children);
         }
      }
   }

   //Put the object into tree item list and return true for it is visible and false for invisible
   private boolean bAddToTree(ObjectGraph parent, ObjectGraph object) {
      boolean bVisible = false;
      try {
         if (DataTypes.DDOP.equals(object.getType()) || DataTypes.GUIDANCE_PATTERN.equals(object.getType()) || DataTypes.FILE.equals(object.getType())) {
            return bVisible;
         }
         //Check if entity can be grouped
         String objectType = object.getType();
         if (TreeEntityHelper.isGroup(objectType) || object.getParent() == null) {
            String gType = TreeEntityHelper.getGroupType(objectType);
            String ggType = TreeEntityHelper.getGroupOfGroupType(objectType);
            GroupObjectGraph group = null;
            GroupObjectGraph gGroup = null;
            for (ObjectGraph child : manager.getChildren(parent)) { //find the group node
               if (child instanceof GroupObjectGraph) {
                  if (child.getType().equals(gType)) {
                     group = (GroupObjectGraph) child;
                     break;
                  }
                  else if (TreeEntityHelper.isGroupOfGroup(objectType) && child.getType().equals(ggType)) {
                     gGroup = (GroupObjectGraph) child;
                     for (ObjectGraph cchild : manager.getChildren(child)) {
                        if (child instanceof GroupObjectGraph && cchild.getType().equals(gType)) {
                           group = (GroupObjectGraph) cchild;
                           break;
                        }
                     }
                     break;
                  }
               }
            }
            if (group == null) { //if group node doesn't exist we gotta make it
               if (TreeEntityHelper.isGroupOfGroup(objectType)) {
                  if (gGroup == null) {
                     gGroup = new GroupObjectGraph(null, ggType, TreeEntityHelper.getGroupName(getActivity(), ggType), null, parent);
                     if (treeBuilder.bAddRelation(parent, gGroup) && !bVisible) {
                        bVisible = true;
                     }
                  }
                  group = new GroupObjectGraph(null, gType, TreeEntityHelper.getGroupName(getActivity(), gType), null, gGroup);
                  if (treeBuilder.bAddRelation(gGroup, group) && !bVisible) {
                     bVisible = true;
                  }
               }
               else {
                  group = new GroupObjectGraph(null, gType, TreeEntityHelper.getGroupName(getActivity(), gType), null, parent);
                  if (treeBuilder.bAddRelation(parent, group) && !bVisible) {
                     bVisible = true;
                  }
               }
            }
            if (treeBuilder.bAddRelation(group, object) && !bVisible) {
               bVisible = true;
            }
         }
         //Else just add to parent
         else {
            if (treeBuilder.bAddRelation(parent, object) && !bVisible) {
               bVisible = true;
            }
         }
         for (ObjectGraph child : object.getChildren()) {
            if (bAddToTree(object, child) && !bVisible) {
               bVisible = true;
            }
         }
      }
      catch (NodeAlreadyInTreeException e) {
         logger.warn("Caught NodeAlreadyInTree exception", e);
      }
      return bVisible;
   }

   /**
    * Return the tree adapter instance.
    *
    * @return  the tree adapter
    */
   public ObjectTreeViewAdapter getTreeAdapter() {
      return treeAdapter;
   }

   /**
    * Count the number of selected items that have only valid records.
    *
    * @return  the number of selected items on the tree UI
    */
   protected int countSelectedItem() {
      int sum = 0;

      if (getTreeAdapter() != null) {
         Set<ObjectGraph> parentSet = new HashSet<ObjectGraph>();
         Set<ObjectGraph> selectedObjs = getTreeAdapter().getSelected();
         for (ObjectGraph obj : selectedObjs) {
            sum += obj.size();

            ObjectGraph parent = obj.getParent();
            while (parent != null) {
               if (!parentSet.contains(parent)) {
                  sum++;
                  parentSet.add(parent);
               }
               parent = parent.getParent();
            }
         }
      }
      return sum;
   }

   /**
    * Select all items in the tree and update UI accordingly.
    */
   protected void makeAllTreeSelection() {
      treeAdapter.selectAll(treeViewList, !getTreeAdapter().areAllSelected());
      updateSelectAllState();
      treeAdapter.refresh();
   }

   /**
    * Enable/Disable the 'Select All' UI button
    *
    * @param enable  flat to set enable/disable
    */
   protected void enableSelectAllButton(boolean enable) {
      if (selectAllBtn != null) {
         selectAllBtn.setEnabled(enable);
      }
   }

   /**
    * Given the current session state, chagne text & UI states for 'Select All'  button.
    */
   protected void updateSelectAllState() {
      boolean allSelected = false;
      boolean enable = false;
      final Session session = getSession();
      final ObjectTreeViewAdapter treeViewAdapter = getTreeAdapter();

      if (treeViewAdapter != null && treeViewAdapter.getCount() > 0 && session != null) {
         if (SessionUtil.isImportAction(session)) {
            enable = !SessionUtil.isInProgress(session) && session.getObjectData() != null && !session.getObjectData().isEmpty();
         }
         else {
            enable = !SessionUtil.isInProgress(session) && session.getObjectData() != null;
         }
         allSelected = treeViewAdapter.areAllSelected();
      }

      logger.debug("Enable Select All button: {}", enable);
      enableSelectAllButton(enable);
      selectAllBtn.setText(allSelected ? R.string.deselect_all : R.string.select_all);
      selectAllBtn.setActivated(allSelected);
   }

   @Override
   public void onDataServiceConnectionChange(boolean connected) {
      logger.debug("onDataServiceConnectionChange(): {}", connected);
      if (connected) {
         onResumeSession();
      }
   }

   /**
    * Updates the blocked overlay, returns weather it is shown or not.
    * @param blockingActions List of Actions that are allowed to block fragments
    * @return True if blocking overlay is shown afterwards, False otherwise
    */
   protected boolean requestAndUpdateBlockedOverlay(@Nullable List<Session.Action> blockingActions) {
      if (blockingActions != null && sessionManager != null && !DataExchangeBlockedOverlay.MODE.DISCONNECTED.equals(disabledOverlay.getMode())) {
         for (Session.Action action : blockingActions) {
            if (action != null) {
               //To show the blocking overlay the following constraints must be valid:
               // - the current action task is actively running
               // - the current action task is NOT a discovery task
               // - the current action task has not been cancelled
               if (sessionManager.actionIsActive(action) && sessionManager.getCurrentSession(action).getType() != Session.Type.DISCOVERY
                     && !Process.Result.CANCEL.equals(sessionManager.getCurrentSession(action).getResultCode())) {
                  //found blocking session, determine reason
                  final DataExchangeBlockedOverlay.MODE overlayMode;
                  switch (action) {
                  case IMPORT:
                     overlayMode = DataExchangeBlockedOverlay.MODE.BLOCKED_BY_IMPORT;
                     break;
                  case EXPORT:
                     overlayMode = DataExchangeBlockedOverlay.MODE.BLOCKED_BY_EXPORT;
                     break;
                  default:
                     overlayMode = DataExchangeBlockedOverlay.MODE.HIDDEN;
                     logger.error("Could not determine blocked-by state of data exchange fragment! Hiding blocked by overlay!");
                     break;
                  }
                  disabledOverlay.setMode(overlayMode);
                  return true; //true:enabled blocking overlay
               }
            }
            else {
               logger.warn("Got empty action in requestAndUpdateBlockedOverlay.");
            }
         }
         //No blocking action found, hide overlay
         disabledOverlay.setMode(DataExchangeBlockedOverlay.MODE.HIDDEN);
      }
      return false; //false:disabled blocking overlay
   }
}
