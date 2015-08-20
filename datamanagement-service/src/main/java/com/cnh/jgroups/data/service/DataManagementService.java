/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */
package com.cnh.jgroups.data.service;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.Operation;
import com.cnh.jgroups.data.service.aidl.IDataManagementListenerAIDL;
import com.cnh.jgroups.data.service.aidl.IDataManagementServiceAIDL;
import com.cnh.jgroups.data.service.helper.DatasourceHelper;
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

   DataManagementSession session = null;
   ConcurrentHashMap<String, IDataManagementListenerAIDL> listeners = new ConcurrentHashMap<String, IDataManagementListenerAIDL>();

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      logger.debug("onStartCommand");
      return START_NOT_STICKY;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      logger.debug("onUnbind");
      return super.onUnbind(intent);
   }

   @Override
   public void onCreate() {
      super.onCreate();
      try {
         mediator.setProgressListener(new Mediator.ProgressListener() {
            @Override
            public void onProgressPublished(String operation, int progress, int max) {
               //TODO send progress to all listeners
            }

            @Override
            public void onSecondaryProgressPublished(String secondaryOperation, int secondaryProgress, int secondaryMax) {

            }

            @Override
            public void onViewAccepted(View new_view) {
               try {
                  dsHelper.setSourceMap(new_view);
               }
               catch (Exception e) {
                  logger.error("Error in updating sourceMap", e);
               }
            }
         });
         new ConnectTask().execute();
      }
      catch (Exception e) {
         logger.error("error in onCreate", e);
      }
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      mediator.close();
   }

   @Override
   public IBinder onBind(Intent intent) {
      logger.debug("onBind");
      return new IServiceBinder();
   }

   private synchronized DataManagementSession getDataManagementSession() {
      return session;
   }

   public class IServiceBinder extends IDataManagementServiceAIDL.Stub {

      @Override
      public void register(String name, IDataManagementListenerAIDL listener) throws RemoteException {
         logger.debug("Register: " + name);
         listeners.put(name, listener);
         listener.onDataSessionUpdated(session);
      }

      @Override
      public void unregister(String name) throws RemoteException {
         logger.debug("unRegister:" + name);
         listeners.remove(name);
      }

      @Override
      public DataManagementSession getSession() throws RemoteException {
         return getDataManagementSession();
      }

      @Override
      public void processOperation(DataManagementSession session, int op) throws RemoteException {
         logger.debug("service.processOperation: " + DataManagementSession.SessionOperation.fromValue(op));
         DataManagementSession.SessionOperation sessionOperation = DataManagementSession.SessionOperation.fromValue(op);
         updateSession(session);
         if (sessionOperation.equals(DataManagementSession.SessionOperation.DISCOVERY)) {
            new DiscoveryTask().execute();
         }
         else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATTE_OPERATIONS)) {
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
   }

   private void updateSession(DataManagementSession session) {
      this.session = session;
   }

   private void performOperations() {
      try {
         new PerformOperationsTask() {
            @Override
            protected void onPostExecute(Void aVoid) {
               super.onPostExecute(aVoid);
               session = null;
               stopSelf();
            }
         }.execute();
      }
      catch (Exception e) {
         logger.error("Could not find destination address for this type", e);
      }
   }

   private void updateListeners() {
      logger.debug("updateListeners");
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

   private class ConnectTask extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground(Void... params) {
         try {
            mediator.start();
         }
         catch (Exception e) {
            logger.error("", e);
         }
         return null;
      }
   }

   private class DiscoveryTask extends SessionOperationTask<Void, Void, Void> {
      @Override
      protected Void doInBackground(Void... params) {
         try {
            session.setObjectData(mediator.discovery(dsHelper.getAddressForSourceType(session.getSourceType())));
            session.setSessionOperation(DataManagementSession.SessionOperation.CALCULATTE_OPERATIONS);
         }
         catch (Exception e) {
            logger.debug("error in discovery", e);
         }
         return null;
      }
   }

   private class CalculateTargetsTask extends SessionOperationTask<Void, Void, Void> {
      @Override
      protected Void doInBackground(Void... params) {
         logger.debug("Calculate Targets...");
         try {
            Address[] addresses = dsHelper.getAddressForSourceType(session.getDestinationType());
            List<Operation> operations = new ArrayList<Operation>();
            for (Address address : addresses) {
               logger.debug("Calculate Targets for Address:" +address);
               operations.addAll(mediator.calculateOperations(address, session.getObjectData()));
            }
            session.setData(operations);
            logger.debug("Got operation:" + session.getData());
            session.setSessionOperation(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS);
         }
         catch (Exception e) {
            logger.error("Send exception in CalculateTargets: ", e);
         }
         return null;
      }
   }

   private class CalculateConflictsTask extends SessionOperationTask<Void, Void, Void> {
      @Override
      protected Void doInBackground(Void... params) {
         logger.debug("Calculate Conflicts...");
         try {
            List<Operation> operations = new ArrayList<Operation>();
            for (Address address : dsHelper.getAddressForSourceType(session.getDestinationType())) {
               logger.debug("Calculate Conflicts for Address:" + address);
               operations.addAll(mediator.calculateConflicts(address, session.getData()));
            }
            session.setData(operations);
            session.setSessionOperation(DataManagementSession.SessionOperation.PERFORM_OPERATIONS);
         }
         catch (Exception e) {
            logger.error("Send exception", e);
         }
         return null;
      }
   }

   private class PerformOperationsTask extends SessionOperationTask<Void, Void, Void> {
      @Override
      protected Void doInBackground(Void... params) {
         logger.debug("Performing Operations...");
         try {
            for (Address address : dsHelper.getAddressForSourceType(session.getDestinationType())) {
               logger.debug("Perform Operations for Address:" + address);
               mediator.performOperations(address, session.getData());
            }
         }
         catch (Exception e) {
            logger.error("Send exception in PerformOperation:", e);
         }
         return null;
      }
   }

   private abstract class SessionOperationTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
      @Override
      protected void onPostExecute(Result result) {
         super.onPostExecute(result);
         updateListeners();
      }
   }
}

