/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.pf.widget.controls.ToastMessageCustom;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.dialog.DeleteDialog;
import com.cnh.pf.android.data.management.dialog.EditDialog;
import com.cnh.pf.android.data.management.dialog.UndeleteObjectDialog;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.pf.android.data.management.helper.DataExchangeBlockedOverlay;
import com.cnh.pf.android.data.management.helper.DmAccessibleObserver;
import com.cnh.pf.android.data.management.misc.DeleteButton;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.datamng.Process;
import com.google.inject.Inject;

import org.jgroups.util.RspList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import pl.polidea.treeview.ImplicitSelectLinearLayout;
import pl.polidea.treeview.TreeNodeInfo;

/**
 * Provide data management Tab implementation
 * Created by f09953c on 9/12/2017.
 */
public class ManageFragment extends BaseDataFragment implements DmAccessibleObserver.Listener {
   private static final Logger logger = LoggerFactory.getLogger(ManageFragment.class);

   private DeleteButton delBtn;
   private TextView header;
   private Set<String> copySet;
   private Set<String> editSet;
   private ProgressDialog updatingProg;
   private ProgressDialog deletingProg;
   private volatile boolean isAccessable = true;
   @Inject
   private DmAccessibleObserver statusHelper;

   private final List<Session.Action> blockingActions = new ArrayList<Session.Action>(Arrays.asList(Session.Action.IMPORT, Session.Action.EXPORT));

   @Override
   protected List<Session.Action> getBlockingActions() {
      return blockingActions;
   }

