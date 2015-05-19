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

import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ch.qos.logback.classic.android.BasicLogcatConfigurator;

import com.cnh.android.data.management.adapter.ConflictResolutionViewAdapter;
import com.cnh.android.data.management.adapter.DataManagementBaseAdapter;
import com.cnh.android.data.management.adapter.ObjectTreeViewAdapater;
import com.cnh.android.data.management.adapter.TargetProcessViewAdapter;
import com.cnh.android.data.management.dialog.ProcessDialog;
import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.jgroups.Slf4jLogImpl;

/**
 * Import Export Test Fragment
 * @author oscar.salazar@cnhind.com
 */
public class ImportFragment extends Fragment implements Mediator.ProgressListener {
   private static final Logger logger = LoggerFactory.getLogger(ImportFragment.class);

   private TabActivity activity;
   private ProcessDialog processDialog;

   private PickListEditable sourcePick;
   private PickListEditable destinationPick;
   private TreeViewList treeView;
   private TreeStateManager<ObjectGraph> manager;
   private ObjectTreeViewAdapater treeAdapter;
   private TreeBuilder<ObjectGraph> treeBuilder;
   private Button importBtn;

   private Mediator mediator;

   static {
      System.setProperty(Global.CUSTOM_LOG_FACTORY, Slf4jLogImpl.class.getName());
   }


