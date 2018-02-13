/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */

package com.cnh.pf.android.data.management.faults;

import com.cnh.pf.fault.FaultHandler;
import com.cnh.pf.model.constants.Ports;
import com.cnh.pf.signal.OnConnectionChangeListener;
import com.cnh.pf.signal.Producer;
import com.cnh.pf.signal.SignalUri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to send fault messages via signals to alert service
 *
 * @author RZimmermann
 */
public class DMFaultHandler {

   private static final Logger LOG = LoggerFactory.getLogger(DMFaultHandler.class);
   private static final String DEFAULT_IP_ADDRESS = "localhost";

   //FaultHandler for faults that go directly to the ui (alertservice)
   private FaultHandler faultHandler = new FaultHandler();

   private int port = Ports.SIGNAL_DAEMON_PORT;
   private String address = DEFAULT_IP_ADDRESS;

   private Producer signalProducer = null;

   /**
    * Contructor given context, will pull from global shared preferences for PCM IP
    * @param address - ip address
    */
   public DMFaultHandler(final String address) {
      if(address != null && !address.isEmpty()) {
         this.address = address;
      }

      init();
   }

   private void init() {
      signalProducer = new Producer(this.address, this.port);
      signalProducer.setOnConnectionChangeListener(new OnConnectionChangeListener() {
         @Override
         public void onConnectionChanged(boolean connected) {
            LOG.info("DMFaultHandler connected to signald");
         }
      });

      faultHandler.initialize(signalProducer, SignalUri.FAULT_DM);
   }

   /**
    * Start and initialize the fault handler
    */
   public void startHandler() {
      if (signalProducer != null) {
         signalProducer.start();
      }
      else {
         LOG.error("signal producer is null");
      }
   }

   /**
    * Stop the fault handler
    */
   public void stopHandler() {
      if (signalProducer != null) {
         signalProducer.close();
      }
   }

   /**
    * Sets a fault, if it is new, it will be sent out to alert service
    *
    * @param baseFaultCode This is the base fault code. For instance based codes, the instance gets added to the base code.
    */
   public void setFault(String baseFaultCode) {
      faultHandler.setFault(baseFaultCode);
   }

   /**
    * Clears a fault, if it was existing, all faults will be sent out to alert service
    *
    * @param baseFaultCode This is the base fault code. For instance based codes, the instance gets added to the base code.
    */
   public void clearFault(String baseFaultCode) {
      faultHandler.clearFault(baseFaultCode);
   }
}
