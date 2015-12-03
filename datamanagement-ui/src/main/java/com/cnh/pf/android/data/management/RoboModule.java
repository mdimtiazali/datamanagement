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
import android.os.Environment;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.data.management.MediumImpl;
import com.cnh.pf.data.management.aidl.MediumDevice;

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

   @Provides
   public MediumImpl getMediums() {
      //TODO add functionality to detect other displays, use mediator to getType of other datasources in combination with ip to detect other displays
      return new MediumImpl() {
         @Override public List<MediumDevice> getDevices() {
            return getUsbFile() != null ? new ArrayList<MediumDevice>() {{add(new MediumDevice(Datasource.Source.USB, getUsbFile()));}} : null;
         }
      };
   }
}