   private JChannel channel;
   private RpcDispatcher disp;
   private List<Address> members = null;
   private Address destinationAddr = null;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      BasicLogcatConfigurator.configureDefaultContext();
      super.onCreate(savedInstanceState);
      activity = (TabActivity) getActivity();
      new ConnectTask().execute();
   }

   @Override
   public void onDestroy() {
      new AsyncTask<Void, Void, Void>() {
         @Override
         protected Void doInBackground(Void... params) {
            if (disp != null) {
               disp.stop();
               disp = null;
            }
            Util.close(channel);
            return null;
         }
      }.execute();
      super.onDestroy();
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.test_fragment, container, false);
      sourcePick = (PickListEditable) layout.findViewById(R.id.source_pick);
      destinationPick = (PickListEditable) layout.findViewById(R.id.destination_pick);
      treeView = (TreeViewList) layout.findViewById(R.id.tree_view);
      importBtn = (Button) layout.findViewById(R.id.btn_import);
      final TabActivity activity = (TabActivity) getActivity();
      return layout;
   }

   public void onProgressPublished(String operation, int progress, int max) {
      logger.debug(String.format("publishProgress(%s, %d, %d)", operation, progress, max));
      final Double percent = ((progress+1 * 1.0) / max) * 100;
      activity.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            processDialog.setProgress(percent.intValue());
         }
      });
   }

   public void onViewAccepted(org.jgroups.View view) {
      logger.debug("Accepted view (" + view.size() + view.getMembers() + ')');
      members = view.getMembers();
      onDataSourceListChange();
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
               if (!members.get(i).equals(channel.getAddress())) {
                  sourcePick.addItem(new PickListItem(i, members.get(i).toString()));
               }
            }

            sourcePick.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                  if (id != PickListEditable.NO_ID) {
                     logger.debug("Selected DataSource:" + members.get((int) id).toString());
                     new DiscoveryTask().execute(members.get((int) id));
                  }
               }

               @Override
               public void onNothingSelected(AdapterView<?> parent) {

               }
            });

            if (destinationPick.getAdapter() != null) {
               destinationPick.clearList();
            }
            destinationPick.setAdapter(new PickListAdapter(destinationPick, getActivity().getApplicationContext()));
            for (int i = 0; i < members.size(); i++) {
               Address addr = members.get(i);
               if (!addr.equals(channel.getAddress())) {
                  destinationPick.addItem(new PickListItem(i, addr.toString()));
               }
            }

            destinationPick.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                  if (id != PickListEditable.NO_ID) {
                     logger.debug("Selected Destination DataSource:" + members.get((int) id).toString());
                     destinationAddr = members.get((int) id);
                  }
               }

               @Override
               public void onNothingSelected(AdapterView<?> parent) {

               }
            });
         }
      });
   }

   private void addChildrenToTree(ObjectGraph parent, int parentLevel) {
      if (!parent.getChildren().isEmpty()) {
         for (ObjectGraph child : parent.getChildren()) {
            treeBuilder.sequentiallyAddNextNode(child, parentLevel+1);
            addChildrenToTree(child, parentLevel+1);
         }
      }
   }

   private void onNewObjectGraphReceived(final List<ObjectGraph> result) {
      manager = new InMemoryTreeStateManager<ObjectGraph>();
      treeBuilder = new TreeBuilder<ObjectGraph>(manager);
      treeView.removeAllViewsInLayout();
      treeView.setVisibility(View.VISIBLE);
      for (ObjectGraph graph : result) {
         treeBuilder.sequentiallyAddNextNode(graph, 0);
         addChildrenToTree(graph, 0);
      }
      treeAdapter = new ObjectTreeViewAdapater(getActivity(), manager, 1);
      treeAdapter.setData(result);
      treeView.setAdapter(treeAdapter);
      manager.collapseChildren(null); //Collapse all children
      importBtn.setVisibility(View.VISIBLE);
      importBtn.setEnabled(true);
      importBtn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            if (destinationAddr != null) {
               if (!treeAdapter.getSelected().isEmpty()) {
                  importBtn.setEnabled(false);
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

   private class ConnectTask extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground(Void... params) {
         try {

            logger.info("Connecting");
            System.setProperty(Global.IPv4, "true");

            //TODO Need smarter way of selecting jgroups configuration
//            UDP MULTICAST
                        channel = new JChannel(getResources().getAssets().open("jgroups.xml"));

            //GOSSIP DISCOVERY
            //            System.setProperty("jgroups.tcp.bind_port", "10000");
            //            System.setProperty(Global.EXTERNAL_ADDR, "10.0.0.11");
            //            System.setProperty(Global.EXTERNAL_PORT, "10000");
            //            System.setProperty("jgroups.tunnel.gossip_router_hosts", "10.0.0.11[12001]");
            //            channel = new JChannel(getResources().getAssets().open("jgroups-gossip.xml"));

            //GOSSIP ROUTING
            //            System.setProperty("jgroups.tunnel.gossip_router_hosts", "10.0.2.2[12001]");
//            System.setProperty("jgroups.tunnel.gossip_router_hosts", "192.168.88.5[12001]");
//            channel = new JChannel(getResources().getAssets().open("jgroups-tunnel.xml"));

            mediator = new Mediator(channel, "Android");
            mediator.setProgressListener(ImportFragment.this);
            mediator.start();
         }
         catch (Exception e) {
            logger.error("", e);
         }
         return null;
      }
   }

   private class DiscoveryTask extends AsyncTask<Address, Void, List<ObjectGraph>> {

      @Override
      protected List<ObjectGraph> doInBackground(Address... params) {
         logger.debug("Discovery for " + params[0].toString() + " source...");
         try {
            return mediator.discovery(params[0].toString());
         }
         catch (Exception e) {
            e.printStackTrace();
         }
         return null;
      }

      @Override
      protected void onPostExecute(List<ObjectGraph> result) {
         super.onPostExecute(result);
         if (result == null) return;
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
            return mediator.calculateOperations(destinationAddr.toString(), objs);
         }
         catch (Exception e) {
            logger.error("Send exception", e);
         }
         return null;
      }

      @Override
      protected void onPostExecute(List<Operation> operations) {
         super.onPostExecute(operations);

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
                     importBtn.setEnabled(true);
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
            return mediator.calculateConflicts(destinationAddr.toString(), objs);
         }
         catch (Exception e) {
            logger.error("Send exception", e);

            //TODO Need to create Exception Dialog when an execption from backed is thrown.
            final DialogView dialogView = new DialogView(activity);
            TextView tv = new TextView(activity);
            tv.setText("Got Exception from backend: " + e.toString());
            tv.setTextSize(28);
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
               if (operation.isConflict()) {
                  hasConflicts = true;
                  break;
               }
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
                        new PerformOperationsTask().execute(Pair.create(destinationAddr.toString(), operations));
                     }
                  }
               });
               processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
                  @Override
                  public void onButtonClick(DialogViewInterface dialog, int which) {
                     if (which == DialogViewInterface.BUTTON_FIRST) {
                        // user pressed "Cancel" button
                        dialog.dismiss();
                        importBtn.setEnabled(true);
                     }
                  }
               });
               processDialog.setOnDismissListener(new DialogViewInterface.OnDismissListener() {
                  @Override
                  public void onDismiss(DialogViewInterface dialog) {
                     processDialog.hide();
                  }
               });
            } else {
               processDialog.hide();
               if (operations.size() > 0) {
                  new PerformOperationsTask().execute(Pair.create(destinationAddr.toString(), operations));
               }
            }
      }
   }

   private class PerformOperationsTask extends ProgressTask<Pair<String, List<Operation>>, Void, Void> {

      @Override
      protected void onPreExecute() {
         super.onPreExecute();
         processDialog.setTitle(getResources().getString(R.string.importing));
      }

      @Override
      protected Void doInBackground(Pair<String, List<Operation>>... params) {
         String dst = params[0].first;
         List<Operation> objs = params[0].second;
         logger.debug("Performing Operations...");
         try {
            mediator.performOperations(dst, objs);
         }
         catch (Exception e) {
            logger.error("Send exception", e);
         }
         return null;
      }

      @Override
      protected void onPostExecute(Void aVoid) {
         super.onPostExecute(aVoid);
         processDialog.setFirstButtonText("Done");
         processDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
            @Override
            public void onButtonClick(DialogViewInterface dialog, int which) {
               if (which == DialogViewInterface.BUTTON_FIRST) {
                  // user pressed "Done" button
                  processDialog.hide();
                  importBtn.setEnabled(true);
                  sourcePick.clearSelection();
                  destinationPick.clearSelection();
                  manager.clear();
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
