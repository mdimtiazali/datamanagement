/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.service;

import android.app.Application;
import android.content.Intent;
import android.os.IBinder;

import com.cnh.android.util.prefs.GlobalPreferences;
import com.cnh.pf.android.data.management.RoboModule;
import com.cnh.pf.jgroups.ChannelModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.inject.Inject;

import roboguice.RoboGuiceHelper;
import roboguice.service.RoboService;

/**
 * Starts network routes in standalone mode.
 *
 * Created by mkedzierski on 12/2/15.
 */
public class MulticastRouteService extends RoboService {
   private static final Logger log = LoggerFactory.getLogger(MulticastRouteService.class);

   @Inject
   private GlobalPreferences prefs;

   @Override
   public void onCreate() {
      final Application app = getApplication();
      //Phoenix Workaround (phoenix sometimes cannot read the manifest)
      RoboGuiceHelper.help(app, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" }, new RoboModule(app), new ChannelModule(app));
      super.onCreate();
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      if (intent == null) return START_STICKY;
      if (!prefs.hasPCM()) {
         try {
            Runtime.getRuntime().exec("su busybox ifconfig lo multicast".split(" "));
            Runtime.getRuntime().exec("su busybox route add -net 224.0.0.0 netmask 240.0.0.0 dev lo".split(" "));
            log.trace("Added loopback multicast route");
         }
         catch (IOException e) {
            log.error("Error", e);
         }
      }
      return START_STICKY;
   }

   @Override
   public IBinder onBind(Intent intent) {
      return null;
   }
}
