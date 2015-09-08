/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.connection;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.ServiceConstants;
import com.cnh.pf.data.management.aidl.IDataManagementListenerAIDL;
import com.cnh.pf.data.management.aidl.IDataManagementServiceAIDL;
import com.cnh.pf.android.data.management.DataManagementActivity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.activity.event.OnPauseEvent;
import roboguice.event.EventManager;
import roboguice.event.Observes;

/**
 * Service Connection handles connection to Data Management Service, notifies listener when connection changes
 * @author oscar.salazar@cnhind.com
 */
@Singleton
public class DataServiceConnection implements DataServiceConnectionImpl {
   private static final Logger logger = LoggerFactory.getLogger(DataServiceConnection.class);

   private EventManager eventManager;

   private Context context;
   private IDataManagementServiceAIDL service = null;

   private static String appName = DataManagementActivity.class.getName();

   private ServiceConnection serviceConnection = new ServiceConnection() {
      @Override public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
         service = IDataManagementServiceAIDL.Stub.asInterface(iBinder);
         try {
            service.register(appName, dataManagementListenerAIDL);
            eventManager.fire(new ConnectionEvent(true));
         }
         catch (RemoteException e) {
            logger.error("error in register with service: ", e);
         }
      }

      @Override public void onServiceDisconnected(ComponentName componentName) {
         service = null;
         eventManager.fire(new ConnectionEvent(false));
      }

   };

   @Inject
   public DataServiceConnection(Application context, EventManager eventManager) {
      this.context = context;
      this.eventManager = eventManager;
      logger.debug("bindService");
      context.bindService(new Intent(ServiceConstants.dataServiceName), serviceConnection, Context.BIND_AUTO_CREATE);
   }

   private void disconnect(@Observes OnPauseEvent event) {
      logger.debug("disconnect");
      try {
         service.unregister(appName);
      }
      catch (RemoteException e) {
         logger.debug("error in disconnect:", e);
      }
      context.unbindService(serviceConnection);
   }

   public boolean isConnected() {
      return service != null;
   }

   @Override
   public DataManagementSession getSession() {
      try {
         return service.getSession();
      }
      catch (RemoteException e) {
         logger.error("Error Getting Session:", e);
      }
      return null;
   }

   @Override
   public void processOperation(DataManagementSession session, DataManagementSession.SessionOperation op) {
      logger.debug("processOperation: {}", op.toString());
      try {
         service.processOperation(session, op.getValue());
      }
      catch (RemoteException e) {
         logger.error("Error processing Operation:", e);
      }
   }

   private final IDataManagementListenerAIDL dataManagementListenerAIDL = new IDataManagementListenerAIDL.Stub() {
      @Override public void onProgressUpdated() throws RemoteException {
      }

      @Override public void onDataSessionUpdated(DataManagementSession session) throws RemoteException {
         logger.debug("onDataSessionUpdated");
         eventManager.fire(new DataSessionEvent(session));
      }
   };
}
