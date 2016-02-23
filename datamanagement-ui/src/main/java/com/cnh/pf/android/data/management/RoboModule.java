/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.android.data.management.helper.ObservesTypeListener2;
import com.cnh.pf.data.management.MediumImpl;
import com.cnh.pf.data.management.aidl.MediumDevice;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.event.EventManager;
import roboguice.event.ObservesTypeListener;
import roboguice.event.eventListener.factory.EventListenerThreadingDecorator;

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

   @Singleton
   private static class DatasourceHelperProvider implements Provider<DatasourceHelper> {

      @Inject
      private Mediator mediator;

      @Override public DatasourceHelper get() {
         return new DatasourceHelper(mediator);
      }
   }
}
