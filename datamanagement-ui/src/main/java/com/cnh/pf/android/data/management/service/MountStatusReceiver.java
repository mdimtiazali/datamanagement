/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
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
 * Listens for USB media events.
 * @author kedzie
 */
public class MountStatusReceiver extends BroadcastReceiver {
   private static final Logger log = LoggerFactory.getLogger(MountStatusReceiver.class);

   @Override
   public void onReceive(Context context, Intent intent) {
      log.info("Received broadcast: {}", intent.getAction());
      context.startService(new Intent(context, DataManagementService.class).setAction(intent.getAction()).setData(intent.getData()).putExtras(intent));
   }

}
