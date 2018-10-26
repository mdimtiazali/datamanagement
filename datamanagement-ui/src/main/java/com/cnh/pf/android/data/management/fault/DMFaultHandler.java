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

import com.cnh.alert.AlertProducer;
import com.cnh.alert.IAlertManager;
import com.cnh.alert.types.Status;
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

import java.util.HashMap;
import java.util.Map;

import roboguice.context.event.OnCreateEvent;
import roboguice.context.event.OnDestroyEvent;
import roboguice.event.EventThread;
import roboguice.event.Observes;

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
   private IAlertManager alertManager;
   private Status importExportStatus;
   private Status usbMountStatus = new Status(Status.StatusType.DM_USB_DETECTED);
   private FaultHandler faultHandler;
   private Mediator mediator;
   private boolean recoverConnection = false;
   // Store fault object for later use rather than creating it every time it's needed.
   private Map<FaultCode, Fault> faultMap = new HashMap<FaultCode, Fault>();

   /**
    * Getter for recoverConnection.
    */

   public boolean isRecoverConnection() {
      return recoverConnection;
   }

   /**
    * Setter for recoverConnection.
    */
   public void setRecoverConnection(boolean recoverConnection) {
      this.recoverConnection = recoverConnection;
   }

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
         if (recoverConnection) {
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
    * Sets a status icon in the status bar if a USB stick was mounted or removes the icon if the USB stick was unmounted
    * @param mounted True, if USB stick is mounted, otherwise false.
    */
   public void changeUSBDetectedStatus(boolean mounted) {
      if (producer != null && alertManager != null) {
         if (mounted) {
            alertManager.setStatus(usbMountStatus);
         }
         else {
            alertManager.cancelStatus(usbMountStatus);
         }
      }
      else {
         logger.debug("Producer or AlertManager is not initialized");
      }
   }

   /**
    * Sets a status icon in the status bar that shows the current status of the import or export
    * @param statusType Current status of the import or export (in progress / complete / failed)
    */
   public void showImportExportStatus(Status.StatusType statusType) {
      if (producer != null && alertManager != null && statusType != null) {

         if (statusType == Status.StatusType.DM_SESSION_EXPORT_IN_PROGRESS || statusType == Status.StatusType.DM_SESSION_IMPORT_IN_PROGRESS) {
            importExportStatus = new Status(statusType);
         }
         else {
            importExportStatus.setType(statusType);
         }

         alertManager.setStatus(importExportStatus);
      }
      else {
         logger.debug("Producer or AlertManager is not initialized");
      }
   }

   /**
    * Connect to signals
    * @param event   the event
    */
   @SuppressWarnings({ "unused" })
   public void connectToDaemon(@Observes(EventThread.BACKGROUND) OnCreateEvent<Service> event) {
      logger.debug("Connecting to {}", daemonAddress);
      try {
         producer = new Producer(daemonAddress);
         producer.setOnConnectionChangeListener(this);
         producer.start();
         alertManager = new AlertProducer(producer);
      }
      catch (Exception e) {
         logger.error("Exception in connecting to Signal", e);
      }
   }

   /**
    * Disconnect from signals
    * @param event   the event
    */
   @SuppressWarnings({ "unused" })
   public void disconnect(@Observes(EventThread.BACKGROUND) OnDestroyEvent<Service> event) {
      logger.debug("Disconnect");
      producer.close();
   }

   /**
    * Getter & Setter for faultMap.
    */
   public Map<FaultCode, Fault> getFaultMap() {
      return faultMap;
   }

   public void setFaultMap(Map<FaultCode, Fault> faultMap) {
      this.faultMap = faultMap;
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
    * Retrieve mediator for error recovery.
    */
   public Mediator getMediator() {
      return mediator;
   }

   /**
    * Store mediator for error recovery.
    * @param mediator mediator
    */
   public void setMediator(Mediator mediator) {
      this.mediator = mediator;
   }

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
         else {
            logger.debug("Fault handler is not ready.");
         }
      }

      /**
       * Send reset signal to alertservice.
       */
      public void reset() {
         if (handler != null && handler.isInitialized()) {
            handler.clearFault(code.getCode());
         }
         else {
            logger.debug("Fault handler is not ready.");
         }
      }
   }

}
