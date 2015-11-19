/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.service;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.inject.name.Named;
import org.jgroups.Address;
import org.jgroups.View;
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
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;
import roboguice.service.RoboService;

/**
 * Service manages a DataManagementSession. Keeps one session alive until completion.
 * Notifies listeners of progress and provides state information to UI.
 * Accepts operations from UI, and relays them to destination datasource.
 * @author oscar.salazar@cnhind.com
 */
public class DataManagementService extends RoboService {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementService.class);

   private @Inject Mediator mediator;
   private @Inject DatasourceHelper dsHelper;
   private @Inject MediumImpl mediumProvider;
   @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)
   @Inject EventManager globalEventManager;

   DataManagementSession session = null;
   ConcurrentHashMap<String, IDataManagementListenerAIDL> listeners = new ConcurrentHashMap<String, IDataManagementListenerAIDL>();
   private Handler handler = new Handler();
   private final IBinder localBinder = new LocalBinder();

   /* Time to wait for USB Datasource to register if the usb has valid data*/
   private static int usbDelay = 6000;

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
      super.onCreate();
      try {
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
         if (session.getDestinationType().equals(Datasource.Source.INTERNAL) && session.getDevice().getType().equals(Datasource.Source.USB)) {
            logger.debug("Starting USB Datasource");
            waitForDatasource = usbDelay;
            Intent i = new Intent(ServiceConstants.USB_ACTION_INTENT);
            i.putExtra(ServiceConstants.USB_PATH, new String[] { session.getDevice().getPath().getPath() });
            i.putExtra(ServiceConstants.CREATE_PATH, false);
            i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            DataManagementService.this.getApplicationContext().sendBroadcast(i);
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

   public void resetSession() {
      logger.debug("resetSession");
      session = null;
   }

   public List<MediumDevice> getMediums() {
      return mediumProvider.getDevices();
   }

   private void performOperations() {
      try {
         int waitStart = 0;
         if (session.getSourceType().equals(Datasource.Source.INTERNAL) && session.getDevice().getType().equals(Datasource.Source.USB)) {
            String path = session.getDevice().getPath().getPath();
            startDisplayServices(path, true);
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
                     stopSelf();
                  }
               }.execute();
            }
         }, waitStart);
      }
      catch (Exception e) {
         logger.error("Could not find destination address for this type", e);
      }
   }

   private void startDisplayServices(String path, boolean create) {
      logger.debug("Starting USB datasource");
      Intent i = new Intent(ServiceConstants.USB_ACTION_INTENT);
      i.putExtra(ServiceConstants.USB_PATH, new String[] { path });
      i.putExtra(ServiceConstants.CREATE_PATH, create);
      i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
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
         final Double percent = ((progress * 1.0) / max) * 100;
         globalEventManager.fire(new DataServiceConnectionImpl.ProgressEvent(operation, progress, max));
      }

      @Override
      public void onSecondaryProgressPublished(String secondaryOperation, int secondaryProgress, int secondaryMax) { }

      @Override
      public void onViewAccepted(View newView) {
         logger.debug("onViewAccepted");
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

   private class DiscoveryTask extends SessionOperationTask<Void, Void, Integer> {
      @Override
      protected Integer doInBackground(Void... params) {
         try {
            logger.debug("Discovery for {}, address: {}", session.getSourceType(), dsHelper.getAddressForSourceType(session.getSourceType()));
            Address[] addrs = dsHelper.getAddressForSourceType(session.getSourceType());
            if (addrs == null || addrs.length > 0) {
               session.setObjectData(mediator.discovery(addrs));
            }
            session.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
         }
         catch (Exception e) {
            logger.debug("error in discovery", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.DISCOVERY_ERROR, ""));
         }
         return new Integer(0);
      }
   }

   private class CalculateTargetsTask extends SessionOperationTask<Void, Void, Integer> {
      @Override
      protected Integer doInBackground(Void... params) {
         logger.debug("Calculate Targets...");
         try {
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationType());
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateOperations(session.getObjectData(), addresses));
               logger.debug("Got operation: {}", session.getData());
            }
            session.setSessionOperation(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS);
         }
         catch (Exception e) {
            logger.error("Send exception in CalculateTargets: ", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.CALCULATE_TARGETS_ERROR, ""));
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
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationType());
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateConflicts(session.getData(), addresses));
            }
            session.setSessionOperation(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS);
         }
         catch (Exception e) {
            logger.error("Send exception", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.CALCULATE_CONFLICT_ERROR, ""));
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
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationType());
            if (addresses != null && addresses.length > 0) {
               mediator.performOperations(session.getData(), addresses);
               session.setSessionOperation(DataManagementSession.SessionOperation.PERFORM_OPERATIONS);
            }
            else {
               globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.NO_USB_DATASOURCE, "No USB Datasource"));
               return new Integer(1);
            }
         }
         catch (Exception e) {
            logger.error("Send exception in PerformOperation:", e);
            globalEventManager.fire(new DataServiceConnectionImpl.ErrorEvent(DataServiceConnectionImpl.ErrorEvent.DataError.NO_USB_DATASOURCE, "No USB Datasource"));
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
