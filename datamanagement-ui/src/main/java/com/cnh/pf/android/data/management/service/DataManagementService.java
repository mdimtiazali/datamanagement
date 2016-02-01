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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.RoboModule;
import com.cnh.pf.datamng.Process;
import com.cnh.pf.jgroups.ChannelModule;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.inject.name.Named;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.MediumImpl;
import com.cnh.pf.data.management.service.ServiceConstants;
import com.cnh.pf.data.management.aidl.IDataManagementListenerAIDL;
import com.cnh.pf.data.management.aidl.MediumDevice;

import org.jgroups.Global;
import roboguice.RoboGuiceHelper;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;
import roboguice.service.RoboService;

/**
 * Service manages a DataManagementSession. Keeps one session alive until completion.
 * Notifies listeners of progress and provides state information to UI.
 * Accepts operations from UI, and relays them to destination datasource.
 * @author oscar.salazar@cnhind.com
 */
public class DataManagementService extends RoboService implements SharedPreferences.OnSharedPreferenceChangeListener {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementService.class);

   private static final int RETURN_SUCCESS = 0;
   private static final int RETURN_ERROR = 1;
   private static final int RETURN_NO_DATASOURCE = 2;
   private static final int RETURN_CANCEL = 3;

   private @Inject Mediator mediator;
   private @Inject DatasourceHelper dsHelper;
   @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)
   @Inject EventManager globalEventManager;
   @Named("global")
   @Inject private SharedPreferences prefs;

   DataManagementSession session = null;
   ConcurrentHashMap<String, IDataManagementListenerAIDL> listeners = new ConcurrentHashMap<String, IDataManagementListenerAIDL>();
   private Handler handler = new Handler();
   private final IBinder localBinder = new LocalBinder();

   /* Time to wait for USB Datasource to register if the usb has valid data*/
   private static int usbDelay = 7000;

   @Override public int onStartCommand(Intent intent, int flags, int startId) {
      logger.debug("onStartCommand");
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

   public void register(String name, IDataManagementListenerAIDL listener) {
      logger.debug("Register: " + name);
      listeners.put(name, listener);
   }

   public void processOperation(DataManagementSession session, DataManagementSession.SessionOperation sessionOperation) {
      logger.debug("service.processOperation: {}", sessionOperation);
      this.session = session;
      //TODO add string constant for action
      if (sessionOperation.equals(DataManagementSession.SessionOperation.DISCOVERY)) {
         int waitForDatasource = 0;
         if (Arrays.binarySearch(session.getDestinationTypes(), Datasource.Source.INTERNAL) > -1 && session.getDevice().getType().equals(Datasource.Source.USB)) {
            logger.debug("Starting USB Datasource");
            waitForDatasource = usbDelay;
            startDisplayServices(session.getDevice().getPath().getPath(), false, session.getFormat());
         }
         handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               logger.debug("Running discovery");
               new DiscoveryTask().execute();
            }
         }, waitForDatasource);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS)) {
         new CalculateTargetsTask().execute();
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS)) {
         new CalculateConflictsTask().execute();
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.PERFORM_OPERATIONS)) {
         performOperations();
      }
      else {
         logger.error("Couldn't find op");
      }
   }

   public void cancel() {
      new CancelTask().execute();
   }

   public void resetSession() {
      logger.debug("resetSession");
      session = null;
   }

   public List<MediumDevice> getMediums() {
      return dsHelper.getDevices();
   }

   private void performOperations() {
      try {
         int waitStart = 0;
         if (Arrays.binarySearch(session.getSourceTypes(), Datasource.Source.INTERNAL) > -1 && session.getDevice().getType().equals(Datasource.Source.USB)) {
            String path = session.getDevice().getPath().getPath();
            startDisplayServices(path, true, session.getFormat());
            waitStart = usbDelay;
         }
         handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               new PerformOperationsTask() {
                  @Override
                  protected void onPostExecute(Integer result) {
                     super.onPostExecute(result);
                     session = null;
                     if(result!=RETURN_CANCEL) {
                        logger.info("perform ops finished good");
                        stopDisplayServices();
                        stopSelf();
                     }
                     else {
                        logger.warn("perform ops finished not so good");
                     }
                  }
               }.execute();
            }
         }, waitStart);
      }
      catch (Exception e) {
         logger.error("Could not find destination address for this type", e);
      }
   }

   private void startDisplayServices(String path, boolean create, String format) {
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

   private void stopDisplayServices() {
      logger.debug("Stopping USB datasource");
      Intent i = new Intent(ServiceConstants.ACTION_STOP);
      getApplicationContext().sendBroadcast(i);
   }

   private void updateListeners() {
      logger.debug("updateListeners, session: {}", session.toString());
      Iterator<Entry<String, IDataManagementListenerAIDL>> entries = listeners.entrySet().iterator();
      while (entries.hasNext()) {
         Entry<String, IDataManagementListenerAIDL> entry = entries.next();
         try {
            entry.getValue().onDataSessionUpdated(session);
         }
         catch (RemoteException e) {
            logger.debug("removing client:" + entry.getValue());
         }
      }
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

   public synchronized  DataManagementSession getSession() {
      return session;
   }

   private Mediator.ProgressListener pListener = new Mediator.ProgressListener() {
      @Override
      public void onProgressPublished(String operation, int progress, int max) {
         logger.debug(String.format("publishProgress(%s, %d, %d)", operation, progress, max));
         globalEventManager.fire(new DataServiceConnectionImpl.ProgressEvent(operation, progress, max));
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

   private class DiscoveryTask extends SessionOperationTask<Void, Void, Integer> {
      @Override
      protected Integer doInBackground(Void... params) {
         try {

            Address[] addrs = (session.getDevice()!=null && session.getDevice().getAddress()!=null) ? Collections2.transform(session.getDevices(), new Function<MediumDevice, Address>() {
               @Nullable @Override public Address apply(@Nullable MediumDevice input) {
                  return input.getAddress();
               }
            }).toArray(new Address[0]) : dsHelper.getAddressForSourceType(session.getSourceTypes());
            logger.debug("Discovery for {}, address: {}", Arrays.toString(session.getSourceTypes()), addressToString(addrs));
            if (addrs == null || addrs.length > 0) {
               session.setObjectData(mediator.discovery(addrs));
            }
            session.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
         }
         catch (Exception e) {
            logger.debug("error in discovery", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.DISCOVERY_ERROR,
               Throwables.getRootCause(e).getMessage()));
         }
         return new Integer(0);
      }
   }

   private class CalculateTargetsTask extends SessionOperationTask<Void, Void, Integer> {
      @Override
      protected Integer doInBackground(Void... params) {
         logger.debug("Calculate Targets...");
         try {
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationTypes());
            logger.debug("Calculate targets to address: {}", addressToString(addresses));
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateOperations(session.getObjectData(), addresses));
               logger.debug("Got operation: {}", session.getData());
            } else {
               logger.warn("Skipping calculate targets");
            }
            session.setSessionOperation(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS);
         }
         catch (Exception e) {
            logger.error("Send exception in CalculateTargets: ", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.CALCULATE_TARGETS_ERROR,
               Throwables.getRootCause(e).getMessage()));
            return new Integer(1);
         }
         return new Integer(0);
      }
   }

   private class CalculateConflictsTask extends SessionOperationTask<Void, Void, Integer> {
      @Override
      protected Integer doInBackground(Void... params) {
         logger.debug("Calculate Conflicts...");
         try {
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationTypes());
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateConflicts(session.getData(), addresses));
            }
            session.setSessionOperation(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS);
         }
         catch (Exception e) {
            logger.error("Send exception", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.CALCULATE_CONFLICT_ERROR,
               Throwables.getRootCause(e).getMessage()));
            return new Integer(1);
         }
         return new Integer(0);
      }
   }

   private class PerformOperationsTask extends SessionOperationTask<Void, Void, Integer> {
      @Override
      protected Integer doInBackground(Void... params) {
         logger.debug("Performing Operations...");
         try {
            if(session.getData() == null) {
               session.setData(new ArrayList<Operation>());
               for(ObjectGraph obj : session.getObjectData()) {
                  session.getData().add(new Operation(obj, null));
               }
            }
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationTypes());
            if (addresses != null && addresses.length > 0) {
               RspList<com.cnh.pf.datamng.Process> retval = mediator.performOperations(session.getData(), addresses);
               boolean hasIncomplete = false;
               for(Rsp<Process> ret : retval) {
                  if(ret.hasException()) throw ret.getException();
                  if(ret.wasReceived() && ret.getValue()!=null) {
                     hasIncomplete |= ret.getValue().getProgress().progress<ret.getValue().getProgress().max;
                  }
                  else {//suspect/unreachable
                     globalEventManager.fire(
                        new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.NO_USB_DATASOURCE, "No USB Datasource"));
                     return RETURN_NO_DATASOURCE;
                  }
               }
               session.setSessionOperation(DataManagementSession.SessionOperation.PERFORM_OPERATIONS);
               return hasIncomplete ? RETURN_CANCEL : RETURN_SUCCESS;
            }
            else {
               globalEventManager.fire(
                  new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.NO_USB_DATASOURCE, "No USB Datasource"));
               return RETURN_NO_DATASOURCE;
            }
         }
         catch (Throwable e) {
            logger.error("Send exception in PerformOperation:", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.PERFORM_ERROR,
               Throwables.getRootCause(e).getMessage()));
            return RETURN_ERROR;
         }
      }
   }

   private class CancelTask extends SessionOperationTask<Void, Void, Integer> {
      @Override
      protected Integer doInBackground(Void... params) {
         logger.debug("Cancelling...");
         try {
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationTypes());
            if (addresses == null || addresses.length > 0) {
               mediator.cancel(addresses);
            }
         }
         catch (Exception e) {
            logger.error("Send exception", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.PERFORM_ERROR,
               Throwables.getRootCause(e).getMessage()));
            return new Integer(1);
         }
         return new Integer(0);
      }
   }

   private abstract class SessionOperationTask<Params, Progress, Integer> extends AsyncTask<Params, Progress, Integer> {
      @Override protected void onPostExecute(Integer success) {
         super.onPostExecute(success);
         if (success.equals(0))
            globalEventManager.fire(new DataServiceConnectionImpl.DataSessionEvent(session));
      }
   }
}
