/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.service;

import static com.cnh.pf.data.management.service.ServiceConstants.ACTION_STOP;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.cnh.android.status.Status;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.DataManagementActivity;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.RoboModule;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.ErrorEvent;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.aidl.IDataManagementListenerAIDL;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.data.management.service.ServiceConstants;
import com.cnh.pf.datamng.Process;
import com.cnh.pf.datamng.Process.Result;
import com.cnh.pf.jgroups.ChannelModule;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.inject.name.Named;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.View;
import org.jgroups.util.Rsp;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.inject.Inject;

import roboguice.RoboGuiceHelper;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;
import roboguice.inject.InjectResource;
import roboguice.service.RoboService;

/**
 * Service manages a DataManagementSession. Keeps one session alive until completion.
 * Notifies listeners of progress and provides state information to UI.
 * Accepts operations from UI, and relays them to destination datasource.
 * @author oscar.salazar@cnhind.com
 */
public class DataManagementService extends RoboService implements SharedPreferences.OnSharedPreferenceChangeListener {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementService.class);

   public static final String ACTION_CANCEL = "com.cnh.pf.data.management.CANCEL";

   private static final boolean SEND_NOTIFICATION = false;
   public static final int KILL_STATUS_DELAY = 5000;

   private @Inject Mediator mediator;
   private @Inject DatasourceHelper dsHelper;
   @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)
   @Inject
   EventManager globalEventManager;
   @Named("global")
   @Inject
   private SharedPreferences prefs;

   @Inject
   private FormatManager formatManager;

   @InjectResource(R.string.exporting_string)
   private String exporting;
   @InjectResource(R.string.importing_string)
   private String importing;
   @InjectResource(R.string.status_successful)
   private String statusSuccessful;
   @InjectResource(R.string.status_cancelled)
   private String statusCancelled;
   @InjectResource(R.string.status_cancelling)
   private String statusCancelling;
   @InjectResource(R.string.status_starting)
   private String statusStarting;

   @Inject
   NotificationManager notifyManager;

   boolean performCalled;
   private Map<DataManagementSession, Status> activeSessions = new HashMap<DataManagementSession, Status>();

   ConcurrentHashMap<String, IDataManagementListenerAIDL> listeners = new ConcurrentHashMap<String, IDataManagementListenerAIDL>();
   private Handler handler = new Handler();
   private final IBinder localBinder = new LocalBinder();

   /* Time to wait for USB Datasource to register if the usb has valid data*/
   private static int usbDelay = 6000;

   private BitmapDrawable statusDrawable;
   private static Status dataStatus;

   private boolean isScanningUsb;

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      logger.debug("onStartCommand {}", intent);

      if (intent == null) { //happens after restart when service is sticky
         return START_STICKY;
      }

      if (ACTION_CANCEL.equals(intent.getAction())) {
         cancel(intent.<DataManagementSession> getParcelableExtra("session"));
      }
      else if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
         sendMediumUpdateEvent();
         Uri mount = intent.getData();
         File mountFile = new File(mount.getPath());
         logger.info("Media has been mounted @{}: ", mountFile.getAbsolutePath());
         scan(mountFile);
      }
      else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction()) || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(intent.getAction())
            || Intent.ACTION_MEDIA_REMOVED.equals(intent.getAction()) || Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
         logger.info("Media has been unmounted");
         sendMediumUpdateEvent();
         removeUsbStatus();
      }
      return START_STICKY;
   }

   @Override
   public IBinder onBind(Intent intent) {
      logger.debug("onBind");
      //TODO distinguish between local and remote bind
      return localBinder;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      logger.debug("onUnbind");
      return super.onUnbind(intent);
   }

   @Override
   public void onCreate() {
      final Application app = getApplication();
      //Phoenix Workaround (phoenix sometimes cannot read the manifest)
      RoboGuiceHelper.help(app, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" }, new RoboModule(app), new ChannelModule(app));
      super.onCreate();
      statusDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.ic_usb);
      try {
         prefs.registerOnSharedPreferenceChangeListener(this);
         mediator.setProgressListener(pListener);
      }
      catch (Exception e) {
         logger.error("error in onCreate", e);
      }
   }

   @Override
   public void onDestroy() {
      stopUsbServices();
      sendBroadcast(new Intent(ServiceConstants.ACTION_INTERNAL_DATA_STOP).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
      removeUsbStatus();
      listeners.clear();
      prefs.unregisterOnSharedPreferenceChangeListener(this);
      new Thread(new Runnable() {
         @Override
         public void run() {
            mediator.close();
         }
      });
      super.onDestroy();
   }

   public DataManagementSession getSession() {
      if (!activeSessions.isEmpty()) {
         return activeSessions.keySet().iterator().next();
      }
      return null;
   }

   public void register(String name, IDataManagementListenerAIDL listener) {
      logger.debug("Register: " + name);
      if (listeners.isEmpty()) {
         if (!mediator.getChannel().isConnected()) {
            logger.debug("first listener comes in, do ConnectTask");
            new ConnectTask().execute();
         }
      }
      listeners.put(name, listener);
   }

   public void unregister(String name) {
      logger.debug("Unegister: " + name);
      listeners.remove(name);
   }

   protected void sendMediumUpdateEvent() {
      try {
         List<MediumDevice> mediums = dsHelper.getTargetDevices();
         for (IDataManagementListenerAIDL listener : listeners.values()) {
            try {
               listener.onMediumsUpdated(mediums);
            }
            catch (RemoteException e) {
               logger.error("", e);
            }
         }
      }
      catch (Exception e) {
         logger.warn("Error sending medium update event, probably mediator isn't connected");
      }
   }

   /**
    * Scan USB stick for data.
    *
    * Starts all USB datasources and checks to see how many found data.
    * Passes all subfolders under the mountpoint.
    * @param mount
    */
   protected void scan(File mount) {
      logger.info("Checking for data");
      isScanningUsb = true;
      dataStatus = new Status(getResources().getString(R.string.usb_data_available), statusDrawable, getApplication().getPackageName());
      sendStatus(dataStatus, getResources().getString(R.string.usb_scanning));
      //Check if USB has any interesting data
      File[] folders = mount.listFiles(new FileFilter() {
         @Override
         public boolean accept(File file) {
            return file.isDirectory() && !file.getName().startsWith(".");
         }
      });
      String[] paths = new String[folders.length];
      for (int i = 0; i < folders.length; i++) {
         paths[i] = folders[i].getAbsolutePath();
      }
      //Launch USB datasource broadcast for every root folder on USB
      startUsbServices(paths, false, null);
      //wait for datasources, then see if any cared about the data.
      handler.postDelayed(new Runnable() {
         @Override
         public void run() {
            logger.info("Checking datasources on USB");
            Address[] addr = dsHelper.getAddressForSourceType(Datasource.Source.USB);
            if (addr != null && addr.length > 0) {
               logger.info("Found USB data! {} datasources to be exact.", addr.length);
               sendStatus(dataStatus, getResources().getString(R.string.usb_data_available));
            }
            else {
               sendStatus(dataStatus, getResources().getString(R.string.usb_no_data_available));
               handler.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                     removeStatus(dataStatus);
                  }
               }, usbDelay);
            }
            stopUsbServices();
            isScanningUsb = false;
         }
      }, usbDelay * 2);
   }

   protected void removeUsbStatus() {
      if (dataStatus != null) {
         sendBroadcast(new Intent(Status.ACTION_STATUS_REMOVE).putExtra(Status.ID, ParcelUuid.fromString(dataStatus.getID().toString())));
         dataStatus = null;
      }
   }

   public DataManagementSession processOperation(final DataManagementSession session, DataManagementSession.SessionOperation sessionOperation) {
      logger.debug("service.processOperation: {}", sessionOperation);
      session.setSessionOperation(sessionOperation);
      session.setResult(null);
      if (sessionOperation.equals(DataManagementSession.SessionOperation.DISCOVERY)) {
         if (isUsbImport(session)) {
            logger.debug("Starting USB Datasource");
            startUsbServices(new String[] { session.getSource().getPath().getPath() }, false, session.getFormat());
            handler.postDelayed(new Runnable() {
               @Override
               public void run() {
                  logger.debug("Running discovery");
                  new DiscoveryTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
               }
            }, usbDelay);
         }
         else {
            new DiscoveryTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
         }
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS)) {
         new CalculateTargetsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS)) {
         new CalculateConflictsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.PERFORM_OPERATIONS)) {
         performOperations(session);
      }
      else {
         logger.error("Couldn't find op");
      }
      return session;
   }

   /**
    * Return whether an operation is currently executing.
    * @return true executing, false otherwise;
    */
   public boolean hasActiveSession() {
      return !isScanningUsb && getActiveSession() != null;
   }

   public DataManagementSession getActiveSession() {
      for (Map.Entry<DataManagementSession, Status> entry : activeSessions.entrySet()) {
         if (entry.getKey().getResult() == null && entry.getKey().getSessionOperation().equals(DataManagementSession.SessionOperation.PERFORM_OPERATIONS)) {
            return entry.getKey();
         }
      }
      return null;
   }

   /**
    * Cancel a session.
    * @param session the session to cancel.
    */
   public void cancel(DataManagementSession session) {
      new CancelTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
   }

   public List<MediumDevice> getMediums() {
      return dsHelper.getTargetDevices();
   }

   public static boolean isUsbExport(DataManagementSession session) {
      return session.getSourceTypes() != null && Arrays.binarySearch(session.getSourceTypes(), Datasource.Source.INTERNAL) > -1 && session.getTarget() != null
            && session.getTarget().getType().equals(Datasource.Source.USB);
   }

   public static boolean isUsbImport(DataManagementSession session) {
      return session.getSource() != null && session.getSource().getType().equals(Datasource.Source.USB) && session.getDestinationTypes() != null
            && Arrays.binarySearch(session.getDestinationTypes(), Datasource.Source.INTERNAL) > -1;
   }

   /**
    * Return whether DISPLAY or INTERNAL data source is available
    * @return true if available, false otherwise;
    */
   public boolean hasLocalSources() {
      return dsHelper.getLocalDatasources(new Datasource.Source[] {Datasource.Source.DISPLAY, Datasource.Source.INTERNAL}).size() > 0;
   }


   private void performOperations(final DataManagementSession session) {
      try {
         session.setResult(null);
         performCalled = false;
         Status status = new com.cnh.android.status.Status("", statusDrawable, getApplicationContext().getPackageName());
         activeSessions.put(session, status);
         sendStatus(session, statusStarting);
         if (isUsbExport(session)) {
            File destinationFolder = new File(session.getTarget().getPath(), formatManager.getFormat(session.getFormat()).path);
            startUsbServices(new String[] { destinationFolder.getPath() }, true, session.getFormat());
            handler.postDelayed(new Runnable() {
               @Override
               public void run() {
                  if (!hasActiveSession()) {
                     logger.debug("Operation cancelled before calling performOperations");
                     return; //if cancel was pressed before datasource started
                  }
                  performCalled = true;
                  new PerformOperationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
               }
            }, usbDelay);
         }
         else {
            performCalled = true;
            new PerformOperationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
         }
      }
      catch (Exception e) {
         logger.error("Could not find destination address for this type", e);
      }
   }

   private void startUsbServices(String[] path, boolean create, String format) {
      logger.debug("Starting USB datasource");
      Intent i = new Intent(ServiceConstants.USB_ACTION_INTENT);
      i.putExtra(ServiceConstants.USB_PATH, path);
      i.putExtra(ServiceConstants.CREATE_PATH, create);
      if (format != null) {
         i.putExtra(ServiceConstants.FORMAT, format);
      }
      i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
      getApplicationContext().sendBroadcast(i);
   }

   public void stopUsbServices() {
      logger.debug("Stopping all USB datasources");
      getApplicationContext().sendBroadcast(new Intent(ACTION_STOP));
   }

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      logger.info("Shared prefs changed {}.  Stopping DatamanagementService", key);
      stopSelf();
   }

   public class LocalBinder extends Binder {

      public DataManagementService getService() {
         return DataManagementService.this;
      }
   }

   private Mediator.ProgressListener pListener = new Mediator.ProgressListener() {
      @Override
      public void onProgressPublished(final String operation, final int progress, final int max) {
         logger.debug(String.format("publishProgress(%s, %d, %d)", operation, progress, max));
         handler.post(new Runnable() {
            @Override
            public void run() {
               for (IDataManagementListenerAIDL listener : listeners.values()) {
                  try {
                     listener.onProgressUpdated(operation, progress, max);
                  }
                  catch (RemoteException e) {
                     logger.error("", e);
                  }
               }
            }
         });
         //dont update status
//         if (hasActiveSession() && performCalled) {
//            sendStatus(getActiveSession(), String.format("%s %d/%d", operation, progress, max));
//
//            if (SEND_NOTIFICATION) {
//               notifyManager.notify(0,
//                     new NotificationCompat.Builder(DataManagementService.this).setContentTitle("Data Operation").setContentText(operation)
//                           .setSmallIcon(android.R.drawable.ic_dialog_info).setProgress(max, progress, false)
//                           .setContentIntent(PendingIntent.getActivity(DataManagementService.this, 0, new Intent(DataManagementService.this, DataManagementActivity.class), 0))
//                           .addAction(R.drawable.button_stop, "Stop", PendingIntent.getService(DataManagementService.this, 0, new Intent(ACTION_CANCEL), 0)).build());
//            }
//         }
      }

      @Override
      public void onSecondaryProgressPublished(String secondaryOperation, int secondaryProgress, int secondaryMax) {
      }

      @Override
      public void onViewAccepted(View newView) {
         logger.debug("onViewAccepted {}", newView);
         try {
            dsHelper.setSourceMap(newView);
         }
         catch (Exception e) {
            logger.error("Error in updating sourceMap", e);
         }
      }
   };

   private class ConnectTask extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground(Void... params) {
         try {
            System.setProperty(Global.IPv4, "true");
            mediator.start();
            logger.debug("mediator started, notify the listeners");
            sendMediumUpdateEvent();
         }
         catch (Exception e) {
            logger.error("", e);
         }
         return null;
      }
   }

   static String addressToString(Address[] addresses) {
      return Collections2.transform(Arrays.asList(addresses), new Function<Address, String>() {
         @Nullable
         @Override
         public String apply(@Nullable Address input) {
            return UUID.get(input);
         }
      }).toString();
   }

   /**
    * Convert list of devices to array of addesses
    * @param devices
    * @return
    */
   private static Address[] getAddresses(List<MediumDevice> devices) {
      return Collections2.transform(devices, new Function<MediumDevice, Address>() {
         @Nullable
         @Override
         public Address apply(@Nullable MediumDevice input) {
            return input.getAddress();
         }
      }).toArray(new Address[devices.size()]);
   }

   private class DiscoveryTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         DataManagementSession session = params[0];
         try {
            if (session.getSources() == null && session.getSourceTypes() != null) { //export
               session.setSources(dsHelper.getLocalDatasources(session.getSourceTypes()));
            }
            else if (session.getSource().getAddress() == null) { //if MediumDevice hasn't been resolved
               session.setSources(dsHelper.getLocalDatasources(session.getSource().getType()));
            }
            Address[] addrs = getAddresses(session.getSources());
            logger.debug("Discovery for {}, address: {}", Arrays.toString(session.getSourceTypes()), addressToString(addrs));
            if (addrs == null || addrs.length > 0) {
               session.setObjectData(mediator.discovery(addrs));
               if (session.getObjectData() == null || session.getObjectData().isEmpty()) {
                  globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_DATA));
                  session.setResult(Result.NO_DATASOURCE);
               }
               else {
                  session.setResult(Process.Result.SUCCESS);
               }
            }
            else {
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_SOURCE_DATASOURCE));
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.debug("error in discovery", e);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.DISCOVERY_ERROR, Throwables.getRootCause(e).getMessage()));
            session.setResult(Process.Result.ERROR);
         }
         return session;
      }
   }

   private void resolveTargets(DataManagementSession session) {
      if (session.getTargets() == null && session.getDestinationTypes() != null) { //resolve by type
         session.setTargets(dsHelper.getLocalDatasources(session.getDestinationTypes()));
      }
      else if (session.getTarget().getAddress() == null) { //if MediumDevice hasn't been resolved
         session.setTargets(dsHelper.getLocalDatasources(session.getTarget().getType()));
      }
   }

   private class CalculateTargetsTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Calculate Targets...");
         DataManagementSession session = params[0];
         try {
            resolveTargets(session);
            Address[] addresses = getAddresses(session.getTargets());
            logger.debug("Calculate targets to address: {}", addressToString(addresses));
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateOperations(session.getObjectData(), addresses));
               logger.debug("Got operation: {}", session.getData());
               session.setResult(Process.Result.SUCCESS);
            }
            else {
               logger.warn("Skipping calculate targets");
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.error("Send exception in CalculateTargets: ", e);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.CALCULATE_TARGETS_ERROR, Throwables.getRootCause(e).getMessage()));
            session.setResult(Process.Result.ERROR);
         }
         return session;
      }
   }

   private class CalculateConflictsTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Calculate Conflicts...");
         DataManagementSession session = params[0];
         try {
            Address[] addresses = getAddresses(session.getTargets());
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateConflicts(session.getData(), addresses));
               session.setResult(Process.Result.SUCCESS);
            }
            else {
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.error("Send exception", e);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.CALCULATE_CONFLICT_ERROR, Throwables.getRootCause(e).getMessage()));
            session.setResult(Process.Result.ERROR);
         }
         return session;
      }
   }

   private class PerformOperationsTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Performing Operations...");
         DataManagementSession session = params[0];
         try {
            resolveTargets(session);
            if (session.getData() == null) {
               session.setData(new ArrayList<Operation>());
               for (ObjectGraph obj : session.getObjectData()) {
                  session.getData().add(new Operation(obj, null));
               }
            }
            Address[] addresses = getAddresses(session.getTargets());
            if (addresses != null && addresses.length > 0) {
               session.setResults(mediator.performOperations(session.getData(), addresses));
               boolean hasCancelled = Result.CANCEL.equals(session.getResult());
               for (Rsp<Process> ret : session.getResults()) {
                  if (ret.hasException()) throw ret.getException();
                  if (ret.wasReceived() && ret.getValue() != null && ret.getValue().getResult() != null) {
                     hasCancelled |= Result.CANCEL.equals(ret.getValue().getResult());
                  }
                  else {//suspect/unreachable
                     globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.PERFORM_ERROR));
                     session.setResult(Result.ERROR);
                  }
               }
               if (hasCancelled) {
                  session.setResult(Result.CANCEL);
               }
               else {
                  session.setResult(Result.SUCCESS);
               }
            }
            else {
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.error("Send exception in PerformOperation:", e);
            session.setResult(Process.Result.ERROR);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.PERFORM_ERROR, Throwables.getRootCause(e).toString()));

         }
         return session;
      }

      @Override
      protected void onPostExecute(DataManagementSession session) {
         completeOperation(session);
         super.onPostExecute(session);
      }
   }

   private void completeOperation(final DataManagementSession session) {
      logger.info("perform ops finished {}", session.getResult().name());
      if (session.getResult().equals(Process.Result.ERROR)) {
         sendStatus(session, "Error");
      }
      else if (session.getResult().equals(Process.Result.CANCEL)) {
         sendStatus(session, statusCancelled);
      }
      else if (session.getResult().equals(Result.SUCCESS)) {
         sendStatus(session, statusSuccessful);
      }
      if (isUsbExport(session)) {//stop USB datasources after export
         new StopTask().execute(getAddresses(session.getTargets()));
      }

      final Status status = activeSessions.remove(session);
      handler.postDelayed(new Runnable() {
         @Override
         public void run() {
            if (status != null) {
               removeStatus(status);
            }
         }
      }, KILL_STATUS_DELAY);
   }

   private void sendStatus(DataManagementSession session, int res, Object... args) {
      if (args != null) {
         sendStatus(session, getResources().getString(res, args));
      }
      else {
         sendStatus(session, getResources().getString(res));
      }
   }

   private void sendStatus(DataManagementSession session, String message) {
      sendStatus(session, activeSessions.get(session), message);
   }

   private void sendStatus(DataManagementSession session, Status status, String message) {
      if (status == null) return;
      sendStatus(status, new StringBuffer(isUsbImport(session) ? importing : exporting).append(" ").append(message).toString());
   }

   private void sendStatus(Status s, String msg) {
      if (s == null) return;
      s.setMessage(msg);
      removeStatus(s);
      sendBroadcast(new Intent(Status.ACTION_STATUS_DISPLAY).putExtra(Status.NAME, s));
   }

   private void removeStatus(Status s) {
      sendBroadcast(new Intent(Status.ACTION_STATUS_REMOVE).putExtra(Status.ID, ParcelUuid.fromString(s.getID().toString())));
   }

   private class CancelTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Cancelling...");
         DataManagementSession session = params[0];
         try {
            Address[] addresses = getAddresses(session.getTargets());
            //if process already running tell it to cancel.
            if (addresses.length > 0 && performCalled) {
               mediator.cancel(addresses);
            }
            else {
               session.setResult(Process.Result.CANCEL);
            }
         }
         catch (Exception e) {
            logger.error("Send exception", e);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.PERFORM_ERROR, Throwables.getRootCause(e).getMessage()));
         }
         return session;
      }

      @Override
      protected void onPostExecute(DataManagementSession session) {
         //only call super and fire event if we caught the datasource before it started working.
         //otherwise the performOperations call itself will return the canceled status.
         if (Process.Result.CANCEL.equals(session.getResult())) {
            completeOperation(session);
            super.onPostExecute(session);
         }
         else {
            sendStatus(session, statusCancelling);
         }
      }
   }

   private class StopTask extends AsyncTask<Address, Void, Void> {
      @Override
      protected Void doInBackground(Address... params) {
         logger.debug("Stopping...");
         try {
            mediator.stop(params);
         }
         catch (Exception e) {
            logger.error("Stop exception", e);
         }
         return null;
      }
   }

   private abstract class SessionOperationTask<Progress> extends AsyncTask<DataManagementSession, Progress, DataManagementSession> {
      @Override
      protected void onPostExecute(DataManagementSession session) {
         super.onPostExecute(session);
         if (session.getResult() != Result.ERROR) {
            for (IDataManagementListenerAIDL listener : listeners.values()) {
               try {
                  listener.onDataSessionUpdated(session);
               }
               catch (RemoteException e) {
                  logger.error("", e);
               }
            }
         }
      }
   }
}
