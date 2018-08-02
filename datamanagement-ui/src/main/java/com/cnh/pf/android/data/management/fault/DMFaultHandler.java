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
import com.cnh.jgroups.Mediator;
import com.cnh.pf.fault.FaultHandler;
import com.cnh.pf.signal.OnConnectionChangeListener;
import com.cnh.pf.signal.Producer;
import com.cnh.pf.signal.SignalUri;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.context.event.OnCreateEvent;
import roboguice.context.event.OnDestroyEvent;
import roboguice.event.EventThread;
import roboguice.event.Observes;

import java.util.HashMap;
import java.util.Map;

/**
 * DMFaultHandler manages the signal producer to send fault alert/reset signals and
 * provides an fault object for the actual alert/reset behavior.
 *
 * @author: junsu.shin@cnhind.com
 */
@Singleton
public class DMFaultHandler implements OnConnectionChangeListener {
   private static final Logger logger = LoggerFactory.getLogger(DMFaultHandler.class);

   @Inject
   @Named("daemon")
   private HostAndPort daemonAddress;

   private Producer producer;
   private FaultHandler faultHandler;
   private Mediator mediator;
   private boolean recoverConnection = false;

   @Override
   public void onConnectionChanged(boolean connected) {
      logger.debug("onConnectionChanged: {}", connected);
      if (connected) {
         logger.debug("Initialize DM FaultHandler.");
         faultHandler = new FaultHandler();
         faultHandler.initialize(producer, SignalUri.FAULT_DM);

         // reset all fault codes
         for (FaultCode code : FaultCode.values()) {
            getFault(code).reset();
         }

         // only reconnect, if there was disconnect before and if this is not the first time connection event.
         if(recoverConnection) {
            try {
               mediator.reconnectChannel();
               recoverConnection = false;
            }
            catch (Exception e) {
               logger.error("Exception in recovering the connection", e);
            }
         }
      }
      else {
         recoverConnection = true;
      }
   }

   /**
    * Connect to signals
    * @param event   the event
    */
   @SuppressWarnings({"unused"})
   public void connectToDaemon(@Observes(EventThread.BACKGROUND) OnCreateEvent<Service> event) {
      logger.debug("Connecting to {}", daemonAddress);
      try {
         producer = new Producer(daemonAddress);
         producer.setOnConnectionChangeListener(this);
         producer.start();
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
         } else {
            logger.debug("Fault handler is not ready.");
         }
      }

      /**
       * Send reset signal to alertservice.
       */
      public void reset() {
         if (handler != null && handler.isInitialized()) {
            handler.clearFault(code.getCode());
         } else {
            logger.debug("Fault handler is not ready.");
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

   /**
    * Store mediator for error recovery.
    * @param mediator mediator
    */
   public void setMediator(Mediator mediator) {
      this.mediator = mediator;
   }

}
