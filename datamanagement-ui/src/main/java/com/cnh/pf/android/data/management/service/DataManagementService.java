/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import com.cnh.android.status.Status;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.DataManagementActivity;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.RoboModule;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.ConnectionEvent;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.DataSessionEvent;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.ErrorEvent;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.ProgressEvent;

import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.DataManagementSession.SessionOperation;
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
import roboguice.RoboGuiceHelper;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;
import roboguice.inject.InjectResource;
import roboguice.service.RoboService;

import static android.R.attr.y;
import static android.R.id.message;
import static com.cnh.pf.data.management.service.ServiceConstants.ACTION_STOP;
import static org.jgroups.conf.ProtocolConfiguration.log;

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

   private @Inject Mediator mediator;
   private @Inject DatasourceHelper dsHelper;
   @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)
   @Inject EventManager globalEventManager;
   @Named("global")
   @Inject private SharedPreferences prefs;

   private BitmapDrawable statusDrawable;
   @InjectResource(R.string.exporting_string) private String exporting;
   @InjectResource(R.string.importing_string) private String importing;
   @InjectResource(R.string.status_cancelled) private String statusCancelled;
   @InjectResource(R.string.status_cancelling) private String statusCancelling;

   @Inject
   NotificationManager notifyManager;

   /** Running session, only one active session supported */
   volatile DataManagementSession activeSession = null;
   Status status;
   boolean performCalled;

   private Map<DataManagementSession, Status> activeSessions
         = new HashMap<DataManagementSession, Status>();

   ConcurrentHashMap<String, IDataManagementListenerAIDL> listeners = new ConcurrentHashMap<String, IDataManagementListenerAIDL>();
   private Handler handler = new Handler();
   private final IBinder localBinder = new LocalBinder();

   /* Time to wait for USB Datasource to register if the usb has valid data*/
   private static int usbDelay = 4000;

   @Override public int onStartCommand(Intent intent, int flags, int startId) {
      logger.debug("onStartCommand {}", intent);

      if(ACTION_CANCEL.equals(intent.getAction())) {
         cancel(intent.<DataManagementSession>getParcelableExtra("session"));
      }
      return START_NOT_STICKY;
   }

   @Override public IBinder onBind(Intent intent) {
      logger.debug("onBind");
      //TODO distinguish between local and remote bind
      return localBinder;
   }

   @Override public boolean onUnbind(Intent intent) {
      logger.debug("onUnbind");
      return super.onUnbind(intent);
   }

   @Override
   public void onCreate() {
      final Application app = getApplication();
      //Phoenix Workaround (phoenix sometimes cannot read the manifest)
      RoboGuiceHelper.help(app, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" },
         new RoboModule(app), new ChannelModule(app));
      super.onCreate();
      statusDrawable = (BitmapDrawable)getResources().getDrawable(R.drawable.button_info);
      try {
         prefs.registerOnSharedPreferenceChangeListener(this);
         mediator.setProgressListener(pListener);
         if (!mediator.getChannel().isConnected())
            new ConnectTask().execute();
      }
      catch (Exception e) {
         logger.error("error in onCreate", e);
      }
   }

   @Override
   public void onDestroy() {
      prefs.unregisterOnSharedPreferenceChangeListener(this);
      super.onDestroy();
      new Thread(new Runnable() {
         @Override
         public void run() {
            mediator.close();
         }
      });
   }

   public DataManagementSession getSession() {
      return activeSession;
   }

   public void register(String name, IDataManagementListenerAIDL listener) {
      logger.debug("Register: " + name);
      listeners.put(name, listener);
   }

   public void processOperation(final DataManagementSession session, DataManagementSession.SessionOperation sessionOperation) {
      logger.debug("service.processOperation: {}", sessionOperation);
      if (sessionOperation.equals(DataManagementSession.SessionOperation.DISCOVERY)) {
         int waitForDatasource = 0;
         if (isUsbImport(session)) {
            logger.debug("Starting USB Datasource");
            waitForDatasource = usbDelay;
            startUsbServices(session.getDevice().getPath().getPath(), false, session.getFormat());
         }
         handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               logger.debug("Running discovery");
               new DiscoveryTask().execute(session);
            }
         }, waitForDatasource);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS)) {
         new CalculateTargetsTask().execute(session);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS)) {
         new CalculateConflictsTask().execute(session);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.PERFORM_OPERATIONS)) {
         performOperations(session);
      }
      else {
         logger.error("Couldn't find op");
      }
   }

   /**
    * Return whether an operation is currently executing.
    * @return true executing, false otherwise;
    */
   public boolean hasActiveSession() {
      if(!activeSessions.isEmpty()) {
         for(DataManagementSession session : activeSessions.keySet()) {
            if(session.getResult()!=null) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Cancel a session.
    * @param session the session to cancel.
    */
   public void cancel(DataManagementSession session) {
      new CancelTask().execute(session);
   }

   public List<MediumDevice> getMediums() {
      return dsHelper.getDevices();
   }

   public static boolean isUsbExport(DataManagementSession session) {
      return Arrays.binarySearch(session.getSourceTypes(), Datasource.Source.INTERNAL) > -1
            && session.getDevice().getType().equals(Datasource.Source.USB);
   }
   public static boolean isUsbImport(DataManagementSession session) {
      return Arrays.binarySearch(session.getDestinationTypes(), Datasource.Source.INTERNAL) > -1
            && session.getDevice().getType().equals(Datasource.Source.USB);
   }

   private void performOperations(final DataManagementSession session) {
      try {
         int waitStart = 0;
         session.setSessionOperation(DataManagementSession.SessionOperation.PERFORM_OPERATIONS);
         session.setResult(null);
         activeSession = session; //track running sessions
         performCalled = false;
         status = new com.cnh.android.status.Status("", statusDrawable, getApplicationContext().getPackageName());
         activeSessions.put(session, status);
         if (isUsbExport(session)) {
            startUsbServices(session.getDevice().getPath().getPath(), true, session.getFormat());
            waitStart = usbDelay;
         }
         handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               if(Process.Result.CANCEL.equals(activeSession.getResult())) {
                  log.debug("Operation cancelled before calling performOperations");
                  return;  //if cancel was pressed before datasource started
               }
               performCalled = true;
               new PerformOperationsTask().execute(session);
            }
         }, waitStart);
      }
      catch (Exception e) {
         logger.error("Could not find destination address for this type", e);
      }
   }

   private void startUsbServices(String path, boolean create, String format) {
      logger.debug("Starting USB datasource");
      Intent i = new Intent(ServiceConstants.USB_ACTION_INTENT);
      i.putExtra(ServiceConstants.USB_PATH, new String[] { path });
      i.putExtra(ServiceConstants.CREATE_PATH, create);
      if(format != null) {
         i.putExtra(ServiceConstants.FORMAT, format);
      }
      i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
      getApplicationContext().sendBroadcast(i);
   }

   public void stopUsbServices() {
      logger.debug("Stopping USB datasource");
      getApplicationContext().sendBroadcast(new Intent(ACTION_STOP));
   }

   @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
      public void onProgressPublished(String operation, int progress, int max) {
         logger.debug(String.format("publishProgress(%s, %d, %d)", operation, progress, max));
         globalEventManager.fire(new DataServiceConnectionImpl.ProgressEvent(operation, progress, max));
         if (activeSession!=null && activeSession.getSessionOperation().equals(DataManagementSession.SessionOperation.PERFORM_OPERATIONS)) {
            sendStatus(activeSession, String.format("%s %d/%d", operation, progress, max));

            if (SEND_NOTIFICATION) {
               notifyManager.notify(0, new NotificationCompat.Builder(DataManagementService.this)
                     .setContentTitle("Data Operation")
                     .setContentText(operation)
                     .setSmallIcon(android.R.drawable.ic_dialog_info)
                     .setProgress(max, progress, false)
                     .setContentIntent(PendingIntent.getActivity(DataManagementService.this, 0, new Intent(DataManagementService.this, DataManagementActivity.class), 0))
                     .addAction(R.drawable.button_stop, "Stop", PendingIntent.getService(DataManagementService.this, 0, new Intent(ACTION_CANCEL), 0))
                     .build());
            }
         }
      }

      @Override
      public void onSecondaryProgressPublished(String secondaryOperation, int secondaryProgress, int secondaryMax) { }

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
         }
         catch (Exception e) {
            logger.error("", e);
         }
         return null;
      }
   }

   static String addressToString(Address []addresses) {
      return Collections2.transform(Arrays.asList(addresses), new Function<Address, String>() {
         @Nullable @Override public String apply(@Nullable Address input) {
            return UUID.get(input);
         }
      }).toString();
   }

   private class DiscoveryTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
            DataManagementSession session = params[0];
         try {
            session.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
            session.setResult(null);
            Address[] addrs = (session.getDevice()!=null && session.getDevice().getAddress()!=null) ? Collections2.transform(session.getDevices(), new Function<MediumDevice, Address>() {
               @Nullable @Override public Address apply(@Nullable MediumDevice input) {
                  return input.getAddress();
               }
            }).toArray(new Address[0]) : dsHelper.getAddressForSourceType(session.getSourceTypes());
            logger.debug("Discovery for {}, address: {}", Arrays.toString(session.getSourceTypes()), addressToString(addrs));
            if (addrs == null || addrs.length > 0) {
               session.setObjectData(mediator.discovery(addrs));
               if(session.getObjectData()==null || session.getObjectData().isEmpty()) {
                  globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_DATA));
                  session.setResult(Result.NO_DATASOURCE);
               } else {
                  session.setResult(Process.Result.SUCCESS);
               }
            } else {
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_SOURCE_DATASOURCE));
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Exception e) {
            logger.debug("error in discovery", e);
            globalEventManager.fire(new ErrorEvent(session,
                  ErrorEvent.DataError.DISCOVERY_ERROR,
                  Throwables.getRootCause(e).getMessage()));
            session.setResult(Process.Result.ERROR);
         }
         return session;
      }
   }

   private class CalculateTargetsTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Calculate Targets...");
         DataManagementSession session = params[0];
         try {
            session.setSessionOperation(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS);
            session.setResult(null);
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationTypes());
            logger.debug("Calculate targets to address: {}", addressToString(addresses));
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateOperations(session.getObjectData(), addresses));
               logger.debug("Got operation: {}", session.getData());
               session.setResult( Process.Result.SUCCESS);
            } else {
               logger.warn("Skipping calculate targets");
               globalEventManager.fire(
                     new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
               session.setResult( Process.Result.NO_DATASOURCE);
            }
         }
         catch (Exception e) {
            logger.error("Send exception in CalculateTargets: ", e);
            globalEventManager.fire(new ErrorEvent(session,
                  ErrorEvent.DataError.CALCULATE_TARGETS_ERROR,
                  Throwables.getRootCause(e).getMessage()));
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
            session.setSessionOperation(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS);
            session.setResult(null);
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationTypes());
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateConflicts(session.getData(), addresses));
               session.setResult( Process.Result.SUCCESS);
            } else {
               session.setResult( Process.Result.NO_DATASOURCE);
            }
         }
         catch (Exception e) {
            logger.error("Send exception", e);
            globalEventManager.fire(new ErrorEvent(session,
                  ErrorEvent.DataError.CALCULATE_CONFLICT_ERROR,
                  Throwables.getRootCause(e).getMessage()));
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
            if(session.getData() == null) {
               session.setData(new ArrayList<Operation>());
               for(ObjectGraph obj : session.getObjectData()) {
                  session.getData().add(new Operation(obj, null));
               }
            }
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationTypes());
            if (addresses != null && addresses.length > 0) {
               session.setResults(mediator.performOperations(session.getData(), addresses));
               boolean hasIncomplete = false;
               for(Rsp<Process> ret : session.getResults()) {
                  if(ret.hasException()) throw ret.getException();
                  if(ret.wasReceived() && ret.getValue()!=null) {
                     hasIncomplete |= ret.getValue().getProgress().progress<ret.getValue().getProgress().max;
                  }
                  else {//suspect/unreachable
                     globalEventManager.fire(
                        new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
                     session.setResult(Process.Result.NO_DATASOURCE);
                  }
               }
               session.setResult(hasIncomplete ? Process.Result.CANCEL : Process.Result.SUCCESS);
            }
            else {
               globalEventManager.fire(
                  new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
               session.setResult( Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.error("Send exception in PerformOperation:", e);
            globalEventManager.fire(new ErrorEvent(session,
                  ErrorEvent.DataError.PERFORM_ERROR,
                  Throwables.getRootCause(e).toString()));
            session.setResult(Process.Result.ERROR);
         }
         return session;
      }

      @Override
      protected void onPostExecute(DataManagementSession session) {
         activeSessions.remove(session); //session no longer active.
         super.onPostExecute(session);
         logger.info("perform ops finished good");
         if (isUsbExport(session)) {
            stopUsbServices();
         }
         if(session.getResult().equals(Process.Result.ERROR)) {
            sendStatus(session, String.format("Error"));
         }
         else if(session.getResult().equals(Process.Result.CANCEL)) {
            sendStatus(session, "Cancelled");
         }
      }
   }

   private void sendStatus(DataManagementSession session, int res, Object...args) {
      if(args != null) {
         sendStatus(session, getResources().getString(res, args));
      }
      else {
         sendStatus(session, getResources().getString(res));
      }
   }

   private void sendStatus(DataManagementSession session, String message) {
      StringBuffer sb = new StringBuffer(isUsbImport(session) ? importing : exporting);
      sb.append(" ").append(message);
      status.setMessage(sb.toString());
      sendBroadcast(new Intent(Status.ACTION_STATUS_REMOVE).putExtra(Status.ID, ParcelUuid.fromString(status.getID().toString())));
      sendBroadcast(new Intent(Status.ACTION_STATUS_DISPLAY).putExtra(Status.NAME, status));
   }

   private class CancelTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Cancelling...");
         DataManagementSession session = params[0];
         try {
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationTypes());
            //if process already running tell it to cancel.
            if (addresses == null || addresses.length > 0 && performCalled) {
               mediator.cancel(addresses);
            }
            else {   //datasource hasn't started yet so finish before it began.
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
         if(Process.Result.CANCEL.equals(session.getResult())) {
            super.onPostExecute(session);
            sendStatus(session, R.string.status_cancelled);
         } else {
            sendStatus(session, R.string.status_cancelling);
         }
      }
   }

   private abstract class SessionOperationTask<Progress> extends AsyncTask<DataManagementSession, Progress, DataManagementSession> {
      @Override protected void onPostExecute(DataManagementSession session) {
         super.onPostExecute(session);
         globalEventManager.fire(new DataSessionEvent(session));
      }
   }
}
