/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import com.cnh.android.util.prefs.GlobalPreferences;
import com.google.inject.name.Named;
import org.jgroups.stack.GossipRouter;
import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.service.RoboService;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Starts Gossip Router in standalone mode.
 *
 * Created by mkedzierski on 12/2/15.
 */
public class GossipRouterService extends RoboService {
   private static final Logger log = LoggerFactory.getLogger(GossipRouterService.class);

   @Named("global")
   @Inject private SharedPreferences prefs;

   private GossipRouter gossip;

   @Override public int onStartCommand(Intent intent, int flags, int startId) {
      if (gossip != null) {
         return START_STICKY;
      }

      if(!prefs.getBoolean(GlobalPreferences.PREF_PCM, false)) {
         try {
            Runtime.getRuntime().exec("su busybox ifconfig lo multicast".split(" "));
            Runtime.getRuntime().exec("su busybox route add -net 224.0.0.0 netmask 240.0.0.0 dev lo".split(" "));
            log.trace("Added routes");
         }
         catch (IOException e) {
            log.error("Error", e);
         }
         //keep this in case we wish to return to using a gossip router on display.
//         new Thread() {
//            public void run() {
//               try {
//                  gossip = new GossipRouter("127.0.0.1", 12001) {
//                     @Override public void stop() {
//                        if(gossip.running()) {
////                           Util.close(gossip);
//                        }
//                        super.stop();
//                     }
//                  };
//                  gossip.useNio(false);
//                  gossip.jmx(false);
//                  gossip.start();
//               }
//               catch (Exception e) {
//                  log.error("Error starting gossip router", e);
//               }
//            }
//         }.start();
         return START_NOT_STICKY;
      }else {
         return START_NOT_STICKY;
      }
   }

   @Override public void onDestroy() {
      if (gossip != null) {
         gossip.stop();
         gossip = null;
      }
      super.onDestroy();
   }

   @Override public IBinder onBind(Intent intent) {
      return null;
   }
}
