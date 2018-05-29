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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;

import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.adapter.SelectionTreeViewAdapter;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.pf.android.data.management.helper.TreeDragShadowBuilder;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.SessionContract;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionExtra;

import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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

   protected DisabledOverlay disabledOverlay = null;
   protected TreeStateManager<ObjectGraph> manager;
   protected TreeBuilder<ObjectGraph> treeBuilder;
   protected ObjectTreeViewAdapter treeAdapter;

   protected static boolean isConnectedToPcm = false;

   private SessionContract.SessionManager sessionManager;

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
         int comparison = lhs.getId().getName().compareToIgnoreCase(rhs.getId().getName());
         if (comparison != 0) {
            return comparison;
         }
         // If the case-insensitive check is same, do the case-sensitive check to make upper-case come first
         return lhs.getId().getName().compareTo(rhs.getId().getName());
      }
   };

   /**
    * Sort a list of child nodes under each tree parent node.
    */
   protected void sortTreeList() {
      if (manager != null && manager instanceof SortableChildrenTree) {
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
   }

   /**
    * Callback when PCM is disconnected. PCM datasources are not joined in the channel.
    */
   public abstract void onPCMDisconnected();

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
   public abstract void onOtherSessionSuccess(Session session);

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
   public abstract void onOtherSessionError(Session session, ErrorCode errorCode);

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
   }

   /**
    * Test whether a node is supported by the current format.
    * @param node the node
    * @return  <code>true</code> if supported, <code>false</code> otherwise.
    */
   public abstract boolean supportedByFormat(ObjectGraph node);

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
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.import_layout, container, false);
      LinearLayout leftPanel = (LinearLayout) layout.findViewById(R.id.left_panel_wrapper);
      inflateViews(inflater, leftPanel);

      disabledOverlay = (DisabledOverlay)layout.findViewById(R.id.disabled_overlay);
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
         disabledOverlay.setMode(DisabledOverlay.MODE.LOADING);
         disabledOverlay.setVisibility(View.VISIBLE);
      }
   }

   /**
    * Make the disabled overlay UI element visible and switch its mode to DISCONNECTED
    */
   protected void showDisconnectedOverlay() {
      if (disabledOverlay != null) {
         disabledOverlay.setMode(DisabledOverlay.MODE.DISCONNECTED);
         disabledOverlay.setVisibility(View.VISIBLE);
      }
   }

   /**
    * Hide the disabled overlay UI element
    */
   protected void hideDisabledOverlay() {
      if (disabledOverlay != null) {
         disabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
         disabledOverlay.setVisibility(View.GONE);
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
            public boolean isSupportedEntitiy(ObjectGraph node) {
               return supportedByFormat(node);
            }
         };
      }
   }

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
   protected void initAndPouplateTree(List<ObjectGraph> objectGraphs) {
      logger.debug("initAndPouplateTree()");

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
      addToTree(objectGraphs);
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
    * adding object(s) to treeview
    * @param  objectGraphs objects to add
    */
   protected void addToTree(List<ObjectGraph> objectGraphs){
      boolean bVisible = false;
      if(objectGraphs!=null && !objectGraphs.isEmpty()){
         for(ObjectGraph o: objectGraphs){
            if(bAddToTree(o.getParent(), o) && !bVisible){
               bVisible = true;
            }
         }
         if(bVisible){
            dataChangedRefresh();
         }
      }
   }
   //notify the treeview data change
   private void dataChangedRefresh(){
      manager.refresh();
   }

   //Put the object into tree item list and return true for it is visible and false for invisible
   private boolean bAddToTree(ObjectGraph parent, ObjectGraph object) {
      boolean bVisible = false;
      try {
         //Check if entity can be grouped
         if (TreeEntityHelper.obj2group.containsKey(object.getType()) || object.getParent() == null) {
            GroupObjectGraph group = null;
            GroupObjectGraph ggroup = null;
            for (ObjectGraph child : manager.getChildren(parent)) { //find the group node
               if (child instanceof GroupObjectGraph && TreeEntityHelper.obj2group.containsKey(object.getType())) {
                  if (child.getType().equals(TreeEntityHelper.obj2group.get(object.getType()))) {
                     group = (GroupObjectGraph) child;//found the group
                     break;
                  } else if (TreeEntityHelper.group2group.containsKey(TreeEntityHelper.obj2group.get(object.getType())) &&
                          child.getType().equals(TreeEntityHelper.group2group.get(TreeEntityHelper.obj2group.get(object.getType())))) {
                     ggroup = (GroupObjectGraph) child;//found the group's parent
                     for (ObjectGraph cchild : manager.getChildren(child)) {
                        if (cchild instanceof GroupObjectGraph && cchild.getType().equals(TreeEntityHelper.obj2group.get(object.getType()))) {
                           group = (GroupObjectGraph) cchild;//found the group
                           break;
                        }
                     }
                  }
               }
            }
            if (group == null) { //if group node doesn't exist we gotta make it
               if(TreeEntityHelper.group2group.containsKey(TreeEntityHelper.obj2group.get(object.getType()))){
                  if(ggroup == null){
                     ggroup = new GroupObjectGraph(null, TreeEntityHelper.group2group.get(TreeEntityHelper.obj2group.get(object.getType())), TreeEntityHelper.getGroupName(getActivity(), TreeEntityHelper.group2group.get(TreeEntityHelper.obj2group.get(object.getType()))),null, parent);
                     if(treeBuilder.bAddRelation(parent,ggroup) && !bVisible){
                        bVisible = true;
                     }
                  }
                  group = new GroupObjectGraph(null, TreeEntityHelper.obj2group.get(object.getType()), TreeEntityHelper.getGroupName(getActivity(), TreeEntityHelper.obj2group.get(object.getType())), null, ggroup);
                  if(treeBuilder.bAddRelation(ggroup, group) && !bVisible){
                     bVisible = true;
                  }
               }
               else{
                  group = new GroupObjectGraph(null, TreeEntityHelper.obj2group.get(object.getType()), TreeEntityHelper.getGroupName(getActivity(), TreeEntityHelper.obj2group.get(object.getType())), null, parent);
                  if(treeBuilder.bAddRelation(parent,group) && !bVisible){
                     bVisible = true;
                  }
               }

            }
            if(treeBuilder.bAddRelation(group, object) && !bVisible){
               bVisible = true;
            }
         }
         //Else just add to parent
         else {
            if(treeBuilder.bAddRelation(parent, object) && !bVisible){
               bVisible = true;
            }
         }
         for (ObjectGraph child : object.getChildren()) {
            if(bAddToTree(object, child) && !bVisible){
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
         Set<ObjectGraph> selectedObjs = getTreeAdapter().getSelected();
         for (ObjectGraph obj : selectedObjs) {
            sum += obj.size();
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
      final Session session = getSession();
      boolean enable = session != null
              && session.getObjectData() != null
              && !session.getObjectData().isEmpty();

      logger.debug("Enable Select All button: {}", enable);
      enableSelectAllButton(enable);
      boolean allSelected = false;
      if (getTreeAdapter() != null) {
         allSelected = getTreeAdapter().areAllSelected();
      }
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
    * Set connection state to BaseDataFragment classes
    * @param isConnectedToPcm Connection state of pcm to display (true: connected, false: not connected)
    */
   protected static void setIsConnectedToPcm(boolean isConnectedToPcm) {
      BaseDataFragment.isConnectedToPcm = isConnectedToPcm;
   }
}
