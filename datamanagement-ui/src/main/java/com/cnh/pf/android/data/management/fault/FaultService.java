/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.fault;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.cnh.pf.fault.FaultHandler;
import com.cnh.pf.signal.OnConnectionChangeListener;
import com.cnh.pf.signal.Producer;
import com.cnh.pf.signal.SignalUri;
import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.context.event.OnCreateEvent;
import roboguice.context.event.OnDestroyEvent;
import roboguice.event.EventThread;
import roboguice.event.Observes;
import roboguice.service.RoboService;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

/**
 * FaultService manages the signal producer to send fault alert/reset signals and
 * provides an fault object for the actual alert/reset behavior.
 *
 * @author: junsu.shin@cnhind.com
 */
public class FaultService extends RoboService {
   private static final Logger logger = LoggerFactory.getLogger(FaultService.class);
   private LocalBinder localBinder = new LocalBinder();

   @Inject
   @Named("daemon")
   private HostAndPort daemonAddress;

   private Producer producer;
   private FaultHandler faultHandler;
   private boolean isConnected;

   /**
    * Connect to signals
    * @param event   the event
    */
   @SuppressWarnings({"unused"})
   public void connectToDaemon(@Observes(EventThread.BACKGROUND) OnCreateEvent<Service> event) {
      logger.debug("Connecting to {}", daemonAddress);
      try {
         producer = new Producer(daemonAddress);
         producer.setOnConnectionChangeListener(new OnConnectionChangeListener() {
            @Override
            public void onConnectionChanged(boolean connected) {
               logger.debug("onConnectionChanged: {}", connected);
               isConnected = connected;
            }
         });
         producer.start();
         faultHandler = new FaultHandler();
         faultHandler.initialize(producer, SignalUri.FAULT_DM);
      }
      catch (Exception e) {
         logger.error("Exception in connecting to Signal", e);
      }
   }

   /**
    * Disconnect from signals
    * @param event   the event
    */
   @SuppressWarnings({"unused"})
   public void disconnect(@Observes(EventThread.BACKGROUND) OnDestroyEvent<Service> event) {
      logger.debug("Disconnect");
      producer.close();
   }

   // Store fault object for later use rather than creating it every time it's needed.
   private Map<FaultCode, Fault> faultMap = new HashMap<FaultCode, Fault>();

   /**
    * An inner class to provide interface to set/clear fault.
    */
   public static class Fault {
      private FaultHandler handler;
      private FaultCode code;

      private Fault(FaultHandler handler, FaultCode code) {
         this.handler = handler;
         this.code = code;
      }

      /**
       * Static factory method to create Fault object.
       * @param handler  fault handler that manages fault's active status and send
       *                 the signal
       * @param code    fault code
       * @return new Fault object
       */
      public static Fault newFault(FaultHandler handler, FaultCode code) {
         return new Fault(handler, code);
      }

      /**
       * Send alert signal to alertservice.
       */
      public void alert() {
         if (handler != null && handler.isInitialized()) {
            handler.setFault(code.getCode());
         }
      }

      /**
       * Send reset signal to alertservice.
       */
      public void reset() {
         if (handler != null && handler.isInitialized()) {
            handler.clearFault(code.getCode());
         }
      }
   }

   /**
    * Return a fault object that corresponds to given fault code.
    * @param code fault code
    * @return fault object that provides an interface to set/clear fault.
    */
   public Fault getFault(FaultCode code) {
      Fault fault = faultMap.get(code);
      if (fault == null) {
         fault = Fault.newFault(faultHandler, code);
         faultMap.put(code, fault);
      }

      return fault;
   }

   @Override
   public IBinder onBind(Intent intent) {
      logger.debug("onBind");
      return localBinder;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      logger.debug("onUnbind");
      return super.onUnbind(intent);
   }

   /**
    * Local binder class to provide fault service object.
    */
   public class LocalBinder extends Binder {
      public FaultService getService() {
         return FaultService.this;
      }
   }
}
