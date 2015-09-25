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

import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.DataManagementActivity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.activity.event.OnPauseEvent;
import roboguice.activity.event.OnResumeEvent;
import roboguice.config.DefaultRoboModule;
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
   private DataManagementService service = null;

   private static String appName = DataManagementActivity.class.getName();

   private ServiceConnection serviceConnection = new ServiceConnection() {
      @Override public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
         DataManagementService.LocalBinder binder = (DataManagementService.LocalBinder) iBinder;
         service = (DataManagementService) binder.getService();
         eventManager.fire(new ConnectionEvent(true, service));
      }

      @Override public void onServiceDisconnected(ComponentName componentName) {
         service = null;
         eventManager.fire(new ConnectionEvent(false, null));
      }

   };

   @Inject
   public DataServiceConnection(Application context, @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME) EventManager eventManager) {
      this.context = context;
      this.eventManager = eventManager;
   }

   private void connect(@Observes OnResumeEvent event) {
      logger.debug("bindService");
      context.bindService(new Intent(context, DataManagementService.class), serviceConnection, Context.BIND_AUTO_CREATE);
   }

   private void disconnect(@Observes OnPauseEvent event) {
      logger.debug("disconnect");
      new Thread(new Runnable() {
         @Override public void run() {
            context.unbindService(serviceConnection);
         }
      });
   }

   public boolean isConnected() {
      return service != null;
   }

   public DataManagementService getService() {
      return service;
   }
}
