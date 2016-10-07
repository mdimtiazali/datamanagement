/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives ACTION_BOOT_COMPLETED broadcast and launches {@link MulticastRouteService}
 *
 * @author kedzie
 */
public class BootReceiver extends BroadcastReceiver {

   private static Logger logger = LoggerFactory.getLogger(BootReceiver.class);

   @Override
   public void onReceive(Context context, Intent intent) {
      logger.info("Got Broadcast: " + intent.getAction());
      if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
         context.startService(new Intent(context, MulticastRouteService.class).setAction(intent.getAction()));
      }
   }
}
