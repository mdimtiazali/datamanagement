package com.cnh.pf.android.data.management;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.dialog.EditDialog;
import com.cnh.pf.android.data.management.graph.GroupObjectGraph;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.datamng.Process;

import org.jgroups.util.RspList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.polidea.treeview.ImplicitSelectLinearLayout;
import pl.polidea.treeview.TreeNodeInfo;

/**
 * Provide data management Tab implementation
 * Created by f09953c on 9/12/2017.
 */
public class ManageFragment extends BaseDataFragment {
   private static final Logger logger = LoggerFactory.getLogger(ManageFragment.class);
   ImageButton delBtn;
   TextView header;
   Set<String> copySet;
   Set<String> editSet;
   List<ObjectGraph> data;
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
               for (ObjectGraph obj : data) {
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
                  getSession().setObjectData(null);//avoid data transfer
                  getSession().setData(operations);
                  getSession().setSessionOperation(DataManagementSession.SessionOperation.UPDATE);
                  getDataManagementService().processOperation(getSession(), DataManagementSession.SessionOperation.UPDATE);
                  if (updatingProg == null) {
                     updatingProg = new ProgressDialog(getActivity());
                     updatingProg.setTitle(R.string.edit_update_title);
                  }
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
            Toast.makeText(getActivity(), "Copy click on node " + nodeInfo.toString(), Toast.LENGTH_LONG).show();
         }
         }
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      copySet = new HashSet<String>(Arrays.asList(getResources().getStringArray(R.array.copy)));
      editSet = new HashSet<String>(Arrays.asList(getResources().getStringArray(R.array.edit)));
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.manage_layout, container, false);
      LinearLayout leftPanel = (LinearLayout) layout.findViewById(R.id.left_panel_wrapper);
      inflateViews(inflater, leftPanel);
      disabled = (DisabledOverlay) layout.findViewById(R.id.disabled_overlay);
      header = (TextView) layout.findViewById(R.id.path_tv);
      delBtn = (ImageButton) layout.findViewById(R.id.dm_delete_button);
      if (delBtn != null) delBtn.setEnabled(false);
      return layout;
   }

   void selectAll() {
      logger.debug("selectAll()");
      treeAdapter.selectAll(treeViewList, !getTreeAdapter().areAllSelected());
      updateSelectAllState();
      treeAdapter.refresh();
   }

   @Override
   public void inflateViews(LayoutInflater inflater, View leftPanel) {
      leftPanel.setVisibility(View.GONE);
   }

   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      startText.setVisibility(View.GONE);
   }

   @Override
   public DataManagementSession createSession() {
      logger.trace("createSession()");
      super.onNewSession();
      DataManagementSession oldSession = getSession();

      treeViewList.setVisibility(View.GONE);

      if (hasLocalSource) {
         disabled.setVisibility(View.GONE);
         treeProgress.setVisibility(View.VISIBLE);
      }
      else {
         logger.trace("hasLocalSource is false, return null");
         disabled.setVisibility(View.VISIBLE);
         disabled.setMode(DisabledOverlay.MODE.DISCONNECTED);
         return null;
      }
      DataManagementSession session = new DataManagementSession(new Datasource.Source[] { Datasource.Source.INTERNAL, Datasource.Source.DISPLAY }, null, null, null);
      sessionInit(session);
      if (oldSession != null) {
         session.setFormat(oldSession.getFormat());
         session.setTargets(oldSession.getTargets());
      }
      return session;
   }

   @Override
   public void configSession(DataManagementSession session) {
      sessionInit(session);//only consider one case
   }

   private DataManagementSession sessionInit(DataManagementSession session) {
      if (session != null) {
         session.setSourceTypes(new Datasource.Source[] { Datasource.Source.INTERNAL, Datasource.Source.DISPLAY });
         session.setSources(null);
         session.setDestinationTypes(null);
         session.setTargets(null);
      }
      return session;
   }

   @Override
   public void setSession(DataManagementSession session) {
      super.setSession(session);

   }

   @Override
   protected void onErrorOperation() {
      if (getSession().getSessionOperation().equals(DataManagementSession.SessionOperation.DISCOVERY)) {
         idleUI();
      }
      else {
         logger.debug("Other operations when error");
         sessionInit(getSession());
         //            postTreeUI();
      }
   }

   @Override
   public void processOperations() {
      if (getSession().getResult() != null && getSession().getResult().equals(Process.Result.ERROR)) {
         if (getSession().getSessionOperation().equals(DataManagementSession.SessionOperation.DISCOVERY)) {
            idleUI();
         }
         else {
            logger.debug("Other operations when error");
            sessionInit(getSession());
            postTreeUI();
         }
      }
      else if (getSession().getSessionOperation().equals(DataManagementSession.SessionOperation.UPDATE)) {
         if (getSession().getResult() != null) {
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
   }

   @Override
   public boolean isCurrentOperation(DataManagementSession session) {
      logger.trace("isCurrentOperation( {} == {})", session, getSession());
      return session.equals(getSession());
   }

   @Override
   public void onTreeItemSelected() {
      super.onTreeItemSelected();
      if (treeAdapter.getSelectionMap().size() > 0) {
         delBtn.setEnabled(true);
         header.setText(getResources().getString(R.string.tab_mng_items_header, treeAdapter.getSelectionMap().size()));
      }
      else {
         delBtn.setEnabled(false);
         header.setText(getResources().getString(R.string.tab_mng_none_header));
      }
   }

   /** Since the tree list on Management tab requires different behaviors (edit,delete,...),
    * this fragment doesn't use tree adapter provided by the BaseDataFragment.
    */
   @Override
   public void createTreeAdapter() {
      if (treeAdapter == null) {
         treeAdapter = new ObjectTreeViewAdapter(getActivity(), manager, 1) {
            @Override
            public boolean isSupportedEdit(ObjectGraph node) {
               if (node instanceof GroupObjectGraph || !editSet.contains(node.getType())) {
                  return false;
               }
               return true;
            }

            @Override
            public boolean isSupportedCopy(ObjectGraph node) {
               return copySet.contains(node.getType());
            }

            @Override
            protected boolean isGroupableEntity(ObjectGraph node) {
               return TreeEntityHelper.groupables.containsKey(node.getType()) || node.getParent() == null;
            }

            @Override
            public boolean isSupportedEntitiy(ObjectGraph node) {
               return supportedByFormat(node);
            }

            @Override
            public View getNewChildView(TreeNodeInfo<ObjectGraph> treeNodeInfo) {
               final View view = getActivity().getLayoutInflater().inflate(R.layout.tree_list_item_with_edit, null);
               logger.debug("getNewChildView() {}", treeNodeInfo.getId());
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

            @Override
            public View updateView(View view, TreeNodeInfo treeNodeInfo) {
               ObjectGraph graph = (ObjectGraph) treeNodeInfo.getId();
               logger.trace("updateView(): Node is {}", graph.getName());
               final TextView nameView = (TextView) view.findViewById(R.id.tree_list_item_text);
               final ImageButton cpButton = (ImageButton) view.findViewById(R.id.mng_copy_button);
               cpButton.setTag(graph);
               cpButton.setOnClickListener(optListener);
               final ImageButton editButton = (ImageButton) view.findViewById(R.id.mng_edit_button);
               editButton.setTag(graph);
               editButton.setOnClickListener(optListener);
               nameView.setText(graph.getName());
               nameView.setTextColor(getActivity().getResources().getColorStateList(R.color.tree_text_color));
               if (TreeEntityHelper.hasIcon(graph.getType()) && (graph instanceof GroupObjectGraph || !isGroupableEntity(graph))) {
                  nameView.setCompoundDrawablesWithIntrinsicBounds(TreeEntityHelper.getIcon(graph.getType()), 0, 0, 0);
               }
               else {
                  nameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
               }
               indicatorShown(view, treeNodeInfo);
               return view;
            }

            @Override
            public void updateViewSelection(final AdapterView<?> parent) {

               for (int i = 0; i < parent.getChildCount(); i++) {
                  View child = parent.getChildAt(i);
                  ObjectGraph node = (ObjectGraph) child.getTag(); //tree associates ObjectGraph with each view
                  if (node == null) continue;
                  ImplicitSelectLinearLayout layout = (ImplicitSelectLinearLayout) child;
                  layout.setSupported(isSupportedEntitiy(node));

                  final ImageButton cpButton = (ImageButton) child.findViewById(R.id.mng_copy_button);
                  final ImageButton editButton = (ImageButton) child.findViewById(R.id.mng_edit_button);

                  if (getSelectionMap().containsKey(node)) {
                     SelectionType type = getSelectionMap().get(node);
                     layout.setSelected(SelectionType.FULL.equals(type));
                     layout.setImplicitlySelected(SelectionType.IMPLICIT.equals(type));
                     if (isSupportedCopy(node)) {
                        cpButton.setVisibility(View.VISIBLE);
                     }
                     if (isSupportedEdit(node)) {
                        editButton.setVisibility(View.VISIBLE);
                     }
                  }
                  else {
                     layout.setSelected(false);
                     layout.setImplicitlySelected(false);
                     cpButton.setVisibility(View.INVISIBLE);
                     editButton.setVisibility(View.INVISIBLE);
                  }
               }
            }

            @Override
            protected int getTreeListItemWrapperId() {
               //   return super.getTreeListItemWrapperId();
               return R.layout.tree_list_item_wrapper_full_screen;
            }

            @Override
            public void selectionImpl(Object id) {
               ObjectGraph start = (ObjectGraph) id;
               logger.trace("selectionImpl({})", start.getName());
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
         treeAdapter.getSelectionMap().clear();
      }
   }

   @Override
   protected void initializeTree() {
      super.initializeTree();
      // display header message when tree items get populated.
      header.setText(getResources().getString(R.string.tab_mng_none_header));
   }

   @Override
   public void onProgressPublished(String operation, int progress, int max) {
      logger.debug("onProgressPublished: {}", progress);
   }

   @Override
   public boolean supportedByFormat(ObjectGraph node) {
      return true;
   }

   @Override
   protected void onOtherSessionUpdate(DataManagementSession session) {
      logger.debug("Other session has been updated");
      if (session.getSessionOperation().equals(DataManagementSession.SessionOperation.PERFORM_OPERATIONS)) {
         logger.trace("Other operation completed.");
      }
   }

   @Override
   public void onMediumsUpdated(List<MediumDevice> mediums) throws RemoteException {

   }
}
