/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.cnh.pf.jgroups.aidl.IDatasourceServiceAIDL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * This class manages (start/stop) USB datasource services.
 *
 * @author: junsu.shin@cnhind.com
 */
public class UsbServiceManager implements ServiceConnection {
   private static final Logger logger = LoggerFactory.getLogger(UsbServiceManager.class);
   private static final String AIDL_FAIL_MSG_PREFIX = "Failure at AIDL call: ";
   private static final String SERVICE_PFDATABASE = "com.cnh.pf.data.xstream.DatasourceService";
   private static final String SERVICE_ISOXML = "com.cnh.pf.isoxml.DatasourceService";
   private static final String SERVICE_SHAPEFILE = "com.cnh.pf.android.shapefile.datasource.DatasourceService";
   private final Context context;

   private Map<String, IDatasourceServiceAIDL> serviceMap = new HashMap<String, IDatasourceServiceAIDL>();

   @Inject
   public UsbServiceManager(Context context) {
      this.context = context;
   }

   /**
    * Bind (connect) to datasource services.
    */
   public void connect() {
      logger.debug("Connect to all datasource services.");
      context.bindService(new Intent(SERVICE_PFDATABASE), this, BIND_AUTO_CREATE);
      context.bindService(new Intent(SERVICE_ISOXML), this, BIND_AUTO_CREATE);
      context.bindService(new Intent(SERVICE_SHAPEFILE), this, BIND_AUTO_CREATE);
   }

   /**
    * Disconnect from datasource services.
    */
   public void disconnect() {
      logger.debug("Disconnect all datasource services.");
      context.unbindService(this);
   }

   @Override
   public void onServiceConnected(ComponentName className, IBinder service) {
      logger.trace("onServiceConnected() : {}", className);
      IDatasourceServiceAIDL aidl = IDatasourceServiceAIDL.Stub.asInterface(service);
      if (!serviceMap.containsKey(className.getClassName())) {
         serviceMap.put(className.getClassName(), aidl);
      }
   }

   @Override
   public void onServiceDisconnected(ComponentName className) {
      logger.trace("onServiceDisconnected(): {}", className);
      if (serviceMap.containsKey(className.getClassName())) {
         serviceMap.remove(className.getClassName());
      }
   }

   /**
    * Start USB datasource by calling start AIDL method. The service manager first finds
    * appropriate datasource from its datasource service pool given the session extra.
    * If found, the service manager will start the datasource.
    *
    * @param extra      Session Extra info that contains path & format
    * @param create    true if a folder needs to be created
    */
   public boolean startUsbDatasource(final SessionExtra extra, final boolean create) {
      boolean retValue = false;

      try {
         final IDatasourceServiceAIDL aidl = findDatasource(extra);
         if (aidl != null) {
            retValue = aidl.startDatasource(extra.getPath(), create);
         }
      }
      catch (RemoteException e) {
         logger.error(AIDL_FAIL_MSG_PREFIX, e);
      }
      return retValue;
   }

   /**
    * Returns true if a datasource interface is ready to handle the given session extra
    *
    * @param extra   the session extra (Device type, path, format)
    * @return True if a datasource interface is ready to handle the given session extra
    */
   public boolean datasourceReady(final SessionExtra extra) {
      return findDatasource(extra) != null;
   }

   /**
    * Find a datasource interface that can handle the given session extra.
    *
    * @param extra      the session extra (Device type, path, format)
    * @return  matching datasource interface
    */
   private IDatasourceServiceAIDL findDatasource(SessionExtra extra) {
      logger.trace("Find datasource: {}, {}", extra.getPath(), extra.getFormat());
      if (serviceMap.isEmpty()) {
         logger.debug("The service map is empty.");
         return null;
      }

      try {
         for (IDatasourceServiceAIDL aidl : serviceMap.values()) {
            if ((extra.getFormat() != null && aidl.acceptsFormat(extra.getFormat())) ||
                    (extra.getPath() != null && aidl.isInterested(extra.getPath()))) {
               return aidl;
            }
         }
      }
      catch (RemoteException e) {
         logger.error(AIDL_FAIL_MSG_PREFIX, e);
      }
      logger.trace("Could not find datasource.");
      return null;
   }

   /**
    * Stop all running datasource.
    */
   public void stopAllDatasourceBeforeStart() {
      logger.trace("stopAllDatasourceBeforeStart()");
      try {
         for (IDatasourceServiceAIDL aidl : serviceMap.values()) {
            aidl.stopDatasourceBeforeStart();
         }
      }
      catch (RemoteException e) {
         logger.error(AIDL_FAIL_MSG_PREFIX, e);
      }
   }

   /**
    * Stop all running datasource.
    */
   public void stopAllDatasources() {
      logger.trace("Stop all registered datasource service");
      try {
         for (IDatasourceServiceAIDL aidl : serviceMap.values()) {
            aidl.stopDatasource();
         }
      }
      catch (RemoteException e) {
         logger.error(AIDL_FAIL_MSG_PREFIX, e);
      }
   }
}
