/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Roboguice module definition
 * @author kedzie
 */
public class RoboModule extends AbstractModule {
   private static final Logger logger = LoggerFactory.getLogger(RoboModule.class);

   public static final String GLOBAL_PREFERENCES_PACKAGE = "com.cnh.pf.android.preference";

   private Application application;

   public RoboModule(Application ctx) {
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

   @Provides
   public File getUsbFile() {
      //Mock until USB support, uses internal sdcard
      return Environment.getExternalStorageDirectory();
   }
}
