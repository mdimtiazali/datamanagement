/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import android.app.Application;

import com.cnh.android.util.prefs.GlobalPreferences;
import com.cnh.android.util.prefs.GlobalPreferencesNotAvailableException;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.model.constants.Ports;
import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;

import org.jgroups.Global;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;

/**
 * Roboguice module definition
 * @author kedzie
 */
public class RoboModule extends AbstractModule {
   private static final Logger logger = LoggerFactory.getLogger(RoboModule.class);

   private Application application;

   public RoboModule(Application ctx) {
      this.application = ctx;
   }

   @Override
   protected void configure() {
      System.setProperty(Global.IPv4, "true");
      bind(DatasourceHelper.class).toProvider(DatasourceHelperProvider.class).in(Singleton.class);
   }

   @Provides
   @Singleton
   public Mediator getMediator(@Named("data") JChannel channel) {
      logger.debug("Using channel {}", channel.getProperties());
      return new Mediator(channel, "DataManagementService");
   }

   @Provides
   @Singleton
   @Named("daemon")
   public HostAndPort getDaemon() {
      try {
         GlobalPreferences gp = new GlobalPreferences(application);
         if(gp.hasPCM()) {
            return HostAndPort.fromParts(gp.getPCMIpAddress(), Ports.SIGNAL_DAEMON_PORT);
         }
         else {
            return HostAndPort.fromParts("localhost", Ports.SIGNAL_DAEMON_PORT);
         }
      }
      catch (GlobalPreferencesNotAvailableException e) {
         Throwables.propagate(e);
         return null;
      }
   }

   @Singleton
   private static class DatasourceHelperProvider implements Provider<DatasourceHelper> {

      @Inject
      private Mediator mediator;
      @Inject
      @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)
      private EventManager eventManager;

      @Override
      public DatasourceHelper get() {
         return new DatasourceHelper(mediator);
      }
   }
}
