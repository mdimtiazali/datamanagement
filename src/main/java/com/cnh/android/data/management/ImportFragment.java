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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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

/**
 * Import Export Test Fragment, Fragment test copying objects from a source datasource to destination datasource. This fragment will be replaced when DataManagement, Import, and Export UI is finalized.
 * @author oscar.salazar@cnhind.com
 */
public class ImportFragment extends Fragment implements Mediator.ProgressListener {
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

   private Mediator mediator;

   static {
      System.setProperty(Global.CUSTOM_LOG_FACTORY, Slf4jLogImpl.class.getName());
   }

   public static final String GLOBAL_PREFERENCES_PACKAGE = "com.cnh.pf.android.preference";
   private JChannel channel;
   private RpcDispatcher disp;
   private List<Address> members = null;
   private Address destinationAddr = null;

   private Handler handler = new Handler();

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
      doneOperation();
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View layout = inflater.inflate(R.layout.test_fragment, container, false);
      sourcePick = (PickListEditable) layout.findViewById(R.id.source_pick);
      destinationPick = (PickListEditable) layout.findViewById(R.id.destination_pick);
      sourceDir = (TextView) layout.findViewById(R.id.source_dir);
      destDir = (TextView) layout.findViewById(R.id.dest_dir);
      treeView = (TreeViewList) layout.findViewById(R.id.tree_view);
      continueBtn = (Button) layout.findViewById(R.id.btn_continue);
      final TabActivity activity = (TabActivity) getActivity();
      return layout;
   }

   /**
    * Called by mediator when ever the target datasource publishes progress back...
    * Updates progressbar on ProgressDialog
    * @param operation  current Current operation being performed (i.e. calculateConflicts)
    * @param progress   current progress. 0 <= progress <= max
    * @param max        max progress
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

   /**
    * Called by mediator when this application has joined the jgroups cluster
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
               if (!members.get(i).equals(channel.getAddress())) {
                  sourcePick.addItem(new PickListItem(i, members.get(i).toString()));
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
                                 public void onPathSelected(String path) {
                                    ignoreNewViews = true;
                                    sourceDir.setText(path);
                                    Intent i = new Intent();
                                    i.setAction("com.cnh.pf.data.EXTERNAL_DATA");
                                    i.putExtra("paths", new String[] {path});
                                    i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                    i.putExtra("create", false);
                                    activity.sendBroadcast(i);

                                    handler.postDelayed(new Runnable() {
                                       @Override
                                       public void run() {
                                          List<Address> addrs = new ArrayList<Address>();
                                          for (Address addr : members) {
                                             try {
                                                if (!addr.equals(channel.getAddress())) {
                                                   logger.debug("Getting sources for: " + addr.toString());
                                                   Datasource.Source[] sources = mediator.getSources(addr);
                                                   if (Arrays.asList(sources).contains(Datasource.Source.USB)) {
                                                      logger.debug("addr: " + addr.toString() + " is valid datasource");
                                                      addrs.add(addr);
                                                   }
                                                   else {
                                                      logger.debug("addr: " + addr.toString() + " is not valid datasource");
                                                   }
                                                }
                                             }
                                             catch (Exception e) {
                                                e.printStackTrace();
                                             }
                                          }
                                          if (!addrs.isEmpty()) {
                                             new DiscoveryTask().execute(addrs);
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
                        logger.debug("Selected DataSource:" + members.get((int) id).toString());
                        ignoreNewViews = false;
                        sourceDir.setVisibility(View.GONE);
                        new DiscoveryTask().execute(new ArrayList<Address>() {{ add(members.get((int) id)); }});
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

   private void popDest() {
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
                        public void onPathSelected(String path) {
                           ignoreNewViews = true;
                           destDir.setText(path);
                           Intent i = new Intent();
                           i.setAction("com.cnh.pf.data.EXTERNAL_DATA");
                           i.putExtra("paths", new String[] { path });
                           i.putExtra("create", true);
                           i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                           activity.sendBroadcast(i);

                           handler.postDelayed(new Runnable() {
                              @Override
                              public void run() {
                                 List<Address> addrs = new ArrayList<Address>();
                                 for (Address addr : members) {
                                    try {
                                       if (!addr.equals(channel.getAddress())) {
                                          logger.debug("Getting sources for: " + addr.toString());
                                          Datasource.Source[] sources = mediator.getSources(addr);
                                          if (Arrays.asList(sources).contains(Datasource.Source.USB)) {
                                             logger.debug("addr: " + addr.toString() + " is valid datasource");
                                             addrs.add(addr);
                                          }
                                          else {
                                             logger.debug("addr: " + addr.toString() + " is not valid datasource");
                                          }
                                       }
                                    }
                                    catch (Exception e) {
                                       e.printStackTrace();
                                    }
                                 }
                                 if (!addrs.isEmpty()) {
                                    destinationAddr = addrs.get(0);
                                    logger.debug("Setting destination addr to:" +destinationAddr);
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
                  logger.debug("Selected Destination DataSource:" + members.get((int) id).toString());
                  destinationAddr = members.get((int) id);
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

   private void addChildrenToTree(ObjectGraph parent, int parentLevel) {
      if (!parent.getChildren().isEmpty()) {
         for (ObjectGraph child : parent.getChildren()) {
            treeBuilder.sequentiallyAddNextNode(child, parentLevel+1);
            addChildrenToTree(child, parentLevel+1);
         }
      }
   }

   private void onNewObjectGraphReceived(final List<ObjectGraph> result) {
      if (!result.isEmpty()) {
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
       Map<ObjectGraph, Operation> operationMap = new HashMap<ObjectGraph,Operation>();
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

   private class ConnectTask extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground(Void... params) {
         try {
            SharedPreferences prefs = getActivity().createPackageContext(GLOBAL_PREFERENCES_PACKAGE, Context.CONTEXT_IGNORE_SECURITY)
                  .getSharedPreferences(GLOBAL_PREFERENCES_PACKAGE, Context.MODE_WORLD_READABLE);
            String config = prefs.getString("jgroups_config", "jgroups.xml");
            boolean gossip = config.equals("jgroups-tunnel.xml");
            boolean tcp = config.equals("jgroups-tcp.xml");
            if(gossip || tcp) {
               System.setProperty("jgroups.tunnel.gossip_router_hosts", String.format("%s[%s]",
                     prefs.getString("jgroups_gossip_host", "10.0.2.2"),
                     prefs.getString("jgroups_gossip_port", "12001")));
            }
            if(tcp) {
               String tcpExternalPort = prefs.getString("jgroups_external_port", "10000");
               System.setProperty("jgroups.tcp.bind_port", tcpExternalPort);
               System.setProperty(Global.EXTERNAL_ADDR, prefs.getString("jgroups_external_host", "10.0.0.11"));
               System.setProperty(Global.EXTERNAL_PORT, tcpExternalPort);
            }
            logger.info("Using JGroups config {}", config);
            logger.info("Connecting");
            System.setProperty(Global.IPv4, "true");
            channel = new JChannel(config);

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

   private class DiscoveryTask extends AsyncTask<List<Address>, Void, List<ObjectGraph>> {

      @Override
      protected List<ObjectGraph> doInBackground(List<Address>... params) {
         logger.debug("Discovery for " + params[0].toString() + " source...");
         try {
            return mediator.discovery(params[0].toArray(new Address[params[0].size()]));
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
            } else {
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
