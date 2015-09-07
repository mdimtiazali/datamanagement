/*
 *  Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.jgroups.data.service;

import android.app.Application;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.jgroups.Global;
import org.jgroups.JChannel;

import com.cnh.jgroups.Mediator;
import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Mediator implementation to all objects that depend on Mediator.
 * @author oscar.salazar@cnhind.com
 */
public class MediatorModule extends AbstractModule {
   private static Logger logger = LoggerFactory.getLogger(MediatorModule.class);

   private Application application;

   public MediatorModule(Application ctx) {
      this.application = ctx;
   }

   @Override
   protected void configure() {
      System.setProperty(Global.IPv4, "true");
   }

   @Provides
   @Singleton
   public Mediator getMediator(@Named("data") JChannel channel) {
      logger.debug("Using channel {}", channel.getProperties());
      return new Mediator(channel, "DataManagementService");
   }

}
