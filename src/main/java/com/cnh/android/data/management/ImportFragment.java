/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.android.data.management;

import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.cnh.android.data.management.adapter.ConflictResolutionViewAdapter;
import com.cnh.android.data.management.adapter.DataManagementBaseAdapter;
import com.cnh.android.data.management.adapter.ObjectTreeViewAdapater;
import com.cnh.android.data.management.adapter.PathTreeViewAdapter;
import com.cnh.android.data.management.adapter.TargetProcessViewAdapter;
import com.cnh.android.data.management.dialog.PathDialog;
import com.cnh.android.data.management.dialog.ProcessDialog;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.jgroups.Slf4jLogImpl;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import roboguice.fragment.provided.RoboFragment;

/**
 * Import Export Test Fragment, Fragment test copying objects from a source datasource to destination datasource. This fragment will be replaced when DataManagement, Import, and
 * Export UI is finalized.
 *
 * @author oscar.salazar@cnhind.com
 */
public class ImportFragment extends RoboFragment implements Mediator.ProgressListener {
   private static final Logger logger = LoggerFactory.getLogger(ImportFragment.class);

   private TabActivity activity;
   private ProcessDialog processDialog;
   private PathDialog pathDialog;
   private boolean ignoreNewViews = false;

   private PickListEditable sourcePick;
   private PickListEditable destinationPick;
   private TextView sourceDir;
   private TextView destDir;
   private TreeViewList treeView;
   private TreeStateManager<ObjectGraph> manager;
   private ObjectTreeViewAdapater treeAdapter;
   private TreeBuilder<ObjectGraph> treeBuilder;
   private Button continueBtn;
   private volatile Long sdCardId;
   private volatile Long destSdCardId;
   private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
         logger.info("Reconnecting to cluster.  Preferences have changed.");
         if (mediator != null) {
            mediator.close();
         }
         new ConnectTask().execute();
      }
   };

   @Inject
   @Named("global")
   private SharedPreferences prefs;

   @Inject
   private Provider<Mediator> mediatorProvider;
   private Mediator mediator;

   private List<Address> members = null;
   private Address destinationAddr = null;
   private Handler handler = new Handler();

   static {
      System.setProperty(Global.CUSTOM_LOG_FACTORY, Slf4jLogImpl.class.getName());
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      activity = (TabActivity) getActivity();
      prefs.registerOnSharedPreferenceChangeListener(listener);
   }

   @Override
   public void onResume() {
      super.onResume();
      new ConnectTask().execute();
   }

   @Override
   public void onPause() {
      if (mediator != null) {
         mediator.close();
      }
      super.onPause();
   }

   @Override
   public void onDestroy() {
      prefs.unregisterOnSharedPreferenceChangeListener(listener);
      doneOperation();
      super.onDestroy();
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.test_fragment, container, false);
      sourcePick = (PickListEditable) layout.findViewById(R.id.source_pick);
      destinationPick = (PickListEditable) layout.findViewById(R.id.destination_pick);
      sourceDir = (TextView) layout.findViewById(R.id.source_dir);
      destDir = (TextView) layout.findViewById(R.id.dest_dir);
      treeView = (TreeViewList) layout.findViewById(R.id.tree_view);
      treeView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
      continueBtn = (Button) layout.findViewById(R.id.btn_continue);
      return layout;
   }

   /**
    * Called by mediator when ever the target datasource publishes progress back...
    * Updates progressbar on ProgressDialog
    *
    * @param operation current Current operation being performed (i.e. calculateConflicts)
    * @param progress current progress. 0 <= progress <= max
    * @param max max progress
    */
   public void onProgressPublished(String operation, int progress, int max) {
      logger.debug(String.format("publishProgress(%s, %d, %d)", operation, progress, max));
      final Double percent = ((progress * 1.0) / max) * 100;
      activity.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            processDialog.setProgress(percent.intValue());
         }
      });
   }

   @Override
   public void onSecondaryProgressPublished(String secondaryOperation, int secondaryProgress, int secondaryMax) {
      //Widget Progress Bar doesn't support secondary progress.... or any string descriptions of operation...
   }

   /**
    * Called by mediator when this application has joined the jgroups cluster
    *
    * @param view View includes all members of this jgroups cluster.
    */
   public void onViewAccepted(org.jgroups.View view) {
      logger.debug("Accepted view (" + view.size() + view.getMembers() + ')');
      members = view.getMembers();
      if (!ignoreNewViews) {
         onDataSourceListChange();
      }
   }

   private synchronized void onDataSourceListChange() {
      getActivity().runOnUiThread(new Runnable() {
         @Override
         public void run() {
            if (sourcePick.getAdapter() != null) {
               sourcePick.clearList();
            }
            sourcePick.setAdapter(new PickListAdapter(sourcePick, getActivity().getApplicationContext()));

            for (int i = 0; i < members.size(); i++) {
               if (!members.get(i).equals(mediator.getAddress())) {
                  sourcePick.addItem(new ObjectPickListItem<Address>(i, members.get(i).toString(), members.get(i)));
               }
            }

            //Add source for sdcard, will be actual device later
            sdCardId = sourcePick.getNextId();
            sourcePick.addItem(new PickListItem(sdCardId, "sdcard"));

            sourcePick.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> parent, View view, int position, final long id) {
                  if (id != PickListEditable.NO_ID) {
                     clearTree();
                     if (id == sdCardId) {
                        sourceDir.setVisibility(View.VISIBLE);
                        sourceDir.setText("/");
                        sourceDir.setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View v) {
                              logger.debug("onclick");
                              pathDialog = new PathDialog(activity);
                              pathDialog.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
                                 @Override
                                 public void onPathSelected(File path) {
                                    ignoreNewViews = true;
                                    sourceDir.setText(path.getAbsolutePath());
                                    Intent i = new Intent();
                                    i.setAction("com.cnh.pf.data.EXTERNAL_DATA");
                                    i.putExtra("paths", new String[] { path.getAbsolutePath() });
                                    i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                    i.putExtra("create", false);
                                    activity.sendBroadcast(i);

                                    handler.postDelayed(new Runnable() {
                                       @Override
                                       public void run() {
                                          List<Address> addrs = getSources(Datasource.Source.USB);
                                          if (!addrs.isEmpty()) {
                                             new DiscoveryTask().execute(addrs.toArray(new Address[addrs.size()]));
                                          }
                                       }
                                    }, 4000); //TODO need to investigate a more precise delay for datasources to register.
                                 }
                              });
                              activity.showPopup(pathDialog, true);
                           }
                        });
                     }
                     else {
                        ObjectPickListItem<Address> item = (ObjectPickListItem<Address>) sourcePick.findItemById(id);
                        logger.debug("Selected DataSource:" + item.getObject().toString());
                        ignoreNewViews = false;
                        sourceDir.setVisibility(View.GONE);
                        new DiscoveryTask().execute(item.getObject());
                     }
                  }
               }

               @Override
               public void onNothingSelected(AdapterView<?> parent) {

               }
            });
            popDest();
         }
      });
   }

   public List<Address> getSources(Datasource.Source source) {
      List<Address> addrs = new ArrayList<Address>();
      try {
         RspList<Datasource.Source[]> rsp = mediator.getAllSources();
         for (Map.Entry<Address, Rsp<Datasource.Source[]>> entry : rsp.entrySet()) {
            Rsp<Datasource.Source[]> response = entry.getValue();
            if (response.hasException()) {
               logger.warn("Couldn't receive sources from {}" + UUID.get(entry.getKey()));
               continue;
            }
            if (response.wasReceived() && Arrays.asList(entry.getValue().getValue()).contains(source)) {
               logger.info("addr: " + entry.getKey().toString() + " is valid datasource");
               addrs.add(entry.getKey());
            }
            else {
               logger.info("addr: " + entry.getKey().toString() + " is not valid datasource");
            }
         }
      }
      catch (Exception e) {
         logger.error("Exception getSources", e);
      }
      return addrs;
   }

   private void popDest() {
      if (destinationPick.getAdapter() != null) {
         destinationPick.clearList();
      }
      destinationPick.setAdapter(new PickListAdapter(destinationPick, getActivity().getApplicationContext()));
      for (int i = 0; i < members.size(); i++) {
         Address addr = members.get(i);
         if (!addr.equals(mediator.getAddress())) {
            destinationPick.addItem(new ObjectPickListItem<Address>(i, addr.toString(), addr));
         }
      }

      //Add destination for sdcard, will be actual device later
      destSdCardId = destinationPick.getNextId();
      destinationPick.addItem(new PickListItem(sdCardId, "sdcard"));

      destinationPick.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (id == destSdCardId) {
               destDir.setVisibility(View.VISIBLE);
               destDir.setText("/");
               destDir.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                     logger.debug("onclick");
                     pathDialog = new PathDialog(activity);
                     pathDialog.setOnPathSelectedListener(new PathTreeViewAdapter.OnPathSelectedListener() {
                        @Override
                        public void onPathSelected(File path) {
                           ignoreNewViews = true;
                           destDir.setText(path.getAbsolutePath());
                           Intent i = new Intent();
                           i.setAction("com.cnh.pf.data.EXTERNAL_DATA");
                           i.putExtra("paths", new String[] { path.getAbsolutePath() });
                           i.putExtra("create", true);
                           i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                           activity.sendBroadcast(i);

                           handler.postDelayed(new Runnable() {
                              @Override
                              public void run() {
                                 List<Address> addrs = getSources(Datasource.Source.USB);
                                 if (!addrs.isEmpty()) {
                                    destinationAddr = addrs.get(0);
                                    logger.debug("Setting destination addr to:" + destinationAddr);
                                    //TODO bad shortcut
                                 }
                              }
                           }, 4000);    //TODO need to investigate a more precise delay for datasources to register.
                        }

                     });
                     activity.showPopup(pathDialog, true);
                  }
               });
            }
            else {
               destDir.setVisibility(View.GONE);
               if (id != PickListEditable.NO_ID) {
                  ObjectPickListItem<Address> item = (ObjectPickListItem<Address>) destinationPick.findItemById(id);
                  logger.debug("Selected Destination DataSource:" + item.getObject().toString());
                  destinationAddr = item.getObject();
               }
            }
         }

         @Override
         public void onNothingSelected(AdapterView<?> parent) {

         }
      });
   }

   private void clearTree() {
      if (manager != null) {
         manager.clear();
      }
      continueBtn.setVisibility(View.GONE);
      destinationPick.clearSelection();
   }

   private void addToTree(ObjectGraph parent, ObjectGraph object) {
      if (parent == null) {
         treeBuilder.sequentiallyAddNextNode(object, 0);
      }
      else {
         treeBuilder.addRelation(parent, object);
      }
      //group children of field by type
      if (object.getType().equals("com.cnh.pf.model.pfds.Field")) {
         Map<String, ObjectGraph> groups = new HashMap<String, ObjectGraph>();
         for (ObjectGraph child : object.getChildren()) {
            final String type = child.getType();
            if (!groups.containsKey(type)) {
               ObjectGraph g = new ObjectGraph(null, type + "_Group", type.substring(type.lastIndexOf('.') + 1) + "s", null, object);
               groups.put(type, g);
               treeBuilder.addRelation(object, g);
            }
            addToTree(groups.get(type), child);
         }
      }
      else {
         for (ObjectGraph child : object.getChildren()) {
            addToTree(object, child);
         }
      }
   }

   private void onNewObjectGraphReceived(final Pair<Datasource.DataType, List<ObjectGraph>> result) {
      if (result != null && !result.second.isEmpty()) {
         manager = new InMemoryTreeStateManager<ObjectGraph>();
         treeBuilder = new TreeBuilder<ObjectGraph>(manager);
         treeView.removeAllViewsInLayout();
         treeView.setVisibility(View.VISIBLE);
         ObjectGraph root = new ObjectGraph(null, result.first.name(), getResources().getString(R.string.pfds));
         addToTree(null, root);
         for (ObjectGraph graph : result.second) {
            addToTree(root, graph);
         }
         treeAdapter = new ObjectTreeViewAdapater(getActivity(), manager, 1);
         treeAdapter.setData(result.second);
         treeView.setAdapter(treeAdapter);
         //         manager.collapseChildren(null); //Collapse all children
         continueBtn.setVisibility(View.VISIBLE);
         continueBtn.setEnabled(true);
         continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if (destinationAddr != null) {
                  if (!treeAdapter.getSelected().isEmpty()) {
                     continueBtn.setEnabled(false);
                     List<ObjectGraph> objs = new ArrayList<ObjectGraph>(treeAdapter.getSelected());
                     new CalculateTargetsTask().execute(Pair.create(destinationAddr.toString(), objs));
                  }
                  else {
                     logger.debug("Nothing selected");
                     Toast.makeText(getActivity().getApplicationContext(), "Select Objects to Import first", Toast.LENGTH_LONG).show();
                  }
               }
               else {
                  Toast.makeText(getActivity().getApplicationContext(), "Select Destination First", Toast.LENGTH_LONG).show();
               }
            }
         });
      }
      else {
         Toast.makeText(getActivity().getApplicationContext(), "No Data Found", Toast.LENGTH_LONG).show();
      }
   }

   private void doneOperation() {
      logger.debug("Sending STOP Intent");
      Intent i = new Intent();
      i.setAction("com.cnh.pf.data.EXTERNAL_DATA_STOP");
      activity.sendBroadcast(i);
   }

   //Set Target for graph to handle partial imports(Inserts parent if not in destination)
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

   private class ConnectTask extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground(Void... params) {
         try {
            System.setProperty(Global.IPv4, "true");
            //get new instance each time, preferences may have changed
            mediator = mediatorProvider.get();
            mediator.setProgressListener(ImportFragment.this);
            mediator.start();
         }
         catch (Exception e) {
            logger.error("", e);
         }
         return null;
      }
   }

   private class DiscoveryTask extends AsyncTask<Address, Void, Pair<Datasource.DataType, List<ObjectGraph>>> {

      @Override
      protected Pair<Datasource.DataType, List<ObjectGraph>> doInBackground(Address... params) {
         logger.debug("Discovery for " + params[0].toString() + " source...");
         try {
            List<ObjectGraph> data = mediator.discovery(params);
            Datasource.DataType type = mediator.getDatatypes(params[0])[0];
            return new Pair<Datasource.DataType, List<ObjectGraph>>(type, data);
         }
         catch (Exception e) {
            showExceptionDialog(e);
         }
         return null;
      }

      @Override
      protected void onPostExecute(Pair<Datasource.DataType, List<ObjectGraph>> result) {
         super.onPostExecute(result);
         if (result == null)
            return;
         onNewObjectGraphReceived(result);
      }
   }

   private class CalculateTargetsTask extends ProgressTask<Pair<String, List<ObjectGraph>>, Void, List<Operation>> {

      @Override
      protected void onPreExecute() {
         super.onPreExecute();
         processDialog.setTitle(getResources().getString(R.string.checking_targets));
      }

      @Override
      protected List<Operation> doInBackground(Pair<String, List<ObjectGraph>>... params) {

         String dst = params[0].first;
         List<ObjectGraph> objs = params[0].second;
         logger.debug("Calculate Targets...");
         try {
            return mediator.calculateOperations(destinationAddr, objs);
         }
         catch (Exception e) {
            logger.error("Send exception", e);
         }
         return null;
      }

      @Override
      protected void onPostExecute(List<Operation> operations) {
         super.onPostExecute(operations);

         operations = processPartialImports(operations);
         boolean hasMultipleTargets = false;
         for (Operation operation : operations) {
            if (operation.getPotentialTargets() != null && operation.getPotentialTargets().size() > 1) {
               hasMultipleTargets = true;
               break;
            }
         }
         // Only show Target Dialog if one operation has more than one target
         if (hasMultipleTargets) {
            TargetProcessViewAdapter adapter = new TargetProcessViewAdapter(activity, operations);
            processDialog.setAdapter(adapter);
            processDialog.setTitle(getResources().getString(R.string.select_target));
            processDialog.clearLoading();
            adapter.setOnTargetsSelectedListener(new TargetProcessViewAdapter.OnTargetsSelectedListener() {
               @Override
               public void onCompletion(List<Operation> operations) {
                  logger.debug("onCompletion, operations: " + operations.toString());
                  processDialog.hide();
                  new CalculateConflictsTask().execute(Pair.create(destinationAddr.toString(), operations));
               }
            });
            processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialog, int which) {
                  if (which == DialogViewInterface.BUTTON_FIRST) {
                     // user pressed "Cancel" button
                     dialog.dismiss();
                     continueBtn.setEnabled(true);
                  }
               }
            });
            processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
               @Override
               public void onDismiss(DialogViewInterface dialog) {
                  processDialog.hide();
               }
            });
         }
         else {
            processDialog.hide();
            new CalculateConflictsTask().execute(Pair.create(destinationAddr.toString(), operations));
         }
      }
   }

   private void showExceptionDialog(Exception e) {
      final DialogView dialogView = new DialogView(activity);
      TextView tv = new TextView(activity);
      tv.setText(String.format("Error: %s\nCause: %s\nStack Trace:\n%s",
         e.toString(),
         Throwables.getRootCause(e),
         Throwables.getStackTraceAsString(e)));
      tv.setTextSize(16);
      dialogView.setBodyView(tv);
      dialogView.setFirstButtonText("Done");
      dialogView.showSecondButton(false);
      dialogView.showThirdButton(false);
      dialogView.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
         @Override
         public void onButtonClick(DialogViewInterface dialog, int which) {
            if (which == DialogViewInterface.BUTTON_FIRST) {
               // user pressed "Done" button
               dialogView.dismiss();
               activity.dismissPopup(dialogView);
            }
         }
      });
      activity.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            activity.showPopup(dialogView, false);
         }
      });
   }

   private class CalculateConflictsTask extends ProgressTask<Pair<String, List<Operation>>, Void, List<Operation>> {

      @Override
      protected void onPreExecute() {
         super.onPreExecute();
         processDialog.setTitle(getResources().getString(R.string.checking_conflicts));
      }

      @Override
      protected List<Operation> doInBackground(Pair<String, List<Operation>>... params) {
         String dst = params[0].first;
         List<Operation> objs = params[0].second;
         logger.debug("Calculate Conflicts...");
         try {
            return mediator.calculateConflicts(destinationAddr, objs);
         }
         catch (Exception e) {
            logger.error("Send exception", e);
            showExceptionDialog(e);
            this.cancel(true);
         }
         return null;
      }

      @Override
      protected void onPostExecute(List<Operation> operations) {
         super.onPostExecute(operations);
         logger.debug("Got conflicts: " + operations.toString());
         //Check for conflicts
         boolean hasConflicts = false;
         for (Operation operation : operations) {
            hasConflicts |= operation.isConflict();
         }
         if (hasConflicts) {
            ConflictResolutionViewAdapter adapter = new ConflictResolutionViewAdapter(activity, operations);
            processDialog.setAdapter(adapter);
            processDialog.clearLoading();
            adapter.setOnTargetsSelectedListener(new DataManagementBaseAdapter.OnTargetsSelectedListener() {
               @Override
               public void onCompletion(List<Operation> operations) {
                  logger.debug("onCompletion");
                  processDialog.hide();
                  if (operations.size() > 0) {
                     new PerformOperationsTask().execute(Pair.create(destinationAddr, operations));
                  }
               }
            });
            processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialog, int which) {
                  if (which == DialogViewInterface.BUTTON_FIRST) {
                     // user pressed "Cancel" button
                     dialog.dismiss();
                     continueBtn.setEnabled(true);
                  }
               }
            });
            processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
               @Override
               public void onDismiss(DialogViewInterface dialog) {
                  processDialog.hide();
               }
            });
         }
         else {
            processDialog.hide();
            if (operations.size() > 0) {
               new PerformOperationsTask().execute(Pair.create(destinationAddr, operations));
            }
         }
      }
   }

   private class PerformOperationsTask extends ProgressTask<Pair<Address, List<Operation>>, Void, Void> {

      @Override
      protected void onPreExecute() {
         super.onPreExecute();
         processDialog.setTitle(getResources().getString(R.string.importing));
      }

      @Override
      protected Void doInBackground(Pair<Address, List<Operation>>... params) {
         List<Operation> objs = params[0].second;
         logger.debug("Performing Operations...");
         try {
            mediator.performOperations(params[0].first, objs);
         }
         catch (Exception e) {
            logger.error("Send exception", e);
            showExceptionDialog(e);
         }
         return null;
      }

      @Override
      protected void onPostExecute(Void error) {
         super.onPostExecute(error);
         processDialog.setFirstButtonText("Done");
         processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
            @Override
            public void onButtonClick(DialogViewInterface dialog, int which) {
               if (which == DialogViewInterface.BUTTON_FIRST) {
                  // user pressed "Done" button
                  processDialog.hide();
                  continueBtn.setEnabled(true);
                  sourcePick.clearSelection();
                  clearTree();
                  doneOperation();
               }
            }
         });
      }
   }

   private abstract class ProgressTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

      @Override
      protected void onPreExecute() {
         super.onPreExecute();
         logger.debug("onPreExecute");
         processDialog = new ProcessDialog(activity);
         processDialog.show();
      }

      @Override
      protected void onPostExecute(Result result) {
         super.onPostExecute(result);
         logger.debug("onPostExecute");
      }

      @Override
      protected void onCancelled() {
         super.onCancelled();
         logger.debug("onCancelled");
         processDialog.hide();
      }
   }
}