   @Override
   public void onAccessStatus(boolean status) {
      logger.debug("onAccessStatus({}), isAccessable={}", status, isAccessable);
      if (isAccessable != status) {
         isAccessable = status;
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               freshUdcUi();
            }
         });
      }
      isAccessable = status;
   }

   final View.OnClickListener optListener = new View.OnClickListener() {
      @Override
      public void onClick(View v) {
         switch (v.getId()) {
         case R.id.mng_edit_button: {
            final ObjectGraph objectGraph = (ObjectGraph) v.getTag();
            Set<String> names = new HashSet<String>();//same name warning
            ObjectGraph parent = objectGraph.getParent();
            if (parent != null) {
               for (ObjectGraph child : parent.getChildren()) {
                  names.add(child.getName());
               }
            }
            else {//if there is no parent, get its entire type group
               for (ObjectGraph obj : treeAdapter.getData()) {//if there is no parent, should be root object
                  if (obj.getType().equals(objectGraph.getType())) {
                     names.add(obj.getName());
                  }
               }
            }
            names.remove(objectGraph.getName());
            EditDialog editDialog = new EditDialog(getActivity(), names);
            editDialog.setDefaultStr(objectGraph.getName());
            editDialog.setUserSelectCallback(new EditDialog.UserSelectCallback() {
               @Override
               public void inputStr(String name) {
                  //two things need to do here 1) update item name 2) update the backend with new name
                  List<Operation> operations = new ArrayList<Operation>(1);
                  Operation op = new Operation(objectGraph.copyUp(), null);
                  op.setNewName(name);
                  operations.add(op);
                  update(operations);

                  if (updatingProg == null) {
                     updatingProg = new ProgressDialog(getActivity(), ProgressDialog.THEME_HOLO_LIGHT);
                     updatingProg.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_circle));
                     updatingProg.setMessage(getString(R.string.edit_update_content));
                  }
                  setHeaderAndDeleteButton(false);
                  updatingProg.show();
               }
            });
            final TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(editDialog);
            break;
         }
         case R.id.mng_copy_button: {
            ObjectGraph nodeInfo = (ObjectGraph) v.getTag();
            //Todo: need to support delete
            ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.copy_click_string) + nodeInfo.toString(),
                  Gravity.TOP | Gravity.CENTER_HORIZONTAL, getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset))
                  .show();
         }
         }
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      logger.debug("onCreate()");
      super.onCreate(savedInstanceState);
      copySet = new HashSet<String>(Arrays.asList(getResources().getStringArray(R.array.copy)));
      editSet = new HashSet<String>(Arrays.asList(getResources().getStringArray(R.array.edit)));
      statusHelper.setListener(this);
   }

   @Override
   public void onStart() {
      super.onStart();
      new AsyncTask<Void, Void, Void>() {
         @Override
         protected Void doInBackground(Void... voids) {
            statusHelper.start();
            return null;
         }
      }.execute();
   }

   @Override
   public void onStop() {
      logger.debug("onStop()");
      super.onStop();
      new AsyncTask<Void, Void, Void>() {
         @Override
         protected Void doInBackground(Void... voids) {
            statusHelper.stop();
            return null;
         }
      }.execute();

   }

   /**
    * refresh edit delete copy UI
    */
   private void freshUdcUi() {
      if (treeAdapter != null && treeAdapter.hasSelection()) {
         if (isAccessable) {
            enableDeleteButton(true);
         }
         else {
            enableDeleteButton(false);
         }
         treeAdapter.refresh();
      }
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.manage_layout, container, false);
      LinearLayout leftPanel = (LinearLayout) layout.findViewById(R.id.left_panel_wrapper);
      inflateViews(inflater, leftPanel);
      disabledOverlay = (DataExchangeBlockedOverlay) layout.findViewById(R.id.disabled_overlay);
      delBtn = (DeleteButton) layout.findViewById(R.id.dm_delete_button);
      delBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            final DeleteDialog delDialog = new DeleteDialog(getActivity(), countSelectedItem());
            delDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialogViewInterface, int i) {
                  if (i == DeleteDialog.BUTTON_FIRST) {
                     Set<ObjectGraph> set = getTreeAdapter().getSelectedRootNodes();
                     List<ObjectGraph> filteredList = new ArrayList<ObjectGraph>(set.size());
                     for (ObjectGraph o : set) {
                        if (!TreeEntityHelper.isGroupType(o.getType())) {
                           filteredList.add(o);
                        }
                     }
                     if (!filteredList.isEmpty()) {
                        List<Operation> delOperations = new ArrayList<Operation>();
                        for (ObjectGraph obj : filteredList) {
                           Operation operation = new Operation(obj, null);
                           operation.setStatus(Operation.Status.NOT_DONE);
                           delOperations.add(operation);
                        }
                        delete(delOperations);
                        if (deletingProg == null) {
                           deletingProg = new ProgressDialog(getActivity(), ProgressDialog.THEME_HOLO_LIGHT);
                           deletingProg.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_circle));
                           deletingProg.setMessage(getString(R.string.delete_progress_content));
                        }
                        deletingProg.show();
                     }
                     else {
                        ToastMessageCustom
                              .makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.no_data_for_delete), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                                    getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset))
                              .show();
                     }
                  }
               }
            });
            ((TabActivity) getActivity()).showPopup(delDialog, true);
         }
      });
      delBtn.setPopContentProvider(new DeleteButton.PopContentProvider() {
         @Override
         public int getStringResourceId() {
            if (!isAccessable) {
               return R.string.delete_select_notice_inaccessible;
            }
            return R.string.delete_select_notice;
         }
      });
      delBtn.setEnabled(false);
      return layout;
   }

   @Override
   public void inflateViews(LayoutInflater inflater, View leftPanel) {
      leftPanel.setVisibility(View.GONE);
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);

      delBtn.setEnabled(false);
   }

   @Override
   public void onPCMDisconnected() {
      logger.trace("PCM is not online.");
      hideTreeList();
      showDisconnectedOverlay();
      updateSelectAllState();
      setHeaderText("");
   }

   @Override
   public void onPCMConnected() {
      logger.trace("PCM is online.");

      showLoadingOverlay();
      onResumeSession();
   }

   @Override
   public void onResumeSession() {
      logger.debug("onResumeSession()");
      //verify that no other session is blocking fragment
      if (!requestAndUpdateBlockedOverlay(getBlockingActions())) {
         hideTreeList();
         showLoadingOverlay();
         discovery();
      }
   }

   @Override
   public void onChannelConnectionChange(boolean updateNeeded) {
      logger.debug("onChannelConnectionChange");

      if (updateNeeded) {
         hideTreeList();
         showLoadingOverlay();

         discovery();
      }
   }

   @Override
   public void onMyselfSessionSuccess(Session session) {
      logger.debug("onMyselfSessionComplete(): {}", session.getType());
      if (SessionUtil.isDiscoveryTask(session)) {
         initAndPouplateTree(session.getObjectData());

         hideDisabledOverlay();
         showTreeList();
         updateSelectAllState();
         setHeaderText(R.string.tab_mng_none_header);
      }
      else if (SessionUtil.isUpdateTask(session)) {
         if (session.getResultCode() != null) {
            if (updatingProg != null) {
               updatingProg.dismiss();
            }
            RspList<Process> rsps = session.getResults();
            for (Process process : rsps.getResults()) {
               for (Operation operation : process.getOperations()) {
                  ObjectGraph objG = operation.getData();
                  //update adaptor data with new name and ui
                  if (manager != null && treeAdapter != null) {
                     List<ObjectGraph> list = manager.getVisibleList();
                     int position = 0;
                     for (ObjectGraph o : list) {
                        if (o.equals(objG)) {
                           treeAdapter.updateNodeName(o, operation.getNewName());
                           treeAdapter.updateItemView(treeViewList, position);
                        }
                        position++;
                     }
                  }
               }
            }
         }
      }
      else if (SessionUtil.isDeleteTask(session)) {
         if (session.getResultCode() != null) {
            if (deletingProg != null) {
               deletingProg.dismiss();
            }
            RspList<Process> rsps = session.getResults();
            final List<ObjectGraph> removedObjects = new LinkedList<ObjectGraph>();
            final List<ObjectGraph> undeletedObjects = new LinkedList<ObjectGraph>();
            for (Process process : rsps.getResults()) {
               if (process.getOperations() != null) {
                  for (Operation operation : process.getOperations()) {
                     if (operation != null && operation.getStatus() == Operation.Status.DONE) {
                        removedObjects.addAll(findRemovedObjects(operation.getData(), operation.getUnprocessedData()));
                        if (operation.getUnprocessedData() != null) {
                           undeletedObjects.add(operation.getUnprocessedData());
                        }
                     }
                  }
               }
            }
            if (undeletedObjects.isEmpty()) {
               removeAndRefreshObjectUI(removedObjects);
            }
            else {
               removeAndRefreshObjectUI(removedObjects);
               UndeleteObjectDialog undeleteObjectDialog = new UndeleteObjectDialog(getActivity(), undeletedObjects);
               undeleteObjectDialog.setBodyHeight(320);
               undeleteObjectDialog.setTitle(getString(R.string.undelete_objects_headline));
               ((TabActivity) getActivity()).showPopup(undeleteObjectDialog, true);
            }
         }
      }
   }

   //remove data and refresh UI
   private void removeAndRefreshObjectUI(List<ObjectGraph> objects) {
      treeAdapter.removeObjectGraphs(objects);
      treeAdapter.updateViewSelection(treeViewList);
      manager.removeNodesRecursively(objects);
      removeParentEmptyGroup(objects);
      setHeaderAndDeleteButton(false);
   }

   //true for empty group false for not
   private boolean isEmptyGroup(ObjectGraph objectGraph) {
      if (objectGraph instanceof GroupObjectGraph) {
         List<ObjectGraph> children = manager.getChildren(objectGraph);
         if (children == null || children.isEmpty()) {
            return true;
         }
         else {
            for (ObjectGraph o : children) {
               if (!isEmptyGroup(o)) {
                  return false;
               }
            }
            return true;
         }
      }
      return false;
   }

   //remove the empty group item
   private void removeParentEmptyGroup(List<ObjectGraph> list) {
      Set<ObjectGraph> emptyGroup = new HashSet<ObjectGraph>();
      List<ObjectGraph> siblingsOrParent = null;
      for (ObjectGraph o : list) {
         if (TreeEntityHelper.obj2group.containsKey(o.getType())) {
            if (TreeEntityHelper.group2group.containsKey(TreeEntityHelper.obj2group.get(o.getType()))) {
               List<ObjectGraph> ggoups = manager.getChildren(o.getParent());
               if (ggoups != null && !ggoups.isEmpty()) {
                  for (ObjectGraph gg : ggoups) {
                     if (gg.getType().equals(TreeEntityHelper.group2group.get(TreeEntityHelper.obj2group.get(o.getType())))) {
                        siblingsOrParent = manager.getChildren(gg);
                        break;
                     }
                  }
               }
            }
            else {
               siblingsOrParent = manager.getChildren(o.getParent());
            }
            if (siblingsOrParent != null && !siblingsOrParent.isEmpty()) {
               for (ObjectGraph obj : siblingsOrParent) {
                  if (obj instanceof GroupObjectGraph && isEmptyGroup(obj) && !emptyGroup.contains(obj)) {
                     emptyGroup.add(obj);
                  }
               }
            }
         }
      }
      if (!emptyGroup.isEmpty()) {
         manager.removeNodesRecursively(new LinkedList<ObjectGraph>(emptyGroup));
      }
   }

   // find out what object in change were removed compared with base object.
   private List<ObjectGraph> findRemovedObjects(ObjectGraph origin, ObjectGraph change) {
      final List<ObjectGraph> removedObjs = new LinkedList<ObjectGraph>();
      if (change == null) {
         if (origin == null) {
            return removedObjs;
         }
         else {
            removedObjs.add(origin);
         }
      }
      else if (origin == null) {
         return removedObjs;
      }
      else {
         final Set<ObjectGraph> set = new HashSet<ObjectGraph>();
         ObjectGraph.traverse(change, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph objectGraph) {
               set.add(objectGraph);
               return true;
            }
         });

         ObjectGraph.traverse(origin, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph objectGraph) {
               if (set.contains(objectGraph)) {
                  return true;
               }
               else {
                  removedObjs.add(objectGraph);
                  return false;
               }
            }
         });
      }
      return removedObjs;
   }

   @Override
   public void onMyselfSessionError(Session session, ErrorCode errorCode) {
      logger.debug("onMyselfSessionError(): {}, {}", session.getType(), errorCode);

      if (SessionUtil.isDeleteTask(session)) {
         if (deletingProg != null) {
            deletingProg.dismiss();
         }
         setHeaderAndDeleteButton(false);
         ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.delete_error_notice), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
               getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
         discovery();
      }
      else if (SessionUtil.isUpdateTask(session)) {
         if (updatingProg != null) {
            updatingProg.dismiss();
         }
         ToastMessageCustom.makeToastMessageText(getActivity().getApplicationContext(), getString(R.string.update_error_notice), Gravity.TOP | Gravity.CENTER_HORIZONTAL,
               getResources().getInteger(R.integer.toast_message_xoffset), getResources().getInteger(R.integer.toast_message_yoffset)).show();
      }
   }

   private void enableDeleteButton(boolean enable) {
      if (isAccessable && enable) {
         delBtn.setEnabled(true);
      }
      else {
         delBtn.setEnabled(false);
      }
   }

   private void setHeaderAndDeleteButton(boolean enable) {
      if (enable) {
         enableDeleteButton(true);
         int count = countSelectedItem();
         logger.info("The # of selected items: {}", count);
         setHeaderText(getResources().getQuantityString(R.plurals.tab_mng_selected_items_header, count, count));
      }
      else {
         delBtn.setEnabled(false);
         setHeaderText(R.string.tab_mng_none_header);
      }
   }

   @Override
   public void onTreeItemSelected() {
      super.onTreeItemSelected();
      if (treeAdapter.getSelectionMap().size() > 0) {
         setHeaderAndDeleteButton(true);
      }
      else {
         setHeaderAndDeleteButton(false);
      }
   }

   /**
    * Since the tree list on Management tab requires different behaviors (edit,delete,...),
    * this fragment doesn't use tree adapter provided by the BaseDataFragment.
    */
   @Override
   public void createTreeAdapter() {
      if (treeAdapter == null) {
         treeAdapter = new ObjectTreeViewAdapter(getActivity(), manager, 1) {
            @Override
            public boolean isSupportedEdit(ObjectGraph node) {
               if (!isAccessable || node instanceof GroupObjectGraph || !editSet.contains(node.getType())) {
                  return false;
               }
               return true;
            }

            @Override
            public boolean isSupportedCopy(ObjectGraph node) {
               return isAccessable && copySet.contains(node.getType());
            }

            @Override
            protected boolean isGroupableEntity(ObjectGraph node) {
               return TreeEntityHelper.obj2group.containsKey(node.getType()) || node.getParent() == null;
            }

            @Override
            public boolean isSupportedEntitiy(ObjectGraph node) {
               return supportedByFormat(node);
            }

            @Override
            public View getNewChildView(TreeNodeInfo<ObjectGraph> treeNodeInfo) {
               final View view = getActivity().getLayoutInflater().inflate(R.layout.tree_list_item_with_edit, null);
               return updateView(view, treeNodeInfo);
            }

            boolean showInd = false;

            //1) if item is visible 2) if all the children is invisible and one of them was selected
            private void indicatorShown(View view, TreeNodeInfo treeNodeInfo) {
               ImageView ind = (ImageView) view.findViewById(R.id.mng_select_inditor);
               final ObjectGraph start = (ObjectGraph) treeNodeInfo.getId();
               if (!getSelectionMap().containsKey(start)) {
                  traverseTree(start, TRAVERSE_DOWN, new Visitor<ObjectGraph>() {
                     @Override
                     public boolean visit(ObjectGraph node) {
                        if (node == start) {
                           return true;
                        }
                        if (manager.getNodeInfo(node).isVisible()) {
                           showInd = false;
                           return false;
                        }
                        if (getSelectionMap().containsKey(node)) {
                           showInd = true;
                        }
                        return true;
                     }
                  });
               }
               if (showInd) {
                  ind.setVisibility(View.VISIBLE);
                  showInd = false;//reset
               }
               else {
                  ind.setVisibility(View.INVISIBLE);
               }
            }

            private void updateButtonVisible(ObjectGraph node, ImageButton copyButton, ImageButton editButton) {
               if (getSelectionMap().containsKey(node)) {
                  if (isSupportedCopy(node)) {
                     copyButton.setVisibility(View.VISIBLE);
                  }
                  else {
                     copyButton.setVisibility(View.INVISIBLE);
                  }
                  if (isSupportedEdit(node)) {
                     editButton.setVisibility(View.VISIBLE);
                  }
                  else {
                     editButton.setVisibility(View.INVISIBLE);
                  }
               }
               else {
                  copyButton.setVisibility(View.INVISIBLE);
                  editButton.setVisibility(View.INVISIBLE);
               }
            }

            @Override
            public View updateView(View view, TreeNodeInfo treeNodeInfo) {
               ObjectGraph graph = (ObjectGraph) treeNodeInfo.getId();
               final TextView nameView = (TextView) view.findViewById(R.id.tree_list_item_text);
               final ImageButton cpButton = (ImageButton) view.findViewById(R.id.mng_copy_button);
               cpButton.setTag(graph);
               cpButton.setOnClickListener(optListener);
               final ImageButton editButton = (ImageButton) view.findViewById(R.id.mng_edit_button);
               editButton.setTag(graph);
               editButton.setOnClickListener(optListener);
               updateButtonVisible(graph, cpButton, editButton);
               nameView.setText(graph.getName());
               nameView.setTextColor(getActivity().getResources().getColorStateList(R.color.tree_text_color));
               if (TreeEntityHelper.hasIcon(graph.getType()) && (graph instanceof GroupObjectGraph || !isGroupableEntity(graph))) {
                  nameView.setCompoundDrawablesWithIntrinsicBounds(TreeEntityHelper.getIcon(graph.getType()), 0, 0, 0);
               }
               else {
                  nameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
               }
               return view;
            }

            @Override
            public void updateViewSelection(final AdapterView<?> parent) {
               for (int i = 0; i < parent.getChildCount(); i++) {
                  View child = parent.getChildAt(i);
                  ObjectGraph node = (ObjectGraph) child.getTag(); //tree associates ObjectGraph with each view
                  if (node != null) {
                     ImplicitSelectLinearLayout layout = (ImplicitSelectLinearLayout) child;
                     layout.setSupported(isSupportedEntitiy(node));

                     final ImageButton cpButton = (ImageButton) child.findViewById(R.id.mng_copy_button);
                     final ImageButton editButton = (ImageButton) child.findViewById(R.id.mng_edit_button);

                     if (getSelectionMap().containsKey(node)) {
                        SelectionType type = getSelectionMap().get(node);
                        layout.setSelected(SelectionType.FULL.equals(type));
                        layout.setImplicitlySelected(SelectionType.IMPLICIT.equals(type));
                     }
                     else {
                        layout.setSelected(false);
                        layout.setImplicitlySelected(false);
                     }
                     updateButtonVisible(node, cpButton, editButton);
                     indicatorShown(child, manager.getNodeInfo(node));
                  }
               }
            }

            @Override
            protected int getTreeListItemWrapperId() {
               return R.layout.tree_list_item_wrapper_full_screen;
            }

            @Override
            public void selectionImpl(Object id) {
               ObjectGraph start = (ObjectGraph) id;
               if (getManager().getParent((ObjectGraph) id) != null && includeParent((ObjectGraph) id)) {
                  start = getManager().getParent((ObjectGraph) id);
               }
               //select itself and all its children
               if (!getSelectionMap().containsKey(start)) {
                  traverseTree(start, TRAVERSE_DOWN, new Visitor<ObjectGraph>() {
                     @Override
                     public boolean visit(ObjectGraph node) {
                        getSelectionMap().put(node, SelectionType.FULL);
                        return true;
                     }
                  });
               }
               else {//unselect all its direct parent
                  if (getManager().getParent(start) != null) {
                     traverseTree(getManager().getParent(start), TRAVERSE_UP, new Visitor<ObjectGraph>() {
                        @Override
                        public boolean visit(ObjectGraph node) {
                           if (getSelectionMap().containsKey(node)) {
                              getSelectionMap().remove(node);
                              return true;
                           }
                           return false;
                        }
                     });
                  }
                  //unselect itself and its all children
                  traverseTree(start, TRAVERSE_DOWN, new Visitor<ObjectGraph>() {
                     @Override
                     public boolean visit(ObjectGraph node) {
                        if (getSelectionMap().containsKey(node)) {
                           getSelectionMap().remove(node);
                           return true;
                        }
                        return false;
                     }
                  });
               }

            }

         };
      }
      else {
         treeAdapter.selectAll(treeViewList, false);
      }
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      return true;
   }

   @Override
   public Session.Action getAction() {
      return Session.Action.MANAGE;
   }
}
