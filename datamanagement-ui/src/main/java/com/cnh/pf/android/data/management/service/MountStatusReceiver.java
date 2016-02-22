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
import android.graphics.drawable.BitmapDrawable;
import android.os.ParcelUuid;
import com.cnh.android.status.Status;
import com.cnh.pf.android.data.management.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for USB media events.
 * @author kedzie
 */
public class MountStatusReceiver extends BroadcastReceiver {
   private static final Logger log = LoggerFactory.getLogger(MountStatusReceiver.class);

   private static Status status;

   @Override
   public void onReceive(Context context, Intent intent) {
      log.info("Received broadcast: {}", intent.getAction() );
      if(status==null) {
         status = new Status("USB Mounted",
               (BitmapDrawable)context.getResources().getDrawable(R.drawable.ic_usb),
               context.getPackageName());
      }
      if(Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {

         context.sendBroadcast(new Intent(Status.ACTION_STATUS_DISPLAY).putExtra(Status.NAME, status));
      }
      else if(Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())
            || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(intent.getAction())
            || Intent.ACTION_MEDIA_REMOVED.equals(intent.getAction())
            || Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
         log.info("Media has been unmounted: {} extras", intent.getExtras().size());
         context.sendBroadcast(new Intent(Status.ACTION_STATUS_REMOVE)
               .putExtra(Status.ID, ParcelUuid.fromString(status.getID().toString())));
      }
   }


}
